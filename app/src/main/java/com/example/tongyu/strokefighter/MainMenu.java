package com.example.tongyu.strokefighter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainMenu extends AppCompatActivity {

    String TAG = "MainMenu";
    public final static String SETTING_FILE_NAME = "default_settings_stroke_fighter.txt";
    ImageView snow, snow2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        snow = (ImageView) findViewById(R.id.img_snow_effect);
        snow2 = (ImageView) findViewById(R.id.img_snow_effect_2);
        TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, 400f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
        translateAnimation.setDuration(15000);  // animation duration
        translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
        snow.setAnimation(translateAnimation);
        snow2.setAnimation(translateAnimation);

        // create default file if not exist
        createFile();
    }

    public void onClickPlay(View v) throws IOException {
        Intent intent = new Intent(this, GameView.class);
        startActivity(intent);
    }

    public void onClcikScore(View v)
    {
        Intent intent = new Intent(this, ScoreView.class);
        startActivity(intent);
    }

    public void onClickConnection(View v)
    {
        Intent intent = new Intent(this, ConnectionView.class);
        startActivity(intent);
    }

    public void onClickSetting(View v)
    {
        Intent intent = new Intent(this, GameSetting.class);
        startActivity(intent);
    }

    public void createFile() {

        File file = getFileStreamPath(SETTING_FILE_NAME);
        if(!file.exists())
        {
            try {
                // default is 10 questions, 10 seconds, difficulity 5, mode challenge
                final String DEFAULT_SETTING = new String("10,10,5,1");

                FileOutputStream fOut = openFileOutput(SETTING_FILE_NAME, MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

                // Write the string to the file
                osw.write(DEFAULT_SETTING);

                osw.flush();
                osw.close();
            } catch (IOException e) {

            }
        }
        else
        {
            Log.d(TAG, "file exist");
        }
    }

}
