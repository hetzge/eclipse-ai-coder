package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;

public final class AgentTrajectoryErrorComposite extends Composite {

	public AgentTrajectoryErrorComposite(Composite parent, String errorMessage) {
		super(parent, SWT.NONE);
		setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		final int fontSize = AiCoderPreferences.getTrajectoryFontSize();
		final Color background = new Color(Display.getDefault(), 255, 235, 235);
		final Color accent = new Color(Display.getDefault(), 180, 60, 60);
		final Font roleFont = new Font(Display.getDefault(), new FontData("Segoe UI", fontSize, SWT.BOLD));
		final Font contentFont = new Font(Display.getDefault(), new FontData("Segoe UI", fontSize, SWT.NORMAL));

		addDisposeListener(event -> {
			background.dispose();
			accent.dispose();
			roleFont.dispose();
			contentFont.dispose();
		});

		final GridLayout layout = new GridLayout(1, false);
		layout.marginTop = 6;
		layout.marginBottom = 6;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		layout.marginWidth = 14;
		layout.marginHeight = 8;
		setLayout(layout);
		setBackground(background);

		final Label roleLabel = new Label(this, SWT.NONE);
		roleLabel.setText("ERROR");
		roleLabel.setFont(roleFont);
		roleLabel.setForeground(accent);
		roleLabel.setBackground(background);
		roleLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		if (errorMessage != null && !errorMessage.isBlank()) {
			final Text errorText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			errorText.setText(errorMessage);
			errorText.setFont(contentFont);
			errorText.setBackground(background);
			errorText.setForeground(new Color(Display.getDefault(), 120, 40, 40));
			errorText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			errorText.addDisposeListener(event -> errorText.getForeground().dispose());
		}
	}
}
