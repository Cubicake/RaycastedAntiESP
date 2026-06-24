package games.cubi.raycastedantiesp.paper;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.paper.utils.PaperScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;


public class UpdateChecker {
    private static final String VERSION_API_ENDPOINT = "https://api.modrinth.com/v2/project/bCjNZu0C/version?version_type=release&include_changelog=false";

    private static CompletableFuture<String> fetchFeaturedVersion(RaycastedAntiESP plugin) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Bukkit.getAsyncScheduler().runNow(plugin, ignored -> {
            try (final InputStreamReader reader = new InputStreamReader(new URI(VERSION_API_ENDPOINT).toURL().openConnection().getInputStream())) {
                final JsonArray array = new JsonArray();
                array.add(new BufferedReader(reader).readLine());
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    stringBuilder.append(array.get(i).getAsString());
                }
                String apiData = stringBuilder.toString();
                JsonArray jsonArray = JsonParser.parseString(apiData).getAsJsonArray();
                JsonObject firstObject = jsonArray.get(0).getAsJsonObject();
                String versionNumber = firstObject.get("version_number").getAsString();

                future.complete(versionNumber);

            } catch (IOException | URISyntaxException e) {
                future.completeExceptionally(new IllegalStateException("Unable to fetch latest version", e));
            }
        });
        return future;
    }

    private static final int INVALID_VERSION_FORMAT = 2; private static final int CURRENT_IS_OLDER = -1; private static final int VERSIONS_ARE_EQUAL = 0; private static final int CURRENT_IS_NEWER = 1;

    private static int checkIfLaterThan(String currentVersion, String fetchedVersion) {
        String[] currentParts = currentVersion.replace("v", "").split("[.-]");
        String[] fetchedParts = fetchedVersion.replace("v", "").split("[.-]");

        if (currentParts.length < 3 || fetchedParts.length < 3) {
            return INVALID_VERSION_FORMAT;
        }

        int majorVersionDifference = Integer.parseInt(currentParts[0]) - Integer.parseInt(fetchedParts[0]);

        if (majorVersionDifference == VERSIONS_ARE_EQUAL) {
            int minorVersionDifference = Integer.parseInt(currentParts[1]) - Integer.parseInt(fetchedParts[1]);

            if (minorVersionDifference == VERSIONS_ARE_EQUAL) {
                @SuppressWarnings("RedundantLocalVariable") //Makes code more readable
                int patchVersionDifference = Integer.parseInt(currentParts[2]) - Integer.parseInt(fetchedParts[2]);
                return patchVersionDifference;
            }
            return minorVersionDifference;
        }
        return majorVersionDifference;
    }

    public static void checkForUpdates(RaycastedAntiESP plugin, CommandSender audience) {
        fetchFeaturedVersion(plugin).thenAccept(version -> {
            PaperScheduler.runForAudience(plugin, audience, () -> {
                @SuppressWarnings("deprecation")
                int versionCheck = checkIfLaterThan(plugin.getDescription().getVersion(), version);

                if (versionCheck == VERSIONS_ARE_EQUAL) {
                    audience.sendRichMessage("<green>You are using the latest version of Raycasted Anti-ESP.");
                    return;
                }

                if (versionCheck <= CURRENT_IS_OLDER) {
                    audience.sendRichMessage("<red>You are not using the latest version of Raycasted Anti-ESP. Please update to <green>v" + version+".");
                    if (audience instanceof Player) audience.sendRichMessage("\n" + "<hover:show_text:'https://modrinth.com/project/bCjNZu0C/versions'><aqua><u><click:open_url:'https://modrinth.com/project/bCjNZu0C/versions'>Click here to download it.</click></u></aqua></hover>");
                    return;
                }

                if (versionCheck == CURRENT_IS_NEWER) {
                    audience.sendRichMessage("<yellow>You are using a development build of Raycasted Anti-ESP. The latest stable version is <green>v" + version + "<yellow>.");
                    return;
                }

                if (versionCheck == INVALID_VERSION_FORMAT) {
                    audience.sendRichMessage("<red>Unable to check for updates, invalid version format.");
                    return;
                }
                audience.sendRichMessage("<red>An unknown error occurred while checking for updates. UpdateChecker#checkIfLaterThan returned unrecognised integer " + versionCheck + ".");
            });
        }).exceptionally(ex -> {
            Logger.error("An error occurred while checking for plugin updates", ex, 4, UpdateChecker.class);
            return null;
        });
    }
}
