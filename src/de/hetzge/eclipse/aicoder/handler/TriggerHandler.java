package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.aicoder.EditorUtils;
import de.hetzge.eclipse.aicoder.InlineCompletionController;

public class TriggerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Trigger.execute()");
		InlineCompletionController.setup(EditorUtils.getActiveTextEditor()).trigger();
		return null;
	}

}
