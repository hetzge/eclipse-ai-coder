package de.hetzge.eclipse.aicoder.llm;

import java.util.Optional;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public final class LlmSelector extends Composite {
	private LlmOption llmModelOption;

	public LlmSelector(Composite parent, int style, LlmOption initialLlmModelOption, Runnable callback) {
		super(parent, style);
		this.llmModelOption = initialLlmModelOption;
		setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).create());
		final Button modelButton = new Button(this, SWT.PUSH);
		modelButton.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, 35).create());
		modelButton.setText(initialLlmModelOption != null ? initialLlmModelOption.getLabel() : "Select model");
		modelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Window.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				this.llmModelOption = optionOptional.get();
				modelButton.setText(this.llmModelOption.getLabel());
				callback.run();
			}
		}));
	}

	public Optional<LlmOption> getOption() {
		return Optional.ofNullable(this.llmModelOption);
	}
}
