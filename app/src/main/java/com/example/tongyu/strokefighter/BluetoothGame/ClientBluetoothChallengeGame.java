package com.example.tongyu.strokefighter.BluetoothGame;

/**
 * Created by tongyu on 12/4/17.
 */

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tongyu.strokefighter.ConnectionView;
import com.example.tongyu.strokefighter.R;
import com.example.tongyu.strokefighter.Services.BluetoothConnectionService;
import com.example.tongyu.strokefighter.Services.ClientServerMessageString;

import java.util.Random;
import java.util.UUID;

/**
 * Created by tongyu on 11/27/17.
 */
public class ClientBluetoothChallengeGame extends AppCompatActivity implements SensorEventListener {
    final String TAG = "ClientBluetoothGameView";
    public Random rand = new Random();

    BluetoothDevice mBluetoothDevice;
    private BluetoothConnectionService mBluetoothConnectionService;
    private final UUID MY_UUID_INSECURE = UUID.fromString("d7579aa2-4a6e-43b7-904c-9b5e9d37de19");

    ImageView imagePhone;
    AnimationSet animationSet;
    RotateAnimation rotateAnimation;
    TranslateAnimation translateAnimation;
    ScaleAnimation scaleAnimation;

    // scores and sensors
    private int mCurrentScore = 0;
    private int mTotalScore = 0;
    private Sensor mLinearAccelerometer;
    private SensorManager mSensorManager;
    private TextView mTvCommand, mTvScore, mTvQuestion, mTvMode;
    private int mNumberCommand, mPrevisousNumberCommand;
    public Object linearLock = new Object();
    private CountDownTimer mCountDownTimer;

    // to handle the message sent from other device
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String msgBack = bundle.getString("key");
            int messageId = Integer.parseInt(String.valueOf(msgBack.charAt(0)));

            // 5 types of messages client will receive
            if(messageId == 1)
                onReceiveStartMessage(msgBack);
            else if (messageId == 3)
                onReceiveCorrectMessage(msgBack);
            else if (messageId == 4)
                onReceiveReRandomMessage(msgBack);
            else if (messageId == 6)
                onReceiveGameOverMessage();

        }
    };

    public void onReceiveStartMessage(String msg)
    {
        new AlertDialog.Builder(ClientBluetoothChallengeGame.this)
                .setCancelable(false)
                .setTitle("Start Game?")
                .setMessage("Server wants to start the game")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_READY.getBytes());

                        mCurrentScore = 0;
                        mTotalScore = 0;
                        mTvScore.setText("0/0");
                        randomCommand();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_NOT_READY.getBytes());
                    }})
                .show();

    }

    public void onReceiveReRandomMessage(String msg)
    {
        randomCommand();
    }
    public void onReceiveCorrectMessage(String msg)
    {
        updateScore(1);
        imagePhone.clearAnimation();
        mTvCommand.setText("Great Job ... waiting for next question");
    }
    public void onReceiveGameOverMessage()
    {
        mTvCommand.setText("Game Over .. Waiting for server");
        imagePhone.clearAnimation();
        new AlertDialog.Builder(ClientBluetoothChallengeGame.this)
                .setCancelable(false)
                .setTitle("Game Over")
                .setMessage("You too slow \nWaiting for the server")
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_view_bluetooth);

        // initialize buttons and text views
        mTvCommand = (TextView) findViewById(R.id.tv_server_command);
        mTvScore = (TextView) findViewById(R.id.tv_score_server);
        mTvQuestion = (TextView) findViewById(R.id.tv_questionNumber);
        imagePhone = (ImageView) findViewById(R.id.img_phone);
        findViewById(R.id.img_start).setVisibility(View.INVISIBLE);
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

        mBluetoothConnectionService = new BluetoothConnectionService(ClientBluetoothChallengeGame.this, mHandler);
        mBluetoothConnectionService.startClient(mBluetoothDevice, MY_UUID_INSECURE);
    }

    @Override
    protected void onDestroy()
    {
        mBluetoothConnectionService.write(ClientServerMessageString.GAME_OVER.getBytes());
        super.onDestroy();
    }
    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
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
                                mNumberCommand = -1;
                                onPhoneMoved();
                                Log.d("Movement", "update left move");
                            }
                            // Log.d("Movement", "Move left");
                        } else {
                            if (mNumberCommand == 6) {
                                mNumberCommand = -1;
                                onPhoneMoved();
                                Log.d("Movement", "update right move");
                            }
                            // Log.d("Movement", "Move Right");
                        }
                    } else if (Math.abs(yy) > Math.abs((xx)) && Math.abs(yy) > Math.abs(zz)) {
                        if (yy > 0) {
                            if (mNumberCommand == 8) {
                                mNumberCommand = -1;
                                onPhoneMoved();
                                Log.d("Movement", "update down move");
                            }
                            // Log.d("Movement", "Move Down");
                        } else {
                            if (mNumberCommand == 7) {
                                mNumberCommand = -1;
                                onPhoneMoved();
                                Log.d("Movement", "update up move");
                            }
                            // Log.d("Movement", "Move Up");
                        }
                    } else if (Math.abs(zz) > Math.abs((xx)) && Math.abs(zz) > Math.abs(yy)) {
                        if (zz > 0) {
                            if (mNumberCommand == 9) {
                                mNumberCommand = -1;
                                onPhoneMoved();
                                Log.d("Movement", "update move towards you");
                            }
                            //Log.d("Movement", "Move Front");
                        } else {
                            if (mNumberCommand == 10) {
                                mNumberCommand = -1;
                                onPhoneMoved();
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void randomCommand()
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
                mTvCommand.setText("Client: Move Left");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, -300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 6:
                mTvCommand.setText("Client: Move Right");
                imagePhone.clearAnimation();
                translateAnimation = new TranslateAnimation(0.0f, 300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 7:
                mTvCommand.setText("Client: Move Up");
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
                mTvCommand.setText("Client: Move Towards you");
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
                mTvCommand.setText("Client: Move away from you");
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

    public void onPhoneMoved()
    {
        long time = System.currentTimeMillis();
        mBluetoothConnectionService.write( (9+","+time).getBytes() );
    }

}
