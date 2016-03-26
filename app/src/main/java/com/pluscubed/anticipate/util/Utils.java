package com.pluscubed.anticipate.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;

public abstract class Utils {

    /**
     * Convert a dp float value to pixels
     *
     * @param dp float value in dps to convert
     * @return DP value converted to pixels
     */
    public static int dp2px(Context context, float dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        return Math.round(px);
    }

    public static Bitmap drawableToBitmap(Context context, int resId) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);

        return drawableToBitmap(drawable);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getHost(String host) {
        if (host.startsWith("http://") || host.startsWith("https://")) {
            host = host.substring(host.indexOf("/") + 2);
        }

        int endOfTld = host.indexOf("/");
        if (endOfTld != -1)
            host = host.substring(0, endOfTld);
        return host;
    }

    /**
     * A helper class for providing a shadow on sheets
     */
    @TargetApi(21)
    public static class ShadowOutline extends ViewOutlineProvider {

        int width;
        int height;

        public ShadowOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, width, height);
        }
    }
}