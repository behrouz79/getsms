package com.example.getsms;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import com.example.getsms.API.SMS_InterFace;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.roomDB.SmsRecord;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ReceiveSms extends BroadcastReceiver {
    String url = "https://smartfixer.ir/";
    Context cxt;
    DataBase db;

    private static final Pattern sPattern
            = Pattern.compile("^([0-9]{11})$");

    @Override
    public void onReceive(Context context, Intent intent) {
        cxt = context;
        db = DataBase.getDbInstance(cxt);

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        String msgBody = msgs[i].getMessageBody();
                        // Api
                        if(sPattern.matcher(msgBody).matches()){
                            sendRequest(msgBody);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendRequest(String msg_from) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SMS_InterFace request = retrofit.create(SMS_InterFace.class);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("text", msg_from);
        request.sendSMS(params).enqueue(
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        SmsRecord record = new SmsRecord();
                        record.title = msg_from;
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                        record.date = sdf.format(c.getTime());
                        record.status = response.code();
                        db.smsDao().insertRecord(record);
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        SmsRecord record = new SmsRecord();
                        record.title = msg_from;
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                        record.date = sdf.format(c.getTime());
                        record.status = 500;
                        db.smsDao().insertRecord(record);
                    }
                }
        );
    }
}
