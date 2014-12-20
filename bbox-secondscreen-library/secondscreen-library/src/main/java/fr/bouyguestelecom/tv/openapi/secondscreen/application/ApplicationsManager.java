package fr.bouyguestelecom.tv.openapi.secondscreen.application;

import android.util.Log;

import java.util.Arrays;
import java.util.List;

import fr.bouyguestelecom.tv.openapi.secondscreen.bbox.Bbox;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.AdapterUtils;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.CallbackHttpStatus;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.IBboxApplicationService;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

/**
 * Applications manager. Provide methods to get and manage applications installed on the Bbox
 *
 * @author Pierre-Etienne Cherière PCHERIER@bouyguestelecom.fr
 */
public class ApplicationsManager {

    private final String LOG_TAG = getClass().toString();

    private Bbox bbox;

    /**
     * Need a {@link fr.bouyguestelecom.tv.openapi.secondscreen.httputils.BboxRestClient BboxRestClient} to be abble to make http calls to the Bbox.
     *
     * @param bbox bbox for this manager
     */
    public ApplicationsManager(Bbox bbox) {
        this.bbox = bbox;
    }

    /**
     * Get the list of all installed applications in the provided callback. Return null if an error occur.
     *
     * @param callbackApplications callback
     */
    public void getApplications(final CallbackApplications callbackApplications) {
        AdapterUtils.createBboxService(bbox, IBboxApplicationService.class).getApplications(bbox.getSessionId(), new Callback<Application[]>() {
            @Override
            public void success(Application[] applications, Response response) {
                int statusCode = response.getStatus();
                if (statusCode == 200) {
                    List<Application> applicationList = Arrays.asList(applications);
                    callbackApplications.onResult(statusCode, applicationList);
                } else {
                    Log.e(LOG_TAG, "Unexpected response while getting applications. HTTP code: " + String.valueOf(statusCode) + " - 200 expected");
                    callbackApplications.onResult(statusCode, null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int statusCode = 500;
                if (error.getResponse() != null)
                    statusCode = error.getResponse().getStatus();

                Log.e(LOG_TAG, "Error while getting applications. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());
                callbackApplications.onResult(statusCode, null);
            }
        });
    }

    /**
     * Get the requested Application.
     *
     * @param packageName         The packageName of the requested application.
     * @param callbackApplication callback
     */
    public void getApplication(final String packageName, final CallbackApplication callbackApplication) {
        AdapterUtils.createBboxService(bbox, IBboxApplicationService.class).getApplications(bbox.getSessionId(), new Callback<Application[]>() {
            @Override
            public void success(Application[] applications, Response response) {
                int statusCode = response.getStatus();
                if (statusCode == 200) {
                    for (Application application : applications) {
                        if (application.getPackageName().equals(packageName)) {
                            callbackApplication.onResult(statusCode, application);
                        }
                    }
                } else {
                    Log.e(LOG_TAG, "Unexpected response while getting application. HTTP code: " + String.valueOf(statusCode) + " - 200 expected");
                    callbackApplication.onResult(statusCode, null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int statusCode = 500;
                if (error.getResponse() != null)
                    statusCode = error.getResponse().getStatus();

                Log.e(LOG_TAG, "Error while getting application. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());
                callbackApplication.onResult(statusCode, null);
            }
        });
    }

    /**
     * Start the provided {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} on the Bbox
     *
     * @param app application to start
     * @param callbackHttpStatus callback
     */
    public void startApplication(final Application app, final CallbackHttpStatus callbackHttpStatus) {
        AdapterUtils.createBboxService(bbox, IBboxApplicationService.class).startApplication(bbox.getSessionId(), app.getPackageName(), new Callback<Object>() {
            @Override
            public void success(Object obj, Response response) {
                int statusCode = response.getStatus();
                if (statusCode == 204) {
                    for (Header header : response.getHeaders()) {
                        if ("location".equalsIgnoreCase(header.getName())) {
                            String[] tab = header.getValue().split("/");
                            String appId = tab[tab.length - 1];
                            app.setAppId(appId);
                            break;
                        }
                    }
                } else {
                    Log.e(LOG_TAG, "Unexpected response while starting application. HTTP code: " + String.valueOf(statusCode) + " - 200 expected");
                }

                if (callbackHttpStatus != null) {
                    callbackHttpStatus.onResult(statusCode);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int statusCode = 500;
                if (error.getResponse() != null)
                    statusCode = error.getResponse().getStatus();

                Log.e(LOG_TAG, "Error while starting application. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());

                if (callbackHttpStatus != null) {
                    callbackHttpStatus.onResult(statusCode);
                }
            }
        });
    }

    public void startApplication(Application app) {
        startApplication(app, null);
    }

    /**
     * Stop the provided {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} on the Bbox
     *
     * @param app application to stop
     * @param callbackHttpStatus callback
     */
    public void stopApplication(final Application app, final CallbackHttpStatus callbackHttpStatus) {
        getApplication(app.getPackageName(), new CallbackApplication() {
            @Override
            public void onResult(int statusCode, Application application) {
                if (application != null) {
                    AdapterUtils.createBboxService(bbox, IBboxApplicationService.class).stopApplication(bbox.getSessionId(), application.getAppId(), new Callback<Object>() {
                        @Override
                        public void success(Object obj, Response response) {
                            int statusCode = response.getStatus();

                            if (callbackHttpStatus != null) {
                                callbackHttpStatus.onResult(statusCode);
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            int statusCode = 500;
                            if (error.getResponse() != null)
                                statusCode = error.getResponse().getStatus();

                            Log.e(LOG_TAG, "Error while stopping application. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());

                            if (callbackHttpStatus != null) {
                                callbackHttpStatus.onResult(statusCode);
                            }
                        }
                    });
                }
            }
        });
    }

    public void stopApplication(Application app) {
        stopApplication(app, null);
    }

    /**
     * Change the state of the {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} on the Bbox.
     * This also update the {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} provided to the new status.
     *
     * @param app                the app
     * @param applicationState   the new state wanted
     * @param callbackHttpStatus callback
     */
    public void changeApplicationState(final Application app, final ApplicationState applicationState, final CallbackHttpStatus callbackHttpStatus) {
        getApplication(app.getPackageName(), new CallbackApplication() {
            @Override
            public void onResult(int statusCode, final Application application) {
                if (application != null) {
                    AdapterUtils.createBboxService(bbox, IBboxApplicationService.class).changeApplicationState(bbox.getSessionId(), application.getAppId(), application.getAppState(), new Callback<Object>() {
                        @Override
                        public void success(Object obj, Response response) {
                            int statusCode = response.getStatus();

                            if (statusCode == 204)
                                app.setAppState(applicationState);
                            callbackHttpStatus.onResult(statusCode);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            int statusCode = 500;
                            if (error.getResponse() != null)
                                statusCode = error.getResponse().getStatus();

                            Log.e(LOG_TAG, "Error while changing application state. HTTP code: " + String.valueOf(statusCode) + " - Server response: " + error.getMessage());
                            callbackHttpStatus.onResult(statusCode);
                        }
                    });
                }
            }
        });
    }

    /**
     * Get the state of the {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} on the Bbox.
     * This also update the {@link fr.bouyguestelecom.tv.openapi.secondscreen.application.Application Application} provided to the new status.
     *
     * @param appToGet         the app
     * @param callbackAppState callback
     */
    public void getAppState(final Application appToGet, final CallbackAppState callbackAppState) {

        getApplication(appToGet.getPackageName(), new CallbackApplication() {
            @Override
            public void onResult(int statusCode, Application app) {
                if (app == null) {
                    callbackAppState.onResult(null);
                } else {
                    appToGet.setAppState(app.getAppState());
                    callbackAppState.onResult(app.getAppState());
                }
            }
        });
    }

    /* Not implemented in bbox api
    public void getMyAppId(String appName, final CallbackAppId callbackAppId) {
        JSONObject body = new JSONObject();

        try {
            body.put("appName", appName);
            bbox.getBboxRestClient().post(BboxApiUrl.APPLICATIONS_REGISTER, body, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    if (statusCode == 204) {
                        for (Header header : headers) {
                            if (header.getName().equals("Location")) {
                                String[] tab = header.getValue().split("/");
                                int idx = tab.length;
                                String appId = tab[idx - 1];

                                callbackAppId.onResult(statusCode, appId);
                            }
                        }
                    } else {
                        callbackAppId.onResult(statusCode, null);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject errorResponse) {
                    callbackAppId.onResult(statusCode, null);
                }
            });
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }*/

    public interface CallbackApplications {
        /**
         * @param statusCode Http status code of the request.
         * @param apps       null if an error occurred
         */
        public void onResult(int statusCode, List<Application> apps);
    }

    public interface CallbackApplication {
        /**
         * @param statusCode Http status code of the request.
         * @param app        null if an error occurred
         */
        public void onResult(int statusCode, Application app);
    }

    public interface CallbackAppState {
        public void onResult(ApplicationState applicationState);
    }

    /* not used (reserved for application register
    public interface CallbackAppId {
        public void onResult(int statusCode, String appId);
    }*/
}
