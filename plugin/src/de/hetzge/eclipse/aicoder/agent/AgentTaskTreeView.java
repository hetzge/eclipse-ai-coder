package de.hetzge.eclipse.aicoder.agent;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.Utils;

public final class AgentTaskTreeView extends ViewPart {

	private final AgentTasksStateListener agentTasksStateListener;
	private TreeViewer treeViewer;

	public AgentTaskTreeView() {
		this.agentTasksStateListener = new AgentTasksStateListener();
	}

	@Override
	public void createPartControl(Composite parent) {
		this.treeViewer = new TreeViewer(parent);
		this.treeViewer.setUseHashlookup(true);
		this.treeViewer.setContentProvider(new AgentTaskContentProvider());
		this.treeViewer.setLabelProvider(new AgentTaskLabelProvider());
		this.treeViewer.addDoubleClickListener(event -> {
			final Object selected = event.getSelection();
			if (selected instanceof final ITreeSelection treeSelection) {
				if (treeSelection.getFirstElement() instanceof final AgentTask agentTask) {
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new AgentTrajectoryEditorInput(agentTask), AgentTrajectoryEditor.ID);
					} catch (final PartInitException exception) {
						AiCoderActivator.log().error("Failed to open agent task editor", exception);
						ErrorDialog.openError(getSite().getShell(), "Error", "Failed to open agent task editor", Status.error(exception.getMessage(), exception));
					}
				}
			}
		});
		AiCoderActivator.getDefault().getAgentTasksState().addListener(this.agentTasksStateListener);
		new Job("Refresh agent task tree") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!AgentTaskTreeView.this.treeViewer.getTree().isDisposed()) {
					refresh();
					this.schedule(1000 * 60);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
		AiCoderActivator.getDefault().getAgentTasksState().removeListener(this.agentTasksStateListener);
	}

	private void refresh() {
		Display.getDefault().asyncExec(() -> {
			AgentTaskTreeView.this.treeViewer.setInput(List.of());
			AgentTaskTreeView.this.treeViewer.refresh();
		});
	}

	private class AgentTasksStateListener implements AgentTasksState.AgentTasksStateListener {

		@Override
		public void onAgentTasksChanged(AgentTask task) {
			refresh();
		}
	}

	private class AgentTaskContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return AiCoderActivator.getDefault().getAgentTasksState().getAgentTasks().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private class AgentTaskLabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof final AgentTask agentTask) {
				if (agentTask.getStatus() == AgentStatus.RUNNING) {
					return AiCoderActivator.getImage(AiCoderImageKey.RUN_ICON);
				} else if (agentTask.getStatus() == AgentStatus.ERROR) {
					return AiCoderActivator.getImage(AiCoderImageKey.REJECT_ICON);
				} else if (agentTask.getStatus() == AgentStatus.SUCCESS) {
					return AiCoderActivator.getImage(AiCoderImageKey.ACCEPT_ICON);
				}
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof final AgentTask agentTask) {
				return String.format("[%s] %s - %s", agentTask.getStatus(), agentTask.getTitle(), Utils.formatRelativeTime(agentTask.getCreationTime()));
			}
			return null;
		}

	}
}
