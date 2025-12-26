package de.hetzge.eclipse.aicoder.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public final class DiffUtils {

	private DiffUtils() {
	}

	public static String diff(String oldContent, String newContent) {
		return diff(oldContent.lines().toList(), newContent.lines().toList());
	}

	public static String diff(List<String> oldList, List<String> newList) {
		final int m = oldList.size();
		final int n = newList.size();

		// Build LCS matrix (O(m*n))
		final int[][] lcs = new int[m + 1][n + 1];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				lcs[i + 1][j + 1] = oldList.get(i).equals(newList.get(j)) ? lcs[i][j] + 1 : Math.max(lcs[i][j + 1], lcs[i + 1][j]);
			}
		}

		// Backtrack to produce diff hunks
		final StringBuilder stringBuilder = new StringBuilder();
		int i = m, j = n;
		while (i > 0 || j > 0) {
			if (i > 0 && j > 0 && oldList.get(i - 1).equals(newList.get(j - 1))) {
				stringBuilder.insert(0, " " + oldList.get(i - 1) + "\n");
				i--;
				j--;
			} else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
				stringBuilder.insert(0, "+" + newList.get(j - 1) + "\n");
				j--;
			} else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
				stringBuilder.insert(0, "-" + oldList.get(i - 1) + "\n");
				i--;
			}
		}

		return stringBuilder.toString();
	}

	public static void openDiff(String content, String previousContent) {
		if (content == null || previousContent == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "No content to compare");
			return;
		}
		CompareUI.openCompareDialog(new LocalHistoryCompareEditorInput(content, previousContent));
	}

	public static void openDiff(ITextViewer parentTextViewer, String newContent) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			final CompareConfiguration compareConfiguration = new CompareConfiguration();
			compareConfiguration.setLeftEditable(true);
			compareConfiguration.setLeftLabel("Proposal");
			compareConfiguration.setRightLabel("Your code");
			compareConfiguration.setRightEditable(true);
			compareConfiguration.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.valueOf(true));
			final CompareEditorInput editorInput = new CompareEditorInput(compareConfiguration) {
				@Override
				protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					return new DiffNode(new CompareItem(newContent), new EditableCompareItem(parentTextViewer.getDocument()));
				}
			};
			CompareUI.openCompareDialog(editorInput);
		});
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

		@Override
		public String getType() {
			return ITypedElement.TEXT_TYPE;
		}
	}

	private static class EditableCompareItem extends CompareItem implements IEditableContent {
		private String content;
		private final IDocument document;

		public EditableCompareItem(IDocument document) {
			super(document.get());
			this.content = document.get();
			this.document = document;
		}

		@Override
		public boolean isEditable() {
			return true;
		}

		@Override
		public void setContent(byte[] newContent) {
			this.content = new String(newContent);
			this.document.set(this.content);
		}

		@Override
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			return null;
		}
	}

}