package de.hetzge.eclipse.aicoder;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SwtCallable;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public class EclipseUtils {

	public static Optional<String> getFilename(IEditorInput editorInput) {
		if (editorInput instanceof final FileEditorInput fileEditorInput) {
			return Optional.of(fileEditorInput.getFile().getLocation().toPath().toString());
		}
		return Optional.empty();
	}

	public static IWorkbenchPage getActiveWorkbenchPage() {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			final IWorkbenchPage page = window.getActivePage();
			return page;
		}
		return null;
	}

	public static ITextEditor getActiveTextEditor() {
		final IWorkbenchPage page = getActiveWorkbenchPage();
		if (page != null) {
			final IEditorPart activeEditor = page.getActiveEditor();
			if (activeEditor instanceof final ITextEditor textEditor) {
				return textEditor;
			}
		}
		return null;
	}

	public static boolean isActiveEditor(ITextEditor textEditor) {
		return textEditor == EclipseUtils.getActiveTextEditor();
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

	public static String getSelectedText() {
		final ITextEditor editor = getActiveTextEditor();
		if (editor != null) {
			return getSelectedText(editor);
		}
		return null;
	}

	public static String getSelectedText(ITextEditor textEditor) {
		final ISelection selection = textEditor.getSelectionProvider().getSelection();
		if (selection instanceof final ITextSelection textSelection) {
			return textSelection.getText();
		}
		return null;
	}

	public static Optional<ICompilationUnit> getCompilationUnit(IEditorInput editorInput) {
		if (editorInput instanceof final FileEditorInput fileEditorInput) {
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
}
