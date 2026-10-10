# Spuds Shop Reforged

<p align="center">
  <img src="docs/assets/spuds-shop-reforged-logo.png" alt="Spuds Shop Reforged logo" width="420">
</p>

Decorative player shops for **Minecraft 1.21.1 • NeoForge**, with fixes for item duplication, trade accounting, and inventory interactions.

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-E87527)
![MIT license](https://img.shields.io/badge/License-MIT-blue)

Spuds Shop Reforged is a community fork of **[Spud's Shops](https://modrinth.com/mod/spuds-shops)** by **Spudacious5705**, maintained upstream at [Milo-Cat/spuds-shops](https://github.com/Milo-Cat/spuds-shops). This codebase comes through [lucab's fork](https://modrinth.com/mod/spuds-shop-fork) and [Ede1vveiss's fork](https://github.com/Ede1vveiss/spuds-shops-forked-squared). The original shop designs and gameplay belong to those projects; Reforged focuses on fixing and maintaining this fork.

## What you can do

- Build decorative shops using crates, shelves, hanging displays, rugs, and wooden counters. Shelves have a compact hanging price tag on the front edge for each offer (price, then payment item).
- Configure a fixed payment and product quantity for each shop inventory.
- Choose from eleven wood variants for crates, shelves, and normal wooden shops. Existing legacy crates remain compatible.
- Wooden model surfaces use vanilla planks and stripped logs, so resource packs change them naturally.
- Crates have closed trays and timber stands with horizontal grain on rails and crossbeams.
- Adjacent rugs use non-overlapping connection geometry to prevent seam flicker.
- Keep stock and collected payments in the shop.
- See remaining matching stock beneath the product in the buying GUI, updated live. Creative shops show unlimited stock (`∞`).
- Assign staff permissions through signed contracts.
- Configure creative shops and visual effects.
- As a customer, sneak (Shift) and left-click a shop to buy one configured trade without opening its GUI. Aim at the upper or lower shelf to select its offer. Owners and staff keep their normal controls.
- Every successful purchase plays a soft item-pickup sound, with a small green particle burst at the shop. Bulk purchases throttle only particles. Failed purchases give the buyer distinct feedback for empty stock, insufficient currency, and a full register. Purchase sounds are sent directly to the buyer, including while the GUI is open.

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

**Validation:** the build and regression checks pass, including exact extraction, repeated-purchase stock conservation, register capacity, and durability matching. Quick-buy mouse behavior, sound feedback, Mouse Tweaks visual behavior, and combined mod interactions still need in-game verification. These fixes are not a claim that every possible exploit has been eliminated.

Selectable trades and an ignore-item-data mode are not implemented in this fork. Shops use fixed item-and-quantity trades. Trade matching ignores durability damage, while names, enchantments, and other item components must still match. Purchased items retain the durability of the actual stock.

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

Purchase effects can be adjusted independently of the shop’s existing idle effects. Restart the game/server after editing. Sound and particle IDs use Minecraft registry names; particles must be simple types without extra data (such as `happy_villager`, `heart`, or `end_rod`). Invalid IDs fall back to the defaults.

The file settings are:

```toml
ToggleEffectsDefault = true
PurchaseSounds = true
PurchaseSound = "minecraft:entity.item.pickup"
PurchaseVolume = 0.45
PurchasePitch = 0.8
PurchaseParticles = true
PurchaseParticle = "minecraft:happy_villager"
PurchaseParticleCount = 6
OutOfStockSounds = true
OutOfStockSound = "minecraft:block.dispenser.fail"
OutOfStockVolume = 0.35
OutOfStockPitch = 0.9
InsufficientCurrencySounds = true
InsufficientCurrencySound = "minecraft:entity.villager.no"
InsufficientCurrencyVolume = 0.5
InsufficientCurrencyPitch = 1.2
RegisterFullSounds = true
RegisterFullSound = "minecraft:block.dispenser.fail"
RegisterFullVolume = 0.5
RegisterFullPitch = 0.65
```

Volume is clamped to 0–2, pitch to 0.5–2, and particle count to 0–64 per burst. Set particle count to 0 or disable either effect to turn it off. Only particles have a short cooldown during bulk purchases; purchase sounds play for every successful trade. Failure sound settings are independent of purchase sounds. All trade sounds use the buyer’s master sound channel, so GUI feedback is unaffected by distance or the Blocks volume slider.

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

Wood and rug model assets can be regenerated and checked with Python 3:

```bash
python3 tools/generate_shop_models.py
python3 tools/check_shop_models.py
```

The model checks cover all rug connections, crate support penetration, horizontal beam grain, and variant resources. New wood crates are crafted with six matching planks, two matching fences, and a barrel (`PPP / FBF / PPP`). The Oak Crate Shop replaces the Merchant’s Crate using its original ID. There is one crate per wood type; the original three-barrel recipe still works.

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

Wooden Counter Shops come in all eleven wood types and use three matching planks above a chest (`PPP /  C `). The former calcite and andesite IDs now represent oak and spruce counters, preserving existing shops and their inventories. Glass Display Shops name the angled glass-and-cushion display explicitly.
