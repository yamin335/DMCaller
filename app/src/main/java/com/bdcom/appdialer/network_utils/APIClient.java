package com.bdcom.appdialer.network_utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static com.bdcom.appdialer.utils.Constants.API_ROOT_URL;
import static com.bdcom.appdialer.utils.Constants.BASE_URL;

public class APIClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        OkHttpClient httpClient = null;
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        try {
            httpClient =
                    new OkHttpClient.Builder()
                            .addInterceptor(interceptor)
                            .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                            .connectTimeout(25, TimeUnit.SECONDS)
                            .writeTimeout(25, TimeUnit.SECONDS)
                            .readTimeout(25, TimeUnit.SECONDS)
                            .addInterceptor(
                                    new Interceptor() {
                                        @Override
                                        public Response intercept(Chain chain) throws IOException {

                                            Request request =
                                                    chain.request()
                                                            .newBuilder()
                                                            .addHeader("platform", "android")
                                                            .build();
                                            return chain.proceed(request);
                                        }
                                    })
                            .build();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .client(httpClient)
                        .build();
        return retrofit;
    }

    public static Retrofit getApiClient() {
        OkHttpClient httpClient = null;
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        try {
            httpClient =
                    new OkHttpClient.Builder()
                            .addInterceptor(interceptor)
                            .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                            .connectTimeout(25, TimeUnit.SECONDS)
                            .writeTimeout(25, TimeUnit.SECONDS)
                            .readTimeout(25, TimeUnit.SECONDS)
                            .addInterceptor(
                                    chain -> {
                                        Request request =
                                                chain.request()
                                                        .newBuilder()
                                                        .addHeader("platform", "android")
                                                        .build();
                                        return chain.proceed(request);
                                    })
                            .build();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if (httpClient == null) return retrofit;

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(API_ROOT_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .client(httpClient)
                        .build();
        return retrofit;
    }

    private static final Interceptor REWRITE_RESPONSE_INTERCEPTOR =
            chain -> {
                Response originalResponse = chain.proceed(chain.request());
                String cacheControl = originalResponse.header("Cache-Control");

                if (cacheControl == null
                        || cacheControl.contains("no-store")
                        || cacheControl.contains("no-cache")
                        || cacheControl.contains("must-revalidate")
                        || cacheControl.contains("max-age=0")) {
                    return originalResponse
                            .newBuilder()
                            .header("Cache-Control", "public, max-age=" + 10)
                            .build();
                } else {
                    return originalResponse;
                }
            };
}
