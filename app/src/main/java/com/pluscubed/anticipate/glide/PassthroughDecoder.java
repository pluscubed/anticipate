package com.pluscubed.anticipate.glide;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.util.Util;

import java.io.IOException;

public class PassthroughDecoder implements ResourceDecoder<Drawable, Drawable> {

    @Override
    public Resource<Drawable> decode(Drawable source, int width, int height) throws IOException {
        return new DrawableResource<Drawable>(source) {
            @Override
            public int getSize() {
                if (drawable instanceof BitmapDrawable) {
                    return Util.getBitmapByteSize(((BitmapDrawable) drawable).getBitmap());
                } else {
                    return 1;
                }
            }

            @Override
            public void recycle() {

            }
        };
    }

    @Override
    public String getId() {
        return "Passthrough";
    }
}
