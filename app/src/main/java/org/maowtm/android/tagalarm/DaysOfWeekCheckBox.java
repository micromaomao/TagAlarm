package org.maowtm.android.tagalarm;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Checkable;
import android.widget.CompoundButton;

public class DaysOfWeekCheckBox extends CompoundButton implements Checkable {
    protected final Paint rectPaint;
    protected final Paint strokePaint;
    protected final Paint textPaint;
    protected final Paint blackTextPaint;

    public DaysOfWeekCheckBox(Context context) {
        this(context, null);
    }

    public DaysOfWeekCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DaysOfWeekCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    protected final float SIZE_STROKE;
    protected final int SIZE_DESIRED;
    protected final int SIZE_TEXT;

    protected float swAngle = 0;
    protected float stAngle = -90;

    protected int dowTarget;
    protected String dowText;
    public int getDowTarget() {
        return dowTarget;
    }
    public void setDowTarget(int dowTarget) {
        if (dowTarget < 0 || dowTarget > 6) {
            throw new IllegalArgumentException("dowTarget out of range.");
        }
        this.dowTarget = dowTarget;
        this.dowText = this.getResources().getStringArray(R.array.dow_checkbox)[this.dowTarget];
        invalidate();
    }

    protected ValueAnimator animator;
    public DaysOfWeekCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.SIZE_STROKE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        this.SIZE_DESIRED = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
        this.SIZE_TEXT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());

        this.setDowTarget(0);

        this.rectPaint = new Paint();
        this.rectPaint.setStrokeWidth(0);
        this.rectPaint.setColor(this.getResources().getColor(R.color.colorAccent));
        this.rectPaint.setStyle(Paint.Style.FILL);
        this.strokePaint = new Paint(rectPaint);
        this.strokePaint.setStrokeWidth(this.SIZE_STROKE);
        this.strokePaint.setColor(this.getResources().getColor(R.color.colorAccentDark) ^ (126 << 24));
        this.strokePaint.setStyle(Paint.Style.STROKE);
        this.textPaint = new Paint();
        this.textPaint.setColor(Color.WHITE);
        this.textPaint.setTextSize(SIZE_TEXT);
        this.textPaint.setTypeface(Typeface.SANS_SERIF);
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        this.blackTextPaint = new Paint(this.textPaint);
        this.blackTextPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        this.blackTextPaint.setARGB(160, 0, 0, 0);
        // Xfermode don't seems to work with hardware.
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        this.animator = new ValueAnimator();
        this.animator.setInterpolator(new AccelerateDecelerateInterpolator());
        this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                DaysOfWeekCheckBox th = DaysOfWeekCheckBox.this;
                th.swAngle = (float) animation.getAnimatedValue();
                if (th.swAngle > 360) {
                    th.swAngle = (360 * 2) - th.swAngle;
                    th.stAngle = -90 - th.swAngle + 360;
                } else {
                    th.stAngle = -90;
                }
                th.invalidate();
            }
        });
        this.animator.setDuration(300);

        this.setClickable(true);
    }

    protected boolean checked = false;

    @Override
    public void setChecked(boolean checked) {
        this.setChecked(checked, false);
    }
    public void setChecked(boolean checked, boolean animate) {
        super.setChecked(checked);
        boolean old = this.checked;
        this.checked = checked;
        if (this.mOnCheckedChangeListener != null) {
            this.mOnCheckedChangeListener.onCheckedChanged(this, checked);
        }
        if (animate && this.animator != null) {
            if (this.animator.isRunning()) {
                this.animator.end();
            }
            this.animator.setFloatValues(this.checked ? (old ? 360 : 0) : 360, this.checked ? 360 : (old ? 720 : 0));
            this.animator.start();
        } else {
            if (old == this.checked)
                return;
            if (this.animator != null && this.animator.isRunning()) {
                this.animator.end();
            }
            this.swAngle = this.checked ? 360 : 0;
            this.stAngle = -90;
        }
        this.invalidate();
    }
    @Override
    public boolean isChecked() {
        return this.checked;
    }
    @Override
    public void toggle() {
        this.setChecked(!this.isChecked(), true);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int widthMode = MeasureSpec.getMode(widthSpec);
        int heightMode = MeasureSpec.getMode(heightSpec);
        int widthSize = MeasureSpec.getSize(widthSpec);
        int heightSize = MeasureSpec.getSize(heightSpec);

        int width, height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = SIZE_DESIRED;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = SIZE_DESIRED;
        }

        this.setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        float radius = Math.min(width / 2 - SIZE_STROKE * 2, height / 2 - SIZE_STROKE * 2);
        float radius2 = Math.min(width / 2 - SIZE_STROKE, height / 2 - SIZE_STROKE);

        canvas.drawArc((width - radius*2) / 2, (height - radius*2) / 2, radius2 * 2, radius2 * 2, stAngle, swAngle, true, rectPaint);
        canvas.drawText(this.dowText, width / 2, height * (33f / 48f), textPaint);
        canvas.drawText(this.dowText, width / 2, height * (33f / 48f), blackTextPaint);
        canvas.drawCircle(width / 2, height / 2, radius, strokePaint);
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;
    @Override
    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }
}
