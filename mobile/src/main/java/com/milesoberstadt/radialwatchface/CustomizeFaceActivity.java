package com.milesoberstadt.radialwatchface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class CustomizeFaceActivity extends Activity {

    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;

    private CanvasDrawnRingView watchView;
    private TextView watchLabel;

    private Button pickFaceButton;

    private final Context context = this;

    private AlertDialog alertDialog = null;

    private String watchFaceCombo = "";
    private int ringColor1 = -1;
    private int ringColor2 = -1;
    private int ringColor3 = -1;

    //Time stuff...
    private Handler graphicsUpdateHandler = new Handler();
    private Runnable graphicsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            watchView.updateSystemTime();
            watchView.updateShapes();

            //watchView2.updateSystemTime();
            //watchView2.updateShapes();

            if (watchView.bShowMilli)
                graphicsUpdateHandler.postDelayed(this, (1000/60)); //Desired framerate is 60fps
            else
                graphicsUpdateHandler.postDelayed(this, 1000);
        }
    };

    private String TAG = "LOLTEST";

    private GoogleApiClient mGoogleApiClient = null;

    private View.OnClickListener watchClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LinearLayout linearView = null;
            CanvasDrawnRingView clickedView = null;

            if (view instanceof LinearLayout)
            {
                linearView = (LinearLayout) view;
                if (linearView.getChildAt(0) instanceof CanvasDrawnRingView) {
                    clickedView = (CanvasDrawnRingView) linearView.getChildAt(0);
                }
            }

            if (clickedView == null)
                return;

            int viewID = clickedView.getId();
            //watchFaceCombo = linearView.getResources().getResourceEntryName(viewID);
            //Log.d("LOLTEST", watchFaceCombo);

            Resources res = getResources();
            String[] colors = new String[0];
            String displayName = "";
            switch (viewID){
                case R.id.watch_rgb:
                    colors = res.getStringArray(R.array.watch_rgb_array);
                    displayName = res.getString(R.string.watch_rgb);
                    break;
                case R.id.watch_cmy:
                    colors = res.getStringArray(R.array.watch_cmy_array);
                    displayName = res.getString(R.string.watch_cmy);
                    break;
                case R.id.watch_gray:
                    colors = res.getStringArray(R.array.watch_gray_array);
                    displayName = res.getString(R.string.watch_gray);
                    break;
                case R.id.watch_crayon:
                    colors = res.getStringArray(R.array.watch_crayon_array);
                    displayName = res.getString(R.string.watch_crayon);
                    break;
                case R.id.watch_pastels:
                    colors = res.getStringArray(R.array.watch_pastels_array);
                    displayName = res.getString(R.string.watch_pastels);
                    break;
            }

            if (colors.length == 3){

                ringColor1 = Color.parseColor(colors[0]);
                ringColor2 = Color.parseColor(colors[1]);
                ringColor3 = Color.parseColor(colors[2]);

                watchView.color1 = ringColor1;
                watchView.color2 = ringColor2;
                watchView.color3 = ringColor3;

                PutDataMapRequest dataMap = PutDataMapRequest.create("/color");
                dataMap.getDataMap().putString("color1", colors[0]);
                dataMap.getDataMap().putString("color2", colors[1]);
                dataMap.getDataMap().putString("color3", colors[2]);
                PutDataRequest request = dataMap.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                        .putDataItem(mGoogleApiClient, request);

                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (dataItemResult.getStatus().isSuccess()) {
                            Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                        }
                        else
                            Log.d(TAG, "Wow, so fail: "+dataItemResult.getStatus().toString());
                    }
                });

                editor = settings.edit();

                int c1 = Color.parseColor(colors[0]);
                int c2 = Color.parseColor(colors[1]);
                int c3 = Color.parseColor(colors[2]);

                editor.putInt("ringColor1", c1);
                editor.putInt("ringColor2", c2);
                editor.putInt("ringColor3", c3);
                editor.putString("watchFaceCombo", displayName);
                editor.apply();

                watchFaceCombo = displayName;
                watchLabel.setText("Current Face: "+watchFaceCombo);
            }

            alertDialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_face);

        //Get our saved prefs
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        //Get specific prefs, with defaults
        watchFaceCombo = settings.getString("watchFaceCombo", "RGB");
        //Let's try to do resources
        Resources res = getResources();

        int tmp1 = settings.getInt("ringColor1", -1);
        int tmp2 = settings.getInt("ringColor2", -1);
        int tmp3 = settings.getInt("ringColor3", -1);

        if (tmp1 != -1)
            ringColor1 = tmp1;
        if (tmp2 != -1)
            ringColor2 = tmp2;
        if (tmp3 != -1)
            ringColor3 = tmp3;

        watchView = (CanvasDrawnRingView) findViewById(R.id.watchView);
        watchLabel = (TextView) findViewById(R.id.watchFaceText);
        watchLabel.setText("Current Face: "+watchFaceCombo);
        pickFaceButton = (Button) findViewById(R.id.changeFaceButton);

        graphicsUpdateHandler.postDelayed(graphicsUpdateRunnable, 0);

        if (ringColor1 != -1)
            watchView.color1 = ringColor1;
        if (ringColor2 != -1)
            watchView.color2 = ringColor2;
        if (ringColor3 != -1)
            watchView.color3 = ringColor3;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the data layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        pickFaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.watch_face_picker, null));
                builder.setTitle(R.string.set_face);

                alertDialog = builder.create();
                alertDialog.show();

                LinearLayout watchHolder = (LinearLayout) alertDialog.findViewById(R.id.watchFaceHolder);
                int watchCount = watchHolder.getChildCount();

                for (int i=0;i<watchCount; i++){
                    ((LinearLayout) watchHolder.getChildAt(i)).setOnClickListener(watchClicked);
                }

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.customize_face, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onStart(){
        super.onStart();

        try {
            mGoogleApiClient.connect();
        }
        catch (Exception e)
        {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }

}
