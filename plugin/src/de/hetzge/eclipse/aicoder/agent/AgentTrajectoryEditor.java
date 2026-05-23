package de.hetzge.eclipse.aicoder.agent;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;

public final class AgentTrajectoryEditor extends EditorPart {

	private final AgentTasksState.AgentTrajectoryStateListener agentTrajectoryStateListener;
	private Composite parentComposite;

	public AgentTrajectoryEditor() {
		this.agentTrajectoryStateListener = new AgentTrajectoryStateListener();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof final AgentTrajectoryEditorInput agentTrajectoryEditorInput) {
			setSite(site);
			setInput(input);
			setPartName(agentTrajectoryEditorInput.getName());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parentComposite = new Composite(parent, SWT.NONE);
		this.parentComposite.setLayout(new GridLayout(1, false));
		new Job("Load trajectory") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					AiCoderActivator.getDefault().getAgentTasksState().addTrajectoryListener(getAgentTask().id(), AgentTrajectoryEditor.this.agentTrajectoryStateListener);
					return Status.OK_STATUS;
				} catch (final Exception exception) {
					return Status.error("Failed to load trajectory", exception);
				}
			}
		}.schedule();
	}

	@Override
	public void dispose() {
		super.dispose();
		AiCoderActivator.getDefault().getAgentTasksState().removeTrajectoryListener(this.agentTrajectoryStateListener);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
	}

	public AgentTask getAgentTask() {
		return ((AgentTrajectoryEditorInput) getEditorInput()).agentTask;
	}

	private class AgentTrajectoryStateListener implements AgentTasksState.AgentTrajectoryStateListener {
		@Override
		public void onAgentTrajectoryChanged(UUID id, LlmMessage message) {
			Display.getDefault().asyncExec(() -> {
				new AgentTrajectoryMessageComposite(AgentTrajectoryEditor.this.parentComposite, message);
			});
		}
	}

	public static class AgentTrajectoryEditorInput implements IEditorInput {

		private final AgentTask agentTask;

		public AgentTrajectoryEditorInput(AgentTask agentTask) {
			this.agentTask = agentTask;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return AiCoderActivator.getImageDescriptor(AiCoderImageKey.FILL_IN_MIDDLE_ICON); // TODO
		}

		@Override
		public String getName() {
			return this.agentTask.title();
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return this.agentTask.title();
		}
	}
}
