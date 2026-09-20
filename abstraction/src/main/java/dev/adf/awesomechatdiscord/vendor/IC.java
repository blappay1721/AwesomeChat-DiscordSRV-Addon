/*
 * This file is part of AwesomeChatDiscordAddon, a fork of
 * InteractiveChatDiscordSrvAddon2 by LoohpJames.
 *
 * Copyright (C) 2020 - 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2020 - 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.adf.awesomechatdiscord.vendor;

import dev.adf.awesomechatdiscord.vendor.objectholders.ConcurrentCacheHashMap;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder;
import dev.adf.awesomechatdiscord.vendor.utils.MCVersion;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import net.milkbowl.vault.permission.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stands in for InteractiveChat's static configuration surface.
 * <p>
 * The renderer this plugin forked reads roughly fifty static fields off
 * InteractiveChat's main class. Rather than thread a config object through
 * ~70 files, this holds the same field names so the vendored code compiles
 * unchanged; {@link #load(Plugin, org.bukkit.configuration.file.FileConfiguration)}
 * fills them from AwesomeChat's config plus this addon's own.
 *
 * ponytail: mutable static config mirroring upstream's shape, kept so the fork
 * stays a small diff against IC. Move to an injected config object
 * if this ever needs to run more than one instance per JVM.
 */
public class IC {

    /** The addon plugin instance, used wherever upstream scheduled against IC. */
    public static Plugin plugin = null;
    public static File jar = null;

    public static MCVersion version = MCVersion.resolve();
    public static String exactMinecraftVersion = Bukkit.getBukkitVersion().split("-")[0];

    /** AwesomeChat is single-server; there is no proxy-side counterpart to talk to. */
    public static final boolean bungeecordMode = false;
    public static final int remoteDelay = 0;

    public static String language = "en_us";

    // Display triggers, mirrored from AwesomeChat's item-display section.
    public static boolean useItem = true;
    public static boolean useInventory = true;
    public static boolean useEnder = true;
    public static boolean itemAirAllow = true;
    public static long itemDisplayTimeout = 300000;
    public static long universalCooldown = 0;

    /** AwesomeChat's permission nodes for the three built-in displays. */
    public static final String ITEM_PERMISSION = "awesomechat.display.item";
    public static final String INVENTORY_PERMISSION = "awesomechat.display.inventory";
    public static final String ENDERCHEST_PERMISSION = "awesomechat.display.enderchest";

    public static ICPlaceholder itemPlaceholder = null;
    public static ICPlaceholder invPlaceholder = null;
    public static ICPlaceholder enderPlaceholder = null;
    public static Map<UUID, ICPlaceholder> placeholderList = new LinkedHashMap<>();
    public static boolean useCustomPlaceholderPermissions = false;

    public static String itemTitle = "%player_name%'s Item";
    public static String invTitle = "%player_name%'s Inventory";
    public static String enderTitle = "%player_name%'s Ender Chest";

    public static Component itemReplaceText = Component.empty();
    public static Component itemSingularReplaceText = Component.empty();
    public static Component invReplaceText = Component.empty();
    public static Component enderReplaceText = Component.empty();

    public static ItemStack invFrame1;
    public static ItemStack invFrame2;
    public static ItemStack itemFrame1;
    public static ItemStack itemFrame2;
    public static ItemStack unknownReplaceItem;

    public static String itemName = "";
    public static boolean itemHover = true;
    public static boolean itemGUI = true;
    public static boolean itemMapPreview = true;
    public static Component itemAlternativeHoverMessage = Component.empty();
    public static int itemTagMaxLength = 32767;
    public static boolean sendOriginalIfTooLong = false;
    public static boolean cancelledMessage = true;

    /** Shared item and map displays, expired on the same timeout as upstream. */
    public static ConcurrentCacheHashMap<String, Inventory> itemDisplay =
            new ConcurrentCacheHashMap<>(itemDisplayTimeout, 60000);
    public static ConcurrentCacheHashMap<String, ItemStack> mapDisplay =
            new ConcurrentCacheHashMap<>(itemDisplayTimeout, 60000);

    public static boolean playerNotFoundHoverEnable = true;
    public static Component playerNotFoundHoverText = Component.empty();
    public static boolean playerNotFoundClickEnable = false;
    public static String playerNotFoundClickAction = "SUGGEST_COMMAND";
    public static String playerNotFoundClickValue = "";
    public static boolean playerNotFoundReplaceEnable = true;
    public static Component playerNotFoundReplaceText = Component.empty();


    public static String mentionPrefix = "@";
    public static Component noPermissionMessage = Component.empty();
    public static boolean hideLodestoneCompassPos = false;

    public static boolean rgbTags = true;
    public static List<Pattern> additionalRGBFormats = new ArrayList<>();
    public static boolean parsePAPIOnMainThread = false;

    /** Tab-completion tooltips belong to InteractiveChat's packet layer, which this fork drops. */
    public static final boolean useTooltipOnTab = false;
    public static final Component tabTooltip = Component.empty();

    /** Upstream's "is the web data feed trusted" flag. No web data here, so never. */
    public static final boolean t = false;

    // Hooks upstream detected on InteractiveChat's behalf. AwesomeChat owns chat
    // formatting now, so the chat-plugin hooks are permanently off.
    public static final boolean essentialsHook = false;
    public static final boolean cmiHook = false;
    public static final boolean ventureChatHook = false;
    public static final boolean mysqlPDBHook = false;
    public static final boolean viaVersionHook = false;
    public static final boolean protocolSupportHook = false;

    /** Vault, used only to read permissions of players who are offline. */
    public static Permission perms = null;

    public static boolean isPluginEnabled(String name) {
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        return p != null && p.isEnabled();
    }

    public static void sendMessage(CommandSender sender, Component component) {
        if (component == null) {
            return;
        }
        if (sender instanceof Audience) {
            ((Audience) sender).sendMessage(component);
        } else {
            sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(component));
        }
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }

    private IC() {
    }
}
