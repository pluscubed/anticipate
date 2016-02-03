package com.pluscubed.anticipate.toolbarcolor;

import android.support.annotation.ColorInt;

import com.afollestad.inquiry.Inquiry;
import com.crashlytics.android.Crashlytics;

public class WebsiteToolbarDbUtil {

    public static final String TABLE_TOOLBAR_COLOR = "WebsiteToolbarColor";

    public static final int NO_COLOR = -1;
    public static final int NOT_FOUND = -2;

    public static void insertUpdateColor(WebsiteToolbarColor websiteToolbarColor) {
        //noinspection ResourceType
        if (getColor(websiteToolbarColor.hostDomain) == NOT_FOUND) {
            Inquiry.get()
                    .insertInto(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                    .values(websiteToolbarColor)
                    .run();
        } else {
            Inquiry.get()
                    .update(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                    .where("host_domain = ?", websiteToolbarColor.hostDomain)
                    .values(websiteToolbarColor)
                    .run();
        }
    }

    @ColorInt
    public static int getColor(String url) {
        try {
            WebsiteToolbarColor websiteToolbarColor = Inquiry.get()
                    .selectFrom(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                    .where("host_domain = ?", url)
                    .one();
            if (websiteToolbarColor != null) {
                return websiteToolbarColor.toolbarColor;
            }

            if (url.startsWith("www.")) {
                url = url.substring(4);
                return getColor(url);
            } else {
                websiteToolbarColor = Inquiry.get()
                        .selectFrom(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                        .where("host_domain = ?", "www." + url)
                        .one();
                //noinspection ResourceAsColor
                return websiteToolbarColor != null ? websiteToolbarColor.toolbarColor : NOT_FOUND;
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
        //noinspection ResourceAsColor
        return NOT_FOUND;
    }

    public static void cleanup() {
        WebsiteToolbarColor[] all = Inquiry.get()
                .selectFrom(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                .all();
        if (all != null) {
            long current = System.currentTimeMillis();
            for (WebsiteToolbarColor color : all) {
                if (color.expireTimestamp < current) {
                    Inquiry.get().deleteFrom(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                            .where("host_domain = ?", color.hostDomain)
                            .run();
                }
            }
        }
    }
}
