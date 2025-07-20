package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.ContextPreferences.ContextTypePositionItem;
import de.hetzge.eclipse.aicoder.context.Context;
import de.hetzge.eclipse.aicoder.context.PrefixContextEntry;
import de.hetzge.eclipse.aicoder.context.SuffixContextEntry;

public class ContextPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private CheckboxTableViewer tableViewer;
	private List<ContextTypePositionItem> contextTypeItems;
	private Button upButton;
	private Button downButton;

	public ContextPreferencePage() {
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Configure context");
	}

	@Override
	public void init(IWorkbench workbench) {
		final Map<String, ContextTypePositionItem> preferenceItemByPrefix = ContextPreferences.getContextTypePositionByPrefix();
		final Map<String, ContextTypePositionItem> calculatedItemByPrefix = new HashMap<>(preferenceItemByPrefix);
		for (final String prefix : Context.DEFAULT_PREFIX_ORDER) {
			if (!calculatedItemByPrefix.containsKey(prefix)) {
				final boolean enabled = preferenceItemByPrefix.isEmpty(); // if user has already stored preferences, then do not enable by default
				calculatedItemByPrefix.put(prefix, new ContextTypePositionItem(prefix, enabled, calculatedItemByPrefix.size() + 1));
			}
		}
		this.contextTypeItems = new ArrayList<>(calculatedItemByPrefix.values());
		this.contextTypeItems.sort((a, b) -> Integer.compare(a.position(), b.position()));
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		// Description label
		final Label descLabel = new Label(composite, SWT.WRAP);
		descLabel.setText("Enable/disable context types and set their order in the prompt");
		final GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		descData.widthHint = 400;
		descLabel.setLayoutData(descData);

		// Table viewer
		this.tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		this.tableViewer.addCheckStateListener(new ICheckStateListener() {
			  @Override
			public void checkStateChanged(CheckStateChangedEvent event) {
			     if (((ContextTypePositionItem) event.getElement()).isPrefixOrSuffix()) {
			        Display.getDefault().asyncExec(() -> ContextPreferencePage.this.tableViewer.setChecked(event.getElement(), true));
			     }
			  }
			});
		final Table table = this.tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 300;
		table.setLayoutData(tableData);

		// Create columns
		final TableViewerColumn nameColumn = new TableViewerColumn(this.tableViewer, SWT.NONE);
		nameColumn.getColumn().setText("Context Type");
		nameColumn.getColumn().setWidth(200);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				final String prefix = ((ContextTypePositionItem) element).prefix();
				return Context.CONTEXT_TYPE_NAME_BY_CONTEXT_PREFIX.getOrDefault(prefix, prefix);
			}

			@Override
			public Color getBackground(Object element) {
				return ((ContextTypePositionItem) element).isPrefixOrSuffix() ? new Color(null, 240, 240, 240) : null;
			}
		});

		final TableViewerColumn positionColumn = new TableViewerColumn(this.tableViewer, SWT.NONE);
		positionColumn.getColumn().setText("Position");
		positionColumn.getColumn().setWidth(80);
		positionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return String.valueOf(((ContextTypePositionItem) element).position());
			}

			@Override
			public Color getBackground(Object element) {
				return ((ContextTypePositionItem) element).isPrefixOrSuffix() ? new Color(null, 240, 240, 240) : null;
			}
		});

		// Set content provider and input
		this.tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		this.tableViewer.setInput(this.contextTypeItems);

		// Set initial checked items
		final List<ContextTypePositionItem> checkedItems = this.contextTypeItems.stream()
				.filter(ContextTypePositionItem::enabled)
				.toList();
		this.tableViewer.setCheckedElements(checkedItems.toArray());

		// Buttons composite
		final Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1, false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		this.upButton = new Button(buttonComposite, SWT.PUSH);
		this.upButton.setText("Move Up");
		this.upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		this.upButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			moveSelectedItem(-1);
		}));

		this.downButton = new Button(buttonComposite, SWT.PUSH);
		this.downButton.setText("Move Down");
		this.downButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		this.downButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			moveSelectedItem(1);
		}));

		// Update button states
		this.tableViewer.addSelectionChangedListener(event -> updateButtonStates());
		updateButtonStates();

		return composite;
	}

	private void moveSelectedItem(int direction) {
		final IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}
		final ContextTypePositionItem selectedItem = (ContextTypePositionItem) selection.getFirstElement();
		final int currentIndex = this.contextTypeItems.indexOf(selectedItem);
		int newIndex = currentIndex + direction;

		// Keep prefix/suffix type together
		if (newIndex >= 0 && newIndex < this.contextTypeItems.size()) {
			final String newIndexPrefix = this.contextTypeItems.get(newIndex).prefix();
			if (newIndexPrefix.equals(PrefixContextEntry.PREFIX) || newIndexPrefix.equals(SuffixContextEntry.PREFIX)) {
				newIndex += direction; // move one more step
			}
		}

		if (newIndex >= 0 && newIndex < this.contextTypeItems.size()) {
			final ContextTypePositionItem otherItem = this.contextTypeItems.get(newIndex);
			// Swap items
			this.contextTypeItems.set(currentIndex, otherItem);
			this.contextTypeItems.set(newIndex, selectedItem);

			this.contextTypeItems = new ArrayList<>(this.contextTypeItems.stream().map(item -> item
					.withPosition(this.contextTypeItems.indexOf(item) + 1)
					.withEnabled(this.tableViewer.getChecked(item))).toList());
			this.tableViewer.setInput(this.contextTypeItems);
			this.tableViewer.setSelection(new StructuredSelection(this.contextTypeItems.get(newIndex)));
			this.tableViewer.setCheckedElements(this.contextTypeItems.stream().filter(ContextTypePositionItem::enabled).toArray());
			updateButtonStates();
		}
	}

	private void updateButtonStates() {
		final IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
		final boolean hasSelection = !selection.isEmpty();

		if (hasSelection) {
			final ContextTypePositionItem selectedItem = (ContextTypePositionItem) selection.getFirstElement();
			final int index = this.contextTypeItems.indexOf(selectedItem);
			this.upButton.setEnabled(index > 0 && !selectedItem.isPrefixOrSuffix());
			this.downButton.setEnabled(index < this.contextTypeItems.size() - 1 && !selectedItem.isPrefixOrSuffix());
		} else {
			this.upButton.setEnabled(false);
			this.downButton.setEnabled(false);
		}
	}

	@Override
	public boolean performOk() {
		final Object[] checkedElements = this.tableViewer.getCheckedElements();
		final List<ContextTypePositionItem> checkedItems = Arrays.asList(Arrays.copyOf(checkedElements, checkedElements.length, ContextTypePositionItem[].class));
		ContextPreferences.setContextTypePositions(this.contextTypeItems.stream().map(item -> item.withEnabled(checkedItems.contains(item))).toList());
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		this.contextTypeItems = new ArrayList<>(Context.DEFAULT_PREFIX_ORDER.stream().map(prefix -> new ContextTypePositionItem(prefix, true, Context.DEFAULT_PREFIX_ORDER.indexOf(prefix) + 1)).toList());
		this.contextTypeItems.sort((a, b) -> Integer.compare(a.position(), b.position()));
		this.tableViewer.setInput(this.contextTypeItems);
		this.tableViewer.setCheckedElements(this.contextTypeItems.toArray());
		super.performDefaults();
	}
}