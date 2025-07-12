package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

import de.hetzge.eclipse.aicoder.AiCoderPreferences;

public class ToggleMultilineHandler extends AbstractHandler {

	public static final String COMMAND_ID = "de.hetzge.eclipse.codestral.commands.toggleMultiline";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Command command = event.getCommand();
		final boolean oldValue = HandlerUtil.toggleCommandState(command);
		AiCoderPreferences.setMultilineEnabled(!oldValue);
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		initializeStateFromPreference();
		super.setEnabled(evaluationContext);
	}

	private void initializeStateFromPreference() {
		final ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		final Command command = commandService.getCommand(COMMAND_ID);
		if (command == null) {
			return;
		}
		final State state = command.getState(RegistryToggleState.STATE_ID);
		if (state == null) {
			return;
		}
		state.setValue(AiCoderPreferences.isMultilineEnabled());
	}
}
