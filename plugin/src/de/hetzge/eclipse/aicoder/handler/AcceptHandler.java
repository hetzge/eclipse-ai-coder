package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.EclipseUtils;
import de.hetzge.eclipse.aicoder.InlineCompletionController;

public class AcceptHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute accept handler");
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor();
		final InlineCompletionController controller = InlineCompletionController.setup(textEditor);
		try {
			controller.accept();
		} catch (final CoreException exception) {
			throw new ExecutionException("Failed to accept inline completion", exception);
		}
		return null;
	}

}
