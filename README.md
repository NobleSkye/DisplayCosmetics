# Display Cosmetics

A Minecraft plugin that lets players create and display custom block cosmetics on their character.

## Features

- **Custom Cosmetics**: Create unique cosmetics using any block
- **Three Slots**: HEAD, SHOULDER, and BACK positions
- **Easy Management**: Browse, equip, and delete cosmetics through a simple GUI
- **Permission-Based Limits**: Control how many cosmetics players can create
- **ProtocolLib Integration**: Smooth client-side rendering for better performance

## Commands

- `/cc` - Main command (aliases: `/cosmetics`, `/cosmetic`)
  - `/cc create` - Start creating a new cosmetic
  - `/cc inv` - Browse your cosmetics
  - `/cc equip` - Open equipment inventory
  - `/cc delete` - Delete a cosmetic
  - `/cc toggle` - Toggle cosmetic visibility
  - `/cc reload` - Reload configuration (admin)

## Permissions

- `customcosmetics.*` - All permissions (default: op)
- `customcosmetics.create` - Create cosmetics (default: true)
- `customcosmetics.inv` - Browse cosmetics (default: true)
- `customcosmetics.equip` - Equip cosmetics (default: true)
- `customcosmetics.delete` - Delete cosmetics (default: true)
- `customcosmetics.limit.bypass` - Unlimited cosmetics
- `customcosmetics.limit.3` - Create up to 3 cosmetics (default)
- `customcosmetics.limit.5` - Create up to 5 cosmetics

## Requirements

- Minecraft 1.21.8 (Paper/Spigot)
- ProtocolLib (optional, recommended)

## Installation

1. Download the latest version from [Modrinth](https://modrinth.com/plugin/display-cosmetics)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. (Optional) Install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) for best performance
