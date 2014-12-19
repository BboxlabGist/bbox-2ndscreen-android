package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import android.content.Context;

import fr.bouyguestelecom.tv.openapi.secondscreen.security.SSLUtils;

public class PFSRestClient extends RestClient {

    public final static String TOKEN_HEADER = "x-token";
    public final static String TOKEN_VALIDITY_HEADER = "x-token-validity";

    private final static String TAG = "PFSRestClient";

    private final static String URL = "https://dev.bouyguestelecom.fr/";

    public PFSRestClient(Context context) {
        super(context, URL, TAG);
        client.setSSLSocketFactory(SSLUtils.getSSLSocketFactory());
    }
}




