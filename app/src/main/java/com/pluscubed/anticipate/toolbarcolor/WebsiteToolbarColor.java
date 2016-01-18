package com.pluscubed.anticipate.toolbarcolor;

import android.support.annotation.ColorInt;

import com.afollestad.inquiry.annotations.Column;

public class WebsiteToolbarColor {

    @Column(name = "host_domain", primaryKey = true)
    public String hostDomain;

    @ColorInt
    @Column(name = "toolbar_color")
    public int toolbarColor;

    @Column(name = "expire")
    public long expireTimestamp;
}
