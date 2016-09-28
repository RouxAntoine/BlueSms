package com.example.root.bluesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by root on 29/01/16.
 */
public class SmsListener extends BroadcastReceiver {
    private Handler hand;
    private JSONObject json = new JSONObject();

    public SmsListener(Handler _h) {
        hand = _h;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for(SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                // recover receive sms and pass it to thClient who send it by bluetooth
                Message m = hand.obtainMessage();
                Bundle b = new Bundle();


                try {
                    json.put("header", new JSONObject().put("type", "sms"));
                    String messa = smsMessage.getMessageBody();
                    json.put("content", new JSONObject().put("num", smsMessage.getOriginatingAddress()).put("message",messa));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                b.putString("smsRecu", json.toString());
                
                m.setData(b);
                hand.sendMessage(m);
            }
        }
    }

}