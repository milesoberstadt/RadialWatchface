package com.milesoberstadt.radialwatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Miles on 10/13/2014.
 */
public class WatchDrawableView extends View {

    private ShapeDrawable mDrawable;

    private int fixedWidth = 320;
    private int fixedHeight = 320;

    public WatchDrawableView(Context context, AttributeSet attrs){
        super(context, attrs);

        int x = 10;
        int y = 10;
        int width = 300;
        int height = 50;

        mDrawable = new ShapeDrawable(new OvalShape());
        mDrawable.getPaint().setColor(0xff74ac23);
        mDrawable.setBounds(x, y, x+width, y+height);
    }

    protected void onDraw(Canvas canvas){
        mDrawable.draw(canvas);
    }



}
