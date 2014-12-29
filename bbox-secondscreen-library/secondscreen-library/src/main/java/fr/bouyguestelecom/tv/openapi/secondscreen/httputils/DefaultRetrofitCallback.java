package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import android.util.Log;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by vincent on 22/12/2014.
 */
public class DefaultRetrofitCallback implements Callback<Object> {
    private static final String LOG_TAG = DefaultRetrofitCallback.class.getName();

    private String action;

    public DefaultRetrofitCallback(String requestedAction) {
        action = requestedAction;
    }
    @Override
    public void success(Object o, Response response) {
        Log.d(LOG_TAG, "Unsubscribe HTTP code: " + String.valueOf(response.getStatus()));
        //nothing to do
    }

    @Override
    public void failure(RetrofitError error) {
        int statusCode = 500;
        if (error.getResponse() != null)
            statusCode = error.getResponse().getStatus();

        Log.e(LOG_TAG, "Error while executing action: " + action + ". HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());
    }
}
