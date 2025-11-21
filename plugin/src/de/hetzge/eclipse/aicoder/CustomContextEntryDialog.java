package de.hetzge.eclipse.aicoder;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.context.CustomContextEntry;
import de.hetzge.eclipse.aicoder.context.CustomContextEntryData;

public class CustomContextEntryDialog extends Dialog {

	private Text titleText;
	private Text contentText;
	private Text globText;

	private String title;
	private String content;
	private String glob;

	private final CustomContextEntry existingEntry;

	public CustomContextEntryDialog(Shell parentShell, CustomContextEntry existingEntry) {
		super(parentShell);
		this.existingEntry = existingEntry;
		if (existingEntry != null) {
			final CustomContextEntryData data = existingEntry.getData();
			this.title = data.getTitle();
			this.content = data.getContent();
			this.glob = data.getGlob();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		// Title
		final Label titleLabel = new Label(container, SWT.NONE);
		titleLabel.setText("Title:");

		this.titleText = new Text(container, SWT.BORDER);
		this.titleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (this.title != null) {
			this.titleText.setText(this.title);
		}

		// Glob pattern
		final Label globLabel = new Label(container, SWT.NONE);
		globLabel.setText("Glob Pattern:");

		this.globText = new Text(container, SWT.BORDER);
		this.globText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (this.glob != null) {
			this.globText.setText(this.glob);
		}

		// Content
		final Label contentLabel = new Label(container, SWT.NONE);
		contentLabel.setText("Content:");
		contentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		this.contentText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		final GridData contentData = new GridData(GridData.FILL_BOTH);
		contentData.heightHint = 200; // Reasonable default height
		this.contentText.setLayoutData(contentData);
		if (this.content != null) {
			this.contentText.setText(this.content);
		}

		return container;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(this.existingEntry != null ? "Edit Custom Context Entry" : "New Custom Context Entry");
		shell.setMinimumSize(600, 400); // Set a wider minimum size
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Save", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	protected void okPressed() {
		// Save the values
		this.title = this.titleText.getText().trim();
		this.content = this.contentText.getText();
		this.glob = this.globText.getText().trim();

		super.okPressed();
	}

	public CustomContextEntryData createEntry() {
		final UUID id = this.existingEntry != null ? this.existingEntry.getData().getId() : UUID.randomUUID();
		return new CustomContextEntryData(
				id,
				this.existingEntry != null ? this.existingEntry.getData().getChildren() : List.of(),
				this.title,
				this.content,
				this.glob);
	}
}