package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ContentPreviewDialog extends Dialog {

	private final String title;
	private final String content;

	public ContentPreviewDialog(Shell parentShell, String title, String content) {
		super(parentShell);
		this.title = title;
		this.content = content;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		final Text textArea = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		textArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		textArea.setText(this.content.toString());
		textArea.setEditable(false);
		final GridData gridData = (GridData) textArea.getLayoutData();
		gridData.widthHint = 600;
		gridData.heightHint = 400;
		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
