package com.example.tongyu.strokefighter.BluetoothGame;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tongyu.strokefighter.ConnectionView;
import com.example.tongyu.strokefighter.Services.BluetoothConnectionService;
import com.example.tongyu.strokefighter.Services.ClientServerMessageString;
import com.example.tongyu.strokefighter.R;

import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by tongyu on 11/27/17.
 */
// Thanks to this code from github
// https://github.com/mitchtabian/Sending-and-Receiving-Data-with-Bluetooth
public class ServerBluetoothChallengeGame extends AppCompatActivity implements SensorEventListener {

    final String TAG = "ServerChallengeGame";
    final String PHONE_TAG ="Phone message";
    BluetoothDevice mBluetoothDevice;
    private BluetoothConnectionService mBluetoothConnectionService;
    private final UUID MY_UUID_INSECURE = UUID.fromString("d7579aa2-4a6e-43b7-904c-9b5e9d37de19");

    TextToSpeech textToSpeech;
    ImageButton imageStartNew;
    ImageView imagePhone;
    AnimationSet animationSet;
    TranslateAnimation translateAnimation;
    ScaleAnimation scaleAnimation;
    ProgressDialog progressDialog;
    MediaPlayer mediaPlayer, mediaCorrect, mediaWrong;
    // scores and sensors
    private int mCurrentScore = 0;
    private int mTotalScore = 0;
    private Sensor mLinearAccelerometer;
    private SensorManager mSensorManager;
    private Random rand = new Random();
    private TextView mTvCommand, mTvScore, mTvQuestion, mTvMode;
    private int mNumberCommand, mPrevisousNumberCommand;
    public Object accLock = new Object();
    public Object linearLock = new Object();
    public static final int MAX_QUESTION_NUMBER = 10;
    private CountDownTimer mCountDownTimer;
    float last_x, last_y, last_z;

    // to handle the message sent from other device
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String msgBack = bundle.getString("key");
            int messageId = Integer.parseInt(String.valueOf(msgBack.charAt(0)));

            // 2 types of messgaes server will receive
            if(messageId == 1)
                onReceiveStartMessage(msgBack);
            else if (messageId == 3)
                onReceiveCorrectMessage();
            else if (messageId == 6)
                onReceiveStopMessage();
            else if (messageId == 9)
                onReceiveTimeMessage(msgBack);
        }
    };

    public void onReceiveStartMessage(String msg)
    {
        progressDialog.cancel();
        Log.d(TAG, "msg is " + msg);
        if(msg.equals(ClientServerMessageString.CLIENT_READY))
        {
            mCurrentScore = 0;
            mTotalScore = 0;
            mTvScore.setText("0/0");
            randomCommand();
            //new SendingVoiceCommand(this).execute(textToSpeak());
        }
    }
    public void onReceiveCorrectMessage()
    {
        updateScore(1);
        randomCommand();
        //new SendingVoiceCommand(this).execute(textToSpeak());
    }

    public void onReceiveStopMessage()
    {
    }

    public void onReceiveTimeMessage(String msg)
    {
        String split[] = msg.split(",");
        Long time = Long.parseLong(split[1]);
        clientMotion(time);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_view_bluetooth);

        // create background, correct, and wrong mp3, and set background to repeat
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.carefree);
        mediaPlayer.setLooping(true);
        mediaCorrect = MediaPlayer.create(getApplicationContext(), R.raw.correct);
        mediaWrong = MediaPlayer.create(getApplicationContext(), R.raw.wrong);
        mediaPlayer.start();

        progressDialog = new ProgressDialog(this);
        // Initialize the TextToSpeech
        textToSpeech=new TextToSpeech(ServerBluetoothChallengeGame.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=textToSpeech.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });

        // initialize buttons and text views
        //mBtnTimer = (Button) findViewById(R.id.btn_time);
        mTvCommand = (TextView) findViewById(R.id.tv_server_command);
        mTvScore = (TextView) findViewById(R.id.tv_score_server);
        imagePhone =(ImageView) findViewById(R.id.img_phone);
        mTvMode = (TextView) findViewById(R.id.tv_bluetooth_mode);
        mTvMode.setText("Challenge Mode");

        // initilaize the senors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mLinearAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).get(0);
        // add sensor to listener
        mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Set up the threads
        mBluetoothDevice = getIntent().getExtras().getParcelable("bluetoothdevice");
        Log.d(TAG, mBluetoothDevice.getName() + " " + mBluetoothDevice.getAddress());

        mBluetoothConnectionService = new BluetoothConnectionService(ServerBluetoothChallengeGame.this, mHandler);
        mBluetoothConnectionService.startClient(mBluetoothDevice, MY_UUID_INSECURE);

        // Initialize the new game button
        imageStartNew = (ImageButton) findViewById(R.id.img_start);
        imageStartNew.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    progressDialog.setMessage("Waiting for client to respond");
                    progressDialog.show();
                    mBluetoothConnectionService.write(ClientServerMessageString.SERVER_START.getBytes());
                } catch (Exception e) {

                }
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
        registerListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterListeners();
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "on destory");
       // mBluetoothConnectionService.write(ClientServerMessageString.GAME_OVER.getBytes());
        super.onDestroy();
    }

    public void unRegisterListeners()
    {
        mSensorManager.unregisterListener(this, mLinearAccelerometer);
    }

    public void registerListeners()
    {
        mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void randomCommand()
    {
        //random.nextInt(max - min + 1) + min
        mNumberCommand = rand.nextInt(10-5+1)+5;

        // can not be same as previous movement
        while(mNumberCommand == mPrevisousNumberCommand)
            mNumberCommand = rand.nextInt(10-5+1) + 5;
        mPrevisousNumberCommand = mNumberCommand;

        imagePhone.clearAnimation();
        switch(mNumberCommand){
            case 5:
                mTvCommand.setText("Server: Move Left");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, -300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 6:
                mTvCommand.setText("Server: Move Right");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 7:
                mTvCommand.setText("Server: Move Up");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, -300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 8:
                mTvCommand.setText("Server: Move Down");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, 300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 9:
                mTvCommand.setText("Server: Move Towards you");
                animationSet = new AnimationSet(true);

                scaleAnimation =  new ScaleAnimation(0.3f, 1.2f, 0.3f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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
                mTvCommand.setText("Server: Move away from you");
                animationSet = new AnimationSet(true);

                scaleAnimation =  new ScaleAnimation(1.2f, 0.3f, 1.2f, 0.3f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(2000);     // animation duration in milliseconds
                scaleAnimation.setRepeatCount(Animation.INFINITE);    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
                translateAnimation = new TranslateAnimation(0.0f, -80f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(2000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count

                animationSet.addAnimation(translateAnimation);
                animationSet.addAnimation(scaleAnimation);
                imagePhone.startAnimation(animationSet);
                break;

            default:
                Log.d(TAG, "random generator error");
                break;
        }
    }

    public void updateScore(int check)
    {
        if(check == 0)
        {
            // mDatabaseMovements = mDatabaseMovements + mNumberCommand;
            //mDataBaseCorrects = mDataBaseCorrects + "0";
            mTotalScore ++;
            mTvScore.setText(mCurrentScore + " / " + mTotalScore);
        }
        else
        {
            // mDatabaseMovements = mDatabaseMovements + mNumberCommand;
            // mDataBaseCorrects = mDataBaseCorrects + "1";
            mCurrentScore++;
            mTotalScore ++;
            mTvScore.setText(mCurrentScore + " / " + mTotalScore);
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

                if (Math.abs(xx) > 4 || Math.abs(yy) > 4 || Math.abs(zz) > 4) {
                    if (Math.abs(xx) > Math.abs((yy)) && Math.abs(xx) > Math.abs(zz)) {
                        if (xx > 0) {
                            if (mNumberCommand == 5) {
                                serverMotion();
                                Log.d("Movement", "update left move");
                            }
                            // Log.d("Movement", "Move left");
                        } else {
                            if (mNumberCommand == 6) {
                                serverMotion();

                                Log.d("Movement", "update right move");
                            }
                            // Log.d("Movement", "Move Right");
                        }
                    } else if (Math.abs(yy) > Math.abs((xx)) && Math.abs(yy) > Math.abs(zz)) {
                        if (yy > 0) {
                            if (mNumberCommand == 8) {
                                serverMotion();

                                Log.d("Movement", "update down move");
                            }
                            // Log.d("Movement", "Move Down");
                        } else {
                            if (mNumberCommand == 7) {
                                serverMotion();

                                Log.d("Movement", "update up move");
                            }
                            // Log.d("Movement", "Move Up");
                        }
                    } else if (Math.abs(zz) > Math.abs((xx)) && Math.abs(zz) > Math.abs(yy)) {
                        if (zz > 0) {
                            if (mNumberCommand == 9) {
                                serverMotion();

                                Log.d("Movement", "update move towards you");
                            }
                            //Log.d("Movement", "Move Front");
                        } else {
                            if (mNumberCommand == 10) {
                                serverMotion();

                                Log.d("Movement", "update move away from you");
                            }
                            // Log.d("Movement", "Move Back");
                        }
                    }
                    //Log.d("TongYu", "x:" + event.values[0] + " y: " + event.values[1] + "z :" + event.values[2]);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private long serverTime = 0;
    private long clientTime = 0;
    private boolean serverMoved = false;
    private boolean clientMoved = false;
    public void bothPhoneMoved()
    {
        Log.d("Phone time", "server time " + serverTime );
        Log.d("Phone time", "client time " + clientTime );

        // if both phones moved within 700ms
        if(Math.abs(serverTime - clientTime) < 700) {

            mediaCorrect.start();
            serverMoved = false;
            clientMoved = false;
            updateScore(1);
            imagePhone.clearAnimation();
            mBluetoothConnectionService.write("3".getBytes());
            mTvCommand.setText("Great Job ... waiting for next question");

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothConnectionService.write("4".getBytes());
                    randomCommand();
                }
            }, 2000);

            Log.d(PHONE_TAG, "great");
        }
        else {
            mediaWrong.start();
            serverMoved = false;
            clientMoved = false;

            mTvCommand.setText("Game Over, Click Start to restart");
            mBluetoothConnectionService.write(ClientServerMessageString.GAME_OVER.getBytes());
            imagePhone.clearAnimation();


            new AlertDialog.Builder(ServerBluetoothChallengeGame.this)
                    .setCancelable(false)
                    .setTitle("Game Over")
                    .setMessage("You too slow \nPress Start to restart")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            imagePhone.clearAnimation();
                        }
                    }).show();

            Log.d(PHONE_TAG, "no");
        }

    }

    public synchronized void serverMotion()
    {
        mNumberCommand = -1;

        serverTime = System.currentTimeMillis();
        serverMoved = true;

        if(clientMoved && serverMoved)
        {
            bothPhoneMoved();
        }

    }

    public synchronized void clientMotion(long time)
    {
        clientTime = System.currentTimeMillis();
        clientMoved = true;
        if(clientMoved && serverMoved)
            bothPhoneMoved();
    }
}
