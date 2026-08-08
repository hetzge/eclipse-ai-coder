package de.hetzge.eclipse.aicoder.agent;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.history.HistoryType;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.inline.Suggestion;
import de.hetzge.eclipse.aicoder.tool.FileSystem;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public final class AgentTaskTreeView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTaskTreeView";

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
			try {
				final Object selected = event.getSelection();
				if (selected instanceof final ITreeSelection treeSelection) {
					if (treeSelection.getFirstElement() instanceof final AgentTask agentTask) {
						try {
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new AgentTrajectoryEditorInput(agentTask), AgentTrajectoryEditor.ID);
						} catch (final PartInitException exception) {
							AiCoderActivator.log().error("Failed to open agent task editor", exception);
							ErrorDialog.openError(getSite().getShell(), "Error", "Failed to open agent task editor", Status.error(exception.getMessage(), exception));
						}
					} else if (treeSelection.getFirstElement() instanceof final AgentChange agentChange) {
						final AgentTask task = findTask(agentChange).orElseThrow(() -> new IllegalStateException("No task found for change: " + agentChange.path().toPortableString()));
						final List<IProject> projects = task.getRequest().projects();
						final FileSystem fileSystem = new FileSystem(projects, projects.get(0).getWorkspace().getRoot());
						fileSystem.load(AgentStorage.getFileSystemPath(task.getId()).toPath());
						final List<Suggestion> suggestions = fileSystem.toSuggestions(agentChange.path(), HistoryType.AGENT);
						final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(agentChange.path());
						IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
						final InlineCompletionController controller = InlineCompletionController.setup(EclipseUtils.getActiveTextEditor().orElseThrow());
						controller.setup(suggestions);
					}
				}
			} catch (final Exception exception) {
				AiCoderActivator.log().error("Failed to open agent task editor", exception);
				ErrorDialog.openError(getSite().getShell(), "Error", "Failed to open agent task editor", Status.error(exception.getMessage(), exception));
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

	private Optional<AgentTask> findTask(AgentChange agentChange) {
		return AiCoderActivator.getDefault().getAgentTasksState().getAgentTasks().stream()
				.filter(it -> it.getChanges().contains(agentChange))
				.findFirst();
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
			if (parentElement instanceof final AgentTask agentTask) {
				return agentTask.getChanges().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof final AgentTask agentTask) {
				return !agentTask.getChanges().isEmpty();
			}
			return false;
		}
	}

	private class AgentTaskLabelProvider implements ILabelProvider {

		private final WorkbenchLabelProvider resourceLabels;

		private AgentTaskLabelProvider() {
			this.resourceLabels = new WorkbenchLabelProvider();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
			this.resourceLabels.dispose();
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
			if (element instanceof final AgentChange agentChange) {
				return this.resourceLabels.getImage(ResourcesPlugin.getWorkspace().getRoot().getFile(agentChange.path()));
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof final AgentTask agentTask) {
				return String.format("[%s] %s - %s", agentTask.getStatus(), agentTask.getTitle(), Utils.formatRelativeTime(agentTask.getCreationTime()));
			} else if (element instanceof final AgentChange agentChange) {
				return String.format("%s [%s, +%d/-%d]",
						this.resourceLabels.getText(ResourcesPlugin.getWorkspace().getRoot().getFile(agentChange.path())),
						agentChange.type(),
						agentChange.linesAdded(),
						agentChange.linesRemoved());
			}
			return null;
		}
	}
}
