package com.example.crudmahasiswa.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Physical phone over USB uses 127.0.0.1 with `adb reverse tcp:3000 tcp:3000`.
    // For Android Emulator without adb reverse, use "http://10.0.2.2:3000/".
    private static final String BASE_URL = "http://127.0.0.1:3000/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static String extractErrorMessage(Response<?> response, String fallback) {
        // Retrofit places non-2xx JSON payloads in errorBody() rather than body().
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String raw = errorBody.string();
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> parsed = new Gson().fromJson(raw, mapType);
                if (parsed != null && parsed.get("error") != null) {
                    return parsed.get("error");
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}

