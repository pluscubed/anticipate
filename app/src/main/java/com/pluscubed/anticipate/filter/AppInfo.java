package com.pluscubed.anticipate.filter;

import android.support.annotation.NonNull;

import com.afollestad.inquiry.annotations.Column;

import java.io.Serializable;

public class AppInfo implements Serializable, Comparable<AppInfo> {
    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long dbId;

    @Column(name = "package_name")
    public String packageName;


    public long id;
    public String name;

    public AppInfo() {

    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AppInfo && packageName.equals(((AppInfo) o).packageName);
    }

    @Override
    public int compareTo(@NonNull AppInfo another) {
        return name.compareTo(another.name);
    }
}
