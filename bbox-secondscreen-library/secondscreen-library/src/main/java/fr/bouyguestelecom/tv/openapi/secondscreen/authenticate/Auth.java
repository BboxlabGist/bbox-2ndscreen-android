package fr.bouyguestelecom.tv.openapi.secondscreen.authenticate;

import android.content.Context;
import android.util.Log;

import java.util.Date;

import fr.bouyguestelecom.tv.openapi.secondscreen.bbox.Bbox;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.AdapterUtils;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.IBboxSecurityService;
import fr.bouyguestelecom.tv.openapi.secondscreen.security.Credentials;
import fr.bouyguestelecom.tv.openapi.secondscreen.security.Token;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
/**
 * This class is the main entry point for all authentication functionality
 * Every single application must register through this process before trying to communicate with the openAPI
 * Created by fab on 01/09/2014.
 */
public class Auth {

    private static final String LOG_TAG = Auth.class.toString();

    private Context context = null;
    private String sessionId = null;
    private Token token = null;
    private Bbox bbox = null;

    public static final String TOKEN_HEADER = "x-token";
    public static final String TOKEN_VALIDITY_HEADER = "x-token-validity";
    public static final String ID_SESSION_HEADER = "x-sessionid";

    /**
     * Create an authorization instance.
     * @param context : Context of the android application
     * @param bbox : bbox to be used in the second authentication step
     */
    public Auth(Context context, Bbox bbox) {
        this.context = context;
        this.bbox = bbox;
    }

    /**
     * @return the sessionId (this session id must be used in every function call
     * between this application and the OpenAPI. To do this the sessionId must be add as an http header
     * in the communication with the OpenAPI
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     *
     * @return the security token provided by the bouyguestelecom external platform. this
     * security token will be processed by the OpenAPI. if this token is "a good one", the openAPI
     * will provide in return a sessionId that must be use in every futher cummincation with
     * the OpenAPI @see{getSessionId}
     */
    public Token getSecurityToken() {
        return token;
    }

    /**
     * This is the main entry point, this function will do all the stuff for you in 
     * an asynchronous way.
     * appId and secretId make a pair of parameters used by a remote bouyguestelecom platform to decide if you are 
     * legitimate to use OpenAPI. This pair (unique by application type) must have been provided to 
     * bouyguestelecom prior to any use, if this is not the case the remote platform could not identify you
     * @param appId : the application identifier supplied by bouyguestelecom (must be unique per application type)
     * @param appSecret : the appSecret supplied by bouyguestelecom
     * @param callback : an IAuthCallBack object that will be triggered at the end of the authorization process
     */
    public void authenticate(String appId, String appSecret, IAuthCallback callback)
    {
        sessionId = null;
        innerAuthenticate(appId, appSecret, callback);
    }

    /**
     * This method should be use only in the case of the communication broke with the bbox or if your sessionId is to old. In this case, no
     * need to identify against the remote platform again, just call this method and you should be authorized to communicate with 
     * the bbox for another period of time.
     * @param callback : an IAuthCallBack object that will be triggered at the end of the authorization process
     */
    public void connectOnly(final IAuthCallback callback)
    {
        AdapterUtils.createBboxService(bbox, IBboxSecurityService.class).getSessionId(token, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                int statusCode = response.getStatus();
                if (statusCode == 401){
                    Log.e(LOG_TAG, "Application is not authorized");
                    callback.onAuthResult(401, "Application is not authorized, please contact bouyguestelecom.");
                } else if (statusCode == 200 || statusCode == 204) {
                    for (Header header : response.getHeaders()) {
                        if (ID_SESSION_HEADER.equalsIgnoreCase(header.getName())){
                            sessionId = header.getValue();
                            callback.onAuthResult(statusCode, "Connexion established");
                            break;
                        }
                    }
                } else {
                    Log.e(LOG_TAG, "Unexpected response while getting session id. HTTP code: " + String.valueOf(statusCode) + " - 200 or 204 expected");
                    callback.onAuthResult(statusCode, "Unexpected response while getting session id. HTTP code: " + String.valueOf(statusCode) + ". Something went wrong while checking your application's authorisation on the bbox.");
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int statusCode = 401;
                if (error.getResponse() != null)
                    statusCode = error.getResponse().getStatus();

                if (statusCode == 401){
                    Log.e(LOG_TAG, "Application is not authorized");
                    callback.onAuthResult(statusCode, "Application is not authorized, please contact bouyguestelecom.");
                }
                else {
                    Log.e(LOG_TAG, "Unexpected response while getting session id. HTTP code: " + String.valueOf(statusCode) + " - 200 or 204 expected");
                    callback.onAuthResult(statusCode, "Unexpected response while getting session id. HTTP code: " + String.valueOf(statusCode) + ". Something went wrong while checking your application's authorisation on the bbox.");
                }
            }
        });
    }

    public void innerAuthenticate(String appId, String appSecret, final IAuthCallback callback) {
            AdapterUtils.createPfsService().getToken(new Credentials(appId, appSecret), new Callback<Object>() {

                @Override
                public void success(Object o, Response response) {
                    int statusCode = response.getStatus();
                    if (statusCode == 401) {
                        Log.e(LOG_TAG, "Application is not authorized, please contact bouyguestelecom.");
                        callback.onAuthResult(401, "Application is not authorized, please contact bouyguestelecom.");
                    }

                    if (statusCode == 200 || statusCode == 204) {
                        String tokenValue = null;
                        Date tokenValidity = null;
                        for (Header header : response.getHeaders()) {
                            if (TOKEN_HEADER.equalsIgnoreCase(header.getName())) {
                                tokenValue = header.getValue();
                            } else if (TOKEN_VALIDITY_HEADER.equalsIgnoreCase(header.getName())) {
                                tokenValidity = new Date(Long.parseLong(header.getValue()));
                            }
                        }

                        // Update token
                        if (tokenValue != null && tokenValidity != null) {
                            token = new Token(tokenValue, tokenValidity);
                            connectOnly(callback);
                        } else {
                            Log.e(LOG_TAG, "Cannot retrieve token info from http headers");
                            callback.onAuthResult(401, "Cannot retrieve token info from http headers");
                        }

                    } else {
                        Log.e(LOG_TAG, "Unexpected response while getting security token. HTTP code: " + String.valueOf(statusCode) + " - 200 or 204 expected");
                        callback.onAuthResult(statusCode, "Unexpected response while getting security token. HTTP code: " + String.valueOf(statusCode) + ". Something went wrong when trying to authenticate your application on the distant PFS. Please make sure your device can access the Internet.");
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    int statusCode = 401;
                    if (error.getResponse() != null)
                        statusCode = error.getResponse().getStatus();

                    if (statusCode == 401) {
                        Log.e(LOG_TAG, "Application is not authorized");
                        callback.onAuthResult(401, "Application is not authorized, please contact bouyguestelecom.");
                        return;
                    }
                    Log.e(LOG_TAG, "Unexpected response while getting security token. HTTP code: " + String.valueOf(statusCode) + " - 200 or 204 expected");
                    callback.onAuthResult(statusCode, "Unexpected response while getting security token. HTTP code: " + String.valueOf(statusCode) + ". Something went wrong went trying to authenticate your application on the distant PFS. Please make sure your device can access the Internet.");
                }
            });
    }
}
