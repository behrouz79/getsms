package com.example.getsms.API;
import java.util.HashMap;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SMS_InterFace {
    @POST("test/")
    Call<ResponseBody> sendSMS(@Body HashMap sms);
}
