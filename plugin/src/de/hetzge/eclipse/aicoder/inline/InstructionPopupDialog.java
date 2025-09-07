package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
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
import de.hetzge.eclipse.aicoder.llm.LlmOption;

public class InstructionPopupDialog extends PopupDialog {

	private InstructionSelector instructionSelector;
	private final List<EditInstruction> instructions;
	private final BiConsumer<EditInstruction, LlmOption> onSelect;
	private final Runnable onClose;

	public InstructionPopupDialog(Shell parent, List<EditInstruction> instructions, BiConsumer<EditInstruction, LlmOption> onSelect, Runnable onClose) {
		super(parent, SWT.NONE, true, false, false, false, false, null, null);
		this.instructions = instructions;
		this.onSelect = onSelect;
		this.onClose = onClose;
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		this.instructionSelector = new InstructionSelector(container, (instruction, llmModelOption) -> {
			close();
			this.onSelect.accept(instruction, llmModelOption);
		});
		this.instructionSelector.setInstructions(this.instructions);
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

	@Override
	public boolean close() {
		this.onClose.run();
		return super.close();
	}
}
