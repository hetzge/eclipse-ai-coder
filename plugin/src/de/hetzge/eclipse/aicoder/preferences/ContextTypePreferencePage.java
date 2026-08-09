package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.context.Context;

public abstract class ContextTypePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final String contextPrefix;
	private CCombo modeCombo;

	public ContextTypePreferencePage(String contextPrefix) {
		this.contextPrefix = contextPrefix;
		final String contextTypeName = Context.CONTEXT_TYPE_NAME_BY_CONTEXT_PREFIX.getOrDefault(contextPrefix, contextPrefix);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Configure " + contextTypeName + " context");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 5;
		composite.setLayout(layout);

		this.modeCombo = new CCombo(composite, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.FLAT | SWT.BORDER);
		this.modeCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		for (final CompletionMode mode : CompletionMode.values()) {
			this.modeCombo.add(mode.name());
		}
		this.modeCombo.select(0);

		return composite;
	}

	public String getContextPrefix() {
		return this.contextPrefix;
	}

	public CompletionMode getSelectedMode() {
		if (this.modeCombo == null) {
			return CompletionMode.values()[0];
		}
		final int index = this.modeCombo.getSelectionIndex();
		if (index >= 0 && index < CompletionMode.values().length) {
			return CompletionMode.values()[index];
		}
		return CompletionMode.values()[0];
	}

	public void setSelectedMode(CompletionMode mode) {
		if (this.modeCombo != null) {
			this.modeCombo.select(mode.ordinal());
		}
	}

	@Override
	public void applyData(Object data) {
		if (data instanceof CompletionMode mode) {
			setSelectedMode(mode);
		}
	}
}
