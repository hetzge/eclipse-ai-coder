package de.hetzge.eclipse.aicoder.preferences;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmProvider;
import de.hetzge.eclipse.aicoder.llm.LlmSelectorDialog;

public class LlmPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final int LABEL_WIDTH = 100;

	public LlmPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder LLM settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		final Group fillInMiddleModelGroup = new Group(getFieldEditorParent(), SWT.NONE);
		fillInMiddleModelGroup.setText("Fill in middle LLM");
		fillInMiddleModelGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final ComboFieldEditor fillInMiddleProviderEditor = new ComboFieldEditor(
				AiCoderPreferences.FILL_IN_MIDDLE_PROVIDER_KEY,
				"Provider:",
				getProviderEntryNamesAndValues(),
				fillInMiddleModelGroup);
		fillInMiddleProviderEditor.getLabelControl(fillInMiddleModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(fillInMiddleProviderEditor);
		final StringFieldEditor fillInMiddleModelEditor = new StringFieldEditor(
				AiCoderPreferences.FILL_IN_MIDDLE_MODEL_KEY,
				"Model:",
				fillInMiddleModelGroup);
		fillInMiddleModelEditor.getLabelControl(fillInMiddleModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(fillInMiddleModelEditor);
		final Button fillInMiddleModelButton = new Button(fillInMiddleModelGroup, SWT.PUSH);
		fillInMiddleModelButton.setText("Select LLM...");
		fillInMiddleModelButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		fillInMiddleModelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				final LlmOption llmOption = optionOptional.get();
				setComboProvider(fillInMiddleModelGroup, fillInMiddleProviderEditor, llmOption);
				fillInMiddleModelEditor.setStringValue(llmOption.modelKey());
			}
		}));

		final Group quickFixModelGroup = new Group(getFieldEditorParent(), SWT.NONE);
		quickFixModelGroup.setText("Quick fix LLM");
		quickFixModelGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final ComboFieldEditor quickFixProviderEditor = new ComboFieldEditor(
				AiCoderPreferences.QUICK_FIX_PROVIDER_KEY,
				"Provider:",
				getProviderEntryNamesAndValues(),
				quickFixModelGroup);
		quickFixProviderEditor.getLabelControl(quickFixModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(quickFixProviderEditor);
		final StringFieldEditor quickFixModelEditor = new StringFieldEditor(
				AiCoderPreferences.QUICK_FIX_MODEL_KEY,
				"Model:",
				quickFixModelGroup);
		quickFixModelEditor.getLabelControl(quickFixModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(quickFixModelEditor);
		final Button quickFixModelButton = new Button(quickFixModelGroup, SWT.PUSH);
		quickFixModelButton.setText("Select LLM...");
		quickFixModelButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		quickFixModelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				final LlmOption llmOption = optionOptional.get();
				setComboProvider(quickFixModelGroup, quickFixProviderEditor, llmOption);
				quickFixModelEditor.setStringValue(llmOption.modelKey());
			}
		}));

		final Group generateModelGroup = new Group(getFieldEditorParent(), SWT.NONE);
		generateModelGroup.setText("Generate LLM");
		generateModelGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final ComboFieldEditor generateProviderEditor = new ComboFieldEditor(
				AiCoderPreferences.GENERATE_PROVIDER_KEY,
				"Provider:",
				getProviderEntryNamesAndValues(),
				generateModelGroup);
		generateProviderEditor.getLabelControl(generateModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(generateProviderEditor);
		final StringFieldEditor generateModelEditor = new StringFieldEditor(
				AiCoderPreferences.GENERATE_MODEL_KEY,
				"Model:",
				generateModelGroup);
		generateModelEditor.getLabelControl(generateModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(generateModelEditor);
		final Button generateModelButton = new Button(generateModelGroup, SWT.PUSH);
		generateModelButton.setText("Select LLM...");
		generateModelButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		generateModelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				final LlmOption llmOption = optionOptional.get();
				setComboProvider(generateModelGroup, generateProviderEditor, llmOption);
				generateModelEditor.setStringValue(llmOption.modelKey());
			}
		}));

		final Group editModelGroup = new Group(getFieldEditorParent(), SWT.NONE);
		editModelGroup.setText("Edit LLM");
		editModelGroup.setLayout(new GridLayout(1, false));
		editModelGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final ComboFieldEditor editProviderEditor = new ComboFieldEditor(
				AiCoderPreferences.EDIT_PROVIDER_KEY,
				"Provider:",
				getProviderEntryNamesAndValues(),
				editModelGroup);
		editProviderEditor.getLabelControl(editModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(editProviderEditor);
		final StringFieldEditor editModelEditor = new StringFieldEditor(
				AiCoderPreferences.EDIT_MODEL_KEY,
				"Model:",
				editModelGroup);
		editModelEditor.getLabelControl(editModelGroup).setLayoutData(GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).create());
		addField(editModelEditor);
		final Button editModelButton = new Button(editModelGroup, SWT.PUSH);
		editModelButton.setText("Select LLM...");
		editModelButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		editModelButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final LlmSelectorDialog dialog = new LlmSelectorDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				final Optional<LlmOption> optionOptional = dialog.getResultOption();
				if (optionOptional.isEmpty()) {
					return;
				}
				final LlmOption llmOption = optionOptional.get();
				setComboProvider(editModelGroup, editProviderEditor, llmOption);
				editModelEditor.setStringValue(llmOption.modelKey());
			}
		}));
	}

	private void setComboProvider(final Group parent, final ComboFieldEditor editor, final LlmOption llmOption) {
		// Hack to access combo :(
		for (final Control children : editor.getLabelControl(parent).getParent().getChildren()) {
			if (children instanceof final Combo combo) {
				combo.select(llmOption.provider().ordinal());
				combo.notifyListeners(SWT.Selection, null);
				break;
			}
		}
	}

	private String[][] getProviderEntryNamesAndValues() {
		return Arrays.stream(LlmProvider.values())
				.map(provider -> new String[] { provider.name(), provider.name() })
				.toList().toArray(new String[0][]);
	}
}
