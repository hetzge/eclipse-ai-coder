package de.hetzge.eclipse.aicoder.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.AiCoderResultView;
import de.hetzge.eclipse.aicoder.handler.AbortAgentTaskHandler;
import de.hetzge.eclipse.aicoder.handler.AbortAllAgentTasksHandler;
import de.hetzge.eclipse.aicoder.history.HistoryType;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.inline.Suggestion;
import de.hetzge.eclipse.aicoder.tool.FileSystem;
import de.hetzge.eclipse.aicoder.util.DiffUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public final class AgentTaskTreeView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTaskTreeView";

	private final AgentTasksStateListener agentTasksStateListener;
	private TreeViewer treeViewer;
	private Action abortTaskAction;
	private Action abortAllTasksAction;

	public AgentTaskTreeView() {
		this.agentTasksStateListener = new AgentTasksStateListener();
	}

	@Override
	public void createPartControl(Composite parent) {
		this.treeViewer = new TreeViewer(parent);
		this.treeViewer.setUseHashlookup(true);
		this.treeViewer.setContentProvider(new AgentTaskContentProvider());
		this.treeViewer.setLabelProvider(new AgentTaskLabelProvider());
		getSite().setSelectionProvider(this.treeViewer);
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
						openChange(agentChange);
					}
				}
			} catch (final Exception exception) {
				AiCoderActivator.log().error("Failed to open agent task editor", exception);
				ErrorDialog.openError(getSite().getShell(), "Error", "Failed to open agent task editor", Status.error(exception.getMessage(), exception));
			}
		});
		this.treeViewer.addSelectionChangedListener(event -> updateActionEnablement());
		createActions();
		contributeToActionBars();
		hookContextMenu();
		updateActionEnablement();
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

	Optional<AgentTask> findTask(AgentChange agentChange) {
		return AiCoderActivator.getDefault().getAgentTasksState().getAgentTasks().stream()
				.filter(it -> it.getChanges().contains(agentChange))
				.findFirst();
	}

	public List<AgentTask> getSelectedAgentTasks() {
		final Set<AgentTask> tasks = new LinkedHashSet<>();
		if (this.treeViewer.getSelection() instanceof final IStructuredSelection structuredSelection) {
			for (final Object element : structuredSelection) {
				if (element instanceof final AgentTask agentTask) {
					tasks.add(agentTask);
				} else if (element instanceof final AgentChange agentChange) {
					findTask(agentChange).ifPresent(tasks::add);
				}
			}
		}
		return List.copyOf(tasks);
	}

	private void createActions() {
		this.abortTaskAction = new Action("Abort Task(s)") {
			@Override
			public void run() {
				EclipseUtils.executeCommand(AbortAgentTaskHandler.COMMAND_ID);
			}
		};
		this.abortTaskAction.setToolTipText("Abort the selected agent task(s)");
		this.abortTaskAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.REJECT_ICON));

		this.abortAllTasksAction = new Action("Abort All Tasks") {
			@Override
			public void run() {
				EclipseUtils.executeCommand(AbortAllAgentTasksHandler.COMMAND_ID);
			}
		};
		this.abortAllTasksAction.setToolTipText("Abort all running agent tasks");
		this.abortAllTasksAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.REJECT_ICON));
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(this.abortTaskAction);
		manager.add(this.abortAllTasksAction);
	}

	private void hookContextMenu() {
		final MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(manager -> fillContextMenu(manager));
		final Menu menu = menuManager.createContextMenu(this.treeViewer.getControl());
		this.treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, this.treeViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		final Action abortTaskAction = new Action("Abort Task(s)") {
			@Override
			public void run() {
				EclipseUtils.executeCommand(AbortAgentTaskHandler.COMMAND_ID);
			}
		};
		abortTaskAction.setEnabled(hasRunningSelectedTasks());
		manager.add(abortTaskAction);
		final Action abortAllTasksAction = new Action("Abort All Tasks") {
			@Override
			public void run() {
				EclipseUtils.executeCommand(AbortAllAgentTasksHandler.COMMAND_ID);
			}
		};
		abortAllTasksAction.setEnabled(hasRunningTasks());
		manager.add(abortAllTasksAction);
		manager.add(new Separator());
		if (this.treeViewer.getSelection() instanceof final IStructuredSelection selection) {
			final Object firstElement = selection.getFirstElement();
			if (firstElement instanceof final AgentTask agentTask) {
				fillTaskContextMenu(manager);
			} else if (firstElement instanceof final AgentChange agentChange) {
				fillFileContextMenu(manager);
			}
		}
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillTaskContextMenu(IMenuManager manager) {
		final List<AgentTask> tasks = getSelectedAgentTasks();
		final Action applyAllAction = new Action("Apply all changes") {
			@Override
			public void run() {
				applyTaskChanges(tasks, false);
			}
		};
		applyAllAction.setEnabled(!tasks.isEmpty());
		manager.add(applyAllAction);
		final Action resetAllAction = new Action("Reset all to reference") {
			@Override
			public void run() {
				applyTaskChanges(tasks, true);
			}
		};
		resetAllAction.setEnabled(!tasks.isEmpty());
		manager.add(resetAllAction);
		final Action openInResultViewAction = new Action("Open in result view") {
			@Override
			public void run() {
				try {
					final Optional<String> resultOptional = AiCoderActivator.getDefault().getAgentTasksState().loadAgentResultMessage(tasks.getLast().getId());
					if (resultOptional.isPresent()) {
						final String result = resultOptional.get();
						try {
							AiCoderResultView.open(result);
						} catch (final PartInitException exception) {
							logAndShowError("Failed to open result view", exception);
						}
					} else {
						ErrorDialog.openError(getSite().getShell(), "Error", "No result found", Status.error("No result found"));
					}
				} catch (final IOException exception) {
					logAndShowError("Failed to load trajectory", exception);
				}
			}
		};
		manager.add(openInResultViewAction);
	}

	private void fillFileContextMenu(IMenuManager manager) {
		final List<AgentChange> changes = getSelectedAgentChanges();
		final Action applyAction = new Action("Apply changes") {
			@Override
			public void run() {
				applyChanges(changes, false);
			}
		};
		applyAction.setEnabled(!changes.isEmpty());
		manager.add(applyAction);
		final Action resetAction = new Action("Reset to reference") {
			@Override
			public void run() {
				applyChanges(changes, true);
			}
		};
		resetAction.setEnabled(!changes.isEmpty());
		manager.add(resetAction);
		manager.add(new Separator());
		if (!changes.isEmpty()) {
			final AgentChange change = changes.get(0);
			final Action openEditorAction = new Action("Open in editor") {
				@Override
				public void run() {
					openChange(change);
				}
			};
			manager.add(openEditorAction);
			final Action compareWorktreeAction = new Action("Open compare dialog (worktree)") {
				@Override
				public void run() {
					openCompareDialog(change, false);
				}
			};
			manager.add(compareWorktreeAction);
			final Action compareReferenceAction = new Action("Open compare dialog (reference)") {
				@Override
				public void run() {
					openCompareDialog(change, true);
				}
			};
			manager.add(compareReferenceAction);
		}
	}

	private List<AgentChange> getSelectedAgentChanges() {
		final List<AgentChange> changes = new ArrayList<>();
		if (this.treeViewer.getSelection() instanceof final IStructuredSelection structuredSelection) {
			for (final Object element : structuredSelection) {
				if (element instanceof final AgentChange agentChange) {
					changes.add(agentChange);
				}
			}
		}
		return changes;
	}

	private FileSystem loadFileSystem(AgentTask task) throws IOException {
		final List<IProject> projects = task.getRequest().projects();
		final FileSystem fileSystem = new FileSystem(projects, projects.get(0).getWorkspace().getRoot());
		fileSystem.load(AgentStorage.getFileSystemPath(task.getId()).toPath());
		return fileSystem;
	}

	private void applyTaskChanges(List<AgentTask> tasks, boolean reset) {
		for (final AgentTask task : tasks) {
			try {
				final FileSystem fileSystem = loadFileSystem(task);
				applyFileSystemChanges(fileSystem, new ArrayList<>(fileSystem.getChangedPaths()), reset);
			} catch (final Exception exception) {
				logAndShowError("Failed to " + (reset ? "reset" : "apply") + " changes", exception);
			}
		}
	}

	private void applyChanges(List<AgentChange> changes, boolean reset) {
		final Map<AgentTask, List<IPath>> pathsByTask = new LinkedHashMap<>();
		for (final AgentChange change : changes) {
			findTask(change).ifPresent(task -> pathsByTask.computeIfAbsent(task, key -> new ArrayList<>()).add(change.path()));
		}
		for (final Map.Entry<AgentTask, List<IPath>> entry : pathsByTask.entrySet()) {
			try {
				final FileSystem fileSystem = loadFileSystem(entry.getKey());
				applyFileSystemChanges(fileSystem, entry.getValue(), reset);
			} catch (final Exception exception) {
				logAndShowError("Failed to " + (reset ? "reset" : "apply") + " changes", exception);
			}
		}
	}

	private void applyFileSystemChanges(FileSystem fileSystem, List<IPath> paths, boolean reset) throws CoreException {
		ResourcesPlugin.getWorkspace().run(monitor -> {
			for (final IPath path : paths) {
				final String content = reset ? fileSystem.getReferenceContent(path) : fileSystem.getChangedContent(path);
				writeFileContent(path, content);
			}
		}, null);
	}

	private void writeFileContent(IPath path, String content) throws CoreException {
		final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		if (content.isBlank()) {
			if (file.exists()) {
				file.delete(true, false, null);
			}
		} else if (!file.exists()) {
			file.create(new ByteArrayInputStream(content.getBytes(getCharset(path))), true, null);
		} else {
			file.setContents(new ByteArrayInputStream(content.getBytes(getCharset(path))), true, true, null);
		}
	}

	private Charset getCharset(IPath path) throws CoreException {
		final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		if (file.exists()) {
			return Charset.forName(file.getCharset());
		}
		return Charset.defaultCharset();
	}

	private void openChange(AgentChange agentChange) {
		try {
			final AgentTask task = findTask(agentChange).orElseThrow(() -> new IllegalStateException("No task found for change: " + agentChange.path().toPortableString()));
			final List<IProject> projects = task.getRequest().projects();
			final FileSystem fileSystem = new FileSystem(projects, projects.get(0).getWorkspace().getRoot());
			fileSystem.load(AgentStorage.getFileSystemPath(task.getId()).toPath());
			final List<Suggestion> suggestions = fileSystem.toSuggestions(agentChange.path(), HistoryType.AGENT);
			final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(agentChange.path());
			IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
			final InlineCompletionController controller = InlineCompletionController.setup(EclipseUtils.getActiveTextEditor().orElseThrow());
			controller.setup(suggestions);
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Failed to open agent task editor", exception);
			final String detail = exception.getMessage() != null ? exception.getMessage() : exception.toString();
			ErrorDialog.openError(getSite().getShell(), "Error", "Failed to open agent task editor", Status.error(detail, exception));
		}
	}

	private void openCompareDialog(AgentChange change, boolean reference) {
		try {
			final AgentTask task = findTask(change).orElseThrow(() -> new IllegalStateException("No task found for change: " + change.path().toPortableString()));
			final FileSystem fileSystem = loadFileSystem(task);
			final IPath path = change.path();
			final String proposedContent = fileSystem.getChangedContent(path);
			final String compareContent = reference ? fileSystem.getReferenceContent(path) : fileSystem.readWorktreeFile(path);
			DiffUtils.openDiff(proposedContent, compareContent);
		} catch (final Exception exception) {
			logAndShowError("Failed to open compare dialog", exception);
		}
	}

	private void logAndShowError(String message, Exception exception) {
		AiCoderActivator.log().error(message, exception);
		final String detail = exception.getMessage() != null ? exception.getMessage() : exception.toString();
		ErrorDialog.openError(getSite().getShell(), "Error", message, Status.error(detail, exception));
	}

	private void updateActionEnablement() {
		if (this.abortTaskAction != null) {
			this.abortTaskAction.setEnabled(hasRunningSelectedTasks());
		}
		if (this.abortAllTasksAction != null) {
			this.abortAllTasksAction.setEnabled(hasRunningTasks());
		}
	}

	private boolean hasRunningSelectedTasks() {
		return getSelectedAgentTasks().stream().anyMatch(task -> task.getStatus() == AgentStatus.RUNNING);
	}

	private boolean hasRunningTasks() {
		return AiCoderActivator.getDefault().getAgentTasksState().getAgentTasks().stream().anyMatch(task -> task.getStatus() == AgentStatus.RUNNING);
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
			if (AgentTaskTreeView.this.treeViewer.getTree().isDisposed()) {
				return;
			}
			AgentTaskTreeView.this.treeViewer.setInput(List.of());
			AgentTaskTreeView.this.treeViewer.refresh();
			updateActionEnablement();
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
			if (element instanceof final AgentChange agentChange) {
				return findTask(agentChange).orElse(null);
			}
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
				} else if (agentTask.getStatus() == AgentStatus.CANCELLED) {
					return AiCoderActivator.getImage(AiCoderImageKey.CANCELED_ICON);
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
				return String.format("%s - %s", agentTask.getTitle(), Utils.formatRelativeTime(agentTask.getCreationTime()));
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
