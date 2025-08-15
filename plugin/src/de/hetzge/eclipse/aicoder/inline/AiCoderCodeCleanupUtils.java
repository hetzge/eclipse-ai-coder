package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public final class AiCoderCodeCleanupUtils {

	private AiCoderCodeCleanupUtils() {
	}

	public static void triggerSaveActions(ICompilationUnit compilationUnit) throws OperationCanceledException, CoreException {
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
