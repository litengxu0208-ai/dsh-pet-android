# 大肥鱼桌宠 · 好感度与喂食系统改造

基于 [Tony-Chen-Stark/dsh-pet-android](https://github.com/Tony-Chen-Stark/dsh-pet-android) 的改造分支。
数值设计参照 DSH 网页版 [dsh-pet](https://github.com/zhu1090093659/dsh-pet)（Apache-2.0）。

## 加了什么

| 功能 | 说明 |
|---|---|
| 好感度 | 摸头 +1（10 秒冷却）；喂食 +5；九级羁绊，累计到 50000 满级 |
| 九级等级 | 幼鲸 → 伙伴 → 挚友 → 深海羁绊 → 心有灵犀 → 传说羁绊 → 神话羁绊 → 永恒之契 → 鲸生共渡 |
| 小鱼干 | 上限 20，初始 3 条，每 30 分钟自动回 1 条 |
| 喂食 | 长按宠物 → 菜单第一项「🐟 喂食」，消耗 1 条，+5 好感、+25 饱食度，30 秒冷却 |
| 饱食度 | 上限 100，初始 80，每 10 分钟掉 1 点 |
| 饿了会叫 | 饱食度 ≤30 时主动碎碎念，最多每 5 分钟一次 |
| 升级提示 | 无论摸头还是喂食触发升级，都会立刻说一句升级台词 |
| 状态显示 | 设置页状态卡片显示等级、好感、小鱼干、饱食度、距下一级差值；长按菜单顶部显示一行摘要 |
| 台词 | 新增 5 类共 50 句：`feed` `nofish` `cooldown` `hungry` `levelup` |

**没有改动**：动画、物理、贴边、多宠物、保活等原有逻辑一行没动；存档独立在
`dsh_pet_stats` 这个 SharedPreferences 文件里，不会和原来的设置互相覆盖，升级不丢数据。

## 改了哪些文件

```
新增  app/src/main/java/com/dshpet/android/PetStats.kt     全部数值逻辑
新增  .github/workflows/build.yml                          GitHub 云端构建
改    app/src/main/java/com/dshpet/android/PetService.kt   摸头加好感 / 菜单喂食 / 饿与衰减
改    app/src/main/java/com/dshpet/android/MainActivity.kt 设置页状态显示
改    app/src/main/res/layout/activity_main.xml            新增一行状态文本
改    app/src/main/java/com/dshpet/android/Lines.kt        新台词类别与中文标签
改    app/src/main/assets/lines.json                       50 句新台词
```

## 数值想调的话

全在 `PetStats.kt` 顶部的常量里，改完推上去自动重新构建：

```kotlin
const val START_FISH = 3          // 初始小鱼干
const val PAT_GAIN = 1            // 摸头加多少好感
const val FEED_GAIN = 5           // 喂食加多少好感
const val FEED_SATIETY = 25       // 喂食回多少饱食度
const val PAT_COOLDOWN_MS = 10_000L
const val FEED_COOLDOWN_MS = 30_000L
const val FISH_REGEN_MS = 30 * 60_000L   // 多久回一条鱼
const val SATIETY_LOSS_MS = 10 * 60_000L // 多久掉一点饱食
val LEVELS = listOf(...)          // 等级阈值
```

## 注意

如果你以前在 `Android/data/com.dshpet.android/files/lines.json` 里改过台词，
那份文件会覆盖内置的，新加的 5 类台词不会出现（只是不说话，不会崩）。
想用新台词就在设置页点「恢复内置」，或者把这 5 类手动补进你的自定义文件。
