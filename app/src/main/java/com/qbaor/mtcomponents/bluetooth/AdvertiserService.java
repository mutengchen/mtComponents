package com.qbaor.mtcomponents.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;



import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by cmt
 * 蓝牙模块从设备，发布成服务
 */

public class AdvertiserService extends Service {

    private static final String TAG = "AdvertiserService";
    private static final int FOREGROUND_NOTIFICATION_ID = 1000010;
    public static boolean isConnectBlue = false;
    public static boolean keyCorrect = false;
    public final static String KEY_SECRET = "U2FsdGVkX183hgb4TM8zRuX0zUSKAYGfYrZj6Gq2ZEQ";

    public static boolean running = false;
    public static final String ADVERTISENG_FILED = "com.examle.lihong.bluetoothadvertisement.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int ADVERTISING_TIMED_OUT = 6;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdertiseCallback;
    private Handler mHandler;
    private Runnable timeoutRunnable;
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattCharacteristic characteristicRead;

    BluetoothManager mBluetoothManager;

    private static UUID UUID_SERVER = UUID.fromString("00001238-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARREAD = UUID.fromString("00007238-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//
//    private static UUID UUID_SERVER = UUID.fromString("10000000-0000-0000-0000-202111091001");
//    private static UUID UUID_CHARREAD = UUID.fromString("11000000-0000-0000-0000-202111091001");
//    private static UUID UUID_CHARWRITE = UUID.fromString("12000000-0000-0000-0000-202111091001");
//    private static UUID UUID_DESCRIPTOR = UUID.fromString("11100000-0000-0000-0000-202111091001");

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        running = true;
//        EventBus.getDefault().register(this);
//      setNotification();
        initialize();
        startAdvertising();
        setTimeout();
        super.onCreate();

    }

    NotificationManager notificationManager;

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, ErCodeScanActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("BlueTooth SERVICE")
                .setContentIntent(pendingIntent)
                .setContentText("");

        //设置Notification的ChannelID,否则不能正常显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("" + Const.BLE_channel);
        }
        Notification notification = builder.build();
        return notification;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onCreate: ");

        running = false;
        stopAdvertising();
        mHandler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialize() {
        Log.d(TAG, "initialize: ");
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, "设备不支持蓝牙广播", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setTimeout() {
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "广播服务已经运行" + TIMEOUT + "秒，停止停止广播");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    private void startAdvertising() {
        goForeground();
        Log.d(TAG, "服务开始广播");
        if (mAdertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
//            AdvertiseData scanRespone = buildScanResponseData();
            mAdertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdertiseCallback);
            }
        }
    }

    private void goForeground() {
        Log.d(TAG, "goForegroud运行过了");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("" + Const.BLE_channel, "蓝牙通道", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(FOREGROUND_NOTIFICATION_ID, getNotification());
    }

    private void stopAdvertising() {
        Log.d(TAG, "服务停止广播");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdertiseCallback);
            mAdertiseCallback = null;
            isConnectBlue = false;
            keyCorrect = false;
        }
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceData(new ParcelUuid(UUID_SERVER), new byte[]{23, 33});
        return dataBuilder.build();
    }

    public AdvertiseData buildScanResponseData() {
        AdvertiseData.Builder scan = new AdvertiseData.Builder();
        scan.addManufacturerData(2, new byte[]{66, 66});
//                    .addServiceUuid(new ParcelUuid(UUID_SERVER));
        return scan.build();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return settingsBuilder.build();
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "广播失败" + errorCode);
            sendFailureIntent(errorCode);
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "服务端的广播成功开启");
            Log.d(TAG, "BLE服务的广播启动成功后：TxPowerLv=" + settingsInEffect.getTxPowerLevel() + "；mode=" + settingsInEffect.getMode() + "；timeout=" + settingsInEffect.getTimeout());
            initServices(getContext());//该方法是添加一个服务，在此处调用即将服务广播出去
        }
    }

    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(ADVERTISENG_FILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    //添加一个服务，该服务有一个读特征、该特征有一个描述；一个写特征。
    //用BluetoothGattServer添加服务，并实现该类的回调接口
    private void initServices(Context context) {
        mBluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        //蓝牙外围服务
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //读特征通道
        characteristicRead = new BluetoothGattCharacteristic(UUID_CHARREAD,BluetoothGattCharacteristic.PROPERTY_WRITE|
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        //通知
        BluetoothGattDescriptor clientConfigurationDescriptor = new BluetoothGattDescriptor(
                UUID_DESCRIPTOR, BluetoothGattDescriptor.PERMISSION_WRITE);
        clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        //为读通道加上通知
        characteristicRead.addDescriptor(clientConfigurationDescriptor);


        //特征添加到服务里面
        service.addCharacteristic(characteristicRead);
        mBluetoothGattServer.addService(service);
        Log.d(TAG, "初始化服务成功：initServices ok");
    }

    //服务事件的回调
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        //1、首先是连接状态的回调
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG, "连接状态发生改变，安卓系统回调onConnectionStateChange:device name=" + device.getName() + "address=" + device.getAddress() + "status=" + status + "newstate=" + newState);
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    isConnectBlue = false;
                    keyCorrect = false;
//                    AlertUtils.toastAlertInThread("手机与设备断开蓝牙连接！");
                    break;
                case BluetoothProfile.STATE_CONNECTED:
//                    AlertUtils.toastAlertInThread("手机与设备蓝牙建立连接！");


                    isConnectBlue = true;
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "客户端有读的请求，安卓系统回调该onCharacteristicReadRequest()方法");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        //接受具体字节，当有特征被写入时，回调该方法，写入的数据为参数中的value
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "客户端有写的请求，安卓系统回调该onCharacteristicWriteRequest()方法" + device.getAddress());
            //特征被读取，在该回调方法中回复客户端响应成功
            String msg = null;
            try {
                msg = new String(value, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
//            AlertUtils.toastAlert(msg);
//            if (keyCorrect) {
//                AlertUtils.toastAlertInThread("写入信息：" + msg);
//                ClientDataDto clientDataDto = GsonUtils.jsonToObject(msg, ClientDataDto.class);
//                SharePreferencesUtils.getInstance().putDto("app_info", clientDataDto);
//                Intent intent = new Intent(getContext(), StartCheckActivity.class);
//                intent.putExtra("username", clientDataDto.getInfo().getName());
//                startActivity(intent);
//            } else {
//                if (msg.equals(KEY_SECRET)) {
//                    keyCorrect = true;
//                    //秘钥验证正确，向小程序发送device_id
//                    if (characteristicRead != null) {
//                        JSONObject params = new JSONObject();
//                        params.put("device_id",NativeUtil.getIMEI(getContext()));
//                        characteristicRead.setValue(params.toJSONString());
//                        if (mBluetoothGattServer != null)
//                            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);//用该方法通知characteristicRead数据发生改变
//                    }
//
//                } else {
//                    AlertUtils.toastAlertInThread("写入密钥不正确:" + msg);
//                }
//            }
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            //处理响应内容
            //value:客户端发送过来的数据
            onResponseToClient(value, device, requestId, characteristic);
        }

        //特征被读取。当回复相应成功后，客户端胡读取然后触发本方法
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "onDescriptorReadRequest: ");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        //2、其次，当有描述请求被写入时，回调该方法，
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onDescriptorWriteRequest: ");
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            onResponseToClient(value, device, requestId, descriptor.getCharacteristic());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d(TAG, "onNotificationSent: ");
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, "添加服务成功，安卓系统回调该onServiceAdded()方法");
        }
    };

    //4.处理相应内容,requestBytes是客户端发送过来的数据
    private void onResponseToClient(byte[] requestBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
        //在服务端接受数据
        Log.d(TAG, "onResponseToClient: " + requestBytes);
        String msg = null;
        try {
            msg = new String(requestBytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "收到：" + msg);
//        characteristicRead.setValue(msg.getBytes());
//        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);//用该方法通知characteristicRead数据发生改变

    }

    private Context getContext() {
        return this;
    }

    public void mockData(String content) {
        Log.d(TAG, "mockData: "+content);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        for (BluetoothDevice bluetoothDevice : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            if (content == null) break;
            Log.d(TAG, "run: result:" + content);
            Log.d(TAG, "mockData: " + bluetoothDevice.getAddress());
            characteristicRead.setValue(content.getBytes());
            mBluetoothGattServer.notifyCharacteristicChanged(bluetoothDevice, characteristicRead, true);
        }
    }

}





