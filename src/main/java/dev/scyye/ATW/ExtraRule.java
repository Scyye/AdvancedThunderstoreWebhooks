package dev.scyye.ATW;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraRule {
	public String url;
	String communityRegex;
	String packageRegex;
	String versionRegex;
	String descriptionRegex;
	String generalRegex;
	String name;
	String pfp;

	public boolean shouldApply(Main.PackageListing listing) {
		System.out.println("Checking ExtraRule: " + this.name + " against listing: " + listing);
		return (communityRegex == null || listing.getOwner().matches(communityRegex)) &&
				(packageRegex == null || listing.getName().matches(packageRegex)) &&
				(versionRegex == null || listing.getLatest().versionNumber.matches(versionRegex)) &&
				(descriptionRegex == null || listing.getLatest().description.matches(descriptionRegex)) &&
				(generalRegex == null || new Gson().toJson(listing).matches(generalRegex));
	}

	@JsonCreator
	public ExtraRule(
			@NotNull @JsonProperty("url") String url,
			@Nullable @JsonProperty("communityRegex") String communityRegex,
			@Nullable @JsonProperty("packageRegex") String packageRegex,
			@Nullable @JsonProperty("versionRegex") String versionRegex,
			@Nullable @JsonProperty("descriptionRegex") String descriptionRegex,
			@Nullable @JsonProperty("generalRegex") String generalRegex,
			@Nullable @JsonProperty("name") String name,
			@Nullable @JsonProperty("pfp") String pfp) {
		this.url = url;
		this.communityRegex = communityRegex;
		this.packageRegex = packageRegex;
		this.versionRegex = versionRegex;
		this.descriptionRegex = descriptionRegex;
		this.generalRegex = generalRegex;
		this.name = (name==null||name.isEmpty())?"Extra Rule":name;
		this.pfp = (pfp==null||pfp.isEmpty())?null:pfp;
	}

	public ExtraRule() {
		// Default constructor for deserialization
	}
}
