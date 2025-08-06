package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public class InstructionSelector extends Composite {
	private final Composite inputComposite;
	private final Text input;
	private final Table table;
	private final Consumer<Instruction> onSelect;
	private List<Instruction> instructions;
	private final Composite tableComposite;

	public InstructionSelector(Composite parent, Consumer<Instruction> onSelect) {
		super(parent, SWT.NONE);
		this.onSelect = onSelect;
		this.instructions = List.of();
		setLayout(new GridLayout(1, false));
		this.inputComposite = new Composite(this, SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		this.inputComposite.setLayout(layout);
		this.input = new Text(this.inputComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		final Button applyButton = new Button(this.inputComposite, SWT.NONE);
		applyButton.addListener(SWT.Selection, event -> applyCustomPrompt());
		this.input.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.keyCode == SWT.CR && (event.stateMask & SWT.CTRL) != 0) {
				applyCustomPrompt();
			}
		}));
		applyButton.setEnabled(false);
		this.input.addModifyListener(event -> {
			applyButton.setEnabled(!this.input.getText().isBlank());
			refreshTable();
		});
		this.input.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).create());
		applyButton.setImage(AiCoderActivator.getImage(AiCoderImageKey.RUN_ICON));
		applyButton.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, true).hint(35, SWT.DEFAULT).create());
		this.tableComposite = new Composite(this, SWT.NONE);
		final TableColumnLayout tableColumnLayout = new TableColumnLayout();
		this.tableComposite.setLayout(tableColumnLayout);
		this.table = new Table(this.tableComposite, SWT.SINGLE | SWT.FULL_SELECTION);
		tableColumnLayout.setColumnData(new TableColumn(this.table, SWT.NONE), new ColumnWeightData(0, 100));
		tableColumnLayout.setColumnData(new TableColumn(this.table, SWT.NONE), new ColumnWeightData(100, 100));
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
				final Instruction instruction = (Instruction) this.table.getSelection()[0].getData();
				this.input.setText(instruction.content());
				this.input.setFocus();
				this.input.setSelection(InstructionSelector.this.input.getText().length());
			}
		}));
		this.table.addMouseListener(MouseListener.mouseDoubleClickAdapter(event -> handleTableSelection()));
		this.addTraverseListener(event -> {
			if (event.detail == SWT.TRAVERSE_RETURN) {
				handleTableSelection();
			}
		});
		this.input.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.keyCode == SWT.ARROW_DOWN && this.table.getItemCount() > 0) {
				this.table.setFocus();
				this.table.setSelection(0);
				event.doit = false;
			}
		}));
	}

	public Text getSearchInput() {
		return this.input;
	}

	public void setInstructions(List<Instruction> instructions) {
		this.instructions = instructions;
		refreshTable();
	}

	private void applyCustomPrompt() {
		this.onSelect.accept(new Instruction("Custom", this.input.getText()));
	}

	private void handleTableSelection() {
		if (this.table.getSelectionCount() == 1) {
			final Instruction instruction = (Instruction) this.table.getSelection()[0].getData();
			this.onSelect.accept(instruction);
		}
	}

	private void refreshTable() {
		this.table.removeAll();
		for (final Instruction instruction : this.instructions) {
			final String query = this.input.getText().toLowerCase().trim();
			if (!query.isBlank() && (!instruction.title().toLowerCase().contains(query)
					&& !instruction.content().toLowerCase().contains(query))) {
				continue;
			}
			final TableItem item = new TableItem(this.table, SWT.NONE);
			item.setData(instruction);
			item.setText(0, instruction.title());
			item.setText(1, instruction.content());
		}
		if (this.table.getItemCount() > 0) {
			this.table.select(0);
			this.inputComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 75).create());
			this.tableComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		} else {
			this.inputComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).create());
			this.tableComposite.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).hint(0, 0).create());
		}
		this.layout();
	}
}
