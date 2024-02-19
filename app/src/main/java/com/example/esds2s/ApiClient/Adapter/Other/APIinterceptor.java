package com.example.esds2s.ApiClient.Adapter.Other;

import android.content.Context;

import com.example.esds2s.ApiClient.Adapter.RequestMethod;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;



public class APIinterceptor implements Interceptor {
    private Context context;

    private RequestMethod requestMethod;

    public   void setRequestMethod(RequestMethod requestMethod)
    {
        this.requestMethod= requestMethod;
    }


    public RequestMethod getRequestMethod()
    {
        return requestMethod;
    }


    public APIinterceptor(RequestMethod requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public APIinterceptor(Context context, RequestMethod requestMethod)
    {
        this.requestMethod = requestMethod;
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException
    {
        Request originalRequest = chain.request();
        Request modifiedRequest = originalRequest;
        RequestBody oldBody = modifiedRequest.body();
        Buffer buffer = new Buffer();
        if(oldBody!=null) {
            oldBody.writeTo(buffer);
        }
        String strOldBody = buffer.readUtf8();
        String newBody = null;
        try {
            newBody = strOldBody;
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, newBody);
        if (context != null)
        {
            String token="";
//            if(UserInfo.Exist(context))
//                token = UserInfo.GetUserCardInfo(context).getToken();
//            else
//                token=Hawk.get("Token","");
//            Log.d("Too",token+"");
            if (token != null)
            {
                modifiedRequest = originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer " +token)
                        .build();
            }

        }


        if(requestMethod== RequestMethod.POST)
            originalRequest = modifiedRequest.newBuilder().post(body).build();
        else if(requestMethod== RequestMethod.GET)
            originalRequest = modifiedRequest.newBuilder().get().build();
        else if(requestMethod== RequestMethod.PUT)
            originalRequest = modifiedRequest.newBuilder().put(body).build();
        else if(requestMethod== RequestMethod.DELETE)
            originalRequest = modifiedRequest.newBuilder().delete(body).build();

        return chain.proceed(originalRequest);
    }

}
