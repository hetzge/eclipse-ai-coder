package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class InstructionPopupDialog extends PopupDialog {

	private InstructionSelector instructionSelector;
	private final Consumer<Instruction> onSelect;

	public InstructionPopupDialog(Shell parent, Consumer<Instruction> onSelect) {
		super(parent, SWT.NONE, true, false, false, false, false, null, null);
		this.onSelect = onSelect;
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		this.instructionSelector = new InstructionSelector(container, instruction -> {
			close();
			this.onSelect.accept(instruction);
		});
		this.instructionSelector.setInstructions(List.of(
				new Instruction("Complete", "Fix/complete the code"),
				new Instruction("Modernize", "Modernize the code"),
				new Instruction("Explain", "Document each step of the code with a meaningful comment. Provide interesting insights and hints."),
				new Instruction("Names", "Use better variable names"),
				new Instruction("For-loop", "Convert to for loop"),
				new Instruction("Stream", "Convert to stream"),
				new Instruction("Monads", "Make the code more readable by using monadic code (map, flatMap...) if possible")));
		return container;
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		parent.getShell().setText("Select instruction");
		return super.createTitleControl(parent);
	}

	@Override
	protected Control getFocusControl() {
		return this.instructionSelector.getSearchInput();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 300);
	}
}
