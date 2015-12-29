package com.pluscubed.anticipate;

import android.graphics.drawable.Drawable;

import com.afollestad.inquiry.annotations.Column;

public class AppPackage {
    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;

    @Column(name = "package_name")
    public String package_name;

    public Drawable icon;

    public AppPackage() {

    }
}
