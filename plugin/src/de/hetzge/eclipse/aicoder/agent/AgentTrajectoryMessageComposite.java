package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.llm.LlmMessage;

public final class AgentTrajectoryMessageComposite extends Composite {

	private final LlmMessage message;

	public AgentTrajectoryMessageComposite(Composite parent, LlmMessage message) {
		super(parent, SWT.NONE);
		this.message = message;
		this.setLayout(new GridLayout(1, false));
		final Label roleLabel = new Label(this, SWT.NONE);
		roleLabel.setText(this.message.role().name());
		final Text contentText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		contentText.setText(this.message.content());
	}
}
