package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.AiCoderResultView;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;

public final class AgentTrajectoryMessageComposite extends Composite {

	public AgentTrajectoryMessageComposite(Composite parent, LlmMessage message) {
		super(parent, SWT.NONE);
		setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		final boolean assistant = "ASSISTANT".equals(message.role().name());
		final Color background = new Color(Display.getDefault(), assistant ? 232 : 245, assistant ? 241 : 247, assistant ? 255 : 250);
		final Color accent = new Color(Display.getDefault(), assistant ? 70 : 110, assistant ? 110 : 120, assistant ? 180 : 130);
		final Font roleFont = new Font(Display.getDefault(), new FontData("Segoe UI", 9, SWT.BOLD));
		final Font contentFont = new Font(Display.getDefault(), new FontData("Segoe UI", 10, SWT.NORMAL));
		final Font reasoningFont = new Font(Display.getDefault(), new FontData("Segoe UI", 9, SWT.ITALIC));

		addDisposeListener(event -> {
			background.dispose();
			accent.dispose();
			roleFont.dispose();
			contentFont.dispose();
			reasoningFont.dispose();
		});

		final GridLayout outerLayout = new GridLayout(1, false);
		outerLayout.marginTop = 6;
		outerLayout.marginBottom = 6;
		outerLayout.marginLeft = assistant ? 70 : 10;
		outerLayout.marginRight = assistant ? 10 : 70;
		outerLayout.marginWidth = 0;
		outerLayout.marginHeight = 0;
		setLayout(outerLayout);

		final Composite bubble = new Composite(this, SWT.NONE);
		bubble.setBackground(background);
		final GridData bubbleData = new GridData(SWT.FILL, SWT.TOP, true, false);
		bubbleData.horizontalIndent = assistant ? 20 : 0;
		bubble.setLayoutData(bubbleData);

		final GridLayout bubbleLayout = new GridLayout(1, false);
		bubbleLayout.marginTop = 10;
		bubbleLayout.marginBottom = 10;
		bubbleLayout.marginLeft = 14;
		bubbleLayout.marginRight = 14;
		bubbleLayout.verticalSpacing = 6;
		bubble.setLayout(bubbleLayout);

		final Label roleLabel = new Label(bubble, SWT.NONE);
		roleLabel.setText(message.role().name());
		roleLabel.setFont(roleFont);
		roleLabel.setForeground(accent);
		roleLabel.setBackground(background);
		roleLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		if (message.reasoning() != null && !message.reasoning().isBlank()) {
			final Text reasoningText = createText(bubble, message.reasoning(), background, reasoningFont);
			reasoningText.setForeground(new Color(Display.getDefault(), 105, 105, 115));
			reasoningText.addDisposeListener(event -> reasoningText.getForeground().dispose());
		}

		if (message.content() != null && !message.content().isBlank()) {
			createText(bubble, message.content(), background, contentFont);

			if (assistant) {
				final Button openButton = new Button(bubble, SWT.PUSH);
				openButton.setText("Open in Result View");
				openButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
				openButton.addListener(SWT.Selection, event -> {
					AiCoderResultView.setContent(message.content());
					AiCoderResultView.openView();
				});
			}
		}

		for (final LlmToolCallRequest toolCallRequest : message.toolCallRequest()) {
			final Composite toolComposite = new Composite(bubble, SWT.NONE);
			toolComposite.setBackground(new Color(Display.getDefault(), 255, 255, 255));
			toolComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			final GridLayout toolLayout = new GridLayout(1, false);
			toolLayout.marginTop = 6;
			toolLayout.marginBottom = 6;
			toolLayout.marginLeft = 10;
			toolLayout.marginRight = 10;
			toolComposite.setLayout(toolLayout);

			final Label toolLabel = new Label(toolComposite, SWT.NONE);
			toolLabel.setText("TOOL  " + toolCallRequest.functionName());
			toolLabel.setFont(roleFont);
			toolLabel.setForeground(accent);
			toolLabel.setBackground(toolComposite.getBackground());
			toolLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			final Text toolText = createText(toolComposite, toolCallRequest.arguments().toString(), toolComposite.getBackground(), contentFont);
			toolText.setForeground(new Color(Display.getDefault(), 70, 70, 75));
			toolText.addDisposeListener(event -> toolText.getForeground().dispose());
		}
	}

	private static Text createText(Composite parent, String text, Color background, Font font) {
		final Text result = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		result.setText(text);
		result.setFont(font);
		result.setBackground(background);
		result.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		return result;
	}
}