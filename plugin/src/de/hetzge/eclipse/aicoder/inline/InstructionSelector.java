package de.hetzge.eclipse.aicoder.inline;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.Debouncer;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.llm.LlmModelOption;
import de.hetzge.eclipse.aicoder.llm.LlmSelector;

public class InstructionSelector extends Composite {
	private final Composite inputComposite;
	private final Text input;
	private final Table table;
	private final BiConsumer<EditInstruction, LlmModelOption> onSelect;
	private List<EditInstruction> instructions;
	private final Composite tableComposite;
	private Button applyButton;
	private LlmSelector llmSelector;
	private final Debouncer debouncer;

	public InstructionSelector(Composite parent, BiConsumer<EditInstruction, LlmModelOption> onSelect) {
		super(parent, SWT.NONE);
		this.onSelect = onSelect;
		this.instructions = List.of();
		this.debouncer = new Debouncer(getDisplay(), () -> Duration.ofMillis(250L));
		setLayout(new GridLayout(1, false));
		this.inputComposite = new Composite(this, SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		this.inputComposite.setLayout(layout);
		this.input = new Text(this.inputComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		this.applyButton = new Button(this.inputComposite, SWT.NONE);
		this.applyButton.addListener(SWT.Selection, event -> applyCustomPrompt());
		this.input.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.keyCode == SWT.CR && (event.stateMask & SWT.CTRL) != 0) {
				applyCustomPrompt();
			}
		}));
		this.input.addModifyListener(event -> {
			updateApplyButton();
			this.debouncer.debounce(() -> {
				Display.getDefault().syncExec(() -> {
					refreshTable();
				});
			});
		});
		this.input.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).create());
		this.applyButton.setImage(AiCoderActivator.getImage(AiCoderImageKey.RUN_ICON));
		this.applyButton.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, true).hint(35, SWT.DEFAULT).create());
		this.applyButton.setEnabled(false);
		this.tableComposite = new Composite(this, SWT.NONE);
		final TableColumnLayout tableColumnLayout = new TableColumnLayout();
		this.tableComposite.setLayout(tableColumnLayout);
		this.table = new Table(this.tableComposite, SWT.SINGLE | SWT.FULL_SELECTION);
		tableColumnLayout.setColumnData(new TableColumn(this.table, SWT.NONE), new ColumnWeightData(0, 300));
		tableColumnLayout.setColumnData(new TableColumn(this.table, SWT.NONE), new ColumnWeightData(100, 300));
		this.table.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.keyCode == SWT.ARROW_UP && InstructionSelector.this.table.getSelectionIndex() == 0) {
				this.input.setFocus();
				this.input.setSelection(InstructionSelector.this.input.getText().length());
			} else if (event.keyCode == SWT.ARROW_UP) {
				this.table.setSelection((this.table.getItemCount() + this.table.getSelectionIndex() - 1) % this.table.getItemCount());
				event.doit = false;
			} else if (event.keyCode == SWT.ARROW_DOWN) {
				this.table.setSelection((this.table.getSelectionIndex() + 1) % this.table.getItemCount());
				event.doit = false;
			}
			if (event.keyCode == SWT.ARROW_RIGHT) {
				final EditInstruction instruction = (EditInstruction) this.table.getSelection()[0].getData();
				this.input.setText(instruction.content());
				this.input.setFocus();
				this.input.setSelection(InstructionSelector.this.input.getText().length());
			}
		}));
		this.table.addMouseListener(MouseListener.mouseDoubleClickAdapter(event -> handleTableSelection()));
		this.table.addTraverseListener(event -> {
			if (event.detail == SWT.TRAVERSE_RETURN) {
				handleTableSelection();
			}
		});
		this.input.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (this.table.getItemCount() == 0) {
				return;
			}
			if (event.keyCode == SWT.ARROW_UP && this.input.getCaretPosition() == 0) {
				this.table.setFocus();
				this.table.setSelection(this.table.getItemCount() - 1);
				event.doit = false;
			} else if (event.keyCode == SWT.ARROW_DOWN && this.input.getCaretPosition() == this.input.getText().length()) {
				this.table.setFocus();
				this.table.setSelection(0);
				event.doit = false;
			}
		}));
		this.llmSelector = new LlmSelector(this, SWT.NONE, LlmModelOption.createEditModelOptionFromPreferences(), () -> {
			updateApplyButton();
		});
		this.llmSelector.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).create());
	}

	private void updateApplyButton() {
		this.applyButton.setEnabled(!this.input.getText().isBlank() && this.llmSelector.getOption().isPresent());
	}

	public Text getSearchInput() {
		return this.input;
	}

	public void setInstructions(List<EditInstruction> instructions) {
		this.instructions = instructions;
		refreshTable();
	}

	private void applyCustomPrompt() {
		final Optional<LlmModelOption> optionOptional = this.llmSelector.getOption();
		if (optionOptional.isEmpty()) {
			MessageDialog.openError(getShell(), "Error", "Please select a model first.");
			return;
		}
		final LlmModelOption option = optionOptional.get();
		this.onSelect.accept(new EditInstruction("Custom", "Custom", this.input.getText()), option);
	}

	private void handleTableSelection() {
		final Optional<LlmModelOption> llmModelOptionOptional = this.llmSelector.getOption();
		if (this.table.getSelectionCount() == 1 && llmModelOptionOptional.isPresent()) {
			final EditInstruction instruction = (EditInstruction) this.table.getSelection()[0].getData();
			this.onSelect.accept(instruction, llmModelOptionOptional.orElseThrow());
		}
	}

	private void refreshTable() {
		final String query = this.input.getText().trim();
		final int beforeItemCount = this.table.getItemCount();
		this.table.removeAll();
		for (final EditInstruction instruction : this.instructions) {
			if (!query.isBlank() && !instruction.match(query)) {
				continue;
			}
			final TableItem item = new TableItem(this.table, SWT.NONE);
			item.setData(instruction);
			item.setText(0, instruction.key() != null ? instruction.key() : "");
			item.setText(1, instruction.title() != null ? instruction.title() : "");
		}
		if (this.table.getItemCount() != beforeItemCount) {
			updateLayout();
		}
	}

	private void updateLayout() {
		if (this.table.getItemCount() > 0) {
			this.table.select(0);
			this.inputComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 110).create());
			this.tableComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		} else {
			this.inputComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).create());
			this.tableComposite.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).hint(0, 0).create());
		}
		this.layout();
	}
}
