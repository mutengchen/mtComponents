package com.qbaor.mtcomponents.BatteryManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;



/**
 * 电池管理服务，动态获取电池状态和当前生命周期
 */
public class BatteryManagerService extends Service {
    private static BatteryManagerService service;
    private NotificationManager notificationManager;
    public  static int battery_status = 0;
    public static boolean battery_showing = false;
    public static int cur_electrity = 20;
    public static boolean isCharging = false;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static BatteryManagerService getService() {
        if (service == null) service = new BatteryManagerService();
        return service;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart: ");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("" + Const.Battery_channel, "MMA_LOCATION_CHECK", NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
            startForeground(1, getNotification());
        }
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mIntentReciver, mIntentFilter);
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.sprd.validationtools.drt_dev_return_value");
        registerReceiver(devIntentTestReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if(mIntentReciver!=null)
            unregisterReceiver(mIntentReciver);
        if(devIntentTestReceiver!=null)
            unregisterReceiver(devIntentTestReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Battery SERVICE")
                .setPriority(Notification.PRIORITY_LOW)
                .setContentText("");

        //设置Notification的ChannelID,否则不能正常显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("" + Const.Battery_channel);
        }
        Notification notification = builder.build();
        return notification;
    }

    //TODO 获取当前的电池电量
    //TODO 判断当前电池是否处于充电状态中
    //TODO
    private String TAG = BatteryManagerService.class.getSimpleName();
    private BroadcastReceiver mIntentReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra("status", 0);
            int level = intent.getIntExtra("level", 0);
            cur_electrity = level;
            //如果电量小于20的话，就显示要充电
            //通知自定义状态栏更新电量
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
//                    Log.d(TAG, "onReceive: charging"+level);
                    if (level > 24 && level < 100) {
                        if(battery_showing||battery_status==Const.Battery.CHARGE)return;
                        else{
                            battery_status = Const.Battery.CHARGE;
                        }
                    } else if (level == 100) {
                        if(battery_showing||battery_status == Const.Battery.FULL)return;
                        else{
                            battery_status = Const.Battery.FULL;

                        }
                    }else if(level<25){
                        if(level<=10){
                            if(battery_showing||battery_status==Const.Battery.NO)return;
                            else{
                                battery_status = Const.Battery.NO;
//                                alertIntent.putExtra("status", Const.Battery.NO);
//                                alertIntent.putExtra("from",1);
//                                alertIntent.putExtra("level", level);
                            }
                        }else{
                            if(battery_showing||battery_status==Const.Battery.LOW)return;
                            else{
                                battery_status = Const.Battery.LOW;

                            }
                        }
                    }
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
//                    Log.d(TAG, "onReceive: discharging");
                    break;
                default:
                    //如果是未充电状态的话，ch
                    if (level <= 10) {
                        if(battery_status==Const.Battery.NO)return;
                        else{
                            battery_status = Const.Battery.NO;
                            Intent powerIntent = new Intent(getApplicationContext(), LowPowerDialog.class);
                            powerIntent.putExtra("status",2);
                            powerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(powerIntent);
                        }
                    }else if(level<=24){
                        if(battery_status==Const.Battery.LOW)return;
                        else{
                            battery_status = Const.Battery.LOW;
                            Intent powerIntent = new Intent(getApplicationContext(), LowPowerDialog.class);
                            powerIntent.putExtra("status",1);
                            powerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(powerIntent);
                        }
                    }
                    break;
            }


        }
    };


    //温度检测广播
    private BroadcastReceiver devIntentTestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.sprd.validationtools.drt_dev_return_value")) {
                try {
                    int dev_id = Integer.parseInt(intent.getStringExtra("dev_id"));

                } catch (Exception e) {
                }
            }
        }
    };

}
