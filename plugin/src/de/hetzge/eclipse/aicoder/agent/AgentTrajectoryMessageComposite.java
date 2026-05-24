package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;

public final class AgentTrajectoryMessageComposite extends Composite {

	public AgentTrajectoryMessageComposite(Composite parent, LlmMessage message) {
		super(parent, SWT.NONE);
		setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		final GridLayout layout = new GridLayout(1, false);
		layout.marginRight = 50;
		this.setLayout(layout);
		final Label roleLabel = new Label(this, SWT.NONE);
		roleLabel.setText(message.role().name());
		if (message.reasoning() != null && !message.reasoning().isBlank()) {
			final Text reasoningText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
			reasoningText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			reasoningText.setText(message.reasoning());
		}
		final Text contentText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
		contentText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		contentText.setText(message.content());
		contentText.setBounds(10, 10, 100, 100);
		for (final LlmToolCallRequest toolCallRequest : message.toolCallRequest()) {
			final Label toolCallLabel = new Label(this, SWT.NONE);
			toolCallLabel.setText(toolCallRequest.functionName());
			final Text toolCallText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
			toolCallText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			toolCallText.setText(toolCallRequest.arguments().toString());
		}
	}
}
