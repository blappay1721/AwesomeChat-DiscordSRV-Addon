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

import dev.adf.awesomechatdiscord.vendor.objectholders.ValuePairs;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * No-op stand-in for InteractiveChat's proxy messaging.
 * <p>
 * Every call site is already behind a {@code IC.bungeecordMode} check, and that
 * is a compile-time {@code false} here, so none of these run. They exist so the
 * vendored code compiles without surgery on thirteen call sites.
 */
public class BungeeMessageSender {

    public static boolean addInventory(long time, ICApi.SharedType type, String hash, String title, Inventory inventory) throws IOException {
        return false;
    }

    public static boolean requestParsedPlaceholders(long time, UUID uuid, String text) throws IOException {
        return false;
    }

    public static boolean forwardPlaceholders(long time, UUID uuid, List<ValuePairs<String, String>> pairs) throws IOException {
        return false;
    }

    public static boolean forwardEquipment(long time, UUID uuid, boolean rightHanded, int selectedSlot, int experienceLevel, ItemStack[] equipment) throws IOException {
        return false;
    }

    public static boolean forwardInventory(long time, UUID uuid, boolean rightHanded, int selectedSlot, int experienceLevel, String title, Inventory inventory) throws IOException {
        return false;
    }

    public static boolean forwardEnderchest(long time, UUID uuid, boolean rightHanded, int selectedSlot, int experienceLevel, String title, Inventory enderchest) throws IOException {
        return false;
    }

    public static boolean sendPlayerUniversalCooldown(UUID uuid, long time) throws IOException {
        return false;
    }

    public static boolean sendPlayerPlaceholderCooldown(UUID uuid, dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder placeholder, long time) throws IOException {
        return false;
    }

    private BungeeMessageSender() {
    }
}
