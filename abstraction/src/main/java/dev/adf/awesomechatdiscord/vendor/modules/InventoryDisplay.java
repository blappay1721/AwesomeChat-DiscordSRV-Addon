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

package dev.adf.awesomechatdiscord.vendor.modules;

/**
 * The one piece of InteractiveChat's inventory display this fork still needs:
 * the vanilla translation key for the experience level shown on the XP slot.
 * Laying the inventory out in-game is AwesomeChat's job now.
 */
public class InventoryDisplay {

    public static String getLevelTranslation(int level) {
        return level == 1 ? "container.enchant.level.one" : "container.enchant.level.many";
    }

    private InventoryDisplay() {
    }
}
