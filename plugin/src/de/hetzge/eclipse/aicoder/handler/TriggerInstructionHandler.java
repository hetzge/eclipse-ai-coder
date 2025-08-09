package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.inline.InstructionPopupDialog;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class TriggerInstructionHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute trigger instruction handler");
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		final InstructionPopupDialog instructionPopupDialog = new InstructionPopupDialog(Display.getDefault().getActiveShell(), (instruction, llmModelOption) -> {
			AiCoderPreferences.setEditLlmModelOption(llmModelOption);
			InlineCompletionController.setup(textEditor).trigger(instruction.content());
		});
		instructionPopupDialog.open();
		return null;
	}
}
