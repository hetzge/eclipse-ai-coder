package de.hetzge.eclipse.aicoder;

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

import mjson.Json;

public final class LlmUtils {

	private LlmUtils() {
	}

	public static String execute(String prompt, String suffix) throws IOException {
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

	private static String executeOllama(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getOllamaBaseUrl();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", AiCoderPreferences.getOllamaModel())
				.set("prompt", prompt)
				.set("suffix", suffix)
				.set("stream", false)
				.set("options", Json.object()
						.set("temperature", 0)
						.set("num_predict", AiCoderPreferences.getMaxTokens())
						.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n")));
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
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final String responseBody = readResponse(connection);
			return Json.read(responseBody).at("response").asString();
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			return "";
		}
	}

	private static String executeMistral(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getCodestralBaseUrl();
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", "codestral-latest")
				.set("prompt", prompt)
				.set("suffix", suffix)
				.set("max_tokens", AiCoderPreferences.getMaxTokens())
				.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"))
				.set("temperature", 0);
		final URL url = URI.create(urlString).resolve("/v1/fim/completions").toURL();
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
		if (responseCode == HttpURLConnection.HTTP_OK) {
			final String responseBody = readResponse(connection);
			return Json.read(responseBody).at("choices").at(0).at("message").at("content").asString();
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, String.format("Error: %s (%s)", connection.getResponseMessage(), responseCode)));
			return "";
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