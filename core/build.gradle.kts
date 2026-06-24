/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

import java.time.Instant

plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
    compileOnly("org.spongepowered:configurate-core:4.2.0")
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
    implementation(project(":locatable-lib"))
    compileOnly(project(":logging"))

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("it.unimi.dsi:fastutil:8.5.18")
}

val coreVersion = "0.5.1-SNAPSHOT"

val isRelease = gradle.startParameter.taskNames.any {
    it.contains("buildRelease")
}

fun getVersionString(): String {
    if (isRelease) {
        return coreVersion.substringBefore("-") // Remove any suffixes like "-SNAPSHOT"
    } else {
        return coreVersion
    }
}

fun getBasicVersionString(): String {
    return if (isRelease) {
        coreVersion.substringBefore("-") // Remove any suffixes like "-SNAPSHOT"
    } else {
        coreVersion
    }
}

version = getVersionString()

val commitShort = providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
}.standardOutput.asText.map { it.trim() }

val commitFull = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

val buildTime = providers.provider {
    Instant.now().toString()
}

tasks {
    processResources {
        val gitProps = mapOf(
            "short_git" to commitShort.get(),
            "long_git" to commitFull.get(),
            "build_time" to buildTime.get(),
            "version" to getBasicVersionString()
        )
        inputs.properties(gitProps)
        filesMatching("build-properties/core.yml") {
            expand(gitProps)
        }
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}
