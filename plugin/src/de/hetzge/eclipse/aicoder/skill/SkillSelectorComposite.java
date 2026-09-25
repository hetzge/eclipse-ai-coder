package de.hetzge.eclipse.aicoder.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public final class SkillSelectorComposite extends Composite {

	private final List<Skill> skills;
	private final Set<String> enabledSkillKeys;
	private final List<Consumer<Set<String>>> enablementListeners = new ArrayList<>();
	private final ViewerFilter skillFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!(element instanceof final Skill skill)) {
				return false;
			}
			final String filter = SkillSelectorComposite.this.filterText.getText().trim().toLowerCase(Locale.ROOT);
			if (filter.isEmpty()) {
				return true;
			}
			final String description = skill.description() != null ? skill.description().toLowerCase(Locale.ROOT) : "";
			return skill.key().toLowerCase(Locale.ROOT).contains(filter)
					|| skill.title().toLowerCase(Locale.ROOT).contains(filter)
					|| description.contains(filter);
		}
	};

	private CheckboxTableViewer viewer;
	private Text filterText;
	private Button selectAllButton;
	private Button deselectAllButton;

	public SkillSelectorComposite(Composite parent, int style, List<Skill> skills, Set<String> enabledSkillKeys) {
		super(parent, style);
		this.skills = new ArrayList<>(skills);
		this.enabledSkillKeys = new LinkedHashSet<>(enabledSkillKeys);
		createControls();
	}

	private void createControls() {
		final GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);

		createTopBar();
		createViewer();

		applyCheckedState();
		updateButtonStates();
	}

	private void createTopBar() {
		final Composite topBar = new Composite(this, SWT.NONE);
		topBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		final GridLayout topBarLayout = new GridLayout(3, false);
		topBarLayout.marginWidth = 0;
		topBarLayout.marginHeight = 0;
		topBarLayout.horizontalSpacing = 6;
		topBar.setLayout(topBarLayout);

		this.filterText = new Text(topBar, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		this.filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		this.filterText.setMessage("Filter skills (key, title, description)");
		this.filterText.addModifyListener(event -> {
			this.viewer.refresh();
			applyCheckedState();
			updateButtonStates();
		});

		this.selectAllButton = new Button(topBar, SWT.PUSH);
		this.selectAllButton.setText("Select All");
		this.selectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		this.selectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> selectAll()));

		this.deselectAllButton = new Button(topBar, SWT.PUSH);
		this.deselectAllButton.setText("Deselect All");
		this.deselectAllButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		this.deselectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> deselectAll()));
	}

	private void createViewer() {
		this.viewer = CheckboxTableViewer.newCheckList(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		final Table table = this.viewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		createColumn("Key", 160, Skill::key);
		createColumn("Title", 260, Skill::title);
		createColumn("Description", 400, skill -> skill.description() != null ? skill.description() : "");

		this.viewer.setContentProvider(ArrayContentProvider.getInstance());
		this.viewer.setInput(this.skills);
		this.viewer.addFilter(this.skillFilter);
		this.viewer.addCheckStateListener(event -> {
			final Skill skill = (Skill) event.getElement();
			if (event.getChecked()) {
				this.enabledSkillKeys.add(skill.key());
			} else {
				this.enabledSkillKeys.remove(skill.key());
			}
			notifyEnablementListeners();
			updateButtonStates();
		});
		this.viewer.addDoubleClickListener(event -> {
			final IStructuredSelection selection = this.viewer.getStructuredSelection();
			if (selection.getFirstElement() instanceof final Skill skill) {
				new SkillDetailDialog(this.viewer.getControl().getShell(), skill).open();
			}
		});
	}

	private void createColumn(String title, int width, Function<Skill, String> textProvider) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(this.viewer, SWT.NONE);
		viewerColumn.getColumn().setText(title);
		viewerColumn.getColumn().setWidth(width);
		viewerColumn.getColumn().setResizable(true);
		viewerColumn.getColumn().setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return textProvider.apply((Skill) element);
			}
		});
	}

	/**
	 * Enables all skills.
	 */
	public void selectAll() {
		this.enabledSkillKeys.clear();
		for (final Skill skill : this.skills) {
			this.enabledSkillKeys.add(skill.key());
		}
		applyCheckedState();
		notifyEnablementListeners();
		updateButtonStates();
	}

	/**
	 * Disables all skills.
	 */
	public void deselectAll() {
		this.enabledSkillKeys.clear();
		applyCheckedState();
		notifyEnablementListeners();
		updateButtonStates();
	}

	/**
	 * Applies the current {@link #enabledSkillKeys} to the checkbox column of the viewer. Must be called whenever rows appear/disappear (e.g. after filtering) so the checkboxes stay in sync with the authoritative enabled set.
	 */
	private void applyCheckedState() {
		this.viewer.setCheckedElements(this.skills.stream()
				.filter(skill -> this.enabledSkillKeys.contains(skill.key()))
				.toArray());
	}

	private void updateButtonStates() {
		final boolean allSelected = !this.skills.isEmpty() && this.enabledSkillKeys.size() == this.skills.size();
		final boolean noneSelected = this.enabledSkillKeys.isEmpty();
		this.selectAllButton.setEnabled(!allSelected);
		this.deselectAllButton.setEnabled(!noneSelected);
	}

	/**
	 * Returns all skills (independent of the enabled state).
	 */
	public List<Skill> getAllSkills() {
		return new ArrayList<>(this.skills);
	}

	/**
	 * Returns the skills that are currently enabled.
	 */
	public List<Skill> getSelectedSkills() {
		return this.skills.stream()
				.filter(skill -> this.enabledSkillKeys.contains(skill.key()))
				.toList();
	}

	/**
	 * Returns a copy of the currently enabled skill keys.
	 */
	public Set<String> getEnabledSkillKeys() {
		return new LinkedHashSet<>(this.enabledSkillKeys);
	}

	/**
	 * Registers a listener that is notified with a snapshot of the enabled skill keys whenever the enablement changes.
	 */
	public void addEnablementListener(Consumer<Set<String>> listener) {
		this.enablementListeners.add(listener);
	}

	public void removeEnablementListener(Consumer<Set<String>> listener) {
		this.enablementListeners.remove(listener);
	}

	private void notifyEnablementListeners() {
		final Set<String> copy = new LinkedHashSet<>(this.enabledSkillKeys);
		this.enablementListeners.forEach(listener -> listener.accept(copy));
	}

	@Override
	public boolean setFocus() {
		if (this.filterText != null && !this.filterText.isDisposed()) {
			return this.filterText.setFocus();
		}
		return super.setFocus();
	}
}