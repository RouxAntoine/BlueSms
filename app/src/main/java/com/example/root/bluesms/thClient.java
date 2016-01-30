package com.example.root.bluesms;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by root on 09/09/15.
 * class that manage client connection for send sms
 *  exemple of format for communicate between thClient and handle into service
 */
public class thClient extends Thread {
//public class thClient extends HandlerThread {

    private BluetoothSocket sock = null;
    private Handler handlerEnvoiSms = null;
    private final JSONObject jsonMsg = new JSONObject();
    private Context ctx;
    private BroadcastReceiver smsReceiver;

    private Handler handlerSmsRecu = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String s = msg.getData().getString("smsRecu");
            write(s);
            System.out.println("Envoi par bluetooth du message reçu par sms : "+s);
        }
    };

    public thClient(BluetoothSocket s, Handler h, Context ctx) {
        super();
        handlerEnvoiSms = h;
        sock = s;
        this.ctx = ctx;
        Log.println(Log.ASSERT, "thClient", "creation d'un thread client");

        smsReceiver = new SmsListener(handlerSmsRecu);
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        this.ctx.registerReceiver(smsReceiver, intentFilter);
    }

    @Override
    public void run() {
        super.run();

        byte[] messageByte = new byte[1024];
        boolean end = false;
        int bytesRead = 0;
        String[] arrayData = null;

        setJson("debug", "Server", "hello world !!!");
        write(jsonMsg.toString());

        try {
            DataInputStream in = new DataInputStream(sock.getInputStream());

            while(!end) {
                Message O_msg = handlerEnvoiSms.obtainMessage();
                Bundle b = new Bundle();

                bytesRead = in.read(messageByte);
                String dataString = new String(messageByte, 0, bytesRead);

                System.err.println("recu : "+dataString);
                arrayData = dataString.split(":");

                if(arrayData.length > 1) {
                    String num = arrayData[0];
                    String messageString = arrayData[1];

                    if (messageString.equals("quit") || !sock.isConnected()) {
                        end = true;
                    }
                    else if (messageString.equals("!shutdown!")) {
                        b.putString("json", setJson("Command", null, "shutdown").toString());
                        end = true;
                    }
                    else {
                        System.err.println("sms recu par bluetooth préparation avant envoi par bluetooth");
                        b.putString("json", setJson("Message", num, messageString).toString());
                    }

                    O_msg.setData(b);
                    handlerEnvoiSms.sendMessage(O_msg);
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

    /**
     * method for properly stop the client thread
     */
    @Override
    public void interrupt() {
        super.interrupt();
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.println(Log.ASSERT, "thClient", "fin d'une connection");
        this.ctx.unregisterReceiver(smsReceiver);
    }

    /**
     * method that set json attribute
     * @param type type of the content of json attr
     * @param num phone number if type == command them the number equal to null
     * @param content content of the message
     * @return return an instance of the JSONObject contain into the json attribute
     */
    public JSONObject setJson(String type, String num, String content) {
        try {
            jsonMsg.put("header",new JSONObject().put("type",type));
            jsonMsg.put("content", new JSONObject().put("num", num).put("message",content));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonMsg;
    }

    public void write(String msg) {
        try {
            byte[] byteString = (msg).getBytes();
            OutputStream out=sock.getOutputStream();

            Log.println(Log.ASSERT, "thClient : ", "envoie du message "+msg);
            Log.println(Log.ASSERT, "thClient", String.valueOf(byteString.length));

            out.write((String.valueOf(byteString.length) + '\n').getBytes());
            out.flush();
            out.write(byteString);

        } catch (IOException e) {
            Log.println(Log.ASSERT,"Erreur de bienvenue", e.getMessage());
        }
    }
}