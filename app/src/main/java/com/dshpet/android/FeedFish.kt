package com.dshpet.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * 屏幕上飘的蓝色小鱼。
 *
 * 纯 Canvas 画出来的，不依赖任何图片资源（上游仓库本来也没带美术素材）。
 * 拖动由 [PetService] 接管，这里只负责「长得像一条鱼」。
 */
class FeedFish(ctx: Context) : View(ctx) {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E8DF7")
        style = Paint.Style.FILL
    }
    private val bellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8FC4FF")
        style = Paint.Style.FILL
    }
    private val finPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F72D8")
        style = Paint.Style.FILL
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B1B2B")
        style = Paint.Style.FILL
    }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        alpha = 200
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val bodyRect = RectF()

    /** 朝左游时为 true（尾巴在右侧） */
    var facingLeft = true

    /** 轻微上下摆动，让"飘"有生命感；0..1 */
    var bob: Float = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / 100f
        val sy = height / 60f
        canvas.save()
        canvas.scale(sx, sy)
        if (!facingLeft) {
            canvas.scale(-1f, 1f, 50f, 30f)
        }
        val lift = bob * 3f
        canvas.translate(0f, lift)

        // 尾巴
        path.reset()
        path.moveTo(28f, 32f)
        path.lineTo(6f, 16f)
        path.lineTo(9f, 32f)
        path.lineTo(6f, 48f)
        path.close()
        canvas.drawPath(path, finPaint)

        // 背鳍
        path.reset()
        path.moveTo(46f, 20f)
        path.lineTo(56f, 6f)
        path.lineTo(64f, 20f)
        path.close()
        canvas.drawPath(path, finPaint)

        // 身体
        bodyRect.set(26f, 17f, 84f, 45f)
        canvas.drawOval(bodyRect, bodyPaint)

        // 肚子（浅色渐变感的第二层）
        bodyRect.set(34f, 31f, 82f, 44f)
        canvas.drawOval(bodyRect, bellyPaint)

        // 眼睛
        canvas.drawCircle(68f, 28f, 3.4f, eyePaint)
        canvas.drawCircle(69.2f, 26.8f, 1.2f, shinePaint)

        // 腮的一笔
        path.reset()
        path.moveTo(60f, 22f)
        path.quadTo(57f, 31f, 60f, 40f)
        canvas.drawPath(path, finPaint)

        canvas.restore()
    }
}
