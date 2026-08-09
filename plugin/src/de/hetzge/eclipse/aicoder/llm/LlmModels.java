package de.hetzge.eclipse.aicoder.llm;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.HttpUtils;
import de.hetzge.eclipse.aicoder.util.Utils;
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
			final CompletableFuture<List<LlmOption>> ollamaModels = CompletableFuture.supplyAsync(this::loadOllamaModels);
			final CompletableFuture<List<LlmOption>> openAiModels = CompletableFuture.supplyAsync(this::loadOpenAiModels);
			final CompletableFuture<List<LlmOption>> openAi2Models = CompletableFuture.supplyAsync(this::loadOpenAi2Models);
			final CompletableFuture<List<LlmOption>> openAi3Models = CompletableFuture.supplyAsync(this::loadOpenAi3Models);
			final CompletableFuture<List<LlmOption>> mistralModels = CompletableFuture.supplyAsync(this::loadMistralModels);
			final CompletableFuture<List<LlmOption>> inceptionLabsModels = CompletableFuture.supplyAsync(this::loadInceptionLabsModels);
			final CompletableFuture<List<LlmOption>> openRouterModels = CompletableFuture.supplyAsync(this::loadOpenRouterModels);
			CompletableFuture.allOf(ollamaModels, openAiModels, openAi2Models, openAi3Models, mistralModels, inceptionLabsModels, openRouterModels).completeOnTimeout(null, 1, TimeUnit.MINUTES).join();
			this.options.addAll(ollamaModels.join());
			this.options.addAll(openAiModels.join());
			this.options.addAll(openAi2Models.join());
			this.options.addAll(openAi3Models.join());
			this.options.addAll(mistralModels.join());
			this.options.addAll(inceptionLabsModels.join());
			this.options.addAll(openRouterModels.join());
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
		return loadOpenAiModels(LlmProvider.OPENAI, AiCoderPreferences.getOpenAiBaseUrl(), AiCoderPreferences.getOpenAiApiKey());
	}

	private List<LlmOption> loadOpenAi2Models() {
		return loadOpenAiModels(LlmProvider.OPENAI_2, AiCoderPreferences.getOpenAi2BaseUrl(), AiCoderPreferences.getOpenAi2ApiKey());
	}

	private List<LlmOption> loadOpenAi3Models() {
		return loadOpenAiModels(LlmProvider.OPENAI_3, AiCoderPreferences.getOpenAi3BaseUrl(), AiCoderPreferences.getOpenAi3ApiKey());
	}

	private List<LlmOption> loadOpenAiModels(LlmProvider provider, String openAiBaseUrl, String openAiApiKey) {
		if (StringUtils.isBlank(openAiBaseUrl)) {
			return List.of();
		}
		if (Objects.equals(openAiBaseUrl, "https://api.openai.com") && StringUtils.isBlank(openAiApiKey)) {
			return List.of();
		}
		return loadOpenAiApiModels(provider, openAiBaseUrl, openAiApiKey).stream()
				.flatMap(model -> {
					if (model.modelKey().startsWith("gpt-") || model.modelKey().startsWith("openai/gpt-")) {
						return Stream.concat(Stream.of(model), LlmUtils.OPENAI_REASONING_SUFFIXES.stream()
								.map(suffix -> new LlmOption(model.provider(), model.modelKey() + suffix)));
					} else {
						return List.of(model).stream();
					}
				})
				.toList();
	}

	private List<LlmOption> loadOpenAiApiModels(LlmProvider provider, String openAiBaseUrl, String openAiApiKey) {
		try {
			final URL url = URI.create(Utils.joinUriParts(List.of(openAiBaseUrl, "/v1/models"))).toURL();
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
			final int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				AiCoderActivator.log().info(String.format("Received openai (%s) response code: %d -> skip openai models", openAiBaseUrl, responseCode));
				return List.of();
			}
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Json responseJson = Json.read(responseBody);
			final Json modelsJson = responseJson.at("data");
			return modelsJson.asJsonList().stream().map(modelJson -> {
				final String modelName = modelJson.at("id").asString();
				return new LlmOption(provider, modelName);
			}).toList();
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying openai models (%s): %s -> skip openai models", exception.getClass().getName(), openAiBaseUrl, exception.getMessage()));
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

	private List<LlmOption> loadInceptionLabsModels() {
		try {
			final String inceptionLabsApiKey = AiCoderPreferences.getInceptionLabsApiKey();
			if (inceptionLabsApiKey == null || inceptionLabsApiKey.isBlank()) {
				return List.of();
			}
			final URL url = URI.create("https://api.inceptionlabs.ai/v1/models").toURL();
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + inceptionLabsApiKey);
			final int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				AiCoderActivator.log().info("Received inceptionlabs response code: " + responseCode + " -> skip inceptionlabs models");
				return List.of();
			}
			final String responseBody = HttpUtils.readResponseBody(connection);
			final Json responseJson = Json.read(responseBody);
			final Json modelsJson = responseJson.at("data");
			return modelsJson.asJsonList().stream().map(modelJson -> {
				final String modelName = modelJson.at("id").asString();
				return new LlmOption(LlmProvider.INCEPTIONLABS, modelName);
			}).toList();
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying inceptionlabs models: %s -> skip inceptionlabs models", exception.getClass().getName(), exception.getMessage()));
			return List.of();
		}
	}

	private List<LlmOption> loadOpenRouterModels() {
		try {
			final String openRouterApiKey = AiCoderPreferences.getOpenRouterApiKey();
			if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
				return List.of();
			}
			return loadOpenAiApiModels(LlmProvider.OPENROUTER, LlmUtils.OPENROUTER_BASE_URL, openRouterApiKey).stream()
					.flatMap(model -> List.of(
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey()),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":free"),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":extended"),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":exacto"),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":thinking"),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":online"),
							new LlmOption(LlmProvider.OPENROUTER, model.modelKey() + ":nitro")).stream())
					.toList();
		} catch (final Exception exception) {
			AiCoderActivator.log().info(String.format("%s while querying openrouter models: %s -> skip openrouter models", exception.getClass().getName(), exception.getMessage()));
			return List.of();
		}
	}
}
