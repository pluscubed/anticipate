package com.pluscubed.anticipate.widget;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.TextView;

public class CharLineBreakTextView extends AppCompatTextView {


    public CharLineBreakTextView(Context context) {
        super(context);
    }

    public CharLineBreakTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CharLineBreakTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //http://stackoverflow.com/questions/14373157/dont-wrap-text-in-android-textview-at-period-in-abbreviation
    private static void breakManually(TextView tv, Editable editable) {
        int width = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight();
        if (width == 0 || editable == null) {
            // Can't break with a width of 0.
            return;
        }

        Paint p = tv.getPaint();
        float[] widths = new float[editable.length()];
        p.getTextWidths(editable.toString(), widths);
        float curWidth = 0.0f;
        int lastWSPos = -1;
        int strPos = 0;
        final char newLine = '\n';
        final String newLineStr = "\n";
        boolean reset = false;
        int insertCount = 0;

        //Traverse the string from the start position, adding each character's
        //width to the total until:
        //* A whitespace character is found.  In this case, mark the whitespace
        //position.  If the width goes over the max, this is where the newline
        //will be inserted.
        //* A newline character is found.  This resets the curWidth counter.
        //* curWidth > width.  Replace the whitespace with a newline and reset
        //the counter.

        while (strPos < editable.length()) {
            curWidth += widths[strPos - insertCount];

            char curChar = editable.charAt(strPos);

            if (((int) curChar) == ((int) newLine)) {
                reset = true;
            } else if (Character.isWhitespace(curChar)) {
                lastWSPos = strPos;
            } else if (curWidth > width) {
                if (lastWSPos >= 0) {
                    editable.replace(lastWSPos, lastWSPos + 1, newLineStr);
                    insertCount++;
                    strPos = lastWSPos;
                    lastWSPos = -1;
                    reset = true;
                } else {
                    editable.insert(strPos, newLineStr);
                    insertCount++;
                    lastWSPos = strPos;
                    reset = true;
                }
            }

            if (reset) {
                curWidth = 0.0f;
                reset = false;
            }

            strPos++;
        }

        if (insertCount != 0) {
            tv.setText(editable);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        breakManually(this, getEditableText());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        breakManually(this, getEditableText());
    }
}
