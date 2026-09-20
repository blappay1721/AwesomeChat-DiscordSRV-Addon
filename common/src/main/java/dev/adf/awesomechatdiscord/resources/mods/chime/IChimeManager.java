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

package dev.adf.awesomechatdiscord.resources.mods.chime;

import dev.adf.awesomechatdiscord.vendor.objectholders.OfflineICPlayer;
import dev.adf.awesomechatdiscord.resources.languages.SpecificTranslateFunction;
import dev.adf.awesomechatdiscord.resources.models.IModelManager;
import dev.adf.awesomechatdiscord.resources.textures.ITextureManager;
import dev.adf.awesomechatdiscord.resources.textures.TextureResource;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface IChimeManager extends IModelManager, ITextureManager {

    TextureResource getArmorOverrideTextures(String layer, ItemStack itemStack, OfflineICPlayer player, World world, LivingEntity entity, SpecificTranslateFunction translateFunction);

}
