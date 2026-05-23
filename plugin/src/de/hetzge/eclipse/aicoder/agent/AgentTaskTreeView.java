package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class AgentTaskTreeView extends ViewPart {

	private final AgentTasksStateListener agentTasksStateListener;
	private TreeViewer treeViewer;

	public AgentTaskTreeView() {
		this.agentTasksStateListener = new AgentTasksStateListener();
	}

	@Override
	public void createPartControl(Composite parent) {
		this.treeViewer = new TreeViewer(parent);
		this.treeViewer.setContentProvider(new AgentTaskContentProvider());
		this.treeViewer.setLabelProvider(new AgentTaskLabelProvider());
		this.treeViewer.addDoubleClickListener(event -> {
			final Object selected = event.getSelection();
			if (selected instanceof final AgentTask agentTask) {
				// TODO open editor
			}
		});
		AiCoderActivator.getDefault().getAgentTasksState().addListener(this.agentTasksStateListener);
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
		AiCoderActivator.getDefault().getAgentTasksState().removeListener(this.agentTasksStateListener);
	}

	private class AgentTasksStateListener implements AgentTasksState.AgentTasksStateListener {

		@Override
		public void onAgentTasksChanged() {
			AgentTaskTreeView.this.treeViewer.setInput(null);
			AgentTaskTreeView.this.treeViewer.expandAll();
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
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof final AgentTask agentTask) {
				return agentTask.title();
			}
			return null;
		}
	}
}
