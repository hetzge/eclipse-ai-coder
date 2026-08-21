package de.hetzge.eclipse.aicoder.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.trajectory.ErrorTrajectoryEntry;
import de.hetzge.eclipse.aicoder.trajectory.MessageTrajectoryEntry;
import de.hetzge.eclipse.aicoder.trajectory.TrajectoryEntry;

public final class AgentTrajectoryEditor extends EditorPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTrajectoryEditor";

	/** Vertical gap between two trajectory entries. */
	private static final int VERTICAL_GAP = 8;

	private final AgentTasksState.AgentTrajectoryStateListener agentTrajectoryStateListener;

	private final List<TrajectoryEntry> entries = new ArrayList<>();
	private final List<Integer> heights = new ArrayList<>();

	private ScrolledComposite scrollComposite;
	private Composite parentComposite;
	private int measuredWidth = -1;
	private int lastStartIndex = -1;
	private int lastEndIndex = -1;

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
		this.scrollComposite.setExpandVertical(false);
		this.scrollComposite.setExpandHorizontal(false);

		this.parentComposite = new Composite(this.scrollComposite, SWT.NONE);
		// Absolute positioning: each visible entry is placed at its fixed vertical offset.
		this.parentComposite.setLayout(null);

		this.scrollComposite.setContent(this.parentComposite);

		this.scrollComposite.getVerticalBar().addListener(SWT.Selection, event -> render());
		this.scrollComposite.addListener(SWT.MouseVerticalWheel, event -> render());
		this.scrollComposite.addListener(SWT.Resize, event -> render());

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

	/**
	 * Renders the currently visible slice of the virtual trajectory. Non-visible entries are not created as widgets; only the total scroll height reflects all entries.
	 */
	private void render() {
		if (this.scrollComposite == null || this.scrollComposite.isDisposed()) {
			return;
		}
		if (this.parentComposite == null || this.parentComposite.isDisposed()) {
			return;
		}

		final int width = Math.max(1, this.scrollComposite.getClientArea().width);
		if (width != this.measuredWidth) {
			// The viewport width changed: discard all cached heights and re-measure everything.
			this.measuredWidth = width;
			this.heights.clear();
			for (final TrajectoryEntry entry : this.entries) {
				this.heights.add(measureEntry(entry));
			}
		} else {
			// Only measure newly appended entries whose height is not yet known.
			while (this.heights.size() < this.entries.size()) {
				this.heights.add(measureEntry(this.entries.get(this.heights.size())));
			}
		}

		final int totalHeight = computeTotalHeight();
		if (totalHeight <= 0) {
			this.scrollComposite.setMinSize(1, 1);
			return;
		}

		// The whole trajectory occupies this much space so the scrollbar is correct.
		this.parentComposite.setSize(width, totalHeight);
		this.scrollComposite.setMinSize(Math.max(1, width), totalHeight);

		final int originY = this.scrollComposite.getOrigin().y;
		final int clientHeight = Math.max(1, this.scrollComposite.getClientArea().height);
		final int startIndex = Math.max(0, indexAtY(originY));
		final int endIndex = Math.min(this.entries.size(), indexAtY(originY + clientHeight) + 1);

		if (startIndex == this.lastStartIndex && endIndex == this.lastEndIndex) {
			return;
		}
		this.lastStartIndex = startIndex;
		this.lastEndIndex = endIndex;

		// Dispose all previously rendered widgets and rebuild only the visible slice.
		for (final Control child : this.parentComposite.getChildren()) {
			child.dispose();
		}

		int y = 0;
		for (int i = 0; i < this.entries.size(); i++) {
			final int height = this.heights.get(i);
			if (i >= startIndex && i < endIndex) {
				final Composite entryComposite = createComposite(this.parentComposite, this.entries.get(i));
				entryComposite.setBounds(0, y, width, height);
			}
			y += height + VERTICAL_GAP;
		}
	}

	private int computeTotalHeight() {
		if (this.heights.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (final int height : this.heights) {
			total += height + VERTICAL_GAP;
		}
		return total - VERTICAL_GAP;
	}

	/**
	 * Finds the index of the entry whose vertical span contains the given absolute y coordinate.
	 */
	private int indexAtY(final int y) {
		int cumulative = 0;
		for (int i = 0; i < this.heights.size(); i++) {
			final int height = this.heights.get(i) + VERTICAL_GAP;
			cumulative += height;
			if (cumulative > y) {
				return i;
			}
		}
		return this.heights.size() - 1;
	}

	private int measureEntry(final TrajectoryEntry entry) {
		final Composite probe = new Composite(this.parentComposite, SWT.NONE);
		probe.setLayout(new GridLayout(1, false));

		final Composite child;
		if (entry instanceof final MessageTrajectoryEntry messageEntry) {
			child = new AgentTrajectoryMessageComposite(probe, messageEntry.message());
		} else if (entry instanceof final ErrorTrajectoryEntry errorEntry) {
			child = new AgentTrajectoryErrorComposite(probe, errorEntry.message());
		} else {
			probe.dispose();
			return 0;
		}

		child.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		probe.setSize(this.measuredWidth, 1000);
		probe.layout(true, true);

		final int height = child.getSize().y;
		child.dispose();
		probe.dispose();
		return height;
	}

	private Composite createComposite(final Composite parent, final TrajectoryEntry entry) {
		if (entry instanceof final MessageTrajectoryEntry messageEntry) {
			return new AgentTrajectoryMessageComposite(parent, messageEntry.message());
		}
		if (entry instanceof final ErrorTrajectoryEntry errorEntry) {
			return new AgentTrajectoryErrorComposite(parent, errorEntry.message());
		}
		return new Composite(parent, SWT.NONE);
	}

	private class AgentTrajectoryStateListener implements AgentTasksState.AgentTrajectoryStateListener {
		@Override
		public void onAgentTrajectoryChanged(UUID id, TrajectoryEntry entry) {
			Display.getDefault().asyncExec(() -> {
				AgentTrajectoryEditor.this.entries.add(entry);
				render();
			});
		}
	}
}