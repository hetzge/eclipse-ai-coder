package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.hetzge.eclipse.aicoder.llm.LlmModelOption;
import de.hetzge.eclipse.aicoder.mcp.McpClients;

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
		this.instructionSelector.setInstructions(Stream.concat(
				McpClients.INSTANCE.getAllEditInstructions().stream(),
				List.of(
						new EditInstruction("Complete", "Fix/complete the code", "Fix/complete the code"),
						new EditInstruction("Modernize", "Modernize the code", "Modernize the code"),
						new EditInstruction("Explain", "Document each step", "Document each step of the code with a meaningful comment. Provide interesting insights and hints."),
						new EditInstruction("Names", "Use better variable names", "Use better variable names"),
						new EditInstruction("For-loop", "Convert to for loop", "Convert to for loop"),
						new EditInstruction("Stream", "Convert to stream", "Convert to stream"),
						new EditInstruction("Monads", "Use monadic abstraction", "Make the code more readable by using monadic code (map, flatMap...) if possible")).stream())
				.toList());
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
