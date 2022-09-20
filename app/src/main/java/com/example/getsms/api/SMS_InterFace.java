package com.example.getsms.api;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SMS_InterFace {
    @GET("?")
    Call<ResponseBody> sendSMS(@Query("from") String from,
                               @Query("message") String message);
}
