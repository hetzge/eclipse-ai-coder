package de.hetzge.eclipse.aicoder.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.ScratchpadStorage;
import de.hetzge.eclipse.aicoder.ScratchpadView;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class ScratchpadAddToScratchpadHandler extends AbstractHandler {

	public static final String ADD_PLAIN_COMMAND_ID = "de.hetzge.eclipse.aicoder.commands.scratchpadAddPlain";
	public static final String ADD_WITH_PATH_COMMAND_ID = "de.hetzge.eclipse.aicoder.commands.scratchpadAddWithPath";
	public static final String ADD_WITH_XML_TAG_COMMAND_ID = "de.hetzge.eclipse.aicoder.commands.scratchpadAddWithXmlTag";
	public static final String ADD_WITH_MARKDOWN_CODE_COMMAND_ID = "de.hetzge.eclipse.aicoder.commands.scratchpadAddWithMarkdownCode";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final AddMode mode = getMode(event);
		if (mode == null) {
			return null;
		}
		final List<ScratchpadItem> items = collectItems(event);
		if (items.isEmpty()) {
			return null;
		}
		final StringBuilder builder = new StringBuilder();
		for (final ScratchpadItem item : items) {
			try {
				appendItem(builder, item, mode);
			} catch (final IOException exception) {
				throw new ExecutionException("Failed to append item to scratchpad", exception);
			}
		}
		if (builder.length() > 0) {
			ScratchpadStorage.append(builder.toString().stripTrailing());
			try {
				ScratchpadView.openView();
			} catch (final Exception exception) {
				// View could not be opened, but the content was still added.
			}
		}
		return null;
	}

	private AddMode getMode(ExecutionEvent event) {
		if (event == null || event.getCommand() == null) {
			return null;
		}
		return switch (event.getCommand().getId()) {
		case ADD_PLAIN_COMMAND_ID -> AddMode.PLAIN;
		case ADD_WITH_PATH_COMMAND_ID -> AddMode.WITH_PATH;
		case ADD_WITH_XML_TAG_COMMAND_ID -> AddMode.WITH_XML_TAG;
		case ADD_WITH_MARKDOWN_CODE_COMMAND_ID -> AddMode.WITH_MARKDOWN_CODE;
		default -> null;
		};
	}

	private List<ScratchpadItem> collectItems(ExecutionEvent event) {
		final List<ScratchpadItem> items = new ArrayList<>();
		final Object currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof final IStructuredSelection structuredSelection) {
			for (final Object element : structuredSelection.toArray()) {
				if (element instanceof final ICompilationUnit unit) {
					final IResource resource = unit.getResource();
					if (resource instanceof final IFile file) {
						collectFile(items, file);
					}
				} else if (element instanceof final IFile file) {
					collectFile(items, file);
				} else {
					AiCoderActivator.log().info("Skipped unsupported selection type: " + element.getClass().getName());
				}
			}
			if (!items.isEmpty()) {
				return items;
			}
		} else if (currentSelection instanceof final ITextSelection textSelection) {
			final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
			collectTextSelection(items, activeEditor, textSelection);
			if (!items.isEmpty()) {
				return items;
			}
		}

		// Fallback: use selection of the active editor
		final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (activeEditor != null && activeEditor.getSite() != null && activeEditor.getSite().getSelectionProvider() != null && activeEditor.getSite().getSelectionProvider().getSelection() instanceof final ITextSelection textSelection) {
			collectTextSelection(items, activeEditor, textSelection);
		}
		return items;
	}

	private void collectFile(List<ScratchpadItem> items, IFile file) {
		try {
			final String content = readFileContent(file);
			items.add(new ScratchpadItem(true, getFileLocation(file), content));
		} catch (final CoreException exception) {
			// Ignore unreadable files
		}
	}

	private void collectTextSelection(List<ScratchpadItem> items, IEditorPart editorPart, ITextSelection textSelection) {
		if (textSelection == null || textSelection.getLength() <= 0) {
			return;
		}
		if (!(editorPart instanceof final ITextEditor textEditor)) {
			return;
		}
		String selectedText = textSelection.getText();
		if (selectedText == null || selectedText.isEmpty()) {
			final org.eclipse.jface.text.ITextViewer textViewer = EclipseUtils.getTextViewer(textEditor);
			if (textViewer != null) {
				selectedText = EclipseUtils.getSelectionText(textViewer);
			}
		}
		if (selectedText == null || selectedText.isEmpty()) {
			return;
		}
		items.add(new ScratchpadItem(false, getSourceLocation(textEditor), selectedText));
	}

	private String readFileContent(IFile file) throws CoreException {
		String charsetName = "UTF-8";
		try {
			charsetName = file.getCharset();
		} catch (final CoreException exception) {
			// Use default charset
		}
		Charset charset;
		try {
			charset = Charset.forName(charsetName);
		} catch (final Exception exception) {
			charset = StandardCharsets.UTF_8;
		}
		try (InputStream in = file.getContents()) {
			return new String(in.readAllBytes(), charset);
		} catch (final java.io.IOException exception) {
			throw new CoreException(org.eclipse.core.runtime.Status.error("Failed to read file: " + file.getName(), exception));
		}
	}

	private String getFileLocation(IFile file) {
		return stripLeadingSlash(file.getFullPath().toString());
	}

	private String getSourceLocation(ITextEditor textEditor) {
		final IEditorInput input = textEditor.getEditorInput();
		if (input != null) {
			final IFile file = input.getAdapter(IFile.class);
			if (file != null) {
				return getFileLocation(file);
			}
			final String name = input.getName();
			if (name != null && !name.isBlank()) {
				return name;
			}
		}
		return "";
	}

	private void appendItem(StringBuilder builder, ScratchpadItem item, AddMode mode) throws IOException {
		switch (mode) {
		case PLAIN -> builder.append(item.content()).append('\n');
		case WITH_PATH -> builder.append(item.path()).append('\n').append(item.content()).append('\n');
		case WITH_XML_TAG -> appendXmlTag(builder, item);
		case WITH_MARKDOWN_CODE -> appendMarkdownCode(builder, item);
		}
	}

	private void appendXmlTag(StringBuilder builder, ScratchpadItem item) throws IOException {
		if (item.isFile()) {
			builder.append("\n<FILE path=\"")
					.append(escapeXml(item.path()))
					.append("\">\n")
					.append(EclipseUtils.readContentFromBufferOrFile(ResourcesPlugin.getWorkspace().getRoot(), IPath.fromPortableString(item.path())))
					.append("\n</FILE>\n");
		} else {
			builder.append("\n<SNIPPET source=\"")
					.append(escapeXml(item.path()))
					.append("\">")
					.append('\n')
					.append(item.content())
					.append('\n')
					.append("</SNIPPET>\n");
		}
	}

	private void appendMarkdownCode(StringBuilder builder, ScratchpadItem item) {
		builder.append("\n```")
				.append(getFileExtension(item.path()))
				.append('\n')
				.append(item.content())
				.append('\n')
				.append("```\n");
	}

	private String getFileExtension(String path) {
		if (path == null) {
			return "";
		}
		final int separatorIndex = path.lastIndexOf('/');
		final int extensionIndex = path.lastIndexOf('.');
		return extensionIndex > separatorIndex && extensionIndex < path.length() - 1
				? path.substring(extensionIndex + 1)
				: "";
	}

	private static String stripLeadingSlash(String path) {
		return path != null && path.startsWith("/") ? path.substring(1) : path;
	}

	private static String escapeXml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private enum AddMode {
		PLAIN,
		WITH_PATH,
		WITH_XML_TAG,
		WITH_MARKDOWN_CODE
	}

	private record ScratchpadItem(boolean isFile, String path, String content) {
	}
}