package de.hetzge.eclipse.aicoder.tool;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class TemporaryBuildUtils {

	private TemporaryBuildUtils() {
	}

	public static synchronized List<IProblem> buildWithModifiedSource(IProgressMonitor progressMonitor, IProject project, FileSystem fileSystem) throws Exception {
		final List<IProblem> problems = new ArrayList<>();
		final List<ICompilationUnit> units = new ArrayList<>();
		try {
			final WorkingCopyOwner owner = new WorkingCopyOwner() {
				@Override
				public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
					return new TemporaryBuildProblemRequestor(problems);
				}
			};
			final List<IPath> javaPaths = fileSystem.getChangedPaths().stream()
					.filter(path -> path.getFileExtension().equals("java"))
					.toList();
			for (final IPath path : javaPaths) {
				final IFile userFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				final ICompilationUnit originalCompilationUnit = JavaCore.createCompilationUnitFrom(userFile);
				final ICompilationUnit workingCopyUnit = originalCompilationUnit.getWorkingCopy(owner, progressMonitor);
				AiCoderActivator.log().info("Working copy unit: " + workingCopyUnit.getElementName());
				workingCopyUnit.getBuffer().setContents(fileSystem.getChangedContent(path));
				units.add(workingCopyUnit);
			}
			JavaCore.run(
					new IWorkspaceRunnable() {
						@Override
						public void run(IProgressMonitor monitor) throws CoreException {
							AiCoderActivator.log().info("Building project: " + project.getName());
							project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
						}
					},
					progressMonitor);
		} finally {
			for (final ICompilationUnit unit : units) {
				unit.discardWorkingCopy();
			}
		}
		return problems;
	}

	private static final class TemporaryBuildProblemRequestor implements IProblemRequestor {
		private final List<IProblem> problems;

		private TemporaryBuildProblemRequestor(List<IProblem> problems) {
			this.problems = problems;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void beginReporting() {
			AiCoderActivator.log().info("Begin reporting problems");
		}

		@Override
		public void acceptProblem(IProblem problem) {
			AiCoderActivator.log().info("Accept problem: " + problem.getMessage());
			this.problems.add(problem);
		}

		@Override
		public void endReporting() {
			AiCoderActivator.log().info("End reporting problems");
		}
	}
}
