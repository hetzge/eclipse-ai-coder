package de.hetzge.eclipse.aicoder.skill;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.MarkdownUtils;

/**
 * Dialog that shows the body of a skill. A toggle switches between the plain markdown text
 * and the rendered html representation of the markdown body.
 */
public class SkillDetailDialog extends Dialog {

	private final Skill skill;
	private Composite contentComposite;
	private StackLayout stackLayout;
	private Text markdownText;
	private Browser browser;
	private Button htmlToggleButton;

	public SkillDetailDialog(Shell parentShell, Skill skill) {
		super(parentShell);
		this.skill = skill;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		final String description = this.skill.description() != null ? this.skill.description() : "";
		final Label headerLabel = new Label(container, SWT.WRAP);
		headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		headerLabel.setText(String.format("%s - %s%n%s", this.skill.key(), this.skill.title(), description));

		this.htmlToggleButton = new Button(container, SWT.CHECK);
		this.htmlToggleButton.setText("Render as HTML");
		this.htmlToggleButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		this.htmlToggleButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			if (this.htmlToggleButton.getSelection()) {
				showRenderedHtml();
			} else {
				showMarkdown();
			}
		}));

		this.contentComposite = new Composite(container, SWT.NONE);
		this.contentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.stackLayout = new StackLayout();
		this.contentComposite.setLayout(this.stackLayout);

		this.markdownText = new Text(this.contentComposite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.markdownText.setText(body());

		this.stackLayout.topControl = this.markdownText;
		this.contentComposite.layout(true, true);

		return container;
	}

	private String body() {
		final String body = this.skill.skillMd().body();
		return body != null ? body : "";
	}

	private void showMarkdown() {
		if (this.browser != null && !this.browser.isDisposed()) {
			this.browser.setVisible(false);
		}
		if (this.markdownText != null && !this.markdownText.isDisposed()) {
			this.markdownText.setVisible(true);
			this.stackLayout.topControl = this.markdownText;
			this.contentComposite.layout(true, true);
		}
	}

	private void showRenderedHtml() {
		if (this.browser == null || this.browser.isDisposed()) {
			try {
				this.browser = new Browser(this.contentComposite, SWT.NONE);
				this.browser.setVisible(false);
			} catch (final RuntimeException | Error exception) {
				this.browser = null;
				AiCoderActivator.log().error("Failed to create browser for skill detail", exception);
			}
		}
		if (this.browser == null) {
			// No browser widget available on this platform - fall back to the markdown text.
			this.htmlToggleButton.setSelection(false);
			showMarkdown();
			return;
		}
		this.stackLayout.topControl = this.browser;
		this.browser.setVisible(true);
		this.markdownText.setVisible(false);
		this.contentComposite.layout(true, true);
		try {
			this.browser.setText(toHtml(body()), true);
		} catch (final RuntimeException | Error exception) {
			AiCoderActivator.log().error("Failed to render skill html", exception);
			this.browser.setVisible(false);
			this.browser.dispose();
			this.browser = null;
			this.htmlToggleButton.setSelection(false);
			showMarkdown();
		}
	}

	private static String toHtml(String markdown) {
		final String body = MarkdownUtils.markdownToHtml(markdown);
		return """
				<!DOCTYPE html>
				<html>
				<head>
				<meta charset="UTF-8">
				<style>
					body {
						font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
						font-size: 14px;
						line-height: 1.6;
						color: #1f2328;
						background-color: #ffffff;
						margin: 16px;
					}
					h1, h2, h3, h4, h5, h6 {
						margin-top: 24px;
						margin-bottom: 16px;
						font-weight: 600;
						line-height: 1.25;
					}
					h1 { font-size: 2em; border-bottom: 1px solid #d1d9e0; padding-bottom: 0.3em; }
					h2 { font-size: 1.5em; border-bottom: 1px solid #d1d9e0; padding-bottom: 0.3em; }
					h3 { font-size: 1.25em; }
					h4 { font-size: 1em; }
					a { color: #0969da; text-decoration: none; }
					a:hover { text-decoration: underline; }
					code {
						font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace;
						font-size: 85%;
						background-color: #eff1f3;
						padding: 0.2em 0.4em;
						border-radius: 6px;
					}
					pre {
						background-color: #f6f8fa;
						border-radius: 6px;
						padding: 16px;
						overflow: auto;
						line-height: 1.45;
					}
					pre code {
						background-color: transparent;
						padding: 0;
						font-size: 100%;
					}
					blockquote {
						margin: 0;
						padding: 0 1em;
						color: #59636e;
						border-left: 0.25em solid #d1d9e0;
					}
					table {
						border-collapse: collapse;
						width: 100%;
					}
					th, td {
						border: 1px solid #d1d9e0;
						padding: 6px 13px;
					}
					th {
						background-color: #f6f8fa;
						font-weight: 600;
					}
					img { max-width: 100%; }
					hr {
						height: 0.25em;
						padding: 0;
						margin: 24px 0;
						background-color: #d1d9e0;
						border: 0;
					}
				</style>
				</head>
				<body>
				%s
				</body>
				</html>
				""".replace("%s", body);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.skill.title());
		newShell.setMinimumSize(600, 400);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}