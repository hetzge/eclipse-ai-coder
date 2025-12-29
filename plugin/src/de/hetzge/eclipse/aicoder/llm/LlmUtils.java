package de.hetzge.eclipse.aicoder.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.next.NextEditRequest;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;
import de.hetzge.eclipse.aicoder.util.Utils;
import mjson.Json;

public final class LlmUtils {

	private static final String CODESTRAL_BASE_URL = "https://codestral.mistral.ai";
	private static final String INCEPTIONLABS_BASE_URL = "https://api.inceptionlabs.ai";

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private LlmUtils() {
	}

	public static CompletableFuture<LlmResponse> executeGenerate(String systemPrompt, String prompt) {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeEdit(String systemPrompt, String prompt) {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeQuickFix(String systemPrompt, String prompt) {
		return execute(LlmOption.createQuickFixModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static CompletableFuture<LlmResponse> executeFillInTheMiddle(String prefix, String suffix) {
		return execute(LlmOption.createFillInMiddleModelOptionFromPreferences(), null, prefix, suffix);
	}

	private static CompletableFuture<LlmResponse> execute(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		AiCoderActivator.log().log(new Status(IStatus.INFO, AiCoderActivator.PLUGIN_ID, String.format("Executing LLM: %s", llmModelOption)));
		final LlmProvider provider = llmModelOption.provider();
		switch (provider) {
		case NONE:
			throw new IllegalStateException("No LLM provider selected.");
		case OLLAMA:
			return executeOllama(llmModelOption, systemPrompt, prompt, suffix);
		case MISTRAL:
			return executeMistral(llmModelOption, systemPrompt, prompt, suffix);
		case OPENAI:
			return executeOpenAi(llmModelOption, systemPrompt, prompt, suffix);
		case INCEPTIONLABS:
			return executeInceptionLabs(llmModelOption, systemPrompt, prompt, suffix);
		default:
			throw new IllegalStateException("Illegal provider: " + provider);
		}
	}

	private static CompletableFuture<LlmResponse> executeOllama(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		final boolean isFillInTheMiddle = suffix != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = AiCoderPreferences.getOllamaBaseUrl();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("stream", false);
		json.set("options", Json.object().set("temperature", 0));
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.set("suffix", suffix);
				json.at("options").set("stop", createStop(multilineEnabled));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
						Map.entry("prefix", prompt),
						Map.entry("suffix", suffix)));
				json.set("system", pseudoFimSystemPrompt);
				json.set("prompt", pseudoFimUserPrompt);
			}
			json.at("options").set("num_predict", AiCoderPreferences.getMaxTokens());
		} else {
			json.set("prompt", prompt);
			json.set("system", systemPrompt);
		}
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, "/api/generate")));
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
						final int inputTokens = responseJson.at("prompt_eval_count").asInteger();
						final int outputTokens = responseJson.at("eval_count").asInteger();
						return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeMistral(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		final boolean isFillInTheMiddle = suffix != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = CODESTRAL_BASE_URL;
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 0);
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.set("prompt", prompt);
				json.set("suffix", suffix);
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(multilineEnabled));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
						Map.entry("prefix", prompt),
						Map.entry("suffix", suffix)));
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("messages", createMessages(pseudoFimSystemPrompt, pseudoFimUserPrompt));
			}
		} else {
			json.set("messages", createMessages(systemPrompt, prompt));
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
						final String content = responseJson.at("choices").at(0).at("message").at("content").asString();
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeOpenAi(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		final boolean isFillInTheMiddle = suffix != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = AiCoderPreferences.getOpenAiBaseUrl();
		final String openAiApiKey = AiCoderPreferences.getOpenAiApiKey();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 0);
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				final String fimTemplatePrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
						Map.entry("prefix", prompt),
						Map.entry("suffix", suffix)));
				json.set("prompt", fimTemplatePrompt);
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(AiCoderPreferences.isMultilineEnabled()));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
						Map.entry("prefix", prompt),
						Map.entry("suffix", suffix)));
				json.set("messages", createMessages(pseudoFimSystemPrompt, pseudoFimUserPrompt));
			}
		} else {
			json.set("messages", createMessages(systemPrompt, prompt));
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
								: responseJson.at("choices").at(0).at("message").at("content").asString();
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
					}
				});
	}

	private static CompletableFuture<LlmResponse> executeInceptionLabs(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) {
		final boolean isFillInTheMiddle = suffix != null;
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = INCEPTIONLABS_BASE_URL;
		final String inceptionApiKey = AiCoderPreferences.getInceptionLabsApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 0);
		if (isFillInTheMiddle) {
			if (!isPseudoFim) {
				json.set("prompt", prompt);
				json.set("suffix", suffix);
				json.set("max_tokens", AiCoderPreferences.getMaxTokens());
				json.set("stop", createStop(multilineEnabled));
			} else {
				final String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				final String pseudoFimUserPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
						Map.entry("prefix", prompt),
						Map.entry("suffix", suffix)));
				json.set("messages", createMessages(pseudoFimSystemPrompt, pseudoFimUserPrompt));
			}
		} else {
			json.set("messages", createMessages(systemPrompt, prompt));
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
								: responseJson.at("choices").at(0).at("message").at("content").asString();
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
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
						final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
						final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
						return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
					} else {
						AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
						return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
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
}