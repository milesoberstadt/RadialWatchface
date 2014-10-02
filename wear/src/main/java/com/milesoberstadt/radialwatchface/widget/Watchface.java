package com.milesoberstadt.radialwatchface.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.twotoasters.watchface.gears.widget.IWatchface;
import com.twotoasters.watchface.gears.widget.Watch;
import com.milesoberstadt.radialwatchface.R;

import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class Watchface extends FrameLayout implements IWatchface {

    /*@InjectView(R.id.face)              ImageView face;
    @InjectView(R.id.shadow_overlay)    ImageView shadowOverlay;
    @InjectView(R.id.hand_hour)         ImageView handHour;
    @InjectView(R.id.hand_minute)       ImageView handMinute;
    @InjectView(R.id.hand_second)       ImageView handSecond;*/

    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;

    private Boolean bHourAddMinutes = false; //This is for rendering the hour bar, if true it adds the percent from minutes towards the next hour

    private Watch mWatch;

    private boolean mInflated;
    private boolean mActive;

    //Draw stuff
    private Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float strokeWidth = 30.f;
    private int myWidth = -1;
    private int red = 0xFFe51c23;
    private int blue = 0xFF03a9f4;
    private int green = 0xFF8bc34a;
    private float textAngle = 15;
    private float textRadians = 0;

    public Watchface(Context context) {
        super(context);
        init(context, null, 0);
    }

    public Watchface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public Watchface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    @DebugLog
    private void init(Context context, AttributeSet attrs, int defStyle) {
        mWatch = new Watch(this);
        setWillNotDraw(false);

        setupDrawObjects();
    }

    private void setupDrawObjects(){

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(strokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mFontPaint.setColor(0xFFFFFFFF);
        mFontPaint.setTextSize(24);

        textRadians = textAngle * (3.14159/180);
    }


    @Override
    protected void onDraw(Canvas canvas){
        Timber.v("onDraw()");
        super.onDraw(canvas);

        myWidth = getWidth();

        RectF secondsOval = new RectF();
        RectF minutesOval = new RectF();
        RectF hoursOval = new RectF();

        //Define our colored radian zones
        hoursOval.set(strokeWidth, strokeWidth, myWidth - (strokeWidth), myWidth - (strokeWidth));
        minutesOval.set(strokeWidth * (float)2.5, strokeWidth * (float)2.5, myWidth - (strokeWidth * (float)2.5), myWidth - (strokeWidth * (float)2.5));
        secondsOval.set(strokeWidth * (float)4, strokeWidth * (float)4, myWidth - (strokeWidth * (float)4), myWidth - (strokeWidth * (float)4));

        //Define our colored radian lengths
        Path secondsPath = new Path();
        secondsPath.addArc(secondsOval, -90, 6 * seconds);
        Path minutesPath = new Path();
        minutesPath.addArc(minutesOval, -90, 6 * minutes);
        Path hoursPath = new Path();
        float hourSize = 30 * hours;
        //This adds the percentage of the the current hour to our hour radian's size
        if (bHourAddMinutes)
            hourSize += 0.5f * minutes;
        hoursPath.addArc(hoursOval, -90, (int) hourSize);

        //These are our text radian paths, nice and long so our text will display correctly
        Path secondsLabelPath = new Path();
        secondsLabelPath.addArc(secondsOval, -90, 180);
        Path minutesLabelPath = new Path();
        minutesLabelPath.addArc(minutesOval, -90, 90);
        Path hoursLabelPath = new Path();
        hoursLabelPath.addArc(hoursOval, -90, 90);

        //Draw our colored radians after setting the color...
        mArcPaint.setColor(blue); //0xFF109618
        canvas.drawPath(secondsPath,mArcPaint);
        mArcPaint.setColor(green); //0xFF3366cc
        canvas.drawPath(minutesPath,mArcPaint);
        mArcPaint.setColor(red); //0xFFdc3912
        canvas.drawPath(hoursPath, mArcPaint);

        //We want 12:00 to actually display at 12, so let's do that
        String displayHours = String.valueOf(hours);
        if (displayHours.equals("0"))
            displayHours = "12";

        //Text draws differ based on device size...
        if (myWidth>=320){
            //Let's try to calculate the hours position...
            float hoursXOffset = (float) ((hoursOval.width()/2)*Math.cos(0.785)); //45 to radians
            canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, 30, 10, mFontPaint);
            canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, 65, 10, mFontPaint);
            canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 10, mFontPaint);
        }
        else {
            mFontPaint.setTextSize(20);
            canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, 10, 10, mFontPaint);
            canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, 42, 10, mFontPaint);
            canvas.drawTextOnPath(displayHours, hoursLabelPath, 70, 10, mFontPaint);
        }

    }


    @DebugLog
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this, getRootView());
        mInflated = true;
    }

    @DebugLog
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWatch.onAttachedToWindow();
    }

    @DebugLog
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWatch.onDetachedFromWindow();
    }

    private void rotateHands(int hour, int minute, int second) {
        int rotHr = (int) (30 * hour + 0.5f * minute);
        int rotMin = 6 * minute;
        int rotSec = 6 * second;

        /*handHour.setRotation(rotHr);
        handMinute.setRotation(rotMin);
        handSecond.setRotation(rotSec);*/
    }

    @Override
    public void onTimeChanged(Calendar time) {
        Timber.v("onTimeChanged()");

        int hr = time.get(Calendar.HOUR_OF_DAY) % 12;
        int min = time.get(Calendar.MINUTE);
        int sec = time.get(Calendar.SECOND);
        int ms = time.get(Calendar.MILLISECOND);

        //We want hr to be 12 instead of 0
        if (hr == 0)
            hr = 12;

        hours = hr;
        minutes = min;
        seconds = sec;

        rotateHands(hr, min, sec);
        invalidate();
    }

    @Override
    @DebugLog
    public void onActiveStateChanged(boolean active) {
        this.mActive = active;
        setImageResources();
    }

    @Override
    public boolean handleSecondsInDimMode() {
        return false;
    }

    @DebugLog
    private void setImageResources() {
        if (mInflated) {
            /*face.setImageResource(mActive ? R.drawable.watch_bg_normal : R.drawable.watch_bg_dimmed);
            shadowOverlay.setImageResource(mActive ? R.drawable.overlay_shadow_normal : R.drawable.overlay_shadow_dimmed);
            handHour.setImageResource(mActive ? R.drawable.hand_hour_normal : R.drawable.hand_hour_dimmed);
            handMinute.setImageResource(mActive ? R.drawable.hand_minute_normal : R.drawable.hand_minute_dimmed);
            handSecond.setImageResource(mActive ? R.drawable.hand_second_normal : R.drawable.hand_second_dimmed);
            handSecond.setVisibility(mActive ? View.VISIBLE : View.INVISIBLE);*/
        }
    }

    private Typeface loadTypeface(int typefaceNameResId) {
        String typefaceName = getResources().getString(typefaceNameResId);
        return Typeface.createFromAsset(getContext().getAssets(), typefaceName);
    }
}
