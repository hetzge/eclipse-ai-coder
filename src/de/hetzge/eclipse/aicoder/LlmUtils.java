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
		case OPENAI:
			return executeOpenAi(prompt, suffix);
		case MISTRAL:
			return executeMistral(prompt, suffix);
		default:
			throw new IllegalStateException("Illegal provider: " + provider);
		}
	}

	private static String executeMistral(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getCodestralBaseUrl();
		final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();
		final Json json = Json.object()
				.set("model", "codestral-latest")
				.set("prompt", prompt.trim())
				.set("suffix", suffix.trim())
				.set("max_tokens", 1024)
				.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"))
				.set("temperature", 0);
		final URL url = URI.create(urlString).toURL();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + codestralApiKey);
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(json.toString().getBytes(StandardCharsets.UTF_8));
		}
		final int responseCode = connection.getResponseCode();
		final StringBuilder responseBody = new StringBuilder();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (InputStream inputStream = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				String line;
				while ((line = reader.readLine()) != null) {
					responseBody.append(line);
				}
			}
			return Json.read(responseBody.toString()).at("choices").at(0).at("message").at("content").asString().trim();
		} else {
			AiCoderActivator.getDefault().getLog().log(
					new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, "Error: " + connection.getResponseMessage()));
			return "";
		}
	}

	private static String executeOpenAi(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getOpenAiBaseUrl();
		final String apiKey = AiCoderPreferences.getOpenAiApiKey();
		final String model = AiCoderPreferences.getOpenAiModel();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();

		final Json json = Json.object()
				.set("model", model)
				.set("messages", Json.array()
						.add(Json.object()
								.set("role", "user")
								.set("content", prompt.trim() + suffix.trim())))
				.set("max_tokens", 1024)
				.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"))
				.set("temperature", 0);

		final URL url = URI.create(urlString).toURL();
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(json.toString().getBytes(StandardCharsets.UTF_8));
		}
		final int responseCode = connection.getResponseCode();
		final StringBuilder responseBody = new StringBuilder();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (InputStream inputStream = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				String line;
				while ((line = reader.readLine()) != null) {
					responseBody.append(line);
				}
			}
			return Json.read(responseBody.toString()).at("choices").at(0).at("message").at("content").asString().trim();
		} else {
			AiCoderActivator.getDefault().getLog().log(
					new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, "Error: " + connection.getResponseMessage()));
			return "";
		}
	}
}