package de.hetzge.eclipse.aicoder.mcp;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import de.hetzge.eclipse.aicoder.mcp.McpConfiguration.CommandMcpConfiguration;
import de.hetzge.eclipse.aicoder.mcp.McpConfiguration.HttpMcpConfiguration;
import de.hetzge.eclipse.aicoder.mcp.McpConfiguration.SseMcpConfiguration;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import mjson.Json;
import tools.jackson.databind.json.JsonMapper;

public sealed interface McpConfiguration permits CommandMcpConfiguration, HttpMcpConfiguration, SseMcpConfiguration {

	McpClientTransport toMcpTransport();

	Json toJson();

	public static McpConfiguration fromJson(Json json) {
		if (json.has("type") && json.at("type").asString().equalsIgnoreCase("http")) {
			return HttpMcpConfiguration.fromJson(json);
		} else {
			return CommandMcpConfiguration.fromJson(json);
		}
	}

	record CommandMcpConfiguration(
			String command,
			String[] args,
			Map<String, String> environment) implements McpConfiguration {

		@Override
		public Json toJson() {
			return Json.object()
					.set("command", this.command)
					.set("args", this.args)
					.set("environment", this.environment);
		}

		@Override
		public McpClientTransport toMcpTransport() {
			final HashMap<String, String> resolvedEnvironment = this.environment.entrySet().stream().collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), resolveEnvironmentVariable(entry)), HashMap::putAll);
			final ServerParameters parameters = ServerParameters
					.builder(this.command)
					.args(this.args)
					.env(resolvedEnvironment)
					.build();
			return new StdioClientTransport(parameters, new JacksonMcpJsonMapper(JsonMapper.shared()));
		}

		private String resolveEnvironmentVariable(Entry<String, String> entry) {
			return entry.getKey().equals(entry.getValue())
					? Optional.ofNullable(System.getenv(entry.getKey())).orElse(entry.getValue())
					: entry.getValue().startsWith("$")
							? Optional.ofNullable(System.getenv(entry.getValue().substring(1))).orElse(entry.getValue())
							: entry.getValue();
		}

		public static CommandMcpConfiguration fromJson(Json json) {
			final String command = json.at("command").asString();
			final String[] args = json.at("args", new String[] {}).asJsonList().stream().map(it -> it.asString()).toList().toArray(new String[] {});
			final Map<String, String> environment = json.at("environment", json.at("env", new HashMap<>())).asJsonMap().entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().asString()), HashMap::putAll);
			return new CommandMcpConfiguration(command, args, environment);
		}
	}

	record HttpMcpConfiguration(
			URI url,
			Map<String, String> headers) implements McpConfiguration {

		@Override
		public Json toJson() {
			return Json.object()
					.set("url", this.url.toString())
					.set("headers", this.headers);
		}

		@Override
		public McpClientTransport toMcpTransport() {
			return HttpClientStreamableHttpTransport
					.builder(this.url.getScheme() + "://" + this.url.getHost() + ":" + this.url.getPort())
					.endpoint(this.url.getPath())
					.httpRequestCustomizer((builder, a, b, c, d) -> {
						for (final Map.Entry<String, String> entry : this.headers.entrySet()) {
							builder.header(entry.getKey(), entry.getValue());
						}
					})
					.build();
		}

		public static HttpMcpConfiguration fromJson(Json json) {
			final URI url = URI.create(json.at("url").asString());
			final Map<String, String> headers = json.at("headers", Map.of()).asJsonMap().entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().asString()), HashMap::putAll);
			return new HttpMcpConfiguration(url, headers);
		}
	}

	record SseMcpConfiguration(
			URI url,
			Map<String, String> headers) implements McpConfiguration {

		@Override
		public Json toJson() {
			return Json.object()
					.set("url", this.url.toString())
					.set("headers", this.headers);
		}

		@Override
		public McpClientTransport toMcpTransport() {
			return HttpClientSseClientTransport
					.builder(this.url.toString())
					.httpRequestCustomizer((builder, a, b, c, d) -> {
						for (final Map.Entry<String, String> entry : this.headers.entrySet()) {
							builder.header(entry.getKey(), entry.getValue());
						}
					})
					.build();
		}

		public static SseMcpConfiguration fromJson(Json json) {
			final URI url = URI.create(json.at("url").asString());
			final Map<String, String> headers = json.at("headers", Map.of()).asJsonMap().entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().asString()), HashMap::putAll);
			return new SseMcpConfiguration(url, headers);
		}
	}
}
