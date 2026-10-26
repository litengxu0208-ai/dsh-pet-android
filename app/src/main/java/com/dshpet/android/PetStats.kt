package com.dshpet.android

import android.content.Context
import android.content.SharedPreferences

/**
 * 好感度 / 饱食度 / 小鱼干库存。
 *
 * 纯本地 SharedPreferences，独立文件（`dsh_pet_stats`），不碰原来的 `dsh_pet`，
 * 所以老用户升级不会丢原来的尺寸/开关设置。
 *
 * 数值设计参照 DSH 网页版 dsh-pet：九级羁绊、摸头 +1（10 秒冷却）、
 * 喂食 +5（30 秒冷却）、小鱼干上限 20、每 30 分钟回 1 条、饱食度每 10 分钟掉 1 点。
 */
object PetStats {
    private const val FILE = "dsh_pet_stats"
    private const val K_AFFINITY = "affinity"
    private const val K_SATIETY = "satiety"
    private const val K_FISH = "fish"
    private const val K_FEED_COUNT = "feed_count"
    private const val K_LAST_PAT = "last_pat_ms"
    private const val K_LAST_FEED = "last_feed_ms"
    private const val K_LAST_FISH_REGEN = "last_fish_regen_ms"
    private const val K_LAST_SATIETY_LOSS = "last_satiety_loss_ms"

    const val MAX_AFFINITY = 999_999_999
    const val MAX_FISH = 20
    const val MAX_SATIETY = 100
    const val START_FISH = 3
    const val START_SATIETY = 80

    const val PAT_GAIN = 1
    const val FEED_GAIN = 5
    const val FEED_SATIETY = 25

    const val PAT_COOLDOWN_MS = 10_000L
    const val FEED_COOLDOWN_MS = 30_000L
    const val FISH_REGEN_MS = 30 * 60_000L
    const val SATIETY_LOSS_MS = 10 * 60_000L
    const val HUNGRY_THRESHOLD = 30

    /** 九级羁绊：值 = 升到该级所需的累计好感 */
    val LEVELS = listOf(
        "幼鲸" to 0,
        "伙伴" to 30,
        "挚友" to 120,
        "深海羁绊" to 400,
        "心有灵犀" to 1_200,
        "传说羁绊" to 3_000,
        "神话羁绊" to 8_000,
        "永恒之契" to 20_000,
        "鲸生共渡" to 50_000,
    )

    enum class FeedResult { OK, NO_FISH, COOLDOWN }

    data class Snapshot(
        val affinity: Int,
        val levelIndex: Int,
        val satiety: Int,
        val fish: Int,
        val feedCount: Int,
    ) {
        val levelName: String get() = LEVELS.getOrElse(levelIndex) { LEVELS.last() }.first
        val title: String get() = "Lv.${levelIndex + 1} $levelName"
        val hungry: Boolean get() = satiety <= HUNGRY_THRESHOLD
        /** 菜单标题用的一行摘要 */
        val menuLine: String get() = "$title · 好感 $affinity · 🐟 $fish · 饱食 $satiety%"
        /** 设置页用的一行摘要 */
        val setupLine: String
            get() = "$title · 好感 $affinity · 小鱼干 $fish/$MAX_FISH · 饱食 $satiety% · 已喂 $feedCount 次"
    }

    data class TapOutcome(val gained: Int, val levelUp: Boolean, val snap: Snapshot)
    data class FeedOutcome(val result: FeedResult, val levelUp: Boolean, val snap: Snapshot)

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ------------------------------------------------------------ 读取

    fun affinity(ctx: Context): Int =
        sp(ctx).getInt(K_AFFINITY, 0).coerceIn(0, MAX_AFFINITY)

    fun satiety(ctx: Context): Int =
        sp(ctx).getInt(K_SATIETY, START_SATIETY).coerceIn(0, MAX_SATIETY)

    fun fish(ctx: Context): Int =
        sp(ctx).getInt(K_FISH, START_FISH).coerceIn(0, MAX_FISH)

    fun feedCount(ctx: Context): Int = sp(ctx).getInt(K_FEED_COUNT, 0)

    fun levelIndex(ctx: Context): Int = levelIndexOf(affinity(ctx))

    private fun levelIndexOf(v: Int): Int {
        var idx = 0
        for ((i, pair) in LEVELS.withIndex()) {
            if (v >= pair.second) idx = i else break
        }
        return idx
    }

    fun snapshot(ctx: Context): Snapshot = Snapshot(
        affinity = affinity(ctx),
        levelIndex = levelIndex(ctx),
        satiety = satiety(ctx),
        fish = fish(ctx),
        feedCount = feedCount(ctx),
    )

    /** 距离下一级还差多少好感；已满级返回 0 */
    fun toNextLevel(ctx: Context): Int {
        val v = affinity(ctx)
        val idx = levelIndexOf(v)
        if (idx >= LEVELS.size - 1) return 0
        return (LEVELS[idx + 1].second - v).coerceAtLeast(0)
    }

    /** 喂食剩余冷却（秒），0 表示可以喂 */
    fun feedCooldownLeftSec(ctx: Context): Long {
        val last = sp(ctx).getLong(K_LAST_FEED, 0L)
        if (last <= 0L) return 0
        val left = FEED_COOLDOWN_MS - (System.currentTimeMillis() - last)
        return if (left <= 0L) 0 else (left / 1000L) + 1L
    }

    // ------------------------------------------------------------ 交互

    /** 摸头：+1 好感，10 秒冷却。gained=0 表示这次没加（冷却中）。 */
    fun pat(ctx: Context): TapOutcome {
        val p = sp(ctx)
        val now = System.currentTimeMillis()
        val lastPat = p.getLong(K_LAST_PAT, 0L)
        if (lastPat > 0L && now - lastPat < PAT_COOLDOWN_MS) {
            return TapOutcome(0, false, snapshot(ctx))
        }
        val before = levelIndexOf(affinity(ctx))
        val next = (affinity(ctx) + PAT_GAIN).coerceAtMost(MAX_AFFINITY)
        p.edit()
            .putInt(K_AFFINITY, next)
            .putLong(K_LAST_PAT, now)
            .apply()
        val after = levelIndexOf(next)
        return TapOutcome(PAT_GAIN, after > before, snapshot(ctx))
    }

    /** 喂食：消耗 1 条小鱼干，+5 好感、饱食度 +25，30 秒冷却 */
    fun feed(ctx: Context): FeedOutcome {
        val p = sp(ctx)
        val now = System.currentTimeMillis()
        val lastFeed = p.getLong(K_LAST_FEED, 0L)
        if (lastFeed > 0L && now - lastFeed < FEED_COOLDOWN_MS) {
            return FeedOutcome(FeedResult.COOLDOWN, false, snapshot(ctx))
        }
        val stock = fish(ctx)
        if (stock <= 0) {
            return FeedOutcome(FeedResult.NO_FISH, false, snapshot(ctx))
        }
        val before = levelIndexOf(affinity(ctx))
        val nextAffinity = (affinity(ctx) + FEED_GAIN).coerceAtMost(MAX_AFFINITY)
        val nextSatiety = (satiety(ctx) + FEED_SATIETY).coerceAtMost(MAX_SATIETY)
        p.edit()
            .putInt(K_FISH, stock - 1)
            .putInt(K_AFFINITY, nextAffinity)
            .putInt(K_SATIETY, nextSatiety)
            .putInt(K_FEED_COUNT, feedCount(ctx) + 1)
            .putLong(K_LAST_FEED, now)
            .apply()
        val after = levelIndexOf(nextAffinity)
        return FeedOutcome(FeedResult.OK, after > before, snapshot(ctx))
    }

    /**
     * 时间推进：小鱼干每 30 分钟回 1 条（封顶 20），饱食度每 10 分钟掉 1 点（见底 0）。
     * 由服务每秒的 tick 调用，开销只有几次 getLong。
     */
    fun tick(ctx: Context) {
        try {
            val p = sp(ctx)
            val now = System.currentTimeMillis()
            var edit: SharedPreferences.Editor? = null

            // 小鱼干恢复
            var stock = fish(ctx)
            var lastRegen = p.getLong(K_LAST_FISH_REGEN, 0L)
            if (lastRegen <= 0L) {
                lastRegen = now
                edit = (edit ?: p.edit()).putLong(K_LAST_FISH_REGEN, now)
            }
            var gained = 0
            while (stock < MAX_FISH && now - lastRegen >= FISH_REGEN_MS) {
                stock++
                gained++
                lastRegen += FISH_REGEN_MS
            }
            if (gained > 0) {
                edit = (edit ?: p.edit())
                    .putInt(K_FISH, stock)
                    .putLong(K_LAST_FISH_REGEN, lastRegen)
            }

            // 饱食度衰减
            var sat = satiety(ctx)
            var lastLoss = p.getLong(K_LAST_SATIETY_LOSS, 0L)
            if (lastLoss <= 0L) {
                lastLoss = now
                edit = (edit ?: p.edit()).putLong(K_LAST_SATIETY_LOSS, now)
            }
            var lost = 0
            while (sat > 0 && now - lastLoss >= SATIETY_LOSS_MS) {
                sat--
                lost++
                lastLoss += SATIETY_LOSS_MS
            }
            if (lost > 0) {
                edit = (edit ?: p.edit())
                    .putInt(K_SATIETY, sat)
                    .putLong(K_LAST_SATIETY_LOSS, lastLoss)
            }

            edit?.apply()
        } catch (t: Throwable) {
            Diag.record(ctx, "PetStats.tick 失败", t)
        }
    }

    /** 调试/补偿用：直接发小鱼干 */
    fun grantFish(ctx: Context, n: Int) {
        val p = sp(ctx)
        p.edit().putInt(K_FISH, (fish(ctx) + n).coerceIn(0, MAX_FISH)).apply()
    }
}
