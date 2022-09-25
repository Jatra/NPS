package uk.co.jatra.nps.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align.LEFT
import android.graphics.Paint.Style.FILL
import android.graphics.Paint.Style.STROKE
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.co.jatra.nps.R


//TODO make these an attribute. Or at least use the given UI spec.
private const val TRACK_END_RADIUS = 10f
private const val THUMB_CORNER_RADIUS = 10f

private val TAG = NpsSlider::class.java.simpleName
class NpsSlider : View {

    private var _trackColor: Int = Color.BLACK // TODO: use a default from R.color...
    private var _thumbColor: Int = Color.GREEN // TODO: use a default from R.color...
    private var _thumbTextColor: Int = Color.WHITE // TODO: use a default from R.color...
    private var _fontSize: Float = 12f // TODO: use a default from R.dimen...

    private lateinit var trackTextPaint: TextPaint
    private lateinit var thumbTextPaint: TextPaint
    private lateinit var trackPaint: Paint
    private lateinit var thumbPaint: Paint
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f

    private var paddingLeftF = 0f
    private var paddingTopF = 0f
    private var paddingRightF = 0f
    private var paddingBottomF = 0f

    private var contentWidth = 0f
    private var contentHeight = 0f
    private var trackBottom = 0f
    private var boxWidth = 0f

    private var textOffsetX = 0f
    private var textOffsetY = 0f

    private var nps: Int? = null
    private var lastMotionEvent: MotionEvent? = null

    private val _value: MutableStateFlow<Int?> = MutableStateFlow(nps)
    public val value: StateFlow<Int?> = _value

    var trackColor: Int
        get() = _trackColor
        set(value) {
            _trackColor = value
            invalidateTextPaintAndMeasurements()
        }

    var thumbTextColor: Int
        get() = _thumbTextColor
        set(value) {
            _thumbTextColor = value
            invalidateTextPaintAndMeasurements()
        }

    var thumbColor: Int
        get() = _thumbColor
        set(value) {
            _thumbColor = value
            invalidateTextPaintAndMeasurements()
        }

    var fontSize: Float
        get() = _fontSize
        set(value) {
            _fontSize = value
            invalidateTextPaintAndMeasurements()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.NpsSlider, defStyle, 0)

        _trackColor = a.getColor(
            R.styleable.NpsSlider_npsTrackColor,
            trackColor)
        _thumbColor = a.getColor(
            R.styleable.NpsSlider_npsThumbColor,
            thumbColor)
        _thumbTextColor = a.getColor(
            R.styleable.NpsSlider_npsThumbTextColor,
            thumbTextColor)

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        _fontSize = a.getDimension(
            R.styleable.NpsSlider_size,
            fontSize)

        a.recycle()

        trackTextPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = LEFT
        }

        thumbTextPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = LEFT
        }

        trackPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            style = STROKE
        }

        thumbPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            style = FILL
        }

        invalidateTextPaintAndMeasurements()

    }

    private fun invalidateTextPaintAndMeasurements() {
        trackTextPaint.run {
            textSize = fontSize
            color = trackColor
            textWidth = measureText("10") //fixme
            textHeight = fontMetrics.bottom - fontMetrics.top
        }
        thumbTextPaint.run {
            textSize = fontSize
            color = thumbTextColor
        }
        thumbPaint.run {
            color = thumbColor
        }

        minimumWidth = (textWidth * 11).toInt() + paddingRight + paddingLeft
        minimumHeight = (textHeight * 1.5).toInt() + paddingTop + paddingBottom

        paddingLeftF = paddingLeft.toFloat()
        paddingTopF = paddingTop.toFloat()
        paddingRightF = paddingRight.toFloat()
        paddingBottomF = paddingBottom.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val resolvedWidth = resolveSizeAndState(minimumWidth, widthMeasureSpec, 0)
        val resolvedHeight = resolveSizeAndState(minimumHeight, heightMeasureSpec, 0)


        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, layoutBottom: Int) {
        contentWidth = (width - paddingLeft - paddingRight).toFloat()
        contentHeight = (height - paddingTop - paddingBottom).toFloat()
        trackBottom = paddingTop + contentHeight
        boxWidth = contentWidth / 11
        textOffsetX = (boxWidth - textWidth) / 2
        textOffsetY = trackBottom - textHeight/3

        super.onLayout(changed, left, top, right, layoutBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //Draw the track
        drawTrack(canvas)

        nps?.let {
            drawThumb(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var newNps = nps
        lastMotionEvent = event
        event?.let {
            newNps = it.toNps()
        }
        if (newNps != nps) {
            nps = newNps
            _value.value = nps
            invalidate()
        }
//        Log.d(TAG, "MotionEvent: NPS: ${event?.toNps()} $event")
        return true
    }

    private fun MotionEvent.toNps() : Int = ((x - paddingLeft) / boxWidth).toInt().coerceIn(0..10)

    private fun drawTrack(canvas: Canvas) {

        canvas.drawRoundRect(paddingLeftF, paddingTopF, paddingLeft+contentWidth,
            trackBottom, TRACK_END_RADIUS, TRACK_END_RADIUS, trackPaint)

        var x = paddingLeftF
        var textX = x + textOffsetX
        val textY = textOffsetY

        //The textX needs to be a bit smarter, since the "0" to "9" are a different width than "10"
        canvas.drawText("0", textX, textY, trackTextPaint)
        for (i in 1..10) {
            x += boxWidth
            textX += boxWidth
            canvas.drawLine(x, paddingTopF, x, trackBottom, trackPaint)
            canvas.drawText(i.toString(), textX, textY, trackTextPaint)
        }
    }

    private fun drawThumb(canvas: Canvas) {
//        Log.d(TAG, "NPS: $nps")
        nps?.let {
            val x = paddingLeftF + (it * boxWidth)
            canvas.drawRoundRect(x-5, paddingTopF-5,  x+boxWidth+5, trackBottom+5, THUMB_CORNER_RADIUS, THUMB_CORNER_RADIUS, thumbPaint)
            canvas.drawText(it.toString(), x+ textOffsetX, textOffsetY, thumbTextPaint)
        }
    }
}
