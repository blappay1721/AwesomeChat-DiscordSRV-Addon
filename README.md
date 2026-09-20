# AwesomeChat DiscordSRV Addon

Renders [AwesomeChat](https://github.com/HackerADF/AwesomeChat)'s chat displays into
DiscordSRV messages. When a player sends `[item]`, `[inv]` or `[ec]` in game, Discord
receives a rendered image of the actual item model, inventory or ender chest instead of
the literal trigger text.

> This is a fork of [InteractiveChat-DiscordSRV-Addon](https://github.com/LOOHP/InteractiveChat-DiscordSRV-Addon)
> by **LoohpJames**, rewired to read AwesomeChat instead of InteractiveChat. The rendering
> engine — resource pack loading, 3D block and item model rendering, font rendering,
> banners, shields, maps and player skins — is his work, used essentially unchanged.
> Please support the original project.

## Requirements

| Dependency | Type | Notes |
|---|---|---|
| [AwesomeChat](https://github.com/HackerADF/AwesomeChat) | **Required** | Provides the chat triggers |
| [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) | **Required** | Tested against 1.30.5 |
| Paper 1.19+ | **Required** | Spigot works, Paper recommended |
| Java 21+ | **Required** | |
| PlaceholderAPI | Optional | Placeholders in custom triggers |
| ItemsAdder / CraftEngine | Optional | Custom item textures |
| ImageFrame | Optional | Map image support |

## What it does

**Game → Discord**
- `[item]` / `[hand]` / `[this]` — renders the held item with its full tooltip
- `[inv]` / `[inventory]` — renders the player's inventory as an image
- `[ec]` / `[echest]` / `[enderchest]` — renders the ender chest
- Custom triggers defined in AwesomeChat's `item-display.custom-triggers`
- Dropdown menus to inspect any individual slot
- Death and advancement messages rendered with icons

**Discord → Game**
- Images and GIFs posted in Discord become viewable in chat
- Discord mentions translated into readable names

**Discord slash commands**

`/item`, `/inv`, `/ender`, and the `asuser` variants (`/itemasuser`, `/invasuser`,
`/enderasuser`), plus `/playerinfo`, `/playerlist` and `/resourcepack`.

## Configuration

Trigger syntax comes from **AwesomeChat's** `config.yml` — prefix, suffix, formats, GUI
titles and custom triggers are all read from there, so the two plugins can never
disagree about what a trigger looks like. Everything Discord-side (embeds, renderer
threads, resource packs, listener priorities) lives in this plugin's own `config.yml`.

Permissions reuse AwesomeChat's nodes: `awesomechat.display.item`,
`awesomechat.display.inventory`, `awesomechat.display.enderchest`.

### In-game commands

`/awesomechatdiscord` (aliases `/acd`, `/awesomechatdiscordsrv`) with subcommands
`status`, `reloadconfig`, `reloadtexture` and `update`.

| Permission | Default |
|---|---|
| `awesomechatdiscord.status` | everyone |
| `awesomechatdiscord.reloadconfig` | op |
| `awesomechatdiscord.reloadtexture` | op |
| `awesomechatdiscord.update` | op |

## Building

```bash
./gradlew :common:shadowJar
```

The jar lands in `common/build/libs/`.

### NMS modules

The `nms/` modules compile against **Spigot-mapped** `org.spigotmc:spigot`, which is on no
public repository. [BuildTools](https://www.spigotmc.org/wiki/buildtools/) must install it
into your local `~/.m2` first, one run per Minecraft version:

```bash
java -jar BuildTools.jar --rev 1.21.5 --remapped
```

Then build against only the versions you run:

```bash
./gradlew :common:shadowJar -Pnms=V1_21_5
```

`-Pnms` takes a comma-separated list (`-Pnms=V1_21_4,V1_21_5`); `-PwithNms` includes all
24, which needs a BuildTools run for every one.

> **Without an NMS module the plugin enables but throws on the first render**, because
> `NMS.getInstance()` resolves its implementation reflectively by version name. A
> production jar needs the module matching your server.

Use the **default** classifier, not `remapped-mojang`: the sources are written against
Spigot names (`BlockPosition`, `MinecraftKey`), not Mojang ones (`BlockPos`,
`ResourceLocation`). Paper remaps Spigot-mapped plugins at load, so these run on Paper
as well as Spigot.

If you run BuildTools under WSL it installs into WSL's `~/.m2`, not the Windows one.
Either build from WSL too, or copy the artifacts across:

```bash
cp -rn ~/.m2/repository/org/spigotmc /mnt/c/Users/<you>/.m2/repository/org/
```

### Tests

```bash
./gradlew checkTriggers
```

Verifies the trigger patterns still match after `ICPlaceholder` rewrites them to tolerate
colour codes. That rewrite walks a regex character by character, so a pattern built with
`Pattern.quote` silently becomes literal text that matches nothing — this check exists
because exactly that shipped once.

## Project layout

All sources live under `src/main/java/dev/adf/awesomechatdiscord/`, elided below.

```
.
├── abstraction/              Types shared with every NMS module
│   ├── grahpics/             Base image helpers (upstream's spelling)
│   ├── nms/                  NMSAddonWrapper — the per-version contract
│   ├── objectholders/
│   └── vendor/               Vendored from InteractiveChat (GPL-3.0)
│       ├── IC.java           Stands in for InteractiveChat's static config
│       ├── ICApi.java        Stands in for its public API
│       ├── BungeeMessageSender.java   No-op; this fork is single-server
│       ├── config/ events/ modules/ objectholders/ registry/ updater/ utils/
│       └── nms/              InteractiveChat's own NMS contract
│
├── common/                   The plugin itself
│   ├── AwesomeChatDiscordAddon.java   Entry point
│   ├── AwesomeChatBridge.java         Reads AwesomeChat's trigger config
│   ├── graphics/             ImageGeneration, ImageUtils, banners, GIF/APNG
│   ├── listeners/            DiscordSRV events in/out, slash commands
│   ├── resources/            Resource packs, models, fonts, languages, mods
│   ├── hooks/                ItemsAdder, ImageFrame, CraftEngine
│   ├── objectholders/ registry/ utils/ wrappers/ debug/ metrics/ updater/
│   └── main/                 Standalone model + font renderer tools
│
├── nms/                      One module per Minecraft revision
│   ├── V1_19/ … V26_2/       24 modules, each compiled against its own server jar
│   │   ├── nms/V1_21_5.java          implements NMSAddonWrapper
│   │   └── vendor/nms/ICV1_21_5.java implements InteractiveChat's NMSWrapper
│   └── …
│
├── build.gradle              Shared config, NMS version table, shaded libraries
└── settings.gradle           Module list; NMS modules are opt-in
```

Item data — NBT, data components, map pixels, rarity, skull profiles — lives on server
internals that are obfuscated differently in every Minecraft release, so there is one
small module per revision behind a shared interface. Only the module matching the
running server is ever loaded, resolved reflectively by version name at startup.

## Differences from upstream

- Minecraft 1.8–1.18 dropped (AwesomeChat requires 1.19+), removing 19 NMS modules
- VentureChat hook removed — it occupies the same slot as AwesomeChat
- Proxy/BungeeCord support removed; AwesomeChat is single-server
- MySQL-PlayerDataBridge, the nickname registry and chat signing removed
- InteractiveChat's shaded library bundle replaced with direct dependencies
  (Adventure 5, gson, json-simple, querz-NBT, XSeries, Simple-YAML, commons), shaded
  and relocated under `dev.adf.awesomechatdiscord.libs`

Asset downloads still use `api.loohpjames.com`, which serves the Minecraft resource data
the renderer needs. Changing those URLs breaks rendering.

## Licence

**GPL-3.0**, inherited from the upstream addon — see [LICENSE](LICENSE). Note that
AwesomeChat itself is MIT; this is a separate plugin under a separate licence.

Original work © 2020–2025 LoohpJames and contributors.
