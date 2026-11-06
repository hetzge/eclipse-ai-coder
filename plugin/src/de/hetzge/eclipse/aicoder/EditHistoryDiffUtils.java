package de.hetzge.eclipse.aicoder;

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

import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.util.PatchUtils;

public final class EditHistoryDiffUtils {

	private EditHistoryDiffUtils() {
	}

	public static List<String> getDiffs(Duration duration) throws Exception {
		return getLastEditedFiles().stream()
				.filter(it -> it.getLocalTimeStamp() > System.currentTimeMillis() - duration.toMillis())
				.flatMap(LambdaExceptionUtils.rethrowFunction(it -> createDiffs(it, duration).stream()))
				.toList();
	}

	private static List<String> createDiffs(IFile file, Duration duration) throws UnsupportedEncodingException, IOException, CoreException, Exception {
		final IFileState[] states = file.getHistory(new NullProgressMonitor());
		if (states.length == 0) {
			return List.of();
		}
		final List<String> list = Stream.concat(Stream.of(EclipseUtils.readContentFromBufferOrFile(file.getWorkspace().getRoot(), file.getFullPath())), Arrays.stream(states)
				.filter(it -> it.getModificationTime() > System.currentTimeMillis() - duration.toMillis())
				.map(LambdaExceptionUtils.rethrowFunction(it -> new String(it.getContents().readAllBytes(), file.getCharset()))))
				.toList();
		return IntStream.range(0, list.size() - 1)
				.filter(i -> !list.get(i + 1).equals(list.get(i)))
				.mapToObj(i -> PatchUtils.createPatch(list.get(i + 1), list.get(i), file.getFullPath().toOSString(), file.getFullPath().toOSString(), 3))
				.toList()
				.reversed();
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
