package de.hetzge.eclipse.aicoder.history;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import mjson.Json;

public final class HistoryEntry {

	private final UUID id;
	private final Instant timestamp;
	private final CompletionMode mode;
	private final Path filePath;
	private String context;
	private String content;
	private List<LlmResponse> responses;
	private Duration duration;
	private HistoryStatus status;

	public HistoryEntry(UUID id, CompletionMode mode, Path filePath, String context, String content, List<LlmResponse> responses, Duration duration, HistoryStatus status) {
		this(id, mode, filePath, context, content, responses, duration, status, Instant.now());
	}

	private HistoryEntry(UUID id, CompletionMode mode, Path filePath, String context, String content, List<LlmResponse> responses, Duration duration, HistoryStatus status, Instant timestamp) {
		this.id = id;
		this.timestamp = timestamp;
		this.mode = mode;
		this.filePath = EclipseUtils.toWorkspaceRootRelativePath(filePath);
		this.context = context;
		this.content = content;
		this.responses = new ArrayList<>(responses);
		this.duration = duration;
		this.status = status;
	}

	public UUID getId() {
		return this.id;
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}

	public CompletionMode getMode() {
		return this.mode;
	}

	public Path getFilePath() {
		return this.filePath;
	}

	public String getContext() {
		return this.context;
	}

	public String getContent() {
		return this.content;
	}

	public List<LlmResponse> getResponses() {
		return this.responses;
	}

	public Duration getDuration() {
		return this.duration;
	}

	public HistoryStatus getStatus() {
		return this.status;
	}

	public Json toJson() {
		final Json responsesJson = Json.array();
		for (final LlmResponse response : getResponses()) {
			responsesJson.add(response.toJson());
		}
		return Json.object()
				.set("id", getId().toString())
				.set("timestamp", getTimestamp().toString())
				.set("mode", getMode().name())
				.set("filePath", getFilePath().toString())
				.set("context", getContext())
				.set("content", getContent())
				.set("responses", responsesJson)
				.set("status", getStatus().name())
				.set("duration", getDuration().toMillis());
	}

	public void update(Consumer<Setter> callback) {
		callback.accept(new Setter());
		AiCoderHistoryView.get().ifPresent(view -> {
			Display.getDefault().asyncExec(() -> {
				view.addHistoryEntry(this);
			});
		});
	}

	public static HistoryEntry fromJson(Json json) {
		final UUID id = UUID.fromString(json.at("id").asString());
		final Instant timestamp = json.has("timestamp") ? Instant.parse(json.at("timestamp").asString()) : Instant.now();
		final CompletionMode mode = CompletionMode.valueOf(json.at("mode").asString());
		final Path filePath = EclipseUtils.toWorkspaceRootRelativePath(Paths.get(json.at("filePath").asString()));
		final String context = json.at("content").asString();
		final String content = json.at("content").asString();
		final List<LlmResponse> responses = new ArrayList<>();
		if (json.has("responses") && json.at("responses").isArray()) {
			for (final Json responseJson : json.at("responses").asJsonList()) {
				responses.add(new LlmResponse(responseJson));
			}
		} else if (json.has("response") && !json.at("response").isNull()) {
			// backward compatibility with the legacy single "response" field
			responses.add(new LlmResponse(json.at("response")));
		}
		final HistoryStatus status = HistoryStatus.valueOf(json.at("status").asString());
		final Duration duration = Duration.ofMillis(json.at("duration").asLong());
		return new HistoryEntry(id, mode, filePath, context, content, responses, duration, status, timestamp);
	}

	public class Setter {

		public void setContext(String context) {
			HistoryEntry.this.context = context;
		}

		public void setContent(String content) {
			HistoryEntry.this.content = content;
		}

		public void setResponses(List<LlmResponse> responses) {
			HistoryEntry.this.responses = new ArrayList<>(responses);
		}

		public void addResponse(LlmResponse response) {
			if (response != null) {
				HistoryEntry.this.responses.add(response);
			}
		}

		public void setDuration(Duration duration) {
			HistoryEntry.this.duration = duration;
		}

		public void setStatus(HistoryStatus status) {
			HistoryEntry.this.status = status;
		}
	}
}