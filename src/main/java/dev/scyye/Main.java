package dev.scyye;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.*;

public class Main {
	private static final RuleService ruleService = new RuleService();
	private static final List<ExtraRule> cachedRules = new CopyOnWriteArrayList<>();
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

	private static final Map<String, PackageListing> packageCache = new ConcurrentHashMap<>();
	private static final JDAWebhookClient webhook = JDAWebhookClient.withUrl("https://discord.com/api/webhooks/1412479049137131622/I1DxMQk4UgcvJUZx2vWx3iR4iFlcXWVLLHUi_gK3KREjl2cThJaD3qm4U-E7-fKmUlNF");
	private static final ObjectMapper objectMapper = new ObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	public static void main(String[] args) throws Exception {
		new Thread(WebServer::start).start();
		Client client = new Client("https://thunderstore.io/");

		// Load rules into cache initially
		cachedRules.addAll(ruleService.getAllRulesForUser());
		System.out.println("Loaded " + cachedRules.size() + " rules from DB.");

		// Schedule periodic refresh every 2 minutes
		scheduler.scheduleAtFixedRate(() -> {
			try {
				List<ExtraRule> freshRules = ruleService.getAllRulesForUser();
				cachedRules.clear();
				cachedRules.addAll(freshRules);
				System.out.println("Refreshed rules cache, total: " + cachedRules.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 2, 2, TimeUnit.MINUTES);

		// Fetch initial packages
		fetchInitialPackages(client);
		scheduler.scheduleAtFixedRate(() ->
				checkForUpdates(client),
				5, 5, TimeUnit.SECONDS);
	}

	private static void fetchInitialPackages(Client client) throws Exception {
		DataObject packagesData = client.get("/api/experimental/package", new DataObject());
		String rawPackageJson = (String) packagesData.get("results");
		List<PackageListing> packages = Arrays.asList(objectMapper.readValue(rawPackageJson, PackageListing[].class));
		packages.forEach(packageListing -> packageCache.put(packageListing.packageUrl.toString(), packageListing));
	}

	private static void checkForUpdates(Client client) {
		long startTime = System.currentTimeMillis();

		try {
			DataObject packagesData = client.get("/api/experimental/package", new DataObject());
			String rawPackageJson = (String) packagesData.get("results");
			List<PackageListing> packages = Arrays.asList(objectMapper.readValue(rawPackageJson, PackageListing[].class));

			packages.stream()
					.filter(Main::isUpdated)
					.forEach(packageListing -> {
						System.out.println("New or updated package: " + packageListing.getName());
						packageCache.put(packageListing.packageUrl.toString(), packageListing);

						// default webhook
						sendWebhook(packageListing, null);

						// extra rules from DB
						cachedRules.stream()
								.filter(r -> r.shouldApply(packageListing))
								.forEach(rule -> sendWebhook(packageListing, rule));
					});
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Checked for new packages in " + (System.currentTimeMillis() - startTime) + "ms.");
	}


	private static boolean isUpdated(PackageListing pkg) {
		PackageListing old = packageCache.get(pkg.packageUrl.toString());
		if (old == null) {
			System.out.println("New package detected: " + pkg.getName());
			return true;
		}
		if (!old.getDateUpdated().equals(pkg.getDateUpdated())) {
			System.out.println("Package updated: " + pkg.getName());
			return true;
		}
		return false;
	}

	private static void sendWebhook(PackageListing pkg, ExtraRule rule) {
		if (rule == null) {
			rule = new ExtraRule();
			rule.name = "Auto Thunderstore Watcher";
			rule.pfp = null;
		}
		WebhookMessageBuilder builder = new WebhookMessageBuilder();
		WebhookMessage message = builder.addEmbeds(
				new WebhookEmbedBuilder()
						.setAuthor(new WebhookEmbed.EmbedAuthor(pkg.getOwner(), null, null))
						.setTitle(new WebhookEmbed.EmbedTitle(pkg.getName() + " v" + pkg.getLatest().versionNumber, "https://thunderstore.io/c/"+ pkg.getCommunityListings()[0].community + "/p/"+pkg.owner+"/"+pkg.name+"/"))
						.setDescription(pkg.getLatest().description)
						.addField(new WebhookEmbed.EmbedField(false, "Total downloads", String.valueOf(getTotalDownloads(pkg.namespace, pkg.name))))
						.addField(new WebhookEmbed.EmbedField(false, "Categories", String.join(", ", Arrays.stream(pkg.getCommunityListings()).map(cl -> cl.categories.length > 0 ? String.join(", ", cl.categories) : "Uncategorized").toArray(String[]::new))))
						.addField(new WebhookEmbed.EmbedField(false, "Community", pkg.getCommunityListings() != null && pkg.getCommunityListings().length > 0 ? pkg.getCommunityListings()[0].community : "Unknown"))
						.setThumbnailUrl(pkg.getLatest().icon)
						.setFooter(getFormattedTime(pkg.getDateUpdated()))
						.build()
		)
				.setUsername(rule.name)
				.setAvatarUrl(rule.pfp)
				.build();
		Main.webhook.send(message);
	}

	private static WebhookEmbed.EmbedFooter getFormattedTime(Date time) {
		TemporalAccessor ta = time.toInstant().atZone(java.time.ZoneId.systemDefault());
		return new WebhookEmbed.EmbedFooter("Updated on " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ta), null);
	}

	private static int getTotalDownloads(String namespace, String name) {
		Client client = new Client("https://thunderstore.io/api/v1/package-metrics/");
		DataObject response = client.get("/"+namespace+"/"+name, new DataObject());
		return (int) Float.parseFloat(response.get("downloads").toString());
	}

	public static class Community {
		String identifier;
		String name;
		public @Nullable String discord_url;
		public @Nullable String wiki_url;
		public boolean require_package_listing_approval;

		public String getIdentifier() {
			return this.identifier;
		}

		public String getName() {
			return this.name;
		}

		public @Nullable String getDiscordUrl() {
			return this.discord_url;
		}

		public @Nullable String getWikiUrl() {
			return this.wiki_url;
		}

		public boolean isRequirePackageListingApproval() {
			return this.require_package_listing_approval;
		}

		public String toString() {
			return (new GsonBuilder()).setPrettyPrinting().create().toJson(this);
		}
	}

	public static class PackageListing {
		public String namespace;
		public String name;
		public String fullName;
		public String owner;
		public URL packageUrl;
		public Date dateCreated;
		public Date dateUpdated;
		public int ratingScore;
		public boolean isPinned;
		public boolean isDeprecated;
		public int totalDownloads;
		public LatestVersion latest;
		public CommunityListing[] communityListings;

		public static class LatestVersion {
			public String namespace;
			public String name;
			public String versionNumber;
			public String fullName;
			public String description;
			public String icon;
			public String[] dependencies;
			public URL downloadUrl;
			public int downloads;
			public Date dateCreated;
			public String websiteUrl;
			public boolean isActive;
		}

		public static class CommunityListing {
			public boolean hasNsfwContent;
			public String[] categories;
			public String community;
			public String reviewStatus;
		}

		// Getters and toString() for debugging
		public String getNamespace() {
			return namespace;
		}

		public String getName() {
			return name;
		}

		public String getFullName() {
			return fullName;
		}

		public String getOwner() {
			return owner;
		}

		public URL getPackageUrl() {
			return packageUrl;
		}

		public Date getDateCreated() {
			return dateCreated;
		}

		public Date getDateUpdated() {
			return dateUpdated;
		}

		public int getRatingScore() {
			return ratingScore;
		}

		public boolean isPinned() {
			return isPinned;
		}

		public boolean isDeprecated() {
			return isDeprecated;
		}

		public int getTotalDownloads() {
			return totalDownloads;
		}

		public LatestVersion getLatest() {
			return latest;
		}

		public CommunityListing[] getCommunityListings() {
			return communityListings;
		}

		@Override
		public String toString() {
			return (new GsonBuilder()).setPrettyPrinting().create().toJson(this);
		}
	}

	public static class PackageVersion {
		public String name;
		public String full_name;
		public String description;
		public String icon;
		public String version_number;
		public String[] dependencies;
		public URL download_url;
		public int downloads;
		public Date date_created;
		public String website_url;
		public boolean is_active;
		public UUID uuid4;
		public long file_size;

		public String getName() {
			return this.name;
		}

		public String getFullName() {
			return this.full_name;
		}

		public String getDescription() {
			return this.description;
		}

		public String getIcon() {
			return this.icon;
		}

		public String getVersionNumber() {
			return this.version_number;
		}

		public String[] getDependencies() {
			return this.dependencies;
		}

		public URL getDownloadUrl() {
			return this.download_url;
		}

		public int getDownloads() {
			return this.downloads;
		}

		public Date getDateCreated() {
			return this.date_created;
		}

		public String getWebsiteUrl() {
			return this.website_url;
		}

		public boolean isActive() {
			return this.is_active;
		}

		public UUID getUuid() {
			return this.uuid4;
		}

		public long getFileSize() {
			return this.file_size;
		}

		public String toString() {
			return (new GsonBuilder()).setPrettyPrinting().create().toJson(this);
		}
	}

}
