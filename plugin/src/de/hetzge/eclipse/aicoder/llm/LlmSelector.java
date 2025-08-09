package de.hetzge.eclipse.aicoder.llm;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public final class LlmSelector extends Composite {
	private List<LlmModelOption> options;
	private final Combo modelCombo;

	public LlmSelector(Composite parent, int style) {
		super(parent, style);
		setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).create());
		this.modelCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
		this.modelCombo.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, 35).create());
		this.options = List.of();
	}

	public Optional<LlmModelOption> getSelectedOption() {
		final int selectionIndex = this.modelCombo.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= this.options.size()) {
			return Optional.empty();
		}
		return Optional.of(this.options.get(selectionIndex));
	}

	public void init(LlmModelOption option) {
		this.modelCombo.setText("Loading...");
		new Job("Load llm models") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				LlmSelector.this.options = LlmModels.INSTANCE.getOptions();
				Display.getDefault().syncExec(() -> {
					if (LlmSelector.this.isDisposed()) {
						return;
					}
					LlmSelector.this.modelCombo.setItems(LlmSelector.this.options.stream().map(LlmModelOption::getLabel).toList().toArray(new String[0]));
					if (option != null) {
						final int index = LlmSelector.this.options.indexOf(option);
						if (index >= 0) {
							LlmSelector.this.modelCombo.select(index);
						}
					}
				});
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public void addSelectionListener(SelectionListener listener) {
		this.modelCombo.addSelectionListener(listener);
	}
}
