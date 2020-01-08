package com.ble.demo.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.ble.api.DataUtil;
import com.ble.demo.R;
import com.ble.demo.util.LeProxy;
import com.ble.utils.TimeUtil;
import com.ble.utils.ToastUtil;

import java.util.List;

/**
 * 大数据传输，即突破一次只能发送20字节的限制，须手机系统不低于Android5.0
 * Created by Administrator on 2017/8/10 0010.
 */

public class MtuFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MtuFragment";

    private LeProxy mLeProxy;
    private Spinner mSpinnerDevice;
    private EditText mEdtTxData;
    private EditText mEdtMtu;
    private CheckBox mBoxEncrypt;
    private TextView mTxtRxData;
    private String mSelectedAddress;
    private int mDataType = -1;
    private ArrayAdapter<String> mDeviceAdapter;

    private int mMtu = 23;

    private final BroadcastReceiver mGattReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);
            if (LeProxy.ACTION_DATA_AVAILABLE.equals(intent.getAction())) {
                if (address.equals(mSelectedAddress)) {
                    displayRxData(intent);
                }
            } else if (LeProxy.ACTION_MTU_CHANGED.equals(intent.getAction())) {
                int status = intent.getIntExtra(LeProxy.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mMtu = intent.getIntExtra(LeProxy.EXTRA_MTU, 23);
                    updateTxData();
                    ToastUtil.show(getActivity(), "MTU has been " + mMtu);
                } else {
                    ToastUtil.show(getActivity(), "MTU update error: " + status);
                }
            }
        }
    };


    private void displayRxData(Intent intent) {
        String uuid = intent.getStringExtra(LeProxy.EXTRA_UUID);
        byte[] data = intent.getByteArrayExtra(LeProxy.EXTRA_DATA);

        String dataStr = "timestamp: " + TimeUtil.timestamp("MM-dd HH:mm:ss.SSS") + '\n'
                + "uuid: " + uuid + '\n'
                + "length: " + (data == null ? 0 : data.length) + '\n';
        if (mDataType == 0) {
            dataStr += "data: " + DataUtil.byteArrayToHex(data);
        } else {
            if (data == null) {
                dataStr += "data: ";
            } else {
                dataStr += "data: " + new String(data);
            }
        }
        mTxtRxData.setText(dataStr);
    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LeProxy.ACTION_MTU_CHANGED);
        filter.addAction(LeProxy.ACTION_DATA_AVAILABLE);
        return filter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLeProxy = LeProxy.getInstance();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mGattReceiver, makeFilter());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mtu, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSpinnerDevice = (Spinner) view.findViewById(R.id.spinner_device);
        updateConnectedDevices();

        ArrayAdapter<String> dataTypeAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, new String[]{"hex", "ascii"});
        Spinner spinnerDataType = (Spinner) view.findViewById(R.id.spinner_data_type);
        spinnerDataType.setAdapter(dataTypeAdapter);
        spinnerDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mDataType != position) {
                    mDataType = position;
                    updateTxData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mTxtRxData = (TextView) view.findViewById(R.id.txt_rx_data);
        mBoxEncrypt = (CheckBox) view.findViewById(R.id.box_encrypt);
        mBoxEncrypt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLeProxy.setEncrypt(isChecked);
                updateTxData();
            }
        });

        mEdtTxData = (EditText) view.findViewById(R.id.edt_tx_data);
        mEdtMtu = (EditText) view.findViewById(R.id.edt_mtu);
        view.findViewById(R.id.btn_send).setOnClickListener(this);
        view.findViewById(R.id.btn_update_mtu).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        mLeProxy.setEncrypt(mBoxEncrypt.isChecked());
    }

    private void updateTxData() {
        StringBuilder sb = new StringBuilder();
        int max = mBoxEncrypt.isChecked() ? mMtu - 6 : mMtu - 3;//加密要用去3字节

        if (mDataType == 0) {//hex
            for (int i = 0; i < max; i++) {
                sb.append(String.format("%02X", i));
            }
        } else {//ascii
            for (int i = 0; i < max; i++) {
                sb.append((char) (i % 26 + 'A'));
            }
        }
        mEdtTxData.setText(sb.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mGattReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                send();
                break;
            case R.id.btn_update_mtu:
                updateMtu();
                break;
        }
    }

    private void updateMtu() {
        String mtuStr = mEdtMtu.getText().toString();
        if (mtuStr.length() > 0) {
            int mtu = Integer.valueOf(mtuStr);
            Log.i(TAG, "updateMtu() - " + mSelectedAddress + ", mtu=" + mtu);
            mLeProxy.requestMtu(mSelectedAddress, mtu);
        }
    }

    private void send() {
        try {
            String txData = mEdtTxData.getText().toString();
            if (txData.length() > 0) {
                byte[] data;
                if (mDataType == 0) {//hex
                    data = DataUtil.hexToByteArray(txData);
                } else {//ascii
                    data = txData.getBytes();
                }
                mLeProxy.send(mSelectedAddress, data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateConnectedDevices() {
        List<BluetoothDevice> deviceList = mLeProxy.getConnectedDevices();
        String[] deviceArr;
        if (deviceList.size() > 0) {
            deviceArr = new String[deviceList.size()];
            for (int i = 0; i < deviceArr.length; i++) {
                deviceArr[i] = deviceList.get(i).getAddress();
            }
        } else {
            deviceArr = new String[]{getString(R.string.mtu_no_device_connected)};
        }
        mDeviceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, deviceArr);
        mSpinnerDevice.setAdapter(mDeviceAdapter);
        mSpinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedAddress = mDeviceAdapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}