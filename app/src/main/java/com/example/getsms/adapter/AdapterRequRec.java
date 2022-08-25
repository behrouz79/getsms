package com.example.getsms.adapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.getsms.API.SMS_InterFace;
import com.example.getsms.R;
import com.example.getsms.modul.Response;
import com.example.getsms.roomDB.DataBase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdapterRequRec extends RecyclerView.Adapter<AdapterRequRec.MyHolder> {

    private Context cxt;
    private List<Response> data;
    DataBase db;
    SharedPreferences sharedPref;

    public AdapterRequRec(Context cxt, List<Response> data) {
        this.cxt = cxt;
        this.data = data;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vv = LayoutInflater.from(cxt).inflate(R.layout.item_rec_request, parent, false);
        db = DataBase.getDbInstance(cxt);
        sharedPref = cxt.getSharedPreferences("BaseUrl",Context.MODE_PRIVATE);
        return new MyHolder(vv);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.name.setText(data.get(position).getName());
        holder.body.setText(data.get(position).getBody());
        holder.statusCode.setText(data.get(position).getStatus_code()+"");
        holder.date.setText(data.get(position).getDate());
        holder.currentId.setText(data.get(position).getID()+"");
        if(data.get(position).getStatus_code() == 200) {
            holder.btnRefresh.setClickable(false);
            holder.btnRefresh.setVisibility(View.INVISIBLE);
        }
        holder.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequest(holder.name.getText().toString(), holder.body.getText().toString(), Integer.parseInt(holder.currentId.getText().toString()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {

        private TextView name, statusCode, date ,currentId, body;
        private ImageButton btnRefresh;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.tvName);
            body = itemView.findViewById(R.id.tvBody);
            statusCode = itemView.findViewById(R.id.tvRes);
            date = itemView.findViewById(R.id.tvDate);
            currentId = itemView.findViewById(R.id.currentId);
            btnRefresh = itemView.findViewById(R.id.btnRequ);
        }
    }


    private void sendRequest(String msg_from,String msg_body, int uid) {
        String url = sharedPref.getString("Url", "");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SMS_InterFace request = retrofit.create(SMS_InterFace.class);
        request.sendSMS(msg_from, msg_body).enqueue(
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                        db.smsDao().updateRecord(msg_from, msg_body, sdf.format(c.getTime()), response.code(), uid);
                        Toast.makeText(cxt, "درخواست مجدد با موفقت ارسال شد.", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                        db.smsDao().updateRecord(msg_from, msg_body, sdf.format(c.getTime()), 500, uid);
                        Toast.makeText(cxt, "درخواست مجدد با خطا مواجه شد.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
