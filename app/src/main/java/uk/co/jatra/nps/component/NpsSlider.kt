package uk.co.jatra.nps.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align.*
import android.graphics.Paint.Style.FILL
import android.graphics.Paint.Style.STROKE
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.co.jatra.nps.R

data class NpsEvent(val nps: Int, val changing: Boolean)

//TODO make these an attribute. Or at least use the given UI spec.
private const val TRACK_END_RADIUS = 10f
private const val THUMB_CORNER_RADIUS = 12f

//private val TAG = NpsSlider::class.java.simpleName

class NpsSlider : View {

    private var _trackColor: Int = Color.BLACK // TODO: use a default from R.color...
    private var _thumbColor: Int = Color.GREEN // TODO: use a default from R.color...
    private var _thumbTextColor: Int = Color.WHITE // TODO: use a default from R.color...
    private var _labelTextColor: Int = Color.BLACK // TODO: use a default from R.color...
    private var _fontSize: Float = 12f // TODO: use a default from R.dimen...
    private var _labelFontSize: Float = 10f // TODO: use a default from R.dimen...
    private var _minLabel: String = "Never" // TODO: use a default from R.string...
    private var _maxLabel: String = "Definitely" // TODO: use a default from R.string...

    private lateinit var trackTextPaint: TextPaint
    private lateinit var thumbTextPaint: TextPaint
    private lateinit var labelTextPaint: TextPaint
    private lateinit var trackPaint: Paint
    private lateinit var thumbPaint: Paint
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f
    private var textBaseLine = 0f
    private var labelHeight: Float = 0f
    private var labelBaseLine = 0f

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

    private var lastNpsEvent: NpsEvent? = null

    private val _value: MutableStateFlow<NpsEvent?> = MutableStateFlow(null)
    val value: StateFlow<NpsEvent?> = _value
    val nps: Int?
        get() = lastNpsEvent?.nps

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

    var labelFontSize: Float
        get() = _labelFontSize
        set(value) {
            _labelFontSize = value
            invalidateTextPaintAndMeasurements()
        }

    var labelTextColor: Int
        get() = _labelTextColor
        set(value) {
            _labelTextColor = value
            invalidateTextPaintAndMeasurements()
        }

    var minLabel: String
        get() = _minLabel
        set(value) {
            _minLabel = value
            invalidateTextPaintAndMeasurements()
        }

    var maxLabel: String
        get() = _maxLabel
        set(value) {
            _maxLabel = value
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
        _labelTextColor = a.getColor(
            R.styleable.NpsSlider_npsLabelTextColor,
            thumbTextColor)

        _minLabel = a.getString(
            R.styleable.NpsSlider_npsMinLabel,
        ) ?: _minLabel
        _maxLabel = a.getString(
            R.styleable.NpsSlider_npsMaxLabel,
        ) ?: _maxLabel
        _labelFontSize = a.getDimension(
            R.styleable.NpsSlider_npsLabelFontSize,
            labelFontSize)

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        _fontSize = a.getDimension(
            R.styleable.NpsSlider_npsTrackFontSize,
            fontSize)

        a.recycle()

        trackTextPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = CENTER
        }

        thumbTextPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = CENTER
        }

        labelTextPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = CENTER
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val resolvedWidth = resolveSizeAndState(minimumWidth, widthMeasureSpec, 0)
        val resolvedHeight = resolveSizeAndState(minimumHeight, heightMeasureSpec, 0)


        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, layoutBottom: Int) {
        contentWidth = (width - paddingLeft - paddingRight).toFloat()
        contentHeight = (height - paddingTop - paddingBottom - labelHeight)
        trackBottom = paddingTop + contentHeight
        boxWidth = contentWidth / 11
        textOffsetX = boxWidth / 2
        textOffsetY = paddingTop + contentHeight/2 + textBaseLine

        super.onLayout(changed, left, top, right, layoutBottom)
    }

    override fun onDraw(canvas: Canvas) {
        //FIXME use background color
        canvas.drawARGB(255, 255, 255, 255)
        //Draw the track
        drawTrack(canvas)
        drawLabels(canvas)
        nps?.let {
            drawThumb(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
//            Log.d(TAG, "MotionEvent $nps ${MotionEvent.actionToString(it.action)}")
            val newNps = it.toNps()
            if (newNps != lastNpsEvent) {
                lastNpsEvent = newNps
                _value.value = newNps
                invalidate()
            }
        }
        return true
    }

    fun clear() {
        lastNpsEvent = null
        _value.value = null
        invalidate()
    }

    private fun invalidateTextPaintAndMeasurements() {
        trackTextPaint.run {
            textSize = fontSize
            color = trackColor
            textWidth = measureText("10") //fixme
            textHeight =  fontMetrics.descent - fontMetrics.ascent
            textBaseLine =   - fontMetrics.ascent / 2  - fontMetrics.descent /2

        }
        thumbTextPaint.run {
            textSize = fontSize
            color = thumbTextColor
        }
        thumbPaint.run {
            color = thumbColor
        }
        labelTextPaint.run {
            textSize = labelFontSize
            color = labelTextColor
            labelHeight =  fontMetrics.descent - fontMetrics.ascent
            labelBaseLine =   - fontMetrics.ascent / 2  - fontMetrics.descent /2
        }

        minimumWidth = (textWidth * 11).toInt() + paddingRight + paddingLeft
        minimumHeight = (textHeight * 2.375).toInt() + paddingTop + paddingBottom + labelHeight.toInt()

        paddingLeftF = paddingLeft.toFloat()
        paddingTopF = paddingTop.toFloat()
        paddingRightF = paddingRight.toFloat()
        paddingBottomF = paddingBottom.toFloat()
    }

    private fun MotionEvent.toNps(): NpsEvent =
        NpsEvent(
            nps = ((x - paddingLeft) / boxWidth).toInt().coerceIn(0..10),
            changing = action != ACTION_UP)

    private fun drawTrack(canvas: Canvas) {

        canvas.drawRoundRect(paddingLeftF, paddingTopF, paddingLeft + contentWidth,
            trackBottom, TRACK_END_RADIUS, TRACK_END_RADIUS, trackPaint)

        var x = paddingLeftF
        var textX = x + textOffsetX

        canvas.drawText("0", textX, textOffsetY, trackTextPaint)
        for (i in 1..10) {
            x += boxWidth
            textX += boxWidth
            canvas.drawLine(x, paddingTopF, x, trackBottom, trackPaint)
            canvas.drawText(i.toString(), textX, textOffsetY, trackTextPaint)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        labelTextPaint.textAlign = LEFT
        canvas.drawText(minLabel, paddingLeftF, trackBottom + labelHeight, labelTextPaint)
        labelTextPaint.textAlign = RIGHT
        canvas.drawText(maxLabel, paddingLeftF+contentWidth, trackBottom+labelHeight, labelTextPaint)
    }

    private fun drawThumb(canvas: Canvas) {
//        Log.d(TAG, "NPS: $nps")
        nps?.let {
            val x = paddingLeftF + (it * boxWidth)
            canvas.drawRoundRect(x-1,
                paddingTopF - 10,
                x + boxWidth + 2,
                trackBottom + 10,
                THUMB_CORNER_RADIUS,
                THUMB_CORNER_RADIUS,
                thumbPaint)
            canvas.drawText(it.toString(), x + textOffsetX, textOffsetY, thumbTextPaint)
        }
    }
}