package de.hetzge.eclipse.aicoder.inline;

import java.util.function.BiConsumer;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.content.InstructionUtils;
import de.hetzge.eclipse.aicoder.llm.LlmModelOption;

public class InstructionPopupDialog extends PopupDialog {

	private InstructionSelector instructionSelector;
	private final BiConsumer<EditInstruction, LlmModelOption> onSelect;

	public InstructionPopupDialog(Shell parent, BiConsumer<EditInstruction, LlmModelOption> onSelect) {
		super(parent, SWT.NONE, true, false, false, false, false, null, null);
		this.onSelect = onSelect;
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		this.instructionSelector = new InstructionSelector(container, (instruction, llmModelOption) -> {
			close();
			this.onSelect.accept(instruction, llmModelOption);
		});
		this.instructionSelector.setInstructions(InstructionUtils.getAllEditInstructions());
		this.instructionSelector.addPaintListener(event -> {
			final Rectangle clientArea = this.instructionSelector.getClientArea();
			event.gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY));
			event.gc.setLineWidth(1);
			event.gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);
		});
		return container;
	}

	@Override
	protected Control getFocusControl() {
		return this.instructionSelector.getSearchInput();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}
}
