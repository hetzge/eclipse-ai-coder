package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.EclipseUtils;
import de.hetzge.eclipse.aicoder.InlineCompletionController;

public class TriggerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.log().info("Execute trigger handler");
		InlineCompletionController.setup(EclipseUtils.getActiveTextEditor()).trigger();
		return null;
	}

}
