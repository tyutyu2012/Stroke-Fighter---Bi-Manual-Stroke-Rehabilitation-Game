package com.example.tongyu.strokefighter;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tongyu.strokefighter.Services.StrokeProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by tongyu on 10/12/17.
 */

public class GameView extends AppCompatActivity implements SensorEventListener{

    public Object accLock = new Object();
    public Object linearLock = new Object();

    public int MAX_QUESTION_NUMBER = 10;
    public int GAME_DIFFICULTY = 5;
    public int MAX_ANSWER_TIME = 10;

    private int mCurrentScore = 0;
    private int mTotalScore = 0;

    private Sensor mAccelerometer, mLinearAccelerometer;
    private SensorManager mSensorManager;
    private Random rand = new Random();
    private TextView mTvCommand, mTvScore, mTvQuestion;
    private Button mBtnTimer;

    private CountDownTimer mCountDownTimer;
    private int mNumberCommand, mPrevisousNumberCommand;

    private String mDatabaseMovements = "";
    private String mDataBaseCorrects = "";
    private String mDataBaseUserName = "Tong";

    ImageView imagePhone;
    AnimationSet animationSet;
    RotateAnimation rotateAnimation;
    TranslateAnimation translateAnimation;
    ScaleAnimation scaleAnimation;

    float last_x, last_y, last_z;

    GameOver mGameOver;
    MediaPlayer mediaPlayer, mediaCorrect, mediaWrong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_view);

        initializeComponent();
        loadFromFile();

        // create background, correct, and wrong mp3, and set background to repeat
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.carefree);
        mediaPlayer.setLooping(true);
        mediaCorrect = MediaPlayer.create(getApplicationContext(), R.raw.correct);
        mediaWrong = MediaPlayer.create(getApplicationContext(), R.raw.wrong);
        //mediaPlayer.start();

        // initilaize the senors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        mLinearAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).get(0);

        // add sensor to listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        imagePhone  =(ImageView) findViewById(R.id.img_phone_single);

        mTvCommand.setText("The game will start in a second");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startTimer();

            }
        }, 2000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  // Respond to the action bar's Up/Home button
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy()
    {
        Log.d("Destory", "Destoryed GameView Activity");
        cancelTimer();
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerListeners();
        mediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterListeners();
        cancelTimer();
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaCorrect.stop();
        mediaWrong.stop();
        if(mGameOver != null)
            mGameOver.cancel(true);
    }

    public void unRegisterListeners()
    {
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mLinearAccelerometer);
    }

    public void registerListeners()
    {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void initializeComponent()
    {
        mBtnTimer = (Button) findViewById(R.id.btn_time);
        mTvCommand = (TextView) findViewById(R.id.tv_command);
        mTvScore = (TextView) findViewById(R.id.tv_score);
        mTvQuestion = (TextView) findViewById(R.id.tv_questionNumber);
    }

    public void restart()
    {
        mCurrentScore = 0;
        mTotalScore = 0;
        mDatabaseMovements = "";
        mDataBaseCorrects = "";
        mTvScore.setText(mCurrentScore + " / " + mTotalScore);
        cancelTimer();
        startTimer();
    }

    private void startTimer()
    {
        if(mTotalScore < MAX_QUESTION_NUMBER) {
            randomCommand();

            mCountDownTimer = new CountDownTimer(MAX_ANSWER_TIME* 1000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    mBtnTimer.setText("" + seconds);
                }

                public void onFinish() {
                    mNumberCommand = -1;
                    mediaWrong.start();
                    imagePhone.clearAnimation();
                    updateScore(0);
                    onTimeOut();
                }
            }.start();
           // mCurrentScore ++;
            mTvQuestion.setText("Question " + (mTotalScore +1));
        }
        else
        {
            mGameOver = new GameOver(this);
            mGameOver.execute();
        }

    }

    public void onTimeOut()
    {
        mTvCommand.setText("Oops! Times Up, next");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                startTimer();
            }
        }, 1500);
    }

    public void updateScore(int check)
    {
        if(check == 0)
        {
            mDatabaseMovements = mDatabaseMovements + mNumberCommand;
            mDataBaseCorrects = mDataBaseCorrects + "0";
            mTotalScore ++;
            mTvScore.setText(mCurrentScore + " / " + mTotalScore);
        }
        else
        {
            mDatabaseMovements = mDatabaseMovements + mNumberCommand;
            mDataBaseCorrects = mDataBaseCorrects + "1";
            mCurrentScore++;
            mTotalScore ++;
            mTvScore.setText(mCurrentScore + " / " + mTotalScore);
        }
        Log.d("Database", mDatabaseMovements);
        Log.d("Database", mDataBaseCorrects);
    }

    public void cancelTimer()
    {
        if(! (mCountDownTimer== null))
          mCountDownTimer.cancel();
    }

    private void randomCommand()
    {
        //random.nextInt(max - min + 1) + min
        mNumberCommand = rand.nextInt(10) + 1;

        // can not be same as previous movement
        while(mNumberCommand == mPrevisousNumberCommand)
            mNumberCommand = rand.nextInt(10) + 1;
        mPrevisousNumberCommand = mNumberCommand;

        imagePhone.clearAnimation();
        switch(mNumberCommand) {
            case 1:
                mTvCommand.setText("Tip Left");
                imagePhone.clearAnimation();
                rotateAnimation = new RotateAnimation(0, -90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(3000);  // animation duration
                rotateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(rotateAnimation);
                break;

            case 2:
                mTvCommand.setText("Tip Right");
                imagePhone.clearAnimation();
                rotateAnimation = new RotateAnimation(0, 90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(3000);  // animation duration
                rotateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(rotateAnimation);
                break;

            case 3:
                mTvCommand.setText("Screen Up");
                break;

            case 4:
                mTvCommand.setText("Screen Down");
                break;
            case 5:
                mTvCommand.setText("Move Left");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, -300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 6:
                mTvCommand.setText("Move Right");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 7:
                mTvCommand.setText("Move Up");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, -300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 8:
                mTvCommand.setText("Move Down");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, 300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 9:
                mTvCommand.setText("Move Towards you");
                animationSet = new AnimationSet(true);

                scaleAnimation = new ScaleAnimation(0.3f, 1.2f, 0.3f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(2000);     // animation duration in milliseconds
                scaleAnimation.setRepeatCount(Animation.INFINITE);    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
                translateAnimation = new TranslateAnimation(0.0f, 80f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(2000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count

                animationSet.addAnimation(translateAnimation);
                animationSet.addAnimation(scaleAnimation);
                imagePhone.startAnimation(animationSet);
                break;
            case 10:
                mTvCommand.setText("Move away from you");
                animationSet = new AnimationSet(true);

                scaleAnimation = new ScaleAnimation(1.2f, 0.3f, 1.2f, 0.3f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(2000);     // animation duration in milliseconds
                scaleAnimation.setRepeatCount(Animation.INFINITE);    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
                translateAnimation = new TranslateAnimation(0.0f, -80f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(2000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count

                animationSet.addAnimation(translateAnimation);
                animationSet.addAnimation(scaleAnimation);
                imagePhone.startAnimation(animationSet);
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
            {
                synchronized (linearLock) {
                    float xx = event.values[0];
                    float yy = event.values[1];
                    float zz = event.values[2];

                    if (Math.abs(xx) > GAME_DIFFICULTY || Math.abs(yy) > GAME_DIFFICULTY || Math.abs(zz) > GAME_DIFFICULTY) {
                        if (Math.abs(xx) > Math.abs((yy)) && Math.abs(xx) > Math.abs(zz)) {
                            if (xx > 0) {
                                if (mNumberCommand == 5) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update left move");
                                }
                                // Log.d("Movement", "Move left");
                            } else {
                                if (mNumberCommand == 6) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update right move");
                                }
                                // Log.d("Movement", "Move Right");
                            }
                        } else if (Math.abs(yy) > Math.abs((xx)) && Math.abs(yy) > Math.abs(zz)) {
                            if (yy > 0) {
                                if (mNumberCommand ==8) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update down move");
                                }
                                // Log.d("Movement", "Move Down");
                            } else {
                                if (mNumberCommand == 7) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update up move");
                                }
                                // Log.d("Movement", "Move Up");
                            }
                        } else if (Math.abs(zz) > Math.abs((xx)) && Math.abs(zz) > Math.abs(yy)) {
                            if (zz > 0) {
                                if (mNumberCommand == 9) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update move towards you");
                                }
                                //Log.d("Movement", "Move Front");
                            } else {
                                if (mNumberCommand == 10) {
                                    onCorrectAnswer();
                                    Log.d("Movement", "update move away from you");
                                }
                                // Log.d("Movement", "Move Back");
                            }
                        }
                        //Log.d("TongYu", "x:" + event.values[0] + " y: " + event.values[1] + "z :" + event.values[2]);
                    }
                }
            }
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //lastUpdate = curTime;
                synchronized (accLock) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    if (Round(x, 4) > 10.0000) {
                        if (mNumberCommand == 1) {
                            onCorrectAnswer();
                            Log.d("Movement", "update left shake");
                        }
                    } else if (Round(x, 4) < -10.0000) {
                        if (mNumberCommand == 2) {
                            onCorrectAnswer();
                            Log.d("Movement", "update right shake");
                        }
                    } else if (Round(z, 4) > 10.0000) {
                        if (mNumberCommand == 3) {
                            onCorrectAnswer();
                            Log.d("Movement", "update screen up");
                        }
                    } else if (Round(z, 4) < -10.0000) {
                        if (mNumberCommand == 4) {
                            onCorrectAnswer();
                            Log.d("Movement", "update screen down");
                        }
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
            }


    }

    public static float Round(float Rval, int Rpl) {
        float p = (float)Math.pow(10,Rpl);
        Rval = Rval * p;
        float tmp = Math.round(Rval);
        //Log.d("TongYuRound", tmp/p + "");
        return (float)tmp/p;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
            MAX_ANSWER_TIME = Integer.valueOf(variables[0]);
            MAX_QUESTION_NUMBER = Integer.valueOf(variables[1]);
            GAME_DIFFICULTY = Integer.valueOf(variables[2]);

            Log.d("File Reading stuff", "success = " + readString);
        }
        catch (IOException e)
        {

        }
    }

    public void onCorrectAnswer()
    {
        mNumberCommand = -1;
        mediaCorrect.start();
        imagePhone.clearAnimation();
        mTvCommand.setText("Great Job, next ");
        updateScore(1);
        cancelTimer();
        // one seocnd wait for each question
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                startTimer();
            }
        }, 1000);
    }

    public void onClickHomeGameView(View v)
    {
        if(!(mGameOver== null))
            mGameOver.cancel(true);
        cancelTimer();
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        startActivity(upIntent);
        finish();
    }

    public void onClickNewGame(View v)
    {
        Toast.makeText(this, "Under Development!", Toast.LENGTH_LONG).show();
    }

    class GameOver extends AsyncTask<String,Void,String> {
        Context context;
        android.app.AlertDialog alertDialog;
        GameOver (Context ctx) {
            context = ctx;
        }
        @Override
        protected String doInBackground(String... params) {

            // sending the data to database
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            ContentValues myCV = new ContentValues();

            myCV.put(StrokeProvider.STROKE_TABLE_USER_NAME, mDataBaseUserName);
            myCV.put(StrokeProvider.STROKE_TABLE_MOVEMENTS_TESTED, mDatabaseMovements);
            myCV.put(StrokeProvider.STROKE_TABLE_CORRECTNESS, mDataBaseCorrects);
            myCV.put(StrokeProvider.STROKE_TABLE_DATE, date);

            //Perform the insert function using the ContentProvider
            getContentResolver().insert(StrokeProvider.CONTENT_URI, myCV);

            // get cursor
            Cursor myCursor = getContentResolver().query(StrokeProvider.CONTENT_URI, StrokeProvider.projection, null, null, null);

            // get all the data out
            while (myCursor.moveToNext())
            {
                int movementIndex = myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_MOVEMENTS_TESTED);
                // getting the data using the indexs
                String movement = myCursor.getString(movementIndex);
                Log.d("DatabaseOutput", movement);
            }

            myCursor.close();

            return null;
        }

        @Override
        protected void onPreExecute() {
            unRegisterListeners();
            mBtnTimer.setText("Game Over");
            mNumberCommand = -1;
        }

        @Override
        protected void onPostExecute(String result) {
            // AlertDialogs
            AlertDialog.Builder builder1 = new AlertDialog.Builder(GameView.this);
            builder1.setMessage("great job, your score is " + mCurrentScore + " / "+mTotalScore +" \nanother game?");
            builder1.setCancelable(false);

            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            registerListeners();
                            restart();
                        }
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // TODO: adding score to database
                            Intent intent = new Intent(GameView.this, MainMenu.class);
                            startActivity(intent);
                            finish();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.setCanceledOnTouchOutside(false);
            alert11.show();

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

}