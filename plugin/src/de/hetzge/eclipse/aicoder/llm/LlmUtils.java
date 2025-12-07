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
		final boolean isPseudoFim = isFillInTheMiddle && AiCoderPreferences.isEnablePseduoFim();
		final String urlString = "https://codestral.mistral.ai";
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
		final String path = isFillInTheMiddle && !isPseudoFim ? "./v1/completions" : "./v1/chat/completions";
		final URL url = URI.create(urlString).resolve(path).toURL();
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
			final String content = isFillInTheMiddle && !isPseudoFim
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