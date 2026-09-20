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

package dev.adf.awesomechatdiscord.utils;

import dev.adf.awesomechatdiscord.vendor.IC;
import net.kyori.adventure.key.Key;
import dev.adf.awesomechatdiscord.vendor.utils.MCVersion;
import dev.adf.awesomechatdiscord.nms.NMSAddon;
import dev.adf.awesomechatdiscord.objectholders.BiomePrecipitation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;

public class WorldUtils {

    public static Key getNamespacedKey(World world) {
        return NMSAddon.getInstance().getNamespacedKey(world);
    }

    public static boolean isNatural(World world) {
        if (IC.version.isNewerOrEqualTo(MCVersion.V1_19)) {
            return world.isNatural();
        } else if (IC.version.isNewerOrEqualTo(MCVersion.V1_16)) {
            return NMSAddon.getInstance().getLegacyDimensionManager(world).natural();
        } else {
            return world.getEnvironment().equals(Environment.NORMAL);
        }
    }

    public static BiomePrecipitation getPrecipitation(Location location) {
        return NMSAddon.getInstance().getPrecipitation(location);
    }

}

