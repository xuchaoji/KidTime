package com.xuchaoji.android.timebank.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.xuchaoji.android.timebank.R;

public class CircleTimerView extends View {

    private static final int COLOR_GREEN = Color.parseColor("#66BB6A");
    private static final int COLOR_YELLOW = Color.parseColor("#FFB300");
    private static final int COLOR_RED = Color.parseColor("#EF5350");
    private static final int COLOR_BG_RING = Color.parseColor("#E0D6C8");
    private static final int COLOR_BG_SHADOW = Color.parseColor("#D7CCC8");
    private static final int COLOR_TEXT = Color.parseColor("#4E342E");
    private static final int COLOR_SUB_TEXT = Color.parseColor("#8D6E63");
    private static final int COLOR_DOT = Color.parseColor("#FF8A65");

    private static final float STROKE_WIDTH_DP = 26f;
    private static final float SHADOW_OFFSET_DP = 4f;
    private static final float START_ANGLE = -90f;
    private static final float FULL_SWEEP = 360f;

    private int maxTime = 30;
    private int currentTime = 30;
    private boolean showSeconds = false;
    private int countdownSeconds = 0;

    private final Paint bgPaint;
    private final Paint shadowPaint;
    private final Paint progressPaint;
    private final Paint textPaint;
    private final Paint subTextPaint;
    private final Paint dotPaint;
    private final RectF ovalRect;
    private final RectF shadowRect;
    private final Rect textBounds;

    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;
        float strokeWidth = STROKE_WIDTH_DP * density;
        float shadowOffset = SHADOW_OFFSET_DP * density;

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(strokeWidth);
        shadowPaint.setColor(COLOR_BG_SHADOW);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(strokeWidth);
        bgPaint.setColor(COLOR_BG_RING);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(COLOR_GREEN);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(80f * density);
        textPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(COLOR_SUB_TEXT);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTextSize(16f * density);
        subTextPaint.setFakeBoldText(true);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        ovalRect = new RectF();
        shadowRect = new RectF();
        textBounds = new Rect();
    }

    public void setMaxTime(int minutes) {
        this.maxTime = Math.max(1, minutes);
        invalidate();
    }

    public void setCurrentTime(int minutes) {
        this.currentTime = Math.max(0, minutes);
        this.showSeconds = false;
        invalidate();
    }

    public void setCountdownSeconds(int seconds) {
        this.showSeconds = true;
        this.countdownSeconds = Math.max(0, Math.min(60, seconds));
        this.currentTime = 0;
        invalidate();
    }

    public void clearCountdown() {
        this.showSeconds = false;
        invalidate();
    }

    public int getMaxTime() {
        return maxTime;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float stroke = progressPaint.getStrokeWidth();
        int padding = (int) (stroke / 2f);
        float shadowOff = SHADOW_OFFSET_DP * getResources().getDisplayMetrics().density;

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) / 2f - padding;

        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        shadowRect.set(cx - radius + shadowOff, cy - radius + shadowOff,
                cx + radius + shadowOff, cy + radius + shadowOff);

        canvas.drawArc(shadowRect, START_ANGLE, FULL_SWEEP, false, shadowPaint);
        canvas.drawArc(ovalRect, START_ANGLE, FULL_SWEEP, false, bgPaint);

        String timeText;
        String unitText;
        float timeRatio;

        if (showSeconds && countdownSeconds > 0) {
            timeText = String.valueOf(countdownSeconds);
            unitText = getContext().getString(R.string.seconds_unit);
            timeRatio = countdownSeconds / 60f;
            textPaint.setTextSize(80f * getResources().getDisplayMetrics().density);
        } else {
            timeText = String.valueOf(currentTime);
            unitText = getContext().getString(R.string.minutes_unit);
            timeRatio = maxTime > 0 ? (float) currentTime / (float) maxTime : 0f;
            textPaint.setTextSize(80f * getResources().getDisplayMetrics().density);
        }

        float ringRatio = showSeconds ? timeRatio :
                (maxTime > 0 ? (float) currentTime / (float) maxTime : 0f);

        if (ringRatio >= 0.5f) {
            progressPaint.setColor(COLOR_GREEN);
        } else if (ringRatio > 0.2f) {
            progressPaint.setColor(COLOR_YELLOW);
        } else {
            progressPaint.setColor(COLOR_RED);
        }

        float sweepAngle = FULL_SWEEP * ringRatio;
        if (sweepAngle > 0f) {
            canvas.drawArc(ovalRect, START_ANGLE, sweepAngle, false, progressPaint);
        }

        if (sweepAngle > 0.5f) {
            float dotAngle = START_ANGLE + sweepAngle;
            float dotRad = (float) Math.toRadians(dotAngle);
            float dotX = cx + radius * (float) Math.cos(dotRad);
            float dotY = cy + radius * (float) Math.sin(dotRad);
            dotPaint.setColor(progressPaint.getColor());
            canvas.drawCircle(dotX, dotY, stroke * 0.5f, dotPaint);
            dotPaint.setColor(Color.WHITE);
            canvas.drawCircle(dotX, dotY, stroke * 0.22f, dotPaint);
        }

        textPaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        float textY = cy - textBounds.exactCenterY();
        canvas.drawText(timeText, cx, textY - 12f, textPaint);

        canvas.drawText(unitText, cx, textY + 44f, subTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
        );
        setMeasuredDimension(size, size);
    }
}
