package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import fr.bouyguestelecom.tv.openapi.secondscreen.application.Application;
import fr.bouyguestelecom.tv.openapi.secondscreen.application.ApplicationState;
import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.Auth;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by vincent on 20/12/2014.
 */
public interface IBboxApplicationService {
    @GET("/applications")
    public void getApplications(@Header(Auth.ID_SESSION_HEADER) String idSession, Callback<Application[]> callback);

    @POST("/applications/{appName}")
    public void startApplication(@Header(Auth.ID_SESSION_HEADER) String idSession, @Path("appName") String appName, Callback<Object> callback);

    @DELETE("/applications/run/{appId}")
    public void stopApplication(@Header(Auth.ID_SESSION_HEADER) String idSession, @Path("appId") String appId, Callback<Object> callback);

    @POST("/applications/run/{appId}")
    public void changeApplicationState(@Header(Auth.ID_SESSION_HEADER) String idSession, @Path("appId") String appId, @Body ApplicationState appState, Callback<Object> callback);

    @POST("/applications/register")
    public void register(@Header(Auth.ID_SESSION_HEADER) String idSession, @Body Application application, Callback<Object> callback);
}
