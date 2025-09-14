package dev.scyye;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class WebGui {
	public static void start() {
		Javalin app = Javalin.create(config -> {
			config.staticFiles.add("/public"); // serve static html/js/css
		}).start(8080);

		app.post("/signup", ctx -> {
			String user = ctx.formParam("username");
			String pass = ctx.formParam("password");
			Main.createUser(user, pass);
			ctx.result("User created");
		});

		app.post("/login", ctx -> {
			String user = ctx.formParam("username");
			String pass = ctx.formParam("password");
			if (Main.checkLogin(user, pass)) {
				ctx.sessionAttribute("user", user);
				ctx.result("Logged in");
			} else {
				ctx.status(401).result("Invalid credentials");
			}
		});

		app.get("/rules", ctx -> {
			if (ctx.sessionAttribute("user") == null) {
				ctx.status(403).result("Not logged in");
				return;
			}
			ctx.json(Main.getAllRules());
		});

		app.post("/rules", ctx -> {
			String user = ctx.sessionAttribute("user");
			if (user == null) {
				ctx.status(403).result("Not logged in");
				return;
			}
			ExtraRule rule = new ExtraRule();
			rule.name = ctx.formParam("name");
			rule.pfp = ctx.formParam("pfp");
			rule.setConditionsFromForm(ctx);
			Main.addRule(user, rule);
			ctx.result("Rule added");
		});
	}
}
