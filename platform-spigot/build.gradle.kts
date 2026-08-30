import java.time.Instant
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.0"
    id("xyz.jpenilla.run-paper")
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://eldonexus.de/repository/maven-public/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.2-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")
    compileOnly("org.spongepowered:configurate-core:4.2.0")
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")

    implementation("org.jetbrains:annotations:24.0.1")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    implementation(project(":leafpile"))
    implementation(project(":locatables"))
    implementation(project(":logging"))
    implementation(project(":core"))
    implementation(project(":packetevents"))

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.named("testRuntimeClasspath") {
    exclude(group = "at.yawk.lz4", module = "lz4-java")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val javaToolchainService = project.extensions.getByType(JavaToolchainService::class.java)

group = "games.cubi.raycastedantiesp.spigot"

val platformPaperVersion: String = "0.0.1-SNAPSHOT"
val coreVersion = project(":core").version.toString()

val commitShort = providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
}.standardOutput.asText.map { it.trim() }

val commitFull = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val buildTime = providers.provider {
    Instant.now().toString().replace(":", "-") // Replace colons to avoid issues in file names on some platforms
}

val isRelease = gradle.startParameter.taskNames.any {
    it.contains("buildSpigotRelease")
}

fun getVersionString(): String {
    if (isRelease) {
        val paperVersion = platformPaperVersion.substringBefore("-") // Remove any suffixes like "-SNAPSHOT"
        return "${coreVersion}-Spigot-${paperVersion}-RELEASE"
    } else {
        return "${coreVersion}-Spigot-${platformPaperVersion}+build-${buildTime.get()}+git-${commitShort.get()}"
    }
}

fun getBasicVersionString(): String {
    return if (isRelease) {
        platformPaperVersion.substringBefore("-") // Remove any suffixes like "-SNAPSHOT"
    } else {
        platformPaperVersion
    }
}

version = getVersionString()

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        javaLauncher = javaToolchainService.launcherFor {
            //languageVersion.set(paperRunJavaVersion.map(JavaLanguageVersion::of))
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        minecraftVersion("26.1.2")
        //minecraftVersion("1.21.11")
        jvmArgs("-Xms4G", "-Xmx4G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version.toString())
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
        val gitProps = mapOf(
            "short_git" to commitShort.get(),
            "long_git" to commitFull.get(),
            "build_time" to buildTime.get(),
            "version" to getBasicVersionString()
        )
        inputs.properties(gitProps)
        filesMatching("build-properties/platform.yml") {
            expand(gitProps)
        }
    }

    test {
        useJUnitPlatform()
    }
}

tasks.shadowJar {
    dependencies {
        include(project(":logging"))
        include(project(":locatables"))
        include(project(":core"))
        include(project(":packetevents"))

        include(project(":leafpile"))
        include(dependency("org.bstats:bstats-base:3.2.1"))
        include(dependency("org.bstats:bstats-bukkit:3.2.1"))
    }
    relocate(
        "ca.spottedleaf",
        "games.cubi.libs.raycastedantiesp.spottedleaf"
    )
    relocate(
        "org.bstats",
        "games.cubi.libs.raycastedantiesp.bstats"
    )
    minimize {} // get rid of leafpile bloat
    archiveBaseName.set("RaycastedAntiESP")
    archiveClassifier.set("")
}

tasks.jar {
    archiveBaseName.set("Incorrectly-Compiled-Without-ShadowJar")
}

// This is the task to run to test if changes to the plugin are working
tasks.register("buildSpigotSnapshot") {
    group = "raycasted anti-esp" //Not caps sensitive so using spacing and hyphen
    description = "Builds a snapshot version of the plugin with git and build-time metadata included in the file name."
    dependsOn("shadowJar")
}

tasks.register("buildSpigotRelease") {
    group = "raycasted anti-esp"
    description = "Builds a release version of the plugin with a clean version number (no git or build-time metadata) included in the file name."
    dependsOn("shadowJar")
}
