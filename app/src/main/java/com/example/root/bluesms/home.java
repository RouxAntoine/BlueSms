package com.example.root.bluesms;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class home extends Activity {
    private Intent i = null;
    private Button btn_run;
    private Button btn_debug;
    private final tunnel receiver = new tunnel();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home);

        // broadcast receiver
        IntentFilter filter = new IntentFilter("08945BlueSms");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        i = new Intent(this, BlueSms.class);
        btn_run = (Button) findViewById(R.id.run);
        btn_debug = (Button) findViewById(R.id.debug);

        btn_run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BlueSms_isrunning(BlueSms.getName())) {
                    startService(i);
                    btn_run.setText("Stop");
                }
                else {
                    stopService(i);
                    btn_run.setText("Lancer");
                }
            }
        });
        btn_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open windows debug
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BlueSms_isrunning(BlueSms.getName())) {
            btn_run.setText("Stop");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private class tunnel extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.println(Log.ASSERT, "receive broadcast", intent.getAction());
            String extra= intent.getStringExtra("content");

            if (extra.equals("service is died")) {
                btn_run.setText("Lancer");
            }
        }
    }

    private boolean BlueSms_isrunning(String serviceName) {
        // return true if service with service name pass in argument is running
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String name = service.service.getShortClassName();
            if (name.contains(serviceName)) {
                return true;
            }
        }
        return false;
    }

}
