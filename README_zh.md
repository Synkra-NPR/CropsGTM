[English](README.md) | **中文**

# CropsGTM

CropsGTM 是 [CropNH](https://github.com/GTNewHorizons/CropsNH) 的 **GregTech CE Unofficial Modern（MC 1.20.1）移植版**。

CropNH 是 GTNH（1.7.10）整合包里的作物科技模组。本版将其作物、作物架、杂交、突变与机器移植到 GTM 1.20.1，内容为数据包驱动：作物、突变、土壤、底土、效力等都由 data/ 下的 JSON 定义，整合包或附属数据包可以很方便地创建自己的作物。

## 环境与依赖

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.0 及以上 |
| Java | 17 |
| GregTech CEu Modern | 7.5.3 |

值得一提的是，GregTech **不是硬性依赖**。未安装时也能玩部分内容：作物架、种子、工具与作物系统照常工作，同时启用 FE 作物管理机承担自动化。

## 游戏内容

- **作物与作物架**：作物架只能放在已注册的土壤上；作物有自己的 soil 要求、光照与底土条件，并受水、肥料、除草剂、疾病与杂草影响。种子带生长 / 产量 / 抗性三项属性，属性可通过杂交、编辑器或基因机器提升。
- **杂交**：双作物架相邻摆放可触发蔓延与杂交，产物由 mutations 的显式亲本组合决定；同 mutation_pools 的作物之间还会随机产出池内成员。另有杂交机、种子生成机与图鉴里的突变池分类。
- **GT 小机器**：作物管理机、作物杂交机、种子生成机（LV ~ OpV），作物基因提取机、作物合成机（EV ~ OpV）。
- **GT 工业农场**：长度随苗床等级变化的多方块，从输入总线取种子与底土、从输入仓取水与富集肥料；有输入 / 农场 / 输出三种模式。可装苗床与施肥、生长加速、进阶收获、环境优化、超频生长加速五类升级组件，环境模块还能模拟额外的生物群系标签。
- **工具**：作物架、植物透镜、铲子与强化铲子、除草剂喷雾罐、植物治愈喷雾罐、含毒粉末、四档洒水壶；依赖 GT 的有作物属性修改器、基因数据球，以及 GT 自带的便携式 / 调试扫描仪（对准作物架时等同高级植物透镜）。

## 联动模组

以下模组的作物已内置，按 data/cropsgtm/crops/<目录名>/ 分组：对应模组未安装时整个目录跳过，装了才注册。

| 模组 | 作物数 | 内容 |
| --- | --- | --- |
| GregTech CEu Modern (gtceu) | 66 | 金属、矿石与石材类作物，如 aluminium_ore_berry、basaltLily |
| Pam's HarvestCraft 2 - Crops (pamhc2crops)（已取得材质引用许可） | 98 | 果蔬、谷物与纤维类作物 |
| Pam's HarvestCraft 2 - Trees (pamhc2trees)（已取得材质引用许可） | 48 | 果树与坚果 |
| Biomes O' Plenty (biomesoplenty) | 7 | eyebulb、glowflower、glowshroom 等 |
| Farmer's Delight (farmersdelight) | 4 | tomato、cabbage、onion、rice |
| The Twilight Forest (twilightforest) | 4 | knightmetal_berry、liveroot、steeleafranks、torchberry |
| Legendary Survival Overhaul (legendarysurvivaloverhaul) | 3 | ice_fern、sun_fern、water_plant |
| Ars Nouveau (ars_nouveau) | 1 | magebloom |

注意作物 id 与文件名不一定相同，引用时以定义里的 id 字段为准。例如 Farmer's Delight 的四个作物沿用 cropsgtm_farmersdelight 命名空间（cropsgtm_farmersdelight:tomato），而 basalt_lily.json 的 id 是 cropsgtm:basaltLily。

## 数据包与本地化

数据包制作的完整目录结构、字段表、默认值、覆盖规则与踩坑说明见 **[DATAPACK.md](DATAPACK.md)**。

可定义的内容：

| 目录 | 内容 |
| --- | --- |
| data/<ns>/crops | 作物：等级、生长时长、收获轮数、掉落、替代种子、土壤、生长条件、突变池、渲染造型、贴图 |
| data/<ns>/mutations | 突变：产出作物 + 2~4 个亲本，可标记只能在机器中成立 |
| data/<ns>/soils | 土壤：方块、方块标签或复合土壤 |
| data/<ns>/sub_soils | 底土：方块、标签或 GregTech 材质 |
| data/<ns>/potency | 水、肥料、除草剂的物品 / 流体效力 |
| data/<ns>/seed_shapes | 种子剪影及其 CustomModelData |
| data/<ns>/recipes | crop_breeder、seed_generator、crop_gene_extractor、crop_synthesizer 配方 |

要点：

- 定义写了 required_mods（或 mod）、或放在 crops/<modid>/ 子目录而该模组未安装时，作物连同引用它的突变都不会注册，不会留下空掉落。
- 无效掉落、重复 id、引用不存在的作物或物品只会写日志；机器配方再用 cropsgtm:crop_exists 条件同步禁用。

## 配置

配置文件为 config/cropsgtm-common.toml：

- industrial_farm：周期时长、产出系数、生长速率系数，以及"始终视为安装了超频生长加速组件"开关。
- crop_sticks：作物架产出与生长速率系数。
- fluids：水、肥料、除草剂的效力与消耗系数。
- crops：杂草与疾病开关、按 id 禁用作物、生长 / 产量 / 抗性三项属性上限（1~31）。
- nutrients：营养值公式的全部参数（基础值、浇水、施肥、见天、喜好群系、湿度加成与每级需求）。

## 配方查看器与信息面板

- **EMI**：原生分类——作物图鉴、杂交路径、突变池、替代种子。
- **JEI**：同一套分类的回退实现；检测到 EMI 或 REI 时不再注册，避免重复页面。
- **Jade**：作物架信息面板与工业农场电力面板。

## 开发

本模组由 Synkra-NPR（GitHub）与 Agent 共同开发。

## 鸣谢与许可

- 原作者：InfinityRaider、mitchej123、C0bra5 以及 GTNH 团队
- 许可证：LGPL-3.0（沿用原项目）
- 材质与资产沿用 CropNH 原项目
