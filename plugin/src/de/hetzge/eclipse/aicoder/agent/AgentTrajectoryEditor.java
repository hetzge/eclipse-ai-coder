package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderResultView;
import de.hetzge.eclipse.aicoder.trajectory.ErrorTrajectoryEntry;
import de.hetzge.eclipse.aicoder.trajectory.MessageTrajectoryEntry;
import de.hetzge.eclipse.aicoder.trajectory.TrajectoryEntry;
import mjson.Json;

public final class AgentTrajectoryEditor extends EditorPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTrajectoryEditor";

	private final AgentTasksState.AgentTrajectoryStateListener agentTrajectoryStateListener;

	private final List<TrajectoryEntry> entries = new ArrayList<>();

	private Browser browser;

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
		this.browser = new Browser(parent, SWT.NONE);
		new BrowserFunction(this.browser, "openResult") {
			@Override
			public Object function(Object[] arguments) {
				final String content = arguments[0].toString();
				AiCoderResultView.showContent(content);
				return null;
			}
		};
		this.browser.setJavascriptEnabled(true);
		try {
			final String html = new String(AgentTrajectoryEditor.class.getResourceAsStream("index.html").readAllBytes(), StandardCharsets.UTF_8);
			final String js = new String(AgentTrajectoryEditor.class.getResourceAsStream("index.js").readAllBytes(), StandardCharsets.UTF_8);
			this.browser.setText(html.replace("<script></script>", "<script>" + js + "</script>"), true);
		} catch (final IOException exception) {
			throw new RuntimeException("Failed to load trajectory editor HTML", exception);
		}

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
		public void onAgentTrajectoryChanged(UUID id, TrajectoryEntry entry) {
			Display.getDefault().asyncExec(() -> {
				AgentTrajectoryEditor.this.entries.add(entry);
				if (entry instanceof final MessageTrajectoryEntry messageTrajectoryEntry) {
					AgentTrajectoryEditor.this.browser.execute("addMessage(" + messageTrajectoryEntry.message().toJson() + ")");
				} else if (entry instanceof final ErrorTrajectoryEntry errorTrajectoryEntry) {
					AgentTrajectoryEditor.this.browser.execute("addError(" + Json.object().set("content", errorTrajectoryEntry.message()) + ")");
				}
			});
		}
	}
}