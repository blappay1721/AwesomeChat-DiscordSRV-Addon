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

package dev.adf.awesomechatdiscord.listeners;

import dev.adf.awesomechatdiscord.vendor.IC;
import dev.adf.awesomechatdiscord.vendor.ICApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlayer;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlayerFactory;
import dev.adf.awesomechatdiscord.vendor.objectholders.OfflineICPlayer;
import dev.adf.awesomechatdiscord.vendor.objectholders.ValuePairs;
import dev.adf.awesomechatdiscord.vendor.objectholders.ValueTrios;
import dev.adf.awesomechatdiscord.vendor.utils.ChatColorUtils;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentStyling;
import dev.adf.awesomechatdiscord.vendor.utils.PlaceholderParser;
import dev.adf.awesomechatdiscord.vendor.utils.PlayerUtils;
import dev.adf.awesomechatdiscord.AwesomeChatDiscordAddon;
import dev.adf.awesomechatdiscord.graphics.ImageGeneration;
import dev.adf.awesomechatdiscord.graphics.ImageUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordChatChannelListCommandMessageEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LegacyDiscordCommandEvents {

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onListPlayers(DiscordChatChannelListCommandMessageEvent event) {
        if (!AwesomeChatDiscordAddon.plugin.playerlistCommandEnabled) {
            return;
        }
        DiscordSRV discordsrv = DiscordSRV.getPlugin();
        TextChannel channel = event.getChannel();
        if (AwesomeChatDiscordAddon.plugin.playerlistCommandIsMainServer) {
            String minecraftChannel = discordsrv.getChannels().entrySet().stream().filter(entry -> channel.getId().equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);
            if (minecraftChannel == null) {
                if (AwesomeChatDiscordAddon.plugin.respondToCommandsInInvalidChannels) {
                    event.setResult(DiscordChatChannelListCommandMessageEvent.Result.TREAT_AS_REGULAR_MESSAGE);
                }
                return;
            }
            if (AwesomeChatDiscordAddon.plugin.playerlistCommandDeleteAfter > 0) {
                event.setExpiration(AwesomeChatDiscordAddon.plugin.playerlistCommandDeleteAfter * 1000);
            } else {
                event.setExpiration(0);
            }
            Map<OfflinePlayer, Integer> players;
            if (IC.bungeecordMode && AwesomeChatDiscordAddon.plugin.playerlistCommandBungeecord && !Bukkit.getOnlinePlayers().isEmpty()) {
                try {
                    List<ValueTrios<UUID, String, Integer>> bungeePlayers = ICApi.getBungeecordPlayerList().get();
                    players = new LinkedHashMap<>(bungeePlayers.size());
                    for (ValueTrios<UUID, String, Integer> playerinfo : bungeePlayers) {
                        UUID uuid = playerinfo.getFirst();
                        ICPlayer icPlayer = ICPlayerFactory.getICPlayer(uuid);
                        if (icPlayer == null || !icPlayer.isVanished()) {
                            if (!AwesomeChatDiscordAddon.plugin.playerlistCommandOnlyInteractiveChatServers || ICPlayerFactory.getICPlayer(uuid) != null) {
                                players.put(Bukkit.getOfflinePlayer(uuid), playerinfo.getThird());
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    event.setPlayerListMessage(ChatColorUtils.stripColor(AwesomeChatDiscordAddon.plugin.unableToRetrieveData) + " (-1)");
                    return;
                }
            } else {
                players = Bukkit.getOnlinePlayers().stream().filter(each -> {
                    ICPlayer icPlayer = ICPlayerFactory.getICPlayer(each);
                    return icPlayer == null || !icPlayer.isVanished();
                }).collect(Collectors.toMap(each -> each, each -> PlayerUtils.getPing(each), (a, b) -> a));
            }
            if (players.isEmpty()) {
                event.setPlayerListMessage(ChatColorUtils.stripColor(AwesomeChatDiscordAddon.plugin.playerlistCommandEmptyServer));
            } else {
                int errorCode = -2;
                try {
                    List<ValueTrios<OfflineICPlayer, Component, Integer>> player = new ArrayList<>();
                    Map<UUID, ValuePairs<List<String>, String>> playerInfo = new HashMap<>();
                    for (Map.Entry<OfflinePlayer, Integer> entry : players.entrySet()) {
                        OfflinePlayer bukkitOfflinePlayer = entry.getKey();
                        @SuppressWarnings("deprecation")
                        OfflineICPlayer offlinePlayer = ICPlayerFactory.getUnsafe().getOfflineICPPlayerWithoutInitialization(bukkitOfflinePlayer.getUniqueId());
                        playerInfo.put(offlinePlayer.getUniqueId(), new ValuePairs<>(DiscordCommands.getPlayerGroups(bukkitOfflinePlayer), offlinePlayer.getName()));
                        String name = PlaceholderParser.parse(offlinePlayer, AwesomeChatDiscordAddon.plugin.playerlistCommandPlayerFormat);
                        Component nameComponent;
                        if (AwesomeChatDiscordAddon.plugin.playerlistCommandParsePlayerNamesWithMiniMessage) {
                            nameComponent = MiniMessage.miniMessage().deserialize(name);
                        } else {
                            nameComponent = LegacyComponentSerializer.legacySection().deserialize(ChatColorUtils.translateAlternateColorCodes('&', name));
                        }
                        player.add(new ValueTrios<>(offlinePlayer, nameComponent, entry.getValue()));
                    }
                    errorCode--;
                    DiscordCommands.sortPlayers(AwesomeChatDiscordAddon.plugin.playerlistOrderingTypes, player, playerInfo);
                    errorCode--;
                    @SuppressWarnings("deprecation")
                    OfflineICPlayer firstPlayer = ICPlayerFactory.getUnsafe().getOfflineICPPlayerWithoutInitialization(players.keySet().iterator().next().getUniqueId());
                    List<Component> header = new ArrayList<>();
                    if (!AwesomeChatDiscordAddon.plugin.playerlistCommandHeader.isEmpty()) {
                        header = ComponentStyling.splitAtLineBreaks(LegacyComponentSerializer.legacySection().deserialize(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(firstPlayer, AwesomeChatDiscordAddon.plugin.playerlistCommandHeader.replace("{OnlinePlayers}", players.size() + "")))));
                    }
                    errorCode--;
                    List<Component> footer = new ArrayList<>();
                    if (!AwesomeChatDiscordAddon.plugin.playerlistCommandFooter.isEmpty()) {
                        footer = ComponentStyling.splitAtLineBreaks(LegacyComponentSerializer.legacySection().deserialize(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(firstPlayer, AwesomeChatDiscordAddon.plugin.playerlistCommandFooter.replace("{OnlinePlayers}", players.size() + "")))));
                    }
                    errorCode--;
                    int playerListMaxPlayers = AwesomeChatDiscordAddon.plugin.playerlistMaxPlayers;
                    if (playerListMaxPlayers < 1) {
                        playerListMaxPlayers = Integer.MAX_VALUE;
                    }
                    BufferedImage image = ImageGeneration.getTabListImage(header, footer, player, AwesomeChatDiscordAddon.plugin.playerlistCommandAvatar, AwesomeChatDiscordAddon.plugin.playerlistCommandPing, playerListMaxPlayers);
                    errorCode--;
                    byte[] data = ImageUtils.toArray(image);
                    errorCode--;
                    channel.sendMessageEmbeds(new EmbedBuilder().setImage("attachment://Tablist.png").setColor(AwesomeChatDiscordAddon.plugin.playerlistCommandColor).build()).addFile(data, "Tablist.png").queue(success -> {
                        if (event.getExpiration() > 0) {
                            success.delete().queueAfter(event.getExpiration(), TimeUnit.MILLISECONDS);
                        }
                    });
                    event.setResult(DiscordChatChannelListCommandMessageEvent.Result.NO_ACTION);
                } catch (Exception e) {
                    e.printStackTrace();
                    event.setPlayerListMessage(ChatColorUtils.stripColor(AwesomeChatDiscordAddon.plugin.unableToRetrieveData) + " (" + errorCode + ")");
                    return;
                }
            }
            if (event.getExpiration() > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelListCommandExpirationDeleteRequest")) {
                event.getTriggeringJDAEvent().getMessage().delete().queueAfter(event.getExpiration(), TimeUnit.MILLISECONDS);
            }
        }
    }

}
