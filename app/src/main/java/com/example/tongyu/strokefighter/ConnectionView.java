package com.example.tongyu.strokefighter;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.tongyu.strokefighter.BluetoothGame.ClientBluetoothChallengeGame;
import com.example.tongyu.strokefighter.BluetoothGame.ClientBluetoothGameView;
import com.example.tongyu.strokefighter.BluetoothGame.ServerBluetoothChallengeGame;
import com.example.tongyu.strokefighter.BluetoothGame.ServerBluetoothGameView;
import com.example.tongyu.strokefighter.Services.BluetoothDeviceListAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by tongyu on 11/23/17.
 */

public class ConnectionView extends AppCompatActivity
{
    private final String TAG = "ConnectionView";
    private BluetoothAdapter mBluetoothAdapter;

    private ListView mListView;
    public BluetoothDeviceListAdapter mBluetoothDeviceListAdapter;

    BluetoothDevice mBTDevice;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    private boolean isServer = false;

    public ImageView imgServer, imgClient, imgPlay;

    private BroadcastReceiver discoverDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            // this whole thing checks if the bluetooth device has a name
            boolean exist = false;
            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, device.getName()  + " " + device.getAddress());

                //
                if(mBTDevices.size() == 0)
                {
                    if(device.getName() == null)
                        exist = true;
                }
                else {
                    for (BluetoothDevice bd : mBTDevices) {
                        if (device.getAddress().equals(bd.getAddress()))
                            exist = true;
                        if (device.getName() == null)
                            exist = true;
                    }
                }

                if(!exist)
                {
                    mBTDevices.add(device);
                    Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                    mBluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(context, R.layout.content_bluetooth_device, mBTDevices);
                    mListView.setAdapter(mBluetoothDeviceListAdapter);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_view);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // set up image views
        imgServer = (ImageView) findViewById(R.id.img_server);
        imgClient = (ImageView) findViewById(R.id.img_client);
        imgPlay = (ImageView) findViewById(R.id.img_play);
        imgServer.setVisibility(View.INVISIBLE);
        imgClient.setVisibility(View.INVISIBLE);
        imgPlay.setVisibility(View.INVISIBLE);

        // set up listviews
        mListView = (ListView)findViewById(R.id.listview_bluetooth);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                mBluetoothAdapter.cancelDiscovery();

                Log.d(TAG, "onItemClick: You Clicked on a device.");
                String deviceName = mBTDevices.get(position).getName();
                String deviceAddress = mBTDevices.get(position).getAddress();

                Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

                //create the bond.
                //NOTE: Requires API 17+? I think this is JellyBean
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                    Log.d(TAG, "Trying to pair with " + deviceName);
                    mBTDevices.get(position).createBond();

                    mBTDevice = mBTDevices.get(position);
                    //mBluetoothConnection = new BluetoothConnectionService(MainActivity.this, mHandler);
                }

                // dialogs to slect device and to be server or client
                selectionDialogs(deviceName, deviceAddress);
            }
        });
        //
    }

    @Override
    protected void onDestroy()
    {
        //unregisterReceiver(discoverDeviceReceiver);
        //mBluetoothAdapter.disable();
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("yes",requestCode + " " + resultCode + " " + RESULT_OK);
        if(requestCode == 100 && resultCode == 300) {
            discoverBluetoothDevices();
        }
    }

    public void selectionDialogs(String deviceName, String deviceAddress)
    {
        new AlertDialog.Builder(ConnectionView.this)
                .setCancelable(false)
                .setTitle("You Select a bluetooth device")
                .setMessage(deviceName +"\n"+deviceAddress +"\nif this is not your device, select another one")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setClientServerVisible();
                        new AlertDialog.Builder(ConnectionView.this)
                                .setCancelable(false)
                                .setTitle("Do you want to be a server or client")
                                .setMessage("")
                                .setPositiveButton("Server", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setServer();
                                        setPlayImageVisible();

                                    }
                                }).setNegativeButton("Client", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setClient();
                                        setPlayImageVisible();
                                    }
                                }).show();
                    }
                }).setNegativeButton("No", null).show();
    }

    public void discoverBluetoothDevices()
    {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
            mBluetoothAdapter.startDiscovery();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkBTPermissions();
            }

            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(discoverDeviceReceiver, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkBTPermissions();
            }
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(discoverDeviceReceiver, discoverDevicesIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    public void onClickEnableDiscover(View v)
    {
        if(!mBluetoothAdapter.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent, 100);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                discoverBluetoothDevices();
            }
        }
    }

    public void onClickServer(View v)
    {
        setServer();
    }

    public void setServer()
    {
        isServer = true;
        imgServer.setPadding(10,10,10,10);
        imgServer.setBackgroundColor(Color.CYAN);
        imgClient.setPadding(0,0,0,0);
    }

    public void onClickClient(View v)
    {
        setClient();
    }

    public void setClient()
    {
        isServer = false;
        imgClient.setPadding(10,10,10,10);
        imgClient.setBackgroundColor(Color.CYAN);
        imgServer.setPadding(0,0,0,0);
    }

    public void setClientServerVisible()
    {
        imgServer.setVisibility(View.VISIBLE);
        imgClient.setVisibility(View.VISIBLE);
    }

    public void setPlayImageVisible()
    {
        imgPlay.setVisibility(View.VISIBLE);
    }

    private int mMode;
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
            mMode = Integer.valueOf(variables[3]);

            Log.d("File Reading stuff", "success = " + readString);
        }
        catch (IOException e)
        {

        }
    }
    public void onClickBluetoothPlay(View v)
    {
        loadFromFile();
        // challenge mode
        if(mMode == 1) {
            if (isServer) {
                Intent intent = new Intent(this, ServerBluetoothChallengeGame.class);
                intent.putExtra("bluetoothdevice", mBTDevice);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ClientBluetoothChallengeGame.class);
                intent.putExtra("bluetoothdevice", mBTDevice);
                startActivity(intent);
            }
        }

        // causal mode
        else if(mMode == 0)
        {
            if (isServer) {
                Intent intent = new Intent(this, ServerBluetoothGameView.class);
                intent.putExtra("bluetoothdevice", mBTDevice);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ClientBluetoothGameView.class);
                intent.putExtra("bluetoothdevice", mBTDevice);
                startActivity(intent);
            }
        }
    }

    public void onClickHomeConnectionView(View v)
    {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        startActivity(upIntent);
    }

}
