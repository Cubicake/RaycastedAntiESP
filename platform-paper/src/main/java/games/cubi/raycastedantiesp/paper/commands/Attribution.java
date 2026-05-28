/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.commands;

import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import org.bukkit.command.CommandSender;

@Command("raycastedantiespCredits") //Deliberately an obscure name and not "attribution" or "credits" to avoid being annoying to server owners. Not permission-gated as all players need to be able to access it to comply with the AGPLv3 licence.
public class Attribution {
    public static final byte READ_COMMENTS_BEFORE_EDITING_OR_DELETING_CLASS_OR_FACE_LEGAL_ACTION = 0; //Provocative name to make sure people actually read the comments.
    /*
Please note that removing this command without providing an alternative, equally (or more) prominent way to view this information is a violation of the AGPLv3 licence, which may result in legal action.
If you are forking this project, read the below note for fork developers to see how to modify the notice to remain AGPLv3 compliant.

If you wish to acquire a copy of the plugin without the obligation to display this notice or equivalent, you may discuss purchasing a commercial licence by contacting Cubicake via Discord (@Cubicake) or by making a GitHub issue at https://github.com/Cubicake/RaycastedAntiESP

Note that using external software to remove this command (for example by using a command blocker plugin to block players from using this command) is ALSO illegal.
    * */
    @Executes
    public void execute(CommandSender sender) {
        sendAttributionMessage(sender);
    }

    public static void sendAttributionMessage(CommandSender sender) {
        sender.sendRichMessage("<white>This server runs <gold><click:open_url:'https://github.com/Cubicake/RaycastedAntiESP'>RaycastedAntiESP</click></gold>, a packet-based anti-esp plugin.\n" +
                "\n" +
                "<white>The plugin is <dark_green>copyright © 2025-2026 Cubicake and Contributors</dark_green>, and licenced under the <dark_green>AGPLv3 licence</dark_green>, which requires the source code to be available to all users of the plugin, including you. \n" +
                "<white>As such, the source code can be found at <u><blue><hover:show_text:'Click to view source'><click:open_url:'https://github.com/Cubicake/RaycastedAntiESP'>https://github.com/Cubicake/RaycastedAntiESP</click></hover></blue></u>.");
        /* Fork developers: Comment out the above message and uncomment this one, making sure to replace the placeholders with the appropriate information about your fork.
// Some notes for fork developers:
// While you are allowed to modify the message below, you must still include all of the legally obligated information. As such, I recommend you only modify the {bracketed} placeholders and the formatting of the message.
// The legally obligated information you must include is as follows:
// 1. You must not claim to be the original creator of the project. Therefore, the notice must still clearly include a mention of "copyright © 2025-2026 Cubicake and Contributors"
// 2. You must include a link to the source code of your fork, which must be accessible to all players on all servers running your plugin, and include the full source code of your fork. This is a requirement of the AGPLv3 licence, which requires that all users of the software have access to the source code. Therefore, you must include a link to the source code of your fork in the message, and it must be easily accessible to users of the plugin.
// 3. You may not remove this notice without providing an alternative **prominent** notice containing a link to the source code and the original copyright notice.

        sender.sendRichMessage("<white>This server runs a fork of <gold><click:open_url:'https://github.com/Cubicake/RaycastedAntiESP'>RaycastedAntiESP</click></gold> called {YOUR_FORK_NAME_HERE}, a packet-based anti-esp plugin.\n" +
                "\n" +
                "<white>The original plugin is <dark_green>copyright © 2025-2026 Cubicake and Contributors</dark_green>, with this fork copyright © 2025-2026 {YOUR_NAMES_HERE}. The plugin in its entirety is licenced under the <dark_green>AGPLv3 licence</dark_green>, which requires the source code to be available to all users of the plugin, including you. \n" +
                "<white>As such, the source code can be found at <u><blue><hover:show_text:'Click to view source'><click:open_url:'{YOUR_SOURCE_CODE_LINK_HERE}'>{YOUR_SOURCE_CODE_LINK_HERE}</click></hover></blue></u>.");
        */
        sender.sendRichMessage("A copy of the AGPLv3 Licence can be found at <hover:show_text:'Click to view licence'><u><blue><click:open_url:'https://www.gnu.org/licenses/agpl-3.0.html'>https://www.gnu.org/licenses/agpl-3.0.html</click></blue></u></hover>.\n" +
                "\n" +
                "Disclaimer: This software is provided “as is”, without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and noninfringement.\n" +
                "In no event shall the authors or copyright holders be liable for any claim, damages, or other liability arising from, out of, or in connection with the software or the use of the software.");
        sender.sendRichMessage("Credits:\n" +
                "\n" +
                "Plugin developers:\n" +
                "- Cubicake (Sole developer, creator and maintainer of RaycastedAntiESP)\n" +
                /*
                * "- Feel free to add your name here as the developer of your fork!\n" +
                * */
                "\n" +
                "Libraries:\n" +
                "- Strokkur424 and other contributors to <click:open_url:'https://github.com/Strokkur424/StrokkCommands'><u><hover:show_text:'Click to view Github repository'><blue>StrokkCommands</blue></hover></u></click>, an LGPL-licensed open-source annotation-based brigadier command tree generator.\n" +
                "- Retrooper, Booky10, and all other contributors to <click:open_url:'https://github.com/retrooper/packetevents'><u><blue><hover:show_text:'Click to view Github repository'>PacketEvents</hover></blue></u></click>, a GPL-licensed open-source library for handling minecraft packets.");
        assert READ_COMMENTS_BEFORE_EDITING_OR_DELETING_CLASS_OR_FACE_LEGAL_ACTION == 0; // Make IDEs highlight the variable to make it easier to read.
    }

    public static final String attributionCommandDescription = "<green>/raycastedantiespCredits - View copyright notice and source code link (required to be displayed by the AGPLv3 licence)";
}
