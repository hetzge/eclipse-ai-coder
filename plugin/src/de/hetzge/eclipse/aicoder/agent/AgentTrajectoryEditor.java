package de.hetzge.eclipse.aicoder.agent;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;

public final class AgentTrajectoryEditor extends EditorPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTrajectoryEditor";

	private final AgentTasksState.AgentTrajectoryStateListener agentTrajectoryStateListener;
	private ScrolledComposite scrollComposite;
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
		} else {
			throw new PartInitException("Invalid input: " + input);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		this.scrollComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.scrollComposite.setExpandVertical(true);
		this.scrollComposite.setExpandHorizontal(true);
		this.parentComposite = new Composite(this.scrollComposite, SWT.NONE);
		this.parentComposite.setLayout(new GridLayout(1, false));
		this.scrollComposite.setContent(this.parentComposite);
		this.scrollComposite.addListener(SWT.Resize, event -> {
			final int width = this.scrollComposite.getClientArea().width;
			if (width > 0) {
				this.parentComposite.layout(true, true);
				final Point size = this.parentComposite.computeSize(width, SWT.DEFAULT);
				this.scrollComposite.setMinSize(width, size.y);
			}
		});
		new Job("Load trajectory") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					AiCoderActivator.getDefault().getAgentTasksState().loadAndAddTrajectoryListener(getAgentTask().getId(), AgentTrajectoryEditor.this.agentTrajectoryStateListener);
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
		return ((AgentTrajectoryEditorInput) getEditorInput()).getAgentTask();
	}

	private class AgentTrajectoryStateListener implements AgentTasksState.AgentTrajectoryStateListener {
		@Override
		public void onAgentTrajectoryChanged(UUID id, LlmMessage message) {
			Display.getDefault().asyncExec(() -> {
				final AgentTrajectoryMessageComposite messageComposite = new AgentTrajectoryMessageComposite(AgentTrajectoryEditor.this.parentComposite, message);
				messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
				AgentTrajectoryEditor.this.parentComposite.layout(true, true);
				AgentTrajectoryEditor.this.parentComposite.pack();
				final int width = AgentTrajectoryEditor.this.scrollComposite.getClientArea().width;
				final int height = AgentTrajectoryEditor.this.parentComposite.computeSize(width, SWT.DEFAULT).y;
				AgentTrajectoryEditor.this.scrollComposite.setMinSize(Math.max(1, width), Math.max(1, height));
			});
		}
	}
}