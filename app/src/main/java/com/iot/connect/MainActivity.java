package com.iot.connect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.iot.connect.cakeshop.Settings;
import com.iot.connect.control.Command;
import com.iot.connect.control.ModulesControl;
import com.iot.connect.control.SensorControl;


public class MainActivity extends Activity implements
        View.OnClickListener, SensorControl.LedListener,SensorControl.MotorListener,SensorControl.TempHumListener,SensorControl.LightSensorListener{

    private static final String TAG = "MainActivity";
    public static final String resume_action = "com.topelec.buscard.resume_action";
    public static final String recharge_action = "com.topelec.buscard.recharge_action";

    private static final int REQ_SYSTEM_SETTINGS = 1;
    private int flag = 0; // 通过标记跳转不同的页面，显示不同的菜单项

    private int Temp;
    private int Hum;

    private boolean fanStatus;

    private TextView tempView;
    private TextView humView;

    private ImageButton btnStart;

    private Button btnSettings;
    private Button btnBack;
    private boolean isAutoTempHum;
    private int settingTemperature;
    private int settingHumidity;


    SensorControl mSensorControl;
    ModulesControl mModulesControl;

    Intent intent;

    /**
     * 用于更新UI
     */
    Handler myHandler = new Handler(getMainLooper()) {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            Bundle data;
            data = msg.getData();
            switch (msg.what) {
                case 0x03:
                    switch (data.getByte("senser_id") ) {
                        case 0x01:
                            Temp = data.getInt("senser_data");
                            tempView.setText(String.valueOf(Temp));
                            //如下温度自动化管理代码
                            if (isAutoTempHum) {
                                if (Temp > settingTemperature) {
                                    //温度大于设定值，降低温度，执行打开空调
                                    if (!fanStatus) {
                                        mSensorControl.fanForward(true);
                                    };
                                } else {
                                    //实时温度小于设定值，停止降低温度，如果此时空调是运行状态，则执行停止空调
                                    if (fanStatus) {
                                        mSensorControl.fanStop(true);
                                    }
                                }
                            }
                            break;
                        case 0x02:
                            Hum = data.getInt("senser_data");
                            humView.setText(String.valueOf(Hum));
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    Handler uiHandler = new Handler(getMainLooper()) {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            Bundle data;
            if (flag == 0) {//resume
                intent = new Intent(resume_action);
            }else if (flag == 1){
                intent = new Intent(recharge_action);
            }
            switch (msg.what) {
                //判断发送的消息
                case Command.HF_TYPE:  //设置卡片类型TypeA返回结果  ,错误类型:1
                    data = msg.getData();
                    if (data.getBoolean("result") == false) {
                        intent.putExtra("what",1);
                        intent.putExtra("Result",getResources().getString(R.string.buscard_type_a_fail));
                        sendBroadcast(intent);
                    }
                    break;
                case  Command.HF_FREQ:  //射频控制（打开或者关闭）返回结果   ,错误类型:1
                    data = msg.getData();
                    if (data.getBoolean("result") == false) {
                        intent.putExtra("what",1);
                        if (data.getBoolean("Result")) {
                            intent.putExtra("Result",getResources().getString(R.string.buscard_frequency_open_fail));
                        }else {
                            intent.putExtra("Result",getResources().getString(R.string.buscard_frequency_close_fail));
                        }
                        sendBroadcast(intent);
                    }

                    break;
                case Command.HF_ACTIVE:       //激活卡片，寻卡，返回结果
                    data = msg.getData();
                    if (data.getBoolean("result")) {
//                        hfView.setText(R.string.active_card_succeed);
                    } else {
                        intent.putExtra("what",2);
                        sendBroadcast(intent);

                    }

                    break;
                case Command.HF_ID:      //防冲突（获取卡号）返回结果
                    data = msg.getData();
                    intent.putExtra("what",3);
                    if (data.getBoolean("result")) {
                        intent.putExtra("Result",data.getString("cardNo"));
                        sendBroadcast(intent);
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //定义发送任务定时器
    Handler timerHandler = new Handler();
    Runnable sendRunnable = new Runnable() {
        int i = 1;
        @Override
        public void run() {
            //查询温度湿度
            switch (i) {
                case 1:
                    mSensorControl.checkTemperature(true);
                    i++;
                    break;
                case 2:
                    mSensorControl.checkHumidity(true);
                    i++;
                    break;
                case 3:
                    mSensorControl.checkBrightness(true);
                    i = 1;
                    break;
                default:
                    break;
            }
            timerHandler.postDelayed(this,Command.CHECK_SENSOR_DELAY);
        }
    };

    private static final boolean AUTO_HIDE = true;
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private final Handler mHideHandler = new Handler();

    //UI全屏显示
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };


    //显示程序中的底部控制按钮
    private View mControlsView;
    private View mBackView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            mControlsView.setVisibility(View.VISIBLE);
            mBackView.setVisibility(View.VISIBLE);
        }
    };


    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smarthome);
        initialization();
        initSettings();
        mVisible = true;
        mControlsView = findViewById(R.id.smarthome_settings);
        mContentView = findViewById(R.id.smarthome_content);
        mBackView = findViewById(R.id.smarthome_back);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mSensorControl = new SensorControl();
        mSensorControl.addLedListener(this);
        mSensorControl.addMotorListener(this);
        mSensorControl.addTempHumListener(this);
        mSensorControl.addLightSensorListener(this);

        mModulesControl = new ModulesControl(uiHandler);
        mModulesControl.actionControl(true);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
    }

    private void hide() {

        mControlsView.setVisibility(View.GONE);
        mBackView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        mVisible = true;
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void initialization() {
        Temp = 101;
        Hum = 101;

        fanStatus = false;

        tempView = (TextView) findViewById(R.id.tempView);
        humView = (TextView) findViewById(R.id.humView);

        btnStart = (ImageButton) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);

        btnSettings = (Button) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(this);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                if (fanStatus)
                {
                    mSensorControl.fanStop(false);
                }else {
                    mSensorControl.fanForward(false);
                }
                break;
            case R.id.btnSettings:
                startActivityForResult(new Intent(this, Settings.class), REQ_SYSTEM_SETTINGS);
                break;
            case R.id.btnBack:
                finish();
                break;
        }
    }

    private void initSettings() {
        PreferenceManager.setDefaultValues(this,R.xml.settings_smarthome,false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        isAutoTempHum = settings.getBoolean("auto_temp_switch",true);
        settingTemperature = Integer.parseInt(settings.getString("temp_settings","27"));
        settingHumidity = Integer.parseInt(settings.getString("hum_settings","40"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_SYSTEM_SETTINGS) {
            PreferenceManager.setDefaultValues(this,R.xml.settings_smarthome,false);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            isAutoTempHum = settings.getBoolean("auto_temp_switch",true);
            settingTemperature = Integer.parseInt(settings.getString("temp_settings","27"));
            settingHumidity = Integer.parseInt(settings.getString("hum_settings","40"));

        }

    }

    /**
     * 由不可见变为可见时调用
     */
    @Override
    protected void onStart() {
        super.onStart();

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mSensorControl.actionControl(true);

        //TODO:每350ms发送一次数据
        timerHandler.postDelayed(sendRunnable, Command.CHECK_SENSOR_DELAY);
    }
    /**
     * 在完全不可见时调用
     */
    @Override
    protected void onStop() {
        super.onStop();
        timerHandler.removeCallbacks(sendRunnable);
        mSensorControl.actionControl(false);
    }

    /**
     * 在活动销毁时调用
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorControl.removeLedListener(this);
        mSensorControl.removeMotorListener(this);
        mSensorControl.removeTempHumListener(this);
        mSensorControl.removeLightSensorListener(this);
        mSensorControl.closeSerialDevice();

        mModulesControl.actionControl(false);
        mModulesControl.closeSerialDevice();
    }

    @Override
    public void LedControlResult(byte led_id,byte led_status) {

        Message msg = new Message();
        msg.what = 0x01;
        Bundle data = new Bundle();
        data.putByte("led_id",led_id);
        data.putByte("led_status",led_status);
        msg.setData(data);
        myHandler.sendMessage(msg);
    }

    @Override
    public void motorControlResult(byte motor_status) {

        Message msg =  new Message();
        msg.what = 0x02;
        Bundle data = new Bundle();
        data.putByte("motor_status",motor_status);
        msg.setData(data);
        myHandler.sendMessage(msg);
    }

    @Override
    public void tempHumReceive(byte senser_id,int senser_data) {

        Message msg  = new Message();
        msg.what = 0x03;
        Bundle data = new Bundle();
        data.putByte("senser_id",senser_id);
        data.putInt("senser_data",senser_data);
        msg.setData(data);
        myHandler.sendMessage(msg);
    }

    @Override
    public void lightSensorReceive(byte senser_status) {

        Message msg = new Message();
        msg.what = 0x04;
        Bundle data = new Bundle();
        data.putByte("senser_status",senser_status);
        msg.setData(data);
        myHandler.sendMessage(msg);
    }
}
