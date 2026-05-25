package de.hetzge.eclipse.aicoder.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.next.NextEditRequest;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;
import de.hetzge.eclipse.aicoder.util.Utils;
import mjson.Json;

public final class LlmUtils {

	public static final List<String> OPENAI_REASONING_SUFFIXES = List.of(":minimal", ":low", ":medium", ":high", ":xhigh");

	public static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api";
	private static final String CODESTRAL_BASE_URL = "https://codestral.mistral.ai";
	private static final String INCEPTIONLABS_BASE_URL = "https://api.inceptionlabs.ai";

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1) // force http 1.1, otherwise lm studio does not work (no response)
			.build();

	private LlmUtils() {
	}

	public static CompletableFuture<LlmResponse> executeGenerate(String systemPrompt, String prompt) {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeEdit(String systemPrompt, String prompt) {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeRerank(String systemPrompt, String prompt) {
		return execute(LlmOption.createRerankModelOptionFromPreferences(), systemPrompt, prompt, null); // TODO
	}

	public static CompletableFuture<LlmResponse> executeQuickFix(String systemPrompt, String prompt) {
		return execute(LlmOption.createQuickFixModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeFillInTheMiddle(String prefix, String suffix) {
		return execute(LlmOption.createFillInMiddleModelOptionFromPreferences(), null, prefix, suffix);
	}

	public static CompletableFuture<LlmResponse> executeAgent(LlmOption llmModelOption, LlmRequest llmRequest) {
		return execute(llmModelOption, llmRequest);
	}

	private static CompletableFuture<LlmResponse> execute(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		final LlmRequest llmRequest = new LlmRequest(List.of(
				new LlmMessage(LlmRole.SYSTEM, systemPrompt, null, List.of()),
				new LlmMessage(LlmRole.USER, prompt, null, List.of())),
				List.of(),
				prompt,
				suffix);
		return execute(llmModelOption, llmRequest);
	}

	private static CompletableFuture<LlmResponse> execute(LlmOption llmModelOption, LlmRequest llmRequest) {
		AiCoderActivator.log().log(new Status(IStatus.INFO, AiCoderActivator.PLUGIN_ID, String.format("Executing LLM: %s", llmModelOption)));
		final LlmProvider provider = llmModelOption.provider();
		switch (provider) {
		case NONE:
			throw new IllegalStateException("No LLM provider selected.");
		case OLLAMA:
			return executeOllama(llmModelOption, llmRequest);
		case MISTRAL:
			return executeMistral(llmModelOption, llmRequest);
		case OPENAI:
			final String urlString = AiCoderPreferences.getOpenAiBaseUrl();
			final String openAiApiKey = AiCoderPreferences.getOpenAiApiKey();
			return executeOpenAi(urlString, openAiApiKey, llmModelOption, llmRequest);
		case OPENROUTER:
			final String openRouterUrlString = OPENROUTER_BASE_URL;
			final String openRouterApiKey = AiCoderPreferences.getOpenRouterApiKey();
			return executeOpenAi(openRouterUrlString, openRouterApiKey, llmModelOption, llmRequest);
		case INCEPTIONLABS:
			return executeInceptionLabs(llmModelOption, llmRequest);
		default:
			throw new IllegalStateException("Illegal provider: " + provider);
		}
	}

	private static CompletableFuture<LlmResponse> executeOllama(LlmOption llmModelOption, LlmRequest llmRequest) {
		final boolean isFillInTheMiddle = llmRequest.suffix() != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = AiCoderPreferences.getOllamaBaseUrl();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("stream", false);
		json.set("think", true);
		json.set("options", Json.object()
				.set("temperature", 0)
				.set("num_ctx", AiCoderPreferences.getOllamaNumCtx()));
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.at("options").set("stop", createStop(multilineEnabled));
				final String fimTemplate = AiCoderPreferences.getFimTemplate();
				if (StringUtils.isNotBlank(fimTemplate)) {
					json.set("raw", true); // disable ollama's template engine
					json.set("prompt", JinjaUtils.applyTemplate(fimTemplate, Map.ofEntries(
							Map.entry("prefix", llmRequest.prefix()),
							Map.entry("suffix", llmRequest.suffix()))));
				} else {
					json.set("prompt", llmRequest.prefix());
					json.set("suffix", llmRequest.suffix());
				}
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getFimTemplate(), Map.ofEntries(
						Map.entry("prefix", llmRequest.prefix()),
						Map.entry("suffix", llmRequest.suffix())));
				json.set("system", pseudoFimSystemPrompt);
				json.set("prompt", pseudoFimUserPrompt);
			}
			json.at("options").set("num_predict", AiCoderPreferences.getMaxTokens());
		} else {
			json.set("tools", llmRequest.toolDefinitions().stream().map(toolDefinition -> Json.object().set("type", "function").set("function", toolDefinition.json())).toList());
			json.set("messages", llmRequest.messages().stream().map(message -> {
				final Json messageJson = Json.object()
						.set("role", message.role().name().toLowerCase())
						.set("content", message.content())
						.set("tool_name", message.toolCallId())
						.set("tool_calls", message.toolCallRequest().stream().map(toolCallRequest -> Json.object()
								.set("id", toolCallRequest.id())
								.set("type", toolCallRequest.type())
								.set("function", Json.object()
										.set("name", toolCallRequest.functionName())
										.set("arguments", toolCallRequest.arguments())))
								.toList());
				if (StringUtils.isNotBlank(message.reasoning())) {
					messageJson.set("thinking", message.reasoning());
				}
				return messageJson;
			}));
		}
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, isFillInTheMiddle ? "/api/generate" : "/api/chat")));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.timeout(AiCoderPreferences.getTimeout())
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
					if (response.statusCode() == 200) {
						final String responseBody = response.body();
						final Json responseJson = Json.read(responseBody);
						final String content = responseJson.at("response").asString();
						final String reasoning = responseJson.has("thinking") && responseJson.at("thinking").isString()
								? responseJson.at("thinking").asString()
								: "";
						final int inputTokens = responseJson.at("prompt_eval_count", 0).asInteger();
						final int outputTokens = responseJson.at("eval_count", 0).asInteger();
						final List<LlmToolCallRequest> toolCallRequests = responseJson.has("tool_calls")
								? responseJson.at("tool_calls").asJsonList().stream().map(toolCallJson -> new LlmToolCallRequest(
										toolCallJson.at("id").asString(),
										toolCallJson.at("type").asString(),
										toolCallJson.at("function").at("name").asString(),
										Json.read(toolCallJson.at("function").at("arguments").asString()))).toList()
								: List.of();
						final String plainResponse = reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
						return new LlmResponse(llmModelOption, reasoning, content, plainResponse, toolCallRequests, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", "", response.body(), List.of(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeMistral(LlmOption llmModelOption, LlmRequest llmRequest) {
		final boolean isFillInTheMiddle = llmRequest.suffix() != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = CODESTRAL_BASE_URL;
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 0);
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.set("prompt", llmRequest.prefix());
				json.set("suffix", llmRequest.suffix());
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(multilineEnabled));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getFimTemplate(), Map.ofEntries(
						Map.entry("prefix", llmRequest.prefix()),
						Map.entry("suffix", llmRequest.suffix())));
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("messages", createMessages(pseudoFimSystemPrompt, pseudoFimUserPrompt));
			}
		} else {
			json.set("tools", llmRequest.toolDefinitions().stream().map(toolDefinition -> Json.object().set("type", "function").set("function", toolDefinition.json())).toList());
			json.set("messages", createMessages(llmRequest.messages()));
		}
		final String path = isFillInTheMiddle && !isPseudoFim ? "/v1/fim/completions" : "/v1/chat/completions";
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, path)));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + codestralApiKey)
				.timeout(AiCoderPreferences.getTimeout())
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
					if (response.statusCode() == 200) {
						final String responseBody = response.body();
						final Json responseJson = Json.read(responseBody);
						final String content = responseJson.at("choices").at(0).at("message").at("content").isString()
								? responseJson.at("choices").at(0).at("message").at("content").asString()
								: "";
						final String reasoning = responseJson.at("choices").at(0).at("message").has("reasoning")
								&& responseJson.at("choices").at(0).at("message").at("reasoning").isString()
										? responseJson.at("choices").at(0).at("message").at("reasoning").asString()
										: "";
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						final List<LlmToolCallRequest> toolCallRequests = responseJson.at("choices").at(0).at("message").has("tool_calls")
								? responseJson.at("choices").at(0).at("message").at("tool_calls").asJsonList().stream().map(toolCallJson -> new LlmToolCallRequest(
										toolCallJson.at("id").asString(),
										toolCallJson.at("type").asString(),
										toolCallJson.at("function").at("name").asString(),
										Json.read(toolCallJson.at("function").at("arguments").asString()))).toList()
								: List.of();
						final String plainResponse = reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
						return new LlmResponse(llmModelOption, reasoning, content, plainResponse, toolCallRequests, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", "", response.body(), List.of(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeOpenAi(String urlString, String openAiApiKey, LlmOption llmModelOption, LlmRequest llmRequest) {
		final boolean isFillInTheMiddle = llmRequest.suffix() != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String reasoningSuffix = OPENAI_REASONING_SUFFIXES.stream()
				.filter(it -> llmModelOption.modelKey().endsWith(it))
				.findFirst()
				.orElse(null);
		final String model = reasoningSuffix != null
				? llmModelOption.modelKey().substring(0, llmModelOption.modelKey().length() - reasoningSuffix.length())
				: llmModelOption.modelKey();
		final Json json = Json.object();
		json.set("model", model);
		if (reasoningSuffix != null) {
			json.set("reasoning_effort", reasoningSuffix.substring(1));
		}
		if (isFillInTheMiddle) {
			final String fimTemplatePrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getFimTemplate(), Map.ofEntries(
					Map.entry("prefix", llmRequest.prefix()),
					Map.entry("suffix", llmRequest.suffix())));
			if (!isPseudoFim) {
				json.set("prompt", fimTemplatePrompt);
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(AiCoderPreferences.isMultilineEnabled()));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				json.set("messages", createMessages(pseudoFimSystemPrompt, fimTemplatePrompt));
			}
		} else {
			json.set("tools", llmRequest.toolDefinitions().stream().map(toolDefinition -> Json.object().set("type", "function").set("function", toolDefinition.json())).toList());
			json.set("messages", createMessages(llmRequest.messages()));
		}
		final String path = isFillInTheMiddle && !isPseudoFim ? "/v1/completions" : "/v1/chat/completions";
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, path)));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + openAiApiKey)
				.timeout(AiCoderPreferences.getTimeout())
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
					if (response.statusCode() == 200) {
						final String responseBody = response.body();
						final Json responseJson = Json.read(responseBody);
						final String content = isFillInTheMiddle && !isPseudoFim
								? responseJson.at("choices").at(0).at("text").asString()
								: responseJson.at("choices").at(0).at("message").at("content").isString()
										? responseJson.at("choices").at(0).at("message").at("content").asString()
										: "";
						final String reasoning = responseJson.at("choices").at(0).at("message").has("reasoning")
								&& responseJson.at("choices").at(0).at("message").at("reasoning").isString()
										? responseJson.at("choices").at(0).at("message").at("reasoning").asString()
										: responseJson.at("choices").at(0).at("message").has("reasoning_content")
												&& responseJson.at("choices").at(0).at("message").at("reasoning_content").isString()
														? responseJson.at("choices").at(0).at("message").at("reasoning_content").asString()
														: "";
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						final List<LlmToolCallRequest> toolCallRequests = responseJson.at("choices").at(0).at("message").has("tool_calls")
								? responseJson.at("choices").at(0).at("message").at("tool_calls").asJsonList().stream().map(toolCallJson -> new LlmToolCallRequest(
										toolCallJson.at("id").asString(),
										toolCallJson.at("type").asString(),
										toolCallJson.at("function").at("name").asString(),
										Json.read(toolCallJson.at("function").at("arguments").asString()))).toList()
								: List.of();
						final String plainResponse = reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
						return new LlmResponse(llmModelOption, reasoning, content, plainResponse, toolCallRequests, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", "", response.body(), List.of(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeInceptionLabs(LlmOption llmModelOption, LlmRequest llmRequest) {
		final boolean isFillInTheMiddle = llmRequest.suffix() != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = INCEPTIONLABS_BASE_URL;
		final String inceptionApiKey = AiCoderPreferences.getInceptionLabsApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 0);
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.set("prompt", llmRequest.prefix());
				json.set("suffix", llmRequest.suffix());
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(multilineEnabled));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getFimTemplate(), Map.ofEntries(
						Map.entry("prefix", llmRequest.prefix()),
						Map.entry("suffix", llmRequest.suffix())));
				json.set("messages", createMessages(pseudoFimSystemPrompt, pseudoFimUserPrompt));
			}
		} else {
			json.set("messages", createMessages(llmRequest.messages()));
		}
		final String path = isFillInTheMiddle && !isPseudoFim ? "/v1/fim/completions" : "/v1/chat/completions";
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, path)));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + inceptionApiKey)
				.timeout(AiCoderPreferences.getTimeout())
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
					if (response.statusCode() == 200) {
						final String responseBody = response.body();
						final Json responseJson = Json.read(responseBody);
						final String content = isFillInTheMiddle && !isPseudoFim
								? responseJson.at("choices").at(0).at("text").asString()
								: responseJson.at("choices").at(0).at("message").at("content").isString()
										? responseJson.at("choices").at(0).at("message").at("content").asString()
										: "";
						final String reasoning = responseJson.at("choices").at(0).at("message").has("reasoning")
								&& responseJson.at("choices").at(0).at("message").at("reasoning").isString()
										? responseJson.at("choices").at(0).at("message").at("reasoning").asString()
										: responseJson.at("choices").at(0).at("message").has("reasoning_content")
												&& responseJson.at("choices").at(0).at("message").at("reasoning_content").isString()
														? responseJson.at("choices").at(0).at("message").at("reasoning_content").asString()
														: "";
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						final List<LlmToolCallRequest> toolCallRequests = responseJson.at("choices").at(0).at("message").has("tool_calls")
								? responseJson.at("choices").at(0).at("message").at("tool_calls").asJsonList().stream().map(toolCallJson -> new LlmToolCallRequest(
										toolCallJson.at("id").asString(),
										toolCallJson.at("type").asString(),
										toolCallJson.at("function").at("name").asString(),
										Json.read(toolCallJson.at("function").at("arguments").asString()))).toList()
								: List.of();
						final String plainResponse = reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
						return new LlmResponse(llmModelOption, reasoning, content, plainResponse, toolCallRequests, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", "", response.body(), List.of(), 0, 0, duration, true);
					}
				});
	}

	public static CompletableFuture<LlmResponse> executeNextEdit(NextEditRequest request) {
		final LlmOption llmModelOption = LlmOption.createNextEditModelOptionFromPreferences();
		if (llmModelOption.provider() != LlmProvider.INCEPTIONLABS) {
			throw new IllegalArgumentException("Only InceptionLabs is currently supported for next edit.");
		}
		final String urlString = INCEPTIONLABS_BASE_URL;
		final String inceptionApiKey = AiCoderPreferences.getInceptionLabsApiKey();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("messages", Json.array()
				.add(Json.object()
						.set("role", "user")
						.set("content", request.toInceptionLabsNextEditPrompt())));

		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, "/v1/edit/completions")));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + inceptionApiKey)
				.timeout(AiCoderPreferences.getTimeout())
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
					if (response.statusCode() == 200) {
						final String responseBody = response.body();
						final Json responseJson = Json.read(responseBody);
						final String content = responseJson.at("choices").at(0).at("message").at("content").asString();
						final String reasoning = responseJson.at("choices").at(0).at("message").has("reasoning")
								&& responseJson.at("choices").at(0).at("message").at("reasoning").isString()
										? responseJson.at("choices").at(0).at("message").at("reasoning").asString()
										: responseJson.at("choices").at(0).at("message").has("reasoning_content")
												&& responseJson.at("choices").at(0).at("message").at("reasoning_content").isString()
														? responseJson.at("choices").at(0).at("message").at("reasoning_content").asString()
														: "";
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						final String plainResponse = reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
						return new LlmResponse(llmModelOption, reasoning, content, plainResponse, List.of(), inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", "", response.body(), List.of(), 0, 0, duration, true);
					}
				});
	}

	private static String getPseduoFIMSystemPrompt() {
		final String systemPrompt = AiCoderPreferences.getPseudoFimSystemPrompt();
		final boolean isMultilineEnabled = AiCoderPreferences.isMultilineEnabled();
		return systemPrompt + (isMultilineEnabled ? "" : "\n- Only generate a single line of code. The user expects only the completion of the current line.");
	}

	private static Json createStop(final boolean multilineEnabled) {
		return Json.array()
				.add(multilineEnabled ? "\n\n" : "\n")
				.add(multilineEnabled ? "\r\n\r\n" : "\r\n");
	}

	private static Json createMessages(String systemPrompt, String prompt) {
		return Json.array()
				.add(Json.object()
						.set("role", "system")
						.set("content", systemPrompt))
				.add(Json.object()
						.set("role", "user")
						.set("content", prompt));
	}

	private static Json createMessages(List<LlmMessage> messages) {
		final Json array = Json.array();
		for (final LlmMessage message : messages) {
			final Json messageJson = Json.object()
					.set("role", message.role().name().toLowerCase())
					.set("content", message.content());
			if (message.toolCallId() != null) {
				messageJson.set("tool_call_id", message.toolCallId());
			}
			if (message.toolCallRequest() != null && !message.toolCallRequest().isEmpty()) {
				messageJson.set("tool_calls", Json.array(message.toolCallRequest().stream()
						.map(toolCallRequest -> Json.object()
								.set("id", toolCallRequest.id())
								.set("type", toolCallRequest.type())
								.set("function", Json.object()
										.set("name", toolCallRequest.functionName())
										.set("arguments", toolCallRequest.arguments())))
						.toArray()));
			}
			if (StringUtils.isNotBlank(message.reasoning())) {
				messageJson.set("reasoning_content", message.reasoning());
			}
			array.add(messageJson);
		}
		return array;
	}

}