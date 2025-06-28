package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.aicoder.AiCoderHistoryView;
import de.hetzge.eclipse.aicoder.EclipseUtils;
import de.hetzge.eclipse.aicoder.InlineCompletionController;

public class DismissHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final boolean aborted = InlineCompletionController.setup(EclipseUtils.getActiveTextEditor()).abort();
		if (aborted) {
			AiCoderHistoryView.get().ifPresent(view -> {
				view.setLatestRejected();
			});
		}
		return null;
	}

}
