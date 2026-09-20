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

package dev.adf.awesomechatdiscord.resources.mods.optifine;

import dev.adf.awesomechatdiscord.vendor.objectholders.ValuePairs;
import dev.adf.awesomechatdiscord.resources.ResourcePackFile;
import dev.adf.awesomechatdiscord.resources.languages.SpecificTranslateFunction;
import dev.adf.awesomechatdiscord.resources.models.BlockModel;
import dev.adf.awesomechatdiscord.resources.models.IModelManager;
import dev.adf.awesomechatdiscord.resources.models.ModelOverride.ModelOverrideType;
import dev.adf.awesomechatdiscord.resources.mods.optifine.cit.CITGlobalProperties;
import dev.adf.awesomechatdiscord.resources.mods.optifine.cit.CITProperties;
import dev.adf.awesomechatdiscord.resources.mods.optifine.cit.EnchantmentProperties.OpenGLBlending;
import dev.adf.awesomechatdiscord.resources.textures.ITextureManager;
import dev.adf.awesomechatdiscord.resources.textures.TextureResource;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface IOptifineManager extends ITextureManager, IModelManager {

    Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> getItemPostResolveFunction(EquipmentSlot heldSlot, ItemStack itemStack, boolean is1_8, Map<ModelOverrideType, Float> predicates, SpecificTranslateFunction translateFunction);

    TextureResource getElytraOverrideTextures(EquipmentSlot heldSlot, ItemStack itemStack, SpecificTranslateFunction translateFunction);

    TextureResource getArmorOverrideTextures(String layer, EquipmentSlot heldSlot, ItemStack itemStack, SpecificTranslateFunction translateFunction);

    List<ValuePairs<TextureResource, OpenGLBlending>> getEnchantmentGlintOverrideTextures(EquipmentSlot heldSlot, ItemStack itemStack, SpecificTranslateFunction translateFunction);

    CITGlobalProperties getCITGlobalProperties();

    <T extends CITProperties> ValuePairs<ResourcePackFile, T> getCITOverride(EquipmentSlot heldSlot, ItemStack itemStack, SpecificTranslateFunction translateFunction, Class<T> type);

    <T extends CITProperties> List<ValuePairs<ResourcePackFile, T>> getCITOverrides(EquipmentSlot heldSlot, ItemStack itemStack, SpecificTranslateFunction translateFunction, Class<T> type);

}
