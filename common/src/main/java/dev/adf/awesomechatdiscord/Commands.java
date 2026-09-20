/*
 * This file is part of InteractiveChatDiscordSrvAddon2.
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
import dev.adf.awesomechatdiscord.vendor.ICApi;
import com.loohp.platformscheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import dev.adf.awesomechatdiscord.vendor.utils.ChatColorUtils;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentStyling;
import dev.adf.awesomechatdiscord.vendor.utils.LanguageUtils;
import dev.adf.awesomechatdiscord.api.events.InteractiveChatDiscordSRVConfigReloadEvent;
import dev.adf.awesomechatdiscord.listeners.InboundToGameEvents;
import dev.adf.awesomechatdiscord.listeners.InboundToGameEvents.DiscordAttachmentData;
import dev.adf.awesomechatdiscord.registry.ResourceRegistry;
import dev.adf.awesomechatdiscord.resources.ResourcePackInfo;
import dev.adf.awesomechatdiscord.updater.Updater;
import dev.adf.awesomechatdiscord.updater.Updater.UpdaterResponse;
import dev.adf.awesomechatdiscord.utils.ResourcePackInfoUtils;
import dev.adf.awesomechatdiscord.utils.TranslationKeyUtils;
import dev.adf.awesomechatdiscord.wrappers.GraphicsToPacketMapWrapper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Commands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!label.equalsIgnoreCase("awesomechatdiscord") && !label.equalsIgnoreCase("acd") && !label.equalsIgnoreCase("awesomechatdiscordsrv")) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "AwesomeChat DiscordSRV Addon written by LOOHP!");
            sender.sendMessage(ChatColor.GOLD + "You are running ICDiscordSRVAddon version: " + AwesomeChatDiscordAddon.plugin.getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (sender.hasPermission("awesomechatdiscord.status")) {
                sender.sendMessage(AwesomeChatDiscordAddon.plugin.defaultResourceHashLang.replaceFirst("%s", AwesomeChatDiscordAddon.plugin.defaultResourceHash + " (" + IC.exactMinecraftVersion + ")"));
                sender.sendMessage(AwesomeChatDiscordAddon.plugin.loadedResourcesLang);
                for (ResourcePackInfo info : AwesomeChatDiscordAddon.plugin.getResourceManager().getResourcePackInfo()) {
                    Component name = ResourcePackInfoUtils.resolveName(info);
                    if (info.getStatus()) {
                        Component component = Component.text(" - ").append(name).color(info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) == 0 ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
                        Component hoverComponent = ResourcePackInfoUtils.resolveDescription(info);
                        if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) > 0) {
                            hoverComponent = hoverComponent.append(Component.text("\n")).append(Component.translatable(TranslationKeyUtils.getNewIncompatiblePack()).color(NamedTextColor.YELLOW));
                        } else if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) < 0) {
                            hoverComponent = hoverComponent.append(Component.text("\n")).append(Component.translatable(TranslationKeyUtils.getOldIncompatiblePack()).color(NamedTextColor.YELLOW));
                        }
                        component = component.hoverEvent(HoverEvent.showText(hoverComponent));
                        ICApi.sendMessage(sender, component);
                        if (!(sender instanceof Player)) {
                            for (Component each : ComponentStyling.splitAtLineBreaks(ResourcePackInfoUtils.resolveDescription(info))) {
                                ICApi.sendMessage(sender, Component.text("   - ").color(NamedTextColor.GRAY).append(each));
                                if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) > 0) {
                                    sender.sendMessage(ChatColor.YELLOW + "     " + LanguageUtils.getTranslation(TranslationKeyUtils.getNewIncompatiblePack(), AwesomeChatDiscordAddon.plugin.language).getResult());
                                } else if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) < 0) {
                                    sender.sendMessage(ChatColor.YELLOW + "     " + LanguageUtils.getTranslation(TranslationKeyUtils.getOldIncompatiblePack(), AwesomeChatDiscordAddon.plugin.language).getResult());
                                }
                            }
                        }
                    } else {
                        Component component = Component.text(" - ").append(name).color(NamedTextColor.RED);
                        if (info.getRejectedReason() != null) {
                            component = component.hoverEvent(HoverEvent.showText(Component.text(info.getRejectedReason()).color(NamedTextColor.RED)));
                        }
                        ICApi.sendMessage(sender, component);
                        if (!(sender instanceof Player)) {
                            ICApi.sendMessage(sender, Component.text("   - ").append(Component.text(info.getRejectedReason()).color(NamedTextColor.RED)).color(NamedTextColor.RED));
                        }
                    }
                }
            } else {
                IC.sendMessage(sender, IC.noPermissionMessage);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadconfig")) {
            if (sender.hasPermission("awesomechatdiscord.reloadconfig")) {
                try {
                    if (AwesomeChatDiscordAddon.plugin.resourceReloadLock.tryLock(0, TimeUnit.MILLISECONDS)) {
                        try {
                            AwesomeChatDiscordAddon.plugin.reloadConfig();
                            Bukkit.getPluginManager().callEvent(new InteractiveChatDiscordSRVConfigReloadEvent());
                            sender.sendMessage(AwesomeChatDiscordAddon.plugin.reloadConfigMessage);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        } finally {
                            AwesomeChatDiscordAddon.plugin.resourceReloadLock.unlock();
                        }
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Resource reloading in progress, please wait!");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                IC.sendMessage(sender, IC.noPermissionMessage);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadtexture")) {
            List<String> argList = Arrays.asList(args);
            boolean clean = argList.contains("--reset");
            boolean redownload = argList.contains("--redownload") || clean;
            if (sender.hasPermission("awesomechatdiscord.reloadtexture")) {
                sender.sendMessage(AwesomeChatDiscordAddon.plugin.reloadTextureMessage);
                AwesomeChatDiscordAddon.plugin.reloadTextures(redownload, clean, sender);
            } else {
                IC.sendMessage(sender, IC.noPermissionMessage);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("awesomechatdiscord.update")) {
                sender.sendMessage(ChatColor.AQUA + "[AwesomeChatDiscordAddon] AwesomeChat DiscordSRV Addon written by LOOHP!");
                sender.sendMessage(ChatColor.GOLD + "[AwesomeChatDiscordAddon] You are running ICDiscordSRVAddon version: " + AwesomeChatDiscordAddon.plugin.getDescription().getVersion());
                Scheduler.runTaskAsynchronously(AwesomeChatDiscordAddon.plugin, () -> {
                    UpdaterResponse version = Updater.checkUpdate();
                    if (version.getResult().equals("latest")) {
                        if (version.isDevBuildLatest()) {
                            sender.sendMessage(ChatColor.GREEN + "[AwesomeChatDiscordAddon] You are running the latest version!");
                        } else {
                            Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId(), true);
                        }
                    } else {
                        Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId());
                    }
                });
            } else {
                IC.sendMessage(sender, IC.noPermissionMessage);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("imagemap")) {
            if (args.length > 1 && sender instanceof Player) {
                try {
                    DiscordAttachmentData data = InboundToGameEvents.DATA.get(UUID.fromString(args[1]));
                    if (data != null && (data.isImage() || data.isVideo())) {
                        GraphicsToPacketMapWrapper imageMap = data.getImageMap();
                        if (imageMap.futureCancelled()) {
                            sender.sendMessage(AwesomeChatDiscordAddon.plugin.linkExpired);
                        } else if (imageMap.futureCompleted()) {
                            if (imageMap.getColors() == null || imageMap.getColors().isEmpty()) {
                                sender.sendMessage(AwesomeChatDiscordAddon.plugin.linkExpired);
                            } else {
                                imageMap.show((Player) sender);
                            }
                        } else {
                            sender.sendMessage(AwesomeChatDiscordAddon.plugin.previewLoading);
                        }
                    } else {
                        sender.sendMessage(AwesomeChatDiscordAddon.plugin.linkExpired);
                    }
                } catch (Exception e) {
                    sender.sendMessage(AwesomeChatDiscordAddon.plugin.linkExpired);
                    e.printStackTrace();
                }
            }
            return true;
        }

        sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', Bukkit.spigot().getConfig().getString("messages.unknown-command")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> tab = new LinkedList<>();
        if (!label.equalsIgnoreCase("awesomechatdiscord") && !label.equalsIgnoreCase("acd") && !label.equalsIgnoreCase("awesomechatdiscordsrv")) {
            return tab;
        }

        switch (args.length) {
            case 0:
                if (sender.hasPermission("awesomechatdiscord.reloadconfig")) {
                    tab.add("reloadconfig");
                }
                if (sender.hasPermission("awesomechatdiscord.reloadtexture")) {
                    tab.add("reloadtexture");
                }
                if (sender.hasPermission("awesomechatdiscord.update")) {
                    tab.add("update");
                }
                if (sender.hasPermission("awesomechatdiscord.status")) {
                    tab.add("status");
                }
                return tab;
            case 1:
                if (sender.hasPermission("awesomechatdiscord.reloadconfig")) {
                    if ("reloadconfig".startsWith(args[0].toLowerCase())) {
                        tab.add("reloadconfig");
                    }
                }
                if (sender.hasPermission("awesomechatdiscord.reloadtexture")) {
                    if ("reloadtexture".startsWith(args[0].toLowerCase())) {
                        tab.add("reloadtexture");
                    }
                }
                if (sender.hasPermission("awesomechatdiscord.update")) {
                    if ("update".startsWith(args[0].toLowerCase())) {
                        tab.add("update");
                    }
                }
                if (sender.hasPermission("awesomechatdiscord.status")) {
                    if ("status".startsWith(args[0].toLowerCase())) {
                        tab.add("status");
                    }
                }
                return tab;
            case 2:
                if (sender.hasPermission("awesomechatdiscord.reloadtexture")) {
                    if ("reloadtexture".equals(args[0].toLowerCase())) {
                        if ("--redownload".startsWith(args[1].toLowerCase())) {
                            tab.add("--redownload");
                        }
                        if ("--reset".startsWith(args[1].toLowerCase())) {
                            tab.add("--reset");
                        }
                    }
                }
                return tab;
            default:
                return tab;
        }
    }

}
