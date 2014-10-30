package com.milesoberstadt.radialwatchface;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/**
 * Created by Miles on 10/13/2014.
 */
public class CanvasDrawnRingView extends View{

    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;
    private int milliseconds = 0;

    private Boolean bHourAddMinutes = false; //This is for rendering the hour bar, if true it adds the percent from minutes towards the next hour
    public Boolean bShowMilli = true; //This decides if we display smooth animations for milliseconds

    //Draw stuff
    private Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF secondsOval = new RectF();
    private RectF minutesOval = new RectF();
    private RectF hoursOval = new RectF();

    private Path secondsPath = new Path();
    private Path minutesPath = new Path();
    private Path hoursPath = new Path();

    //These are our text radian paths, nice and long so our text will display correctly
    private Path secondsLabelPath = new Path();
    private Path minutesLabelPath = new Path();
    private Path hoursLabelPath = new Path();

    private float strokeWidth = 30.f;
    private int myWidth = 320;
    public int color1 = 0xFFe51c23;
    public int color2 = 0xFF8bc34a;
    public int color3 = 0xFF03a9f4;
    private float textAngle = 45;
    private float textRadians = 0;

    private Calendar time;

    public CanvasDrawnRingView(Context context){
        super(context);

        init();
    }

    public CanvasDrawnRingView(Context context, AttributeSet attrs){
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.radialColors);
        color1 = a.getColor(R.styleable.radialColors_hour_color, color1);
        color2 = a.getColor(R.styleable.radialColors_minute_color, color2);
        color3 = a.getColor(R.styleable.radialColors_second_color, color3);

        init();
    }

    private void init(){
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(strokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mFontPaint.setColor(0xFFFFFFFF);
        mFontPaint.setTextSize(24);

        textRadians = textAngle * (float)(3.14159/180);

        //Define our colored radian zones
        hoursOval.set(strokeWidth, strokeWidth, myWidth - (strokeWidth), myWidth - (strokeWidth));
        minutesOval.set(strokeWidth * (float)2.5, strokeWidth * (float)2.5, myWidth - (strokeWidth * (float)2.5), myWidth - (strokeWidth * (float)2.5));
        secondsOval.set(strokeWidth * (float)4, strokeWidth * (float)4, myWidth - (strokeWidth * (float)4), myWidth - (strokeWidth * (float)4));

        secondsLabelPath.addArc(secondsOval, -90, 180);
        minutesLabelPath.addArc(minutesOval, -90, 90);
        hoursLabelPath.addArc(hoursOval, -90, 90);


        updateSystemTime();
        initShapes();
    }

    protected void onDraw(Canvas canvas){

        //Draw our colored radians after setting the color...
        mArcPaint.setColor(color3); //0xFF109618
        canvas.drawPath(secondsPath,mArcPaint);
        mArcPaint.setColor(color2); //0xFF3366cc
        canvas.drawPath(minutesPath,mArcPaint);
        mArcPaint.setColor(color1); //0xFFdc3912
        canvas.drawPath(hoursPath, mArcPaint);

        //The math for horizontal offset is r * cos(t) where r is radius and t is radians
        float secondsXOffset = (float) ((secondsOval.width()/2)*Math.cos(textRadians));
        float minutesXOffset = (float) ((minutesOval.width()/2)*Math.cos(textRadians));
        float hoursXOffset = (float) ((hoursOval.width()/2)*Math.cos(textRadians));

        canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, secondsXOffset, 10, mFontPaint);
        canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, minutesXOffset, 10, mFontPaint);
        canvas.drawTextOnPath(String.valueOf(hours), hoursLabelPath, hoursXOffset, 10, mFontPaint);

    }

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec); // receives parents width in pixel
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        //your desired sizes, converted from pixels to setMeasuredDimension's unit
        final int desiredWSpec = MeasureSpec.makeMeasureSpec(myWidth, MeasureSpec.EXACTLY);
        final int desiredHSpec = MeasureSpec.makeMeasureSpec(myWidth, MeasureSpec.EXACTLY);
        this.setMeasuredDimension(desiredWSpec, desiredHSpec);
    }

    public void updateSystemTime(){
        //Initial time vals
        time = Calendar.getInstance();

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
        milliseconds = ms;
    }

    public void updateShapes(){
        initShapes();

        invalidate();
    }

    private void initShapes(){
        //Define our colored radian lengths
        secondsPath = new Path();
        float secondsSize = 6* seconds;
        if (bShowMilli)
            secondsSize += 6f * (milliseconds/1000f);
        secondsPath.addArc(secondsOval, -90, secondsSize);

        minutesPath = new Path();
        minutesPath.addArc(minutesOval, -90, 6 * minutes);

        hoursPath = new Path();
        float hourSize = 30 * hours;
        //This adds the percentage of the the current hour to our hour radian's size
        if (bHourAddMinutes)
            hourSize += 0.5f * minutes;
        hoursPath.addArc(hoursOval, -90, (int) hourSize);
    }
}
