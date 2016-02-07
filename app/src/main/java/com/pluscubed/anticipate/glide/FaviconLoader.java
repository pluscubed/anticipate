package com.pluscubed.anticipate.glide;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

public class FaviconLoader implements ModelLoader<Uri, InputStream> {

    final ModelLoader<GlideUrl, InputStream> urlLoader;

    public FaviconLoader(ModelLoader<GlideUrl, InputStream> urlLoader) {
        this.urlLoader = urlLoader;
    }


    @Override
    public DataFetcher<InputStream> getResourceFetcher(final Uri model, final int width, final int height) {
        return new DataFetcher<InputStream>() {
            private DataFetcher<InputStream> fetcher;

            @Override
            public String getId() {
                return model.getPath();
            }

            @Override
            public InputStream loadData(Priority priority) throws Exception {
                String uriString = "http://icons.better-idea.org/icon?url="
                        + Uri.encode(model.toString()) + "&formats=ico&fallback_icon_url=anticipate&size=";

                try {
                    fetcher = urlLoader.getResourceFetcher(new GlideUrl(uriString + "32"), width, height);
                    return fetcher.loadData(priority);
                } catch (Exception ex) {
                    if (fetcher != null) {
                        fetcher.cleanup();
                    }

                    fetcher = urlLoader.getResourceFetcher(new GlideUrl(uriString + "16"), width, height);
                    return fetcher.loadData(priority);
                }
            }

            @Override
            public void cleanup() {
                if (fetcher != null) {
                    fetcher.cleanup();
                }
            }

            @Override
            public void cancel() {
                if (fetcher != null) {
                    fetcher.cancel();
                }
            }
        };
    }

    public static class Factory implements ModelLoaderFactory<Uri, InputStream> {
        @Override
        public ModelLoader<Uri, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new FaviconLoader(factories.buildModelLoader(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
        }
    }
}
