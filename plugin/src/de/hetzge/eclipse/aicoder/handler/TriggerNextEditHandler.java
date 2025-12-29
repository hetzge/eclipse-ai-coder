package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class TriggerNextEditHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute trigger next edit handler");
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		try {
			InlineCompletionController.setup(textEditor).triggerNextEdit();
		} catch (final BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}