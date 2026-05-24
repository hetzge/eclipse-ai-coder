package de.hetzge.eclipse.aicoder.copy;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

final class AiCoderPartListener implements IPartListener2 {
	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		Display.getDefault().asyncExec(() -> {
			// delayed because the editor is not yet ready
			AiCoderActivator.getDefault().getEditorViewMemory().update(partRef.getPage().getActiveEditor().getAdapter(ITextEditor.class));
		});
	}
}