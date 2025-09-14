package dev.scyye;

import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

public class Stuff {
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
