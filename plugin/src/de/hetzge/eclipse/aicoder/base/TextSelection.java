package de.hetzge.eclipse.aicoder.base;

import java.util.Optional;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import mjson.Json;

public record TextSelection(IPath path, int offset, int length, String content) {

	public Json toJson() {
		return Json.object()
				.set("path", this.path.toPortableString())
				.set("offset", this.offset)
				.set("length", this.length)
				.set("content", this.content);
	}

	public static TextSelection fromJson(Json json) {
		return new TextSelection(
				IPath.fromPortableString(json.at("path").asString()),
				json.at("offset").asInteger(),
				json.at("length").asInteger(),
				json.at("content").asString());
	}

	public static Optional<TextSelection> fromTextEditor(ITextEditor textEditor) {
		final Optional<IPath> pathOptional = EclipseUtils.getEclipsePath(textEditor);
		if (pathOptional.isEmpty()) {
			return Optional.empty();
		}
		if (textEditor.getSelectionProvider().getSelection() instanceof final ITextSelection textSelection) {
			return Optional.of(new TextSelection(
					pathOptional.get(),
					textSelection.getOffset(),
					textSelection.getLength(),
					textSelection.getText()));
		}
		return Optional.empty();
	}
}
