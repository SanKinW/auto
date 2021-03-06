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

    private int Temp;
    private int Hum;

    private boolean fanStatus;
    private boolean isBright;

    private ImageButton btnLed1;
    private ImageButton btnLed2;
    private ImageButton btnLed3;
    private ImageButton btnLed4;

    private TextView tempView;
    private TextView humView;
    private TextView moneyView;

    private ImageView fanView;
    private ImageButton btnStart;

    private ImageView brightnessView;

    private Button btnSetting;
    private Button btnBack;
    private boolean isAutoTempHum;
    private boolean isAutoBrightness;
    private int settingTemperature;
    private int settingHumidity;


    SensorControl mSensorControl;

    private static final int TEMPERATURE_SENSOR = 31;
    private static final int HUMIDITY_SENSOR = 32;
    private static final int BRIGHTNESS_SENSOR = 40;

    static final String AutoListKey = "setting_list";
    private String toControl;//1——充值；2——消费
    private String settingMoney;

    /**
     * 用于更新UI
     */
    Handler myHandler = new Handler() {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            Bundle data;
            data = msg.getData();
            switch (msg.what) {
                //判断发送的消息
                case 0x01:
                    switch (data.getByte("led_id")) {
                        case 0x01:
                            if (data.getByte("led_status") == 0x01) {
                                btnLed1.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                isLed1On = true;
                            }else {
                                btnLed1.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                isLed1On = false;
                            }
                            break;
                        case 0x02:
                            if (data.getByte("led_status") == 0x01) {
                                btnLed2.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                isLed2On = true;
                            }else {
                                btnLed2.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                isLed2On = false;
                            }
                            break;
                        case 0x03:
                            if (data.getByte("led_status") == 0x01) {
                                btnLed3.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                isLed3On = true;
                            } else {
                                btnLed3.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                isLed3On = false;
                            }
                            break;
                        case 0x04:
                            if (data.getByte("led_status") == 0x01) {
                                btnLed4.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                isLed4On = true;
                            }else {
                                btnLed4.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                isLed4On = false;
                            }
                            break;
                        case 0x05:
                            if (data.getByte("led_status") == 0x01) {
                                btnLed1.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                btnLed2.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                btnLed3.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                btnLed4.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_on));
                                isLed1On = true;
                                isLed2On = true;
                                isLed3On = true;
                                isLed4On = true;
                            } else {
                                btnLed1.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                btnLed2.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                btnLed3.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                btnLed4.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_led_off));
                                isLed1On = false;
                                isLed2On = false;
                                isLed3On = false;
                                isLed4On = false;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case 0x02:
                    if (data.getByte("motor_status") == 0x01) {
                        fanView.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_motor_start));
                        btnStart.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_btn_motor_on));
                        fanStatus = true;
                    }else {
                        fanView.setImageDrawable(null);
                        btnStart.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_btn_motor_off));
                        fanStatus = false;
                    }
                    break;
                case 0x03:
                    switch (data.getByte("senser_id") ) {
                        case 0x01:
                            Temp = data.getInt("senser_data");
                            tempView.setText(String.valueOf(Temp));
                            //如下温度自动化管理代码
                            if (isAutoTempHum) {
                                if (Temp > settingTemperature) {
                                    //TODO 温度大于设定值，降低温度，执行打开风扇动作
                                    if (!fanStatus) {
                                        mSensorControl.fanForward(true);
                                    };
                                } else {
                                    //TODO 实时温度小于设定值，停止降低温度，如果此时风扇是运行状态，则执行停止风扇动作。
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
                case 0x04:
                    if (data.getByte("senser_status") == 0x01) {
                        brightnessView.setImageDrawable(getResources().getDrawable((R.drawable.smarthome_bright)));
                        if (isAutoBrightness)
                            mSensorControl.allLeds_Off(true);
                    } else {
                        brightnessView.setImageDrawable(getResources().getDrawable(R.drawable.smarthome_dark));
                        if (isAutoBrightness)
                            mSensorControl.allLeds_On(true);
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
            //TODO:查询温度湿度
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

        setContentView(R.layout.activity_cakeshop);
        initialization();
        initSettings();
        mVisible = true;
        mControlsView = findViewById(R.id.cakeshopcard_action_settings);
        mContentView = findViewById(R.id.cakeshop_content);


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

    private void initialization(){
        Temp = 101;
        settingTemperature=101;

        fanStatus = false;

        moneyView = (TextView) findViewById(R.id.moenyView);

        btnSetting = (Button) findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.btnSettings:
                startActivityForResult(new Intent(this, Settings.class), REQ_SYSTEM_SETTINGS);
                break;
        }
    }

    private void initSettings() {

        PreferenceManager.setDefaultValues(this,R.xml.settings_cakeshop,false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        toControl = settings.getString(AutoListKey, "1");
        settingMoney = Integer.parseInt(settings.getString("money_settings","40"));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_SYSTEM_SETTINGS) {
            PreferenceManager.setDefaultValues(this,R.xml.settings_cakeshop,false);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            toControl = settings.getString(AutoListKey, "1");
            settingMoney = Integer.parseInt(settings.getString("money_settings","40"));
            moneyView.setText(String.valueOf(settingMoney));
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
