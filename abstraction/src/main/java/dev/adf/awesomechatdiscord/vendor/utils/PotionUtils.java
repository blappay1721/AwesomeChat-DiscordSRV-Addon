/*
 * This file is part of AwesomeChatDiscordAddon, a fork of InteractiveChat4.
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

package dev.adf.awesomechatdiscord.vendor.utils;

import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PotionUtils {

    /**
     * 1.20.5 renamed most PotionType constants to match their vanilla ids, so the
     * enum name is the answer for everything except these. Looking the aliases up
     * by string keeps one source compiling against both API generations.
     */
    private static final Map<String, String> LEGACY_ALIASES = new HashMap<>();

    static {
        LEGACY_ALIASES.put("JUMP", "leaping");
        LEGACY_ALIASES.put("SPEED", "swiftness");
        LEGACY_ALIASES.put("INSTANT_HEAL", "healing");
        LEGACY_ALIASES.put("INSTANT_DAMAGE", "harming");
        LEGACY_ALIASES.put("REGEN", "regeneration");
        LEGACY_ALIASES.put("UNCRAFTABLE", "empty");
    }

    public static String getVanillaPotionName(PotionType type) {
        if (type == null) {
            return "empty";
        }
        String alias = LEGACY_ALIASES.get(type.name());
        return alias != null ? alias : type.name().toLowerCase(Locale.ROOT);
    }

    private PotionUtils() {
    }
}
