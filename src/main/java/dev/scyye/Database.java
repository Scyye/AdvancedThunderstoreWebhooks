package dev.scyye;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
	private static final String URL = "jdbc:sqlite:thunderstore.db";

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL);
	}

	public static void init() {
		try (Connection conn = getConnection()) {
			var stmt = conn.createStatement();
			// Create users table
			stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    passwordHash TEXT NOT NULL
                )
            """);

			// Create rules table
			stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    conditionKeyword TEXT,
                    pfp TEXT,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
            """);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}

