import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.0.20"
	id("com.gradleup.shadow") version "8.3.5"
	id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.mrshawn.mlib.test"
version = "1.0.0"

// The Mlib version to test against. Must match the root pom.xml <version>.
// Build it locally first:  (from the repo root)  mvn install
val mlibVersion = "0.1.0"

repositories {
	mavenLocal() // resolves the locally-installed Mlib artifact (`mvn install` in the repo root)
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://repo.extendedclip.com/releases/") // PlaceholderAPI (optional)
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
	compileOnly("me.clip:placeholderapi:2.11.6")

	// The shaded Mlib jar (already contains relocated Adventure + kotlin-reflect).
	implementation("dev.mrshawn:mlib:$mlibVersion")
	implementation(kotlin("stdlib"))
}

// Compile to Java 17 bytecode using whatever JDK runs Gradle (17 or newer), keeping the
// Java and Kotlin tasks on the same target (avoids toolchain provisioning of a separate JDK).
java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

tasks {
	shadowJar {
		// Produce a single, clean plugin jar (Mlib + kotlin-stdlib bundled in).
		archiveClassifier.set("")
	}

	// `./gradlew runServer` downloads a Paper server and runs it with this plugin.
	// run-paper automatically uses the shadowJar output when the Shadow plugin is applied.
	runServer {
		minecraftVersion("1.20.4")
	}

	build {
		dependsOn(shadowJar)
	}
}
