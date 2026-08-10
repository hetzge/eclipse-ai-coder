package de.hetzge.eclipse.aicoder.handler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

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
		Optional<IResource> selectedResourceOptional = EclipseUtils.getCurrentSelectedResource();
		Optional<AbstractTextEditor> activeTextEditorOptional = EclipseUtils.getActiveTextEditor();
		final TextSelection textSelection = selectedResourceOptional.map(resource -> new TextSelection(resource.getFullPath(), 0, 0, ""))
				.or(() -> activeTextEditorOptional.flatMap(TextSelection::fromTextEditor))
				.orElseThrow(() -> new ExecutionException("No text selection"));
		final IProject project = selectedResourceOptional.map(IResource::getProject)
				.or(() -> activeTextEditorOptional.map(textEditor -> (IProject) textEditor.getEditorInput().getAdapter(IFile.class).getProject()))
				.orElseThrow(() -> new ExecutionException("No project"));
		final List<EditInstruction> instructions = InstructionUtils.resolve(textSelection.path().toPath());
		final EditInstruction lastInstruction = instructionStorage.getLastInstruction();
		final CompletionMode completionMode = CompletionMode.AGENT;
		this.instructionPopupDialog = new InstructionPopupDialog(Display.getDefault().getActiveShell(), completionMode, completionMode, instructions, lastInstruction.content(), selection -> {
			try {
				instructionStorage.addEditInstruction(selection.instruction().content());
			} catch (final IOException exception) {
				AiCoderActivator.log().error("Failed to store instruction.", exception);
			}
			AiCoderPreferences.setLlmModelOption(completionMode, selection.llmModelOption());
			try {
				final AgentRequest agentRequest = new AgentRequest(List.of(project), selection.llmModelOption(), textSelection, selection.instruction().content(), selection.readonly());
				AiCoderActivator.getDefault().getAgentService().execute(agentRequest);
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(AgentTaskTreeView.ID);
			} catch (final IOException | PartInitException exception) {
				AiCoderActivator.log().error("Failed to start agent", exception);
				AiCoderActivator.openErrorDialog("Failed to start agent", exception.getMessage(), exception);
			}
		}, () -> EclipseUtils.getActiveEditor().ifPresent(editor -> editor.setFocus()));
		this.instructionPopupDialog.open();
		return null;
	}

}