package com.example.root.bluesms;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Iterator;
import java.util.UUID;
import android.os.Handler;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


public class BlueSms extends Service {
    private final boolean ENABLE_BLUE=true;
    private final boolean DISABLE_BLUE=false;

    private BluetoothAdapter adapter=null;
    private thServer server=null;
    private UUID uuid = UUID.randomUUID();

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String data = msg.getData().getString("json");
            if (data != null) {
                Log.println(Log.ASSERT,"Handler service", data);
                try {
                    JSONObject json = new JSONObject(data);                     // json object recover into the handler
                    JSONObject head = (JSONObject)json.get("header");           // json object (head of the json)
                    JSONObject content = (JSONObject)json.get("content");       // json object (content of the json)

                    if(head.get("type").equals("Command")) {
                        if(content.getString("message").equals("shutdown")) {
                            Intent killService = new Intent(getApplicationContext(), BlueSms.class);
                            killService.putExtra("kill", true);
                            startService(killService);
                        }
                    }
                    else if (head.get("type").equals("Message")) {
                        String num = content.getString("num");
                        String message = content.getString("message");

                        SmsManager sms = SmsManager.getDefault();
                        sms.sendTextMessage(num, null, message, null, null);

                        Toast T = Toast.makeText(getApplicationContext(), num+":"+message, Toast.LENGTH_LONG);
                        T.show();
                    }
                    else {
                        Log.println(Log.ASSERT, "Error", "message reçu dans le handler non reconnu");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public BlueSms() {
        //constructor
        Log.println(Log.ASSERT, "debug", "Code du service");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent killService = new Intent(this, BlueSms.class);
        killService.putExtra("kill", true);
        PendingIntent close = PendingIntent.getService(this, 0, killService, 0);

        Notification notificationBuilder = new Notification.Builder(this)
                .setContentTitle("BlueSms")
                .setContentText("Service BlueSms")
                .setSmallIcon(R.drawable.bluetooth)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

                .addAction(new Notification.Action(R.drawable.close, "close Service", close))
                .build();

        startForeground(1, notificationBuilder);

        Log.println(Log.ASSERT, "UUID" ,uuid.toString());
        // initialise bluetooth
        adapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        toggleBluetooth(ENABLE_BLUE);

        server = new thServer(adapter, uuid, handler, getApplicationContext());
        server.start();
        Log.println(Log.ASSERT, "service", " Server running");

        Intent i = new Intent("08945BlueSms");
        i.putExtra("content", new String[] { "uuid", uuid.toString(), adapter.getAddress()});
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        toggleBluetooth(DISABLE_BLUE);
        super.onDestroy();

        Iterator it = server.getClients().iterator();
        while(it.hasNext()) {
            thClient current = (thClient) it.next();
            Log.println(Log.ASSERT, "kill thread", String.valueOf(current));
            current.interrupt();
        }
        server.interrupt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //if boolean kill is set to true them kill the service
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            Intent i = new Intent("08945BlueSms");
            i.putExtra("content", new String[]{"service is died"});
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            Log.println(Log.ASSERT, "onDestroy", "send to broadcast");

            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void toggleBluetooth(boolean action) {
        // change bluetooth state to action state true == enable and false == disable
        try {
            if (!action && adapter.isEnabled()) {
                adapter.disable();
                while (adapter.isEnabled()) {}
            }
            else if(action && !adapter.isEnabled()){
                adapter.enable();
                while (!adapter.isEnabled()) {}
            }
        }
        catch (Exception e) {
            Log.println(Log.ASSERT, "erreur bluetooth", e.toString());
        }
    }

    public static String getName() {
        return "BlueSms";
    }

}
