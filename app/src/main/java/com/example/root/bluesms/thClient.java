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
import java.io.OutputStream;

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
//                        {"header":{"size":"5","type":"message"},"content":"salut"}
//                        {"header":{"size":"5","type":"numero"},"content":{"num":"0677564892","name":"toto"}}
        write("hello world !!!");

        try {
            DataInputStream in = new DataInputStream(sock.getInputStream());
            while(!end) {
                bytesRead = in.read(messageByte);
                String dataString = new String(messageByte, 0, bytesRead);
                arrayData = dataString.split(":");

                if(arrayData.length > 1) {
                    String num =arrayData[0];
                    String messageString = arrayData[1];

                    if (messageString.equals("quit") || !sock.isConnected()) {
                        end = true;
                    }
                    if (messageString.equals("!shutdown!")) {
                        Intent i = new Intent("08945BlueSms");
                        i.putExtra("content", new String[]{"killSevrice"});
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    }

                    Intent i = new Intent("08945BlueSms");
                    i.putExtra("content", new String[]{"toast", num, messageString});
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            }
            Log.println(Log.ASSERT, "thClient", "fin d'une connection");
            sock.close();
        }
        catch (Exception e) {
            try {
                sock.close();
            } catch (IOException e1) {
                Log.println(Log.ASSERT, "erreur close", String.valueOf(e));
            }
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

    public void write(String msg) {
        try {
            byte[] byteString = (msg).getBytes();
            OutputStream out=sock.getOutputStream();

            Log.println(Log.ASSERT, "thClient : ", "envoie du message "+msg);
            Log.println(Log.ASSERT, "thClient", String.valueOf(byteString.length));

            out.write(String.valueOf(byteString.length).getBytes());
            out.flush();
            out.write(byteString);

        } catch (IOException e) {
            Log.println(Log.ASSERT,"Erreur de bienvenue", e.getMessage());
        }
    }
}
