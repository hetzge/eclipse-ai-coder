package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class GeneralPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public GeneralPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("General AI Coder settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		// General settings group
		final Group generalGroup = new Group(getFieldEditorParent(), SWT.NONE);
		generalGroup.setText("General");
		generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Multiline completion setting
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_MULTILINE_KEY,
				"Enable multiline completion",
				generalGroup));

		// Autocomplete settings
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_AUTOCOMPLETE_KEY,
				"Enable autocomplete",
				generalGroup));

		addField(new BooleanFieldEditor(
				AiCoderPreferences.ONLY_ON_CHANGE_AUTOCOMPLETE_KEY,
				"Only on change autocomplete",
				generalGroup));

		// Ignore jre/jdk classes
		final BooleanFieldEditor ignoreJreClassesEditor = new BooleanFieldEditor(
				AiCoderPreferences.IGNORE_JRE_CLASSES_KEY,
				"Ignore JRE classes",
				generalGroup);
		addField(ignoreJreClassesEditor);

		// Cleanup code on apply
		addField(new BooleanFieldEditor(
				AiCoderPreferences.CLEANUP_CODE_ON_APPLY_KEY,
				"Cleanup code on apply",
				generalGroup));

		// Context size settings
		final IntegerFieldEditor maxPrefixSizeEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_PREFIX_SIZE_KEY,
				"Maximum prefix size (lines):",
				generalGroup);
		maxPrefixSizeEditor.setValidRange(0, 10000);
		addField(maxPrefixSizeEditor);

		final IntegerFieldEditor maxSuffixSizeEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_SUFFIX_SIZE_KEY,
				"Maximum suffix size (lines):",
				generalGroup);
		maxSuffixSizeEditor.setValidRange(0, 10000);
		addField(maxSuffixSizeEditor);

		final IntegerFieldEditor maxTokensEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_TOKENS_KEY,
				"Maximum tokens:",
				generalGroup);
		maxTokensEditor.setValidRange(0, 10000);
		addField(maxTokensEditor);

		final IntegerFieldEditor debounceInMsEditor = new IntegerFieldEditor(
				AiCoderPreferences.DEBOUNCE_IN_MS_KEY,
				"Debounce (in ms):",
				generalGroup);
		debounceInMsEditor.setValidRange(0, 10000);
		addField(debounceInMsEditor);
	}
}
