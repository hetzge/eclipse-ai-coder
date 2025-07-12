package de.hetzge.eclipse.aicoder;

import java.util.Optional;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * https://stackoverflow.com/a/26901175/7662651
 * https://github.com/TabbyML/tabby/blob/main/clients/eclipse/plugin/src/com/tabbyml/tabby4eclipse/editor/WorkbenchPartListener.java
 * https://docs.continue.dev/autocomplete/context-selection
 */
public class AiCoderStartup implements IStartup {

	@Override
	public void earlyStartup() {
		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			final IWorkbenchPage activePage = window.getActivePage();
			activePage.addPartListener(new IPartListener2() {
				@Override
				public void partOpened(IWorkbenchPartReference partReference) {
					getTextEditor(partReference).ifPresent(InlineCompletionController::setup);
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference partReference) {
				}
			});
			for (final IEditorReference editorReference : activePage.getEditorReferences()) {
				final IEditorPart editorPart = editorReference.getEditor(false);
				if (editorPart instanceof final ITextEditor textEditor) {
					InlineCompletionController.setup(textEditor);
				}
			}
		}
	}

	private static Optional<ITextEditor> getTextEditor(IWorkbenchPartReference partReference) {
		if (partReference instanceof final IEditorReference editorReference) {
			final IEditorPart editor = editorReference.getEditor(false);
			if (editor instanceof final ITextEditor textEditor) {
				return Optional.of(textEditor);
			}
		}
		return Optional.empty();
	}
}
