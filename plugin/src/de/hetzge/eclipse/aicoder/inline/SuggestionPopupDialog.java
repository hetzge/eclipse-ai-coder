package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.DiffUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public final class SuggestionPopupDialog {

	private static final int TOOLBAR_HEIGHT = 24;

	public static final int ACCEPT_RETURN_CODE = 10;
	public static final int REJECT_RETURN_CODE = 20;

	private final ITextViewer parentTextViewer;
	private final Suggestion suggestion;
	private SuggestionStyledTextViewer styledTextViewer;
	private ToolItem acceptItem;
	private Composite container;
	private int returnCode;

	public SuggestionPopupDialog(ITextViewer parentTextViewer, Suggestion suggestion) {
		this.parentTextViewer = parentTextViewer;
		this.suggestion = suggestion;
	}

	public void open() {
		this.container = new Composite(this.parentTextViewer.getTextWidget().getParent(), SWT.BORDER);
		final GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		this.container.setLayout(layout);
		this.container.moveAbove(null);

		this.styledTextViewer = new SuggestionStyledTextViewer(this.container, this.parentTextViewer, this.suggestion);
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

		final Composite toolbarContainer = new Composite(this.container, SWT.NONE);
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
			try {
				final String content = this.parentTextViewer.getDocument().get();
				final Document document = new Document(content);
				this.suggestion.applyTo(document);
				DiffUtils.openDiff(this.parentTextViewer, document.get());
			} catch (final BadLocationException exception) {
				AiCoderActivator.log().error("Failed to open diff", exception);
				AiCoderActivator.openErrorDialog("Failed to open diff", "Failed to open diff", exception);
			}
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
		this.container.addDisposeListener(event -> {
			parentStyledText.getShell().removeControlListener(controlListener);
			parentStyledText.removeControlListener(controlListener);
			parentStyledText.removePaintListener(paintListener);
		});
		this.styledTextViewer.getFocusControl().addListener(SWT.Traverse, event -> {
			if (event.type == SWT.Traverse && event.detail == SWT.TRAVERSE_ESCAPE) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				reject();
			}
		});
		this.styledTextViewer.getFocusControl().addListener(SWT.Traverse, event -> {
			if (event.type == SWT.Traverse && (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS)) {
				event.doit = false;
				event.detail = SWT.TRAVERSE_NONE;
				accept();
			}
		});
		this.container.addListener(SWT.MouseWheel, event -> {
			final int deltaLines = -event.count;
			final int newTop = Math.max(0, parentStyledText.getTopIndex() + deltaLines);
			parentStyledText.setTopIndex(newTop);
			event.type = SWT.None;
		});

		updateSizeAndLocation();
	}

	public void close() {
		if (this.container != null && !this.container.isDisposed()) {
			this.container.dispose();
		}
	}

	private void accept() {
		this.returnCode = ACCEPT_RETURN_CODE;
		close();
	}

	private void reject() {
		this.returnCode = REJECT_RETURN_CODE;
		close();
	}

	protected Control getFocusControl() {
		return this.styledTextViewer.getFocusControl();
	}

	public Point getDefaultSize() {
		return calculateSize(this.parentTextViewer, this.suggestion, this.styledTextViewer.getLineCount());
	}

	public Point getDefaultLocation(Point initialSize) {
		return calculateLocation(this.parentTextViewer, this.suggestion);
	}

	public void updateSizeAndLocation() {
		if (this.container == null || this.container.isDisposed() || this.styledTextViewer == null) {
			return;
		}
		final StyledText textWidget = this.parentTextViewer.getTextWidget();
		if (textWidget.isDisposed()) {
			return;
		}
		final Point size = calculateSize(this.parentTextViewer, this.suggestion, this.styledTextViewer.getLineCount());
		final Point displayLocation = calculateLocation(this.parentTextViewer, this.suggestion);
		final Point location = textWidget.toControl(displayLocation);
		this.container.setBounds(location.x + 40, location.y, Math.max(1, size.x), Math.max(1, size.y)); // TODO 40
		this.container.layout(true, true);
		this.container.moveAbove(null);
	}

	public int getLineCount() {
		return this.styledTextViewer.getLineCount();
	}

	public Widget getContainer() {
		return this.container;
	}

	public int getReturnCode() {
		return this.returnCode;
	}

	private static Point calculateSize(ITextViewer parentTextViewer, Suggestion suggestion, int lineCount) {
		final StyledText textWidget = parentTextViewer.getTextWidget();
		final int widgetOffset = EclipseUtils.getWidgetOffset(parentTextViewer, suggestion.modelOffset());
		final Point location = textWidget.getLocationAtOffset(widgetOffset);
		final int width = textWidget.getSize().x - location.x - 24;
		final int height = (lineCount + 2) * textWidget.getLineHeight();
		return new Point(Math.max(1, width), Math.max(1, height));
	}

	private static Point calculateLocation(ITextViewer parentTextViewer, Suggestion suggestion) {
		final StyledText textWidget = parentTextViewer.getTextWidget();
		final int widgetOffset = EclipseUtils.getWidgetOffset(parentTextViewer, suggestion.modelOffset());
		final Point location = textWidget.getLocationAtOffset(widgetOffset);
		final int lineHeight = textWidget.getLineHeight();
		final int offset = lineHeight * (2 + suggestion.newLines());
		return textWidget.toDisplay(new Point(location.x - 2, location.y + (suggestion.oldLines() == 0 ? -offset : 0) - 2));
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