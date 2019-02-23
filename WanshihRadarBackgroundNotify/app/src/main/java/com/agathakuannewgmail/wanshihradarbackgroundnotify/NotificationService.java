package com.agathakuannewgmail.wanshihradarbackgroundnotify;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends Service {
    public static final String TAG = "OUTPUT_SERVICE";
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private static int count = 0;
    private boolean isPause = false;
    private boolean isStop = true;
    private static int DELAY = 1000;  //1s
    private static int PERIOD = 1000;  //1s

    private IntentFilter mInputIntentFilter;

    public static final String mInputBroadcastStringAction = "com.agathakuannewgmail.doubleservicetest.inputstring";
    public static final String mInputBLEData="ble.send.radar.data";
    public static final String mInputBLECharacteristicRaw="ble.send.radar.characteristic.raw";
    public static final String mInputRadarActivitySpeedThers="radar.activity.send.speed.thers";

    private NotificationHelper mNotificationHelper;
    private static final int    DEFAULT_ID = 1001;

    private double mSpeedNum = 0.0;
    private int mSpeedThers = 100;
    private String mDirection =" ";


    private outputBinder mBinder = new outputBinder();

    class outputBinder extends Binder
    {
        public void startOutput()
        {
            Log.d(TAG,"OUTPUT START...");
            startTimer();
        }
    }

    public NotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInputIntentFilter = new IntentFilter();
        mInputIntentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        mInputIntentFilter.addAction(mInputBroadcastStringAction);
        mInputIntentFilter.addAction(mInputBLEData);
        mInputIntentFilter.addAction(mInputBLECharacteristicRaw);
        mInputIntentFilter.addAction(mInputRadarActivitySpeedThers);

        registerReceiver(mInputReceiver, mInputIntentFilter);

        //https://blog.csdn.net/jdsjlzx/article/details/84327815
        mNotificationHelper = new NotificationHelper(this);
    }

    @Override
    public void onDestroy() {
        stopTimer();
        unregisterReceiver(mInputReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed by Alex");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    private void startTimer()
    {
        if (mTimer == null) {
            mTimer = new Timer();
        }

        if (mTimerTask == null)
        {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "emain Notification COUNT = "+String.valueOf(count));

                    do{
                        try
                        {
                            Thread.sleep(1000);

                        }catch (InterruptedException e){}

                    }while (isPause);

                    count++;
                }
            };
        }

        if(mTimer != null && mTimerTask != null )
        {
            mTimer.schedule(mTimerTask, DELAY, PERIOD);
        }

    }

    private void stopTimer()
    {
        if(mTimer != null)
        {
            mTimer.cancel();
            mTimer = null;
        }
        if(mTimerTask != null)
        {
            mTimerTask.cancel();
            mTimerTask = null;
        }

        count = 0;
    }

    public final BroadcastReceiver mInputReceiver =  new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            if(intent.getAction().equals(mInputBroadcastStringAction))
            {
                showNotification("BLE Service",intent.getStringExtra("Data") );
            }
            else if(intent.getAction().equals(mInputBLEData))
            {
                showNotification("BLE Service in data",intent.getStringExtra("Data") );
            }
            else if(intent.getAction().equals(mInputBLECharacteristicRaw))
            {
                //showNotification("BLE Service in chara raw",intent.getStringExtra("Data") );
                bleSpeedCompare(intent.getStringExtra(BluetoothLeService.EXTRA_DATA),mSpeedThers);
            }
            else if(intent.getAction().equals(mInputRadarActivitySpeedThers))
            {
                setSpeedThers(tryParseInt(intent.getStringExtra("Data")));
            }

        }
    };

    public void setSpeedThers(int newThers)
    {
        mSpeedThers = newThers;
    }

    public void bleSpeedCompare(String s, int speedThershold)
    {
        String[] tokens = s.split("=");

        if(tokens[0].equals("speed"))
        {
            mSpeedNum = tryParseDouble(tokens[1])/100.0;
            mDirection = tokens[2];
        }

        if(mSpeedThers<= mSpeedNum)
        {
            showNotification("Speed Over Warning!","speed:"+
                    String.valueOf(mSpeedNum)+"km/h"+" "+"thers:"+
                    String.valueOf(speedThershold)+" "+"direction:"+mDirection);
        }
    }



    public void showNotification(String title, String content)
    {
        NotificationCompat.Builder builder = mNotificationHelper.getNotification(title, content);
        mNotificationHelper.notify(DEFAULT_ID, builder);
    }

    public static Double tryParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
