package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.MarkdownUtils;

/**
 * View that renders provided markdown content in a webview and offers a toolbar action to copy the original markdown into the clipboard.
 * The last {@value #MAX_OPENED_MARKDOWNS} opened markdowns are kept in a history and can be navigated with the previous/next toolbar actions.
 */
public class AiCoderResultView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.AiCoderResultView";

	private static final int MAX_OPENED_MARKDOWNS = 20;
	private static final List<String> openedMarkdowns = new ArrayList<>();
	private static volatile String currentMarkdown = "";
	private static int currentMarkdownIndex = -1;

	private Browser browser;
	private StyledText fallbackText;
	private Action previousAction;
	private Action nextAction;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		try {
			this.browser = new Browser(parent, SWT.NONE);
		} catch (final RuntimeException | Error exception) {
			// Fall back to a plain text widget if no browser widget is available on this platform.
			this.browser = null;
			this.fallbackText = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
		}
		render(currentMarkdown);
		contributeToActionBars();
	}

	@Override
	public void setFocus() {
		if (this.browser != null) {
			this.browser.setFocus();
		} else if (this.fallbackText != null) {
			this.fallbackText.setFocus();
		}
	}

	@Override
	public void dispose() {
		this.browser = null;
		this.fallbackText = null;
		this.previousAction = null;
		this.nextAction = null;
		super.dispose();
	}

	private void contributeToActionBars() {
		final IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		final Action previousAction = new Action("Previous") {
			@Override
			public void run() {
				showPrevious();
			}
		};
		previousAction.setToolTipText("Show previous markdown");
		previousAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.PREVIOUS_ICON));
		previousAction.setEnabled(hasPrevious());
		toolBarManager.add(previousAction);
		this.previousAction = previousAction;

		final Action nextAction = new Action("Next") {
			@Override
			public void run() {
				showNext();
			}
		};
		nextAction.setToolTipText("Show next markdown");
		nextAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.NEXT_ICON));
		nextAction.setEnabled(hasNext());
		toolBarManager.add(nextAction);
		this.nextAction = nextAction;

		final Action copyAction = new Action("Copy") {
			@Override
			public void run() {
				copyContentToClipboard();
			}
		};
		copyAction.setToolTipText("Copy markdown content to the clipboard");
		copyAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.COPY_ICON));
		toolBarManager.add(copyAction);
	}

	private void copyContentToClipboard() {
		final Shell shell = getViewSite().getShell();
		final Clipboard clipboard = new Clipboard(shell.getDisplay());
		try {
			clipboard.setContents(new Object[] { currentMarkdown }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
		MessageDialog.openInformation(shell, "AI Coder Result", "Markdown content copied to the clipboard.");
	}

	private void render(String markdown) {
		final String content = markdown != null ? markdown : "";
		if (this.browser != null && !this.browser.isDisposed()) {
			try {
				this.browser.setText(toHtml(content), true);
			} catch (final RuntimeException | Error exception) {
				// Ignore browser rendering failures and fall back to the plain text widget if present.
			}
		}
		if (this.fallbackText != null && !this.fallbackText.isDisposed()) {
			this.fallbackText.setText(content);
		}
	}

	/**
	 * Sets the markdown content shown by the view. If the view is already open the content is updated immediately, otherwise it is stored and rendered once the view is opened.
	 */
	public static void setContent(String content) {
		final String normalizedContent = content != null ? content : "";
		addToOpenedMarkdowns(normalizedContent);
		currentMarkdown = normalizedContent;
		EclipseUtils.asyncExec(() -> {
			findView().ifPresent(view -> {
				view.render(currentMarkdown);
				view.updateNavigationActions();
			});
		});
	}

	/**
	 * Opens the result view and shows the currently set markdown content.
	 */
	public static void openView() {
		EclipseUtils.asyncExec(() -> {
			try {
				final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				final AiCoderResultView view = (AiCoderResultView) activePage.showView(ID);
				view.setFocus();
			} catch (final PartInitException exception) {
				throw new RuntimeException("Failed to open view", exception);
			}
		});
	}

	/**
	 * Sets the markdown content and opens the result view.
	 */
	public static void open(String content) throws PartInitException {
		setContent(content);
		openView();
	}

	public static Optional<AiCoderResultView> findView() {
		return PlatformUI.getWorkbench().getDisplay().syncCall(() -> {
			final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			return Optional.ofNullable(activePage.findView(ID)).map(AiCoderResultView.class::cast);
		});
	}

	private void showPrevious() {
		showHistoryEntry(currentMarkdownIndex - 1);
	}

	private void showNext() {
		showHistoryEntry(currentMarkdownIndex + 1);
	}

	private void showHistoryEntry(int index) {
		if (moveToHistoryIndex(index)) {
			render(currentMarkdown);
			updateNavigationActions();
		}
	}

	private void updateNavigationActions() {
		if (this.previousAction != null) {
			this.previousAction.setEnabled(hasPrevious());
		}
		if (this.nextAction != null) {
			this.nextAction.setEnabled(hasNext());
		}
	}

	private static boolean hasPrevious() {
		return currentMarkdownIndex > 0;
	}

	private static boolean hasNext() {
		return currentMarkdownIndex >= 0 && currentMarkdownIndex < openedMarkdowns.size() - 1;
	}

	private static synchronized void addToOpenedMarkdowns(String content) {
		if (currentMarkdownIndex >= 0 && currentMarkdownIndex < openedMarkdowns.size()
				&& Objects.equals(openedMarkdowns.get(currentMarkdownIndex), content)) {
			return;
		}
		// Drop the forward history when a new markdown is opened from a previous position.
		if (currentMarkdownIndex >= 0 && currentMarkdownIndex < openedMarkdowns.size()) {
			openedMarkdowns.subList(currentMarkdownIndex + 1, openedMarkdowns.size()).clear();
		}
		openedMarkdowns.add(content);
		if (openedMarkdowns.size() > MAX_OPENED_MARKDOWNS) {
			// Keep only the last MAX_OPENED_MARKDOWNS opened markdowns.
			final int removedCount = openedMarkdowns.size() - MAX_OPENED_MARKDOWNS;
			openedMarkdowns.subList(0, removedCount).clear();
		}
		currentMarkdownIndex = openedMarkdowns.size() - 1;
	}

	private static synchronized boolean moveToHistoryIndex(int index) {
		if (index < 0 || index >= openedMarkdowns.size()) {
			return false;
		}
		currentMarkdownIndex = index;
		currentMarkdown = openedMarkdowns.get(index);
		return true;
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
}
