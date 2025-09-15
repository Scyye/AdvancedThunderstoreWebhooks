package dev.scyye;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class WebServer {
	private static final Gson gson = new Gson();
	private static final RuleService ruleService = new RuleService();

	public static void start() {
		port(8080);
		staticFiles.location("/public");

		// ---------------- AUTH ---------------- //
		post("/register", (req, res) -> {
			Map<String, String> body = gson.fromJson(req.body(), Map.class);
			String username = body.get("username");
			String password = body.get("password");

			boolean success = ruleService.register(username, password);
			if (success) {
				res.status(201);
				return "User created";
			} else {
				res.status(400);
				return "Username already exists";
			}
		});

		post("/login", (req, res) -> {
			Map<String, String> body = gson.fromJson(req.body(), Map.class);
			String username = body.get("username");
			String password = body.get("password");

			RuleService.User user = ruleService.login(username, password);
			if (user != null) {
				// For simplicity, return user id as a "token"
				res.type("application/json");
				return gson.toJson(Map.of("token", user.id()));
			} else {
				res.status(401);
				return "Invalid credentials";
			}
		});

		// ---------------- RULES ---------------- //
		// Get all rules for a user
		get("/rules", (req, res) -> {
			String token = req.headers("Authorization");
			if (token == null) {
				res.status(401);
				return "Missing token";
			}

			RuleService.User user = ruleService.getUserById(Integer.parseInt(token));
			if (user == null) {
				res.status(401);
				return "Invalid token";
			}

			List<ExtraRule> rules = ruleService.getRulesForUser(user);
			res.type("application/json");
			return gson.toJson(rules);
		});

		// Add a rule
		post("/rules", (req, res) -> {
			String token = req.headers("Authorization");
			if (token == null) {
				res.status(401);
				return "Missing token";
			}

			RuleService.User user = ruleService.getUserById(Integer.parseInt(token));
			if (user == null) {
				res.status(401);
				return "Invalid token";
			}

			ExtraRule rule = gson.fromJson(req.body(), ExtraRule.class);
			ruleService.addRule(user, rule);
			res.status(201);
			return "Rule added";
		});

		// DELETE a rule
		delete("/rules/:id", (req, res) -> {
			String token = req.headers("Authorization");
			if (token == null) {
				res.status(401);
				return "Missing token";
			}

			RuleService.User user = ruleService.getUserById(Integer.parseInt(token));
			if (user == null) {
				res.status(401);
				return "Invalid token";
			}

			int ruleId = Integer.parseInt(req.params(":id"));
			boolean success = ruleService.deleteRule(user, ruleId);
			if (success) {
				res.status(200);
				return "Deleted";
			} else {
				res.status(403);
				return "Cannot delete rule";
			}
		});

// PUT to edit a rule
		put("/rules/:id", (req, res) -> {
			String token = req.headers("Authorization");
			if (token == null) {
				res.status(401);
				return "Missing token";
			}

			RuleService.User user = ruleService.getUserById(Integer.parseInt(token));
			if (user == null) {
				res.status(401);
				return "Invalid token";
			}

			int ruleId = Integer.parseInt(req.params(":id"));
			ExtraRule updatedRule = gson.fromJson(req.body(), ExtraRule.class);
			boolean success = ruleService.updateRule(user, ruleId, updatedRule);
			if (success) {
				res.status(200);
				return "Updated";
			} else {
				res.status(403);
				return "Cannot update rule";
			}
		});

		get("/admin/rules", (req, res) -> {
			String token = req.headers("Authorization");
			RuleService.User user = ruleService.getUserById(Integer.parseInt(token));
			if (user == null || !user.isAdmin()) {
				res.status(403);
				return "Forbidden";
			}

			List<ExtraRule> allRules = ruleService.getAllRulesForUser(); // return all users' rules
			res.type("application/json");
			return gson.toJson(allRules);
		});


	}
}
