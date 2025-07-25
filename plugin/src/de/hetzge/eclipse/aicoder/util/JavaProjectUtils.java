package de.hetzge.eclipse.aicoder.util;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class JavaProjectUtils {

	private JavaProjectUtils() {
	}

	public static Optional<IPackageFragment> findPackageFragment(IJavaProject javaProject, String packageString) throws JavaModelException, CoreException {
		for (final IPackageFragmentRoot roots : javaProject.getAllPackageFragmentRoots()) {
			final IPackageFragment packageFragment = roots.getPackageFragment(packageString);
			if (packageFragment.exists()) {
				return Optional.of(packageFragment);
			}
		}
		return Optional.empty();
	}

	public static Optional<IJavaProject> getCurrentJavaProject() throws CoreException {
		// Try to get from active editor first
		final IJavaProject project = getJavaProjectFromActiveEditor();
		if (project != null) {
			return Optional.of(project);
		}

		// Fall back to selection
		return Optional.ofNullable(getJavaProjectFromSelection());
	}

	private static IJavaProject getJavaProjectFromActiveEditor() throws CoreException {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null) {
			final IEditorPart editor = window.getActivePage().getActiveEditor();
			if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
				final IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
				final IFile file = input.getFile();
				return getJavaProject(file.getProject());
			}
		}
		return null;
	}

	private static IJavaProject getJavaProjectFromSelection() throws CoreException {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			final ISelection selection = window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				final Object firstElement = structuredSelection.getFirstElement();

				if (firstElement instanceof IJavaProject) {
					return (IJavaProject) firstElement;
				} else if (firstElement instanceof IAdaptable) {
					final IAdaptable adaptable = (IAdaptable) firstElement;
					final IResource resource = adaptable.getAdapter(IResource.class);
					if (resource != null) {
						return getJavaProject(resource.getProject());
					}
				}
			}
		}
		return null;
	}

	private static IJavaProject getJavaProject(IProject project) throws CoreException {
		if (project != null && project.exists() && project.isOpen()) {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				return JavaCore.create(project);
			}
		}
		return null;
	}
}