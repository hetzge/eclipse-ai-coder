package de.hetzge.eclipse.aicoder.handler;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.agent.AgentRequest;
import de.hetzge.eclipse.aicoder.agent.AgentTaskTreeView;
import de.hetzge.eclipse.aicoder.base.TextSelection;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.content.InstructionStorage;
import de.hetzge.eclipse.aicoder.content.InstructionUtils;
import de.hetzge.eclipse.aicoder.inline.InstructionPopupDialog;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class StartAgentHandler extends AbstractHandler {

	private InstructionPopupDialog instructionPopupDialog;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Start agent handler");
		if (EclipseUtils.isDialogActive(this.instructionPopupDialog)) {
			this.instructionPopupDialog.toggleQueryMode();
			return null;
		}
		final InstructionStorage instructionStorage = AiCoderActivator.getDefault().getInstructionStorage();
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		final List<EditInstruction> instructions = InstructionUtils.resolve(EclipseUtils.getPath(textEditor).orElse(null));
		final EditInstruction lastInstruction = instructionStorage.getLastInstruction();
		final CompletionMode completionMode = CompletionMode.AGENT;
		this.instructionPopupDialog = new InstructionPopupDialog(Display.getDefault().getActiveShell(), completionMode, completionMode, instructions, lastInstruction.content(), selection -> {
			try {
				instructionStorage.addEditInstruction(selection.instruction().content());
			} catch (final IOException exception) {
				AiCoderActivator.log().error("Failed to store instruction.", exception);
			}
			AiCoderPreferences.setLlmModelOption(completionMode, selection.llmModelOption());
			if (EclipseUtils.getActiveTextEditor().get().getEditorInput() instanceof final IFileEditorInput fileEditorInput) {
				try {
					final IFile file = fileEditorInput.getFile();
					final IProject project = file.getProject();
					final TextSelection textSelection = TextSelection.fromTextEditor(textEditor).orElseThrow(() -> new ExecutionException("No text selection"));
					final AgentRequest agentRequest = new AgentRequest(List.of(project), selection.llmModelOption(), textSelection, selection.instruction().content(), selection.readonly());
					AiCoderActivator.getDefault().getAgentService().execute(agentRequest);
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(AgentTaskTreeView.ID);
				} catch (final IOException | ExecutionException | PartInitException exception) {
					AiCoderActivator.log().error("Failed to start agent", exception);
					AiCoderActivator.openErrorDialog("Failed to start agent", exception.getMessage(), exception);
				}
			}
		}, () -> textEditor.setFocus());
		this.instructionPopupDialog.open();
		return null;
	}

}