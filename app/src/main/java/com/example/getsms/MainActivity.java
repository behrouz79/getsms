package com.example.getsms;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent service = new Intent(getApplicationContext(), GetSms.class);
        getApplicationContext().startService(service);

        /*if (Build.VERSION.SOK_INT >= Build.VERSION_CODES.M && checkSelf
        Permission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION GRANTED){
            request Permissions (new String[] (Manifest.permission.RECEIVE_SMS), 1000);

        }*/
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[]permissions,@NonNull int[]grantResults) {
        if (request Code == 1000){
            if (grantResults[0] = PackageManager.PERMISSION GRANTED){
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }*/
}