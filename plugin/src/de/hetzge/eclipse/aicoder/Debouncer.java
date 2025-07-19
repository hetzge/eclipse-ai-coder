package de.hetzge.eclipse.aicoder;

import java.time.Duration;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Display;

/**
 * SWT-safe debouncer: only the <em>last</em> runnable that arrives within <delay> ms will actually be executed.
 *
 * Not thread-safe – must be called from the SWT UI thread.
 */
public final class Debouncer {

	private final Display display;
	private final Supplier<Duration> delaySupplier;

	private Runnable pending; // last runnable scheduled

	public Debouncer(Display display, Supplier<Duration> delaySupplier) {
		if (display == null) {
			throw new IllegalArgumentException("Display is null");
		}
		this.display = display;
		this.delaySupplier = delaySupplier;
	}

	/**
	 * Schedule a new runnable; the previously-scheduled one (if any) is cancelled and replaced by this one.
	 */
	public void debounce(Runnable runnable) {
		// SWT’s timerExec uses '== runnable' identity, so we can cancel the old and
		// replace it safely.
		if (this.pending != null) {
			this.display.timerExec(-1, this.pending); // cancel old – no-op if already fired
		}
		this.pending = () -> {
			this.pending = null; // so we don’t hold references
			runnable.run();
		};
		this.display.timerExec((int) this.delaySupplier.get().toMillis(), this.pending);
	}
}