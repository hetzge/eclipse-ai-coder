package de.hetzge.eclipse.aicoder.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SwtCallable;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.AbstractMultiEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

public class EclipseUtils {

	public static Optional<String> getFilename(IEditorInput editorInput) {
		if (editorInput instanceof final FileEditorInput fileEditorInput) {
			final IPath location = fileEditorInput.getFile().getLocation();
			if (location == null) {
				return Optional.of(fileEditorInput.getFile().getName());
			}
			return Optional.of(fileEditorInput.getFile().getLocation().toPath().toString());
		}
		return Optional.empty();
	}

	public static Optional<IWorkbenchPage> getActiveWorkbenchPage() {
		return syncCall(() -> {
			return Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow())
					.map(IWorkbenchWindow::getActivePage);
		});
	}

	public static Optional<AbstractTextEditor> getActiveTextEditor() {
		return getActiveWorkbenchPage()
				.map(it -> it.getActivePart())
				.map(it -> getActiveTextEditor(it));
	}

	private static AbstractTextEditor getActiveTextEditor(IWorkbenchPart part) {
		if (part instanceof AbstractTextEditor) {
			return (AbstractTextEditor) part;
		} else if ((part instanceof AbstractMultiEditor) && ((AbstractMultiEditor) part).getActiveEditor() instanceof AbstractTextEditor) {
			return (AbstractTextEditor) ((AbstractMultiEditor) part).getActiveEditor();
		} else if ((part instanceof MultiPageEditorPart) && ((MultiPageEditorPart) part).getSelectedPage() instanceof AbstractTextEditor) {
			return (AbstractTextEditor) ((MultiPageEditorPart) part).getSelectedPage();
		}
		return part != null ? part.getAdapter(AbstractTextEditor.class) : null;
	}

	public static Optional<IEditorPart> getActiveEditor() {
		return syncCall(() -> getActiveWorkbenchPage().map(IWorkbenchPage::getActiveEditor));
	}

	public static boolean isActiveEditor(ITextEditor textEditor) {
		return EclipseUtils.getActiveTextEditor().stream().anyMatch(it -> Objects.equals(it, textEditor));
	}

	public static boolean isActiveEditor(IEditorPart editor) {
		return EclipseUtils.getActiveEditor().stream().anyMatch(it -> Objects.equals(it, editor));
	}

	public static ITextViewer getTextViewer(ITextEditor textEditor) {
		return textEditor.getAdapter(ITextViewer.class);
	}

	public static StyledText getStyledTextWidget(ITextEditor textEditor) {
		return getTextViewer(textEditor).getTextWidget();
	}

	public static Display getDisplay(ITextEditor textEditor) {
		return getStyledTextWidget(textEditor).getDisplay();
	}

	public static IContextService getContextService(ITextEditor textEditor) {
		return textEditor.getSite().getService(IContextService.class);
	}

	public static void asyncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	}

	public static void asyncExec(ITextEditor textEditor, Runnable runnable) {
		getDisplay(textEditor).asyncExec(runnable);
	}

	public static void syncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
	}

	public static void syncExec(ITextEditor textEditor, Runnable runnable) {
		getDisplay(textEditor).syncExec(runnable);
	}

	public static <T, E extends Exception> T syncCall(SwtCallable<T, E> callable) throws E {
		return PlatformUI.getWorkbench().getDisplay().syncCall(callable);
	}

	public static <T, E extends Exception> T syncCall(ITextEditor textEditor, SwtCallable<T, E> callable) throws E {
		return getDisplay(textEditor).syncCall(callable);
	}

	public static int getCurrentOffsetInDocument(ITextEditor textEditor) throws IllegalStateException {
		return syncCall(textEditor, () -> {
			int offset = -1;
			final ISelection selection = textEditor.getSelectionProvider().getSelection();
			if (selection instanceof final ITextSelection textSelection) {
				offset = textSelection.getOffset();
			}
			if (offset != -1) {
				return offset;
			}
			final int offsetInWidget = getStyledTextWidget(textEditor).getCaretOffset();
			final ITextViewer textViewer = getTextViewer(textEditor);
			if (textViewer instanceof final ITextViewerExtension5 textViewerExt) {
				offset = textViewerExt.widgetOffset2ModelOffset(offsetInWidget);
			}
			if (offset != -1) {
				return offset;
			}
			throw new IllegalStateException("Failed to get current offset in document.");
		});
	}

	public static Optional<ICompilationUnit> getCompilationUnit(IEditorInput editorInput) {
		IJavaElement javaElement = null;
		if (editorInput instanceof IAdaptable) {
			javaElement = editorInput.getAdapter(IJavaElement.class);
		}
		if (javaElement == null) {
			javaElement = JavaUI.getEditorInputJavaElement(editorInput);
		}
		if (javaElement instanceof final ICompilationUnit compilationUnit) {
			return Optional.of(compilationUnit);
		} else if (javaElement instanceof final IType type) {
			return Optional.of(type.getCompilationUnit());
		} else if (javaElement instanceof final IOrdinaryClassFile classFile) {
			return Optional.ofNullable(classFile.getType().getCompilationUnit());
		} else if (editorInput instanceof final FileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IJavaElement element = JavaCore.create(file);
			if (element instanceof final ICompilationUnit compilationUnit) {
				return Optional.of(compilationUnit);
			}
		}
		return Optional.empty();
	}

	public static String getJavadoc(final IJavaElement element) throws CoreException {
		return JavadocContentAccess2.getHTMLContent(element, true);
	}

	public static Optional<IDocument> getDocumentForEditor(Object input) {
		if (!(input instanceof IEditorInput)) {
			return Optional.empty();
		}
		return syncCall(() -> getActiveWorkbenchPage()
				.map(page -> page.findEditor((IEditorInput) input))
				.filter(ITextEditor.class::isInstance)
				.map(ITextEditor.class::cast)
				.map(EclipseUtils::getTextViewer)
				.map(ITextViewer::getDocument));
	}

	public static Optional<IFile> getFileForEditor(Object input) {
		return Optional.ofNullable(input)
				.filter(IFileEditorInput.class::isInstance)
				.map(IFileEditorInput.class::cast)
				.map(IFileEditorInput::getFile);
	}

	public static IProject getProject(IEditorInput input) {
		IProject project = input.getAdapter(IProject.class);
		if (project == null) {
			final IResource resource = input.getAdapter(IResource.class);
			if (resource != null) {
				project = resource.getProject();
			}
		}
		return project;
	}

	public static Optional<String> stringFromInput(IEditorInput input, IWorkbenchPart part) throws IOException, CoreException {
		if (part instanceof ITextEditor) {
			final IDocument doc = ((ITextEditor) part).getDocumentProvider().getDocument(input);
			return doc != null ? Optional.of(doc.get()) : Optional.empty();
		}
		if (input instanceof IFileEditorInput) {
			final IFile file = ((IFileEditorInput) input).getFile();
			if (file.exists()) {
				try (InputStream in = file.getContents()) {
					return Optional.of(new String(in.readAllBytes(), file.getCharset()));
				}
			}
		}
		if (input instanceof FileStoreEditorInput) {
			final URI uri = ((FileStoreEditorInput) input).getURI();
			try (InputStream in = uri.toURL().openStream()) {
				return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return Optional.empty();
	}

	public static String getFileExtension(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return ((IFileEditorInput) input).getFile().getFileExtension();
		}
		return getFilename(input).map(name -> {
			final int index = name.lastIndexOf('.');
			return index == -1 ? null : name.substring(index + 1);
		}).orElse("");
	}

	public static int getWidgetLine(ITextViewer textViewer, int modelOffset) throws BadLocationException {
		if (textViewer instanceof final ITextViewerExtension5 extension5) {
			return extension5.modelLine2WidgetLine(textViewer.getDocument().getLineOfOffset(modelOffset));
		} else {
			return textViewer.getDocument().getLineOfOffset(modelOffset);
		}
	}

	public static int getWidgetOffset(ITextViewer textViewer, int modelOffset) {
		if (textViewer instanceof final ITextViewerExtension5 extension5) {
			return extension5.modelOffset2WidgetOffset(modelOffset);
		} else {
			return modelOffset;
		}
	}

	public static boolean hasSelection(ITextViewer textViewer) {
		return Display.getDefault().syncCall(() -> textViewer.getSelectedRange().y > 0);
	}

	public static String getSelectionText(ITextViewer textViewer) {
		return Display.getDefault().syncCall(() -> textViewer.getSelectionProvider().getSelection() instanceof final ITextSelection textSelection ? textSelection.getText() : "");
	}

	public static Optional<Path> getPath(final ITextEditor editor) {
		final IFile file = editor.getEditorInput().getAdapter(IFile.class);
		return Optional.ofNullable(file).map(IFile::getLocation).map(IPath::toFile).map(File::toPath);
	}

	public static Optional<IPath> getEclipsePath(ITextEditor editor) {
		final IFile file = editor.getEditorInput().getAdapter(IFile.class);
		return Optional.ofNullable(file).map(IFile::getFullPath);
	}
}
