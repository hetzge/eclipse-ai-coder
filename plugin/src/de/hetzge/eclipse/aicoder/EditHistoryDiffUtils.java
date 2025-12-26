package de.hetzge.eclipse.aicoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INavigationHistory;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.aicoder.util.DiffUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public final class EditHistoryDiffUtils {

	private EditHistoryDiffUtils() {
	}

	public static String getDiff(Duration duration) throws Exception {
		return getLastEditedFiles().stream()
				.filter(it -> it.getLocalTimeStamp() > System.currentTimeMillis() - duration.toMillis())
				.map(LambdaExceptionUtils.rethrowFunction(it -> createDiff(it, duration)))
				.collect(Collectors.joining("\n"));
	}

	private static String createDiff(IFile file, Duration duration) throws CoreException, UnsupportedEncodingException, IOException {
		final IFileState[] states = file.getHistory(new NullProgressMonitor());
		if (states.length == 0) {
			return "";
		}
		IFileState state = states[0];
		for (int i = 0; i < states.length; i++) {
			if (states[i].getModificationTime() > System.currentTimeMillis() - duration.toMillis()) {
				state = states[i];
				break;
			}
		}
		final String a = new String(state.getContents().readAllBytes(), file.getCharset());
		final String b = getFileContent(file);
		final String pathString = file.getFullPath().toString();
		final String diff = DiffUtils.diff(a, b);
		return String.format("---%s\n+++%s\n@@\n%s\n", pathString, pathString, diff);
	}

	private static String getFileContent(IFile file) throws UnsupportedEncodingException, IOException, CoreException {
		return new String(file.getContents().readAllBytes(), file.getCharset());
	}

	private static List<IFile> getLastEditedFiles() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) {
			AiCoderActivator.log().warn("Workbench not available");
			return List.of();
		}
		final INavigationHistory history = EclipseUtils.getActiveWorkbenchPage().get().getNavigationHistory();
		if (history == null) {
			AiCoderActivator.log().warn("Navigation history not available");
			return List.of();
		}
		return Arrays.stream(history.getLocations())
				.map(INavigationLocation::getInput)
				.filter(input -> input != null && input instanceof IFileEditorInput)
				.map(input -> ((IFileEditorInput) input).getFile())
				.distinct()
				.sorted((a, b) -> Long.compare(b.getLocalTimeStamp(), a.getLocalTimeStamp()))
				.toList();
	}

}
