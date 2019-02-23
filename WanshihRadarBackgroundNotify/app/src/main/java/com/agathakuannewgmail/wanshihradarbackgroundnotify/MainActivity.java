package com.agathakuannewgmail.wanshihradarbackgroundnotify;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private NotificationHelper mNotificationHelper;
    private Button mStartBt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartBt = (Button)findViewById(R.id.scan_start_bt);
        mStartBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("mainActivity", "goto scan activity");

                final Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                //final Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                startActivity(intent);
            }
        });

        mNotificationHelper = new NotificationHelper(this);
    }

    /**
     * 打开Chanel设置界面，提供Channel ID
     *
     * @param view
     */
    public void openChannelSetting(View view)
    {
        mNotificationHelper.openChannelSetting(NotificationHelper.CHANNEL_ID);
    }

    public void openNotificationSetting(View view)
    {
        mNotificationHelper.openNotificationSetting();
    }
}
