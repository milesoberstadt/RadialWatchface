package com.milesoberstadt.radialwatchface.activity;

import com.twotoasters.watchface.gears.activity.GearsWatchfaceActivity;
import com.twotoasters.watchface.gears.widget.IWatchface;
import com.milesoberstadt.radialwatchface.R;

public class WatchfaceActivity extends GearsWatchfaceActivity {
    @Override
    protected int getLayoutResId() {
        return R.layout.watchface;
    }

    @Override
    protected IWatchface getWatchface() {
        return (IWatchface) findViewById(R.id.watchface);
    }
}
