package com.pluscubed.anticipate.util;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.pluscubed.anticipate.R;

public enum AnimationStyle {

    NONE(2, R.drawable.ic_android_black_24dp, R.string.none),
    BOTTOM(0, R.drawable.ic_expand_less_black_24dp, R.string.slide_bottom),
    RIGHT(1, R.drawable.ic_chevron_left_black_24dp, R.string.slide_right),
    LOLLIPOP(3, R.drawable.ic_expand_less_black_24dp, R.string.slide_bottom_android);

    public int id;
    @DrawableRes
    public int icon;
    @StringRes
    public int name;

    AnimationStyle(int id, @DrawableRes int icon, int name) {
        this.id = id;
        this.icon = icon;
        this.name = name;
    }

    public static AnimationStyle valueWithId(int id) {
        for (AnimationStyle style : values()) {
            if (style.id == id) {
                return style;
            }
        }
        return null;
    }
}
