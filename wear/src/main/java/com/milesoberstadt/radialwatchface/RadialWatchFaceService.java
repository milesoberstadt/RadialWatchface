package com.milesoberstadt.radialwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.example.radialwatchdisplay.DrawableWatchFace;
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

import java.util.Calendar;

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

        boolean mMute;

        private GoogleApiClient mGoogleApiClient;

        private DrawableWatchFace faceDrawer;

        private Calendar mTime;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mTime.clear(intent.getStringExtra("time-zone"));
                //mTime.setToNow();
                mTime = Calendar.getInstance();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            faceDrawer = new DrawableWatchFace();

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            faceDrawer.loadSettings(getApplicationContext());

            setWatchFaceStyle(new WatchFaceStyle.Builder(RadialWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());


            mTime = Calendar.getInstance();

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
            faceDrawer.mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + faceDrawer.mLowBitAmbient);
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

            faceDrawer.setAmbient(inAmbientMode);

            invalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }

            mTime = Calendar.getInstance();

            faceDrawer.draw(canvas, mTime, bounds);

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
                //mTime.clear(TimeZone.getDefault().getID());
                //mTime.setToNow();
                mTime = Calendar.getInstance();

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

            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    DataItem di = event.getDataItem();
                    byte[] diBytes = di.getData();
                    DataMap dm =  DataMap.fromByteArray(diBytes);

                    if (dm.containsKey("color1"))
                        faceDrawer.color1 = Color.parseColor(dm.getString("color1"));
                    if (dm.containsKey("color2"))
                        faceDrawer.color2 = Color.parseColor(dm.getString("color2"));
                    if (dm.containsKey("color3"))
                        faceDrawer.color3 = Color.parseColor(dm.getString("color3"));
                    if (dm.containsKey("bg"))
                        faceDrawer.backgroundColor = Color.parseColor(dm.getString("bg"));
                    // Parse text colors...
                    if (dm.containsKey("textColor"))
                        faceDrawer.textColor = Color.parseColor(dm.getString("textColor"));
                    if (dm.containsKey("textStrokeColor"))
                        faceDrawer.textStrokeColor = Color.parseColor(dm.getString("textStrokeColor"));

                    if (dm.containsKey("enableText")){
                        faceDrawer.bTextEnabled = dm.getBoolean("enableText");
                    }
                    // TODO: Remove this, it's in for legacy theme support...
                    if (dm.containsKey("invertText")){
                        faceDrawer.textColor = 0xFF000000;
                        faceDrawer.textStrokeColor = 0xFFFFFFFF;
                    }
                    if (dm.containsKey("strokeText")){
                        faceDrawer.bTextStroke = dm.getBoolean("strokeText");
                    }
                    if (dm.containsKey("smoothAnim")){
                        faceDrawer.bShowMilli = dm.getBoolean("smoothAnim");
                    }
                    if (dm.containsKey("grayAmbient")){
                        faceDrawer.bGrayAmbient = dm.getBoolean("grayAmbient");
                    }
                    if (dm.containsKey("24hourtime")){
                        faceDrawer.b24HourTime = dm.getBoolean("24hourtime");
                    }
                    if (dm.containsKey("ringSizePercent")){
                        faceDrawer.ringSizePercent = dm.getInt("ringSizePercent");
                    }
                    if (dm.containsKey("textSizePercent")){
                        faceDrawer.textSizePercent = dm.getInt("textSizePercent");
                    }
                }
            }

            faceDrawer.saveSettings(getApplicationContext());
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connection failed");
            }
        }
    }
}
