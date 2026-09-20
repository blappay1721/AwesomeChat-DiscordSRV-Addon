/*
 * This file is part of InteractiveChat4.
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

import dev.adf.awesomechatdiscord.vendor.IC;
import dev.adf.awesomechatdiscord.vendor.ICApi;
import dev.adf.awesomechatdiscord.vendor.ICApi.SharedType;
import dev.adf.awesomechatdiscord.vendor.events.ItemPlaceholderEvent;
import dev.adf.awesomechatdiscord.vendor.BungeeMessageSender;
import dev.adf.awesomechatdiscord.vendor.nms.NMS;
import dev.adf.awesomechatdiscord.vendor.objectholders.CustomPlaceholder;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICInventoryHolder;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlayer;
import dev.adf.awesomechatdiscord.vendor.objectholders.OfflineICPlayer;
import dev.adf.awesomechatdiscord.vendor.utils.ChatColorUtils;
import dev.adf.awesomechatdiscord.vendor.utils.CompassUtils;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentCompacting;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentFlattening;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentReplacing;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentUtils;
import dev.adf.awesomechatdiscord.vendor.utils.FilledMapUtils;
import dev.adf.awesomechatdiscord.vendor.utils.HashUtils;
import dev.adf.awesomechatdiscord.vendor.utils.InteractiveChatComponentSerializer;
import dev.adf.awesomechatdiscord.vendor.utils.InventoryUtils;
import dev.adf.awesomechatdiscord.vendor.utils.ItemNBTUtils;
import dev.adf.awesomechatdiscord.vendor.utils.ItemStackUtils;
import dev.adf.awesomechatdiscord.vendor.utils.MCVersion;
import dev.adf.awesomechatdiscord.vendor.utils.PlaceholderParser;
import dev.adf.awesomechatdiscord.vendor.utils.PlayerUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class ItemDisplay {

    @SuppressWarnings("deprecation")
    public static Component process(Component component, Optional<ICPlayer> optplayer, Player receiver, boolean preview, long unix) throws Exception {
        String plain = InteractiveChatComponentSerializer.plainText().serialize(component);
        if (IC.itemPlaceholder.getKeyword().matcher(plain).find()) {
            String regex = IC.itemPlaceholder.getKeyword().pattern();
            if (IC.bungeecordMode && optplayer.isPresent() && optplayer.get().isLocal()) {
                ICPlayer player = optplayer.get();
                ItemStack[] equipment;
                if (IC.version.isOld()) {
                    equipment = new ItemStack[] {player.getEquipment().getHelmet(), player.getEquipment().getChestplate(), player.getEquipment().getLeggings(), player.getEquipment().getBoots(), player.getEquipment().getItemInHand()};
                } else {
                    equipment = new ItemStack[] {player.getEquipment().getHelmet(), player.getEquipment().getChestplate(), player.getEquipment().getLeggings(), player.getEquipment().getBoots(), player.getEquipment().getItemInMainHand(), player.getEquipment().getItemInOffHand()};
                }
                try {
                    BungeeMessageSender.forwardEquipment(unix, player.getUniqueId(), player.isRightHanded(), player.getSelectedSlot(), player.getExperienceLevel(), equipment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (optplayer.isPresent()) {
                ICPlayer player = optplayer.get();
                if (PlayerUtils.hasPermission(player.getUniqueId(), IC.ITEM_PERMISSION, true, 5)) {
                    Component alternativeHover = null;
                    if (!IC.itemHover && !ComponentUtils.isEmpty(IC.itemAlternativeHoverMessage)) {
                        alternativeHover = IC.itemAlternativeHoverMessage;
                    }
                    Component itemComponent = ComponentFlattening.flatten(createItemDisplay(player, receiver, component, unix, IC.itemHover, alternativeHover, preview));
                    component = ComponentReplacing.replace(component, regex, true, itemComponent);
                }
            } else {
                Component message;
                if (IC.playerNotFoundReplaceEnable) {
                    message = IC.playerNotFoundReplaceText.replaceText(TextReplacementConfig.builder().matchLiteral("{Placeholder}").replacement(IC.itemName).build());
                } else {
                    message = Component.text(IC.itemName);
                }
                if (IC.playerNotFoundHoverEnable && IC.itemHover) {
                    message = message.hoverEvent(HoverEvent.showText(IC.playerNotFoundHoverText.replaceText(TextReplacementConfig.builder().matchLiteral("{Placeholder}").replacement(IC.itemName).build())));
                }
                if (IC.playerNotFoundClickEnable) {
                    String clickValue = ChatColorUtils.translateAlternateColorCodes('&', IC.playerNotFoundClickValue.replace("{Placeholder}", IC.itemName));
                    ClickEvent.Action<?> clickEventAction = ClickEvent.Action.NAMES.value(CustomPlaceholder.ClickEventAction.of(IC.playerNotFoundClickAction).getId());
                    ClickEvent.Payload.Text payload = ClickEvent.Payload.string(clickValue);
                    if (clickEventAction != null && clickEventAction.supports(payload)) {
                        //noinspection unchecked
                        message = message.clickEvent(ClickEvent.clickEvent((ClickEvent.Action<ClickEvent.Payload.Text>) clickEventAction, payload));
                    }
                }
                component = ComponentReplacing.replace(component, regex, true, message);
            }
        }
        return component;
    }

    public static boolean useInventoryView(ItemStack item) {
        try {
            if (item.getItemMeta() instanceof BlockStateMeta) {
                BlockState bsm = ((BlockStateMeta) item.getItemMeta()).getBlockState();
                if (bsm instanceof InventoryHolder) {
                    Inventory container = ((InventoryHolder) bsm).getInventory();
                    if ((container.getSize() % 9) != 0) {
                        return false;
                    }
                    for (int i = 0; i < container.getSize(); i++) {
                        ItemStack containerItem = container.getItem(i);
                        if (containerItem != null && !containerItem.getType().equals(Material.AIR)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Component createItemDisplay(ICPlayer player, Player receiver, Component component, long timeSent, boolean preview) throws Exception {
        return createItemDisplay(player, receiver, component, timeSent, true, null, preview);
    }

    public static Component createItemDisplay(ICPlayer player, Player receiver, Component component, long timeSent, boolean showHover, Component alternativeHover, boolean preview) throws Exception {
        ItemStack item = PlayerUtils.getHeldItem(player);

        item = ICApi.transformItemStack(item, receiver.getUniqueId());

        ItemPlaceholderEvent event = new ItemPlaceholderEvent(player, receiver, component, timeSent, item);
        Bukkit.getPluginManager().callEvent(event);
        item = event.getItemStack();

        return createItemDisplay(player, item, IC.itemTitle, showHover, alternativeHover, preview);
    }

    public static Component createItemDisplay(OfflineICPlayer player, ItemStack item) throws Exception {
        return createItemDisplay(player, item, IC.itemTitle, true, null, false);
    }

    public static Component createItemDisplay(OfflineICPlayer player, ItemStack item, String rawTitle, boolean showHover, Component alternativeHover, boolean preview) throws Exception {
        if (item == null) {
            item = new ItemStack(Material.AIR);
        }
        if (IC.hideLodestoneCompassPos) {
            item = CompassUtils.hideLodestoneCompassPosition(item);
        }

        boolean trimmed = false;
        boolean isAir = item.getType().equals(Material.AIR);
        int itemAmount = isAir && IC.version.isNewerOrEqualTo(MCVersion.V1_20_5) ? 1 : item.getAmount();
        ItemMeta itemMeta = item.getItemMeta();

        ItemStack originalItem = item.clone();

        String itemJson = ItemNBTUtils.getNMSItemStackJson(item);
        ItemStack trimmedItem = null;
        if (IC.sendOriginalIfTooLong && itemJson.length() > IC.itemTagMaxLength) {
            trimmedItem = new ItemStack(item.getType());
            trimmedItem.addUnsafeEnchantments(item.getEnchantments());
            if (itemMeta != null && itemMeta.hasDisplayName()) {
                ItemStack nameItem = trimmedItem.clone();
                Component name = NMS.getInstance().getItemStackDisplayName(item);
                NMS.getInstance().setItemStackDisplayName(nameItem, name);
                String newjson = ItemNBTUtils.getNMSItemStackJson(nameItem);
                if (newjson.length() <= IC.itemTagMaxLength) {
                    trimmedItem = nameItem;
                }
            }
            if (item.getItemMeta() != null && item.getItemMeta().hasLore()) {
                ItemStack loreItem = trimmedItem.clone();
                ItemMeta meta = loreItem.getItemMeta();
                meta.setLore(item.getItemMeta().getLore());
                loreItem.setItemMeta(meta);
                String newjson = ItemNBTUtils.getNMSItemStackJson(loreItem);
                if (newjson.length() <= IC.itemTagMaxLength) {
                    trimmedItem = loreItem;
                }
            }
            trimmed = true;
        }

        String amountString = "";
        Component itemDisplayNameComponent = ItemStackUtils.getDisplayName(item);

        amountString = String.valueOf(itemAmount);
        Key key = ItemNBTUtils.getNMSItemStackNamespacedKey(item);

        ShowItem showItem;
        if (IC.version.isNewerOrEqualTo(MCVersion.V1_20_5)) {
            if (item.getType().equals(Material.AIR)) {
                showHover = false;
            }
            Map<Key, DataComponentValue> dataComponents = ItemNBTUtils.getNMSItemStackDataComponents(trimmedItem == null ? item : trimmedItem);
            showItem = dataComponents.isEmpty() ? ShowItem.showItem(key, itemAmount) : ShowItem.showItem(key, itemAmount, dataComponents);
        } else {
            String tag = ItemNBTUtils.getNMSItemStackTag(trimmedItem == null ? item : trimmedItem);
            showItem = tag == null ? ShowItem.showItem(key, itemAmount) : ShowItem.showItem(key, itemAmount, BinaryTagHolder.binaryTagHolder(tag));
        }

        HoverEvent<ShowItem> hoverEvent = HoverEvent.showItem(showItem);
        String title = ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(player, rawTitle));
        String sha1 = HashUtils.createSha1(title, item);

        String command = null;
        boolean isMapView = false;

        if (!preview) {
            if (IC.itemMapPreview && FilledMapUtils.isFilledMap(item)) {
                isMapView = true;
                if (!IC.mapDisplay.containsKey(sha1)) {
                    ICApi.addMapToMapSharedList(sha1, item);
                }
            } else if (!IC.itemDisplay.containsKey(sha1)) {
                if (useInventoryView(item)) {
                    Inventory container = ((InventoryHolder) ((BlockStateMeta) item.getItemMeta()).getBlockState()).getInventory();
                    Inventory inv = Bukkit.createInventory(ICInventoryHolder.INSTANCE, container.getSize() + 9, title);
                    ItemStack empty = IC.itemFrame1.clone();
                    if (item.getType().equals(IC.itemFrame1.getType())) {
                        empty = IC.itemFrame2.clone();
                    }
                    if (empty.getItemMeta() != null) {
                        ItemMeta emptyMeta = empty.getItemMeta();
                        emptyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "");
                        empty.setItemMeta(emptyMeta);
                    }
                    for (int j = 0; j < 9; j++) {
                        inv.setItem(j, empty);
                    }
                    inv.setItem(4, isAir ? null : originalItem);
                    for (int j = 0; j < container.getSize(); j++) {
                        ItemStack shulkerItem = container.getItem(j);
                        if (shulkerItem != null && !shulkerItem.getType().equals(Material.AIR)) {
                            inv.setItem(j + 9, shulkerItem == null ? null : shulkerItem.clone());
                        }
                    }
                    ICApi.addInventoryToItemShareList(SharedType.ITEM, sha1, inv);
                } else {
                    if (IC.version.isOld()) {
                        Inventory inv = Bukkit.createInventory(ICInventoryHolder.INSTANCE, 27, title);
                        ItemStack empty = IC.itemFrame1.clone();
                        if (item.getType().equals(IC.itemFrame1.getType())) {
                            empty = IC.itemFrame2.clone();
                        }
                        if (empty.getItemMeta() != null) {
                            ItemMeta emptyMeta = empty.getItemMeta();
                            emptyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "");
                            empty.setItemMeta(emptyMeta);
                        }
                        for (int j = 0; j < inv.getSize(); j++) {
                            inv.setItem(j, empty);
                        }
                        inv.setItem(13, isAir ? null : originalItem);
                        ICApi.addInventoryToItemShareList(SharedType.ITEM, sha1, inv);
                    } else {
                        Inventory inv = InventoryUtils.CAN_USE_DROPPER_TYPE ? Bukkit.createInventory(ICInventoryHolder.INSTANCE, InventoryType.DROPPER, title) : Bukkit.createInventory(ICInventoryHolder.INSTANCE, 27, title);
                        ItemStack empty = IC.itemFrame1.clone();
                        if (item.getType().equals(IC.itemFrame1.getType())) {
                            empty = IC.itemFrame2.clone();
                        }
                        if (empty.getItemMeta() != null) {
                            ItemMeta emptyMeta = empty.getItemMeta();
                            emptyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "");
                            empty.setItemMeta(emptyMeta);
                        }
                        for (int j = 0; j < inv.getSize(); j++) {
                            inv.setItem(j, empty);
                        }
                        inv.setItem(inv.getSize() / 2, isAir ? null : originalItem);
                        ICApi.addInventoryToItemShareList(SharedType.ITEM, sha1, inv);
                    }
                }
            }
            command = isMapView ? "/interactivechat viewmap " + sha1 : "/interactivechat viewitem " + sha1;
        }

        if (trimmed && IC.cancelledMessage) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[InteractiveChat] " + ChatColor.RED + "Trimmed an item display's meta data as it's NBT exceeds the maximum characters allowed in the chat [THIS IS NOT A BUG]");
        }

        Component itemDisplayComponent = PlaceholderParser.parse(player, itemAmount == 1 ? IC.itemSingularReplaceText : IC.itemReplaceText.replaceText(TextReplacementConfig.builder().matchLiteral("{Amount}").replacement(Component.text(amountString)).build()));
        itemDisplayComponent = itemDisplayComponent.replaceText(TextReplacementConfig.builder().matchLiteral("{Item}").replacement(itemDisplayNameComponent).build());
        if (showHover) {
            itemDisplayComponent = itemDisplayComponent.hoverEvent(hoverEvent);
        } else if (alternativeHover != null) {
            itemDisplayComponent = itemDisplayComponent.hoverEvent(HoverEvent.showText(alternativeHover));
        }
        if (command != null && !isAir && (isMapView || (!isMapView && IC.itemGUI))) {
            itemDisplayComponent = itemDisplayComponent.clickEvent(ClickEvent.runCommand(command));
        }
        return ComponentCompacting.optimize(itemDisplayComponent);
    }

}
