package de.hetzge.eclipse.aicoder.llm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.HttpUtils;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;
import mjson.Json;

public final class LlmUtils {

	private LlmUtils() {
	}

	public static LlmResponse executeGenerate(String systemPrompt, String prompt) throws IOException {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static LlmResponse executeEdit(String systemPrompt, String prompt) throws IOException {
		return execute(LlmOption.createEditModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static LlmResponse executeQuickFix(String systemPrompt, String prompt) throws IOException {
		return execute(LlmOption.createQuickFixModelOptionFromPreferences(), systemPrompt, prompt, null);
	}

	public static LlmResponse executeFillInTheMiddle(String prefix, String suffix) throws IOException {
		return execute(LlmOption.createFillInMiddleModelOptionFromPreferences(), null, prefix, suffix);
	}

	private static LlmResponse execute(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) throws IOException {
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
		default:
			throw new IllegalStateException("Illegal provider: " + provider);
		}
	}

	private static LlmResponse executeOllama(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) throws IOException {
		final boolean isFillInTheMiddle = suffix != null;
		final String urlString = AiCoderPreferences.getOllamaBaseUrl();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", llmModelOption.modelKey())
				.set("prompt", prompt)
				.set("stream", false)
				.set("options", Json.object()
						.set("temperature", 0));
		if (systemPrompt != null) {
			json.set("system", systemPrompt);
		}
		if (isFillInTheMiddle) {
			json.set("suffix", suffix);
			json.at("options")
					.set("num_predict", AiCoderPreferences.getMaxTokens())
					.set("stop", createStop(multilineEnabled));
		}
		final URL url = URI.create(urlString).resolve("/api/generate").toURL();
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);
		HttpUtils.writeRequestBody(connection, json);
		final int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			final Json responseJson = Json.read(responseBody);
			final String content = responseJson.at("response").asString();
			final int inputTokens = responseJson.at("prompt_eval_count").asInteger();
			final int outputTokens = responseJson.at("eval_count").asInteger();
			return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			final String responseBody = HttpUtils.readErrorResponseBody(connection);
			return new LlmResponse(llmModelOption, "", responseBody, 0, 0, duration, true);
		}
	}

	private static LlmResponse executeMistral(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) throws IOException {
		final boolean isFillInTheMiddle = suffix != null;
		final String urlString = "https://codestral.mistral.ai";
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", llmModelOption.modelKey())
				.set("temperature", 0);
		if (isFillInTheMiddle) {
			json.set("prompt", prompt)
					.set("suffix", suffix)
					.set("max_tokens", AiCoderPreferences.getMaxTokens())
					.set("stop", createStop(multilineEnabled));
		} else {
			final Json messagesJson = Json.array();
			if (systemPrompt != null) {
				messagesJson.add(Json.object()
						.set("role", "system")
						.set("content", systemPrompt));
			}
			messagesJson.add(Json.object()
					.set("role", "user")
					.set("content", prompt));
			json.set("messages", messagesJson);
		}
		final String path = isFillInTheMiddle ? "/v1/fim/completions" : "/v1/chat/completions";
		final URL url = URI.create(urlString).resolve(path).toURL();
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + codestralApiKey);
		connection.setDoOutput(true);
		HttpUtils.writeRequestBody(connection, json);
		final int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			final Json responseJson = Json.read(responseBody);
			final String content = responseJson.at("choices").at(0).at("message").at("content").asString();
			final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
			final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
			return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			final String responseBody = HttpUtils.readErrorResponseBody(connection);
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			return new LlmResponse(llmModelOption, "", responseBody, 0, 0, duration, true);
		}
	}

	private static LlmResponse executeOpenAi(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix) throws IOException {
		final boolean isFillInTheMiddle = suffix != null;
		final String urlString = AiCoderPreferences.getOpenAiBaseUrl();
		final String openAiApiKey = AiCoderPreferences.getOpenAiApiKey();
		final Json json = Json.object()
				.set("model", llmModelOption.modelKey())
				.set("temperature", 0);
		if (!isFillInTheMiddle) {
			final Json messagesJson = Json.array();
			if (systemPrompt != null) {
				messagesJson.add(Json.object()
						.set("role", "system")
						.set("content", systemPrompt));
			}
			messagesJson.add(Json.object()
					.set("role", "user")
					.set("content", prompt));
			json.set("messages", messagesJson);
		} else {
			final String fimPrompt = JinjaUtils.applyTemplate(AiCoderPreferences.getOpenAiFimTemplate(), Map.ofEntries(
					Map.entry("prefix", prompt),
					Map.entry("suffix", suffix)));
			json.set("prompt", fimPrompt)
					.set("max_tokens", AiCoderPreferences.getMaxTokens())
					.set("stop", createStop(AiCoderPreferences.isMultilineEnabled()));
		}

		final URL url = URI.create(urlString + "/").resolve(isFillInTheMiddle ? "./v1/completions" : "./v1/chat/completions").toURL();
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
		connection.setDoOutput(true);
		HttpUtils.writeRequestBody(connection, json);
		final int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			final Json responseJson = Json.read(responseBody);
			final String content = isFillInTheMiddle
					? responseJson.at("choices").at(0).at("text").asString()
					: responseJson.at("choices").at(0).at("message").at("content").asString();
			final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
			final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
			return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			final String responseBody = HttpUtils.readErrorResponseBody(connection);
			final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
			return new LlmResponse(llmModelOption, "", responseBody, 0, 0, duration, true);
		}
	}

	private static Json createStop(final boolean multilineEnabled) {
		return Json.array()
				.add(multilineEnabled ? "\n\n" : "\n")
				.add(multilineEnabled ? "\r\n\r\n" : "\r\n");
	}
}