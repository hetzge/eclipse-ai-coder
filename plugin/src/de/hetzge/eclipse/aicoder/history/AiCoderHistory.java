package de.hetzge.eclipse.aicoder.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class AiCoderHistory {

	private static final int HISTORY_LIMIT = 100;

	private final HistoryDatabase database;
	private final List<HistoryEntry> historyEntries;

	public AiCoderHistory(HistoryDatabase database) {
		this.historyEntries = Collections.synchronizedList(new ArrayList<>());
		this.database = database;
	}

	public void loadHistoryEntries() {
		try {
			final List<HistoryEntry> persistedEntries = this.database.loadHistoryEntries(HISTORY_LIMIT);
			this.historyEntries.addAll(persistedEntries);
		} catch (final RuntimeException exception) {
			AiCoderActivator.log().error("Failed to load history entries", exception);
		}
	}

	public List<HistoryEntry> getCurrentHistoryEntries() {
		return this.historyEntries;
	}

	public void saveHistoryEntry(HistoryEntry entry) {
		this.database.save(entry);
		if (!this.historyEntries.contains(entry)) {
			this.historyEntries.add(0, entry); // Add to the beginning of the list
			if (this.historyEntries.size() > HISTORY_LIMIT) { // TODO max preference
				this.historyEntries.removeLast();
			}
			refreshWholeHistoryView();
		} else {
			refreshSingleHistoryEntry(entry);
		}
	}

	public void deleteHistoryEntry(HistoryEntry entry) {
		this.database.delete(entry.getId());
		this.historyEntries.remove(entry);
		refreshWholeHistoryView();
	}

	private void refreshWholeHistoryView() {
		AiCoderHistoryView.get().ifPresent(view -> {
			Display.getDefault().asyncExec(() -> {
				view.refreshHistory();
			});
		});
	}

	private void refreshSingleHistoryEntry(HistoryEntry entry) {
		AiCoderHistoryView.get().ifPresent(view -> {
			Display.getDefault().asyncExec(() -> {
				view.refreshHistory(entry);
			});
		});
	}
}
