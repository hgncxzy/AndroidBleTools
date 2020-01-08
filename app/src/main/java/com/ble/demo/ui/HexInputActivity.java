package com.ble.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.ble.demo.R;
import com.ble.demo.util.HexAsciiWatcher;
import com.ble.demo.util.HexKeyboardUtil;

/**
 * Created by JiaJiefei on 2016/8/18.
 */
public class HexInputActivity extends AppCompatActivity {
    public static final String EXTRA_HEX_STRING = "extra_hex_string";
    public static final String EXTRA_MAX_LENGTH = "extra_max_length";

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_hex_input);
        getSupportActionBar().setTitle("Hex");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditText = (EditText) findViewById(R.id.editText);
        TextView indicator = (TextView) findViewById(R.id.tv_input_bytes);

        String hexStr = getIntent().getStringExtra(EXTRA_HEX_STRING);
        int len;
        if (!TextUtils.isEmpty(hexStr)) {
            mEditText.setText(hexStr);
            mEditText.setSelection(hexStr.length());
            len = hexStr.length();
            len = len % 2 == 0 ? len / 2 : (len / 2 + 1);
        } else {
            len = 0;
        }
        indicator.setText(getString(R.string.input_bytes, len));

        int maxLength = getIntent().getIntExtra(EXTRA_MAX_LENGTH, 40);

        HexAsciiWatcher watcher = new HexAsciiWatcher(this);
        watcher.setHost(mEditText);
        watcher.setTextType(HexAsciiWatcher.HEX);
        watcher.setMaxLength(maxLength);
        watcher.setIndicator(indicator);
        mEditText.addTextChangedListener(watcher);

        HexKeyboardUtil keyboardUtil = new HexKeyboardUtil(this, mEditText, maxLength);
        keyboardUtil.showKeyboard();
        keyboardUtil.setOnDoneListener(new HexKeyboardUtil.OnDoneListener() {
            @Override
            public void done(String input) {
                Intent data = new Intent();
                data.putExtra(EXTRA_HEX_STRING, input);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}