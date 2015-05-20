package com.milesoberstadt.radialwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;

/**
 * Created by milesoberstadt on 12/16/14.
 */
public class RadialWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements MessageApi.MessageListener,
            DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        String TAG = "RadialWatchFaceService";
        //This is something I'm going to play with...if we're in 24 hour mode, this determines if the hour bar is a percent of 24 instead of 12
        Boolean b24HourAltHour = true;
        Boolean bHourAddMinutes = false; //This is for rendering the hour bar, if true it adds the percent from minutes towards the next hour
        Boolean mActive = true;
        //This makes minutes and seconds always 2 digits
        Boolean bPadTimeVals = true;

        //Draw options
        Boolean bInvertText = false;
        Boolean bTextEnabled = true;
        Boolean bTextStroke = false;
        Boolean bShowMilli = false; //This decides if we display smooth animations for milliseconds
        Boolean b24HourTime = false;
        Boolean bGrayAmbient = false; //Black and white mode for ambient displays


        //Draw stuff
        Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mFontStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float strokeWidth = 30.f;
        int myWidth = -1;
        int color1 = 0xFFe51c23;
        int color2 = 0xFF8bc34a;
        int color3 = 0xFF03a9f4;
        float textAngle = 45;
        float textRadians = 0;

        boolean mMute;
        Time mTime;

        private GoogleApiClient mGoogleApiClient;

        protected SharedPreferences settings;
        protected SharedPreferences.Editor editor;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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

            //Get text options
            bTextEnabled = settings.getBoolean("enableText", true);
            bInvertText = settings.getBoolean("invertText", false);
            bTextStroke = settings.getBoolean("strokeText", false);
            bShowMilli = settings.getBoolean("smoothAnim", false);
            bGrayAmbient = settings.getBoolean("grayAmbient", false);
            b24HourTime = settings.getBoolean("24hourtime", false);

            setWatchFaceStyle(new WatchFaceStyle.Builder(RadialWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = RadialWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            //Setup draw styles
            mArcPaint.setStyle(Paint.Style.STROKE);
            mArcPaint.setStrokeWidth(strokeWidth);
            mArcPaint.setStrokeCap(Paint.Cap.BUTT);

            mFontStrokePaint.setStyle(Paint.Style.STROKE);
            mFontStrokePaint.setTextSize(24);
            mFontStrokePaint.setStrokeWidth(2);

            mFontPaint.setColor(0xFFFFFFFF);
            mFontPaint.setTextSize(24);

            textRadians = textAngle * (float)(3.14159/180);

            mTime = new Time();

            try {
                mGoogleApiClient.connect();
            }
            catch (Exception e){
                Log.d(TAG, e.getMessage());
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            mActive = !inAmbientMode;
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;

                mArcPaint.setAntiAlias(antiAlias);
                mFontPaint.setAntiAlias(antiAlias);
                mFontStrokePaint.setAntiAlias(antiAlias);
            }
            invalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                /*mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);*/
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }

            long now = System.currentTimeMillis();
            mTime.set(now);
            int milliseconds = (int) (now % 1000);

            int seconds = mTime.second;
            int minutes = mTime.minute;
            int hours = mTime.hour;

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            myWidth = bounds.width();
            if (myWidth > 300)
                strokeWidth = 30.f;
            else
                strokeWidth = 25.f;


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

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            RadialWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            RadialWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            Log.d(TAG, messageEvent.toString());
        }

        @Override
        public void onConnected(Bundle bundle) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connected to Google Api Service");
            }
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connection suspended");
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {

            int c1 = -1, c2 = -1, c3 = -1;
            int changedPropCount = 0;

            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    DataItem di = event.getDataItem();
                    byte[] diBytes = di.getData();
                    DataMap dm =  DataMap.fromByteArray(diBytes);

                    if (dm.containsKey("color1"))
                        c1 = Color.parseColor(dm.getString("color1"));
                    if (dm.containsKey("color2"))
                        c2 = Color.parseColor(dm.getString("color2"));
                    if (dm.containsKey("color3"))
                        c3 = Color.parseColor(dm.getString("color3"));

                    if (dm.containsKey("enableText")){
                        changedPropCount++;
                        bTextEnabled = dm.getBoolean("enableText");
                    }
                    if (dm.containsKey("invertText")){
                        changedPropCount++;
                        bInvertText = dm.getBoolean("invertText");
                    }
                    if (dm.containsKey("strokeText")){
                        changedPropCount++;
                        bTextStroke = dm.getBoolean("strokeText");
                    }
                    if (dm.containsKey("smoothAnim")){
                        changedPropCount++;
                        bShowMilli = dm.getBoolean("smoothAnim");
                    }
                    if (dm.containsKey("grayAmbient")){
                        changedPropCount++;
                        bGrayAmbient = dm.getBoolean("grayAmbient");
                    }
                    if (dm.containsKey("24hourtime")){
                        changedPropCount++;
                        b24HourTime = dm.getBoolean("24hourtime");
                    }

                }
            }

            //If we have anything to save, make sure stuff is ready...
            if ((c1 != -1 && c2 != -1 && c3 != -1) || (changedPropCount>0)){
                settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                editor = settings.edit();
            }

            if (c1 != -1 && c2 != -1 && c3 != -1){
                editor.putInt("ringColor1", c1);
                editor.putInt("ringColor2", c2);
                editor.putInt("ringColor3", c3);
                editor.apply();

                color1 = c1;
                color2 = c2;
                color3 = c3;
            }

            if (changedPropCount>0){
                //If something changed, save all our vars again...
                editor.putBoolean("enableText", bTextEnabled);
                editor.putBoolean("invertText", bInvertText);
                editor.putBoolean("strokeText", bTextStroke);
                editor.putBoolean("smoothAnim", bTextStroke);
                editor.putBoolean("grayAmbient", bGrayAmbient);
                editor.putBoolean("24hourtime", b24HourTime);
                editor.apply();
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connection failed");
            }
        }
    }
}
