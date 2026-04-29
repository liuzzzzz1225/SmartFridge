package com.example.myapplication;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import okhttp3.*;

public class BaiduAiHelper {
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String VEGETABLE_DETECT_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient";
    
    private static String accessToken;
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public interface ApiCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public static void getAccessToken(final ApiCallback callback) {
        HttpUrl url = HttpUrl.parse(TOKEN_URL).newBuilder()
                .addQueryParameter("grant_type", "client_credentials")
                .addQueryParameter("client_id", API_KEY)
                .addQueryParameter("client_secret", SECRET_KEY)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("获取token失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonResponse = response.body().string();
                JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
                accessToken = jsonObject.get("access_token").getAsString();
                callback.onSuccess(accessToken);
            }
        });
    }

    public static void detectVegetable(Bitmap bitmap, final ApiCallback callback) {
        if (accessToken == null) {
            callback.onFailure("AccessToken未初始化");
            return;
        }

        // 将Bitmap转换为Base64
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // 构建请求体
        RequestBody requestBody = new FormBody.Builder()
                .add("image", base64Image)
                .build();

        // 构建请求
        HttpUrl url = HttpUrl.parse(VEGETABLE_DETECT_URL).newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("识别失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onSuccess(response.body().string());
            }
        });
    }
} 