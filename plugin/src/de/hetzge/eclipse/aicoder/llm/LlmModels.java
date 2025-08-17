package de.hetzge.eclipse.aicoder.llm;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.HttpUtils;
import mjson.Json;

public enum LlmModels {
	INSTANCE;

	private final List<LlmOption> options;

	private LlmModels() {
		this.options = new ArrayList<>();
	}

	public void reset() {
		this.options.clear();
	}

	public synchronized List<LlmOption> getOrLoadOptions() {
		if (this.options.isEmpty()) {
			this.options.addAll(loadOllamaModels());
			this.options.addAll(loadOpenAiModels());
			this.options.addAll(loadMistralModels());
		}
		return this.options;
	}

	private List<LlmOption> loadOllamaModels() {
		try {
			final String ollamaBaseUrl = AiCoderPreferences.getOllamaBaseUrl();
			final URL url = URI.create(ollamaBaseUrl).resolve("/api/tags").toURL();
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			final int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				AiCoderActivator.log().info("Received ollama response code: " + responseCode + " -> skip ollama models");
				return List.of();
			}
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Json responseJson = Json.read(responseBody);
			final Json modelsJson = responseJson.at("models");
			return modelsJson.asJsonList().stream().map(modelJson -> {
				final String modelName = modelJson.at("name").asString();
				return new LlmOption(LlmProvider.OLLAMA, modelName);
			}).toList();
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying ollama models: %s -> skip ollama models", exception.getClass().getName(), exception.getMessage()));
			return List.of();
		}
	}

	private List<LlmOption> loadOpenAiModels() {
		try {
			final String openAiBaseUrl = AiCoderPreferences.getOpenAiBaseUrl();
			final URL url = URI.create(openAiBaseUrl + "/").resolve("./v1/models").toURL();
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			final int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				AiCoderActivator.log().info("Received openai response code: " + responseCode + " -> skip openai models");
				return List.of();
			}
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Json responseJson = Json.read(responseBody);
			final Json modelsJson = responseJson.at("data");
			return modelsJson.asJsonList().stream().map(modelJson -> {
				final String modelName = modelJson.at("id").asString();
				return new LlmOption(LlmProvider.OPENAI, modelName);
			}).toList();
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying openai models: %s -> skip openai models", exception.getClass().getName(), exception.getMessage()));
			return List.of();
		}
	}

	private List<LlmOption> loadMistralModels() {
		try {
			final String codestralApiKey = AiCoderPreferences.getCodestralApiKey();
			if (codestralApiKey == null || codestralApiKey.isBlank()) {
				return List.of();
			}
			return List.of(new LlmOption(LlmProvider.MISTRAL, "codestral-latest"));
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying mistral models: %s -> skip mistral models", exception.getClass().getName(), exception.getMessage()));
			return List.of();
		}
	}
}
