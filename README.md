**English** | [中文](README_zh.md)

# CropsGTM

CropsGTM is a **GregTech CE Unofficial Modern (MC 1.20.1) port** of [CropNH](https://github.com/GTNewHorizons/CropsNH).

CropNH is the crop-tech mod in the GTNH (1.7.10) modpack. This version ports its crops, crop sticks, crossbreeding, mutations and machines to GTM 1.20.1, with the content being data-pack driven: crops, mutations, soils, sub-soils, potency, etc. are all defined by JSON under `data/`, so modpacks or add-on data packs can create their own crops very easily.

## Environment and dependencies

| Item | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.0 or later |
| Java | 17 |
| GregTech CEu Modern | 7.5.3 |

It is worth mentioning that GregTech is **not a hard dependency**. When it is not installed, part of the content is still playable: crop sticks, seeds, tools and the crop system work as usual, and the FE crop manager is enabled to handle automation.

## Content

- **Crops and crop sticks**: crop sticks can only be placed on registered soils; crops have their own `soil` requirement, light and sub-soil conditions, and are affected by water, fertilizer, weed-ex, disease and weeds. Seeds carry three stats — growth / gain / resistance — and the stats can be improved through crossbreeding, the editor or the gene machines.
- **Crossbreeding**: adjacent double crop sticks can trigger spreading and crossbreeding; the result is determined by the explicit parent combination in `mutations`; crops that share `mutation_pools` can also randomly produce a member of the pool. There are also the crop breeder, the seed generator, and the mutation pool category in the index.
- **GT machines**: crop manager, crop breeder, seed generator (LV ~ OpV), crop gene extractor, crop synthesizer (EV ~ OpV).
- **GT industrial farm**: a multiblock whose length changes with the seed bed tier; it takes seeds and sub-soil from the input bus and water and enriched fertilizer from the input hatch; it has input / farm / output modes. It can be fitted with a seed bed and five upgrade components: fertilizer, growth acceleration, advanced harvesting, environmental enhancement and overclocked growth acceleration; the environmental module can also simulate additional biome tags.
- **Tools**: crop sticks, plant lens, spade and reinforced spade, weed-ex spray can, plant cure spray can, poison powder, four tiers of watering can; the GT-dependent ones are the crop attribute editor, gene data orb, and GT's own portable / debug scanner (equivalent to an advanced plant lens when aimed at crop sticks).

## Mod integration

Crops from the following mods are built in, grouped by `data/cropsgtm/crops/<directory name>/`: when the corresponding mod is not installed the whole directory is skipped, and it is registered only when the mod is installed.

| Mod | Crops | Content |
| --- | --- | --- |
| GregTech CEu Modern (gtceu) | 66 | Metal, ore and stone crops, such as aluminium_ore_berry, basaltLily |
| Pam's HarvestCraft 2 - Crops (pamhc2crops) (texture reference permission obtained) | 98 | Fruit, vegetable, grain and fiber crops |
| Pam's HarvestCraft 2 - Trees (pamhc2trees) (texture reference permission obtained) | 48 | Fruit trees and nuts |
| Biomes O' Plenty (biomesoplenty) | 7 | eyebulb, glowflower, glowshroom, etc. |
| Farmer's Delight (farmersdelight) | 4 | tomato, cabbage, onion, rice |
| The Twilight Forest (twilightforest) | 4 | knightmetal_berry, liveroot, steeleafranks, torchberry |
| Legendary Survival Overhaul (legendarysurvivaloverhaul) | 3 | ice_fern, sun_fern, water_plant |
| Ars Nouveau (ars_nouveau) | 1 | magebloom |

Note that crop ids are not always the same as file names; always use the `id` field in the definition. For example, the four Farmer's Delight crops keep the `cropsgtm_farmersdelight` namespace (`cropsgtm_farmersdelight:tomato`), while the id of `basalt_lily.json` is `cropsgtm:basaltLily`.

## Data packs and localization

For data pack creation, the complete directory structure, field tables, defaults, override rules and pitfall notes are in **[DATAPACK.md](DATAPACK.md)**.

Content that can be defined:

| Directory | Content |
| --- | --- |
| `data/<ns>/crops` | Crops: tier, growth duration, drop rounds, drops, alternate seeds, soil, growth requirements, mutation pools, render shape, sprites |
| `data/<ns>/mutations` | Mutations: output crop + 2~4 parents, optionally marked as machine-only |
| `data/<ns>/soils` | Soils: blocks, block tags or compound soils |
| `data/<ns>/sub_soils` | Sub-soils: blocks, tags or GregTech materials |
| `data/<ns>/potency` | Item / fluid potency for water, fertilizer and weed-ex |
| `data/<ns>/seed_shapes` | Seed silhouettes and their `CustomModelData` |
| `data/<ns>/recipes` | `crop_breeder`, `seed_generator`, `crop_gene_extractor`, `crop_synthesizer` recipes |

Key points:

- When a definition declares `required_mods` (or `mod`), or is placed in a `crops/<modid>/` subdirectory and that mod is not installed, the crop and every mutation referencing it are not registered, so no empty drops are left behind.
- Invalid drops, duplicate ids, and references to nonexistent crops or items only write log entries; machine recipes are then disabled in sync through the `cropsgtm:crop_exists` condition.

## Configuration

The config file is `config/cropsgtm-common.toml`:

- `industrial_farm`: cycle length, output multiplier, growth rate multiplier, and the "always treat the overclocked growth acceleration component as installed" toggle.
- `crop_sticks`: crop stick output and growth rate multipliers.
- `fluids`: potency and consumption multipliers for water, fertilizer and weed-ex.
- `crops`: weed and disease toggles, disabling crops by id, and the caps for the growth / gain / resistance stats (1~31).
- `nutrients`: every parameter of the nutrient formula (base value, watering, fertilizer, sky access, liked biomes, humidity bonus and per-tier requirement).

## Recipe viewers and info panels

- **EMI**: native categories — crop index, breeding paths, mutation pools, alternate seeds.
- **JEI**: a fallback implementation of the same categories; not registered when EMI or REI is detected, to avoid duplicate pages.
- **Jade**: crop stick info panel and industrial farm power panel.

## Development

This mod is developed by Synkra-NPR (GitHub) and Agent.

## Credits and license

- Original authors: InfinityRaider, mitchej123, C0bra5 and the GTNH team
- License: LGPL-3.0 (inherited from the original project)
- Textures and assets come from the original CropNH project
