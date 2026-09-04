package de.hetzge.eclipse.aicoder.history;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
	private Optional<LlmResponse> responseOptional;
	private Duration duration;
	private HistoryStatus status;

	public HistoryEntry(UUID id, CompletionMode mode, Path filePath, String context, String content, Optional<LlmResponse> responseOptional, Duration duration, HistoryStatus status) {
		this(id, mode, filePath, context, content, responseOptional, duration, status, Instant.now());
	}

	private HistoryEntry(UUID id, CompletionMode mode, Path filePath, String context, String content, Optional<LlmResponse> responseOptional, Duration duration, HistoryStatus status, Instant timestamp) {
		this.id = id;
		this.timestamp = timestamp;
		this.mode = mode;
		this.filePath = EclipseUtils.toWorkspaceRootRelativePath(filePath);
		this.context = context;
		this.content = content;
		this.responseOptional = responseOptional;
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

	public Optional<LlmResponse> getResponseOptional() {
		return this.responseOptional;
	}

	public Duration getDuration() {
		return this.duration;
	}

	public HistoryStatus getStatus() {
		return this.status;
	}

	public Json toJson() {
		return Json.object()
				.set("id", getId().toString())
				.set("timestamp", getTimestamp().toString())
				.set("mode", getMode().name())
				.set("filePath", getFilePath().toString())
				.set("context", getContext())
				.set("content", getContent())
				.set("response", getResponseOptional().map(LlmResponse::toJson).orElse(Json.nil()))
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
		final Optional<LlmResponse> responseOptional = json.has("response") && !json.at("response").isNull()
				? Optional.of(new LlmResponse(json.at("response")))
				: Optional.empty();
		final HistoryStatus status = HistoryStatus.valueOf(json.at("status").asString());
		final Duration duration = Duration.ofMillis(json.at("duration").asLong());
		return new HistoryEntry(id, mode, filePath, context, content, responseOptional, duration, status, timestamp);
	}

	public class Setter {

		public void setContext(String context) {
			HistoryEntry.this.context = context;
		}

		public void setContent(String content) {
			HistoryEntry.this.content = content;
		}

		public void setResponseOptional(Optional<LlmResponse> responseOptional) {
			HistoryEntry.this.responseOptional = responseOptional;
		}

		public void setDuration(Duration duration) {
			HistoryEntry.this.duration = duration;
		}

		public void setStatus(HistoryStatus status) {
			HistoryEntry.this.status = status;
		}
	}
}