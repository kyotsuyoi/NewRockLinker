package com.rocklinker.Common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    //public static final String IP = "187.35.128.157";
    public static final String IP = "192.168.0.99";
    public static final String PORT = "71";
    public static final String BASE_URL = "http://" + IP + ":" + PORT + "/song/";
    public static Retrofit retrofit;

    public  static Retrofit getApiClient(){

        if (retrofit == null){
            Gson gson = new GsonBuilder().setLenient().create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit;
    }

}
