package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;

/**
 * A {@link ListEditor} that stores its entries as a comma separated string.
 */
public class StringListFieldEditor extends ListEditor {

	public StringListFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	@Override
	protected String createList(String[] items) {
		return String.join(",", items);
	}

	@Override
	protected String getNewInputObject() {
		final InputDialog dialog = new InputDialog(getShell(), "Add entry", "Enter new entry:", "", null);
		if (dialog.open() == Window.OK) {
			final String value = dialog.getValue();
			return value == null || value.isBlank() ? null : value.trim();
		}
		return null;
	}

	@Override
	protected String[] parseString(String stringList) {
		if (stringList == null || stringList.isBlank()) {
			return new String[0];
		}
		return java.util.Arrays.stream(stringList.split(","))
				.map(String::trim)
				.filter(entry -> !entry.isEmpty())
				.toArray(String[]::new);
	}
}
