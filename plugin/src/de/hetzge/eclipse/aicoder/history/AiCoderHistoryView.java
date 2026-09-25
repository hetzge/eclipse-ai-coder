package de.hetzge.eclipse.aicoder.history;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.ContentPreviewDialog;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;

public class AiCoderHistoryView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AiCoderHistoryView";

	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		final GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		createViewer(parent);

		// Make the table fill the entire view
		final GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		this.viewer.getControl().setLayoutData(gridData);

		refreshHistory();
	}

	private void createViewer(Composite parent) {
		this.viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		// Create the table with column names
		createColumns();

		final Table table = this.viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		this.viewer.setContentProvider(ArrayContentProvider.getInstance());

		hookContextMenu();
		hookDoubleClickAction();
	}

	private void hookContextMenu() {
		final MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(manager -> {
			final HistoryEntry entry = getSelectedEntry();
			if (entry == null) {
				return;
			}
			manager.add(new Action("Context") {
				@Override
				public void run() {
					new ContentPreviewDialog(getShell(), "Context", entry.getContext()).open();
				}
			});
			manager.add(new Action("Content") {
				@Override
				public void run() {
					new ContentPreviewDialog(getShell(), "Content", entry.getContent()).open();
				}
			});
			final Action responseAction = new Action("LLM Response") {
				@Override
				public void run() {
					final String plainResponses = entry.getResponses().stream()
							.map(LlmResponse::getPlainResponse)
							.collect(Collectors.joining("\n\n----------------------------------------\n\n"));
					new ContentPreviewDialog(getShell(), "LLM Response", plainResponses).open();
				}
			};
			responseAction.setEnabled(!entry.getResponses().isEmpty());
			manager.add(responseAction);
			manager.add(new Separator());
			manager.add(new Action("Delete") {
				@Override
				public void run() {
					AiCoderActivator.getDefault().getHistory().deleteHistoryEntry(entry);
				}
			});
		});
		final Menu menu = menuManager.createContextMenu(this.viewer.getControl());
		this.viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, this.viewer);
	}

	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(event -> {
			final HistoryEntry entry = getSelectedEntry();
			if (entry != null) {
				new ContentPreviewDialog(getShell(), "Content", entry.getContent()).open();
			}
		});
	}

	private HistoryEntry getSelectedEntry() {
		return (HistoryEntry) this.viewer.getStructuredSelection().getFirstElement();
	}

	private Shell getShell() {
		return this.viewer.getControl().getShell();
	}

	public void refreshHistory() {
		this.viewer.setInput(AiCoderActivator.getDefault().getHistory().getCurrentHistoryEntries());
	}

	public void refreshHistory(HistoryEntry entry) {
		this.viewer.update(entry, null);
	}

	private void createColumns() {
		createColumn("Time", 135, entry -> formatTimestamp(entry.getTimestamp()));
		createColumn("Mode", 90, entry -> entry.getMode().name());
		createColumn("File", 220, entry -> entry.getFilePath().toString());
		createColumn("Status", 90, entry -> entry.getStatus().name());
		createColumn("Model", 150, AiCoderHistoryView::getModelLabel);

		// Context (input) statistics
		createColumn("Context Chars", 60, entry -> String.valueOf(getCharCount(entry.getContext())));
		createColumn("Context Words", 60, entry -> String.valueOf(getWordCount(entry.getContext())));
		createColumn("Context Lines", 60, entry -> String.valueOf(getLineCount(entry.getContext())));

		// Content (output) statistics
		createColumn("Content Chars", 60, entry -> String.valueOf(getCharCount(entry.getContent())));
		createColumn("Content Words", 60, entry -> String.valueOf(getWordCount(entry.getContent())));
		createColumn("Content Lines", 60, entry -> String.valueOf(getLineCount(entry.getContent())));

		// LLM token statistics
		createColumn("Input Tokens", 70, entry -> getTokenCount(entry, LlmResponse::getInputTokens));
		createColumn("Output Tokens", 70, entry -> getTokenCount(entry, LlmResponse::getOutputTokens));
		createColumn("Cached Tokens", 70, entry -> getTokenCount(entry, LlmResponse::getCachedTokens));
		createColumn("Reasoning Tokens", 80, entry -> getTokenCount(entry, LlmResponse::getReasoningTokens));

		// Timing
		createColumn("Duration", 70, entry -> formatDuration(entry.getDuration()));
		createColumn("LLM duration", 90, entry -> entry.getResponses().isEmpty() ? "" : formatDuration(getTotalResponseDuration(entry)));
		createColumn("Tokens/s", 70, AiCoderHistoryView::getTokensPerSecond);
	}

	private TableViewerColumn createColumn(String title, int width, Function<HistoryEntry, String> textProvider) {
		final TableViewerColumn viewerColumn = createTableViewerColumn(title, width);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return textProvider.apply((HistoryEntry) element);
			}
		});
		return viewerColumn;
	}

	private static String formatTimestamp(Instant timestamp) {
		if (timestamp == null) {
			return "";
		}
		return TIMESTAMP_FORMATTER.format(timestamp);
	}

	private static String getModelLabel(HistoryEntry entry) {
		return entry.getResponses().stream()
				.map(response -> response.getLlmModelOption().getLabel())
				.distinct()
				.collect(Collectors.joining(", "));
	}

	private static String getTokenCount(HistoryEntry entry, ToIntFunction<LlmResponse> tokenExtractor) {
		if (entry.getResponses().isEmpty()) {
			return "-";
		}
		return String.valueOf(entry.getResponses().stream().mapToInt(tokenExtractor).sum());
	}

	private static String getTokensPerSecond(HistoryEntry entry) {
		if (entry.getResponses().isEmpty()) {
			return "-";
		}
		final double seconds = getTotalResponseDuration(entry).toMillis() / 1000.0;
		if (seconds <= 0.0) {
			return "-";
		}
		final int outputTokens = entry.getResponses().stream().mapToInt(LlmResponse::getOutputTokens).sum();
		return String.format("%.1f", outputTokens / seconds);
	}

	private static Duration getTotalResponseDuration(HistoryEntry entry) {
		return entry.getResponses().stream()
				.map(LlmResponse::getDuration)
				.reduce(Duration.ZERO, Duration::plus);
	}

	private static int getCharCount(String text) {
		return text == null ? 0 : text.length();
	}

	private static int getWordCount(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return text.trim().split("\\s+").length;
	}

	private static int getLineCount(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return (int) text.lines().count();
	}

	private static String formatDuration(Duration duration) {
		if (duration == null) {
			return "";
		}
		final long millis = duration.toMillis();
		if (millis < 1000) {
			return millis + "ms";
		}
		final long seconds = millis / 1000;
		final long minutes = seconds / 60;
		final long remainderSeconds = seconds % 60;
		if (minutes > 0) {
			return minutes + "m " + String.format("%02d", remainderSeconds) + "s";
		}
		return remainderSeconds + "s";
	}

	private TableViewerColumn createTableViewerColumn(String title, int width) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(this.viewer, SWT.NONE);
		final org.eclipse.swt.widgets.TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(width);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	@Override
	public void setFocus() {
		this.viewer.getControl().setFocus();
	}

	public static Optional<AiCoderHistoryView> get() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench.getDisplay().syncCall(() -> {
			return Optional.ofNullable(workbench.getActiveWorkbenchWindow().getActivePage().findView(ID))
					.map(view -> (AiCoderHistoryView) view);
		});
	}

	public static AiCoderHistoryView open() throws CoreException {
		try {
			final IWorkbench workbench = PlatformUI.getWorkbench();
			return workbench.getDisplay().syncCall(() -> {
				return (AiCoderHistoryView) workbench.getActiveWorkbenchWindow().getActivePage().showView(ID);
			});
		} catch (final PartInitException exception) {
			throw new CoreException(Status.error("Failed to open view", exception));
		}
	}
}