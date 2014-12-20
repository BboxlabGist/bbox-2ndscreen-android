package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.Auth;
import fr.bouyguestelecom.tv.openapi.secondscreen.security.Token;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.POST;

/**
 * Created by vincent on 19/12/2014.
 */
public interface IBboxSecurityService {
    @POST("/security/sessionId")
    public void getSessionId(@Body Token token, Callback<Object> callback);
}
