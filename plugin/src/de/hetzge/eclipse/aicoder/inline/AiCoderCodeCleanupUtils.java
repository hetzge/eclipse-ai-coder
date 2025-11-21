package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class AiCoderCodeCleanupUtils {

	private AiCoderCodeCleanupUtils() {
	}

	public static void triggerSaveActions(ICompilationUnit compilationUnit) throws OperationCanceledException, CoreException {
		final IEditorPart activeEditor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.getActiveEditor();

		Position cursorPosition = null;
		IDocument document = null;

		if (activeEditor instanceof ITextEditor) {
			final ITextEditor textEditor = (ITextEditor) activeEditor;
			document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
			final ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();

			// Create a tracked position
			cursorPosition = new Position(selection.getOffset(), 0);
			try {
				document.addPosition(cursorPosition);
			} catch (final BadLocationException exception) {
				AiCoderActivator.getDefault().getLog().log(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to add cursor position", exception));
			}
		}

		try {
			final CleanUpOptions options = new CleanUpOptions();
			options.setOption(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpOptions.TRUE);
			options.setOption(CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpOptions.TRUE);

			final ICleanUp[] cleanUps = JavaPlugin.getDefault().getCleanUpRegistry().createCleanUps();
			for (final ICleanUp cleanUp : cleanUps) {
				if (cleanUp instanceof AbstractCleanUp) {
					((AbstractCleanUp) cleanUp).setOptions(options);
				}
			}
			final CleanUpRefactoring refactoring = new CleanUpRefactoring();
			refactoring.addCompilationUnit(compilationUnit);
			for (final ICleanUp cleanUp : cleanUps) {
				refactoring.addCleanUp(cleanUp);
			}
			executeRefactoring(refactoring);
		} finally {
			// Restore cursor position
			if (cursorPosition != null && activeEditor instanceof ITextEditor) {
				final ITextEditor textEditor = (ITextEditor) activeEditor;
				final int newOffset = cursorPosition.getOffset();
				textEditor.selectAndReveal(newOffset, 0);
				textEditor.setFocus();
				document.removePosition(cursorPosition);
			}
		}
	}

	private static void executeRefactoring(Refactoring refactoring) throws OperationCanceledException, CoreException {
		final RefactoringStatus initialStatus = refactoring.checkInitialConditions(new NullProgressMonitor());
		if (initialStatus.hasFatalError()) {
			return;
		}
		final RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
		if (finalStatus.hasFatalError()) {
			return;
		}
		final PerformRefactoringOperation operation = new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		operation.run(new NullProgressMonitor());
	}

}
