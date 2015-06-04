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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.chiralcode.colorpicker.ColorPickerDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


public class CustomizeFaceActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {

    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;

    private CanvasDrawnRingView watchView;
    private TextView watchLabel;

    private Button pickFaceButton;
    private Button pickCustomButton;

    private Switch textSwitch, militarySwitch, invertSwitch, strokeSwitch, smoothSwitch, graySwitch;


    private final Context context = this;

    private AlertDialog alertDialog = null;

    //Settings get stored to these
    private String watchFaceCombo = "";
    private int ringColor1 = -1, ringColor2 = -1, ringColor3 = -1;
    private boolean bTextEnabled = true, b24HourTime = false, bInvertText = false, bTextStroke = false, bSmoothAnimations = false, bGrayAmbient = false;

    //This is for tracking our dialog position when setting up a custom watch face...
    private int customChooserPosition = 0;
    //Custom colors...
    private int[] customColors = new int[3];

    //Time stuff...
    private Handler graphicsUpdateHandler = new Handler();
    private Runnable graphicsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            watchView.updateSystemTime();
            watchView.updateShapes();

            if (watchView.bShowMilli)
                graphicsUpdateHandler.postDelayed(this, (1000/60)); //Desired framerate is 60fps
            else
                graphicsUpdateHandler.postDelayed(this, 1000);
        }
    };

    private String TAG = "LOLTEST";

    private GoogleApiClient mGoogleApiClient = null;
    private String mPeerId;
    private static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";

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

                commitColors(colors[0],colors[1],colors[2], displayName);

                watchFaceCombo = displayName;
                watchLabel.setText("Current Face: "+watchFaceCombo);
            }

            alertDialog.dismiss();
        }
    };

    private void commitColors(String col1, String col2, String col3, String displayName){

        ringColor1 = Color.parseColor(col1);
        ringColor2 = Color.parseColor(col2);
        ringColor3 = Color.parseColor(col3);

        watchView.color1 = ringColor1;
        watchView.color2 = ringColor2;
        watchView.color3 = ringColor3;
        //Disable this feature until it's ready for primetime
        watchView.bShowMilli = false;

        PutDataMapRequest dataMap = PutDataMapRequest.create("/color");
        dataMap.getDataMap().putString("color1", col1);
        dataMap.getDataMap().putString("color2", col2);
        dataMap.getDataMap().putString("color3", col3);
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

        int c1 = Color.parseColor(col1);
        int c2 = Color.parseColor(col2);
        int c3 = Color.parseColor(col3);

        editor.putInt("ringColor1", c1);
        editor.putInt("ringColor2", c2);
        editor.putInt("ringColor3", c3);
        editor.putString("watchFaceCombo", displayName);
        editor.apply();
    }

    private void syncBoolean(String path, String boolName, boolean value){
        PutDataMapRequest dataMap = PutDataMapRequest.create(path);
        dataMap.getDataMap().putBoolean(boolName, value);
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
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_face);

        Log.d(TAG, "onCreate");

        //TODO: Migrate all setting strings to @string vals

        //Get our saved prefs
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        //Get specific prefs, with defaults
        watchFaceCombo = settings.getString("watchFaceCombo", "RGB");
        //Let's try to do resources
        Resources res = getResources();

        //If we don't have settings, set to RGB
        int tmp1 = settings.getInt("ringColor1", 0xFFe51c23);
        int tmp2 = settings.getInt("ringColor2", 0xFF8bc34a);
        int tmp3 = settings.getInt("ringColor3", 0xFF03a9f4);

        if (tmp1 != -1)
            ringColor1 = tmp1;
        if (tmp2 != -1)
            ringColor2 = tmp2;
        if (tmp3 != -1)
            ringColor3 = tmp3;

        //Get text options
        bTextEnabled = settings.getBoolean("enableText", true);
        b24HourTime = settings.getBoolean("24hourtime", false);
        bInvertText = settings.getBoolean("invertText", false);
        bTextStroke = settings.getBoolean("strokeText", false);
        bSmoothAnimations = settings.getBoolean("smoothAnim", false);
        bGrayAmbient = settings.getBoolean("grayAmbient", false);

        watchView = (CanvasDrawnRingView) findViewById(R.id.watchView);
        watchLabel = (TextView) findViewById(R.id.watchFaceText);
        watchLabel.setText("Current Face: "+watchFaceCombo);
        pickFaceButton = (Button) findViewById(R.id.changeFaceButton);
        pickCustomButton = (Button) findViewById(R.id.changeCustomFace);

        //Define switches
        textSwitch = (Switch) findViewById(R.id.text_switch);
        militarySwitch = (Switch) findViewById(R.id.military_switch);
        invertSwitch = (Switch) findViewById(R.id.invert_switch);
        strokeSwitch = (Switch) findViewById(R.id.stroke_switch);
        smoothSwitch = (Switch) findViewById(R.id.smooth_switch);
        graySwitch = (Switch) findViewById(R.id.gray_switch);

        graphicsUpdateHandler.postDelayed(graphicsUpdateRunnable, 0);

        //If saved colors exist, update watch preview
        if (ringColor1 != -1)
            watchView.color1 = ringColor1;
        if (ringColor2 != -1)
            watchView.color2 = ringColor2;
        if (ringColor3 != -1)
            watchView.color3 = ringColor3;
        //Update text options too...
        watchView.bShowText = bTextEnabled;
        watchView.bInvertText = bInvertText;
        watchView.bStrokeText = bTextStroke;
        watchView.bShowMilli = bSmoothAnimations;
        watchView.b24HourTime = b24HourTime;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        //Once we're connected, send all our previously set settings...
        sendAllSettings();

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

        pickCustomButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showColorPicker();
            }

        });

        textSwitch.setChecked(bTextEnabled);
        militarySwitch.setChecked(b24HourTime);
        invertSwitch.setChecked(bInvertText);
        strokeSwitch.setChecked(bTextStroke);
        smoothSwitch.setChecked(bSmoothAnimations);
        graySwitch.setChecked(bGrayAmbient);

        if (textSwitch.isChecked()){
            invertSwitch.setEnabled(true);
            strokeSwitch.setEnabled(true);
        }
        else{
            invertSwitch.setEnabled(false);
            strokeSwitch.setEnabled(false);
        }

        textSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("enableText", textSwitch.isChecked());
                editor.apply();

                watchView.bShowText = bTextStroke = textSwitch.isChecked();

                if (textSwitch.isChecked())
                {
                    invertSwitch.setEnabled(true);
                    strokeSwitch.setEnabled(true);
                }

                else{
                    invertSwitch.setEnabled(false);
                    strokeSwitch.setEnabled(false);
                }

                //syncBoolean("/text", "enableText", textSwitch.isChecked());
                sendAllSettings();
            }
        });

        militarySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("24hourtime", militarySwitch.isChecked());
                editor.apply();

                watchView.b24HourTime = b24HourTime = militarySwitch.isChecked();

                //syncBoolean("/text", "24hourtime", militarySwitch.isChecked());
                sendAllSettings();
            }
        });

        invertSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("invertText", invertSwitch.isChecked());
                editor.apply();

                watchView.bInvertText = bInvertText = invertSwitch.isChecked();

                //syncBoolean("/text", "invertText", invertSwitch.isChecked());
                sendAllSettings();
            }
        });

        strokeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("strokeText", strokeSwitch.isChecked());
                editor.apply();

                watchView.bStrokeText = bTextStroke = strokeSwitch.isChecked();

                //syncBoolean("/text", "strokeText", strokeSwitch.isChecked());
                sendAllSettings();
            }
        });

        smoothSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("smoothAnim", smoothSwitch.isChecked());
                editor.apply();

                watchView.bShowMilli = bSmoothAnimations = smoothSwitch.isChecked();

                //syncBoolean("/anim", "smoothAnim", smoothSwitch.isChecked());
                sendAllSettings();
            }
        });

        graySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("grayAmbient", graySwitch.isChecked());
                editor.apply();

                //syncBoolean("/color", "grayAmbient", graySwitch.isChecked());
                sendAllSettings();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }
    }

    @Override // ResultCallback<DataApi.DataItemResult>
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();

        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.

        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private void sendAllSettings(){

        String col1 = String.format("#%06X", (0xFFFFFF & ringColor1));
        String col2 = String.format("#%06X", (0xFFFFFF & ringColor2));
        String col3 = String.format("#%06X", (0xFFFFFF & ringColor3));

        PutDataMapRequest dataMap = PutDataMapRequest.create("/color");
        dataMap.getDataMap().putString("color1", col1);
        dataMap.getDataMap().putString("color2", col2);
        dataMap.getDataMap().putString("color3", col3);
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

        PutDataMapRequest dataMap2 = PutDataMapRequest.create("/text");
        dataMap2.getDataMap().putBoolean("enableText", bTextEnabled);
        dataMap2.getDataMap().putBoolean("24hourtime", b24HourTime);
        dataMap2.getDataMap().putBoolean("invertText", bInvertText);
        dataMap2.getDataMap().putBoolean("strokeText", bTextStroke);
        dataMap2.getDataMap().putBoolean("smoothAnim", bSmoothAnimations);
        dataMap2.getDataMap().putBoolean("grayAmbient", bGrayAmbient);

        PutDataRequest request2 = dataMap2.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult2 = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request2);

        pendingResult2.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                }
                else
                    Log.d(TAG, "Wow, so fail: "+dataItemResult.getStatus().toString());
            }
        });

    }

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showColorPicker(){

        int initialColor = 0xFF000000;
        String title = "";

        if (customChooserPosition == 0){
            initialColor = ringColor1;
            title = "Choose Hour Color...";
        }
        else if (customChooserPosition == 1){
            initialColor = ringColor2;
            title = "Choose Minute Color...";
        }
        else if (customChooserPosition == 2){
            initialColor = ringColor3;
            title = "Choose Second Color...";
        }

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {

            @Override
            public void onColorSelected(int color) {
                if (customChooserPosition < 2){
                    customColors[customChooserPosition] = color;
                    customChooserPosition++;
                    //if (colorPickerDialog)
                    //colorPickerDialog.dismiss();
                    showColorPicker();
                }
                else if (customChooserPosition == 2){
                    customColors[customChooserPosition] = color;

                    String col1 = String.format("#%06X", (0xFFFFFF & customColors[0]));
                    String col2 = String.format("#%06X", (0xFFFFFF & customColors[1]));
                    String col3 = String.format("#%06X", (0xFFFFFF & customColors[2]));

                    commitColors(col1, col2, col3, "Custom");

                    watchFaceCombo = "Custom";
                    watchLabel.setText("Current Face: "+watchFaceCombo);

                    customChooserPosition = 0;
                    //colorPickerDialog.dismiss();
                }
            }

        });


        colorPickerDialog.setTitle(title);

        Resources res = getResources();

        if (customChooserPosition > 0){
            colorPickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(R.string.previous), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    customChooserPosition--;
                    colorPickerDialog.dismiss();
                    showColorPicker();
                }
            });
        }

        /*if (customChooserPosition < 2){
            colorPickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(R.string.next), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    customColors[customChooserPosition] = colorPickerDialog.hashCode();
                    customChooserPosition++;
                    colorPickerDialog.dismiss();
                    showColorPicker();
                }
            });
        }

        if (customChooserPosition == 2){
            colorPickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(R.string.done), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    customColors[customChooserPosition] = colorPickerDialog.hashCode();

                    String col1 = String.format("#%06X", (0xFFFFFF & customColors[0]));
                    col1 = String.format("#%08X", customColors[0]);
                    String col2 = String.format("#%06X", (0xFFFFFF & customColors[1]));
                    String col3 = String.format("#%06X", (0xFFFFFF & customColors[2]));

                    commitColors(col1, col2, col3, "Custom");

                    watchFaceCombo = "Custom";
                    watchLabel.setText("Current Face: "+watchFaceCombo);

                    customChooserPosition = 0;
                    colorPickerDialog.dismiss();
                }
            });
        }*/

        colorPickerDialog.show();


        /*AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.watch_face_picker, null));
        builder.setTitle(R.string.set_face);

        alertDialog = builder.create();
        alertDialog.show();

        LinearLayout watchHolder = (LinearLayout) alertDialog.findViewById(R.id.watchFaceHolder);
        int watchCount = watchHolder.getChildCount();

        for (int i=0;i<watchCount; i++){
            ((LinearLayout) watchHolder.getChildAt(i)).setOnClickListener(watchClicked);
        }*/
    }


}
