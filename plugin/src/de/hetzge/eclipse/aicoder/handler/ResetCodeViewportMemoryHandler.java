package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class ResetCodeViewportMemoryHandler extends AbstractHandler {

	public static final String COMMAND_ID = "de.hetzge.eclipse.codestral.commands.resetCodeViewportMemory";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.getDefault().getEditorViewMemory().clear();
		MessageDialog.openInformation(
				null,
				"AI Coder",
				"Code viewport memory has been reset");
		return null;
	}
}
