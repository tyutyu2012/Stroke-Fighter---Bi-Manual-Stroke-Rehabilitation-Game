package com.example.tongyu.strokefighter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by tongyu on 11/17/17.
 */

public class SpecificScoreView extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_score_view);

        Bundle extra = getIntent().getExtras();
        String id = extra.getString("id");
    }
}
