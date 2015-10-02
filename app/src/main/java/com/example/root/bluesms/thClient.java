package com.example.root.bluesms;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by root on 09/09/15.
 * class that manage client connection for send sms
 *  exemple of format for communicate between thClient and handle into service
 */
public class thClient extends Thread {
    private BluetoothSocket sock = null;
    private Handler handler = null;
    private final JSONObject jsonMsg = new JSONObject();

    public thClient(BluetoothSocket s, Handler h) {
        super();
        handler = h;
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

        write("hello world !!!");

        try {
            Message O_msg = handler.obtainMessage();
            DataInputStream in = new DataInputStream(sock.getInputStream());

            while(!end) {
                Bundle b = new Bundle();

                bytesRead = in.read(messageByte);
                String dataString = new String(messageByte, 0, bytesRead);
                arrayData = dataString.split(":");

                if(arrayData.length > 1) {
                    String num =arrayData[0];
                    String messageString = arrayData[1];

                    if (messageString.equals("quit") || !sock.isConnected()) {
                        end = true;
                    }
                    else if (messageString.equals("!shutdown!")) {
                        b.putString("json", setJson("Command", null, "shutdown").toString());
                        end = true;
                    }
                    else {
                        b.putString("json", setJson("Message", num, messageString).toString());
                    }

                    O_msg.setData(b);
                    handler.sendMessage(O_msg);
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

            out.write(String.valueOf(byteString.length).getBytes());
            out.flush();
            out.write(byteString);

        } catch (IOException e) {
            Log.println(Log.ASSERT,"Erreur de bienvenue", e.getMessage());
        }
    }
}