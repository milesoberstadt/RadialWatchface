package com.milesoberstadt.radialwatchface.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.watchface.gears.widget.IWatchface;
import com.twotoasters.watchface.gears.widget.Watch;

import java.util.Calendar;
import java.util.Map;

import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class Watchface extends FrameLayout implements IWatchface, MessageApi.MessageListener,
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    /*@InjectView(R.id.face)              ImageView face;
    @InjectView(R.id.shadow_overlay)    ImageView shadowOverlay;
    @InjectView(R.id.hand_hour)         ImageView handHour;
    @InjectView(R.id.hand_minute)       ImageView handMinute;
    @InjectView(R.id.hand_second)       ImageView handSecond;*/

    private boolean bTextEnabled = true, bInvertText = false, bTextStroke = false;

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
    private Paint mFontStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float strokeWidth = 30.f;
    private int myWidth = -1;
    private int color1 = 0xFFe51c23;
    private int color2 = 0xFF8bc34a;
    private int color3 = 0xFF03a9f4;
    private float textAngle = 45;
    private float textRadians = 0;

    private String TAG = "LOLTEST";

    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;

    private GoogleApiClient mGoogleApiClient;

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
        Log.d(TAG, "init called");
        mWatch = new Watch(this);
        setWillNotDraw(false);

        setupDrawObjects();


        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        try {
            mGoogleApiClient.connect();
        }
        catch (Exception e){
            Log.d(TAG, e.getMessage());
        }

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();
        //Get specific prefs, with defaults
        int tmp1 = settings.getInt("ringColor1", -1);
        int tmp2 = settings.getInt("ringColor2", -1);
        int tmp3 = settings.getInt("ringColor3", -1);

        if (tmp1 != -1){
            color1 = tmp1;
            //Log.d(TAG, "Color 1 was applied from settings");
        }
        if (tmp2 != -1)
            color2 = tmp2;
        if (tmp3 != -1)
            color3 = tmp3;

        //Get text options
        bTextEnabled = settings.getBoolean("enableText", true);
        bInvertText = settings.getBoolean("invertText", false);
        bTextStroke = settings.getBoolean("strokeText", false);
    }

    private void setupDrawObjects(){

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(strokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mFontStrokePaint.setStyle(Paint.Style.STROKE);
        mFontStrokePaint.setTextSize(24);
        mFontStrokePaint.setStrokeWidth(2);

        mFontPaint.setColor(0xFFFFFFFF);
        mFontPaint.setTextSize(24);

        textRadians = textAngle * (float)(3.14159/180);
    }


    @Override
    protected void onDraw(Canvas canvas){

        Timber.v("onDraw()");
        super.onDraw(canvas);

        myWidth = getWidth();
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
        mArcPaint.setColor(color3); //0xFF109618
        canvas.drawPath(secondsPath,mArcPaint);
        mArcPaint.setColor(color2); //0xFF3366cc
        canvas.drawPath(minutesPath,mArcPaint);
        mArcPaint.setColor(color1); //0xFFdc3912
        canvas.drawPath(hoursPath, mArcPaint);

        //We want 12:00 to actually display at 12, so let's do that
        String displayHours = String.valueOf(hours);
        if (displayHours.equals("0"))
            displayHours = "12";

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
            if (bTextStroke){
                if (bInvertText){
                    mFontStrokePaint.setColor(0xFFFFFFFF);
                }
                else{
                    mFontStrokePaint.setColor(0xFF000000);
                }
                //Text draws differ based on device size...
                if (myWidth >= 320) {
                    canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, secondsXOffset, 10, mFontStrokePaint);
                    canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, minutesXOffset, 10, mFontStrokePaint);
                    canvas.drawTextOnPath(String.valueOf(hours), hoursLabelPath, hoursXOffset, 10, mFontStrokePaint);
                }
                else {
                    canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, secondsXOffset, 5, mFontStrokePaint);
                    canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, minutesXOffset, 5, mFontStrokePaint);
                    canvas.drawTextOnPath(String.valueOf(hours), hoursLabelPath, hoursXOffset, 5, mFontStrokePaint);
                }
            }
            //Text draws differ based on device size...
            if (myWidth >= 320) {
                canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, secondsXOffset, 10, mFontPaint);
                canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, minutesXOffset, 10, mFontPaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 10, mFontPaint);
            } else {
                mFontPaint.setTextSize(20);
                canvas.drawTextOnPath(String.valueOf(seconds), secondsLabelPath, secondsXOffset, 5, mFontPaint);
                canvas.drawTextOnPath(String.valueOf(minutes), minutesLabelPath, minutesXOffset, 5, mFontPaint);
                canvas.drawTextOnPath(displayHours, hoursLabelPath, hoursXOffset, 5, mFontPaint);
            }
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

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        int c1 = -1, c2 = -1, c3 = -1;
        boolean b1 = false, b2 = false, b3 = false;

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
                    b1 = true;
                    bTextEnabled = dm.getBoolean("enableText");
                }
                if (dm.containsKey("invertText")){
                    b2 = true;
                    bInvertText = dm.getBoolean("invertText");
                }
                if (dm.containsKey("strokeText")){
                    b3 = true;
                    bTextStroke = dm.getBoolean("strokeText");
                }

            }
        }

        //If we have anything to save, make sure stuff is ready...
        if ((c1 != -1 && c2 != -1 && c3 != -1) || (b1 || b2 || b3)){
            settings = PreferenceManager.getDefaultSharedPreferences(getContext());
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

        if (b1 || b2 || b3){
            if (b1)
                editor.putBoolean("enableText", bTextEnabled);
            if (b2)
                editor.putBoolean("invertText", bInvertText);
            if (b3)
                editor.putBoolean("strokeText", bTextStroke);
            editor.apply();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
