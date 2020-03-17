package com.banana.b19.clock

import android.animation.ValueAnimator
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.OvershootInterpolator
import java.util.*
import kotlin.math.min

const val SP_NAME = "test"
const val SP_SCALE = "SCALE"

class ClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val calendar: Calendar = GregorianCalendar.getInstance()
    val paint: Paint = Paint() // кисть для рисования

    var seconds: Float = calendar.get(Calendar.SECOND).toFloat()
    val animator: ValueAnimator = ValueAnimator()

    var bgColor = Color.BLUE

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var scale = 1f
    private var dX = 0f
    private var dY = 0f

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

        initializeGestures()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor) // закрашиваем полностью холст красным
        val cx = (width / 2).toFloat() + dX // x-координата центра
        val cy = (height / 2).toFloat() + dY // y-координата центра
        val r = (min(width / 2, height / 2) - 10) * scale // радиус
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
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    /* метод вызывается когда вьюшка добавляется в приложение */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        load() // загружаем состояние
    }

    fun onPause() {
        save() // когда активность паузится - сохраняем состояние
    }

    /* метод настраивает жесты */
    private fun initializeGestures() {
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                return true // true - чтобы и дальше приходили события
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scale *= detector.scaleFactor // обновляем скейл
                invalidate()
                return true // true - чтобы и дальше приходили события
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                dX -= distanceX
                dY -= distanceY
                invalidate()
                return true // true - чтобы и дальше приходили события
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                bgColor = Color.rgb(e.x.toInt(), e.y.toInt(), 0)
                invalidate()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                dX = e.x - width / 2
                dY = e.y - height / 2
                invalidate()
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return super.onDoubleTap(e)
            }
        })
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
        val cx = (width / 2).toFloat() + dX // x-координата центра
        val cy = (height / 2).toFloat() + dY // y-координата центра
        val r = (min(width / 2, height / 2) - 10) * scale // радиус
        val l = r * handType.length

        // рисуем стрелку
        canvas.save()
        canvas.rotate(angle, cx, cy) // поворачиваем холст
        paint.color = handType.color
        canvas.drawLine(cx, cy, cx, cy - l, paint) // рисуем линию
        canvas.restore()
    }

    /* метод сохраняет состояние часов */
    private fun save() {
        // достаём файлик с сохранёнками (в SP_NAME - название файлика)
        val sp = context.getSharedPreferences(SP_NAME, MODE_PRIVATE)
        sp.edit() // открываем файлик
            .putFloat(SP_SCALE, scale) // кладём туда scale в ящик с названием SP_SCALE
            // TODO сохранять dX и dY ещё
            .apply() // сохраняем изменения
    }

    /* метод загружает состояние часов */
    private fun load() {
        // достаём файлик с сохранёнками (в SP_NAME - название файлика)
        val sp = context.getSharedPreferences(SP_NAME, MODE_PRIVATE)
        scale = sp.getFloat(SP_SCALE, 1f) // загружаем скейл
        // TODO загружать dX и dY ещё
        invalidate() // запрашиваем перерисовку
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