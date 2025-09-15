package dev.scyye;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

public class RuleService {
	private final String dbUrl = "jdbc:sqlite:thunderstore.sqlite";

	public RuleService() {
		initDb();
	}

	private void initDb() {
		try (Connection conn = DriverManager.getConnection(dbUrl)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL,
                        admin BOOLEAN DEFAULT 0
                    )
                """);

				stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        url TEXT,
                        community_regex TEXT,
                        package_regex TEXT,
                        version_regex TEXT,
                        description_regex TEXT,
                        general_regex TEXT,
                        name TEXT,
                        pfp TEXT,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                """);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ------------------ USER MANAGEMENT ------------------ //

	public boolean register(String username, String password) {
		String hash = BCrypt.hashpw(password, BCrypt.gensalt());
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement(
					 "INSERT INTO users(username,password_hash) VALUES (?,?)")) {
			ps.setString(1, username);
			ps.setString(2, hash);
			ps.executeUpdate();

			return true;
		} catch (SQLException e) {
			System.err.println("Register failed: " + e.getMessage());
			return false;
		}
	}

	public User login(String username, String password) {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement(
					 "SELECT id, password_hash FROM users WHERE username = ?")) {
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String hash = rs.getString("password_hash");
				if (BCrypt.checkpw(password, hash)) {
					return new User(rs.getInt("id"), username, Objects.equals(username, "admin"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ------------------ RULES ------------------ //

	public void addRule(User user, ExtraRule rule) {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO rules(user_id, url, community_regex, package_regex, version_regex,
                                  description_regex, general_regex, name, pfp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
             """)) {
			ps.setInt(1, user.id());
			ps.setString(2, rule.url);
			ps.setString(3, rule.communityRegex);
			ps.setString(4, rule.packageRegex);
			ps.setString(5, rule.versionRegex);
			ps.setString(6, rule.descriptionRegex);
			ps.setString(7, rule.generalRegex);
			ps.setString(8, rule.name);
			ps.setString(9, rule.pfp);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public User getUserById(int id) {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new User(id, rs.getString("username"), Objects.equals(rs.getString("username"), "admin"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}


	public List<ExtraRule> getRulesForUser(User user) {
		List<ExtraRule> rules = new ArrayList<>();
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement("SELECT * FROM rules WHERE user_id = ?")) {
			ps.setInt(1, user.id());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ExtraRule rule = new ExtraRule(
						rs.getString("url"),
						rs.getString("community_regex"),
						rs.getString("package_regex"),
						rs.getString("version_regex"),
						rs.getString("description_regex"),
						rs.getString("general_regex"),
						rs.getString("name"),
						rs.getString("pfp")
				);
				rules.add(rule);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rules;
	}

	public List<ExtraRule> getAllRulesForUser() {
		List<ExtraRule> rules = new ArrayList<>();
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT * FROM rules")) {
			while (rs.next()) {
				ExtraRule rule = new ExtraRule(
						rs.getString("url"),
						rs.getString("community_regex"),
						rs.getString("package_regex"),
						rs.getString("version_regex"),
						rs.getString("description_regex"),
						rs.getString("general_regex"),
						rs.getString("name"),
						rs.getString("pfp")
				);
				rules.add(rule);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rules;
	}

	// For admin
	public List<ExtraRule> getAllRules() {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement("SELECT * FROM rules")) {
			ResultSet rs = ps.executeQuery();
			List<ExtraRule> rules = new ArrayList<>();
			while(rs.next()) {
				ExtraRule rule = new ExtraRule();
				rule.generalRegex = rs.getString("general_regex");
				rule.versionRegex = rs.getString("version_regex");
				rule.descriptionRegex = rs.getString("description_regex");
				rule.packageRegex = rs.getString("package_regex");
				rule.communityRegex = rs.getString("community_regex");
				rule.name = rs.getString("name");
				rule.pfp = rs.getString("pfp");
				rule.url = rs.getString("url");
				rules.add(rule);
			}
			return rules;
		} catch (SQLException e) { e.printStackTrace(); return Collections.emptyList(); }
	}


	public boolean deleteRule(User user, int ruleId) {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement(
					 "DELETE FROM rules WHERE id = ? AND user_id = ?")) {
			ps.setInt(1, ruleId);
			ps.setInt(2, user.id());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean updateRule(User user, int ruleId, ExtraRule rule) {
		try (Connection conn = DriverManager.getConnection(dbUrl);
			 PreparedStatement ps = conn.prepareStatement("""
                 UPDATE rules SET
                 url = ?, community_regex = ?, package_regex = ?, version_regex = ?,
                 description_regex = ?, general_regex = ?, name = ?, pfp = ?
                 WHERE id = ? AND user_id = ?
         """)) {
			ps.setString(1, rule.url);
			ps.setString(2, rule.communityRegex);
			ps.setString(3, rule.packageRegex);
			ps.setString(4, rule.versionRegex);
			ps.setString(5, rule.descriptionRegex);
			ps.setString(6, rule.generalRegex);
			ps.setString(7, rule.name);
			ps.setString(8, rule.pfp);
			ps.setInt(9, ruleId);
			ps.setInt(10, user.id());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	public class User {
		private final int id;
		private final String username;
		private final boolean isAdmin;

		public User(int id, String username, boolean isAdmin) {
			this.id = id;
			this.username = username;
			this.isAdmin = isAdmin;
		}

		public int id() { return id; }
		public String username() { return username; }
		public boolean isAdmin() { return isAdmin; }
	}

}
