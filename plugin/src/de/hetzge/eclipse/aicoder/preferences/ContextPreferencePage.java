package de.hetzge.eclipse.aicoder.preferences;

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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.context.Context;
import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences.ContextTypePositionItem;

public class ContextPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context";

	private static final Map<String, String> SUB_PAGE_ID_BY_CONTEXT_PREFIX = Map.ofEntries(
			Map.entry(ProjectInformationsContextPreferencePage.CONTEXT_PREFIX, ProjectInformationsContextPreferencePage.ID),
			Map.entry(OpenEditorsContextPreferencePage.CONTEXT_PREFIX, OpenEditorsContextPreferencePage.ID),
			Map.entry(ImportsContextPreferencePage.CONTEXT_PREFIX, ImportsContextPreferencePage.ID),
			Map.entry(SuperContextPreferencePage.CONTEXT_PREFIX, SuperContextPreferencePage.ID),
			Map.entry(FileTreeContextPreferencePage.CONTEXT_PREFIX, FileTreeContextPreferencePage.ID),
			Map.entry(StickyContextPreferencePage.CONTEXT_PREFIX, StickyContextPreferencePage.ID),
			Map.entry(TypeContextPreferencePage.CONTEXT_PREFIX, TypeContextPreferencePage.ID),
			Map.entry(FillInMiddleContextPreferencePage.CONTEXT_PREFIX, FillInMiddleContextPreferencePage.ID),
			Map.entry(CustomContextPreferencePage.CONTEXT_PREFIX, CustomContextPreferencePage.ID),
			Map.entry(ClipboardContextPreferencePage.CONTEXT_PREFIX, ClipboardContextPreferencePage.ID),
			Map.entry(LastEditsContextPreferencePage.CONTEXT_PREFIX, LastEditsContextPreferencePage.ID),
			Map.entry(EmptyContextPreferencePage.CONTEXT_PREFIX, EmptyContextPreferencePage.ID),
			Map.entry(BlacklistedContextPreferencePage.CONTEXT_PREFIX, BlacklistedContextPreferencePage.ID),
			Map.entry(ScopeContextPreferencePage.CONTEXT_PREFIX, ScopeContextPreferencePage.ID),
			Map.entry(UserContextPreferencePage.CONTEXT_PREFIX, UserContextPreferencePage.ID),
			Map.entry(RootContextPreferencePage.CONTEXT_PREFIX, RootContextPreferencePage.ID),
			Map.entry(TypeMemberContextPreferencePage.CONTEXT_PREFIX, TypeMemberContextPreferencePage.ID),
			Map.entry(PackageContextPreferencePage.CONTEXT_PREFIX, PackageContextPreferencePage.ID),
			Map.entry(AiRerankContextPreferencePage.CONTEXT_PREFIX, AiRerankContextPreferencePage.ID),
			Map.entry(CodeViewportMemoryContextPreferencePage.CONTEXT_PREFIX, CodeViewportMemoryContextPreferencePage.ID));

	private final Map<CompletionMode, ContextPreferenceSubPage> subPagesByMode;
	private CCombo modeCombo;

	public ContextPreferencePage() {
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Configure context");
		this.subPagesByMode = new HashMap<>();
		for (final CompletionMode mode : CompletionMode.values()) {
			this.subPagesByMode.put(mode, new ContextPreferenceSubPage(mode, this));
		}
	}

	@Override
	public void init(IWorkbench workbench) {
		for (final ContextPreferenceSubPage subPage : this.subPagesByMode.values()) {
			subPage.init(workbench);
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);

		final Composite modeComposite = new Composite(composite, SWT.NONE);
		modeComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		final GridLayout modeLayout = new GridLayout(1, false);
		modeLayout.marginWidth = 0;
		modeLayout.marginHeight = 0;
		modeComposite.setLayout(modeLayout);

		this.modeCombo = new CCombo(modeComposite, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.FLAT | SWT.BORDER);
		this.modeCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		final Composite panelContainer = new Composite(composite, SWT.NONE);
		panelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final StackLayout stackLayout = new StackLayout();
		panelContainer.setLayout(stackLayout);

		final List<Control> panels = new ArrayList<>();
		for (final CompletionMode mode : CompletionMode.values()) {
			this.modeCombo.add(mode.name());
			panels.add(this.subPagesByMode.get(mode).createContents(panelContainer));
		}

		this.modeCombo.select(0);
		stackLayout.topControl = panels.get(0);
		panelContainer.layout();

		this.modeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final int index = this.modeCombo.getSelectionIndex();
			if (index >= 0 && index < panels.size()) {
				stackLayout.topControl = panels.get(index);
				panelContainer.layout();
			}
		}));

		return composite;
	}

	@Override
	protected void performDefaults() {
		for (final ContextPreferenceSubPage subPage : this.subPagesByMode.values()) {
			subPage.performDefaults();
		}
	}

	@Override
	public boolean performOk() {
		for (final ContextPreferenceSubPage subPage : this.subPagesByMode.values()) {
			subPage.performOk();
		}
		return true;
	}

	private CompletionMode getSelectedMode() {
		if (this.modeCombo == null) {
			return CompletionMode.values()[0];
		}
		final int index = this.modeCombo.getSelectionIndex();
		if (index >= 0 && index < CompletionMode.values().length) {
			return CompletionMode.values()[index];
		}
		return CompletionMode.values()[0];
	}

	private void openSubPreferencePage(String contextPrefix) {
		final String pageId = SUB_PAGE_ID_BY_CONTEXT_PREFIX.get(contextPrefix);
		if (pageId == null) {
			return;
		}
		if (getContainer() instanceof IWorkbenchPreferenceContainer container) {
			container.openPage(pageId, getSelectedMode());
		}
	}

	public static class ContextPreferenceSubPage {

		private final CompletionMode mode;
		private final ContextPreferencePage preferencePage;
		private CheckboxTableViewer tableViewer;
		private List<ContextTypePositionItem> contextTypeItems;
		private Button upButton;
		private Button downButton;
		private Button configureButton;

		public ContextPreferenceSubPage(CompletionMode mode, ContextPreferencePage preferencePage) {
			this.mode = mode;
			this.preferencePage = preferencePage;
		}

		public void init(IWorkbench workbench) {
			final Map<String, ContextTypePositionItem> preferenceItemByPrefix = ContextPreferences.get(this.mode).getContextTypePositionByPrefix();
			final Map<String, ContextTypePositionItem> calculatedItemByPrefix = new HashMap<>(preferenceItemByPrefix);
			for (final String prefix : Context.DEFAULT_PREFIX_ORDER) {
				if (!calculatedItemByPrefix.containsKey(prefix)) {
					final boolean enabled = Context.DEFAULT_ACTIVE_PREFIXES.contains(prefix) && preferenceItemByPrefix.isEmpty();
					calculatedItemByPrefix.put(prefix, new ContextTypePositionItem(prefix, enabled, calculatedItemByPrefix.size() + 1));
				}
			}
			this.contextTypeItems = new ArrayList<>(calculatedItemByPrefix.values());
			this.contextTypeItems.sort((a, b) -> Integer.compare(a.position(), b.position()));
		}

		protected Control createContents(Composite parent) {
			final Composite composite = new Composite(parent, SWT.NONE);
			final GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.horizontalSpacing = 0;
			layout.verticalSpacing = 0;
			layout.marginTop = 10;
			composite.setLayout(layout);

			final Label descLabel = new Label(composite, SWT.WRAP);
			descLabel.setText("Enable/disable context types and set their order in the prompt");
			final GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
			descData.widthHint = 400;
			descLabel.setLayoutData(descData);

			this.tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
			this.tableViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					if (((ContextTypePositionItem) event.getElement()).prefix().equals(FillInMiddleContextEntry.PREFIX)) {
						Display.getDefault().asyncExec(() -> ContextPreferenceSubPage.this.tableViewer.setChecked(event.getElement(), true));
					}
				}
			});
			final Table table = this.tableViewer.getTable();
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
			table.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(event -> openSelectedSubPage()));

			final GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
			tableData.heightHint = 300;
			table.setLayoutData(tableData);

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
					return ((ContextTypePositionItem) element).prefix().equals(FillInMiddleContextEntry.PREFIX) ? new Color(null, 240, 240, 240) : null;
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
					return ((ContextTypePositionItem) element).prefix().equals(FillInMiddleContextEntry.PREFIX) ? new Color(null, 240, 240, 240) : null;
				}
			});

			this.tableViewer.setContentProvider(ArrayContentProvider.getInstance());
			this.tableViewer.setInput(this.contextTypeItems);

			final List<ContextTypePositionItem> checkedItems = this.contextTypeItems.stream()
					.filter(ContextTypePositionItem::enabled)
					.toList();
			this.tableViewer.setCheckedElements(checkedItems.toArray());

			final Composite buttonComposite = new Composite(composite, SWT.NONE);
			buttonComposite.setLayout(new GridLayout(1, false));
			buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

			this.configureButton = new Button(buttonComposite, SWT.PUSH);
			this.configureButton.setText("Configure");
			this.configureButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			this.configureButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> openSelectedSubPage()));

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
			final int newIndex = currentIndex + direction;

			if (newIndex >= 0 && newIndex < this.contextTypeItems.size()) {
				final ContextTypePositionItem otherItem = this.contextTypeItems.get(newIndex);
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
				this.upButton.setEnabled(index > 0);
				this.downButton.setEnabled(index < this.contextTypeItems.size() - 1);
				this.configureButton.setEnabled(true);
			} else {
				this.upButton.setEnabled(false);
				this.downButton.setEnabled(false);
				this.configureButton.setEnabled(false);
			}
		}

		private void openSelectedSubPage() {
			final IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
			if (selection.isEmpty()) {
				return;
			}
			final ContextTypePositionItem selectedItem = (ContextTypePositionItem) selection.getFirstElement();
			this.preferencePage.openSubPreferencePage(selectedItem.prefix());
		}

		public boolean performOk() {
			final Object[] checkedElements = this.tableViewer.getCheckedElements();
			final List<ContextTypePositionItem> checkedItems = Arrays.asList(Arrays.copyOf(checkedElements, checkedElements.length, ContextTypePositionItem[].class));
			ContextPreferences.get(this.mode).setContextTypePositions(this.contextTypeItems.stream().map(item -> item.withEnabled(checkedItems.contains(item))).toList());
			return true;
		}

		protected void performDefaults() {
			this.contextTypeItems = new ArrayList<>(Context.DEFAULT_PREFIX_ORDER.stream()
					.map(prefix -> new ContextTypePositionItem(prefix, Context.DEFAULT_ACTIVE_PREFIXES.contains(prefix), Context.DEFAULT_PREFIX_ORDER.indexOf(prefix) + 1)).toList());
			this.contextTypeItems.sort((a, b) -> Integer.compare(a.position(), b.position()));
			this.tableViewer.setInput(this.contextTypeItems);
			this.tableViewer.setCheckedElements(this.contextTypeItems.stream()
					.filter(ContextTypePositionItem::enabled)
					.toArray());
		}
	}
}
