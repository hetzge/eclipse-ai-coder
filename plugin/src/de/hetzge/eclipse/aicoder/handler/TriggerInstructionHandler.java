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
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute trigger instruction handler");
		final InstructionStorage instructionStorage = AiCoderActivator.getDefault().getInstructionStorage();
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		final List<EditInstruction> instructions = InstructionUtils.resolve(EclipseUtils.getPath(textEditor).orElse(null));
		final EditInstruction lastInstruction = instructionStorage.getLastInstruction();
		final CompletionMode mode = CompletionMode.getMode(EclipseUtils.getTextViewer(textEditor), "dummy", true); // TODO !!!!!!!!!!!!!
		final InstructionPopupDialog instructionPopupDialog = new InstructionPopupDialog(Display.getDefault().getActiveShell(), mode, instructions, lastInstruction.content(), (instruction, llmModelOption) -> {
			try {
				instructionStorage.addEditInstruction(instruction.content());
			} catch (final IOException exception) {
				AiCoderActivator.log().error("Failed to store instruction.", exception);
			}
			AiCoderPreferences.setLlmModelOption(mode, llmModelOption);

			// TODO
			InlineCompletionController.setup(textEditor).trigger(instruction.content());

//			if (EclipseUtils.getActiveTextEditor().get().getEditorInput() instanceof final IFileEditorInput fileEditorInput) {
//				try {
//					final IFile file = fileEditorInput.getFile();
//					final IProject project = file.getProject();
//					final TextSelection textSelection = TextSelection.fromTextEditor(textEditor).orElseThrow(() -> new ExecutionException("No text selection"));
//					final AgentRequest agentRequest = new AgentRequest(List.of(project), llmModelOption, textSelection, instruction.content());
//					AiCoderActivator.getDefault().getAgentService().execute(agentRequest);
//					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(AgentTaskTreeView.ID);
//				} catch (final IOException | ExecutionException | PartInitException e) {
//					e.printStackTrace(); // TODO
//				}
//			}
		}, () -> textEditor.setFocus());
		instructionPopupDialog.open();
		return null;
	}
}
