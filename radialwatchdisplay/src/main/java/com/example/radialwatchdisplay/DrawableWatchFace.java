package com.example.radialwatchdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.text.format.Time;

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
    public Boolean bInvertText = false;
    public Boolean bTextEnabled = true;
    public Boolean bTextStroke = false;
    public Boolean bShowMilli = false; //This decides if we display smooth animations for milliseconds
    public Boolean b24HourTime = false;
    public int color1 = 0xFFe51c23;
    public int color2 = 0xFF8bc34a;
    public int color3 = 0xFF03a9f4;
    public String colorComboName = "RGB";

    private Boolean _bShowBackground = true;


    public Boolean bGrayAmbient = false; //Black and white mode for ambient displays
    //Draw stuff
    Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mFontStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    float strokeWidth = 30.f;
    int myWidth = -1;
    float textAngle = 45;
    float textRadians = 0;

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
        mArcPaint.setStrokeWidth(strokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mFontStrokePaint.setStyle(Paint.Style.STROKE);
        mFontStrokePaint.setTextSize(24);
        mFontStrokePaint.setStrokeWidth(2);

        mFontPaint.setColor(0xFFFFFFFF);
        mFontPaint.setTextSize(24);

        setBShowBackground(_bShowBackground);

        textRadians = textAngle * (float)(3.14159/180);
    }

    public void loadSettings(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();
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

        colorComboName = settings.getString("watchFaceCombo", "RGB");

        //Get text options
        bTextEnabled = settings.getBoolean("enableText", true);
        bInvertText = settings.getBoolean("invertText", false);
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
        editor.putString("watchFaceCombo", colorComboName);

        editor.putBoolean("enableText", bTextEnabled);
        editor.putBoolean("invertText", bInvertText);
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
        if (_bShowBackground)
            mBackgroundPaint.setColor(0xFF000000);
        else
            mBackgroundPaint.setColor(0x00000000);
    }

    public void draw(Canvas canvas, Calendar mTime, Rect bounds){

        int hours = mTime.get(Calendar.HOUR_OF_DAY);
        int minutes = mTime.get(Calendar.MINUTE);
        int seconds = mTime.get(Calendar.SECOND);
        int milliseconds = mTime.get(Calendar.MILLISECOND);

        myWidth = bounds.width();
        if (myWidth > 300)
            strokeWidth = 30.f;
        else
            strokeWidth = 25.f;

        // Draw our background / wipe the screen
        canvas.drawRect(bounds, mBackgroundPaint);

        RectF secondsOval = new RectF();
        RectF minutesOval = new RectF();
        RectF hoursOval = new RectF();

        //Define our colored radian zones
        hoursOval.set(strokeWidth, strokeWidth, myWidth - (strokeWidth), myWidth - (strokeWidth));
        minutesOval.set(strokeWidth * (float)2.5, strokeWidth * (float)2.5, myWidth - (strokeWidth * (float)2.5), myWidth - (strokeWidth * (float)2.5));
        secondsOval.set(strokeWidth * (float)4, strokeWidth * (float)4, myWidth - (strokeWidth * (float)4), myWidth - (strokeWidth * (float)4));

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

        //The math for horizontal offset is r * cos(t) where r is radius and t is radians
        float secondsXOffset = (float) ((secondsOval.width()/2)*Math.cos(textRadians));
        float minutesXOffset = (float) ((minutesOval.width()/2)*Math.cos(textRadians));
        float hoursXOffset = (float) ((hoursOval.width()/2)*Math.cos(textRadians));

        if (bInvertText){
            mFontPaint.setColor(0xFF000000);
        }
        else{
            mFontPaint.setColor(0xFFFFFFFF);
        }

        if (bTextEnabled) {
            //If we have to draw a stroke, we need another paint...
            //Draw stroke in ambient so it's visible
            if ((bTextStroke && mActive) || (bGrayAmbient && !mActive)){
                if (bInvertText || (bGrayAmbient && !mActive)){
                    mFontStrokePaint.setColor(0xFFFFFFFF);
                }
                else{
                    mFontStrokePaint.setColor(0xFF000000);
                }

                //Text draws differ based on device size...
                if (myWidth >= 320) {
                    if (this.mActive)
                        canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, 10, mFontStrokePaint);
                    canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, 10, mFontStrokePaint);
                    canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 10, mFontStrokePaint);
                }
                else {
                    if (this.mActive)
                        canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, 5, mFontStrokePaint);
                    canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, 5, mFontStrokePaint);
                    canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 5, mFontStrokePaint);
                }
            }
            //Override normal color for high contrast ambient mode
            if (!mActive && bGrayAmbient)
                mFontPaint.setColor(0xFF000000);
            //Text draws differ based on device size...
            if (myWidth >= 320) {
                if (this.mActive)
                    canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, 10, mFontPaint);
                canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, 10, mFontPaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 10, mFontPaint);
            } else {
                mFontPaint.setTextSize(20);
                if (this.mActive)
                    canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, 5, mFontPaint);
                canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, 5, mFontPaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 5, mFontPaint);
            }
        }
    }
}
