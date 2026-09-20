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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import dev.adf.awesomechatdiscord.resources.models.BlockModel;
import dev.adf.awesomechatdiscord.resources.models.IModelManager;
import dev.adf.awesomechatdiscord.resources.models.ModelDisplay;
import dev.adf.awesomechatdiscord.resources.models.ModelDisplay.ModelDisplayPosition;
import dev.adf.awesomechatdiscord.resources.models.ModelElement;
import dev.adf.awesomechatdiscord.resources.models.ModelGUILight;
import dev.adf.awesomechatdiscord.resources.models.ModelOverride;
import dev.adf.awesomechatdiscord.resources.models.ModelOverride.ModelOverrideType;
import dev.adf.awesomechatdiscord.resources.models.TextureInfo;
import dev.adf.awesomechatdiscord.resources.mods.chime.ChimeModelOverride.ChimeModelOverrideType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ChimeBlockModel extends BlockModel {

    public static ChimeBlockModel fromJson(IModelManager manager, String resourceLocation, JSONObject rootJson, boolean useLegacyOverrides) {
        BlockModel base = BlockModel.fromJson(manager, resourceLocation, rootJson, useLegacyOverrides);
        List<ChimeModelOverride> overrides;
        if (useLegacyOverrides) {
            overrides = new ArrayList<>();
            JSONArray overridesArray = (JSONArray) rootJson.get("overrides");
            if (overridesArray != null) {
                ListIterator<Object> itr = overridesArray.listIterator(overridesArray.size());
                while (itr.hasPrevious()) {
                    JSONObject overrideJson = (JSONObject) itr.previous();
                    JSONObject predicateJson = (JSONObject) overrideJson.get("predicate");
                    Map<ModelOverrideType, Float> predicates = new EnumMap<>(ModelOverrideType.class);
                    for (Object obj1 : predicateJson.keySet()) {
                        String predicateTypeKey = obj1.toString();
                        ModelOverrideType type = ModelOverrideType.fromKey(predicateTypeKey);
                        if (type != null) {
                            Object value = predicateJson.get(predicateTypeKey);
                            predicates.put(type, ((Number) value).floatValue());
                        }
                    }
                    Map<ChimeModelOverrideType, Object> chimePredicates = ChimeUtils.getAllPredicates(predicateJson);
                    String model = (String) overrideJson.get("model");
                    if (overrideJson.containsKey("texture")) {
                        String armorTexture = (String) overrideJson.get("texture");
                        overrides.add(new ChimeModelOverride(predicates, chimePredicates, model, armorTexture));
                    } else {
                        overrides.add(new ChimeModelOverride(predicates, chimePredicates, model));
                    }
                }
            }
        } else {
            overrides = Collections.emptyList();
        }
        return new ChimeBlockModel(base, overrides);
    }

    public ChimeBlockModel(IModelManager manager, String resourceLocation, String parent, boolean ambientocclusion, ModelGUILight guiLight, Map<ModelDisplayPosition, ModelDisplay> display, Map<String, TextureInfo> textures, List<ModelElement> elements, List<ChimeModelOverride> overrides) {
        super(manager, resourceLocation, parent, ambientocclusion, guiLight, display, textures, elements, (List<ModelOverride>) (List<?>) overrides);
    }

    public ChimeBlockModel(BlockModel baseBlockModel, List<ChimeModelOverride> overrides) {
        super(baseBlockModel.getManager(), baseBlockModel.getResourceLocation(), baseBlockModel.getRawParent(), baseBlockModel.isAmbientocclusion(), baseBlockModel.getGUILight(), baseBlockModel.getRawDisplay(), baseBlockModel.getTextures(), baseBlockModel.getElements(), (List<ModelOverride>) (List<?>) overrides);
    }

    public List<ChimeModelOverride> getChimeOverrides() {
        return (List<ChimeModelOverride>) (List<?>) getOverrides();
    }

}
