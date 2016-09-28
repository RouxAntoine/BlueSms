/*
 *  FILE create by antoine Roux
 *  <antoinroux@hotmail.fr>
 */

package com.example.root.bluesms;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.provider.ContactsContract;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

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
//public class thClient extends HandlerThread {

    private BluetoothSocket sock = null;
    private Handler handlerEnvoiSms = null;
    private final JSONObject jsonMsg = new JSONObject();
    private final JSONObject jsonContact = new JSONObject();
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

        // initialize the json contact object
        try {
            jsonContact.put("header", new JSONObject().put("type", "contact"));
            jsonContact.put("content", new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                    else if (messageString.equals("!contact!")) {
                        // recuperation des contacts et json-nization
                        ContentResolver cr = ctx.getContentResolver();
                        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

                        while (cursor.moveToNext()) {
                            try {
                                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                                String name      = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                                String hasPhone  = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                                if (Integer.parseInt(hasPhone) > 0) {
                                    Cursor phones = cr.query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                                    while (phones.moveToNext()) {
                                        String phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));
                                        setJsonContact(name, phoneNumber);
                                    }
                                    phones.close();
                                }
                            }catch(Exception e){}
                        }
                        System.err.println(jsonContact.toString());
                        write(jsonContact.toString());
                    }
                    else {
                        System.err.println("sms recu préparation avant envoi par bluetooth");
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

    public void setJsonContact(String nom, String num) {
        JSONObject temp = new JSONObject();
        try {
            temp.put("nom", nom);
            temp.put("num", num);

            ((JSONArray)jsonContact.get("content")).put(temp);

        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            jsonMsg.put("header", new JSONObject().put("type", type));
            jsonMsg.put("content", new JSONObject().put("num", num).put("message", content));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonMsg;
    }

    public void write(String in) {
        CharsetEncoder enc = Charset.forName("UTF-8").newEncoder();
//        CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();

        ByteBuffer msg = null;
        try {
            msg = enc.encode(CharBuffer.wrap(in));
            byte[] byteString = new byte[msg.remaining()];
            msg.get(byteString, 0, msg.remaining());
            OutputStream out=sock.getOutputStream();

            out.write((String.valueOf(byteString.length)).getBytes());
//            out.flush();
            out.write(byteString);

        } catch (IOException e) {
            Log.println(Log.ASSERT,"Erreur de bienvenue", e.getMessage());
        }
    }
}
