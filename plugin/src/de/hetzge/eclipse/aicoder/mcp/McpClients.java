package de.hetzge.eclipse.aicoder.mcp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import mjson.Json;

public enum McpClients {
	INSTANCE;

	private final Map<String, AiCoderMcpContent> contentByMcpKey;

	private McpClients() {
		this.contentByMcpKey = new ConcurrentHashMap<>();
	}

	public List<AiCoderMcpContent> getContents() {
		return this.contentByMcpKey.values().stream().toList();
	}

	public List<EditInstruction> getEditInstructions() {
		return this.contentByMcpKey.values().stream()
				.filter(AiCoderMcpContent::success)
				.flatMap(content -> content.editInstructions().stream())
				.toList();
	}

	public String getMcpStatusCountsString() {
		final long successCount = this.contentByMcpKey.values().stream().filter(AiCoderMcpContent::success).count();
		final long failureCount = this.contentByMcpKey.values().stream().filter(Predicate.not(AiCoderMcpContent::success)).count();
		return "%d success, %d failure".formatted(successCount, failureCount);
	}

	public synchronized void reload(Runnable callback) {
		new Job("Reload mcp servers") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final Map<String, Json> jsonByName = AiCoderPreferences.getMcpServerConfigurations().asJsonMap();
					final List<? extends Job> jobs = jsonByName.entrySet().stream().map(entry -> new Job("Load MCP '%s'".formatted(entry.getKey())) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							reload(entry);
							return Status.OK_STATUS;
						}
					}).toList();
					jobs.forEach(Job::schedule);
					jobs.forEach(LambdaExceptionUtils.rethrowConsumer(job -> job.join(Duration.ofSeconds(60).toMillis(), monitor)));
				} catch (final InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Failed to load mcp servers", exception);
				} finally {
					callback.run();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private void reload(Entry<String, Json> entry) {
		final StringBuilder loggingStringBuilder = new StringBuilder();
		final AtomicReference<McpSyncClient> clientReference = new AtomicReference<>();
		try {
			final Optional<McpSyncClient> clientOptional = createClient(entry.getValue(), loggingStringBuilder);
			if (clientOptional.isEmpty()) {
				return;
			}
			final McpSyncClient client = clientOptional.get();
			clientReference.set(client);
			final InitializeResult initializeResult = client.initialize();
			AiCoderActivator.log().info("Connected to mcp server '%s'".formatted(entry.getKey()));
			final List<EditInstruction> editInstructions = client.listPrompts().prompts().stream()
					.filter(prompt -> {
						// only prompts without arguments are supported
						return prompt.arguments().isEmpty();
					})
					.flatMap(prompt -> {
						final List<PromptMessage> messages = client.getPrompt(new GetPromptRequest(prompt.name(), Map.of())).messages();
						final Content content = messages.getFirst().content();
						if (!(content instanceof TextContent)) {
							// only text prompts are supported
							return Stream.empty();
						}
						final TextContent textContent = (TextContent) content;
						final String text = textContent.text();
						final String firstLine = text.lines().findFirst().orElse("");
						final String title = prompt.title() != null ? prompt.title() : firstLine.substring(0, Math.min(100, firstLine.length()));
						return Stream.of(new EditInstruction("MCP/%s/%s".formatted(entry.getKey(), prompt.name()), title, text));
					})
					.toList();
			this.contentByMcpKey.put(entry.getKey(), new AiCoderMcpContent(
					entry.getKey(),
					true,
					loggingStringBuilder.toString(),
					initializeResult.serverInfo().title(),
					initializeResult.instructions(),
					editInstructions));
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Failed to query prompts from mcp server '%s'".formatted(entry.getKey()), exception);
			final String output = loggingStringBuilder.toString() + "\n---\n" + stacktraceToString(exception);
			this.contentByMcpKey.put(entry.getKey(), new AiCoderMcpContent(entry.getKey(), false, output, "???", "???", List.of()));
		} finally {
			AiCoderActivator.log().info("Disconnect from mcp server '%s'".formatted(entry.getKey()));
			final McpSyncClient client = clientReference.get();
			if (client != null) {
				if (!client.closeGracefully()) {
					client.close();
				}
			}
		}
	}

	private String stacktraceToString(final Exception exception) {
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		exception.printStackTrace(printWriter);
		final String stacktrace = stringWriter.getBuffer().toString();
		return stacktrace;
	}

	private Optional<McpSyncClient> createClient(Json json, StringBuilder loggingStringBuilder) throws MalformedURLException {
		return createClientTransport(json)
				.stream()
				.peek(transport -> transport.setExceptionHandler(loggingStringBuilder::append))
				.map(transport -> McpClient.sync(transport)
						.requestTimeout(Duration.ofSeconds(30))
						.capabilities(ClientCapabilities.builder().build())
						.loggingConsumer(loggingStringBuilder::append)
						.build())
				.findFirst();
	}

	private Optional<McpClientTransport> createClientTransport(Json json) throws MalformedURLException {
		if (json.has("command")) {
			final String command = json.at("command", "").asString();
			final List<String> arguments = json.at("args", Json.array()).asJsonList().stream().map(Json::asString).toList();
			final Map<String, String> environment = json.at("env", Json.object()).asJsonMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asString()));
			return Optional.of(new StdioClientTransport(ServerParameters.builder(command).args(arguments).env(environment).build()));
		} else if (json.has("url")) {
			final URI uri = URI.create(json.at("url", "").asString());
			final Map<String, String> headers = json.at("headers", Json.object()).asJsonMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asString()));
			return Optional.of(HttpClientSseClientTransport
					.builder(uri.toURL().getProtocol() + "://" + uri.toURL().getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""))
					.sseEndpoint(uri.getPath())
					.customizeRequest(builder -> {
						for (final Map.Entry<String, String> entry : headers.entrySet()) {
							builder.setHeader(entry.getKey(), entry.getValue());
						}
					}).build());
		} else {
			return Optional.empty();
		}
	}
}
