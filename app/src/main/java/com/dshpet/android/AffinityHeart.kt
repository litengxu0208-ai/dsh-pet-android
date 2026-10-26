package com.dshpet.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/**
 * 好感度爱心：点一下大肥鱼，它身边浮出一颗心。
 *
 * - 心形从下往上按「当前等级内的进度」逐渐填满；
 * - 每填满一次（= 升一级），底下就多一颗小爱心；
 * - 纯 Canvas 绘制，不依赖图片资源，也不需要布局文件。
 */
class AffinityHeart(ctx: Context) : View(ctx) {

    private val d = resources.displayMetrics.density

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * d
        color = Color.parseColor("#FF9FC0")
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33FFFFFF")
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF5C93")
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF7BA8")
    }

    private val heartPath = Path()
    private val smallPath = Path()

    /** 目标进度 0..1 */
    var target: Float = 0f

    /** 当前显示进度，由服务每帧向 target 靠拢 */
    var shown: Float = 0f

    /** 已经填满的阶段数（小爱心颗数） */
    var hearts: Int = 0

    /** 升阶时的弹跳，1 → 0 衰减 */
    private var pop = 0f

    fun popOnce() {
        pop = 1f
    }

    /** 推进动画；返回 true 表示还在动，需要继续重绘 */
    fun advance(): Boolean {
        var moving = false
        val diff = target - shown
        if (abs(diff) > 0.002f) {
            shown += diff * 0.16f
            moving = true
        } else if (shown != target) {
            shown = target
            moving = true
        }
        if (pop > 0f) {
            pop = (pop - 0.05f).coerceAtLeast(0f)
            moving = true
        }
        return moving
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val rowH = h * 0.17f
        val heartAreaH = h - rowH
        val size = min(w * 0.42f, heartAreaH * 0.52f) * (1f + pop * 0.12f)
        val cx = w / 2f
        val top = (heartAreaH - size * 0.86f) / 2f

        buildHeart(heartPath, cx, top, size)
        canvas.drawPath(heartPath, trackPaint)

        // 按进度从下往上填充
        canvas.save()
        canvas.clipPath(heartPath)
        val bottomY = top + size * 1.02f
        val fillTop = bottomY - (bottomY - top) * shown.coerceIn(0f, 1f)
        canvas.drawRect(cx - size, fillTop, cx + size, bottomY, fillPaint)
        canvas.restore()

        canvas.drawPath(heartPath, outlinePaint)

        // 底下的小爱心：每填满一个阶段多一颗
        val n = hearts.coerceIn(0, PetStats.LEVELS.size - 1)
        if (n > 0) {
            val ss = min(size * 0.20f, (w * 0.86f) / (n * 1.5f))
            val gap = ss * 0.42f
            val totalW = n * ss + (n - 1) * gap
            var x = cx - totalW / 2f + ss / 2f
            val yTop = h - rowH * 0.92f
            for (i in 0 until n) {
                buildHeart(smallPath, x, yTop, ss)
                canvas.drawPath(smallPath, smallPaint)
                x += ss + gap
            }
        }
    }

    /** 画一颗心：cx 是水平中心，top 是心的上沿，size 近似为宽度 */
    private fun buildHeart(path: Path, cx: Float, top: Float, size: Float) {
        path.reset()
        val bottom = top + size * 0.86f
        path.moveTo(cx, bottom)
        path.cubicTo(
            cx - size * 0.92f, top + size * 0.46f,
            cx - size * 0.58f, top - size * 0.02f,
            cx, top + size * 0.26f,
        )
        path.cubicTo(
            cx + size * 0.58f, top - size * 0.02f,
            cx + size * 0.92f, top + size * 0.46f,
            cx, bottom,
        )
        path.close()
    }
}
