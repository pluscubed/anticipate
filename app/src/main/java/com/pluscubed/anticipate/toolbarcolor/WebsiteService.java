package com.pluscubed.anticipate.toolbarcolor;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Color;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;

public class WebsiteService extends IntentService {

    public static final String EXTRA_HOST = "com.pluscubed.anticipate.EXTRA_HOST";

    public WebsiteService() {
        super("ToolbarColorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra(EXTRA_HOST);

        int color = -1;
        try {
            OkHttpClient client = new OkHttpClient();

            String loadUrl = url;
            if (!loadUrl.startsWith("http://") && !loadUrl.startsWith("https://")) {
                loadUrl = "http://" + loadUrl;
            }

            Response response = client.newCall(new Request.Builder().url(loadUrl).build())
                    .execute();

            BufferedReader stream = new BufferedReader(response.body().charStream());

            String line;
            while ((line = stream.readLine()) != null) {

                if (line.contains("<meta name=\"theme-color\" content=\"#")) {
                    String substring = line.substring(line.indexOf('#'), line.lastIndexOf('"'));
                    color = Color.parseColor(substring);
                    break;
                }

                if (line.contains("</head>")) {
                    break;
                }
            }

            stream.close();

            response.body().close();

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            //Ignore if invalid address/color
        }


        WebsiteToolbarColor websiteToolbarColor = new WebsiteToolbarColor();

        websiteToolbarColor.hostDomain = url;
        websiteToolbarColor.toolbarColor = color;
        websiteToolbarColor.expireTimestamp = System.currentTimeMillis() + 1296000000;

        WebsiteToolbarDbUtil.insertUpdateColor(websiteToolbarColor);


    }
}
