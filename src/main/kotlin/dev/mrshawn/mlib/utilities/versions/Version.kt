package dev.mrshawn.mlib.utilities.versions

import org.bukkit.Bukkit

enum class Version(
	private val versionName: String,
	private val versionWeight: Int
) {

	V1_19("1.19", 12),
	V1_18("1.18", 11),
	V1_17("1.17", 10),
	V1_16("1.16", 9),
	V1_15("1.15", 8),
	V1_14("1.14", 7),
	V1_13("1.13", 6),
	V1_12("1.12", 5),
	V1_11("1.11", 4),
	V1_10("1.10", 3),
	V1_9("1.9", 2),
	V1_8("1.8", 1),
	UNKNOWN("unknown", 0);

	companion object {

		fun getCurrentVersion(): Version {
			val version = Bukkit.getServer().version
			return values().firstOrNull { version.contains(it.versionName) } ?: UNKNOWN
		}

		fun isAtLeast(version: Version): Boolean {
			return getCurrentVersion().versionWeight >= version.versionWeight
		}

		fun isAtMost(version: Version): Boolean {
			return getCurrentVersion().versionWeight <= version.versionWeight
		}

	}

}