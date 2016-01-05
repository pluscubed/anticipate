package com.pluscubed.anticipate.toolbarcolor;

import android.support.annotation.ColorInt;

import com.afollestad.inquiry.Inquiry;

public class WebsiteToolbarDbUtil {

    public static final String TABLE_TOOLBAR_COLOR = "WebsiteToolbarColor";

    public static void insertColor(WebsiteToolbarColor websiteToolbarColor) {
        if (getColor(websiteToolbarColor.hostDomain) == -1) {
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
        WebsiteToolbarColor websiteToolbarColor = Inquiry.get()
                .selectFrom(TABLE_TOOLBAR_COLOR, WebsiteToolbarColor.class)
                .where("host_domain = ?", url)
                .one();
        return websiteToolbarColor != null ? websiteToolbarColor.toolbarColor : -1;
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
