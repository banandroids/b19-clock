package com.banana.b19.clock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import java.util.*
import kotlin.math.min

class ClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val calendar: Calendar = GregorianCalendar.getInstance()
    val paint: Paint = Paint() // кисть для рисования

    var seconds: Float = calendar.get(Calendar.SECOND).toFloat()
    val animator: ValueAnimator = ValueAnimator()

    var bgColor = Color.BLUE

    init {
        calendar.timeZone = TimeZone.getTimeZone("GMT+3")

        paint.style = Paint.Style.STROKE // стиль (есть ещё FILL и FILL_AND_STROKE)
        paint.color = Color.WHITE // цвет
        paint.strokeWidth = 4f // ширина кисти для линий
        paint.isAntiAlias = true // сглаживание

        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                post {
                    animateSeconds(calendar.get(Calendar.SECOND))
                    invalidate()
                }
            }
        }
        timer.schedule(timerTask, 1000, 1000)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor) // закрашиваем полностью холст красным
        val cx = (width / 2).toFloat() // x-координата центра
        val cy = (height / 2).toFloat() // y-координата центра
        val r = min(cx, cy) - 10 // радиус
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r, paint) // рисуем контур часов

        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = animator.animatedValue as Float? ?: 0f

        drawHand(canvas, HandType.HOUR, (hour * 30 + minute * .5).toFloat())
        drawHand(canvas, HandType.MINUTE, (minute * 6).toFloat())
        drawHand(canvas, HandType.SECOND, (second * 6).toFloat())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == ACTION_DOWN || event.action == ACTION_MOVE) {
            bgColor = Color.rgb(event.x.toInt(), event.y.toInt(), 0)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun animateSeconds(newSeconds: Int) {
        animator.cancel() // отменяем предыдущую анимацию
        animator.interpolator = OvershootInterpolator(6f) // меняем интерполятор
        animator.duration = 150 // ставим поменьше длительность
        animator.setFloatValues(seconds, newSeconds.toFloat()) // двигаемся от предыдущего значения к новому
        animator.addUpdateListener { invalidate() }
        seconds = newSeconds.toFloat() // сохраняем новое значение секунд
        animator.start() // стартуем анимацию!!
    }

    private fun drawHand(canvas: Canvas, handType: HandType, angle: Float) {
        val cx = (width / 2).toFloat() // x-координата центра
        val cy = (height / 2).toFloat() // y-координата центра
        val r = min(cx, cy) - 10 // радиус
        val l = r * handType.length

        // рисуем стрелку
        canvas.save()
        canvas.rotate(angle, cx, cy) // поворачиваем холст
        paint.color = handType.color
        canvas.drawLine(cx, cy, cx, cy - l, paint) // рисуем линию
        canvas.restore()
    }

    private enum class HandType(
        val length: Float,
        val color: Int
    ) {
        HOUR(.4f, 0xFFFFFFFF.toInt()),
        MINUTE(.6f, 0xFFFFFFFF.toInt()),
        SECOND(.8f, 0xFFFF0000.toInt())
    }

}