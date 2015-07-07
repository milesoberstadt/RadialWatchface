package com.example.radialwatchdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;

import java.util.Calendar;

/**
 * Created by miles on 6/9/15.
 */
public class DrawableWatchFace {
    //This is something I'm going to play with...if we're in 24 hour mode, this determines if the hour bar is a percent of 24 instead of 12
    Boolean b24HourAltHour = true;
    Boolean bHourAddMinutes = false; //This is for rendering the hour bar, if true it adds the percent from minutes towards the next hour
    Boolean mActive = true;
    //This makes minutes and seconds always 2 digits
    Boolean bPadTimeVals = true;

    //Draw options - these settings need to be public...
    public Boolean bTextEnabled = true;
    public Boolean bTextStroke = false;
    public Boolean bShowMilli = false; //This decides if we display smooth animations for milliseconds
    public Boolean b24HourTime = false;
    public int color1 = 0xFFe51c23;
    public int color2 = 0xFF8bc34a;
    public int color3 = 0xFF03a9f4;
    public int backgroundColor = 0xFF000000;

    public int textColor = 0xFFFFFFFF;
    public int textStrokeColor = 0xFF000000;

    public int ringSizePercent = 50;
    public int textSizePercent = 50;

    public String colorComboName = "RGB";

    private Boolean _bShowBackground = true;


    public Boolean bGrayAmbient = false; //Black and white mode for ambient displays
    //Draw stuff
    Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mFontStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float maximumRingSize;
    float maximumTextSize;

    private float defaultPadding; // Originally, on a 320x320 watch, it was 15 px. I want to maintain that ratio...

    int myWidth = -1;
    float textAngle = 45;
    float textRadians;

    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;

    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    public boolean mLowBitAmbient = false;

    public DrawableWatchFace(){
        //Setup draw styles
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mFontStrokePaint.setStyle(Paint.Style.STROKE);
        mFontStrokePaint.setTextSize(24);
        mFontStrokePaint.setStrokeWidth(2);

        mFontPaint.setColor(textColor);
        mFontPaint.setTextSize(24);

        setBShowBackground(_bShowBackground);
    }

    public void loadSettings(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        //editor = settings.edit();
        //Get specific prefs, with defaults
        int tmp1 = settings.getInt("ringColor1", -1);
        int tmp2 = settings.getInt("ringColor2", -1);
        int tmp3 = settings.getInt("ringColor3", -1);

        if (tmp1 != -1)
            color1 = tmp1;
        if (tmp2 != -1)
            color2 = tmp2;
        if (tmp3 != -1)
            color3 = tmp3;

        backgroundColor = settings.getInt("bg", backgroundColor);
        textColor = settings.getInt("textColor", textColor);
        textStrokeColor = settings.getInt("textStrokeColor", textStrokeColor);
        ringSizePercent = settings.getInt("ringSizePercent", ringSizePercent);
        textSizePercent = settings.getInt("textSizePercent", textSizePercent);

        colorComboName = settings.getString("watchFaceCombo", "RGB");

        //Get text options
        bTextEnabled = settings.getBoolean("enableText", true);
        bTextStroke = settings.getBoolean("strokeText", false);
        bShowMilli = settings.getBoolean("smoothAnim", false);
        bGrayAmbient = settings.getBoolean("grayAmbient", false);
        b24HourTime = settings.getBoolean("24hourtime", false);
    }

    public void saveSettings(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();

        editor.putInt("ringColor1", color1);
        editor.putInt("ringColor2", color2);
        editor.putInt("ringColor3", color3);
        editor.putInt("bg", backgroundColor);
        editor.putInt("textColor", textColor);
        editor.putInt("textStrokeColor", textStrokeColor);
        editor.putInt("ringSizePercent", ringSizePercent);
        editor.putInt("textSizePercent", textSizePercent);

        editor.putString("watchFaceCombo", colorComboName);

        editor.putBoolean("enableText", bTextEnabled);
        editor.putBoolean("strokeText", bTextStroke);
        editor.putBoolean("smoothAnim", bTextStroke);
        editor.putBoolean("grayAmbient", bGrayAmbient);
        editor.putBoolean("24hourtime", b24HourTime);
        editor.apply();
    }

    public void setAmbient(Boolean state){
        mActive = !state;
        if (mLowBitAmbient) {
            boolean antiAlias = !state;

            mArcPaint.setAntiAlias(antiAlias);
            mFontPaint.setAntiAlias(antiAlias);
            mFontStrokePaint.setAntiAlias(antiAlias);
        }
    }

    public void setBShowBackground(Boolean state){
        _bShowBackground = state;
    }

    public void draw(Canvas canvas, Calendar mTime, Rect bounds, Context context){

        int hours = mTime.get(Calendar.HOUR_OF_DAY);
        int minutes = mTime.get(Calendar.MINUTE);
        int seconds = mTime.get(Calendar.SECOND);
        int milliseconds = mTime.get(Calendar.MILLISECOND);

        myWidth = bounds.width();
        maximumRingSize = (myWidth/3)/2;
        maximumTextSize = maximumRingSize;
        defaultPadding = myWidth * (float)0.046875; // Golden ratio derived from 15/320

        float strokeWidth = maximumRingSize * ((float)ringSizePercent/100.f);

        mArcPaint.setStrokeWidth(strokeWidth);

        if (_bShowBackground)
            mBackgroundPaint.setColor(backgroundColor);
        else
            mBackgroundPaint.setColor(0x00000000);

        // Draw our background / wipe the screen
        canvas.drawRect(bounds, mBackgroundPaint);

        RectF hoursOval = new RectF();
        RectF minutesOval = new RectF();
        RectF secondsOval = new RectF();

        //Define our colored radian zones
        //Rings get drawn around these rects where the center of the ring is the outside edge of the rect
        float totalMaxSize = maximumRingSize * 3;
        float ourTotalSize = strokeWidth * 3;

        float leftoverSpace = totalMaxSize - ourTotalSize;
        float ringPadding = Math.min(defaultPadding, leftoverSpace/3);

        float hourStrokeOffset = ringPadding+(strokeWidth/2);
        float minuteStrokeOffset = (ringPadding*2.f) + (strokeWidth * (float)1.5);
        float secondStrokeOffset = (ringPadding*3.f) + (strokeWidth * (float)2.5);

        //float hourStrokeOffset = maximumRingSize/2;
        //float minuteStrokeOffset = maximumRingSize * (float)1.5;
        //float secondStrokeOffset = maximumRingSize * (float)2.5;

        hoursOval.set(hourStrokeOffset, hourStrokeOffset, myWidth - (hourStrokeOffset), myWidth - (hourStrokeOffset));
        minutesOval.set(minuteStrokeOffset, minuteStrokeOffset, myWidth - (minuteStrokeOffset), myWidth - (minuteStrokeOffset));
        secondsOval.set(secondStrokeOffset, secondStrokeOffset, myWidth - (secondStrokeOffset), myWidth - (secondStrokeOffset));

        //Define our colored radian lengths
        Path secondsPath = new Path();
        float secondsSize = 6* seconds;
        if (bShowMilli)
            secondsSize += 6f * (milliseconds/1000f);
        secondsPath.addArc(secondsOval, -90, secondsSize);

        Path minutesPath = new Path();
        minutesPath.addArc(minutesOval, -90, 6 * minutes);
        Path hoursPath = new Path();
        float hourSize = -1;
        if (!b24HourAltHour || !b24HourTime) {
            hourSize = 30 * (hours % 12);
        }
        else {
            int displayHours = hours;
            if (displayHours >= 12)
                displayHours -= 12;
            hourSize = 30 * (displayHours);
        }
        //I THINK I want to always do this...
        if (hourSize == 0)
            hourSize = 30 * 12;

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

        /*Paint whitePaint = new Paint();
        whitePaint.setColor(0xFFFFFFFF);
        canvas.drawRect(hoursOval, whitePaint);*/
        //If we don't need grayscale in ambient mode, do this the normal way...
        if (!bGrayAmbient || this.mActive) {
            //Draw our colored radians after setting the color...
            mArcPaint.setColor(color3); //0xFF109618
            //Only draw our seconds path when we're active...
            if (this.mActive)
                canvas.drawPath(secondsPath, mArcPaint);
            mArcPaint.setColor(color2); //0xFF3366cc
            canvas.drawPath(minutesPath, mArcPaint);
            mArcPaint.setColor(color1); //0xFFdc3912
            canvas.drawPath(hoursPath, mArcPaint);
        }
        //If we want to do grayscale (high contrast mode) do that here...
        else if (bGrayAmbient){
            mArcPaint.setColor(0xFFBBBBBB);
            canvas.drawPath(minutesPath, mArcPaint);
            mArcPaint.setColor(0xFFFFFFFF);
            canvas.drawPath(hoursPath, mArcPaint);
        }

        //We want 12:00 to actually display at 12, so let's do that...unless we don't lol
        String displayHours;
        if (b24HourTime)
            displayHours = String.valueOf(hours);
        else {
            displayHours = String.valueOf(hours % 12);

        }
        //Only show a 0 at midnight with our alt style
        if (!b24HourAltHour || !b24HourTime) {
            if (displayHours.equals("0"))
                displayHours = "12";
        }

        String displayMinutes = String.valueOf(minutes);
        String displaySeconds = String.valueOf(seconds);
        if (bPadTimeVals)
        {
            if (displayMinutes.length()==1)
                displayMinutes = "0" + displayMinutes;
            if (displaySeconds.length()==1)
                displaySeconds = "0" + displaySeconds;
        }

        float textSize = maximumTextSize * (textSizePercent/100.f);
        textRadians = (textAngle) * (float)(Math.PI/180);

        //The math for horizontal offset is r * cos(t) where r is radius and t is radians
        float secondsXOffset = (float) ((secondsOval.width()/2)*Math.cos(textRadians));
        float minutesXOffset = (float) ((minutesOval.width()/2)*Math.cos(textRadians));
        float hoursXOffset = (float) ((hoursOval.width()/2)*Math.cos(textRadians));

        mFontPaint.setColor(textColor);

        if (bTextEnabled) {
            mFontStrokePaint.setTextSize(textSize);
            mFontPaint.setTextSize(textSize);

            // By default, text is bottom aligned to the center of the corresponding stroke
            // Offset by half of the text height and half the stroke size
            // All text SHOULD be the same height...
            Rect textBounds = new Rect();
            mFontPaint.getTextBounds(displayHours, 0, displayHours.length(), textBounds);
            float vOffset = (textBounds.height()/2);

            //If we have to draw a stroke, we need another paint...
            //Draw stroke in ambient so it's visible
            if ((bTextStroke && mActive) || (bGrayAmbient && !mActive)){
                if ((bGrayAmbient && !mActive)){
                    mFontStrokePaint.setColor(0xFFFFFFFF);
                }
                else{
                    mFontStrokePaint.setColor(textStrokeColor);
                }

                if (this.mActive)
                    canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, vOffset, mFontStrokePaint);
                canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, vOffset, mFontStrokePaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, vOffset, mFontStrokePaint);
            }
            //Override normal color for high contrast ambient mode
            if (!mActive && bGrayAmbient)
                mFontPaint.setColor(0xFF000000);

            if (this.mActive)
                canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, vOffset, mFontPaint);
            canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, vOffset, mFontPaint);
            canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, vOffset, mFontPaint);
        }
    }
}
