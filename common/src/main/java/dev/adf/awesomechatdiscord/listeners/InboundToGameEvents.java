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
import dev.adf.awesomechatdiscord.vendor.events.PrePacketComponentProcessEvent;
import com.loohp.platformscheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder;
import dev.adf.awesomechatdiscord.vendor.utils.ComponentReplacing;
import dev.adf.awesomechatdiscord.vendor.utils.CustomStringUtils;
import dev.adf.awesomechatdiscord.vendor.utils.HTTPRequestUtils;
import dev.adf.awesomechatdiscord.AwesomeChatDiscordAddon;
import dev.adf.awesomechatdiscord.api.events.DiscordAttachmentConversionEvent;
import dev.adf.awesomechatdiscord.debug.Debug;
import dev.adf.awesomechatdiscord.graphics.APNGReader;
import dev.adf.awesomechatdiscord.graphics.GifReader;
import dev.adf.awesomechatdiscord.modules.DiscordToGameMention;
import dev.adf.awesomechatdiscord.objectholders.PreviewableImageContainer;
import dev.adf.awesomechatdiscord.utils.ThrowingSupplier;
import dev.adf.awesomechatdiscord.utils.URLRequestUtils;
import dev.adf.awesomechatdiscord.wrappers.GraphicsToPacketMapWrapper;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message.Attachment;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageSticker;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InboundToGameEvents implements Listener {

    public static final Pattern TENOR_HTML_PATTERN = Pattern.compile("<link class=\\\"dynamic\\\" rel=\\\"image_src\\\" href=\\\"https://media1\\.tenor\\.com/m/(.*?)/.*?\\\">");

    public static final Map<UUID, DiscordAttachmentData> DATA = new ConcurrentHashMap<>();
    public static final Map<Player, GraphicsToPacketMapWrapper> MAP_VIEWERS = new ConcurrentHashMap<>();

    @Subscribe(priority = ListenerPriority.LOWEST)
    public void onReceiveMessageFromDiscordPre(DiscordGuildMessagePreProcessEvent event) {
        Debug.debug("Triggering onReceiveMessageFromDiscordPre");
        DiscordSRV srv = AwesomeChatDiscordAddon.discordsrv;
        Map<Pattern, String> discordRegexes = srv.getDiscordRegexes();
        if (discordRegexes != null) {
            discordRegexes.keySet().removeIf(pattern -> pattern.pattern().equals("@+(everyone|here)"));
        }
    }

    @Subscribe(priority = ListenerPriority.LOW)
    public void onDiscordToGame(DiscordGuildMessagePostProcessEvent event) {
        Debug.debug("Triggering onDiscordToGame");
        AwesomeChatDiscordAddon.plugin.messagesCounter.incrementAndGet();
        github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component component = event.getMinecraftMessage();
        if (AwesomeChatDiscordAddon.plugin.escapePlaceholdersFromDiscord) {
            Debug.debug("onDiscordToGame escaping placeholders");
            for (ICPlaceholder placeholder : IC.placeholderList.values()) {
                component = component.replaceText(github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig.builder().match(placeholder.getKeyword()).replacement((result, builder) -> builder.content("\\" + result.group())).build());
            }
            event.setMinecraftMessage(component);
        }
    }

    @Subscribe(priority = ListenerPriority.LOWEST)
    public void onReceiveMessageFromDiscordPostLowest(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.LOWEST)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    @Subscribe(priority = ListenerPriority.LOW)
    public void onReceiveMessageFromDiscordPostLow(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.LOW)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onReceiveMessageFromDiscordPostNormal(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.NORMAL)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    @Subscribe(priority = ListenerPriority.HIGH)
    public void onReceiveMessageFromDiscordPostHigh(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.HIGH)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    @Subscribe(priority = ListenerPriority.HIGHEST)
    public void onReceiveMessageFromDiscordPostHighest(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.HIGHEST)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onReceiveMessageFromDiscordPostMonitor(DiscordGuildMessagePostProcessEvent event) {
        if (AwesomeChatDiscordAddon.plugin.discordToGamePriority.equals(ListenerPriority.MONITOR)) {
            handleReceiveMessageFromDiscordPost(event);
        }
    }

    public void handleReceiveMessageFromDiscordPost(DiscordGuildMessagePostProcessEvent event) {
        try {
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<?> future = service.submit(() -> {
                Debug.debug("Triggering onReceiveMessageFromDiscordPost");
                Message message = event.getMessage();

                github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component component = event.getMinecraftMessage();

                DiscordSRV srv = AwesomeChatDiscordAddon.discordsrv;
                User author = message.getAuthor();

                if (AwesomeChatDiscordAddon.plugin.translateMentions) {
                    Debug.debug("onReceiveMessageFromDiscordPost translating mentions");

                    Set<UUID> mentionTitleSent = new HashSet<>();
                    Map<Member, UUID> channelMembers = new HashMap<>();

                    TextChannel channel = event.getChannel();
                    Guild guild = channel.getGuild();
                    Member authorAsMember = guild.getMember(author);
                    String senderDiscordName = authorAsMember == null ? author.getName() : authorAsMember.getEffectiveName();
                    UUID senderUUID = srv.getAccountLinkManager().getUuid(author.getId());

                    for (Entry<UUID, String> entry : srv.getAccountLinkManager().getManyDiscordIds(Bukkit.getOnlinePlayers().stream().map(each -> each.getUniqueId()).collect(Collectors.toSet())).entrySet()) {
                        Member member = guild.getMemberById(entry.getValue());
                        if (member != null && member.hasAccess(channel)) {
                            channelMembers.put(member, entry.getKey());
                        }
                    }

                    if (message.mentionsEveryone()) {
                        //github.scarsz.discordsrv.dependencies.kyori.adventure.text.event.HoverEvent<Component> hover = Component.text(AwesomeChatDiscordAddon.plugin.mentionHover.replace("{DiscordUser}", senderDiscordName).replace("{TextChannel}", "#" + channel.getName()).replace("{Guild}", guild.getName())).asHoverEvent();
                        component = component.replaceText(github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("@here").replacement(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text(AwesomeChatDiscordAddon.plugin.mentionHighlight.replace("{DiscordMention}", "@here"))).build()).replaceText(github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("@everyone").replacement(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text(AwesomeChatDiscordAddon.plugin.mentionHighlight.replace("{DiscordMention}", "@everyone"))).build());
                        for (UUID uuid : channelMembers.values()) {
                            mentionTitleSent.add(uuid);
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                DiscordToGameMention.playTitleScreen(senderDiscordName, channel.getName(), guild.getName(), player);
                            }
                        }
                    }

                    List<Role> mentionedRoles = message.getMentionedRoles();
                    for (Role role : mentionedRoles) {
                        //github.scarsz.discordsrv.dependencies.kyori.adventure.text.event.HoverEvent<Component> hover = Component.text(AwesomeChatDiscordAddon.plugin.mentionHover.replace("{DiscordUser}", senderDiscordName).replace("{TextChannel}", "#" + channel.getName()).replace("{Guild}", guild.getName())).asHoverEvent();
                        component = component.replaceText(github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("@" + role.getName()).replacement(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text(AwesomeChatDiscordAddon.plugin.mentionHighlight.replace("{DiscordMention}", "@" + role.getName()))).build());
                        for (Entry<Member, UUID> entry : channelMembers.entrySet()) {
                            UUID uuid = entry.getValue();
                            if (!mentionTitleSent.contains(uuid) && entry.getKey().getRoles().contains(role)) {
                                mentionTitleSent.add(uuid);
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) {
                                    DiscordToGameMention.playTitleScreen(senderDiscordName, channel.getName(), guild.getName(), player);
                                }
                            }
                        }
                    }

                    List<User> mentionedUsers = message.getMentionedUsers();
                    if (!mentionedUsers.isEmpty()) {
                        for (User user : mentionedUsers) {
                            //github.scarsz.discordsrv.dependencies.kyori.adventure.text.event.HoverEvent<Component> hover = Component.text(AwesomeChatDiscordAddon.plugin.mentionHover.replace("{DiscordUser}", senderDiscordName).replace("{TextChannel}", "#" + channel.getName()).replace("{Guild}", guild.getName())).asHoverEvent();
                            component = component.replaceText(github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("@" + user.getName()).replacement(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text(AwesomeChatDiscordAddon.plugin.mentionHighlight.replace("{DiscordMention}", "@" + user.getName()))).build());
                            Member member = guild.getMember(user);
                            if (member != null) {
                                UUID uuid = channelMembers.get(member);
                                if (uuid != null && !mentionTitleSent.contains(uuid) && (senderUUID == null || !senderUUID.equals(uuid))) {
                                    mentionTitleSent.add(uuid);
                                    Player player = Bukkit.getPlayer(uuid);
                                    if (player != null) {
                                        DiscordToGameMention.playTitleScreen(senderDiscordName, channel.getName(), guild.getName(), player);
                                    }
                                }
                            }
                        }
                    }

                    event.setMinecraftMessage(component);
                }

                String processedMessage = MessageUtil.toLegacy(component);

                if (AwesomeChatDiscordAddon.plugin.convertDiscordAttachments) {
                    Debug.debug("onReceiveMessageFromDiscordPost converting discord attachments");
                    Set<String> processedUrl = new HashSet<>();
                    List<PreviewableImageContainer> previewableImageContainers = new ArrayList<>(message.getAttachments().size() + message.getStickers().size());
                    for (Attachment attachment : message.getAttachments()) {
                        AwesomeChatDiscordAddon.plugin.attachmentCounter.incrementAndGet();
                        String url = attachment.getUrl();
                        if (processedMessage.contains(url)) {
                            processedUrl.add(url);
                            if ((attachment.isImage() || attachment.isVideo()) && attachment.getSize() <= AwesomeChatDiscordAddon.plugin.discordAttachmentsPreviewLimit) {
                                previewableImageContainers.add(PreviewableImageContainer.fromAttachment(attachment));
                            } else {
                                DiscordAttachmentData data = new DiscordAttachmentData(attachment.getFileName(), url);
                                DiscordAttachmentConversionEvent dace = new DiscordAttachmentConversionEvent(url, data);
                                Bukkit.getPluginManager().callEvent(dace);
                                DATA.put(data.getUniqueId(), data);
                                Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> DATA.remove(data.getUniqueId()), AwesomeChatDiscordAddon.plugin.discordAttachmentTimeout);
                            }
                        }
                    }
                    for (MessageSticker sticker : message.getStickers()) {
                        previewableImageContainers.add(PreviewableImageContainer.fromSticker(sticker));
                    }
                    for (PreviewableImageContainer imageContainer : previewableImageContainers) {
                        AwesomeChatDiscordAddon.plugin.attachmentImageCounter.incrementAndGet();
                        String url = imageContainer.getUrl();
                        List<ThrowingSupplier<InputStream>> methods = new ArrayList<>();
                        for (String url0 : imageContainer.getAllUrls()) {
                            if (URLRequestUtils.isAllowed(url0)) {
                                methods.add(() -> URLRequestUtils.getInputStream0(url0));
                            }
                        }

                        try (InputStream stream = URLRequestUtils.retrieveUntilSuccessful(methods)) {
                            String type = imageContainer.getContentType();
                            GraphicsToPacketMapWrapper map;
                            boolean isVideo = false;
                            if (type.endsWith("gif.png") || type.endsWith("apng")) {
                                map = new GraphicsToPacketMapWrapper(AwesomeChatDiscordAddon.plugin.playbackBarEnabled, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                                APNGReader.readAPNG(stream, AwesomeChatDiscordAddon.plugin.mediaReadingService, (frames, e) -> {
                                    if (e != null) {
                                        e.printStackTrace();
                                        map.completeFuture(null);
                                    } else {
                                        map.completeFuture(frames);
                                    }
                                });
                            } else if (type.endsWith("gif")) {
                                map = new GraphicsToPacketMapWrapper(AwesomeChatDiscordAddon.plugin.playbackBarEnabled, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                                GifReader.readGif(stream, AwesomeChatDiscordAddon.plugin.mediaReadingService, (frames, e) -> {
                                    if (e != null) {
                                        e.printStackTrace();
                                        map.completeFuture(null);
                                    } else {
                                        map.completeFuture(frames);
                                    }
                                });
                            } else {
                                BufferedImage image = ImageIO.read(stream);
                                map = new GraphicsToPacketMapWrapper(image, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                            }
                            DiscordAttachmentData data = new DiscordAttachmentData(imageContainer.getName(), url, map, isVideo);
                            DiscordAttachmentConversionEvent dace = new DiscordAttachmentConversionEvent(url, data);
                            Bukkit.getPluginManager().callEvent(dace);
                            DATA.put(data.getUniqueId(), data);
                            Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> DATA.remove(data.getUniqueId()), AwesomeChatDiscordAddon.plugin.discordAttachmentTimeout);
                        } catch (Exception e) {
                            e.printStackTrace();
                            DiscordAttachmentData data = new DiscordAttachmentData(imageContainer.getName(), url);
                            DiscordAttachmentConversionEvent dace = new DiscordAttachmentConversionEvent(url, data);
                            Bukkit.getPluginManager().callEvent(dace);
                            DATA.put(data.getUniqueId(), data);
                            Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> DATA.remove(data.getUniqueId()), AwesomeChatDiscordAddon.plugin.discordAttachmentTimeout);
                        }
                    }

                    Matcher matcher = URLRequestUtils.URL_PATTERN.matcher(message.getContentRaw());
                    while (matcher.find()) {
                        String url = matcher.group();
                        String imageUrl = url;
                        if (!processedUrl.contains(url) && URLRequestUtils.isAllowed(url)) {
                            if (url.startsWith("https://tenor.com/")) {
                                try {
                                    String html = HTTPRequestUtils.getTextResponse(url);
                                    Matcher matcher2 = TENOR_HTML_PATTERN.matcher(html);
                                    if (matcher2.find()) {
                                        imageUrl = "https://c.tenor.com/" + matcher2.group(1) + "/tenor.gif";
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            long size = HTTPRequestUtils.getContentSize(imageUrl);
                            if (size >= 0 && size <= AwesomeChatDiscordAddon.plugin.discordAttachmentsPreviewLimit) {
                                AwesomeChatDiscordAddon.plugin.attachmentImageCounter.incrementAndGet();
                                try (InputStream stream = URLRequestUtils.getInputStream(imageUrl)) {
                                    String type = HTTPRequestUtils.getContentType(imageUrl);

                                    if (type == null || !type.startsWith("image/")) {
                                        continue;
                                    }
                                    GraphicsToPacketMapWrapper map;
                                    boolean isVideo = false;
                                    if (type.endsWith("gif.png") || type.endsWith("apng")) {
                                        map = new GraphicsToPacketMapWrapper(AwesomeChatDiscordAddon.plugin.playbackBarEnabled, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                                        APNGReader.readAPNG(stream, AwesomeChatDiscordAddon.plugin.mediaReadingService, (frames, e) -> {
                                            if (e != null) {
                                                e.printStackTrace();
                                                map.completeFuture(null);
                                            } else {
                                                map.completeFuture(frames);
                                            }
                                        });
                                    } else if (type.endsWith("gif")) {
                                        map = new GraphicsToPacketMapWrapper(AwesomeChatDiscordAddon.plugin.playbackBarEnabled, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                                        GifReader.readGif(stream, AwesomeChatDiscordAddon.plugin.mediaReadingService, (frames, e) -> {
                                            if (e != null) {
                                                e.printStackTrace();
                                                map.completeFuture(null);
                                            } else {
                                                map.completeFuture(frames);
                                            }
                                        });
                                    } else {
                                        BufferedImage image = ImageIO.read(stream);
                                        map = new GraphicsToPacketMapWrapper(image, AwesomeChatDiscordAddon.plugin.discordAttachmentsMapBackgroundColor);
                                    }
                                    String name = matcher.group(1);
                                    DiscordAttachmentData data = new DiscordAttachmentData(name, url, map, isVideo);
                                    DiscordAttachmentConversionEvent dace = new DiscordAttachmentConversionEvent(url, data);
                                    Bukkit.getPluginManager().callEvent(dace);
                                    DATA.put(data.getUniqueId(), data);
                                    Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> DATA.remove(data.getUniqueId()), AwesomeChatDiscordAddon.plugin.discordAttachmentTimeout);
                                } catch (FileNotFoundException ignore) {
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
            future.get(5000, TimeUnit.MILLISECONDS);
            service.shutdownNow();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onChatPacket(PrePacketComponentProcessEvent event) {
        Debug.debug("Triggering onChatPacket");
        if (AwesomeChatDiscordAddon.plugin.convertDiscordAttachments) {
            Debug.debug("onChatPacket converting discord attachments");
            for (Entry<UUID, DiscordAttachmentData> entry : DATA.entrySet()) {
                DiscordAttachmentData data = entry.getValue();
                String url = data.getUrl();
                Component component = event.getComponent();

                String replacement = AwesomeChatDiscordAddon.plugin.discordAttachmentsFormattingText.replace("{FileName}", data.getFileName());
                Component textComponent = LegacyComponentSerializer.legacySection().deserialize(replacement);
                if (AwesomeChatDiscordAddon.plugin.discordAttachmentsFormattingHoverEnabled) {
                    String hover = AwesomeChatDiscordAddon.plugin.discordAttachmentsFormattingHoverText.replace("{FileName}", data.getFileName());
                    textComponent = textComponent.hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(hover)));
                }
                if (AwesomeChatDiscordAddon.plugin.discordAttachmentsImagesUseMaps && data.isImage()) {
                    textComponent = textComponent.clickEvent(ClickEvent.runCommand("/awesomechatdiscord imagemap " + data.getUniqueId().toString()));
                    Component imageAppend = LegacyComponentSerializer.legacySection().deserialize(AwesomeChatDiscordAddon.plugin.discordAttachmentsFormattingImageAppend.replace("{FileName}", data.getFileName()));
                    imageAppend = imageAppend.hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(AwesomeChatDiscordAddon.plugin.discordAttachmentsFormattingImageAppendHover.replace("{FileName}", data.getFileName()))));
                    imageAppend = imageAppend.clickEvent(ClickEvent.openUrl(url));
                    textComponent = textComponent.append(imageAppend);
                } else {
                    textComponent = textComponent.clickEvent(ClickEvent.openUrl(url));
                }

                component = ComponentReplacing.replace(component, "\\\\?" + CustomStringUtils.escapeMetaCharacters(url), textComponent);

                event.setComponent(component);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventory(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        boolean removed = MAP_VIEWERS.remove(player) != null;

        if (removed) {
            player.getInventory().setItemInHand(player.getInventory().getItemInHand());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> {
                boolean removed = MAP_VIEWERS.remove(player) != null;

                if (removed) {
                    player.getInventory().setItemInHand(player.getInventory().getItemInHand());
                }
            }, 1, player);
        } else {
            boolean removed = MAP_VIEWERS.remove(player) != null;

            if (removed) {
                player.getInventory().setItemInHand(player.getInventory().getItemInHand());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventory(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();
        boolean removed = MAP_VIEWERS.remove(player) != null;

        int slot = event.getSlot();

        if (removed) {
            if (player.getInventory().equals(event.getClickedInventory()) && slot >= 9) {
                ItemStack item = player.getInventory().getItem(slot);
                Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> player.getInventory().setItem(slot, item), 1, player);
            } else {
                event.setCursor(null);
            }
        }

        if (removed) {
            player.getInventory().setItemInHand(player.getInventory().getItemInHand());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        if (event.getNewSlot() == event.getPreviousSlot()) {
            return;
        }

        Player player = event.getPlayer();
        boolean removed = MAP_VIEWERS.remove(player) != null;

        if (removed) {
            player.getInventory().setItemInHand(player.getInventory().getItemInHand());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.PHYSICAL)) {
            return;
        }
        Player player = event.getPlayer();

        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            Scheduler.runTaskLater(AwesomeChatDiscordAddon.plugin, () -> {
                boolean removed = MAP_VIEWERS.remove(player) != null;

                if (removed) {
                    player.getInventory().setItemInHand(player.getInventory().getItemInHand());
                }
            }, 1, player);
        } else {
            boolean removed = MAP_VIEWERS.remove(player) != null;

            if (removed) {
                player.getInventory().setItemInHand(player.getInventory().getItemInHand());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            boolean removed = MAP_VIEWERS.remove(player) != null;

            if (removed) {
                player.getInventory().setItemInHand(player.getInventory().getItemInHand());
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        MAP_VIEWERS.remove(event.getPlayer());
    }

    public static class DiscordAttachmentData {

        private final String fileName;
        private final String url;
        private final GraphicsToPacketMapWrapper imageMap;
        private final UUID uuid;
        private final boolean isVideo;

        public DiscordAttachmentData(String fileName, String url, GraphicsToPacketMapWrapper imageMap, boolean isVideo) {
            this.fileName = fileName;
            this.url = url;
            this.imageMap = imageMap;
            this.uuid = UUID.randomUUID();
            this.isVideo = isVideo;
        }

        public DiscordAttachmentData(String fileName, String url) {
            this(fileName, url, null, false);
        }

        public String getFileName() {
            return fileName;
        }

        public String getUrl() {
            return url;
        }

        public boolean isImage() {
            return imageMap != null && !isVideo;
        }

        public boolean isVideo() {
            return imageMap != null && isVideo;
        }

        public GraphicsToPacketMapWrapper getImageMap() {
            return imageMap;
        }

        public UUID getUniqueId() {
            return uuid;
        }

        public int hashCode() {
            return 17 * uuid.hashCode();
        }

        public boolean equals(Object object) {
            if (object instanceof DiscordAttachmentData) {
                return ((DiscordAttachmentData) object).uuid.equals(this.uuid);
            }
            return false;
        }

    }

}
