package com.ble.demo.ui;

import android.app.Dialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ble.api.DataUtil;
import com.ble.ble.oad.OADListener;
import com.ble.ble.oad.OADProxy;
import com.ble.ble.oad.OADStatus;
import com.ble.ble.oad.OADType;
import com.ble.demo.MainActivity;
import com.ble.demo.R;
import com.ble.demo.adapter.LogListAdapter;
import com.ble.demo.util.LeProxy;
import com.ble.gatt.GattAttributes;
import com.ble.utils.TimeUtil;
import com.ble.utils.ToastUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * CC2541的OAD需判断镜像类别是A还是B，只有类别不同才可以升级
 * <p>
 * 本Demo给出的几个发送间隔仅供参考
 */
public class OADActivity extends AppCompatActivity implements OnClickListener, OADListener {
    private final String TAG = "OADActivity";

    private static final int REQ_FILE_PATH = 1;

    private static final int MSG_OAD_IMAGE_TYPE = 8;
    private static final int MSG_OAD_PREPARED = 9;
    private static final int MSG_OAD_PROGRESS_CHANGED = 10;
    private static final int MSG_OAD_INTERRUPT = 11;
    private static final int MSG_OAD_FINISH = 12;
    private static final int MSG_OAD_STATUS = 13;

    private static final String EXTRA_IMAGE_TYPE = "extra_image_type";
    private static final String EXTRA_I_BYTES = "extra_i_bytes";
    private static final String EXTRA_N_BYTES = "extra_n_bytes";
    private static final String EXTRA_MILLISECONDS = "extra_milliseconds";

    private final List<String> mAssetsFiles = new ArrayList<>();

    private TextView mTvConnectionState;
    private TextView mTvTargetImageType;
    private TextView mTvProgress;
    private TextView mTvBytes;
    private TextView mTvTime;
    private TextView mTvFilePath;
    private ProgressBar mProgressBar;
    private Button mBtnStart;
    private ListView mLogList;
    private LogListAdapter mLogListAdapter;

    private int mSendInterval = 20;//发送间隔
    private String mDeviceName;
    private String mDeviceAddress;
    private String mFilePath;
    private final ProgressInfo mProgressInfo = new ProgressInfo();
    private OADProxy mOADProxy;//升级的关键类
    private LeProxy mLeProxy;

    private static class ProgressInfo {
        int iBytes;
        int nBytes;
        long milliseconds;
    }

    private Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        final WeakReference<OADActivity> weakReference;

        MyHandler(OADActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(android.os.Message msg) {

            OADActivity activity = weakReference.get();
            if (activity == null) {
                return;
            }

            Bundle data = msg.getData();
            String s = null;
            switch (msg.what) {
                case MSG_OAD_PREPARED:
                    activity.mBtnStart.setText(R.string.oad_cancel);
                    // 准备就绪，开始升级
                    activity.mOADProxy.startProgramming(activity.mSendInterval);
                    s = "OAD Prepared";
                    break;

                case MSG_OAD_FINISH:
                case MSG_OAD_INTERRUPT:
                    activity.displayData(data);
                    activity.mBtnStart.setText(R.string.oad_start);
                    break;

                case MSG_OAD_PROGRESS_CHANGED:
                    activity.displayData(data);
                    break;

                case MSG_OAD_STATUS:
                    s = msg.arg1 + " [" + OADStatus.getMessage(msg.arg1) + "]";
                    break;
                default:
                    break;
            }

            activity.appendLog(s);
        }
    }


    private void displayData(Bundle data) {
        mProgressInfo.iBytes = data.getInt(EXTRA_I_BYTES);
        mProgressInfo.nBytes = data.getInt(EXTRA_N_BYTES);
        mProgressInfo.milliseconds = data.getLong(EXTRA_MILLISECONDS);

        updateProgressUi();
    }

    private void updateProgressUi() {
        long seconds = mProgressInfo.milliseconds / 1000;

        int progress = 0;
        if (mProgressInfo.nBytes != 0) {
            progress = 100 * mProgressInfo.iBytes / mProgressInfo.nBytes;
        }
        String time = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        String bytes = mProgressInfo.iBytes / 1024 + "KB/" + mProgressInfo.nBytes / 1024 + "KB";

        mTvProgress.setText(progress + "%");
        mTvTime.setText(time);
        mTvBytes.setText(bytes);
        mProgressBar.setProgress(progress);
    }

    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);
            if (!address.equals(mDeviceAddress)) {
                return;
            }

            String s = null;
            switch (intent.getAction()) {
                case LeProxy.ACTION_GATT_DISCONNECTED:// 断线
                    mTvConnectionState.setText(R.string.disconnected);
                    s = "Disconnected";
                    break;

                case LeProxy.ACTION_GATT_CONNECTED:
                    mTvConnectionState.setText(R.string.connected);
                    s = "Connected";
                    break;

                case LeProxy.ACTION_DATA_AVAILABLE: {
                    String uuid = intent.getStringExtra(LeProxy.EXTRA_UUID);
                    byte[] data = intent.getByteArrayExtra(LeProxy.EXTRA_DATA);
                    if (GattAttributes.TI_OAD_Image_Identify.toString().equals(uuid)) {
                        short ver = DataUtil.buildUint16(data[1], data[0]);
                        Character imgType = ((ver & 1) == 1) ? 'B' : 'A';
                        // 显示模块当前程序的镜像类型（A/B）
                        mTvTargetImageType.setText("Target Image Type: " + imgType);

                    } else if (GattAttributes.TI_OAD_Image_Block.toString().equals(uuid)) {
                        s = "OAD Block Rx: " + DataUtil.byteArrayToHex(data);
                        Log.e(TAG, s);
                    }
                }
                break;

                case LeProxy.ACTION_MTU_CHANGED:
                    int mtu = intent.getIntExtra(LeProxy.EXTRA_MTU, 23);
                    s = "MTU Changed: " + mtu;
                    break;
                default:
                    break;
            }

            appendLog(s);
        }
    };

    private void appendLog(String s) {
        if (s != null) {
            mLogListAdapter.add(TimeUtil.timestamp("HH:mm:ss.SSS - ") + s);
            mLogList.setSelection(mLogListAdapter.getCount() - 1);
        }
    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LeProxy.ACTION_GATT_CONNECTED);
        filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
        filter.addAction(LeProxy.ACTION_MTU_CHANGED);
        filter.addAction(LeProxy.ACTION_DATA_AVAILABLE);
        return filter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oad);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            String[] assetsFiles = getAssets().list("");
            for (String name : assetsFiles) {
                if (name.endsWith(".bin")) {
                    mAssetsFiles.add(name);
                }
            }
            mAssetsFiles.add("本地文件");

        } catch (Exception e) {
            e.printStackTrace();
        }


        mDeviceName = getIntent().getStringExtra(MainActivity.EXTRA_DEVICE_NAME);
        mDeviceAddress = getIntent().getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        initView();

        mLeProxy = LeProxy.getInstance();
        /*if (Build.VERSION.SDK_INT >= 21) {
            BluetoothGatt gatt = mLeProxy.getBluetoothGatt(mDeviceAddress);
            if (gatt != null)
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }*/
        mOADProxy = mLeProxy.getOADProxy(this, OADType.cc2640_r2_oad);//TODO 升级类型，依模块型号而定
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, makeFilter());

        //这一步只有CC2541 OAD才需要
        new Timer().schedule(new GetTargetImgInfoTask(mDeviceAddress), 100, 100);
    }

    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setSubtitle(mDeviceAddress);

        mTvConnectionState = (TextView) findViewById(R.id.oad_tv_state);
        mTvTargetImageType = (TextView) findViewById(R.id.oad_tv_image_type);
        mTvProgress = (TextView) findViewById(R.id.oad_tv_progress);
        mTvBytes = (TextView) findViewById(R.id.oad_tv_bytes);
        mTvTime = (TextView) findViewById(R.id.oad_tv_time);
        mTvFilePath = (TextView) findViewById(R.id.oad_tv_filepath);
        mProgressBar = (ProgressBar) findViewById(R.id.oad_progressBar);
        mBtnStart = (Button) findViewById(R.id.oad_btn_start);

        mTvFilePath.setText(mFilePath);
        mBtnStart.setOnClickListener(this);

        updateProgressUi();

        findViewById(R.id.oad_btn_load_file).setOnClickListener(this);

        // 发送间隔
        final String[] intervals = getResources().getStringArray(R.array.oad_send_interval_values);
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, intervals);
        Spinner intervalSpinner = (Spinner) findViewById(R.id.oad_sp_send_interval);
        intervalSpinner.setAdapter(intervalAdapter);
        intervalSpinner.setSelection(1);// 默认20ms
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = intervals[position];
                mSendInterval = Integer.valueOf(s.substring(0, s.indexOf("ms")));
                Log.i(TAG, "发送间隔：" + mSendInterval + "ms");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mLogListAdapter = new LogListAdapter(this);
        mLogList = (ListView) findViewById(R.id.logList);
        mLogList.setAdapter(mLogListAdapter);
    }

    private class GetTargetImgInfoTask extends TimerTask {
        int i = 0;
        BluetoothGatt gatt;
        BluetoothGattCharacteristic charIdentify;
        BluetoothGattCharacteristic charBlock;

        GetTargetImgInfoTask(String address) {
            gatt = mLeProxy.getBluetoothGatt(address);
            if (gatt != null) {
                BluetoothGattService oadService = gatt.getService(GattAttributes.TI_OAD_Service);
                if (oadService != null) {
                    charIdentify = oadService.getCharacteristic(GattAttributes.TI_OAD_Image_Identify);
                    charIdentify.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    mLeProxy.enableNotification(gatt, charIdentify);

                    charBlock = oadService.getCharacteristic(GattAttributes.TI_OAD_Image_Block);
                }
            }
        }

        @Override
        public void run() {
            if (charIdentify != null) {
                switch (i) {
                    case 0:
                        charIdentify.setValue(new byte[]{0});
                        Log.e(TAG, "write 0: " + gatt.writeCharacteristic(charIdentify));
                        break;

                    case 1:
                        charIdentify.setValue(new byte[]{1});
                        Log.e(TAG, "write 1: " + gatt.writeCharacteristic(charIdentify));
                        break;

                    default:
                        Log.w(TAG, "$GetTargetImgInfoTask.cancel(): " + cancel());
                        break;
                }
            } else {
                cancel();
            }
            i++;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 重写返回键事件
        if (mOADProxy.isProgramming()) {
            ToastUtil.show(this, R.string.oad_programming);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        mOADProxy.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.oad_btn_load_file:// 加载升级文件
                showLoadFileMenu();
                break;

            case R.id.oad_btn_start:
                if (mOADProxy.isProgramming()) {
                    // 取消升级
                    mOADProxy.stopProgramming();
                } else {
                    // 开始升级
                    if (mFilePath != null) {
                        boolean isAssets = mAssetsFiles.contains(mFilePath);
                        mOADProxy.prepare(mDeviceAddress, mFilePath, isAssets);
                    } else {
                        ToastUtil.show(this, R.string.oad_please_select_a_image);
                    }
                }
                break;
        }
    }

    /**
     * 加载升级文件
     */
    private void showLoadFileMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView menuList = new ListView(this);

        menuList.setAdapter(new ArrayAdapter<>(this, R.layout.text_view, mAssetsFiles));
        menuList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (position < mAssetsFiles.size() - 1) {
                    // 加载assets文件
                    mFilePath = mAssetsFiles.get(position);
                    mTvFilePath.setText(mFilePath);
                } else {
                    // 加载本地文件（Download目录）
                    startActivityForResult(new Intent(OADActivity.this, FileActivity.class), REQ_FILE_PATH);
                }
                dialog.dismiss();
            }
        });
        dialog.setContentView(menuList);
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQ_FILE_PATH) {
            String filepath = data.getStringExtra(FileActivity.EXTRA_FILE_PATH);
            if (filepath != null) {
                mFilePath = filepath;
                mTvFilePath.setText(mFilePath);
            }
            Log.e(TAG, "########### " + mFilePath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPrepared(String address) {
        // 准备就绪，开始升级
        mHandler.sendEmptyMessage(MSG_OAD_PREPARED);
    }

    @Override
    public void onFinished(String address, int nBytes, long milliseconds) {
        // 升级完毕，这里只是APP端发送完所有有数据
        handleMessage(MSG_OAD_FINISH, nBytes, nBytes, milliseconds);
    }

    @Override
    public void onInterrupted(String address, int iBytes, int nBytes, long milliseconds) {
        // 升级异常中断
        handleMessage(MSG_OAD_INTERRUPT, iBytes, nBytes, milliseconds);
    }

    @Override
    public void onProgressChanged(String address, int iBytes, int nBytes, long milliseconds) {
        // 升级进度
        handleMessage(MSG_OAD_PROGRESS_CHANGED, iBytes, nBytes, milliseconds);
    }

    @Override
    public void onBlockWrite(byte[] arg0) {
        //升级过程中发的数据，可忽略
    }

    // R2 OAD 才有状态回调
    @Override
    public void onStatusChange(String address, int status) {
        if (status == OADStatus.SUCCESS) {
            Log.i(TAG, "升级成功");
        } else {
            Log.e(TAG, "升级异常：" + OADStatus.getMessage(status));
        }
        Message msg = new Message();
        msg.what = MSG_OAD_STATUS;
        msg.arg1 = status;
        mHandler.sendMessage(msg);
    }

    /**
     * @param what
     * @param iBytes       已经升级（发送）的字节数
     * @param nBytes       总的字节数
     * @param milliseconds 升级时间（ms）
     */
    private void handleMessage(int what, int iBytes, int nBytes, long milliseconds) {
        Bundle data = new Bundle();
        data.putInt(EXTRA_I_BYTES, iBytes);
        data.putInt(EXTRA_N_BYTES, nBytes);
        data.putLong(EXTRA_MILLISECONDS, milliseconds);
        Message msg = new Message();
        msg.what = what;
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
}