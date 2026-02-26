package de.hetzge.eclipse.aicoder;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

final class AiCoderWindowListener implements IWindowListener {
	@Override
	public void windowOpened(IWorkbenchWindow window) {
		final IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			page.addPartListener(new AiCoderPartListener());
		}
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}
}