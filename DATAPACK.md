# CropsGTM 数据包教程

适用于 CropsGTM 2.1.0-alpha / Minecraft 1.20.1 / Forge 47.x。本文只讲数据包侧的写法，字段名与默认值均以 `com.synkra.cropsgtm.farming.cropdata` 下的解析代码为准。

## 1. 目录结构

```
my_crops/
├── pack.mcmeta
├── data/
│   └── mypack/
│       ├── crops/                     作物定义
│       ├── mutations/                 突变定义
│       ├── soils/                     土壤
│       ├── sub_soils/                 底土
│       ├── potency/                   水 / 肥料 / 除草剂效力
│       ├── seed_shapes/               种子剪影
│       ├── recipes/                   GT 配方（crop_breeder 等）
│       └── tags/worldgen/biome/       群系标签（可选）
└── assets/
    └── mypack/
        ├── textures/block/...         植物贴图
        ├── models/item/...            种子模型（仅新剪影需要）
        └── lang/zh_cn.json            翻译
```

`pack.mcmeta`（1.20.1 的 `pack_format` 是 `15`）：

```json
{
  "pack": {
    "description": "我的 CropsGTM 作物",
    "pack_format": 15
  }
}
```

## 2. 加载规则

| 规则 | 说明 |
| --- | --- |
| 定义 id | 没写 `id` 时，id = `<文件的命名空间>:<文件名>`，**不含子目录**。`data/mypack/crops/my_plant.json` → `mypack:my_plant` |
| 显式 id 不带冒号 | 命名空间按 `cropsgtm` 处理，即 `"id": "my_plant"` 等于 `cropsgtm:my_plant` |
| 覆盖内置定义 | 把文件放到**完全相同的路径**，例如 `data/cropsgtm/crops/core/weed.json`，由数据包加载优先级替换。同时 `id` 必须与原来一致 |
| 子目录名即模组门控 | `crops/<目录名>/`、`mutations/<目录名>/`里的目录名被当作 mod id：该模组未加载时整目录跳过；`core` 恒加载；不分子目录恒加载 |
| 缺依赖跳过 | 顶层写了 `required_mods`（或 `mod`）而模组缺失时整份定义跳过，引用它的突变也一起静默跳过 |
| 未知引用 | 引用不存在的物品、作物、土壤分别记日志并按下方规则处理，不会让整个数据包加载失败 |

内置定义的位置：`data/cropsgtm/{crops,mutations,soils,sub_soils,potency,seed_shapes}`，作物与突变按 `core`、`gtceu`、`pamhc2crops` 等模组目录分组，可直接当范例。`core` 是不依赖任何可选模组的那一份。

## 3. 作物定义

路径 `data/<命名空间>/crops/**.json`，子目录的第一段会被当作模组 id（见上一节）。最小可用示例：

```json
{
  "id": "mypack:my_plant",
  "tier": 2,
  "growth_duration": 1200,
  "drop_rounds": 1.5,
  "colors": ["#2e7d32", "#a5d6a7"],
  "soil": "farmland",
  "sprites": [
    "mypack:block/my_plant/1",
    "mypack:block/my_plant/2",
    "mypack:block/my_plant/3",
    "mypack:block/my_plant/4"
  ],
  "drops": [
    { "item": "minecraft:wheat", "count": 2, "chance": 10000 }
  ],
  "alternate_seeds": ["minecraft:wheat_seeds"],
  "liked_biomes": ["forge:is_wet"]
}
```

### 字段表

| 字段 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 按文件名推导 | 作物 id，大小写敏感，用于存档兼容 |
| `lang_key` | string | `cropsgtm_crops.<id 的路径部分>` | 显示名翻译键 |
| `creator` | string | `CropsGTM Team` | 作者字段，目前不显示在任何界面 |
| `flavour_text` | string | 无 | 种子提示中的风味文本，填**翻译键** |
| `tier` | int | `1` | 作物等级：影响所需营养值（每级基础需求可配置）与稀有度颜色（>4 为罕见、>8 为稀有、>12 为史诗） |
| `growth_duration` | int | `-1` | 成熟所需生长点数。**必须填**，默认值下作物永不成熟 |
| `drop_rounds` | float | `-1` | 基础收获轮数（不是概率）。**必须填**，默认值下不掉落 |
| `machine_breeding_tier` | int | `1` | 机器处理所需的电压等级，目前只被作物基因提取机使用（钳制到 EV~UXV） |
| `min_seed_bed_tier` | int | `-1` | 工业农场种植它所需的最低苗床等级，`-1` 无限制 |
| `entity_damage` | float | `-1` | 穿过作物受到的伤害，`<= 0` 无伤害 |
| `crossing_threshold` | float | `0.8` | 蔓延到相邻空作物架所需的生长百分比，负值禁止蔓延 |
| `breeding_threshold` | float | `0.8` | 参与双作物架杂交所需的生长百分比，负值禁止杂交 |
| `starts_spreading_weeds_at` | int | `24` | 成熟后，若种子的**生长属性**高于该值且生长 > 抗性，才会向相邻作物架播种杂草 |
| `hide_from_nei` | bool | `false` | 不在配方查看器的作物分类中显示 |
| `render_shape` | `hash` / `x` / `flower` | `hash` | 渲染造型：四平面十字 / 双十字 / 花朵 |
| `seed_shape` | string | `vanilla` | 种子剪影 id，见第 5 节 |
| `seed_model_data` | int | `0` | 非 0 时覆盖剪影给出的 `CustomModelData` |
| `machine_breeding_only` | bool | `false` | 该作物只能通过杂交机获得 |
| `seed_enchanted` | bool | `false` | 种子是否带附魔光泽 |
| `soil` | string | `farmland` | 土壤 id，见第 7 节；`global` 表示任意土壤 |
| `behavior` | string | `none` | 设为 `weed` 时是杂草 |
| `mutation_pools` | string[] | 空 | 加入哪些突变池。同池作物互相杂交时，即使没有显式突变也能产出池内随机一个成员；池内少于 2 个成员会被移除 |
| `colors` | 两个 `#RRGGBB` | `#FFFFFF` | 种子物品模型两层的染色，见第 5 节 |
| `sprite` | object | 见第 4 节 | 推导贴图路径 |
| `sprites` | string[] | 空 | 显式贴图列表，优先级高于 `sprite` |
| `drops` | array | 空 | 收获掉落 |
| `alternate_seeds` | array | 空 | 可直接种植的普通种子物品 |
| `liked_biomes` | string[] | 空 | 群系标签 id，命中可提供营养值加成 |
| `growth_requirements` | array | 空 | 生长 / 收获条件 |
| `required_mods` / `mod` | string[] / string | 无 | 缺任一模组则跳过整份定义 |

### 掉落

```json
"drops": [
  { "item": "minecraft:wheat", "count": 2, "chance": 10000 },
  { "gt": "dustTiny", "material": "aluminium", "chance": 10000 },
  { "item": "somemod:thing", "count": 1, "mod": "somemod", "chance": 500, "required": false }
]
```

- `chance` 单位是万分之一，`10000` = 每次必出。
- 机制：先按 `drop_rounds`掷出收获轮数，每一轮对每个掉落条目独立掷一次 `chance`；产量属性还会在成功时额外增加数量。
- `count` 默认 1，最少 1。
- `gt` + `material` 走 GregTech 材质系统：`gt` 支持 `dust`、`dustTiny`、`dustSmall`、`gem`、`gemExquisite`、`gemFlawless`、`gemFlawed`、`gemChipped`、`crushed`、`crushedPurified`、`plate`、`ingot`、`nugget`、`rodLong`、`block`，其余会按 TagPrefix 名称再找一次。未安装 GT 时该条目直接丢弃。
- 条目的 `mod` 是**条目级**可选依赖，模组未装就跳过这一条；`required` 只控制解析失败时打 error 还是 debug。
- `alternate_seeds` 支持同样的 `item` / `gt` / `mod` 写法。

### 生长条件

```json
"growth_requirements": [
  { "type": "min_light", "level": 9 },
  { "type": "max_light", "level": 15 },
  { "type": "sub_soil", "id": "aluminium" },
  { "type": "machine_only" }
]
```

- `min_light` / `max_light`：方块光照要求，`level` 默认分别是 `0` / `15`。
- `sub_soil`：要求土壤正下方是指定底土（第 7 节）。**引用的底土必须已定义**，否则该条件永远不成立，作物长不起来。
- `machine_only`：作物架上能种能长，但只能由工业农场收获。
- 未知 `type` 只记一条 error 日志，作物仍会注册。

## 4. 贴图与渲染（资源包）

植物在客户端是 cutout 渲染，贴图取自**方块贴图集**，所以要放在 `assets/<命名空间>/textures/block/...`。

- 直接写 `sprites`，按生长阶段从早到晚排列，可引用任意命名空间（内置 `water_plant` 就复用了 `legendarysurvivaloverhaul` 的方块贴图）。
- 也可以写 `sprite` 让它推导：

```json
"sprite": { "base": "mypack:block/my_plant/", "min": 1, "max": 4 }
```

推导的默认值是：`base = cropsgtm:block/crops/<子目录名>/<id 路径小写>/`，`min = 1`，`max = 4`。默认 **base 的命名空间固定为 `cropsgtm`**，`min`/`max` 之间会逐个拼数字加上.png，所以包作者通常应显式写 `sprites` 或 `sprite.base`。`sprite` 必须是对象，写字符串会让该作物解析失败。

阶段选择规则：

- 贴图 4 张以上：按生长百分比均分，成熟固定用最后一张。
- 正好 3 张：40% 前进度用第 1 张，之后用第 2 张，成熟时切到第 3 张。

## 5. 种子剪影、染色与本地化

剪影定义在 `data/<命名空间>/seed_shapes/*.json`：

```json
{
  "id": "my_shape",
  "model_data": 13,
  "dark_texture": "mypack:item/genericseed/my_shape1",
  "light_texture": "mypack:item/genericseed/my_shape2"
}
```

- `id` 默认取文件名；作物通过 `seed_shape` 引用它。
- `model_data` 会写进种子物品的 `CustomModelData`，游戏据此选择物品模型。
- `dark_texture` / `light_texture` 游戏运行时不读取，只是告诉包作者该形状该用哪两张图层。
- 剪影外观由**资源包**里 `cropsgtm:item/generic_seed` 的物品模型 override 决定，数据包只能改 `model_data` 的数值。

内置可复用的形状（括号内是 `CustomModelData`）：

`oreberry`(1)、`bonsai`(2)、`botania`(3)、`flower`(4)、`grains`(5)、`magic`(6)、`spore`(7)、`vanilla`(8)、`troll`(9)、`farmersdelight`(10)、`pam`(11)、`pamtree`(12)。

`colors` 是种子物品两层的染色：第 1 个色给 `tintIndex 0`（暗层），第 2 个给 `tintIndex 1`（亮层）。写成 `"#RRGGBB"` 字符串；直接写十进制数字会被当成十六进制字符串解析，颜色会错。

创建新剪影，还需要资源包里给 `cropsgtm:item/generic_seed` 增加一条 `custom_model_data` 的 override 指向新模型。只想换名字就复用已有形状。

本地化键：

```json
{
  "cropsgtm_crops.my_plant": "我的作物",
  "mypack_flavour.my_plant": "一种很有个性的作物"
}
```

作物名默认键是 `cropsgtm_crops.<id 路径>`；`flavour_text` 填任意翻译键。

## 6. 杂交与突变池

`data/<命名空间>/mutations/*.json` 定义**确定**的杂交产物：

```json
{
  "output": "cropsgtm:bamboo",
  "parents": ["cropsgtm:bonsaiJungle", "cropsgtm:vine"],
  "machine_only": false
}
```

- `parents` 必须 2~4 个且互不相同，否则该定义作废并记 error。
- `output` 不写时按文件名推导。
- `machine_only` 为真，或产物作物声明了 `machine_breeding_only`，则该杂交只在杂交机里成立。
- 产物作物自身实现了繁殖接口的生长需求（例如 `sub_soil`）会一并成为该突变的繁殖条件。
- 引用不存在 / 被跳过的作物时，该突变被跳过；如果被引用的作物是因为缺模组而跳过的，只记 debug，不报错。

除杂交，还有突变：作物通过 `mutation_pools` 声明自己属于哪些池，同池作物杂交时若没有匹配的杂交，就从池内随机产出一个成员（该成员需要满足当前生长条件，可以是亲本自身）。池内成员少于 2 个时该池会被丢弃。

### 作物架上的判定顺序

作物架每 256 tick 做一次生长更新（初始相位随机）。空的双作物架上：

1. 先有 1/3 概率触发繁殖尝试。
2. 其中 50% 概率走"蔓延"：挑一个达到 `crossing_threshold` 的相邻作物，把同种作物种到空位。
3. 否则走"杂交"：相邻达到 `breeding_threshold` 的作物至少 2 个，先匹配显式突变（亲本必须完全对应），再匹配突变池（任意 2 个亲本同池即可）。
4. 结果还必须能种在当前土壤上、并满足该作物的生长条件，才会真正种下。

## 7. 土壤与底土

土壤 `data/<命名空间>/soils/*.json`：

```json
{ "id": "farmland", "blocks": ["minecraft:farmland"] }
```

```json
{ "id": "sugarcane", "include": ["sand", "dirtGrass"] }
```

```json
{ "id": "my_soil", "tags": ["minecraft:dirt"] }
```

- `blocks` 列方块 id，`tags` 列方块标签，`include` 把其他土壤合成复合土壤（可多层嵌套，被引用的土壤必须先定义）。
- `id` 不写时取**文件路径**（不是文件名），嵌套目录会带进 id。建议总是显式写 `id`。
- 作物架只能放在任意已注册土壤上；作物本身只认自己的 `soil`。
- 作物 `soil` 的解析顺序：`dirt_grass`、`nether_mushroom`、`oil_sands`、`slimy_dirt` 这几个旧写法会映射到驼峰 id；`global` 表示所有土壤；解析不到就记 error 并回退到 `farmland`。

底土 `data/<命名空间>/sub_soils/*.json`：

```json
{ "id": "aluminium", "materials": ["gtceu:aluminium"] }
```

- `blocks`、`tags`、`materials` 三种匹配方式任一命中即通过。
- `materials` 会展开成该 GT 材质的矿石、粗矿、存储方块等变体；未安装 GT 时忽略这一项。
- `id` 不写时与土壤一样取文件路径，建议显式写。
- 底土定义同时是用来给杂交机 / 种子生成机提供催化剂的物品来源。

## 8. 效力（水 / 肥料 / 除草剂）

`data/<命名空间>/potency/*.json`，一个文件一个 `type`：

```json
{
  "type": "fertilizer",
  "items": [
    { "item": "minecraft:bone_meal", "potency": 25, "max_storage": 30 }
  ],
  "fluids": [
    { "fluid": "gtceu:enriched_fertilizer", "potency": 10, "mod": "gtceu" }
  ]
}
```

- `type` 只能是 `fertilizer`、`hydration`、`weed_ex`。**只有 `fertilizer` 接受 `items`**，其余写了会记警告并跳过。
- `potency` 默认 1；`max_storage` 默认 0（手动使用本物品在作物架上达到的肥料上限）；`units` 默认 1，表示每次操作消耗几单位。
- 模组未装则跳过该条。

## 9. 机器配方

本模组注册了四个 GT 配方类型：`cropsgtm:crop_breeder`、`cropsgtm:seed_generator`、`cropsgtm:crop_gene_extractor`、`cropsgtm:crop_synthesizer`。内置配方在 `data/cropsgtm/recipes/<配方类型>/`。

种子用 NBT 谓词匹配，并用 `cropsgtm:crop_exists` 让作物被禁用或缺依赖时配方自动消失：

```json
{
  "conditions": [
    { "type": "cropsgtm:crop_exists", "crop": "cropsgtm:my_plant" },
    { "type": "forge:mod_loaded", "modid": "gtceu" }
  ],
  "type": "cropsgtm:crop_breeder",
  "duration": 400,
  "category": "cropsgtm:crop_breeder",
  "tickInputs": { "eu": [{ "content": 30 }] },
  "inputs": {
    "item": [
      {
        "content": {
          "type": "gtceu:nbt_predicate",
          "item": "cropsgtm:generic_seed",
          "count": 1,
          "nbt": "{CustomModelData:4,crop:\"cropsgtm:my_plant\",ga:1b,gr:1b,re:1b,scan:1b}",
          "predicate": { "type": "equals", "key": "crop", "value": "cropsgtm:my_plant", "inverted": false }
        }
      }
    ]
  },
  "outputs": {
    "item": [
      {
        "content": {
          "type": "gtceu:nbt_predicate",
          "item": "cropsgtm:generic_seed",
          "count": 1,
          "nbt": "{CustomModelData:4,crop:\"cropsgtm:my_plant\",ga:1b,gr:1b,re:1b,scan:1b}",
          "predicate": { "type": "equals", "key": "crop", "value": "cropsgtm:my_plant", "inverted": false }
        },
        "chance": 4000
      }
    ]
  }
}
```

- 种子 NBT：`crop` 是作物 id 字符串，`gr` / `ga` / `re` 是生长 / 产量 / 抗性的字节值，`scan` 是分析标记。
- `CustomModelData` 用种子剪影的 `model_data`（若作物写了 `seed_model_data` 则用它）。
- `cropsgtm:crop_exists` 支持单个 `"crop"`，也支持 `"crops": [...]` 数组，表示全部作物都注册时才加载。
- 配方只在安装 GT 时生效，务必带 `forge:mod_loaded` 条件。

## 10. 检查清单

- `growth_duration`、`drop_rounds` 都填了，否则作物不成熟 / 不掉落。
- 掉落物品真实存在；跨模组掉落加了 `mod`；GT 材质掉落只在装 GT 时存在。
- 贴图在 `assets/<ns>/textures/block/`，且被某个方块模型引用，否则不进方块图集。
- 有 `alternate_seeds` 或有突变产出，否则生存里拿不到种子。
- 覆盖内置作物时路径与 `id` 都一致，别用两个不同文件撞同一个 id。
- 改完先 `/reload` 看日志：`Loaded N crop definitions`、`Loaded N mutation definitions`、`Loaded N soil definitions` 的数字是否变化；有 `Could not load the crop definition ...` 就说明解析失败。
