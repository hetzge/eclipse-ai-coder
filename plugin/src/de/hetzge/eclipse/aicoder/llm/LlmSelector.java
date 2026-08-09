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
	private Button modelButton;

	public LlmSelector(Composite parent, int style, LlmOption initialLlmModelOption, Runnable callback) {
		super(parent, style);
		setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).create());
		this.modelButton = new Button(this, SWT.PUSH);
		this.modelButton.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, 35).create());
		this.modelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Window.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				this.llmModelOption = optionOptional.get();
				this.modelButton.setText(this.llmModelOption.getLabel());
				callback.run();
			}
		}));
		setModelOption(initialLlmModelOption);
	}

	public void setModelOption(LlmOption llmModelOption) {
		this.llmModelOption = llmModelOption;
		this.modelButton.setText(llmModelOption != null ? llmModelOption.getLabel() : "Select model");
	}

	public Optional<LlmOption> getOption() {
		if (!hasValidOption()) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.llmModelOption);
	}

	public boolean hasValidOption() {
		return this.llmModelOption != null && this.llmModelOption.provider() != LlmProvider.NONE;
	}
}
