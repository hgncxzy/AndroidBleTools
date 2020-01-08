package com.ble.demo.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ble.utils.DimensUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/8/7 0007.
 */

public class LogListAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mLogList;

    public LogListAdapter(Context context) {
        mContext = context;
        mLogList = new ArrayList<>();
    }

    public void add(String s) {
        if (mLogList.size() > 50) mLogList.remove(0);//最多缓存50条数据
        mLogList.add(s);
        notifyDataSetChanged();
    }

    public void clear() {
        mLogList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mLogList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public String getItem(int position) {
        return mLogList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(mContext);
            textView.setTextIsSelectable(true);//设置文字可复制
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            int txtColor = Resources.getSystem().getColor(android.R.color.holo_blue_dark);
            textView.setTextColor(txtColor);
            int padding = DimensUtil.dp2Px(mContext, 6);
            textView.setPadding(padding, 0, padding, 0);
            int w = DimensUtil.getScreenWidth(mContext) - DimensUtil.dp2Px(mContext, 10);
            int h = AbsListView.LayoutParams.WRAP_CONTENT;
            textView.setLayoutParams(new AbsListView.LayoutParams(w, h));
            convertView = textView;
        } else {
            textView = (TextView) convertView;
        }
        textView.setText(getItem(position).trim());
        return convertView;
    }
}