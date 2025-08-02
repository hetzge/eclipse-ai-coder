package de.hetzge.eclipse.aicoder.inline;

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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public final class SuggestionPopupDialog extends PopupDialog {

	private static final int TOOLBAR_HEIGHT = 24;

	private final StyledText parentStyledText;
	private final Suggestion suggestion;
	private final Runnable acceptListener;
	private final Runnable rejectListener;

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
		final ToolBar toolBar = new ToolBar(container, SWT.HORIZONTAL);
		toolBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, TOOLBAR_HEIGHT).create());
		final ToolItem acceptItem = new ToolItem(toolBar, SWT.FLAT | SWT.PUSH);
		acceptItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.ACCEPT_ICON));
		acceptItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> this.acceptListener.run()));
		acceptItem.setToolTipText("Accept");
		final ToolItem rejectItem = new ToolItem(toolBar, SWT.FLAT | SWT.PUSH);
		rejectItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.REJECT_ICON));
		rejectItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> this.rejectListener.run()));
		rejectItem.setToolTipText("Reject");
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
				this.rejectListener.run();
			}
		});
		this.styledText.addListener(SWT.Traverse, event -> {
			if (event.type == SWT.Traverse &&
					(event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS)) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				this.acceptListener.run();
			}
		});
		return container;
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
		getShell().setSize(calculateSize(this.parentStyledText, this.suggestion.content(), this.suggestion.modelOffset()));
		getShell().setLocation(calculateLocation(this.parentStyledText, this.suggestion.modelOffset()));
	}

	private static Point calculateSize(StyledText parentStyledText, String content, int modelOffset) {
		final Point location = parentStyledText.getLocationAtOffset(modelOffset);
		final int width = parentStyledText.getSize().x - location.x - 24; // space for scrollbar
		final int height = content.lines().toList().size() * parentStyledText.getLineHeight() + TOOLBAR_HEIGHT + 10;
		return new Point(width, height);
	}

	private static Point calculateLocation(StyledText parentStyledText, int modelOffset) {
		final Point locationAtOffset = parentStyledText.getLocationAtOffset(modelOffset);
		// border offset
		locationAtOffset.x -= 2;
		locationAtOffset.y -= 2;
		return parentStyledText.toDisplay(locationAtOffset);
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
