package com.example.tongyu.strokefighter.BluetoothGame;

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
import com.example.tongyu.strokefighter.Services.BluetoothConnectionService;
import com.example.tongyu.strokefighter.Services.ClientServerMessageString;
import com.example.tongyu.strokefighter.R;

import java.util.UUID;

/**
 * Created by tongyu on 11/27/17.
 */
public class ClientBluetoothGameView extends AppCompatActivity implements SensorEventListener{
    final String TAG = "ClientBluetoothGameView";

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
    private Sensor mAccelerometer, mLinearAccelerometer;
    private SensorManager mSensorManager;
    private TextView mTvCommand, mTvScore, mTvQuestion, mTvMode;
    private Button mBtnTimer, mBtnNewGame, mBtnPause;
    private int mNumberCommand;
    public Object accLock = new Object();
    public Object linearLock = new Object();
    private CountDownTimer mCountDownTimer;
    float last_x, last_y, last_z;

    Button btnNewGame;
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
            else if (messageId == 2)
                onReceiveCommandMessage(msgBack);
            else if (messageId == 3)
                onReceiveCorrectMessage(msgBack);
            else if (messageId == 4)
                onReceiveTimeOutMessage(msgBack);
            else if (messageId == 6)
                onReceiveGameOverMessage();

        }
    };

    public void onReceiveStartMessage(String msg)
    {
        new AlertDialog.Builder(ClientBluetoothGameView.this)
                .setCancelable(false)
                .setTitle("Start Game?")
                .setMessage("Server wants to start the game")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_READY.getBytes());
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_NOT_READY.getBytes());
                    }})
                .show();

    }
    public void onReceiveCommandMessage(String msg)
    {
        String commandString = msg.substring(1);
        mNumberCommand = Integer.valueOf(commandString);

        imagePhone.clearAnimation();
        switch(mNumberCommand){

            case 1:
                mTvCommand.setText("Server: Tip Left");
                break;

            case 2:
                mTvCommand.setText("Server: Tip Right");
                break;

            case 3:
                mTvCommand.setText("Server: Screen Up");
                break;

            case 4:
                mTvCommand.setText("Server: Screen Down");
                break;
            case 5:
                mTvCommand.setText("Server: Move Left");
                break;
            case 6:
                mTvCommand.setText("Server: Move Right");
                break;
            case 7:
                mTvCommand.setText("Server: Move Up");
                break;
            case 8:
                mTvCommand.setText("Server: Move Down");
                break;
            case 9:
                mTvCommand.setText("Server: Move Towards you");
                break;
            case 10:
                mTvCommand.setText("Server: Move away from you");
                break;
            case 11:
                mTvCommand.setText("Client: Tip Left");
                rotateAnimation = new RotateAnimation(0, -90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(3000);  // animation duration
                rotateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(rotateAnimation);
                break;
            case 12:
                mTvCommand.setText("Client: Tip Right");
                rotateAnimation = new RotateAnimation(0, 90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(3000);  // animation duration
                rotateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(rotateAnimation);
                break;
            case 13:
                mTvCommand.setText("Client: Screen Up");
                break;
            case 14:
                mTvCommand.setText("Client: Screen Down");
                break;
            case 15:
                mTvCommand.setText("Client: Move Left");
                translateAnimation = new TranslateAnimation(0.0f, -300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 16:
                mTvCommand.setText("Client: Move Right");
                translateAnimation = new TranslateAnimation(0.0f, 300f, 0.0f, 0f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 17:
                mTvCommand.setText("Client: Move Up");
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, -300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 18:
                mTvCommand.setText("Client: Move Down");
                translateAnimation = new TranslateAnimation(0.0f, 0f, 0.0f, 300f); //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
                translateAnimation.setDuration(3000);  // animation duration
                translateAnimation.setRepeatCount(Animation.INFINITE);  // animation repeat count
                imagePhone.setAnimation(translateAnimation);
                break;
            case 19:
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
            case 20:
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

    public void onReceiveTimeOutMessage(String msg)
    {

    }
    public void onReceiveCorrectMessage(String msg)
    {
        updateScore(1);
    }
    public void onReceiveGameOverMessage()
    {
        new AlertDialog.Builder(ClientBluetoothGameView.this)
                .setCancelable(false)
                .setTitle("Connection Lost")
                .setMessage("Going back to connection page")
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ClientBluetoothGameView.this, ConnectionView.class);
                        startActivity(intent);
                    }
                }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_view_bluetooth);

        // initialize buttons and text views
        mBtnTimer = (Button) findViewById(R.id.btn_time);
        mTvCommand = (TextView) findViewById(R.id.tv_server_command);
        mTvScore = (TextView) findViewById(R.id.tv_score_server);
        mTvQuestion = (TextView) findViewById(R.id.tv_questionNumber);
        imagePhone = (ImageView) findViewById(R.id.img_phone);
        mTvMode = (TextView) findViewById(R.id.tv_bluetooth_mode);
        mTvMode.setText("Causal Mode");
        findViewById(R.id.img_start).setVisibility(View.INVISIBLE);

        // initilaize the senors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        mLinearAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).get(0);
        // add sensor to listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Set up the threads
        mBluetoothDevice = getIntent().getExtras().getParcelable("bluetoothdevice");
        Log.d(TAG, mBluetoothDevice.getName() + " " + mBluetoothDevice.getAddress());

        mBluetoothConnectionService = new BluetoothConnectionService(ClientBluetoothGameView.this, mHandler);
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

                if (Math.abs(xx) > 5 || Math.abs(yy) > 5 || Math.abs(zz) > 5) {
                    if (Math.abs(xx) > Math.abs((yy)) && Math.abs(xx) > Math.abs(zz)) {
                        if (xx > 0) {
                            if (mNumberCommand == 15) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
                                Log.d("Movement", "update left move");
                            }
                            // Log.d("Movement", "Move left");
                        } else {
                            if (mNumberCommand == 16) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
                                Log.d("Movement", "update right move");
                            }
                            // Log.d("Movement", "Move Right");
                        }
                    } else if (Math.abs(yy) > Math.abs((xx)) && Math.abs(yy) > Math.abs(zz)) {
                        if (yy > 0) {
                            if (mNumberCommand == 18) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
                                Log.d("Movement", "update down move");
                            }
                            // Log.d("Movement", "Move Down");
                        } else {
                            if (mNumberCommand == 17) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
                                Log.d("Movement", "update up move");
                            }
                            // Log.d("Movement", "Move Up");
                        }
                    } else if (Math.abs(zz) > Math.abs((xx)) && Math.abs(zz) > Math.abs(yy)) {
                        if (zz > 0) {
                            if (mNumberCommand == 19) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
                                Log.d("Movement", "update move towards you");
                            }
                            //Log.d("Movement", "Move Front");
                        } else {
                            if (mNumberCommand == 20) {
                                mNumberCommand = -1;
                                mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                                updateScore(1);
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
                    if (mNumberCommand == 11) {
                        mNumberCommand = -1;
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                        updateScore(1);
                        Log.d("Movement", "update left shake");
                    }
                } else if (Round(x, 4) < -10.0000) {
                    if (mNumberCommand == 12) {
                        mNumberCommand = -1;
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                        updateScore(1);
                        Log.d("Movement", "update right shake");

                    }
                } else if (Round(z, 4) > 10.0000) {
                    if (mNumberCommand == 13) {
                        mNumberCommand = -1;
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                        updateScore(1);
                        Log.d("Movement", "update screen up");
                    }
                } else if (Round(z, 4) < -10.0000) {
                    if (mNumberCommand == 14) {
                        mNumberCommand = -1;
                        mBluetoothConnectionService.write(ClientServerMessageString.CLIENT_CORRECT.getBytes());
                        updateScore(1);
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
}
