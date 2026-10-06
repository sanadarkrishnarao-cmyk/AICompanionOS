package com.myai.companion;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceStateSafe(savedInstanceState));
        setContentView(R.layout.activity_main);
    }
    private Bundle savedInstanceStateSafe(Bundle b) { return b; }
}
