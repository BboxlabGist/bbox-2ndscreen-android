package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.Auth;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by vincent on 22/12/2014.
 */
public interface IBboxNotificationService {

    @POST("/notification")
    public void subscribe(@Header(Auth.ID_SESSION_HEADER) String idSession, @Body NotificationBody notificationBody, Callback<Object> callback);

    @DELETE("/notification/{channelId}")
    public void unsubscribe(@Header(Auth.ID_SESSION_HEADER) String idSession, @Path("channelId") String channelId, Callback<Object> callback);

    public class NotificationBody {
        public String appId;
        public Resource[] resources;
    }

    public class Resource {
        public String resourceId;
    }
}
