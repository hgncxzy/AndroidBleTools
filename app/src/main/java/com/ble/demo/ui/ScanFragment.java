package com.ble.demo.ui;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ble.api.DataUtil;
import com.ble.ble.LeScanRecord;
import com.ble.demo.LeDevice;
import com.ble.demo.R;
import com.ble.demo.util.LeProxy;
import com.ble.utils.ToastUtil;

import java.util.ArrayList;

public class ScanFragment extends Fragment {
    private final static String TAG = "ScanFragment";
    private static final long SCAN_PERIOD = 5000;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private boolean mScanning;
    private LeProxy mLeProxy;

    private SwipeRefreshLayout mRefreshLayout;


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    scanLeDevice(true);
                }
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLeProxy = LeProxy.getInstance();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        getActivity().registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    private void initView(View view) {
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scanLeDevice(true);
            }
        });

        ListView listView = (ListView) view.findViewById(R.id.listView1);
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(mOnItemClickListener);
        listView.setOnItemLongClickListener(mOnItemLongClickListener);
    }

    /**
     * 扫描BLE设备
     *
     * @param enable true开始扫描，false停止扫描
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mBluetoothAdapter.isEnabled()) {
                if (mScanning) {
                    return;
                }
                mScanning = true;
                mRefreshLayout.setRefreshing(true);
                mLeDeviceListAdapter.clear();
                mHandler.postDelayed(mScanRunnable, SCAN_PERIOD);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                ToastUtil.show(getActivity(), R.string.scan_bt_disabled);
            }
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mRefreshLayout.setRefreshing(false);
            mHandler.removeCallbacks(mScanRunnable);
            mScanning = false;
        }
    }

    private final Runnable mScanRunnable = new Runnable() {

        @Override
        public void run() {
            scanLeDevice(false);
        }
    };

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //单击连接设备
            scanLeDevice(false);
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            mLeProxy.connect(device.getAddress(), false);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 233) {
            scanLeDevice(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private final OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            //长按查看广播数据
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            showAdvDetailsDialog(device);
            return true;
        }
    };

    //显示广播数据
    private void showAdvDetailsDialog(LeDevice device) {
        LeScanRecord record = device.getLeScanRecord();

        StringBuilder sb = new StringBuilder();
        sb.append(device.getAddress() + "\n\n");
        sb.append('[' + DataUtil.byteArrayToHex(record.getBytes()) + "]\n\n");
        sb.append(record.toString());

        TextView textView = new TextView(getActivity());
        textView.setPadding(32, 32, 32, 32);
        textView.setText(sb.toString());

        Dialog dialog = new Dialog(getActivity());
        dialog.setTitle(device.getName());
        dialog.setContentView(textView);
        dialog.show();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<LeDevice> mLeDevices;
        private LayoutInflater mInflater;

        LeDeviceListAdapter() {
            mLeDevices = new ArrayList<>();
            mInflater = getActivity().getLayoutInflater();
        }

        void addDevice(LeDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        void clear() {
            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public LeDevice getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {
                view = mInflater.inflate(R.layout.item_device_list, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.txt_rssi);
                viewHolder.connect = (TextView) view.findViewById(R.id.btn_connect);
                viewHolder.connect.setVisibility(View.VISIBLE);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            LeDevice device = mLeDevices.get(i);
            String deviceName = device.getName();
            if (!TextUtils.isEmpty(deviceName)) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRssi.setText("rssi: " + device.getRssi() + "dbm");

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(new LeDevice(device.getName(), device.getAddress(), rssi, scanRecord));
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView connect;
    }
}