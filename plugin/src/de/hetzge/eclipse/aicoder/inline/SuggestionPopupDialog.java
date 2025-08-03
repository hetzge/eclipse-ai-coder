package de.hetzge.eclipse.aicoder.inline;

import java.util.Objects;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public final class SuggestionPopupDialog extends PopupDialog {

	private static final int TOOLBAR_HEIGHT = 24;

	private final StyledText parentStyledText;
	private final Suggestion suggestion;
	private Runnable acceptListener;
	private Runnable rejectListener;

	private StyledText styledText;

	public SuggestionPopupDialog(StyledText parentStyledText, Suggestion suggestion, Runnable acceptListener, Runnable rejectListener) {
		super(parentStyledText.getShell(), SWT.ON_TOP, true, false, false, false, false, null, null);
		this.parentStyledText = parentStyledText;
		this.suggestion = suggestion;
		this.acceptListener = acceptListener;
		this.rejectListener = rejectListener;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		this.styledText = SuggestionStyledText.create(container, this.parentStyledText, this.suggestion.content());
		this.styledText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		final ToolBar toolBar = new ToolBar(container, SWT.HORIZONTAL | SWT.TRAIL);
		toolBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, TOOLBAR_HEIGHT).create());
		final ToolItem acceptItem = new ToolItem(toolBar, SWT.PUSH);
		acceptItem.setText("Accept");
		acceptItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.ACCEPT_ICON));
		acceptItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> accept()));
		final ToolItem rejectItem = new ToolItem(toolBar, SWT.PUSH);
		rejectItem.setText("Reject");
		rejectItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.REJECT_ICON));
		rejectItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> reject()));
		toolBar.pack();
		final ControlListener controlListener = new ControlListenerImplementation();
		final PaintListener paintListener = new PaintListenerImplementation();
		this.parentStyledText.getShell().addControlListener(controlListener);
		this.parentStyledText.addControlListener(controlListener);
		this.parentStyledText.addPaintListener(paintListener);
		getShell().addDisposeListener(event -> {
			this.parentStyledText.getShell().removeControlListener(controlListener);
			this.parentStyledText.removeControlListener(controlListener);
			this.parentStyledText.removePaintListener(paintListener);
		});
		getShell().addListener(SWT.Traverse, event -> {
			if (event.type == SWT.Traverse && event.detail == SWT.TRAVERSE_ESCAPE) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				reject();
			}
		});
		this.styledText.addListener(SWT.Traverse, event -> {
			if (event.type == SWT.Traverse &&
					(event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS)) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				accept();
			}
		});
		this.styledText.addListener(SWT.MouseWheel, event -> {
			// event.count is positive when wheel scrolls up, negative when down
			final int deltaLines = -event.count; // SWT uses inverted sign for wheel by default
			final int newTop = Math.max(0, this.parentStyledText.getTopIndex() + deltaLines);
			this.parentStyledText.setTopIndex(newTop);
			// Prevent default scrolling of the source if desired:
			event.type = SWT.None;
		});
		return container;
	}

	private void accept() {
		if (this.acceptListener == null) {
			return;
		}
		final Runnable acceptListener = this.acceptListener;
		this.acceptListener = null; // unset to prevent double execution
		acceptListener.run();
	}

	private void reject() {
		if (this.rejectListener == null) {
			return;
		}
		final Runnable rejectListener = this.rejectListener;
		this.rejectListener = null; // unset to prevent double execution
		rejectListener.run();
	}

	@Override
	public boolean close() {
		final boolean close = super.close();
		reject();
		return close;
	}

	@Override
	protected Control getFocusControl() {
		return this.styledText;
	}

	@Override
	public Point getDefaultSize() {
		return calculateSize(this.parentStyledText, this.suggestion.content(), this.suggestion.modelOffset());
	}

	@Override
	public Point getDefaultLocation(Point initialSize) {
		return calculateLocation(this.parentStyledText, this.suggestion.modelOffset());
	}

	public void updateSizeAndLocation() {
		final Shell shell = getShell();
		final Point newLocation = calculateLocation(this.parentStyledText, this.suggestion.modelOffset());
		if (!Objects.equals(shell.getLocation(), newLocation)) {
			shell.setLocation(newLocation);
			shell.layout();
		}
		final Point newSize = calculateSize(this.parentStyledText, this.suggestion.content(), this.suggestion.modelOffset());
		if (!Objects.equals(shell.getSize(), newSize)) {
			shell.setSize(newSize);
			shell.layout();
		}
	}

	private static Point calculateSize(StyledText parentStyledText, String content, int modelOffset) {
		final Point location = parentStyledText.getLocationAtOffset(modelOffset);
		final int width = parentStyledText.getSize().x - location.x - 24; // space for scrollbar
		final int height = (int) (content.lines().count() * parentStyledText.getLineHeight() + TOOLBAR_HEIGHT + 10);
		return new Point(width, height);
	}

	private static Point calculateLocation(StyledText parentStyledText, int modelOffset) {
		final Point location = parentStyledText.getLocationAtOffset(modelOffset);
		// border offset
		location.x -= 2;
		location.y -= 2;
		return parentStyledText.toDisplay(location);
	}

	private final class PaintListenerImplementation implements PaintListener {
		@Override
		public void paintControl(PaintEvent event) {
			updateSizeAndLocation();
		}
	}

	private final class ControlListenerImplementation implements ControlListener {
		@Override
		public void controlMoved(ControlEvent event) {
			updateSizeAndLocation();
		}

		@Override
		public void controlResized(ControlEvent event) {
			updateSizeAndLocation();
		}
	}
}
