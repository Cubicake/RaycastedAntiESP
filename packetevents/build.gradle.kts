plugins {
    id("java")
}

group = "games.cubi.raycastedantiesp.packetevents"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
    implementation(project(":locatable-lib"))
    implementation(project(":logging"))
    implementation(project(":core"))

    compileOnly("com.github.retrooper:packetevents-api:2.12.0")
    compileOnly("org.spongepowered:configurate-core:4.2.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}
