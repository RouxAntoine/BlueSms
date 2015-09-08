package com.example.root.bluesms;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BlueSms extends Service {

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


        // Gestion du bluetooth
        BluetoothAdapter blue = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        if (!blue.isEnabled()) {
            Log.println(Log.ASSERT, "Bluetooth", "Bluetooth est desactivé");
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //if boolean kill is set to true them kill the service
        if (intent.getBooleanExtra("kill", false)) {
            Intent i = new Intent("08945BlueSms");
            i.putExtra("content","service is died");
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


    public static String getName() {
        return "BlueSms";
    }
}
