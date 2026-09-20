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

package dev.adf.awesomechatdiscord.nms;

import dev.adf.awesomechatdiscord.vendor.IC;
import dev.adf.awesomechatdiscord.AwesomeChatDiscordAddon;

import java.lang.reflect.InvocationTargetException;

public class NMSAddon {

    private static NMSAddonWrapper instance;

    @SuppressWarnings("deprecation")
    public synchronized static NMSAddonWrapper getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            Class<NMSAddonWrapper> nmsImplClass = (Class<NMSAddonWrapper>) Class.forName("dev.adf.awesomechatdiscord.nms." + IC.version.name());
            instance = nmsImplClass.getConstructor().newInstance();
            NMSAddonWrapper.setup(instance, AwesomeChatDiscordAddon.plugin);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            if (IC.version.isSupported()) {
                throw new RuntimeException("Missing NMSWrapper implementation for version " + IC.version.name(), e);
            } else {
                throw new RuntimeException("No NMSWrapper implementation for UNSUPPORTED version " + IC.version.name(), e);
            }
        }
    }

}
