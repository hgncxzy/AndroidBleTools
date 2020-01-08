package com.ble.demo.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.ble.demo.R;

/**
 * 16进制或ASCII码字符过滤
 */
public class HexAsciiWatcher implements TextWatcher {
    public static final int HEX = 0;
    public static final int ASCII = 1;

    private Context context;
    private EditText host;
    private TextView indicator;
    private int textType = HEX;
    private int maxLength = 40;

    public HexAsciiWatcher(Context context) {
        this.context = context;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * @param maxLength must > 0
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * @param textType {@link HexAsciiWatcher#HEX} or {@link HexAsciiWatcher#ASCII}
     */
    public void setTextType(int textType) {
        this.textType = textType;
    }

    public void setHost(EditText host) {
        this.host = host;
    }

    public void setIndicator(TextView indicator) {
        this.indicator = indicator;
    }

    public void setIndicatorText(String text) {
        this.indicator.setText(text);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // 过滤字符
        filterCharSequence(s);
        String data = s.toString().trim();

        if (data.length() < maxLength + 1) {
            int len = data.length();
            if (textType == HEX) {
                len = len % 2 == 1 ? len / 2 + 1 : len / 2;
            }
            if (indicator != null) {
                indicator.setText(context.getString(R.string.input_bytes, len));
            }

        } else {
            // 多余的字符全部清掉
            s.delete(maxLength, s.length());
            if (host != null && context != null) {
                // 提示输入字节数已达上限
                if (textType == HEX) {
                    host.setError(context.getResources().getString(R.string.max_bytes, maxLength / 2));
                } else {
                    host.setError(context.getResources().getString(R.string.max_bytes, maxLength));
                }
            }
        }
    }

    private void filterCharSequence(Editable s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (textType) {
                case HEX:
                    // 0-9 48-57
                    // A-F 65-70
                    // a-z 97-102
                    if ((c > 47 && c < 58) || (c > 64 && c < 71) || (c > 96 && c < 103)) {
                        //
                    } else {
                        s.delete(i, i + 1);
                        i--;
                    }
                    break;

                case ASCII:
                    // 32-126范围之外的都是乱码字符或是无法输入的字符
                    if (c >= 0 && c <= 127) {
                        //
                    } else {
                        s.delete(i, i + 1);
                        i--;
                    }
                    break;
            }
        }
    }

}
