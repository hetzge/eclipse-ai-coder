package de.hetzge.eclipse.aicoder.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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

	public static Diff diff(String oldContent, String newContent) {
		final List<String> oldList = oldContent.lines().toList();
		final List<String> newList = newContent.lines().toList();

		final List<String> oldListWithLineending = Arrays.asList(oldContent.split("(?<=\\n)")); // not using .lines() here to keep \r\n line endings
		final List<String> newListWithLineending = Arrays.asList(newContent.split("(?<=\\n)")); // not using .lines() here to keep \r\n line endings

		final int m = oldList.size();
		final int n = newList.size();
		final int[][] lcs = new int[m + 1][n + 1];

		for (int i = m - 1; i >= 0; i--) {
			for (int j = n - 1; j >= 0; j--) {
				lcs[i][j] = oldList.get(i).equals(newList.get(j))
						? lcs[i + 1][j + 1] + 1
						: Math.max(lcs[i + 1][j], lcs[i][j + 1]);
			}
		}

		final StringBuilder patchBuilder = new StringBuilder();
		final List<Change> changes = new java.util.ArrayList<>();
		int oldIndex = 0;
		int newIndex = 0;
		int added = 0;
		int removed = 0;

		while (oldIndex < m || newIndex < n) {
			if (oldIndex < m && newIndex < n && oldList.get(oldIndex).equals(newList.get(newIndex))) {
				patchBuilder.append(' ').append(oldList.get(oldIndex)).append('\n');
				oldIndex++;
				newIndex++;
				continue;
			}

			final int startLine = oldIndex;
			String oldChangeContent = "";
			String newChangeContent = "";

			while (oldIndex < m || newIndex < n) {
				if (oldIndex < m && newIndex < n && oldList.get(oldIndex).equals(newList.get(newIndex))) {
					break;
				}

				if (newIndex < n && (oldIndex == m || lcs[oldIndex][newIndex + 1] >= lcs[oldIndex + 1][newIndex])) {
					patchBuilder.append('+').append(newList.get(newIndex)).append('\n');
					newChangeContent += newListWithLineending.get(newIndex);
					newIndex++;
					added++;
				} else {
					patchBuilder.append('-').append(oldList.get(oldIndex)).append('\n');
					oldChangeContent += oldListWithLineending.get(oldIndex);
					oldIndex++;
					removed++;
				}
			}

			changes.add(new Change(startLine, newChangeContent, oldChangeContent));
		}

		return new Diff(patchBuilder.toString(), changes, added, removed);
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

	public static record Change(int startLine, String newContent, String oldContent) {
	}

	public static record Diff(String patch, List<Change> changes, int added, int removed) {
	}

}