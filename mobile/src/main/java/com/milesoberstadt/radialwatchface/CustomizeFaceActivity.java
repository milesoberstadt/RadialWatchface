package com.milesoberstadt.radialwatchface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

    private CanvasDrawnRingView watchView;
    private TextView watchLabel;

    private Button pickFaceButton, pickCustomButton, pickTextColorButton, pickTextStrokeButton;


    private Switch textSwitch, militarySwitch, strokeSwitch, smoothSwitch, graySwitch;


    private final Context context = this;

    private AlertDialog alertDialog = null;

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

            if (watchView.faceDrawer.bShowMilli)
                graphicsUpdateHandler.postDelayed(this, (1000/60)); //Desired framerate is 60fps
            else
                graphicsUpdateHandler.postDelayed(this, 1000);
        }
    };

    private String TAG = "LOLTEST";

    private GoogleApiClient mGoogleApiClient = null;
    private String mPeerId;
    private static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";


    // Depreciated: Use sendAllSettings()
    /*private void commitColors(String col1, String col2, String col3, String displayName){

        watchView.faceDrawer.color1 = Color.parseColor(col1);
        watchView.faceDrawer.color2 = Color.parseColor(col2);
        watchView.faceDrawer.color3 = Color.parseColor(col3);
        watchView.faceDrawer.colorComboName = displayName;
        //Disable this feature until it's ready for primetime
        //watchView.faceDrawer.bShowMilli = false;

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


        watchView.faceDrawer.saveSettings(getApplicationContext());
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_face);

        Log.d(TAG, "onCreate");

        watchView = (CanvasDrawnRingView) findViewById(R.id.watchView);

        //TODO: Migrate all setting strings to @string vals

        //Get our saved prefs
        watchView.faceDrawer.loadSettings(this);

        watchLabel = (TextView) findViewById(R.id.watchFaceText);
        watchLabel.setText("Current Face: "+watchView.faceDrawer.colorComboName);
        pickFaceButton = (Button) findViewById(R.id.changeFaceButton);
        pickCustomButton = (Button) findViewById(R.id.changeCustomFace);
        pickTextColorButton = (Button) findViewById(R.id.set_text_color);
        pickTextStrokeButton = (Button) findViewById(R.id.set_text_stroke_color);

        //Define switches
        textSwitch = (Switch) findViewById(R.id.text_switch);
        militarySwitch = (Switch) findViewById(R.id.military_switch);
        strokeSwitch = (Switch) findViewById(R.id.stroke_switch);
        smoothSwitch = (Switch) findViewById(R.id.smooth_switch);
        graySwitch = (Switch) findViewById(R.id.gray_switch);

        graphicsUpdateHandler.postDelayed(graphicsUpdateRunnable, 0);

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

                for (int i = 0; i < watchCount; i++) {
                    (watchHolder.getChildAt(i)).setOnClickListener(watchClicked);
                }

            }
        });

        pickCustomButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showColorPicker();
            }

        });

        pickTextColorButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showTextColorPicker();
            }
        });

        pickTextStrokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTextStrokePicker();
            }
        });

        textSwitch.setChecked(watchView.faceDrawer.bTextEnabled);
        militarySwitch.setChecked(watchView.faceDrawer.b24HourTime);
        strokeSwitch.setChecked(watchView.faceDrawer.bTextStroke);
        smoothSwitch.setChecked(watchView.faceDrawer.bShowMilli);
        graySwitch.setChecked(watchView.faceDrawer.bGrayAmbient);

        if (textSwitch.isChecked()){
            textSwitch.setEnabled(true);
            militarySwitch.setEnabled(true);
            strokeSwitch.setEnabled(true);
        }
        else{
            militarySwitch.setEnabled(false);
            strokeSwitch.setEnabled(false);
        }

        textSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                watchView.faceDrawer.bTextEnabled = textSwitch.isChecked();

                if (textSwitch.isChecked())
                {
                    textSwitch.setEnabled(true);
                    militarySwitch.setEnabled(true);
                    strokeSwitch.setEnabled(true);
                }

                else{
                    militarySwitch.setEnabled(false);
                    militarySwitch.setChecked(false);
                    strokeSwitch.setEnabled(false);
                    strokeSwitch.setChecked(false);
                }

                sendAllSettings();
            }
        });

        militarySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watchView.faceDrawer.b24HourTime = militarySwitch.isChecked();

                sendAllSettings();
            }
        });

        strokeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watchView.faceDrawer.bTextStroke = strokeSwitch.isChecked();

                sendAllSettings();
            }
        });

        smoothSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                watchView.faceDrawer.bShowMilli = smoothSwitch.isChecked();

                sendAllSettings();
            }
        });

        graySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Nothing to display (phone can't be ambient) just send changes...
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

    // TODO: Delete this method if we really don't use it...
    @Override // ResultCallback<DataApi.DataItemResult>
    public void onResult(DataApi.DataItemResult dataItemResult) {
        /*if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();

        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.

        }*/
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

        //This pretty much just always needs to be done...
        watchView.faceDrawer.saveSettings(getApplicationContext());

        String col1 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color1));
        String col2 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color2));
        String col3 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color3));
        String textCol = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.textColor));
        String textSCol = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.textStrokeColor));

        PutDataMapRequest dataMap = PutDataMapRequest.create("/color");
        dataMap.getDataMap().putString("color1", col1);
        dataMap.getDataMap().putString("color2", col2);
        dataMap.getDataMap().putString("color3", col3);
        dataMap.getDataMap().putString("textColor", textCol);
        dataMap.getDataMap().putString("textStrokeColor", textSCol);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                } else
                    Log.d(TAG, "Wow, so fail: " + dataItemResult.getStatus().toString());
            }
        });

        PutDataMapRequest dataMap2 = PutDataMapRequest.create("/text");
        dataMap2.getDataMap().putBoolean("enableText", watchView.faceDrawer.bTextEnabled);
        dataMap2.getDataMap().putBoolean("24hourtime", watchView.faceDrawer.b24HourTime);
        dataMap2.getDataMap().putBoolean("strokeText", watchView.faceDrawer.bTextStroke);
        dataMap2.getDataMap().putBoolean("smoothAnim", watchView.faceDrawer.bShowMilli);
        dataMap2.getDataMap().putBoolean("grayAmbient", watchView.faceDrawer.bGrayAmbient);

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

    // TODO: Make this actually happen again
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

                //commitColors(colors[0],colors[1],colors[2], displayName);
                watchView.faceDrawer.color1 = Color.parseColor(colors[0]);
                watchView.faceDrawer.color2 = Color.parseColor(colors[1]);
                watchView.faceDrawer.color3 = Color.parseColor(colors[2]);
                watchView.faceDrawer.colorComboName = displayName;

                sendAllSettings();

                watchLabel.setText("Current Face: "+watchView.faceDrawer.colorComboName);
            }

            alertDialog.dismiss();
        }
    };

    private void showTextColorPicker(){
        int initialColor = watchView.faceDrawer.textColor;
        String title = getString(R.string.text_color);

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                Log.d(TAG, "Text color selected: "+String.valueOf(color));
                watchView.faceDrawer.textColor = color;
                watchView.faceDrawer.saveSettings(getApplicationContext());
                sendAllSettings();
            }
        });

        colorPickerDialog.setTitle(title);
        colorPickerDialog.show();
    }

    private void showTextStrokePicker(){
        int initialColor = watchView.faceDrawer.textStrokeColor;
        String title = getString(R.string.text_stroke_color);

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                Log.d(TAG, "Text stroke selected: "+String.valueOf(color));
                watchView.faceDrawer.textStrokeColor = color;
                watchView.faceDrawer.saveSettings(getApplicationContext());
                sendAllSettings();
            }
        });

        colorPickerDialog.setTitle(title);
        colorPickerDialog.show();
    }

    private void showColorPicker(){

        int initialColor = 0xFF000000;
        String title = "";

        if (customChooserPosition == 0){
            initialColor = watchView.faceDrawer.color1;
            title = "Choose Hour Color...";
        }
        else if (customChooserPosition == 1){
            initialColor = watchView.faceDrawer.color2;
            title = "Choose Minute Color...";
        }
        else if (customChooserPosition == 2){
            initialColor = watchView.faceDrawer.color3;
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

                    /*String col1 = String.format("#%06X", (0xFFFFFF & customColors[0]));
                    String col2 = String.format("#%06X", (0xFFFFFF & customColors[1]));
                    String col3 = String.format("#%06X", (0xFFFFFF & customColors[2]));*/

                    watchView.faceDrawer.color1 = customColors[0];
                    watchView.faceDrawer.color2 = customColors[1];
                    watchView.faceDrawer.color3 = customColors[2];

                    watchView.faceDrawer.colorComboName= "Custom";
                    //commitColors(col1, col2, col3, "Custom");
                    sendAllSettings();

                    watchLabel.setText("Current Face: "+watchView.faceDrawer.colorComboName);

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
