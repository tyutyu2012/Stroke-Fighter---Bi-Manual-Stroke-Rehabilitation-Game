package com.example.tongyu.strokefighter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by tongyu on 12/2/17.
 */

public class GameSetting extends AppCompatActivity {

    private int mTime, mQuestionNumber, mDifficulity, mMode;
    TextView tvSettingTime, tvSettingQuestionNumber, tvSettingDifficulity, tvSettingMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_setting);

        // initialize component
        tvSettingTime = (TextView)findViewById(R.id.tv_time_setting);
        tvSettingQuestionNumber = (TextView) findViewById(R.id.tv_qn_setting);
        tvSettingDifficulity = (TextView) findViewById(R.id.tv_df_setting);
        tvSettingMode = (TextView) findViewById(R.id.tv_mode_setting);

        loadFromFile();

        tvSettingTime.setText(mTime + "");
        tvSettingQuestionNumber.setText(mQuestionNumber + "");
        tvSettingDifficulity.setText(mDifficulity + "");
        if(mMode == 1)
            tvSettingMode.setText("Challenge");
        else
            tvSettingMode.setText("Causal");
        // create default file if not exist
    }

    public void loadFromFile()
    {
        try {
            FileInputStream fIn = openFileInput(MainMenu.SETTING_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fIn);

            File file = getFileStreamPath(MainMenu.SETTING_FILE_NAME);
            /* Prepare a char-Array that will
            *  hold the chars we read back in. */
            char[] inputBuffer = new char[(int)file.length()];

            // Fill the Buffer with data from the file
            isr.read(inputBuffer);

            // Transform the chars to a String
            String readString = new String(inputBuffer);
            String[] variables = readString.split(",");
            mTime = Integer.valueOf(variables[0]);
            mQuestionNumber = Integer.valueOf(variables[1]);
            mDifficulity = Integer.valueOf(variables[2]);
            mMode = Integer.valueOf(variables[3]);

            Log.d("File Reading stuff", "success = " + readString);
        }
        catch (IOException e)
        {

        }
    }

    public void writeToFile()
    {
        try {
            // default is 10 questions, 10 seconds, difficulity 5
            String settingString = mTime + "," + mQuestionNumber + "," +mDifficulity + "," + mMode;

            FileOutputStream fOut = openFileOutput(MainMenu.SETTING_FILE_NAME, MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            // Write the string to the file
            osw.write(settingString);

            osw.flush();
            osw.close();
        } catch (IOException e) {

        }
    }

    public void onClickSaveSetting(View v)
    {
        writeToFile();
        Toast.makeText(this, "Setting Updated", Toast.LENGTH_LONG).show();
    }

    public void onClickTimeAdd(View v)
    {
        // maximum time is 15
        if(mTime < 15)
        {
            mTime ++;
            tvSettingTime.setText(mTime + "");
        }
    }

    public void onClickTimeMinus(View v)
    {
        // minimum time is 3
        if(mTime > 3)
        {
            mTime --;
            tvSettingTime.setText(mTime + "");
        }

    }

    public void onClickQuestionNumberAdd(View v)
    {
        // maximum question number is 20
        if(mQuestionNumber < 20)
        {
            mQuestionNumber ++;
            tvSettingQuestionNumber.setText(mQuestionNumber + "");
        }

    }

    public void onClickQuestionNumberMinus(View v)
    {
        // minimum question number is 5
        if(mQuestionNumber > 5)
        {
            mQuestionNumber --;
            tvSettingQuestionNumber.setText(mQuestionNumber + "");
        }
    }

    public void onClickDifficultyAdd(View v)
    {
        // maximum diffculity is 15
        if(mDifficulity < 15)
        {
            mDifficulity ++;
            tvSettingDifficulity.setText(mDifficulity + "");
        }
    }

    public void onClickDifficultyMinus(View v)
    {
        // minimum difficulty is 5
        if(mDifficulity > 5)
        {
            mDifficulity --;
            tvSettingDifficulity.setText(mDifficulity + "");
        }
    }

    public void onClickHomeGameSetting(View v)
    {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        startActivity(upIntent);
    }

    public void onClickArrow(View v)
    {
        if(mMode == 1)
        {
            mMode = 0;
            tvSettingMode.setText("Causal");
        }
        else if (mMode == 0)
        {
            mMode = 1;
            tvSettingMode.setText("Challenge");
        }
    }


}
