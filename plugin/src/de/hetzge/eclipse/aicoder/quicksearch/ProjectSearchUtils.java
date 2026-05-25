package de.hetzge.eclipse.aicoder.quicksearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.GitUtils;
import de.hetzge.eclipse.aicoder.util.GitUtils.GitState;

/**
 * High-performance regex search utility for Eclipse projects. Respects .gitignore patterns and supports parallel searching.
 */
public final class ProjectSearchUtils {

	private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final Set<String> BINARY_EXTENSIONS = Set.of(
			"jar", "class", "exe", "dll", "so", "dylib", "zip", "tar", "gz",
			"png", "jpg", "jpeg", "gif", "ico", "pdf", "doc", "docx", "xls",
			"xlsx", "ppt", "pptx", "mp3", "mp4", "avi", "mov", "war", "ear");

	private ProjectSearchUtils() {
		// Utility class - no instantiation
	}

	// ==================== Public API ====================

	/**
	 * Search for a regex pattern in all non-ignored files of a project. This is a blocking call.
	 *
	 * @param project The Eclipse project to search
	 * @param options Search options including regex pattern
	 * @param monitor Progress monitor (can be null)
	 * @return List of search results
	 * @throws CoreException if project access fails
	 */
	public static List<SearchResult> search(IProject project, SearchOptions options, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		try (GitState gitIgnoreMatcher = GitUtils.getGitState(project)) {
			final List<IFile> filesToSearch = collectSearchableFiles(project, gitIgnoreMatcher, options);

			monitor.beginTask("Searching project...", filesToSearch.size());

			try {
				return searchFilesParallel(filesToSearch, options, monitor);
			} finally {
				monitor.done();
			}
		} catch (final IOException exception) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to get git state", exception));
		}
	}

	/**
	 * Search asynchronously and return results via callback.
	 *
	 * @param project  The Eclipse project to search
	 * @param options  Search options
	 * @param callback Callback for receiving results
	 * @return A Job that can be used to monitor/cancel the search
	 */
	public static Job searchAsync(IProject project, SearchOptions options,
			SearchResultCallback callback) {
		final Job job = new Job("Searching project: " + project.getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final List<SearchResult> results = search(project, options, monitor);
					if (!monitor.isCanceled()) {
						callback.onComplete(results);
					}
					return Status.OK_STATUS;
				} catch (final CoreException exception) {
					callback.onError(exception);
					return new Status(IStatus.ERROR, "com.yourplugin", "Search failed", exception);
				}
			}
		};
		job.setUser(true);
		job.schedule();
		return job;
	}

	/**
	 * Quick search that returns only file matches (not line details). Faster for just finding files containing a pattern.
	 */
	public static List<IFile> findFilesContaining(IProject project, String regex, IProgressMonitor monitor) throws CoreException {
		final SearchOptions options = SearchOptions.builder(regex).maxResults(1).build();
		try (GitState matcher = GitUtils.getGitState(project)) {
			final List<IFile> files = collectSearchableFiles(project, matcher, options);

			return files.parallelStream()
					.filter(file -> fileContainsPattern(file, options.getPattern()))
					.collect(Collectors.toList());
		} catch (final IOException exception) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to get git state", exception));
		}
	}

	// ==================== Internal Implementation ====================

	/**
	 * Collect all files that should be searched (respecting gitignore and options).
	 */
	private static List<IFile> collectSearchableFiles(IProject project,
			GitState gitIgnoreMatcher,
			SearchOptions options) throws CoreException {
		final List<IFile> files = new ArrayList<>();
		collectFilesRecursive(project, gitIgnoreMatcher, options, files);
		return files;
	}

	private static void collectFilesRecursive(IContainer container,
			GitState matcher,
			SearchOptions options,
			List<IFile> files) throws CoreException {
		for (final IResource resource : container.members()) {
			// Skip ignored resources
			if (matcher.isIgnored(resource)) {
				continue;
			}

			if (resource instanceof IFile) {
				final IFile file = (IFile) resource;
				if (shouldSearchFile(file, options)) {
					files.add(file);
				}
			} else if (resource instanceof IContainer) {
				collectFilesRecursive((IContainer) resource, matcher, options, files);
			}
		}
	}

	private static boolean shouldSearchFile(IFile file, SearchOptions options) {
		String extension = file.getFileExtension();
		if (extension != null) {
			extension = extension.toLowerCase();
		}

		// Check binary files
		if (!options.searchBinaryFiles() && isBinaryFile(extension)) {
			return false;
		}

		// Check file size
		try {
			final IFileInfo info = file.getLocation().toFile() != null ? EFS.getStore(file.getLocationURI()).fetchInfo() : null;
			if (info != null && info.getLength() > options.getMaxFileSizeBytes()) {
				return false;
			}
		} catch (final Exception exception) {
			// Continue if we can't get file info
		}

		// Check included extensions
		final Set<String> included = options.getIncludedExtensions();
		if (included != null && !included.isEmpty()) {
			return extension != null && included.contains(extension);
		}

		// Check excluded extensions
		final Set<String> excluded = options.getExcludedExtensions();
		if (excluded != null && !excluded.isEmpty()) {
			return extension == null || !excluded.contains(extension);
		}

		// Check file pattern
		final Pattern filePattern = options.getFilePattern();
		if (filePattern != null && !filePattern.matcher(file.getName()).find()) {
			return false;
		}

		return true;
	}

	private static boolean isBinaryFile(String extension) {
		return extension != null && BINARY_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Search files in parallel using a thread pool.
	 */
	private static List<SearchResult> searchFilesParallel(List<IFile> files,
			SearchOptions options,
			IProgressMonitor monitor) {
		if (files.isEmpty()) {
			return Collections.emptyList();
		}

		final List<SearchResult> allResults = new CopyOnWriteArrayList<>();
		final AtomicInteger resultCount = new AtomicInteger(0);
		final int maxResults = options.getMaxResults();

		final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		final List<Future<?>> futures = new ArrayList<>();

		try {
			for (final IFile file : files) {
				if (monitor.isCanceled() || resultCount.get() >= maxResults) {
					break;
				}

				futures.add(executor.submit(() -> {
					if (monitor.isCanceled() || resultCount.get() >= maxResults) {
						return;
					}

					try {
						final List<SearchResult> fileResults = searchFile(file, options.getPattern());

						for (final SearchResult result : fileResults) {
							if (resultCount.incrementAndGet() <= maxResults) {
								allResults.add(result);
							} else {
								break;
							}
						}
					} catch (final Exception exception) {
						// Log but continue with other files
						exception.printStackTrace();
					}

					synchronized (monitor) {
						monitor.worked(1);
					}
				}));
			}

			// Wait for all tasks to complete
			for (final Future<?> future : futures) {
				try {
					future.get(30, TimeUnit.SECONDS);
				} catch (final TimeoutException e) {
					future.cancel(true);
				} catch (InterruptedException | ExecutionException e) {
					// Continue
				}
			}
		} finally {
			executor.shutdownNow();
		}

		return new ArrayList<>(allResults);
	}

	/**
	 * Search a single file for the pattern.
	 */
	private static List<SearchResult> searchFile(IFile file, Pattern pattern) {
		final List<SearchResult> results = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getContents(), getCharset(file)))) {

			String line;
			int lineNumber = 0;

			while ((line = reader.readLine()) != null) {
				lineNumber++;
				final Matcher matcher = pattern.matcher(line);

				while (matcher.find()) {
					results.add(new SearchResult(
							file.getFullPath().makeRelativeTo(file.getProject().getWorkspace().getRoot().getFullPath()),
							lineNumber,
							matcher.start(),
							matcher.end(),
							line,
							matcher.group()));
				}
			}
		} catch (CoreException | IOException e) {
			// Unable to read file - skip it
		}

		return results;
	}

	private static boolean fileContainsPattern(IFile file, Pattern pattern) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getContents(), getCharset(file)))) {

			String line;
			while ((line = reader.readLine()) != null) {
				if (pattern.matcher(line).find()) {
					return true;
				}
			}
		} catch (CoreException | IOException e) {
			// Unable to read file
		}
		return false;
	}

	private static String getCharset(IFile file) {
		try {
			return file.getCharset();
		} catch (final CoreException e) {
			return StandardCharsets.UTF_8.name();
		}
	}

	// ==================== Callback Interface ====================

	@FunctionalInterface
	public interface SearchResultCallback {
		void onComplete(List<SearchResult> results);

		default void onError(Exception e) {
			e.printStackTrace();
		}
	}
}