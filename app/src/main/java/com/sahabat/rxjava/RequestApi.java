package com.sahabat.rxjava;

import io.reactivex.Flowable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

public interface RequestApi {
    @GET("todos/1")
    Flowable<ResponseBody> makeQuery();
}
