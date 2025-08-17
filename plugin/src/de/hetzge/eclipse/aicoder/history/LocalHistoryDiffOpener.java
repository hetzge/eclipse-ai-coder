package de.hetzge.eclipse.aicoder.history;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public final class LocalHistoryDiffOpener {

	private LocalHistoryDiffOpener() {
	}

	public static void openDiff(AiCoderHistoryEntry historyEntry) {
		final String content = historyEntry.getContent();
		final String previousContent = historyEntry.getPreviousContent();
		if (content == null || previousContent == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "No content to compare");
			return;
		}
		CompareUI.openCompareDialog(new LocalHistoryCompareEditorInput(content, previousContent));
	}

	private static final class LocalHistoryCompareEditorInput extends CompareEditorInput {
		private final String content;
		private final String previousContent;

		public LocalHistoryCompareEditorInput(String content, String previousContent) {
			super(new CompareConfiguration());
			this.content = content;
			this.previousContent = previousContent;
			final CompareConfiguration config = getCompareConfiguration();
			config.setLeftLabel("New");
			config.setRightLabel("Old");
		}

		@Override
		protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {
			final ITypedElement left = new CompareItem(this.content);
			final ITypedElement right = new CompareItem(this.previousContent);
			return new DiffNode(left, right);
		}
	}

	private static class CompareItem implements IStreamContentAccessor, ITypedElement, IModificationDate {
		private final String contents;

		public CompareItem(String contents) {
			this.contents = contents;
		}

		@Override
		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(this.contents.getBytes());
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public long getModificationDate() {
			return 0L;
		}

		@Override
		public String getName() {
			return "";
		}

		public String getString() {
			return this.contents;
		}

		@Override
		public String getType() {
			return ITypedElement.TEXT_TYPE;
		}
	}
}