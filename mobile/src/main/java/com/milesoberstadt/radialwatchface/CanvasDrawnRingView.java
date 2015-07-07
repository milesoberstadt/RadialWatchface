package com.milesoberstadt.radialwatchface;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.example.radialwatchdisplay.DrawableWatchFace;

import java.util.Calendar;

/**
 * Created by Miles on 10/13/2014.
 */
public class CanvasDrawnRingView extends View{

    private Rect mySize;

    private Calendar time;

    public DrawableWatchFace faceDrawer;

    //Time stuff...
    private Handler graphicsUpdateHandler = new Handler();
    private Runnable graphicsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateSystemTime();
            invalidate();

            graphicsUpdateHandler.postDelayed(this, (1000 / 60)); //Desired framerate is 60fps
        }
    };

    public CanvasDrawnRingView(Context context){
        super(context);

        init();
    }

    public CanvasDrawnRingView(Context context, AttributeSet attrs){
        super(context, attrs);

        faceDrawer = new DrawableWatchFace();
        //Don't show background with this...
        //faceDrawer.setBShowBackground(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.radialColors);
        faceDrawer.color1 = a.getColor(R.styleable.radialColors_hour_color, faceDrawer.color1);
        faceDrawer.color2 = a.getColor(R.styleable.radialColors_minute_color, faceDrawer.color2);
        faceDrawer.color3 = a.getColor(R.styleable.radialColors_second_color, faceDrawer.color3);
        a.recycle();

        if (!isInEditMode()){
            init();

            graphicsUpdateHandler.postDelayed(graphicsUpdateRunnable, 0);
        }
    }

    private void init(){
        updateSystemTime();
    }

    protected void onDraw(Canvas canvas) {

        mySize = new Rect(0,0,getWidth(),getWidth());
        if (!isInEditMode())
            faceDrawer.draw(canvas, time, mySize, getContext());
    }

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //int parentWidth = MeasureSpec.getSize(widthMeasureSpec); // receives parents width in pixel
        //int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        //your desired sizes, converted from pixels to setMeasuredDimension's unit
        //final int desiredWSpec = MeasureSpec.makeMeasureSpec(mySize.width(), MeasureSpec.EXACTLY);
        //final int desiredHSpec = MeasureSpec.makeMeasureSpec(mySize.height(), MeasureSpec.EXACTLY);
        this.setMeasuredDimension(widthMeasureSpec, widthMeasureSpec);
    }

    public void updateSystemTime(){
        //Initial time vals
        time = Calendar.getInstance();
    }

    public void updateShapes(){
        invalidate();
    }
}
