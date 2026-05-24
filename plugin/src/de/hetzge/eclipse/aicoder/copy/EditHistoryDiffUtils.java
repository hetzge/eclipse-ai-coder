package de.hetzge.eclipse.aicoder.copy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

	public static List<String> getDiffs(Duration duration) throws Exception {
		return getLastEditedFiles().stream()
				.filter(it -> it.getLocalTimeStamp() > System.currentTimeMillis() - duration.toMillis())
				.flatMap(LambdaExceptionUtils.rethrowFunction(it -> createDiffs(it, duration).stream()))
				.toList();
	}

	/**
	 * Creates a list of diffs for the given file. The diffs are created between the current content of the file and the content of the file at the given duration ago. The diffs are created for each state of the file that was modified within the given duration. The diffs are returned in reverse order, so the most recent diff is last.
	 */
	public static List<String> createDiffs(IFile file, Duration duration) throws UnsupportedEncodingException, IOException, CoreException, Exception {
		final IFileState[] states = file.getHistory(new NullProgressMonitor());
		if (states.length == 0) {
			return List.of();
		}
		final List<String> list = Stream.concat(Stream.of(getFileContent(file)), Arrays.stream(states)
				.filter(it -> it.getModificationTime() > System.currentTimeMillis() - duration.toMillis())
				.map(LambdaExceptionUtils.rethrowFunction(it -> new String(it.getContents().readAllBytes(), file.getCharset()))))
				.toList();
		return IntStream.range(0, list.size() - 1)
				.mapToObj(i -> DiffUtils.diff(list.get(i + 1), list.get(i)))
				.toList()
				.reversed();
	}

	/**
	 * Creates a diff of the given file. The diff is created between the current content of the file and the content of the file at the given duration ago.
	 */
	public static String createDiff(IFile file, Duration duration) throws CoreException, UnsupportedEncodingException, IOException {
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
