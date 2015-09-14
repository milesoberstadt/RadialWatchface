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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.chiralcode.colorpicker.ColorPickerDialog;
import com.example.radialwatchdisplay.DrawableWatchFace;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.milesoberstadt.util.IabHelper;
import com.milesoberstadt.util.IabResult;
import com.milesoberstadt.util.Inventory;
import com.milesoberstadt.util.Purchase;

import java.util.ArrayList;
import java.util.List;


public class CustomizeFaceActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {

    private CanvasDrawnRingView watchView;
    private TextView watchLabel;

    private Button pickFaceButton, swapTextStrokeButton, donateButton;
    private CircleView pickRing1, pickRing2, pickRing3,
            pickBackgroundButton, pickTextColorButton, pickTextStrokeButton;
    private SeekBar textSizeSeek, textAngleSeek, ringSizeSeek;

    private Switch textSwitch, militarySwitch, strokeSwitch, smoothSwitch, graySwitch,
            reverseOrderSwitch, showSecondsSwitch;

    private ArrayList<View> customRingViews = new ArrayList<>();

    private final Context context = this;

    private AlertDialog alertDialog = null;

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
    private IabHelper mHelper;

    private String mPeerId;
    private static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";

    private String SKU_DONATE = "radialwatch_donate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getActionBar().hide();
        }
        catch (NullPointerException e){
            Log.d(TAG, "Couldn't hide actionBar because: "+e.getMessage());
        }
        setContentView(R.layout.activity_customize_face);

        Log.d(TAG, "onCreate");

        watchView = (CanvasDrawnRingView) findViewById(R.id.watchView);

        //TODO: Migrate all setting strings to @string vals

        //Get our saved prefs
        watchView.faceDrawer.loadSettings(this);
        // Upgrade existing settings
        //watchView.faceDrawer.convertSettingsToCustom(this);

        watchLabel = (TextView) findViewById(R.id.watchFaceText);
        pickFaceButton = (Button) findViewById(R.id.changeFaceButton);
        pickRing1 = (CircleView) findViewById(R.id.bg_color_ring1);
        pickRing2 = (CircleView) findViewById(R.id.bg_color_ring2);
        pickRing3 = (CircleView) findViewById(R.id.bg_color_ring3);
        swapTextStrokeButton = (Button) findViewById(R.id.swap_color_button);
        pickTextColorButton = (CircleView) findViewById(R.id.text_color_preview);
        pickTextStrokeButton = (CircleView) findViewById(R.id.stroke_color_preview);
        pickBackgroundButton = (CircleView) findViewById(R.id.bg_color_preview);

        textSizeSeek = (SeekBar) findViewById(R.id.text_size_seek);
        textAngleSeek = (SeekBar) findViewById(R.id.text_angle_seek);
        ringSizeSeek = (SeekBar) findViewById(R.id.ring_size_seek);

        //Define switches
        textSwitch = (Switch) findViewById(R.id.text_switch);
        militarySwitch = (Switch) findViewById(R.id.military_switch);
        strokeSwitch = (Switch) findViewById(R.id.stroke_switch);
        smoothSwitch = (Switch) findViewById(R.id.smooth_switch);
        graySwitch = (Switch) findViewById(R.id.gray_switch);
        reverseOrderSwitch = (Switch) findViewById(R.id.reverse_ring_order_switch);
        showSecondsSwitch = (Switch) findViewById(R.id.show_seconds_switch);

        donateButton = (Button) findViewById(R.id.donate_button);

        graphicsUpdateHandler.postDelayed(graphicsUpdateRunnable, 0);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        // Billing lib
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr9lOZF0gMyuWeWY0WPl9lQbxcaSfBmbK6rKugnh6HhJkDo1OAM3z57stwfh79ZBDkum8bz0c4OlJQoHeP1fwKKhfHtALxKywliey7Oe2DZi/VQOGK/LpSam0+B8IAUJU3tx74/NBueLyer9xeuXtk1uc0SyVaqP9S8fSKCwpegmidp7QernYZa+GKsl53fIOeoZoUjkUmeidxux9u/D5Dl17IlXy55VK6FNwESGcywjxGvJdYVraCcH9lfWahQsYTJL46LwW/2wcziYJYBiNnPMQyRY0OCl8AjnoA9LL5/xCa6vZmPKTLBNmaovnWZl9iQ+PBDBsW2GgC7EOeG7jMwIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // uhh... issue here...
                    Log.d(TAG, "Problem setting up billing: " + result);
                }
                //Otherwise it's working!
            }
        });

        //Once we're connected, send all our previously set settings...
        sendAllSettings();

        //Call this before the event listeners, that way changes don't create a new custom ring set
        updateUIFromSettings();

        pickFaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.watch_face_picker, null));
                builder.setTitle(R.string.set_face);

                //Empty our custom rings, this gets populated every time this view opens.
                customRingViews.clear();

                alertDialog = builder.create();
                alertDialog.show();

                final LinearLayout watchHolder = (LinearLayout) alertDialog.findViewById(R.id.watchFaceHolder);

                //Get our whole 'add watch' section as a "button"
                RelativeLayout addCustomButton = (RelativeLayout) alertDialog.findViewById(R.id.addNewContainer);

                //Build our default colors...
                Resources res = getResources();
                ArrayList<Integer> colorArraysToFetch = new ArrayList<>();
                colorArraysToFetch.add(R.array.watch_rgb_array);
                colorArraysToFetch.add(R.array.watch_cmy_array);
                colorArraysToFetch.add(R.array.watch_crayon_array);
                colorArraysToFetch.add(R.array.watch_gray_array);
                colorArraysToFetch.add(R.array.watch_pastels_array);

                String[] colorNames = res.getStringArray(R.array.watch_faces);

                //Create previews for built in options...
                for (int i = 0; i < colorArraysToFetch.size(); i++) {
                    String[] colorArray = res.getStringArray(colorArraysToFetch.get(i));

                    View testLayout = LayoutInflater.from(watchHolder.getContext()).inflate(R.layout.watch_preview, null, false);
                    CanvasDrawnRingView testRing = (CanvasDrawnRingView) (((ViewGroup) testLayout).getChildAt(0));

                    testRing.faceDrawer.color1 = Color.parseColor(colorArray[0]);
                    testRing.faceDrawer.color2 = Color.parseColor(colorArray[1]);
                    testRing.faceDrawer.color3 = Color.parseColor(colorArray[2]);

                    testRing.faceDrawer.colorComboName = colorNames[i];

                    TextView titleText = (TextView) (((ViewGroup) testLayout).getChildAt(1));
                    titleText.setText(colorNames[i]);

                    watchHolder.addView(testLayout);
                }

                JsonParser parser = new JsonParser();

                //Generate previews for custom faces...
                for (int i = 0; i < watchView.faceDrawer.customRings.size(); i++) {
                    String currentCustom = watchView.faceDrawer.customRings.get(i);
                    JsonObject customFace = (JsonObject) parser.parse(currentCustom);

                    final View customRingLayout = LayoutInflater.from(watchHolder.getContext()).inflate(R.layout.watch_preview_custom, null, false);
                    CanvasDrawnRingView customRing = (CanvasDrawnRingView) (((ViewGroup) customRingLayout).getChildAt(0));

                    customRing.faceDrawer.applySettingsFromCustomRing(customFace);

                    customRing.faceDrawer.colorComboName = "Custom " + (i + 1);

                    // Update the name display
                    final TextView customText = (TextView) (((ViewGroup) customRingLayout).getChildAt(1));
                    customText.setText(customRing.faceDrawer.colorComboName);

                    ImageView deleteButton = (ImageView) ((ViewGroup) customRingLayout).getChildAt(2);

                    deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String deleteRingName = customText.getText().toString();
                            deleteCustomWatch(deleteRingName, customRingLayout, watchHolder);
                        }
                    });

                    watchHolder.addView(customRingLayout);
                    customRingViews.add(customRingLayout);
                }

                int watchCount = watchHolder.getChildCount();

                for (int i = 0; i < watchCount; i++) {
                    (watchHolder.getChildAt(i)).setOnClickListener(watchClicked);
                }

                addCustomButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Make a new custom view and add it!
                        final View customRingLayout = LayoutInflater.from(watchHolder.getContext()).inflate(R.layout.watch_preview_custom, null, false);
                        CanvasDrawnRingView customRing = (CanvasDrawnRingView) (((ViewGroup) customRingLayout).getChildAt(0));
                        //Update the view
                        copySettingsFromOneFaceToAnother(watchView.faceDrawer, customRing.faceDrawer);

                        customRing.faceDrawer.colorComboName = "Custom " + (watchView.faceDrawer.customRings.size() + 1);

                        final TextView customText = (TextView) (((ViewGroup) customRingLayout).getChildAt(1));
                        //Update the actual text view...
                        customText.setText(customRing.faceDrawer.colorComboName);
                        watchView.faceDrawer.addUpdateCustomRing(watchHolder.getContext(), watchView.faceDrawer.customRings.size());
                        sendAllSettings();

                        ImageView deleteButton = (ImageView) ((ViewGroup) customRingLayout).getChildAt(2);

                        deleteButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String deleteRingName = customText.getText().toString();
                                deleteCustomWatch(deleteRingName, customRingLayout, watchHolder);
                            }
                        });

                        customRingLayout.setOnClickListener(watchClicked);
                        watchHolder.addView(customRingLayout);
                        customRingViews.add(customRingLayout);
                    }
                });

            }
        });

        pickRing1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(1);
            }
        });

        pickRing2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(2);
            }
        });

        pickRing3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(3);
            }
        });

        pickTextColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextColorPicker();
            }
        });

        pickTextStrokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTextStrokePicker();
            }
        });

        swapTextStrokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int oldText = watchView.faceDrawer.textColor;
                int oldStroke = watchView.faceDrawer.textStrokeColor;

                watchView.faceDrawer.textStrokeColor = oldText;
                watchView.faceDrawer.textColor = oldStroke;

                pickTextColorButton.circleFillColor = watchView.faceDrawer.textColor;
                pickTextColorButton.invalidate();
                pickTextStrokeButton.circleFillColor = watchView.faceDrawer.textStrokeColor;
                pickTextStrokeButton.invalidate();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        pickBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBGColorPicker();
            }
        });

        textSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "Text Size: " + progress);
                watchView.faceDrawer.textSizePercent = progress;

                saveCustomWatchFace();
                updateUIFromSettings();

                sendAllSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        textAngleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float calcDegrees = (progress/100.f) * 360;
                calcDegrees = calcDegrees % 360;

                watchView.faceDrawer.textAngle = calcDegrees;

                saveCustomWatchFace();

                sendAllSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ringSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "Ring Size: " + progress);
                watchView.faceDrawer.ringSizePercent = progress;

                saveCustomWatchFace();
                updateUIFromSettings();

                sendAllSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        textSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bTextEnabled = isChecked;

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        militarySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.b24HourTime = militarySwitch.isChecked();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        strokeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bTextStroke = strokeSwitch.isChecked();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        smoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bShowMilli = smoothSwitch.isChecked();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        graySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bGrayAmbient = graySwitch.isChecked();

                saveCustomWatchFace();
                sendAllSettings();
            }
        });

        reverseOrderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bReverseRingOrder = reverseOrderSwitch.isChecked();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        showSecondsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                watchView.faceDrawer.bShowSeconds = showSecondsSwitch.isChecked();

                saveCustomWatchFace();

                sendAllSettings();
            }
        });

        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the info about donating...
                List productSKUs = new ArrayList();
                productSKUs.add(SKU_DONATE);
                mHelper.queryInventoryAsync(true, productSKUs, mQueryFinishedListener);


            }

            IabHelper.QueryInventoryFinishedListener mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                    if (result.isFailure()){
                        // Whoops.
                        Log.d(TAG, "Failed to get inventory");
                    }

                    String donatePrice = inv.getSkuDetails(SKU_DONATE).getPrice();
                    Log.d(TAG, "Got donate price: "+donatePrice);

                    mHelper.launchPurchaseFlow((Activity) getApplicationContext(), SKU_DONATE, 10001, mPurchaseFinishedListener, "");
                }
            };

            IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        Log.d(TAG, "Error purchasing: " + result);
                        return;
                    }
                    else if (info.getSku().equals(SKU_DONATE)) {
                        // They bought it, show a thank you toast!
                    }
                }
            };
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
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }

    private void sendAllSettings(){

        //This pretty much just always needs to be done...
        watchView.faceDrawer.saveSettings(getApplicationContext());

        String col1 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color1));
        String col2 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color2));
        String col3 = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.color3));
        String bg = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.backgroundColor));
        String textCol = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.textColor));
        String textSCol = String.format("#%06X", (0xFFFFFF & watchView.faceDrawer.textStrokeColor));

        PutDataMapRequest dataMap = PutDataMapRequest.create("/radialSettings");
        dataMap.getDataMap().putString("color1", col1);
        dataMap.getDataMap().putString("color2", col2);
        dataMap.getDataMap().putString("color3", col3);
        dataMap.getDataMap().putString("bg", bg);
        dataMap.getDataMap().putString("textColor", textCol);
        dataMap.getDataMap().putString("textStrokeColor", textSCol);

        dataMap.getDataMap().putBoolean("enableText", watchView.faceDrawer.bTextEnabled);
        dataMap.getDataMap().putBoolean("24hourtime", watchView.faceDrawer.b24HourTime);
        dataMap.getDataMap().putBoolean("strokeText", watchView.faceDrawer.bTextStroke);
        dataMap.getDataMap().putBoolean("smoothAnim", watchView.faceDrawer.bShowMilli);
        dataMap.getDataMap().putBoolean("grayAmbient", watchView.faceDrawer.bGrayAmbient);
        dataMap.getDataMap().putBoolean("reverseRingOrder", watchView.faceDrawer.bReverseRingOrder);
        dataMap.getDataMap().putBoolean("showSeconds", watchView.faceDrawer.bShowSeconds);
        dataMap.getDataMap().putInt("ringSizePercent", watchView.faceDrawer.ringSizePercent);
        dataMap.getDataMap().putInt("textSizePercent", watchView.faceDrawer.textSizePercent);
        dataMap.getDataMap().putFloat("textAngle", watchView.faceDrawer.textAngle);

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
    }

    // TODO: Make this actually happen again
    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private View.OnClickListener watchClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LinearLayout linearView;
            CanvasDrawnRingView clickedView = null;

            if (view instanceof LinearLayout)
            {
                linearView = (LinearLayout) view;
                if (linearView.getChildAt(0) instanceof CanvasDrawnRingView) {
                    clickedView = (CanvasDrawnRingView) linearView.getChildAt(0);
                }
            }

            if (clickedView == null) {
                return;
            }

            copySettingsFromOneFaceToAnother(clickedView.faceDrawer, watchView.faceDrawer);

            updateUIFromSettings();

            sendAllSettings();

            watchLabel.setText("Current Face: "+watchView.faceDrawer.colorComboName);

            alertDialog.dismiss();
        }
    };

    private void deleteCustomWatch(String customRingName, View viewToDelete, LinearLayout layoutToDeleteFrom){
        // Before we delete this, we need to remove it from our data...
        int deleteIndex = Integer.parseInt(customRingName.split("Custom ")[1]);
        watchView.faceDrawer.customRings.remove(deleteIndex-1);

        //Update the display of all the custom rings to show correct numbers...
        customRingViews.remove(deleteIndex-1);
        for(int i=0; i<customRingViews.size(); i++){
            TextView currentRingText =  (TextView) (((ViewGroup) customRingViews.get(i)).getChildAt(1));
            currentRingText.setText("Custom "+(i+1));
        }

        sendAllSettings();

        if (!watchView.faceDrawer.colorComboName.contains("Custom ")){
            //If we aren't using a custom face...do nothing lol
        }
        //If we just deleted the last custom ring, select RGB by default.
        else if (watchView.faceDrawer.customRings.isEmpty()) {
            watchView.faceDrawer.colorComboName = "RGB";
            watchView.faceDrawer.resetDefaultStyle();
            updateUIFromSettings();
        }
        else{
            // Since we just deleted one, we need to temporarily select something else
            // Otherwise our user could hit back and have an invalid face selected...
            // Select the next ring, or the last one, whatever's safest.
            int customIndex = Math.min(deleteIndex, watchView.faceDrawer.customRings.size()-1);
            watchView.faceDrawer.colorComboName = "Custom " + (customIndex + 1);

            //Reject this if it's out of bounds
            if (customIndex >= watchView.faceDrawer.customRings.size())
                return;

            JsonParser parser = new JsonParser();
            JsonObject customFace = (JsonObject) parser.parse(watchView.faceDrawer.customRings.get(customIndex));
            watchView.faceDrawer.applySettingsFromCustomRing(customFace);
            updateUIFromSettings();
        }

        layoutToDeleteFrom.removeView(viewToDelete);
    }

    public void updateUIFromSettings(){
        watchLabel.setText("Current Face: "+watchView.faceDrawer.colorComboName);

        pickBackgroundButton.circleFillColor = watchView.faceDrawer.backgroundColor;
        pickBackgroundButton.invalidate();

        pickRing1.circleFillColor = watchView.faceDrawer.color1;
        pickRing1.invalidate();
        pickRing2.circleFillColor = watchView.faceDrawer.color2;
        pickRing2.invalidate();
        pickRing3.circleFillColor = watchView.faceDrawer.color3;
        pickRing3.invalidate();

        pickTextColorButton.circleFillColor = watchView.faceDrawer.textColor;
        pickTextColorButton.invalidate();
        pickTextStrokeButton.circleFillColor = watchView.faceDrawer.textStrokeColor;
        pickTextStrokeButton.invalidate();

        textSwitch.setChecked(watchView.faceDrawer.bTextEnabled);
        strokeSwitch.setChecked(watchView.faceDrawer.bTextStroke);
        militarySwitch.setChecked(watchView.faceDrawer.b24HourTime);
        smoothSwitch.setChecked(watchView.faceDrawer.bShowMilli);
        graySwitch.setChecked(watchView.faceDrawer.bGrayAmbient);
        reverseOrderSwitch.setChecked(watchView.faceDrawer.bReverseRingOrder);
        showSecondsSwitch.setChecked(watchView.faceDrawer.bShowSeconds);

        textSizeSeek.setProgress(watchView.faceDrawer.textSizePercent);
        ringSizeSeek.setProgress(watchView.faceDrawer.ringSizePercent);

        //Convert our text angle back to a percent
        int calcPercent = (int) Math.floor((watchView.faceDrawer.textAngle/360.f) * 100.f);
        textAngleSeek.setProgress(calcPercent);

        if (textSwitch.isChecked()){
            textSwitch.setEnabled(true);
            militarySwitch.setEnabled(true);
            strokeSwitch.setEnabled(true);
        }
        else{
            militarySwitch.setEnabled(false);
            strokeSwitch.setEnabled(false);
        }
    }

    private void showBGColorPicker(){
        int initialColor = watchView.faceDrawer.backgroundColor;
        String title = getString(R.string.text_color);

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                Log.d(TAG, "BG color selected: "+String.valueOf(color));
                watchView.faceDrawer.backgroundColor = color;

                saveCustomWatchFace();
                updateUIFromSettings();

                sendAllSettings();
            }
        });

        colorPickerDialog.setTitle(title);
        colorPickerDialog.show();
    }

    private void showTextColorPicker(){
        int initialColor = watchView.faceDrawer.textColor;
        String title = getString(R.string.text_color);

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                Log.d(TAG, "Text color selected: "+String.valueOf(color));
                watchView.faceDrawer.textColor = color;

                saveCustomWatchFace();
                updateUIFromSettings();

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

                saveCustomWatchFace();
                updateUIFromSettings();

                sendAllSettings();
            }
        });

        colorPickerDialog.setTitle(title);
        colorPickerDialog.show();
    }

    private void showColorPicker(final int customChooserPosition){

        int initialColor = 0xFF000000;
        String title = "";

        if (customChooserPosition == 1){
            initialColor = watchView.faceDrawer.color1;
            title = "Choose Hour Color...";
        }
        else if (customChooserPosition == 2){
            initialColor = watchView.faceDrawer.color2;
            title = "Choose Minute Color...";
        }
        else if (customChooserPosition == 3){
            initialColor = watchView.faceDrawer.color3;
            title = "Choose Second Color...";
        }

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {

            @Override
            public void onColorSelected(int color) {

                   if (customChooserPosition == 1){
                       watchView.faceDrawer.color1 = color;
                       pickRing1.circleFillColor = watchView.faceDrawer.color1;
                       pickRing1.invalidate();
                    }
                    else if (customChooserPosition == 2){
                       watchView.faceDrawer.color2 = color;
                       pickRing2.circleFillColor = watchView.faceDrawer.color2;
                       pickRing2.invalidate();
                    }
                    else if (customChooserPosition == 3){
                       watchView.faceDrawer.color3 = color;
                       pickRing3.circleFillColor = watchView.faceDrawer.color3;
                       pickRing3.invalidate();
                    }

                    saveCustomWatchFace();

                    sendAllSettings();

                    watchLabel.setText("Current Face: " + watchView.faceDrawer.colorComboName);
            }
        });

        colorPickerDialog.setTitle(title);

        colorPickerDialog.show();
    }

    public void copySettingsFromOneFaceToAnother(DrawableWatchFace originFace, DrawableWatchFace destinationFace){
        // Duh, just get colors from the rings...
        int color1 = originFace.color1;
        int color2 = originFace.color2;
        int color3 = originFace.color3;

        destinationFace.color1 = color1;
        destinationFace.color2 = color2;
        destinationFace.color3 = color3;

        destinationFace.backgroundColor = originFace.backgroundColor;

        destinationFace.textColor = originFace.textColor;
        destinationFace.textStrokeColor = originFace.textStrokeColor;

        destinationFace.bTextEnabled = originFace.bTextEnabled;
        destinationFace.bTextStroke = originFace.bTextStroke;
        destinationFace.b24HourTime = originFace.b24HourTime;
        destinationFace.bShowMilli = originFace.bShowMilli;
        destinationFace.bGrayAmbient = originFace.bGrayAmbient;

        destinationFace.textSizePercent = originFace.textSizePercent;
        destinationFace.ringSizePercent = originFace.ringSizePercent;

        destinationFace.colorComboName = originFace.colorComboName;
    }

    /**
     * This function is for making a new watch face if you're editing settings while on a "built in"
     * watch face. If you aren't it'll save settings to the selected custom watch face.
     */
    public void saveCustomWatchFace(){
        // If we're editing a built in one, make a new face.
        if (!watchView.faceDrawer.colorComboName.contains("Custom ")){
            watchView.faceDrawer.colorComboName = "Custom "+(watchView.faceDrawer.customRings.size()+1);
            watchView.faceDrawer.addUpdateCustomRing(this, watchView.faceDrawer.customRings.size());
        }
        // Otherwise, update existing
        else{
            int customIndex = Integer.parseInt(watchView.faceDrawer.colorComboName.split("Custom ")[1])-1;
            watchView.faceDrawer.addUpdateCustomRing(this, customIndex);
        }
    }


}