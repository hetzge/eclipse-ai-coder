package de.hetzge.eclipse.aicoder.handler;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.content.InstructionStorage;
import de.hetzge.eclipse.aicoder.content.InstructionUtils;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.inline.InstructionPopupDialog;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class TriggerInstructionHandler extends AbstractHandler {

	private InstructionPopupDialog instructionPopupDialog;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute trigger instruction handler");
		if (EclipseUtils.isDialogActive(this.instructionPopupDialog)) {
			this.instructionPopupDialog.toggleQueryMode();
			return null;
		}
		final InstructionStorage instructionStorage = AiCoderActivator.getDefault().getInstructionStorage();
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		final List<EditInstruction> instructions = InstructionUtils.resolve(EclipseUtils.getPath(textEditor).orElse(null));
		final EditInstruction lastInstruction = instructionStorage.getLastInstruction();
		final CompletionMode editCompletionMode = CompletionMode.getMode(EclipseUtils.getTextViewer(textEditor), "dummy", false, false);
		final CompletionMode readOnlyCompletionMode = CompletionMode.getMode(EclipseUtils.getTextViewer(textEditor), "dummy", false, true);
		this.instructionPopupDialog = new InstructionPopupDialog(Display.getDefault().getActiveShell(), editCompletionMode, readOnlyCompletionMode, instructions, lastInstruction.content(), (selection) -> {
			try {
				instructionStorage.addEditInstruction(selection.instruction().content());
			} catch (final IOException exception) {
				AiCoderActivator.log().error("Failed to store instruction.", exception);
			}
			AiCoderPreferences.setLlmModelOption(selection.readonly() ? readOnlyCompletionMode : editCompletionMode, selection.llmModelOption());
			InlineCompletionController.setup(textEditor).trigger(selection.instruction().content(), selection.readonly());
		}, () -> textEditor.setFocus());
		this.instructionPopupDialog.open();
		return null;
	}
}
