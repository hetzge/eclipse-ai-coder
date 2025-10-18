package de.hetzge.eclipse.aicoder.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

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