package fr.bouyguestelecom.tv.openapi.secondscreen.notification;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.bouyguestelecom.tv.openapi.secondscreen.bbox.Bbox;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.AdapterUtils;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.IBboxNotificationService;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.DefaultRetrofitCallback;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * @author Pierre-Etienne Cherière PCHERIER@bouyguestelecom.fr
 */
public class WebSocket implements NotificationManager {

    private static String LOG_TAG = "WebSocket";

    private static String WEBSOCKET_PREFIX = "ws://";
    private static String WEBSOCKET_PORT = "9090";
    private static String websocketAddress;

    private static String RESOURCE_ID_KEY = "resourceId";
    private static String ERROR_KEY = "error";
    private static String BODY_KEY = "body";
    private static String MESSAGE_KEY = "message";
    private static String DESTINATION_KEY = "destination";
    private static String SOURCE_KEY = "source";
    private static WebSocket instance = null;
    private WebSocketClient webSocketClient;
    private CallbackConnected callbackConnected;
    private String appId;
    private String channelId;
    private Bbox bbox;
    private List<Listener> allNotificationsListeners = new ArrayList<Listener>();
    private List<Listener> applicationsListeners = new ArrayList<Listener>();
    private List<Listener> messagesListeners = new ArrayList<Listener>();
    private List<Listener> mediaListeners = new ArrayList<Listener>();
    private List<Listener> userInputListeners = new ArrayList<Listener>();
    private List<Listener> errorListener = new ArrayList<Listener>();
    private Map<String, List<Listener>> listenerMap = new HashMap<String, List<Listener>>();
    private HashSet<String> notificationsSubscribed = new HashSet<String>();

    public WebSocket(final String appId, Bbox bbox) {
        assert (appId == null) : "You must provide a valid appId when initializing WebSocket";
        assert (bbox == null) : "You must provide a valid bbox object when initializing WebSocket";

        this.appId = appId;
        this.bbox = bbox;
        listenerMap.put(NotificationType.APPLICATION.toString(), applicationsListeners);
        listenerMap.put(NotificationType.MESSAGE.toString(), messagesListeners);
        listenerMap.put(NotificationType.MEDIA.toString(), mediaListeners);
        listenerMap.put(NotificationType.USER_INPUT.toString(), userInputListeners);

        websocketAddress = WEBSOCKET_PREFIX + bbox.getIp() + ":" + WEBSOCKET_PORT;
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    public static WebSocket getInstance(final String appId, Bbox bbox) {
        if (instance == null) {
            instance = new WebSocket(appId, bbox);
        }
        return instance;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public void subscribe(NotificationType notificationType, CallbackSubscribed callbackSubscribed) {
        notificationsSubscribed.add(notificationType.toString());
        updateSubscribe(callbackSubscribed);
    }

    @Override
    public void subscribe(NotificationType notificationType, String additionalParam, CallbackSubscribed callbackSubscribed) {
        notificationsSubscribed.add(notificationType.toString() + "/" + additionalParam);
        updateSubscribe(callbackSubscribed);
    }

    @Override
    public void unSubscribe(NotificationType notificationType, CallbackSubscribed callbackSubscribed) {
        notificationsSubscribed.remove(notificationType.toString());
        updateSubscribe(callbackSubscribed);
    }

    @Override
    public void unSubscribe(NotificationType notificationType, String additionalParam, CallbackSubscribed callbackSubscribed) {
        notificationsSubscribed.remove(notificationType.toString() + "/" + additionalParam);
        updateSubscribe(callbackSubscribed);
    }

    @Override
    public void unSubscribeToAll() {
        notificationsSubscribed.clear();

        if (channelId != null) {
            AdapterUtils.createBboxService(bbox, IBboxNotificationService.class).unsubscribe(bbox.getSessionId(), channelId, new DefaultRetrofitCallback("Notification unsubscribe"));
        }
    }

    @Override
    public void subscribe(NotificationType[] notificationTypes, CallbackSubscribed callbackSubscribed) {
        for (int i = 0; i < notificationTypes.length; i++) {
            notificationsSubscribed.add(notificationTypes[i].toString());
        }
        updateSubscribe(callbackSubscribed);
    }

    @Override
    public void subscribe(String[] notificationTypes, CallbackSubscribed callbackSubscribed) {
        for (int i = 0; i < notificationTypes.length; i++) {
            notificationsSubscribed.add(notificationTypes[i]);
        }
        updateSubscribe(callbackSubscribed);
    }

    public void updateSubscribe(final CallbackSubscribed callbackSubscribed) {
        IBboxNotificationService.NotificationBody toSend = new IBboxNotificationService.NotificationBody();
        List<IBboxNotificationService.Resource> toSubscribe = new ArrayList<IBboxNotificationService.Resource>();

        for (String notification : notificationsSubscribed) {
            IBboxNotificationService.Resource resource = new IBboxNotificationService.Resource();
            resource.resourceId = notification;
            toSubscribe.add(resource);
        }
        toSend.appId = appId;
        toSend.resources = toSubscribe.toArray(new IBboxNotificationService.Resource[toSubscribe.size()]);

        AdapterUtils.createBboxService(bbox, IBboxNotificationService.class).subscribe(bbox.getSessionId(), toSend, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                int statusCode = response.getStatus();
                if (statusCode == 204) {
                    for (retrofit.client.Header header : response.getHeaders()) {
                        if ("location".equalsIgnoreCase(header.getName())) {
                            String[] tab = header.getValue().split("/");
                            channelId = tab[tab.length - 1];
                            Log.d(LOG_TAG, "channelId: " + channelId);
                        }
                    }
                } else {
                    Log.e(LOG_TAG, "Unexpected response while starting application. HTTP code: " + String.valueOf(statusCode) + " - 200 expected");
                }

                if (callbackSubscribed != null) {
                    callbackSubscribed.onResult(statusCode);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int statusCode = 500;
                if (error.getResponse() != null)
                    statusCode = error.getResponse().getStatus();

                Log.e(LOG_TAG, "Error while getting subscribing notification. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());

                callbackSubscribed.onResult(statusCode);
            }
        });
    }

    @Override
    public void sendMessage(String channelID, String msg) {

        JSONObject toSend = new JSONObject();

        try {
            toSend.put(DESTINATION_KEY, channelID);
            toSend.put(SOURCE_KEY, appId);
            toSend.put(MESSAGE_KEY, msg);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        Log.d(LOG_TAG, "sending: " + msg);
        if (webSocketClient.getReadyState().equals(org.java_websocket.WebSocket.READYSTATE.OPEN)) {
            webSocketClient.send(toSend.toString());
        }
    }

    @Override
    public void sendMessage(String channelID, JSONObject msg) {
        sendMessage(channelID, msg.toString());
    }

    @Override
    public void sendRoomMessage(String room, String msg) {
        sendMessage(NotificationType.MESSAGE + "/" + room, msg);
    }

    @Override
    public void sendRoomMessage(String room, JSONObject msg) {
        sendMessage(room, msg.toString());
    }

    @Override
    public void addApplicationListener(Listener listener) {
        applicationsListeners.add(listener);
    }

    @Override
    public void addMediaListener(Listener listener) {
        mediaListeners.add(listener);
    }

    @Override
    public void addMessageListener(Listener listener) {
        messagesListeners.add(listener);
    }

    @Override
    public void addUserInputListener(Listener listener) {
        userInputListeners.add(listener);
    }

    @Override
    public void addErrorListener(Listener listener) {
        errorListener.add(listener);
    }

    @Override
    public void addAllNotificationsListener(Listener listener) {
        allNotificationsListeners.add(listener);
    }

    @Override
    public void removeApplicationListener(Listener listener) {
        applicationsListeners.remove(listener);
    }

    @Override
    public void removeMediaListener(Listener listener) {
        mediaListeners.remove(listener);
    }

    @Override
    public void removeMessageListener(Listener listener) {
        messagesListeners.remove(listener);
    }

    @Override
    public void removeUserInputListener(Listener listener) {
        userInputListeners.remove(listener);
    }

    @Override
    public void removeErrorListener(Listener listener) {
        errorListener.remove(listener);
    }

    @Override
    public void removeAllNotificationsListener(Listener listener) {
        allNotificationsListeners.remove(listener);
    }

    @Override
    public void listen(final CallbackConnected callbackConnected) {
        assert (webSocketClient == null) : "You must init WebSocket with the init() method before trying to listen";
        this.callbackConnected = callbackConnected;

        webSocketClient = new WebSocketClient(URI.create(websocketAddress)) {
            @Override
            public void onOpen(ServerHandshake handShakeData) {
                Log.d(LOG_TAG, "socket open");
                webSocketClient.send(appId);
                if (callbackConnected != null) {
                    callbackConnected.onConnect();
                }
            }

            @Override
            public void onMessage(String message) {
                try {

                    Log.d(LOG_TAG, "receiving: " + message);

                    JSONObject msg = new JSONObject(message);
                    JSONObject ret = new JSONObject();

                    for (Listener listener : allNotificationsListeners) {
                        listener.onNotification(msg);
                    }

                    if (msg.has(ERROR_KEY)) {
                        for (Listener listener : errorListener) {
                            listener.onNotification(msg);
                        }
                        Log.e(LOG_TAG, msg.toString());
                    } else if (listenerMap.containsKey(msg.getString(RESOURCE_ID_KEY))) {
                        for (Listener listener : listenerMap.get(msg.getString(RESOURCE_ID_KEY))) {
                            listener.onNotification(msg.getJSONObject(BODY_KEY));
                        }
                    } else {
                        JSONObject error = new JSONObject();
                        try {
                            error.put("msg", "invalid resourceId: " + msg.getString(RESOURCE_ID_KEY));
                            error.put("status", "0");
                            ret.put(ERROR_KEY, msg);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }

                        for (Listener listener : errorListener) {
                            listener.onNotification(error);
                        }

                        Log.e(LOG_TAG, "invalid resourceId: " + msg.getString(RESOURCE_ID_KEY));

                    }

                } catch (JSONException e) {

                    JSONObject error = new JSONObject();
                    JSONObject msg = new JSONObject();

                    try {
                        msg.put("msg", e.getMessage());
                        msg.put("status", "0");
                        error.put(ERROR_KEY, msg);
                    } catch (JSONException e1) {
                        Log.e(LOG_TAG, e1.getMessage());
                    }

                    for (Listener listener : errorListener) {
                        listener.onNotification(error);
                    }
                    Log.e(LOG_TAG, e.getMessage() + " Message: " + message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.e(LOG_TAG, "socket closed: "+reason);
            }

            @Override
            public void onError(Exception ex) {
                Log.e(LOG_TAG, ex.getMessage());
            }
        };

        webSocketClient.connect();

    }

    @Override
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}