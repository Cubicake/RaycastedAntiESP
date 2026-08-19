/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.benchmark;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class EventBenchmarkPlugin extends JavaPlugin {
    private static EventBenchmarkPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Starting the event-dispatch benchmark asynchronously.");
        Bukkit.getScheduler().runTaskAsynchronously(this, this::runBenchmark);
    }

    static EventBenchmarkPlugin instance() {
        EventBenchmarkPlugin current = instance;
        if (current == null) {
            throw new IllegalStateException("Benchmark plugin is not enabled");
        }
        return current;
    }

    private void runBenchmark() {
        try {
            Path resultFile = Path.of(System.getProperty(
                    "eventBenchmark.resultFile",
                    getDataPath().resolve("results.json").toString()
            ));
            Files.createDirectories(resultFile.toAbsolutePath().getParent());

            Options options = new OptionsBuilder()
                    .include(EventDispatchBenchmark.class.getName())
                    .mode(org.openjdk.jmh.annotations.Mode.AverageTime)
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .threads(1)
                    .forks(0)
                    .warmupIterations(5)
                    .warmupTime(TimeValue.seconds(1))
                    .measurementIterations(10)
                    .measurementTime(TimeValue.seconds(1))
                    .shouldFailOnError(true)
                    .resultFormat(ResultFormatType.JSON)
                    .result(resultFile.toString())
                    .build();

            Collection<RunResult> results = new Runner(options).run();
            logSummary(results, resultFile);
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "Event-dispatch benchmark failed", throwable);
        } finally {
            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
        }
    }

    private void logSummary(Collection<RunResult> results, Path resultFile) {
        record Score(double value, double error) {}

        Map<Integer, Score> cubi = new HashMap<>();
        Map<Integer, Score> bukkit = new HashMap<>();
        results.stream()
                .sorted(Comparator.comparingInt(result -> Integer.parseInt(
                        result.getParams().getParam("listenerCount")
                )))
                .forEach(result -> {
                    int count = Integer.parseInt(result.getParams().getParam("listenerCount"));
                    Score score = new Score(
                            result.getPrimaryResult().getScore(),
                            result.getPrimaryResult().getScoreError()
                    );
                    if (result.getParams().getBenchmark().endsWith(".cubi")) {
                        cubi.put(count, score);
                    } else if (result.getParams().getBenchmark().endsWith(".bukkit")) {
                        bukkit.put(count, score);
                    }
                });

        getLogger().info("Event dispatch results (average ns/op, error is 99.9% confidence interval):");
        getLogger().info("listeners | cubi ns/op | bukkit ns/op | bukkit/cubi");
        cubi.keySet().stream().sorted().forEach(count -> {
            Score cubiScore = cubi.get(count);
            Score bukkitScore = bukkit.get(count);
            getLogger().info(String.format(
                    Locale.ROOT,
                    "%9d | %8.3f ± %-8.3f | %10.3f ± %-8.3f | %.2fx",
                    count,
                    cubiScore.value(),
                    cubiScore.error(),
                    bukkitScore.value(),
                    bukkitScore.error(),
                    bukkitScore.value() / cubiScore.value()
            ));
        });
        getLogger().info("Machine-readable JMH results: " + resultFile.toAbsolutePath());
    }
}
