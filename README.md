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
- Images and GIFs posted in Discord become a clickable label in chat that opens them on
  an in-game map. Attachments, stickers, direct image links, and Tenor and Klipy GIF
  links are supported
- Discord mentions translated into readable names

**Discord slash commands**

`/item`, `/inv`, `/ender`, and the `asuser` variants (`/itemasuser`, `/invasuser`,
`/enderasuser`), plus `/playerinfo`, `/playerlist` and `/resourcepack`.

> **Image previews fetch every link posted in Discord** to find out whether it is an
> image. To limit this to sites you trust, set `DiscordAttachments.RestrictImageUrl.Enabled`
> to `true` in this plugin's `config.yml` and list the allowed URL prefixes.

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

Building needs the Spigot server jar for each Minecraft version you target, installed
locally with [BuildTools](https://www.spigotmc.org/wiki/buildtools/):

```bash
java -jar BuildTools.jar --rev 1.21.5 --remapped
./gradlew :common:shadowJar -Pnms=V1_21_5
```

Use the version your server runs. `-Pnms` takes a comma-separated list, and `-PwithNms`
builds every supported version. The jar lands in `common/build/libs/`.

## Differences from upstream

- Requires Minecraft 1.19+ (AwesomeChat's minimum)
- Single-server only: no BungeeCord/proxy support
- VentureChat and MySQL-PlayerDataBridge hooks removed
- Klipy GIF links get in-game previews

## Licence

**GPL-3.0**, inherited from the upstream addon — see [LICENSE](LICENSE). Note that
AwesomeChat itself is MIT; this is a separate plugin under a separate licence.

Original work © 2020–2025 LoohpJames and contributors.
