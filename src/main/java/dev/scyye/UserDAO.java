package dev.scyye;

import java.sql.*;
import java.util.Optional;

public class UserDao {

	public void createUser(String username, String passwordHash) {
		try (Connection conn = Database.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "INSERT INTO users (username, passwordHash) VALUES (?, ?)")) {
			stmt.setString(1, username);
			stmt.setString(2, passwordHash);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Optional<User> findByUsername(String username) {
		try (Connection conn = Database.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT * FROM users WHERE username = ?")) {
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return Optional.of(new User(
						rs.getLong("id"),
						rs.getString("username"),
						rs.getString("passwordHash")
				));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return Optional.empty();
	}
}
