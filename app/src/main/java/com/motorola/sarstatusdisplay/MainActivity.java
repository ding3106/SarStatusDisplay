package com.motorola.sarstatusdisplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.motorola.sarcontrol.ISarStatusManager;
import com.motorola.sarcontrol.IOnSarStatusChangedListener;
import com.motorola.sarcontrol.SarStatus;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MESSAGE_PROPERTY_INFO_UPDATE = 1;


    ISarStatusManager sarStatusManager;

    private List<String> sarStatusList = new ArrayList<>();
    private List<String> tunerStatusList = new ArrayList<>();
    private List<String> wifiStatusList = new ArrayList<>();
    private List<String> sensorStatusList = new ArrayList<>();


    private SarStateAdapter adapter_sar;
    private WifiStateAdapter adapter_wifi;
    private SarStateAdapter adapter_tuner;
    private SarStateAdapter adapter_sensor;

    private AudioManager audioManager;
    private boolean savedIsSpeakerPhoneOn;
    private int savedAudioMode;


    //Android L之后，非显式地在Intent中指定Service的情况下，需要执行setPackage，否则无法绑定Service
    private Intent intent_SarStatusProviderService = new Intent("android.intent.action.SAR_STATUS_DISPLAY").setPackage("com.motorola.sarcontrol");
    //private Intent intent_new = new Intent().setComponent(new ComponentName("com.motorola.sarcontrol", "com.motorola.sarcontrol.SarStatusDebuggerService"));


    private Handler mHandle = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_PROPERTY_INFO_UPDATE:
                    SarStatus propertyInfo = (SarStatus)msg.obj;
                    updateStatusDisplay(propertyInfo);//更新显示Server端传递过来的SarStatus数据
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {//在主线程被回调
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected() called with: componentName = [" + componentName + "], iBinder = [" + iBinder + "]");
            if(iBinder == null){
                Log.w(TAG, "onServiceConnected: iBinder = null." );
                Toast.makeText(MainActivity.this, "绑定返回值binder出现异常\n 请重启app", Toast.LENGTH_SHORT).show();
                return;
            }

            sarStatusManager = ISarStatusManager.Stub.asInterface(iBinder);
            try {
                iBinder.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                //初始化界面显示，并向Service中注册监听
                SarStatus propertyInfo = (SarStatus) sarStatusManager.getSarStatus();
                updateStatusDisplay(propertyInfo);
                sarStatusManager.registerListener(mOnSarStatusChangedListener);

            } catch (Exception e) {//不使用RemoteException，防止SarStatus数据结构不匹配时App crash
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "绑定SarStatusDebuggerService失败", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(MainActivity.this, "成功绑定", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {//Service被意外销毁时才被调用，正常unbind和stopService不会引起其被调用
            Log.d(TAG, "onServiceDisconnected() called with: componentName = [" + componentName + "]");

        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {//在binder线程池被回调
            Log.d(TAG, "binderDied() called");

            if(sarStatusManager == null)
                return;
            sarStatusManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
            sarStatusManager = null;
            bindService(intent_SarStatusProviderService, mServiceConnection, BIND_AUTO_CREATE);

        }
    };

    private IOnSarStatusChangedListener mOnSarStatusChangedListener = new IOnSarStatusChangedListener.Stub(){

        @Override
        public void onSarStatusChanged(SarStatus propertyInfo) throws RemoteException {

            Log.d(TAG, "onSarStatusChanged() called with: propertyInfo = [" + propertyInfo + "]");
            mHandle.obtainMessage(MESSAGE_PROPERTY_INFO_UPDATE, propertyInfo).sendToTarget();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
        savedAudioMode = audioManager.getMode();

        final Switch audioSwitch = (Switch) findViewById(R.id.switch_audio);
        audioSwitch.setChecked(false);
        audioSwitch.setSwitchTextAppearance(this,R.style.s_false);
        audioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //控制开关字体颜色
                if (b) {
                    Log.w(TAG, "onCheckedChanged: on");
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.setSpeakerphoneOn(false);
                    audioSwitch.setSwitchTextAppearance(MainActivity.this,R.style.s_true);
                }else {
                    Log.w(TAG, "onCheckedChanged: off");
                    audioManager.setMode(savedAudioMode);
                    audioManager.setSpeakerphoneOn(savedIsSpeakerPhoneOn);
                    audioSwitch.setSwitchTextAppearance(MainActivity.this,R.style.s_false);
                }

            }
        });


        //初始化待显示的数据
        initSarStatusList();
        initWifiStatusList();
        initTunerStatusList();
        initSensorStatusList();


        RecyclerView recyclerView_sar = (RecyclerView)findViewById(R.id.recycler_view_sar);
        RecyclerView recyclerView_tuner = (RecyclerView)findViewById(R.id.recycler_view_turner);
        RecyclerView recyclerView_wifi = (RecyclerView)findViewById(R.id.recycler_view_wifi);
        RecyclerView recyclerView_sensor = (RecyclerView)findViewById(R.id.recycler_view_sensor);

        //SAR list控件设置
        LinearLayoutManager layoutManager_sar = new LinearLayoutManager(this);
        recyclerView_sar.setLayoutManager(layoutManager_sar);
        recyclerView_sar.addItemDecoration(new MyDividerItemDecoration(this, layoutManager_sar.getOrientation()));
        adapter_sar = new SarStateAdapter(sarStatusList);
        recyclerView_sar.setAdapter(adapter_sar);

        //WIFI list控件设置
        LinearLayoutManager layoutManager_wifi = new LinearLayoutManager(this);
        recyclerView_wifi.setLayoutManager(layoutManager_wifi);
        recyclerView_wifi.addItemDecoration(new MyDividerItemDecoration(this, layoutManager_wifi.getOrientation()));
        adapter_wifi = new WifiStateAdapter(wifiStatusList);
        recyclerView_wifi.setAdapter(adapter_wifi);

        //TUNER list控件设置
        LinearLayoutManager layoutManager_tuner = new LinearLayoutManager(this);
        recyclerView_tuner.setLayoutManager(layoutManager_tuner);
        recyclerView_tuner.addItemDecoration(new MyDividerItemDecoration(this, layoutManager_tuner.getOrientation()));
        adapter_tuner = new SarStateAdapter(tunerStatusList);
        recyclerView_tuner.setAdapter(adapter_tuner);

        //SENSOR list控件设置
        LinearLayoutManager layoutManager_sensor = new LinearLayoutManager(this);
        recyclerView_sensor.setLayoutManager(layoutManager_sensor);
        recyclerView_sensor.addItemDecoration(new MyDividerItemDecoration(this, layoutManager_sensor.getOrientation()));
        adapter_sensor = new SarStateAdapter(sensorStatusList);
        recyclerView_sensor.setAdapter(adapter_sensor);

        try {//潜在的无访问权限导致的异常
            bindService(intent_SarStatusProviderService, mServiceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        if(sarStatusManager != null && sarStatusManager.asBinder().isBinderAlive()){
            try {
                //解除注册
                sarStatusManager.unregisterListener(mOnSarStatusChangedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        unbindService(mServiceConnection);
        super.onDestroy();
    }


    //更新display
    private void updateStatusDisplay(SarStatus sarStatus){

        Log.d(TAG, "updateStatusDisplay() called with: propertyInfo = [" + sarStatus.toString() + "]");

        updateSarStatusList(sarStatus);
        adapter_sar.notifyDataSetChanged();

        updateWifiStatusList(sarStatus);
        adapter_wifi.notifyDataSetChanged();

        updateTunerStatusList(sarStatus);
        adapter_tuner.notifyDataSetChanged();

        updateSensorStatusList(sarStatus);
        adapter_sensor.notifyDataSetChanged();

    }



    private void initWifiStatusList(){
        //WIFI
        wifiStatusList.add("//WIFI");
        wifiStatusList.add("WifiCutbackUsed: ");
        wifiStatusList.add("mWifiTxPower: ");
        wifiStatusList.add("Wifi: ");
        wifiStatusList.add("MiFi: ");
        wifiStatusList.add("WiFiDirect: ");
        wifiStatusList.add("VoiceCall: ");
        wifiStatusList.add("DataCall: ");
        wifiStatusList.add("Connection: ");
        wifiStatusList.add("EarPiece: ");
        wifiStatusList.add("OB5State: ");
        wifiStatusList.add("WifiBand: ");
        wifiStatusList.add("MifiBand: ");
    }



    private void updateWifiStatusList(SarStatus sarStatus){
        wifiStatusList.clear();
        //WIFI
        wifiStatusList.add("//WIFI");
        wifiStatusList.add("WifiCutbackUsed: " + sarStatus.mWifiCutbackUsed);
        wifiStatusList.add("WifiTxPower: " + sarStatus.mWifiTxPower);
        wifiStatusList.add("Wifi: " + sarStatus.mWifi);
        wifiStatusList.add("MiFi: " + sarStatus.mMifi);
        wifiStatusList.add("WiFiDirect: " + sarStatus.mWifiDirect);
        wifiStatusList.add("VoiceCall: " + sarStatus.mVoiceCall);
        wifiStatusList.add("DataCall: " + sarStatus.mDataCall);
        wifiStatusList.add("Connection: " + sarStatus.mConnection);
        wifiStatusList.add("EarPiece: " + sarStatus.mEarPiece);
        wifiStatusList.add("OB5State: " + sarStatus.mOB5State);
        wifiStatusList.add("WifiBand: " + sarStatus.mWifiBand);
        wifiStatusList.add("MifiBand: " + sarStatus.mMifiBand);
    }

    private void initTunerStatusList(){
        //TUNER
        tunerStatusList.add("//TUNER");
        tunerStatusList.add("TunerState: ");
        tunerStatusList.add("EarPiece: ");
        tunerStatusList.add("ProximitySensor: ");
        tunerStatusList.add("Charger: ");
        tunerStatusList.add("Headset: ");
    }

    private void updateTunerStatusList(SarStatus sarStatus){

        tunerStatusList.clear();
        //TUNER
        tunerStatusList.add("//TUNER");
        tunerStatusList.add("TunerState: " + sarStatus.mTunerState);
        tunerStatusList.add("EarPiece: " + sarStatus.mEarPiece);
        tunerStatusList.add("ProximitySensor: " + sarStatus.mPsensorNear);
        tunerStatusList.add("Charger: " + sarStatus.mChargerConnected);
        tunerStatusList.add("Headset: " + sarStatus.mHeadsetConnected);

    }

    private void initSensorStatusList(){
        //SENSOR
        sensorStatusList.add("//SENSOR");
        sensorStatusList.add("SensorState[]: ");
    }

    private void updateSensorStatusList(SarStatus sarStatus){

        sensorStatusList.clear();
        //SENSOR
        sensorStatusList.add("//SENSOR");

        StringBuilder string_mSensorState = new StringBuilder("");
        if(sarStatus.mSensorState != null){
            for(int i = 0; i < sarStatus.mSensorState.length; i++ ){
                string_mSensorState.append(sarStatus.mSensorState[i] + " ");
            }
        }

        sensorStatusList.add("SensorState[]: " + string_mSensorState);//33

    }

    private void initSarStatusList(){
        //SAR
        sarStatusList.add("//SAR");
        sarStatusList.add("SarState: ");
        sarStatusList.add("EarPiece: ");
        sarStatusList.add("VoiceCall: ");
        sarStatusList.add("DataCall: ");
        sarStatusList.add("Connection: ");
        sarStatusList.add("Wifi: ");
        sarStatusList.add("MiFi: ");
        sarStatusList.add("WiFiDirect: ");
        sarStatusList.add("AirPlaneMode: ");
        sarStatusList.add("RfCable: ");
        sarStatusList.add("OB5State: ");
        sarStatusList.add("TX0State: ");
    }

    private void updateSarStatusList(SarStatus sarStatus){
        sarStatusList.clear();
        //SAR
        sarStatusList.add("//SAR");
        sarStatusList.add("SarState: " + sarStatus.mSarState);
        sarStatusList.add("EarPiece: " + sarStatus.mEarPiece);
        sarStatusList.add("VoiceCall: " + sarStatus.mVoiceCall);
        sarStatusList.add("DataCall: " + sarStatus.mDataCall);
        sarStatusList.add("Connection: " + sarStatus.mConnection);
        sarStatusList.add("Wifi: " + sarStatus.mWifi);
        sarStatusList.add("MiFi: " + sarStatus.mMifi);
        sarStatusList.add("WiFiDirect: " + sarStatus.mWifiDirect);
        sarStatusList.add("AirPlaneMode: " + sarStatus.mAirPlaneMode);
        sarStatusList.add("RfCable: " + sarStatus.mRfCable);
        sarStatusList.add("OB5State: " + sarStatus.mOB5State);
        sarStatusList.add("TX0State: " + sarStatus.mTX0State);

    }

}



