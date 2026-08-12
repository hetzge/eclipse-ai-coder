package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class AgentPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public AgentPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder agent settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		final Group agentGroup = new Group(getFieldEditorParent(), SWT.NONE);
		agentGroup.setText("Agent");
		agentGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final IntegerFieldEditor searchToolResultLimitEditor = new IntegerFieldEditor(
				AiCoderPreferences.SEARCH_TOOL_RESULT_LIMIT_KEY,
				"Search tool result limit:",
				agentGroup);
		searchToolResultLimitEditor.setValidRange(1, 100000);
		addField(searchToolResultLimitEditor);

		final IntegerFieldEditor searchToolLineContentLengthEditor = new IntegerFieldEditor(
				AiCoderPreferences.SEARCH_TOOL_LINE_CONTENT_LENGTH_KEY,
				"Search tool line content length limit (in characters):",
				agentGroup);
		searchToolLineContentLengthEditor.setValidRange(1, 100000);
		addField(searchToolLineContentLengthEditor);

		final IntegerFieldEditor readFileDefaultMaxLineCountEditor = new IntegerFieldEditor(
				AiCoderPreferences.READ_FILE_DEFAULT_MAX_LINE_COUNT_KEY,
				"Read file default max line count:",
				agentGroup);
		readFileDefaultMaxLineCountEditor.setValidRange(1, 100000);
		addField(readFileDefaultMaxLineCountEditor);

		final IntegerFieldEditor maxAgentIterationsEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_AGENT_ITERATIONS_KEY,
				"Max agent iterations:",
				agentGroup);
		maxAgentIterationsEditor.setValidRange(1, 10000);
		addField(maxAgentIterationsEditor);

		final IntegerFieldEditor toolCallOutputLimitEditor = new IntegerFieldEditor(
				AiCoderPreferences.TOOL_CALL_OUTPUT_LIMIT_KEY,
				"Tool call output limit (in characters):",
				agentGroup);
		toolCallOutputLimitEditor.setValidRange(1, 10000000);
		addField(toolCallOutputLimitEditor);

		final IntegerFieldEditor trajectoryFontSizeEditor = new IntegerFieldEditor(
				AiCoderPreferences.TRAJECTORY_FONT_SIZE_KEY,
				"Trajectory font size:",
				agentGroup);
		trajectoryFontSizeEditor.setValidRange(1, 100);
		addField(trajectoryFontSizeEditor);
	}
}
