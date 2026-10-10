# Spuds Shop Reforged

Decorative player shops for **Minecraft 1.21.1 • NeoForge**, with fixes for item duplication, trade accounting, and inventory interactions.

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-E87527)
![MIT license](https://img.shields.io/badge/License-MIT-blue)

Spuds Shop Reforged is a community fork of **[Spud's Shops](https://modrinth.com/mod/spuds-shops)** by **Spudacious5705**, maintained upstream at [Milo-Cat/spuds-shops](https://github.com/Milo-Cat/spuds-shops). This codebase comes through [lucab's fork](https://modrinth.com/mod/spuds-shop-fork) and [Ede1vveiss's fork](https://github.com/Ede1vveiss/spuds-shops-forked-squared). The original shop designs and gameplay belong to those projects; Reforged focuses on fixing and maintaining this fork.

## What you can do

- Build decorative shops using crates, shelves, hooks, rugs, and window sills.
- Configure a fixed payment and product quantity for each shop inventory.
- Keep stock and collected payments in the shop.
- Assign staff permissions through signed contracts.
- Configure creative shops and visual effects.

Trade configuration uses **ghost items**: setting a price or product does not consume the item in your cursor. Actual purchases consume real payment and stock.

## Changes in Reforged

| Area | Changes |
| --- | --- |
| Duplication prevention | Excludes trade templates from inventory sorting, blocks ghost-item hotbar swaps across tabs, and blocks contract-scroll hotbar swaps. |
| InvTweaks Refoxed | Registers its supported blacklist for shop screens; shop-container sorting is intentionally disabled. |
| Trade accounting | Extracts exact quantities across multiple stacks, including items with stack limits of 16 or 1. Adds item-conservation regression checks. |
| Trade validation | Executes trades on the server and checks the player, dimension, distance, and current shop block entity before trading. |
| Shelf inventories | Uses the selected shelf inventory when opening the customer menu. |
| Shift-click | Restricts bulk purchases to the product slot, includes the final hotbar slot when withdrawing stock, and completes transfer callbacks. |
| Inventory display | Refreshes server slot state after owner-menu transfers to address stale item displays during rapid transfers. |
| Contracts | Avoids a server-side exception when clicking your own contract to open the removal warning. |
| Branding | Uses Spuds Shop Reforged in mod metadata, creative tabs, resource-pack metadata, build output, and the config filename. |

**Validation:** the build and trade-extraction regression checks pass. Mouse Tweaks visual behavior and combined mod interactions still need in-game verification. These fixes are not a claim that every possible exploit has been eliminated.

Selectable trades and an ignore-item-data mode are not implemented in this fork. Shops use fixed item-and-quantity trades with exact item-component matching.

## Installation

1. Install **Minecraft 1.21.1** with **NeoForge** (the current build targets NeoForge **21.1.65**).
2. Obtain the Reforged JAR from the maintainer, or build it using the instructions below.
3. Put it in the `mods` folder on the client and server.
4. Remove other Spud's Shops JARs before starting the game: they share the same internal mod ID.

The internal ID remains `spudaciousshops` to preserve existing item and block IDs. Back up an existing world before changing mod versions; compatibility with every upstream version has not been verified.

## Configuration

The config lives at:

```text
config/spuds_shop_reforged.toml
```

If `config/spudaciousshops.toml` exists and the new file does not, Reforged copies the old settings automatically. It leaves the old file intact and never overwrites an existing Reforged config.

The current file setting is:

```toml
ToggleEffectsDefault = true
```

Other shop options are configured through the shop GUI.

## Build from source

Requires **JDK 21**. The Gradle wrapper downloads build dependencies on the first run.

```bash
git clone https://github.com/oNxZero/spuds-shop-reforged.git
cd spuds-shop-reforged
bash gradlew build
```

On Windows, use `gradlew.bat build`.

Output:

```text
build/libs/Spuds Shop Reforged-1.9.11.jar
```

`build` runs the trade-extraction regressions. To run them separately:

```bash
bash gradlew tradeRegression
```

## Report a problem

Open an [issue](https://github.com/oNxZero/spuds-shop-reforged/issues) with your Minecraft and NeoForge versions, other relevant mods, reproduction steps, and logs. For inventory bugs, include the items, quantities, shop type, and click or sorting action involved.

## Credits and license

- **Spudacious5705 / Milo-Cat** — original Spud's Shops project.
- **lucab** — the intermediate fork.
- **Ede1vveiss** — the fork this codebase builds on.
- **oNxZero** — Spuds Shop Reforged maintenance.

Licensed under the **[MIT License](LICENSE.txt)**. Original copyright notices are preserved.
