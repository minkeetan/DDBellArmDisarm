package com.example.wbf486.armdisarmweapon;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    /*0 indicates ARM, 1 indicates DISARM*/
    private boolean ArmDisarmStatus = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void ButtonArmPress(View view)
    {
        ArmDisarmStatus = false;

        TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("ARM");
    }

    public void ButtonDisarmPress(View view)
    {
        ArmDisarmStatus = true;

        TextView StatusView = (TextView)findViewById(R.id.arm_disarm_status_id);
        StatusView.setText("DISARM");
    }

}
