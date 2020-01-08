package com.ble.demo.util;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.ble.demo.R;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by JiaJiefei on 2016/8/18.
 */
public class HexKeyboardUtil {

    private static final Character[] HEX_CHARS = new Character[]{'D', 'E', 'F', 'A',
            'B', 'C', '7', '8', '9', '4', '5', '6', '1', '2', '3', 0x1F, '0', 0x1F};

    private KeyboardView keyboardView;
    private Keyboard k1;
    boolean isShow = false;

    private EditText inputEditText;
    private OnDoneListener mOnDoneListener;
    private int maxLength;

    public void setOnDoneListener(OnDoneListener listener) {
        mOnDoneListener = listener;
    }

    public HexKeyboardUtil(Activity act, EditText edit, int maxLength) {
        this.inputEditText = edit;
        this.maxLength = maxLength;

        k1 = new Keyboard(act, R.xml.keyboard);
        keyboardView = (KeyboardView) act.findViewById(R.id.keyboard_view);
        keyboardView.setKeyboard(k1);
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);

        keyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);

        hideSoftKeyboard(act, edit);
    }

    // 隐藏系统的输入键盘
    private void hideSoftKeyboard(Activity ctx, EditText edit) {
        ctx.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        try {
            Method setShowSoftInputOnFocus = EditText.class.getMethod("setShowSoftInputOnFocus", boolean.class);
            setShowSoftInputOnFocus.setAccessible(false);
            setShowSoftInputOnFocus.invoke(edit, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override
        public void swipeUp() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void onText(CharSequence text) {
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onPress(int primaryCode) {
            // checkIShowPreview(primaryCode);
        }

        // 显示预览
        private void checkIShowPreview(int primaryCode) {
            List<Integer> list = Arrays.asList(15, 17);// Del,Done不显示预览
            if (list.contains(primaryCode)) {
                keyboardView.setPreviewEnabled(false);
            } else {
                keyboardView.setPreviewEnabled(true);
            }
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            // checkIShowPreview(primaryCode);
            // Log.i("KeyBoard", "primaryCode=" + primaryCode);
            /**
             * 实体键盘：
             * D    E   F
             * A    B   C
             * 7    8   9
             * 4    5   6
             * 1    2   3
             * del  0   Done
             *
             * Key code:
             * 0    1   2
             * 3    4   5
             * 6    7   8
             * 9    10  11
             * 12   13  14
             * 15   16  17
             */
            Editable editable = inputEditText.getText();
            int start = inputEditText.getSelectionStart();
            if (primaryCode == 17) {// 完成
                // hideKeyboard();
                if (mOnDoneListener != null) {
                    mOnDoneListener.done(inputEditText.getText().toString());
                }

            } else if (primaryCode == 15) {// 回退
                if (editable != null && editable.length() > 0) {
                    if (start > 0) {
                        editable.delete(start - 1, start);
                    }
                }
            } else {
                if (editable.length() >= maxLength) {
                    inputEditText.setError(inputEditText.getContext().getResources().getString(R.string.max_bytes, maxLength / 2));
                } else {
                    editable.insert(start, Character.toString(HEX_CHARS[primaryCode]));
                }
            }
        }
    };

    public void showKeyboard() {
        int visibility = keyboardView.getVisibility();
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            keyboardView.setVisibility(View.VISIBLE);
            isShow = true;
        }
    }

    public void hideKeyboard() {
        int visibility = keyboardView.getVisibility();
        if (visibility == View.VISIBLE) {
            keyboardView.setVisibility(View.GONE);
            isShow = false;
        }
    }

    public interface OnDoneListener {
        void done(String input);
    }
}