package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import fr.bouyguestelecom.tv.openapi.secondscreen.security.Credentials;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Service to access PFS.
 */
public interface IPFSService {
    @POST("/security/token")
    public void getToken(@Body Credentials auth, Callback<Object> callback);
}
