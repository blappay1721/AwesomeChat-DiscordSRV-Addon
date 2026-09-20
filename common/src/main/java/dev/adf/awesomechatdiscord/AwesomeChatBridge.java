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

package dev.adf.awesomechatdiscord;

import dev.adf.awesomechatdiscord.vendor.IC;
import dev.adf.awesomechatdiscord.vendor.objectholders.BuiltInPlaceholder;
import dev.adf.awesomechatdiscord.vendor.objectholders.CustomPlaceholder;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder;
import dev.adf.awesomechatdiscord.vendor.utils.CustomStringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Translates AwesomeChat's display-trigger configuration into the placeholder
 * shape the forked renderer expects.
 * <p>
 * AwesomeChat writes triggers like {@code [item]}, {@code [inv]} and
 * {@code [ec]} into chat, and keeps the legacy {@code AsyncPlayerChatEvent}
 * alive so DiscordSRV still sees the message. The renderer inherited from
 * InteractiveChat matches placeholders by regex, so all this has to do is build
 * the equivalent patterns and fill in {@link IC}.
 * <p>
 * Nothing here calls into AwesomeChat's code: it reads AwesomeChat's
 * {@code config.yml} through the Bukkit API, so the two plugins stay
 * independently versionable.
 */
public final class AwesomeChatBridge {

    /** Mirrors {@code ItemDisplayManager.KEYWORDS}, split by what each one shows. */
    private static final List<String> ITEM_KEYWORDS = Arrays.asList("item", "hand", "this");
    private static final List<String> INVENTORY_KEYWORDS = Arrays.asList("inventory", "inv");
    private static final List<String> ENDERCHEST_KEYWORDS = Arrays.asList("enderchest", "echest", "ec");

    private static final List<String> RESERVED = new ArrayList<>();

    static {
        RESERVED.addAll(ITEM_KEYWORDS);
        RESERVED.addAll(INVENTORY_KEYWORDS);
        RESERVED.addAll(ENDERCHEST_KEYWORDS);
    }

    public static Plugin getAwesomeChat() {
        return Bukkit.getPluginManager().getPlugin("AwesomeChat");
    }

    /**
     * Reads AwesomeChat's config and fills in every field the renderer consults.
     * Safe to call again on reload.
     */
    public static void load() {
        Plugin awesomeChat = getAwesomeChat();
        if (awesomeChat == null) {
            throw new IllegalStateException("AwesomeChat is not installed");
        }
        FileConfiguration config = awesomeChat.getConfig();

        boolean enabled = config.getBoolean("item-display.enabled", false);
        IC.useItem = enabled;
        IC.useInventory = enabled;
        IC.useEnder = enabled;

        IC.itemDisplayTimeout = config.getLong("item-display.snapshot-ttl-seconds", 300) * 1000L;

        String prefix = config.getString("item-display.trigger-prefix", "[");
        String suffix = config.getString("item-display.trigger-suffix", "]");

        IC.itemPlaceholder = builtIn(triggerPattern(prefix, suffix, ITEM_KEYWORDS),
                "Item", IC.ITEM_PERMISSION);
        IC.invPlaceholder = builtIn(triggerPattern(prefix, suffix, INVENTORY_KEYWORDS),
                "Inventory", IC.INVENTORY_PERMISSION);
        IC.enderPlaceholder = builtIn(triggerPattern(prefix, suffix, ENDERCHEST_KEYWORDS),
                "EnderChest", IC.ENDERCHEST_PERMISSION);

        // What replaces the trigger in the Discord message. AwesomeChat spells the
        // tokens {item}/{count}/{player}; the renderer wants {Item}/{Amount} and
        // PlaceholderAPI's %player_name%.
        String itemFormat = tokens(config.getString(
                "item-display.formats.item", "&f[{item}&f x{count}&f]"));
        IC.itemReplaceText = legacy(itemFormat);
        // A stack of one takes the singular format, and the renderer only substitutes
        // {Amount} on the plural branch. AwesomeChat prints the count either way
        // ("always show count, including x1"), so resolve it here rather than leaving
        // a literal {Amount} in the message.
        IC.itemSingularReplaceText = legacy(itemFormat.replace("{Amount}", "1"));
        IC.invReplaceText = legacy(tokens(config.getString(
                "item-display.formats.inventory", "&a[{player}'s Inventory]")));
        IC.enderReplaceText = legacy(tokens(config.getString(
                "item-display.formats.enderchest", "&5[{player}'s Ender Chest]")));

        IC.itemTitle = tokens(config.getString("item-display.gui-titles.item", "{player}'s Item"));
        IC.invTitle = tokens(config.getString("item-display.gui-titles.inventory", "{player}'s Inventory"));
        IC.enderTitle = tokens(config.getString("item-display.gui-titles.enderchest", "{player}'s Ender Chest"));

        IC.placeholderList = new LinkedHashMap<>();
        for (ICPlaceholder placeholder : Arrays.asList(IC.itemPlaceholder, IC.invPlaceholder, IC.enderPlaceholder)) {
            IC.placeholderList.put(placeholder.getInternalId(), placeholder);
        }
        loadCustomTriggers(config, prefix, suffix);

        // AwesomeChat gates its own triggers on permissions, so the renderer has to
        // check the custom ones too rather than trusting the message text.
        IC.useCustomPlaceholderPermissions = true;

        verify(IC.itemPlaceholder, prefix, "item", suffix);
        verify(IC.invPlaceholder, prefix, "inv", suffix);
        verify(IC.enderPlaceholder, prefix, "ec", suffix);

        Bukkit.getLogger().info("[AwesomeChatDiscordAddon] Read AwesomeChat triggers: enabled=" + enabled
                + " item=" + IC.itemPlaceholder.getKeyword().pattern()
                + " inventory=" + IC.invPlaceholder.getKeyword().pattern()
                + " enderchest=" + IC.enderPlaceholder.getKeyword().pattern()
                + " (" + IC.placeholderList.size() + " placeholders total)");
    }

    /**
     * Registers AwesomeChat's {@code item-display.custom-triggers} so their text is
     * substituted into the Discord message instead of being sent as a raw
     * {@code [trigger]}.
     */
    private static void loadCustomTriggers(FileConfiguration config, String prefix, String suffix) {
        ConfigurationSection section = config.getConfigurationSection("item-display.custom-triggers");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String name = key.toLowerCase(Locale.ROOT);
            if (RESERVED.contains(name)) {
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            String text = entry == null ? section.getString(key, "") : entry.getString("text", "");
            if (text.isEmpty()) {
                continue;
            }
            String permission = entry == null ? "" : entry.getString("permission", "");
            String hover = entry == null ? "" : entry.getString("hover", "");

            CustomPlaceholder placeholder = new CustomPlaceholder(
                    name,
                    CustomPlaceholder.ParsePlayer.SENDER,
                    triggerPattern(prefix, suffix, Collections.singletonList(name)),
                    false,
                    0,
                    new CustomPlaceholder.CustomPlaceholderHoverEvent(!hover.isEmpty(), legacy(tokens(hover))),
                    new CustomPlaceholder.CustomPlaceholderClickEvent(false, null, ""),
                    new CustomPlaceholder.CustomPlaceholderReplaceText(true, legacy(tokens(text))),
                    name,
                    Component.text(name),
                    permission.isEmpty() ? CustomPlaceholder.CUSTOM_PLACEHOLDER_PERMISSION + name : permission);
            IC.placeholderList.put(placeholder.getInternalId(), placeholder);
        }
    }

    /**
     * Rebuilds AwesomeChat's trigger regex for one group of keywords. Longest
     * first, so {@code [inventory]} is not matched as {@code [inv]} plus junk.
     * <p>
     * Escaping is per character, never {@link Pattern#quote}: {@link ICPlaceholder}
     * rewrites these patterns so colour codes between characters still match, and it
     * walks the regex one character at a time with no notion of {@code \Q...\E}. A
     * quoted block comes out of that rewrite as literal text and matches nothing.
     */
    private static Pattern triggerPattern(String prefix, String suffix, List<String> keywords) {
        String alternation = keywords.stream()
                .sorted((a, b) -> b.length() - a.length())
                .map(CustomStringUtils::escapeMetaCharacters)
                .collect(Collectors.joining("|"));
        // Case-insensitivity is inline rather than a Pattern flag: colorCodeIgnoredPattern
        // recompiles from the pattern string alone and drops any flags passed here, so
        // CASE_INSENSITIVE would be lost and [ITEM] would stop matching.
        String regex = suffix.isEmpty()
                ? "(?i)" + CustomStringUtils.escapeMetaCharacters(prefix) + "(?:" + alternation + ")(?!\\w)"
                : "(?i)" + CustomStringUtils.escapeMetaCharacters(prefix) + "(?:" + alternation + ")"
                        + CustomStringUtils.escapeMetaCharacters(suffix);
        return Pattern.compile(regex);
    }

    /**
     * Checks a built placeholder still matches its own trigger after
     * {@link ICPlaceholder} has rewritten it for colour codes. Cheap, and it turns a
     * silently dead regex into a startup error rather than displays that never render.
     */
    private static void verify(ICPlaceholder placeholder, String prefix, String keyword, String suffix) {
        String sample = prefix + keyword + suffix;
        if (!placeholder.getKeyword().matcher(sample).find()) {
            Bukkit.getLogger().severe("[AwesomeChatDiscordAddon] Trigger \"" + sample
                    + "\" does not match its own pattern " + placeholder.getKeyword().pattern()
                    + " -- displays will not render to Discord. Please report this.");
        }
    }

    private static BuiltInPlaceholder builtIn(Pattern keyword, String name, String permission) {
        return new BuiltInPlaceholder(keyword, name, Component.text(name), permission, 0);
    }

    private static String tokens(String awesomeChatFormat) {
        if (awesomeChatFormat == null) {
            return "";
        }
        return awesomeChatFormat
                .replace("{item}", "{Item}")
                .replace("{count}", "{Amount}")
                .replace("{player}", "%player_name%");
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private AwesomeChatBridge() {
    }
}
