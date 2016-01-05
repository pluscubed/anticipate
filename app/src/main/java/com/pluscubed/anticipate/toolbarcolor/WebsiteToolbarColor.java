package com.pluscubed.anticipate.toolbarcolor;

import com.afollestad.inquiry.annotations.Column;

public class WebsiteToolbarColor {

    @Column(name = "host_domain", primaryKey = true)
    public String hostDomain;

    @Column(name = "toolbar_color")
    public int toolbarColor;

    @Column(name = "expire")
    public long expireTimestamp;
}
