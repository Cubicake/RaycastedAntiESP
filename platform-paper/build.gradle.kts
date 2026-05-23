import java.time.Instant
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://eldonexus.de/repository/maven-public/") }
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    //paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")
    compileOnly("org.spongepowered:configurate-core:4.2.0")
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")

    compileOnly("net.strokkur.commands:annotations-paper:2.1.2")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.2")

    implementation("org.jetbrains:annotations:24.0.1")

    implementation(project(":locatable-lib"))
    implementation(project(":logging"))
    implementation(project(":core"))
    implementation(project(":packetevents"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val javaToolchainService = project.extensions.getByType(JavaToolchainService::class.java)

group = "games.cubi.raycastedantiesp.paper"

val platformPaperVersion: String = "0.4.5-SNAPSHOT"
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
    it.contains("buildRelease")
}

fun getVersionString(): String {
    if (isRelease) {
        val paperVersion = platformPaperVersion.substringBefore("-") // Remove any suffixes like "-SNAPSHOT"
        return "${coreVersion}-Paper-${paperVersion}-RELEASE"
    } else {
        return "${coreVersion}-Paper-${platformPaperVersion}+build-${buildTime.get()}+git-${commitShort.get()}"
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
}

tasks.shadowJar {
    dependencies {
        include(project(":logging"))
        include(project(":locatable-lib"))
        include(project(":core"))
        include(project(":packetevents"))
    }
    archiveBaseName.set("RaycastedAntiESP")
    archiveClassifier.set("")
}

tasks.jar {
    archiveBaseName.set("Incorrectly-Compiled-Without-ShadowJar")
}

// This is the task to run to test if changes to the plugin are working
tasks.register("buildSnapshot") {
    group = "raycasted anti-esp" //Not caps sensitive so using spacing and hyphen
    description = "Builds a snapshot version of the plugin with git and build-time metadata included in the file name."
    dependsOn("shadowJar")
}

tasks.register("buildRelease") {
    group = "raycasted anti-esp"
    description = "Builds a release version of the plugin with a clean version number (no git or build-time metadata) included in the file name."
    dependsOn("shadowJar")
}

tasks.register("runPaper") {
    // alias for runServer to put it in the same group as the build tasks
    group = "raycasted anti-esp"
    description = "Runs a Paper server with the plugin loaded for testing purposes."
    dependsOn("runServer")
}
