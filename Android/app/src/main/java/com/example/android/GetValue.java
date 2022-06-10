package com.example.android;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GetValue extends AppCompatActivity {

    private EditText ip,port;
    private Button over;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_value);
        initView();
        initEvent();
    }

    private void initView() {
        ip=(EditText) findViewById(R.id.ip);
        port=(EditText) findViewById(R.id.port);
        over=(Button) findViewById(R.id.over);
    }

    private void initEvent(){
        String ipText=ip.getText().toString();
        String portText=port.getText().toString();

        over.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), ipText, Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), portText, Toast.LENGTH_LONG).show();
            }
        });
    }
}