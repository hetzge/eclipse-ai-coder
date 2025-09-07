package de.hetzge.eclipse.aicoder;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.quickaccess.IQuickAccessComputer;
import org.eclipse.ui.quickaccess.QuickAccessElement;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.content.InstructionUtils;
import de.hetzge.eclipse.aicoder.inline.InlineCompletionController;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class AiCoderInstructionsQuickAccessComputer implements IQuickAccessComputer {

	@Override
	public QuickAccessElement[] computeElements() {
		final Optional<AbstractTextEditor> activeTextEditorOptional = EclipseUtils.getActiveTextEditor();
		if (activeTextEditorOptional.isEmpty()) {
			return null;
		}
		final ITextEditor editor = activeTextEditorOptional.get();
		final List<EditInstruction> instructions = InstructionUtils.resolve(EclipseUtils.getPath(editor).orElse(null));
		return instructions.stream()
				.map(InstructionQuickAccessElement::new)
				.toArray((length) -> new QuickAccessElement[length]);
	}

	@Override
	public void resetState() {
	}

	@Override
	public boolean needsRefresh() {
		return false;
	}

	private static final class InstructionQuickAccessElement extends QuickAccessElement {
		private final EditInstruction instruction;

		private InstructionQuickAccessElement(EditInstruction instruction) {
			this.instruction = instruction;
		}

		@Override
		public String getLabel() {
			return String.format("AI: %s - %s", this.instruction.key(), this.instruction.title());
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return AiCoderActivator.getImageDescriptor(AiCoderImageKey.RUN_ICON);
		}

		@Override
		public String getId() {
			return String.format("AI_CODER_INSTRUCTION:%s", this.instruction.key());
		}

		@Override
		public void execute() {
			AiCoderActivator.log().info("Execute instruction quick access");
			final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow();
			InlineCompletionController.setup(textEditor).trigger(this.instruction.content());
		}
	}
}
