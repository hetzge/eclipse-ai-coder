package de.hetzge.eclipse.aicoder.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiProvider;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import mjson.Json;

public final class LlmUtils {

	private LlmUtils() {
	}

	/**
	 * Execute the configured
	 *
	 * @param prompt the prompt
	 * @param suffix the suffix (can be <code>null</code> if chat instead of fill in middle should be used)
	 * @return the {@link LlmResponse}
	 */
	public static LlmResponse execute(String prompt, String suffix) throws IOException {
		final AiProvider provider = AiCoderPreferences.getAiProvider();
		switch (provider) {
		case OLLAMA:
			return executeOllama(prompt, suffix);
		case MISTRAL:
			return executeMistral(prompt, suffix);
		default:
			throw new IllegalStateException("Illegal provider: " + provider);
		}
	}

	private static LlmResponse executeOllama(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getOllamaBaseUrl();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", AiCoderPreferences.getOllamaModel())
				.set("prompt", prompt)
				.set("stream", false)
				.set("options", Json.object()
						.set("temperature", 0));
		if (suffix != null) {
			json.set("suffix", suffix);
			json.at("options")
					.set("num_predict", AiCoderPreferences.getMaxTokens())
					.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"));
		}
		final URL url = URI.create(urlString).resolve("/api/generate").toURL();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);
		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
		}
		final int responseCode = connection.getResponseCode();
		final String responseBody = readResponse(connection);
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final Json responseJson = Json.read(responseBody);
			final String content = responseJson.at("response").asString();
			final int inputTokens = responseJson.at("prompt_eval_count").asInteger();
			final int outputTokens = responseJson.at("eval_count").asInteger();
			return new LlmResponse(content, responseBody, inputTokens, outputTokens);
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			return new LlmResponse("", responseBody, 0, 0);
		}
	}

	private static LlmResponse executeMistral(String prompt, String suffix) throws IOException {
		final String urlString = "https://codestral.mistral.ai";
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", "codestral-latest")
				.set("temperature", 0);
		if (suffix != null) {
			json.set("prompt", prompt)
					.set("suffix", suffix)
					.set("max_tokens", AiCoderPreferences.getMaxTokens())
					.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"));
		} else {
			json.set("messages", Json.array().add(Json.object()
					.set("role", "user")
					.set("content", prompt)));
		}
		final String path = suffix != null ? "/v1/fim/completions" : "/v1/chat/completions";
		final URL url = URI.create(urlString).resolve(path).toURL();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + codestralApiKey);
		connection.setDoOutput(true);
		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
		}
		final int responseCode = connection.getResponseCode();
		final String responseBody = readResponse(connection);
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final Json responseJson = Json.read(responseBody);
			final String content = responseJson.at("choices").at(0).at("message").at("content").asString();
			final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
			final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
			return new LlmResponse(content, responseBody, inputTokens, outputTokens);
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			return new LlmResponse("", responseBody, 0, 0);
		}
	}

	private static String readResponse(final HttpURLConnection connection) throws IOException {
		final StringBuilder responseBody = new StringBuilder();
		try (InputStream inputStream = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				responseBody.append(line);
			}
		}
		return responseBody.toString();
	}

}