package com.example.root.bluesms;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class home extends Activity {
    private Button btn_run;
    private Button btn_debug;
    private TextView txt_uuid;

    private Intent i = null;
    private final tunnel receiver = new tunnel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home);

        i = new Intent(this, BlueSms.class);
        // register broadcast receiver
        IntentFilter filter = new IntentFilter("08945BlueSms");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        btn_run = (Button) findViewById(R.id.run);
        btn_debug = (Button) findViewById(R.id.debug);
        txt_uuid = (TextView) findViewById(R.id.txt_uuid);

        btn_run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BlueSms_isrunning(BlueSms.getName())) {
                    startService(i);
                    btn_run.setText("Stop");
                }
                else {
                    stopService(i);
                    serviceStopped();
                }
            }
        });

        btn_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivity(discoverableIntent);
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

    private void serviceStopped() {
        // ensemble des modifications sur l'interface l'orsque le service se coupe
        btn_run.setText("Lancer");
        txt_uuid.setText("UUID : null");
    }

    private class tunnel extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.println(Log.ASSERT, "receive broadcast", intent.getAction());
            String[] extra = intent.getStringArrayExtra("content");

            if (extra[0].equals("uuid")) {
                txt_uuid.setText("Uuid : " + extra[1].toString() + "\n" + "addr : " + extra[2].toString());
            }
            if (extra[0].equals("service is died")) {
                serviceStopped();
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
