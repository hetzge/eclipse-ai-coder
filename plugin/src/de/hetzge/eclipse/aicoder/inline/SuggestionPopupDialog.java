package de.hetzge.eclipse.aicoder.inline;

import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.DiffUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public final class SuggestionPopupDialog extends PopupDialog {

	private static final int TOOLBAR_HEIGHT = 24;

	public static final int ACCEPT_RETURN_CODE = 10;
	public static final int REJECT_RETURN_CODE = 20;

	private final ITextViewer parentTextViewer;
	private final Suggestion suggestion;
	private final IFile file;
	private SuggestionStyledTextViewer styledTextViewer;
	private ToolItem acceptItem;

	public SuggestionPopupDialog(ITextViewer parentTextViewer, Suggestion suggestion, IFile file) {
		super(parentTextViewer.getTextWidget().getShell(), SWT.ON_TOP, true, false, false, false, false, null, null);
		this.parentTextViewer = parentTextViewer;
		this.suggestion = suggestion;
		this.file = file;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);

		this.styledTextViewer = new SuggestionStyledTextViewer(container, this.parentTextViewer, this.suggestion.content());
		final DiffMode diffMode = AiCoderPreferences.getDiffMode();
		if (diffMode == DiffMode.LINE) {
			this.styledTextViewer.setupLineDiff();
		} else if (diffMode == DiffMode.CHAR) {
			this.styledTextViewer.setupCharDiff();
		} else if (diffMode == DiffMode.ORIGINAL) {
			this.styledTextViewer.setupOriginalDiff();
		} else if (diffMode == DiffMode.NEW) {
			this.styledTextViewer.setupNewDiff();
		} else {
			throw new IllegalStateException("Unknown diff mode: " + diffMode);
		}

		final Composite toolbarContainer = new Composite(container, SWT.NONE);
		final GridData gridData = GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, TOOLBAR_HEIGHT).create();
		toolbarContainer.setLayoutData(gridData);
		final GridLayout footerLayout = new GridLayout(2, true);
		footerLayout.marginWidth = 0;
		footerLayout.marginHeight = 0;
		toolbarContainer.setLayout(footerLayout);

		final ToolBar leftToolBar = new ToolBar(toolbarContainer, SWT.HORIZONTAL | SWT.TRAIL);
		leftToolBar.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, TOOLBAR_HEIGHT).create());
		this.acceptItem = new ToolItem(leftToolBar, SWT.PUSH);
		this.acceptItem.setText("Accept");
		this.acceptItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.ACCEPT_ICON));
		this.acceptItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			accept();
		}));
		final ToolItem rejectItem = new ToolItem(leftToolBar, SWT.PUSH);
		rejectItem.setText("Reject");
		rejectItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.REJECT_ICON));
		rejectItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			reject();
		}));
		final ToolItem mergeItem = new ToolItem(leftToolBar, SWT.PUSH);
		mergeItem.setText("Merge");
		mergeItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.RUN_ICON));
		mergeItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final String content = this.parentTextViewer.getDocument().get();
			DiffUtils.openDiff(this.file, this.suggestion.applyTo(content));
		}));
		leftToolBar.pack();

		final ToolBar rightToolBar = new ToolBar(toolbarContainer, SWT.HORIZONTAL | SWT.TRAIL);
		rightToolBar.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, TOOLBAR_HEIGHT).create());
		final ToolItem lineDiffItem = new ToolItem(rightToolBar, SWT.RADIO);
		lineDiffItem.setText("Line");
		lineDiffItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.DIFF_LINE_ICON));
		lineDiffItem.setSelection(diffMode == DiffMode.LINE);
		lineDiffItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			this.styledTextViewer.setupLineDiff();
			AiCoderPreferences.setDiffMode(DiffMode.LINE);
		}));
		final ToolItem charDiffItem = new ToolItem(rightToolBar, SWT.RADIO);
		charDiffItem.setText("Char");
		charDiffItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.DIFF_CHAR_ICON));
		charDiffItem.setSelection(diffMode == DiffMode.CHAR);
		charDiffItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			this.styledTextViewer.setupCharDiff();
			AiCoderPreferences.setDiffMode(DiffMode.CHAR);
		}));
		final ToolItem originalDiffItem = new ToolItem(rightToolBar, SWT.RADIO);
		originalDiffItem.setText("Original");
		originalDiffItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.DIFF_OLD_ICON));
		originalDiffItem.setSelection(diffMode == DiffMode.ORIGINAL);
		originalDiffItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			this.styledTextViewer.setupOriginalDiff();
			AiCoderPreferences.setDiffMode(DiffMode.ORIGINAL);
		}));
		final ToolItem newDiffItem = new ToolItem(rightToolBar, SWT.RADIO);
		newDiffItem.setText("New");
		newDiffItem.setImage(AiCoderActivator.getImage(AiCoderImageKey.DIFF_NEW_ICON));
		newDiffItem.setSelection(diffMode == DiffMode.NEW);
		newDiffItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			this.styledTextViewer.setupNewDiff();
			AiCoderPreferences.setDiffMode(DiffMode.NEW);
		}));
		rightToolBar.pack();

		final StyledText parentStyledText = this.parentTextViewer.getTextWidget();
		final ControlListener controlListener = new ControlListenerImplementation();
		final PaintListener paintListener = new PaintListenerImplementation();
		parentStyledText.getShell().addControlListener(controlListener);
		parentStyledText.addControlListener(controlListener);
		parentStyledText.addPaintListener(paintListener);
		getShell().addDisposeListener(event -> {
			parentStyledText.getShell().removeControlListener(controlListener);
			parentStyledText.removeControlListener(controlListener);
			parentStyledText.removePaintListener(paintListener);
		});
		this.styledTextViewer.getFocusControl().addListener(SWT.Traverse, event -> {
			System.out.println("SuggestionPopupDialog.createDialogArea(aaa)");
			if (event.type == SWT.Traverse && event.detail == SWT.TRAVERSE_ESCAPE) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				reject();
			}
		});
		this.styledTextViewer.getFocusControl().addListener(SWT.Traverse, event -> {
			System.out.println("SuggestionPopupDialog.createDialogArea(bbb)");
			if (event.type == SWT.Traverse && (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS)) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				accept();
			}
		});
		getShell().addListener(SWT.MouseWheel, event -> {
			// event.count is positive when wheel scrolls up, negative when down
			final int deltaLines = -event.count; // SWT uses inverted sign for wheel by default
			final int newTop = Math.max(0, parentStyledText.getTopIndex() + deltaLines);
			parentStyledText.setTopIndex(newTop);
			// Prevent default scrolling of the source if desired:
			event.type = SWT.None;
		});
		return container;
	}

	private void accept() {
		setReturnCode(ACCEPT_RETURN_CODE);
		close();
	}

	private void reject() {
		setReturnCode(REJECT_RETURN_CODE);
		close();
	}

	@Override
	protected Control getFocusControl() {
		return this.styledTextViewer.getFocusControl();
	}

	@Override
	public Point getDefaultSize() {
		return calculateSize(this.parentTextViewer, this.suggestion, this.styledTextViewer.getLineCount());
	}

	@Override
	public Point getDefaultLocation(Point initialSize) {
		return calculateLocation(this.parentTextViewer, this.suggestion.modelOffset());
	}

	public void updateSizeAndLocation() {
		final Shell shell = getShell();
		final Point newLocation = calculateLocation(this.parentTextViewer, this.suggestion.modelOffset());
		if (!Objects.equals(shell.getLocation(), newLocation)) {
			shell.setLocation(newLocation);
			shell.layout();
		}
		final Point newSize = calculateSize(this.parentTextViewer, this.suggestion, this.styledTextViewer.getLineCount());
		if (!Objects.equals(shell.getSize(), newSize)) {
			shell.setSize(newSize);
			shell.layout();
		}
	}

	public int getLineCount() {
		return this.styledTextViewer.getLineCount();
	}

	private static Point calculateSize(ITextViewer parentTextViewer, Suggestion suggestion, int lineCount) {
		final int widgetOffset = EclipseUtils.getWidgetOffset(parentTextViewer, suggestion.modelOffset());
		final Point location = parentTextViewer.getTextWidget().getLocationAtOffset(widgetOffset);
		final int width = parentTextViewer.getTextWidget().getSize().x - location.x - 24; // space for scrollbar
		final int height = (lineCount + 2) * parentTextViewer.getTextWidget().getLineHeight(); // +2 for toolbar
		return new Point(width, height);
	}

	private static Point calculateLocation(ITextViewer parentTextViewer, int modelOffset) {
		final int widgetOffset = EclipseUtils.getWidgetOffset(parentTextViewer, modelOffset);
		final Point location = parentTextViewer.getTextWidget().getLocationAtOffset(widgetOffset);
		return parentTextViewer.getTextWidget().toDisplay(new Point(location.x - 2, location.y - 2)); // -2 border offset
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
