package de.hetzge.eclipse.aicoder.history;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mjson.Json;

public final class HistoryDatabase implements AutoCloseable {

	private final Path databasePath;
	private Connection connection;

	public HistoryDatabase(Path databasePath) {
		this.databasePath = databasePath;
	}

	public synchronized List<HistoryEntry> loadHistoryEntries(int limit) {
		try (PreparedStatement statement = connect().prepareStatement("SELECT id, document FROM history_entries ORDER BY created DESC LIMIT ?")) {
			statement.setInt(1, limit);
			final ResultSet resultSet = statement.executeQuery();
			final List<HistoryEntry> historyEntries = new ArrayList<>();
			while (resultSet.next()) {
				final Json json = Json.read(resultSet.getString("document"));
				historyEntries.add(HistoryEntry.fromJson(json));
			}
			return historyEntries;
		} catch (final SQLException exception) {
			throw new IllegalStateException("Failed to load history entries", exception);
		}
	}

	public synchronized void save(HistoryEntry historyEntry) {
		try (PreparedStatement statement = connect().prepareStatement("INSERT OR REPLACE INTO history_entries (id, document) VALUES (?, ?)")) {
			statement.setString(1, historyEntry.getId().toString());
			statement.setString(2, historyEntry.toJson().toString());
			statement.executeUpdate();
		} catch (final SQLException exception) {
			throw new IllegalStateException("Failed to save history entry", exception);
		}
	}

	public synchronized void delete(UUID id) {
		try (PreparedStatement statement = connect().prepareStatement("DELETE FROM history_entries WHERE id = ?")) {
			statement.setString(1, id.toString());
			statement.executeUpdate();
		} catch (final SQLException exception) {
			throw new IllegalStateException("Failed to delete history entry", exception);
		}
	}

	public synchronized void deleteAll() {
		try (PreparedStatement statement = connect().prepareStatement("DELETE FROM history_entries")) {
			statement.executeUpdate();
		} catch (final SQLException exception) {
			throw new IllegalStateException("Failed to delete all history entries", exception);
		}
	}

	private synchronized Connection connect() {
		try {
			if (this.connection == null || this.connection.isClosed()) {
				this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databasePath.toAbsolutePath().toString());
				this.connection.createStatement().execute("CREATE TABLE IF NOT EXISTS history_entries (id TEXT PRIMARY KEY, document JSONB NOT NULL, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)");
			}
		} catch (final SQLException exception) {
			throw new IllegalStateException("Failed to connect to history database", exception);
		}
		return this.connection;
	}

	@Override
	public void close() throws Exception {
		if (this.connection != null) {
			this.connection.close();
		}
	}

}
