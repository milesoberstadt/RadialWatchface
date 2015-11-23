package com.example.radialwatchdisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

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
    public Boolean bReverseRingOrder = false;
    public Boolean bShowSeconds = true;

    public int color1 = 0xFFe51c23;
    public int color2 = 0xFF8bc34a;
    public int color3 = 0xFF03a9f4;
    public int backgroundColor = 0xFF000000;

    public int textColor = 0xFFFFFFFF;
    public int textStrokeColor = 0xFF000000;

    public int ringSizePercent = 50;
    public int textSizePercent = 50;

    public String colorComboName = "RGB";

    public Gson gson = new Gson();
    //public JSONArray customRings = new JSONArray();
    public ArrayList<String> customRings = new ArrayList<>();

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
    public float textAngle = 45;
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
        mFontStrokePaint.setTextAlign(Paint.Align.CENTER);
        mFontStrokePaint.setStrokeWidth(2);

        mFontPaint.setColor(textColor);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
        mFontPaint.setTextSize(24);

        setBShowBackground(_bShowBackground);
    }

    public void resetDefaultStyle(){
        color1 = 0xFFe51c23;
        color2 = 0xFF8bc34a;
        color3 = 0xFF03a9f4;
        backgroundColor = 0xFF000000;

        textColor = 0xFFFFFFFF;
        textStrokeColor = 0xFF000000;

        ringSizePercent = 50;
        textSizePercent = 50;

        bTextEnabled = true;
        bTextStroke = false;
        bShowMilli = false;
        bGrayAmbient = false;
        b24HourTime = false;
    }

    public void loadSettingsLegacy(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        //editor = settings.edit();
        //Get specific prefs, with defaults
        int tmp1 = settings.getInt("ringColor1", -1);
        int tmp2 = settings.getInt("ringColor2", -1);
        int tmp3 = settings.getInt("ringColor3", -1);

        // If we've got these, we're using legacy settings, if not, let's use new stuff.
        if (tmp1 != -1 && tmp2 != -1 && tmp3 != -1){
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
    }

    public void loadSettings(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        //editor = settings.edit();
        //Get specific prefs, with defaults
        int tmp1 = settings.getInt("ringColor1", -1);
        int tmp2 = settings.getInt("ringColor2", -1);
        int tmp3 = settings.getInt("ringColor3", -1);

        // If we've got these, we're using legacy settings, if not, let's use new stuff.
        if (tmp1 != -1 && tmp2 != -1 && tmp3 != -1){
            loadSettingsLegacy(context);
        }
        // Otherwise, use new settings...
        else{
            colorComboName = settings.getString("watchFaceCombo", "RGB");

            String customRingsFromSettings = settings.getString("customRings", "");
            if (customRingsFromSettings != ""){
                buildCustomRingsFromString(customRingsFromSettings);
            }

            //If we're using new settings, but have no custom rings, check yourself before you rek yourself
            else{
                //Check for an invalid face...
                if (!colorComboName.contains("Custom")) {
                    colorComboName = "RGB";
                    resetDefaultStyle();
                    return;
                }
            }

            // If we're using a built in face, load it's settings!
            if (colorComboName.indexOf("Custom") == -1){
                applySettingsFromPresetRing(context);
            }
            // Otherwise, load settings from our custom ring...
            else{
                int customIndex = Integer.parseInt(colorComboName.split("Custom ")[1]) - 1;
                //Reject this if it's out of bounds
                if (customIndex >= customRings.size())
                    return;

                JsonParser parser = new JsonParser();
                JsonObject customFace = (JsonObject) parser.parse(customRings.get(customIndex));
                applySettingsFromCustomRing(customIndex);
            }
        }
    }

    public void saveSettings(Context context){
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();

        editor.putString("watchFaceCombo", colorComboName);

        editor.apply();

        saveCustomRings(context);
    }

    public void buildCustomRingsFromString(String sourceString){
        JsonParser parser = new JsonParser();
        JsonElement foundElement = parser.parse(sourceString);
        JsonArray loadedArray = foundElement.getAsJsonArray();
        for (int i=0; i<loadedArray.size(); i++){
            if (customRings.size() <= i)
                customRings.add(loadedArray.get(i).toString());
            else
                customRings.set(i, (loadedArray.get(i).toString()));
        }
    }

    public void applySettingsFromCustomRing(int customIndex){
        //Reject this if it's out of bounds
        if (customIndex >= customRings.size())
            return;

        JsonParser parser = new JsonParser();
        JsonObject customFace = (JsonObject) parser.parse(customRings.get(customIndex));

        color1 = customFace.get("ringColor1").getAsInt();
        color2 = customFace.get("ringColor2").getAsInt();
        color3 = customFace.get("ringColor3").getAsInt();

        backgroundColor = customFace.get("bg").getAsInt();
        textColor = customFace.get("textColor").getAsInt();
        textAngle = customFace.get("textAngle").getAsInt();
        textStrokeColor = customFace.get("textStrokeColor").getAsInt();

        ringSizePercent = customFace.get("ringSizePercent").getAsInt();
        textSizePercent = customFace.get("textSizePercent").getAsInt();

        bTextEnabled = customFace.get("enableText").getAsBoolean();
        bTextStroke = customFace.get("strokeText").getAsBoolean();
        bShowMilli = customFace.get("smoothAnim").getAsBoolean();
        bGrayAmbient = customFace.get("grayAmbient").getAsBoolean();
        b24HourTime = customFace.get("24hourtime").getAsBoolean();
        bReverseRingOrder = customFace.get("reverseRingOrder").getAsBoolean();
        bShowSeconds = customFace.get("showSeconds").getAsBoolean();
    }

    public void applySettingsFromPresetRing(Context context){
        Resources res = context.getResources();
        ArrayList<Integer> colorArraysToFetch = new ArrayList<>();
        colorArraysToFetch.add(R.array.watch_rgb_array);
        colorArraysToFetch.add(R.array.watch_cmy_array);
        colorArraysToFetch.add(R.array.watch_crayon_array);
        colorArraysToFetch.add(R.array.watch_gray_array);
        colorArraysToFetch.add(R.array.watch_pastels_array);

        String[] colorNames = res.getStringArray(R.array.watch_faces);
        int facePos = Arrays.asList(colorNames).indexOf(colorComboName);
        if (facePos > -1){
            resetDefaultStyle();

            String[] colors = res.getStringArray(colorArraysToFetch.get(facePos));
            color1 = Color.parseColor(colors[0]);
            color2 = Color.parseColor(colors[1]);
            color3 = Color.parseColor(colors[2]);
        }
    }

    public void convertSettingsToCustom(Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        //Check for default settings
        int tempRing1 = -1;
        tempRing1 = settings.getInt("ringColor1", -1);
        if (tempRing1 != -1){
            colorComboName = "Custom 1";
            //If we have any of these settings, remove them, we'll start making custom faces.
            editor = settings.edit();
            editor.clear();
            editor.putString("watchFaceCombo", colorComboName);
            editor.apply();

            //Make a new custom ring out of the current settings
            addUpdateCustomRing(context, 0);
        }
    }

    public void addUpdateCustomRing(Context context, int customIndex){
        JsonObject customRing = new JsonObject();
        customRing.addProperty("ringColor1", color1);
        customRing.addProperty("ringColor2", color2);
        customRing.addProperty("ringColor3", color3);
        customRing.addProperty("bg", backgroundColor);
        customRing.addProperty("textColor", textColor);
        customRing.addProperty("textStrokeColor", textStrokeColor);
        customRing.addProperty("textAngle", textAngle);
        customRing.addProperty("ringSizePercent", ringSizePercent);
        customRing.addProperty("textSizePercent", textSizePercent);

        customRing.addProperty("enableText", bTextEnabled);
        customRing.addProperty("strokeText", bTextStroke);
        customRing.addProperty("smoothAnim", bShowMilli);
        customRing.addProperty("grayAmbient", bGrayAmbient);
        customRing.addProperty("24hourtime", b24HourTime);
        customRing.addProperty("reverseRingOrder", bReverseRingOrder);
        customRing.addProperty("showSeconds", bShowSeconds);

        String customToString = gson.toJson(customRing);
        // Make sure our array is big enough for this...
        while(customRings.size() <= customIndex){
            customRings.add(null);
        }
        customRings.set(customIndex, customToString);

        saveCustomRings(context);
    }

    public void saveCustomRings(Context context){
        //If we aren't on a default, and have no rings, make a custom one just in time (usually for watch)
        if (colorComboName.contains("Custom ") && customRings.size() == 0){
            addUpdateCustomRing(context, 0);
        }

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();

        JsonArray ringsArray = new JsonArray();
        JsonParser parser = new JsonParser();
        for (int i=0; i<customRings.size(); i++) {
            ringsArray.add(parser.parse(customRings.get(i)));
        }

        editor.putString("customRings", ringsArray.toString());
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

        RectF outerOval = new RectF();
        RectF middleOval = new RectF();
        RectF innerOval = new RectF();

        //Use these for referring to which rings should be used to paint what, since those can be changed
        RectF hoursOval, minutesOval, secondsOval;

        //Define our colored radian zones
        //Rings get drawn around these rects where the center of the ring is the outside edge of the rect
        float totalMaxSize = maximumRingSize * 3;
        float ourTotalSize = strokeWidth * 3;

        float leftoverSpace = totalMaxSize - ourTotalSize;
        float ringPadding = Math.min(defaultPadding, leftoverSpace/3);

        float hourStrokeOffset = ringPadding+(strokeWidth/2);
        float minuteStrokeOffset = (ringPadding*2.f) + (strokeWidth * (float)1.5);
        float secondStrokeOffset = (ringPadding*3.f) + (strokeWidth * (float)2.5);

        outerOval.set(hourStrokeOffset, hourStrokeOffset, myWidth - (hourStrokeOffset), myWidth - (hourStrokeOffset));
        middleOval.set(minuteStrokeOffset, minuteStrokeOffset, myWidth - (minuteStrokeOffset), myWidth - (minuteStrokeOffset));
        innerOval.set(secondStrokeOffset, secondStrokeOffset, myWidth - (secondStrokeOffset), myWidth - (secondStrokeOffset));

        //Figure out which ovals to use where...
        if (!bReverseRingOrder){
            hoursOval = outerOval;
            minutesOval = middleOval;
            secondsOval = innerOval;
        }
        else if (bReverseRingOrder && bShowSeconds){
            hoursOval = innerOval;
            minutesOval = middleOval;
            secondsOval = outerOval;
        }
        else{ //if (bReverseRingOrder && !bShowSeconds){ (Only condition that SHOULD match...)
            hoursOval = middleOval;
            minutesOval = outerOval;
            secondsOval = innerOval;
        }

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
        //People think of a circle's origin starting at the top most, middle most point. Let's make that happen.

        Path secondsLabelPath = new Path();
        secondsLabelPath.addArc(secondsOval, textAngle-180, 180);
        Path minutesLabelPath = new Path();
        minutesLabelPath.addArc(minutesOval, textAngle-180, 180);
        Path hoursLabelPath = new Path();
        hoursLabelPath.addArc(hoursOval, textAngle-180, 180);

        //If we don't need grayscale in ambient mode, do this the normal way...
        if (!bGrayAmbient || this.mActive) {
            //Draw our colored radians after setting the color...
            mArcPaint.setColor(color3); //0xFF109618
            //Only draw our seconds path when we're active...
            if (this.mActive && this.bShowSeconds)
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
        textRadians = 0;//(textAngle) * (float)(Math.PI/180);

        //The math for horizontal offset is r * cos(t) where r is radius and t is radians
        float secondsXOffset = 0;//(float) ((secondsOval.width()/2)*Math.cos(textRadians));
        float minutesXOffset = 0;//(float) ((minutesOval.width()/2)*Math.cos(textRadians));
        float hoursXOffset = 0;//(float) ((hoursOval.width()/2)*Math.cos(textRadians));

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

                if (this.mActive && this.bShowSeconds)
                    canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, vOffset, mFontStrokePaint);
                canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, vOffset, mFontStrokePaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, vOffset, mFontStrokePaint);
            }
            //Override normal color for high contrast ambient mode
            if (!mActive && bGrayAmbient)
                mFontPaint.setColor(0xFF000000);

            if (this.mActive && this.bShowSeconds)
                canvas.drawTextOnPath(displaySeconds, secondsLabelPath, secondsXOffset, vOffset, mFontPaint);
            canvas.drawTextOnPath(displayMinutes, minutesLabelPath, minutesXOffset, vOffset, mFontPaint);
            canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, vOffset, mFontPaint);
        }
    }
}
