package de.hetzge.eclipse.aicoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import mjson.Json;

public final class LlmUtils {

	private static final HttpClient CLIENT = HttpClient.newHttpClient();

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
				.set("prompt", prompt)
				.set("suffix", suffix)
				.set("max_tokens", AiCoderPreferences.getMaxTokens())
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
			AiCoderActivator.log().log(
					new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, "Error: " + connection.getResponseMessage()));
			return "";
		}
	}

	private static String executeOpenAi(String prompt, String suffix) throws IOException {
		final String urlString = AiCoderPreferences.getOpenAiBaseUrl();
		final String apiKey = AiCoderPreferences.getOpenAiApiKey();
		final String model = AiCoderPreferences.getOpenAiModel();
		final boolean multilineEnabled = AiCoderPreferences.isMultilineEnabled();

		// TODO
		// https://github.com/continuedev/continue/blob/030e534098da86c649fa19add7df251ad60f6333/core/autocomplete/templating/AutocompleteTemplate.ts#L156

		final Json json = Json.object()
				.set("model", model)
				.set("messages", Json.array()
						.add(Json.object()
								.set("role", "system")
								.set("content",
										"""
												You're a code completion assistant.
												You are provided code that contains a "{:FILL_HERE:}" placeholder.
												Generate the code that most likely could replace the placeholder.
												Only provide the code that should replace the placeholder in the output.
												Provide the replacement code between XML "<CODE>" tags.
																						""".trim()))
						.add(Json.object()
								.set("role", "user")
								.set("content", "Here is the code:\n" + prompt + "{:FILL_HERE:}" + suffix)))
				.set("max_tokens", AiCoderPreferences.getMaxTokens())
				.set("stop", Json.array().add(multilineEnabled ? "\n\n" : "\n"))
				.set("temperature", 0);

		final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(urlString))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		HttpResponse<String> response;
		try {
			response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (final InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted", exception);
		}
		if (response.statusCode() == 200) {
			final String body = response.body();
			if(body.isBlank() || !body.contains("<CODE>")) {
				return "";
			}
			return Json.read(body).at("choices").at(0).at("message").at("content").asString().trim().replace("<CODE>", "").replace("</CODE>", "");
		} else {
			AiCoderActivator.log().log(new Status(IStatus.WARNING, AiCoderActivator.PLUGIN_ID, "Error: " + response.body()));
			return "";
		}
	}
}