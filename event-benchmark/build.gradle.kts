/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

plugins {
    id("java")
    id("xyz.jpenilla.run-paper")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation(project(":leafpile"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

group = "games.cubi.raycastedantiesp.benchmark"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.shadowJar {
    archiveBaseName.set("EventDispatchBenchmark")
    archiveClassifier.set("")
}

tasks.jar {
    archiveBaseName.set("Incorrectly-Compiled-Without-ShadowJar")
}

tasks.runServer {
    minecraftVersion("1.21.11")
    jvmArgs(
        "-Xms1G",
        "-Xmx1G",
        "-Dcom.mojang.eula.agree=true",
        "-Dterminal.jline=false",
        "-Dterminal.ansi=false",
        "-DeventBenchmark.resultFile=${layout.buildDirectory.file("reports/event-benchmark/results.json").get().asFile.absolutePath}",
    )
}
