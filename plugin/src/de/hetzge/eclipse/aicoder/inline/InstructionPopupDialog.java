package de.hetzge.eclipse.aicoder.inline;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.inline.InstructionSelector.InstructionSelection;

public class InstructionPopupDialog extends PopupDialog {

	private InstructionSelector instructionSelector;
	private final CompletionMode editCompletionMode;
	private final CompletionMode readOnlyCompletionMode;
	private final List<EditInstruction> instructions;
	private final String initial;
	private final Consumer<InstructionSelection> onSelect;
	private final Runnable onClose;

	public InstructionPopupDialog(Shell parent, CompletionMode editCompletionMode, CompletionMode readOnlyCompletionMode, List<EditInstruction> instructions, String initial, Consumer<InstructionSelection> onSelect, Runnable onClose) {
		super(parent, SWT.NONE, true, false, false, false, false, null, null);
		this.editCompletionMode = editCompletionMode;
		this.readOnlyCompletionMode = readOnlyCompletionMode;
		this.instructions = instructions;
		this.initial = initial;
		this.onSelect = onSelect;
		this.onClose = onClose;
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		this.instructionSelector = new InstructionSelector(container, this.editCompletionMode, this.readOnlyCompletionMode, this.instructions, this.initial, (selection) -> {
			close();
			this.onSelect.accept(selection);
		});
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
		return this.instructionSelector.getFocusControl();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	public boolean close() {
		try {
			AiCoderActivator.getDefault().getInstructionStorage().setLastInstruction(this.instructionSelector.getInstructionText());
		} catch (final IOException exception) {
			AiCoderActivator.log().error("Failed to save last instruction.", exception);
		}
		this.onClose.run();
		return super.close();
	}

	public void toggleQueryMode() {
		this.instructionSelector.toggleQueryMode();
	}
}
