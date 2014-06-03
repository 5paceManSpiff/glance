package com.glance.app;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class Request {

    public static final String TAG = "Request";

    public static HttpResponse make (String url, StringEntity input) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        try {
            post.setEntity(input);
            HttpResponse response = client.execute(post);
            return response;
        } catch (ClientProtocolException e) {
            // TODO request protocol exception
        } catch (IOException e) {
            // TODO request io exception
        }

        return null;
    }
}
