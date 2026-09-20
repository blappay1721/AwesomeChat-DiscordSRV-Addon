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

import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder;
import dev.adf.awesomechatdiscord.vendor.objectholders.ValueTrios;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the parts of InteractiveChat's public API this fork still calls.
 * <p>
 * Everything proxy-related collapses: AwesomeChat runs on a single server, so
 * the Bungee player list is always empty and item transform providers, which
 * only InteractiveChat's own addons registered, are the identity function.
 */
public class ICApi {

    /** Inventories shared to Discord, keyed by the hash embedded in the message. */
    private static final Map<SharedType, Map<String, Inventory>> SHARE_LISTS = new EnumMap<>(SharedType.class);
    private static final Map<String, ItemStack> MAP_SHARE_LIST = new ConcurrentHashMap<>();

    static {
        for (SharedType type : SharedType.values()) {
            SHARE_LISTS.put(type, new ConcurrentHashMap<>());
        }
    }

    public static void sendMessage(CommandSender receiver, Component component) {
        IC.sendMessage(receiver, component);
    }

    public static List<ICPlaceholder> getICPlaceholderList() {
        return new ArrayList<>(IC.placeholderList.values());
    }

    public static Map<String, Inventory> getItemShareList(SharedType type) {
        return SHARE_LISTS.get(type);
    }

    public static String addInventoryToItemShareList(SharedType type, String hash, Inventory inventory) {
        SHARE_LISTS.get(type).put(hash, inventory);
        return hash;
    }

    public static Map<String, ItemStack> getMapShareList() {
        return MAP_SHARE_LIST;
    }

    public static String addMapToMapSharedList(String hash, ItemStack item) {
        MAP_SHARE_LIST.put(hash, item);
        return hash;
    }

    /** AwesomeChat has no nickname registry of its own. */
    public static List<String> getNicknames(UUID uuid) {
        return Collections.emptyList();
    }

    /** No transform providers exist outside InteractiveChat's own ecosystem. */
    public static ItemStack transformItemStack(ItemStack itemStack, UUID uuid) {
        return itemStack;
    }

    /** Single-server only, so there is never anyone on the other side of a proxy. */
    public static CompletableFuture<List<ValueTrios<UUID, String, Integer>>> getBungeecordPlayerList() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    public enum SharedType {

        ITEM(0),
        INVENTORY(1),
        INVENTORY1_UPPER(2),
        INVENTORY1_LOWER(3),
        ENDERCHEST(4);

        private final int id;

        SharedType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private ICApi() {
    }
}
