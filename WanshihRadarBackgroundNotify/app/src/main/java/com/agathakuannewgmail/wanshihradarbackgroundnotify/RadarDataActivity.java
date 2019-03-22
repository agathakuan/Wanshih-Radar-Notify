package com.agathakuannewgmail.wanshihradarbackgroundnotify;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class RadarDataActivity extends Activity {
    private final static String TAG = "Main2Activity";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mPrintfText;
    private String mPrintData;
    private Button mClearPrintf;

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Button mConnect;
    private Button mDisconnect;
    private Button mNotifyRadarData;

    private double mSpeedNum;

    //seekbar example
    //https://tekeye.uk/android/examples/ui/seekbar-android-example
    private TextView mProgressText;
    private SeekBar seekbar;
    private int seekbar_progress;
    private TextView mSpeedField;
    private TextView mDirectionField;


    //scroll graphicView
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;

    private NotificationService.outputBinder outputBinder;
    private ServiceConnection notificationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            outputBinder = (NotificationService.outputBinder) iBinder;
            outputBinder.startOutput();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            outputBinder = null;
        }
    };


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radardata);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        mPrintfText = (TextView)findViewById(R.id.info_channel);
        mClearPrintf = (Button)findViewById(R.id.clear_print);
        mClearPrintf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printfClear();
            }
        });

        mConnect = (Button)findViewById(R.id.connect_bt);
        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.connect(mDeviceAddress);
                mDisconnect.setClickable(true);
                mConnect.setClickable(false);
            }
        });
        mDisconnect = (Button)findViewById(R.id.disconnect_bt);
        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.disconnect();
                mConnect.setClickable(true);
                mDisconnect.setClickable(false);
            }
        });

        mNotifyRadarData = (Button)findViewById(R.id.notify_radar_data);
        mNotifyRadarData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printf("press Notify button");
                if (mGattCharacteristics != null)
                {
                    //Alex:第三service 的第0 characteristic　這個日後要再優化
                    final BluetoothGattCharacteristic characteristic=
                            mGattCharacteristics.get(3).get(0);

                    final int charaProp = characteristic.getProperties();

                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
                    {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(characteristic);

                        printf("PROPERTY_READ > 0");
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                    {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);

                        printf("PROPERTY_NOTIFY > 0");
                    }
                }
            }
        });

        mSpeedField = (TextView) findViewById(R.id.speed);
        mDirectionField = (TextView)findViewById(R.id.direction);
        seekbar= (SeekBar)findViewById(R.id.seek);
        mProgressText = (TextView)findViewById(R.id.thershold_lb);
        //seekbar example
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                mProgressText.setText("Speed thershold:"+progress);
                seekbar_progress = progress;
                updateThershold2Notify(seekbar_progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                //mProgressText.setText("track on" );
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                //mProgressText.setText("track off" );
            }
        });

        //https://github.com/jjoe64/GraphView
        GraphView graph = (GraphView) findViewById(R.id.graph);
        initGraph(graph);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Intent notificationServiceIntent = new Intent(this, NotificationService.class);
        bindService(notificationServiceIntent, notificationConnection, BIND_AUTO_CREATE);
    }

    public void initGraph(GraphView graph)
    {
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(10);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(20);

        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling


        //graph.getGridLabelRenderer().setLabelVerticalWidth(100);
        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("point/per 0.3 second");
        gridLabel.setVerticalAxisTitle("speed");

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        mTimer = new Runnable()
        {
            @Override
            public void run()
            {
                graphLastXValue += 1.0;
                mSeries.appendData(new DataPoint(graphLastXValue, mSpeedNum),
                        true, 35);
                mHandler.postDelayed(this, 330);
            }
        };
        mHandler.postDelayed(mTimer, 1500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);

        unbindService(notificationConnection);
        mBluetoothLeService = null;
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            String dir="";

            mDataField.setText(data);
            String[] tokens = data.split(",");
            if("@".equals(tokens[0]))
            {
                mSpeedField.setText("current speed :"+tryParseDouble(tokens[2])/100.0);

                if("a".equals(tokens[1]))
                    dir = "apart";
                else if("b".equals(tokens[1]))
                    dir="depart";
                else if("c".equals(tokens[1]))
                    dir="no detect";

                mDirectionField.setText("direction:"+dir);
                mSpeedNum = tryParseDouble(tokens[2])/100.0;

                if (mSpeedNum >= seekbar_progress)
                    mSpeedField.setTextColor(Color.rgb(255, 0, 0));
                else
                    mSpeedField.setTextColor(Color.rgb(0, 255, 120));
            }


        }
    }

    public static Double tryParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );

        //printf(String.valueOf(gattServiceData)+String.valueOf(gattCharacteristicData));


    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateThershold2Notify(int updateSeekbarProgress)
    {
        Intent broadcast2NotificaitonService = new Intent();
        broadcast2NotificaitonService.setAction(NotificationService.mInputRadarActivitySpeedThers);
        broadcast2NotificaitonService.putExtra("Data",String.valueOf(updateSeekbarProgress));
        sendBroadcast(broadcast2NotificaitonService);
    }

    private void printf(String s)
    {
        mPrintData = mPrintData+"\r\n"+s+"\r\n";
        mPrintfText.setText(mPrintData);
    }

    private void printfClear()
    {
        mPrintData = "";
        mPrintfText.setText(" ");
    }
}