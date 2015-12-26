package com.pluscubed.anticipate.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class DispatchBackEditText extends AppCompatEditText {
    public DispatchBackEditText(Context context) {
        super(context);
    }

    public DispatchBackEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DispatchBackEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            clearFocus();
            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
