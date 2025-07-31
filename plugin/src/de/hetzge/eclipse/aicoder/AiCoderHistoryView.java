package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
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

public class AiCoderHistoryView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AiCoderHistoryView";

	private TableViewer viewer;
	private final List<AiCoderHistoryEntry> historyEntries;

	public AiCoderHistoryView() {
		this.historyEntries = new ArrayList<>();
	}

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

		// Set initial data
		this.viewer.setInput(this.historyEntries);
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
			manager.add(new Action("Input") {
				@Override
				public void run() {
					openInputDialog();
				}
			});
		});
		menuManager.addMenuListener(manager -> {
			manager.add(new Action("Output") {
				@Override
				public void run() {
					openOutputDialog();
				}
			});
		});
		menuManager.addMenuListener(manager -> {
			manager.add(new Action("Response") {
				@Override
				public void run() {
					openLlmResponseDialog();
				}
			});
		});
		final Menu menu = menuManager.createContextMenu(this.viewer.getControl());
		this.viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, this.viewer);
	}

	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(event -> {
			openOutputDialog();
		});
	}

	private void openInputDialog() {
		final Shell shell = this.viewer.getControl().getShell();
		new ContentPreviewDialog(shell, "Input", ((AiCoderHistoryEntry) AiCoderHistoryView.this.viewer.getStructuredSelection().getFirstElement()).getInput())
				.open();
	}

	private void openOutputDialog() {
		final Shell shell = this.viewer.getControl().getShell();
		new ContentPreviewDialog(shell, "Output", ((AiCoderHistoryEntry) AiCoderHistoryView.this.viewer.getStructuredSelection().getFirstElement()).getOutput())
				.open();
	}

	private void openLlmResponseDialog() {
		final Shell shell = this.viewer.getControl().getShell();
		new ContentPreviewDialog(shell, "LLM Response", ((AiCoderHistoryEntry) AiCoderHistoryView.this.viewer.getStructuredSelection().getFirstElement()).getPlainLlmResponse())
				.open();
	}

	public void addHistoryEntry(AiCoderHistoryEntry entry) {
		this.historyEntries.add(0, entry); // Add to the beginning of the list
		if (this.historyEntries.size() > 100) { // TODO max preference
			this.historyEntries.removeLast();
		}
		this.viewer.refresh();
	}

	public void setLatestAccepted() {
		if (this.historyEntries.isEmpty()) {
			return;
		}
		this.historyEntries.get(0).setStatus("Accepted");
		this.viewer.refresh();
	}

	public void setLatestRejected() {
		if (this.historyEntries.isEmpty()) {
			return;
		}
		this.historyEntries.get(0).setStatus("Rejected");
		this.viewer.refresh();
	}

	private void createColumns() {
		// Timestamp column
		TableViewerColumn column = createTableViewerColumn("Timestamp", 175);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getTimestamp().toString();
			}
		});

		// Provider column
		column = createTableViewerColumn("Provider", 80);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getProvider().name();
			}
		});

		// File column
		column = createTableViewerColumn("File", 220);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getFile();
			}
		});

		// Input character count column
		column = createTableViewerColumn("Input Chars", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getInputCharacterCount());
			}
		});

		// Input word count column
		column = createTableViewerColumn("Input Words", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getInputWordCount());
			}
		});

		// Input line count column
		column = createTableViewerColumn("Input Lines", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getInputLineCount());
			}
		});

		// Output character count column
		column = createTableViewerColumn("Output Chars", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getOutputCharacterCount());
			}
		});

		// Output word count column
		column = createTableViewerColumn("Output Words", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getOutputWordCount());
			}
		});

		// Output line count column
		column = createTableViewerColumn("Output Lines", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getOutputLineCount());
			}
		});

		// Input token count column
		column = createTableViewerColumn("Input Tokens", 70);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getInputTokenCount());
			}
		});

		// Output token count column
		column = createTableViewerColumn("Output Tokens", 70);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return String.valueOf(entry.getOutputTokenCount());
			}
		});

		// Duration column
		column = createTableViewerColumn("Duration", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getFormattedDuration();
			}
		});

		// LLM duration column
		column = createTableViewerColumn("LLM duration", 60);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getFormattedLlmDuration();
			}
		});

		// Accepted
		column = createTableViewerColumn("Status", 100);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final AiCoderHistoryEntry entry = (AiCoderHistoryEntry) element;
				return entry.getStatus();
			}
		});
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