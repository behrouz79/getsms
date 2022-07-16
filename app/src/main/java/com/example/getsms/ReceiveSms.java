package com.example.getsms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.widget.Toast;


//public class ReceiveSms extends Service {
//    private static BroadcastReceiver br_ScreenOffReceiver;
//
//    @Override
//    public IBinder onBind(Intent arg0) {
//        return null;
//    }
//
//    @Override
//    public void onCreate() {
//        registerScreenOffReceiver();
//    }
//
//    @Override
//    public void onDestroy() {
//        unregisterReceiver(br_ScreenOffReceiver);
//        br_ScreenOffReceiver = null;
//    }
//
//    private void registerScreenOffReceiver() {
//        br_ScreenOffReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
//                    Bundle bundle = intent.getExtras();
//                    SmsMessage[] msgs;
//                    String msg_from;
//                    if (bundle != null) {
////                try {
//                        Object[] pdus = (Object[]) bundle.get("pdus");
//                        msgs = new SmsMessage[pdus.length];
//                        for (int i = 0; i < msgs.length; i++) {
//                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
//                            msg_from = msgs[i].getOriginatingAddress();
//                            String msgBody = msgs[i].getMessageBody();
//                            Toast.makeText(context, "From:" + msg_from + ",Body:" + msgBody, Toast.LENGTH_SHORT).show();
//                        }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//                    }
//                }
//            }
//        };
//
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
//        registerReceiver(br_ScreenOffReceiver, filter);
//    }
//}


public class ReceiveSms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String msg_from;
            if (bundle != null) {
//                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        Toast.makeText(context, "From:" + msg_from + ",Body:" + msgBody, Toast.LENGTH_SHORT).show();
                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }
    }
}
