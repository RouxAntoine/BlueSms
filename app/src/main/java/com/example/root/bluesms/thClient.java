package com.example.root.bluesms;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by root on 09/09/15.
 * class that manage client connection for send sms
 *
 */
public class thClient extends Thread {
    private BluetoothSocket sock = null;
    private Context context = null;

    public thClient(BluetoothSocket s,Context ctx) {
        super();
        context = ctx;
        sock = s;
        Log.println(Log.ASSERT, "thClient", "creation d'un thread client");
    }

    @Override
    public void run() {
        super.run();

        byte[] messageByte = new byte[1024];
        boolean end = false;
        int bytesRead = 0;
        String[] arrayData = null;

        try {
            DataInputStream in = new DataInputStream(sock.getInputStream());
            while(!end) {
                bytesRead = in.read(messageByte);
                String dataString = new String(messageByte, 0, bytesRead);

                if (dataString.equals("quit") || !sock.isConnected()) {
                    end = true;
                }

                arrayData = dataString.split(":");
                if(arrayData.length > 1) {
                    String num =arrayData[0];
                    String messageString = arrayData[1];

                    Intent i = new Intent("08945BlueSms");
                    i.putExtra("content",new String[] { "toast", num, messageString});
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            }
            Log.println(Log.ASSERT, "thClient", "fin d'une connection");
            sock.close();
        }
        catch (Exception e) {
            Log.println(Log.ASSERT, "erreur client", String.valueOf(e));
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.println(Log.ASSERT, "thClient", "fin d'une connection");
    }
}
