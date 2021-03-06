package org.gophillygo.app.tasks;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.gophillygo.app.R;
import org.gophillygo.app.data.DestinationDao;
import org.gophillygo.app.data.EventDao;
import org.gophillygo.app.data.models.AttractionFlag;
import org.gophillygo.app.data.models.Destination;
import org.gophillygo.app.data.models.DestinationLocation;
import org.gophillygo.app.data.models.Event;
import org.gophillygo.app.data.models.EventInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static org.gophillygo.app.tasks.RemoveGeofenceWorker.REMOVE_GEOFENCES_KEY;
import static org.gophillygo.app.tasks.RemoveGeofenceWorker.REMOVE_GEOFENCE_TAG;

/**
 * Add geofences by scheduling one-time worker job.
 *
 * Expects calling intent to set geofence data as three arrays with labels and coordinates.
 *
 * Per dagger docs:
 * `DaggerBroadcastReceiver` should only be used when the BroadcastReceiver is registered in the AndroidManifest.xml.
 * When the BroadcastReceiver is created in your own code, prefer constructor injection instead.
 * https://google.github.io/dagger/android.html
 *
 * This is registered in the manifest to listen for reboots, so we should use the dagger library.
 */
public class AddRemoveGeofencesBroadcastReceiver extends BroadcastReceiver {

    @Inject
    DestinationDao destinationDao;

    @Inject
    EventDao eventDao;

    public static final String ADD_GEOFENCE_TAG = "gpg-add-geofences";

    private static final String LOG_LABEL = "AddGeofenceBroadcast";
    private static final int MAX_GEOFENCES = 100; // cannot set up more than these

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(Context context, Intent intent) {
        AndroidInjection.inject(this, context);

        String action = intent.getAction();

        // Do not allow invoking broadcast receiver with unexpected action types
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Objects.equals(action, "org.gophillygo.app.tasks.ACTION_GEOFENCE_TRANSITION")) {
            Log.e(LOG_LABEL, "FIXME: Need to handle intent action type: " + intent.getAction());
            throw new UnsupportedOperationException("Unrecognized broadcast action type " + action);
        }

        // Before adding geofence, check user settings to see if geofence notifications are enabled.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.general_preferences_allow_notifications_key);
        final boolean allowNotifications = !sharedPref.contains(key) || sharedPref.getBoolean(key, true);

        double[] latitudes;
        double[] longitudes;
        String[] labels;
        String[] names;

        // Use intent extras when adding geofence(s) in response to user action.
        if (intent.hasExtra(AddGeofenceWorker.LATITUDES_KEY) && intent.hasExtra(AddGeofenceWorker.LONGITUDES_KEY)
                && intent.hasExtra(AddGeofenceWorker.GEOFENCE_LABELS_KEY) &&
                intent.hasExtra(AddGeofenceWorker.GEOFENCE_NAMES_KEY)) {

            latitudes = intent.getDoubleArrayExtra(AddGeofenceWorker.LATITUDES_KEY);
            longitudes = intent.getDoubleArrayExtra(AddGeofenceWorker.LONGITUDES_KEY);
            labels = intent.getStringArrayExtra(AddGeofenceWorker.GEOFENCE_LABELS_KEY);
            names = intent.getStringArrayExtra(AddGeofenceWorker.GEOFENCE_NAMES_KEY);

            // Sanity check the data before starting the worker
            if (Objects.requireNonNull(latitudes).length == 0 || latitudes.length != Objects.requireNonNull(longitudes).length ||
                    latitudes.length != Objects.requireNonNull(labels).length || latitudes.length != Objects.requireNonNull(names).length) {
                String message = "Extras data of zero or mismatched length found";
                Log.e(LOG_LABEL, message);
                FirebaseCrashlytics.getInstance().log(message);
                return;
            }

            Data data = new Data.Builder()
                    .putDoubleArray(AddGeofenceWorker.LATITUDES_KEY, latitudes)
                    .putDoubleArray(AddGeofenceWorker.LONGITUDES_KEY, longitudes)
                    .putStringArray(AddGeofenceWorker.GEOFENCE_LABELS_KEY, labels)
                    .putStringArray(AddGeofenceWorker.GEOFENCE_NAMES_KEY, names)
                    .build();

            startWorker(data);

        } else {
            Log.d(LOG_LABEL, "Reading data to add geofences from database.");

            // Query database on background thread by taking broadcast receiver async briefly
            // https://developer.android.com/guide/components/broadcasts
            final PendingResult pendingResult = goAsync();
            // Read datatabase instead of relying on an intent with extras; on boot, have no extras set
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPostExecute(Void aVoid) {
                    // Need to release BroadcastReceiver since have gone async
                    pendingResult.finish();
                }

                @Override
                protected void onCancelled() {
                    pendingResult.abortBroadcast();
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    List<Destination> destinations = destinationDao
                            .getGeofenceDestinations(AttractionFlag.Option.WantToGo.code,
                                    AttractionFlag.Option.Liked.code);
                    List<EventInfo> events = eventDao.getGeofenceEvents(AttractionFlag.Option.WantToGo.code,
                            AttractionFlag.Option.Liked.code);

                    // Check that there is at least one place to geofence and prevent NPEs by
                    // initializing null lists
                    if ((events == null || events.isEmpty()) &&
                            (destinations == null || destinations.isEmpty())) {
                        String message = "Have no destinations or events with geofences to add.";
                        FirebaseCrashlytics.getInstance().log(message);
                        Log.d(LOG_LABEL, message);
                        return null;
                    } else if (events == null) {
                        events = new ArrayList<>(0);
                    } else if (destinations == null) {
                        destinations = new ArrayList<>(0);
                    }

                    int destinationsCount = destinations.size();
                    int geofencesCount = destinationsCount + events.size();
                    if (geofencesCount > MAX_GEOFENCES && allowNotifications) {
                        // FIXME: handle having too many geofences
                        String message = "Too many destinations with geofences to add.";
                        Log.e(LOG_LABEL, message);
                        FirebaseCrashlytics.getInstance().log(message);
                        return null;
                    }

                    // send arrays of combined values for destinations and events
                    double[] latitudes = new double[geofencesCount];
                    double[] longitudes = new double[geofencesCount];
                    String[] labels = new String[geofencesCount];
                    String[] names = new String[geofencesCount];

                    // add destinations to the beginning
                    for (int i = 0; i < destinationsCount; i++) {
                        Destination destination = destinations.get(i);
                        labels[i] = GeofenceTransitionWorker.DESTINATION_PREFIX + destination.getId();
                        names[i] = destination.getName();
                        DestinationLocation location = destination.getLocation();
                        latitudes[i] = location.getY();
                        longitudes[i] = location.getX();
                    }

                    // add events to the end
                    for (int i = 0; i < events.size(); i++) {
                        int combinedIndex = i + destinationsCount;
                        EventInfo eventInfo = events.get(i);
                        labels[combinedIndex] = GeofenceTransitionWorker.EVENT_PREFIX + eventInfo.getAttraction().getId();
                        names[combinedIndex] = eventInfo.getEvent().getName();
                        DestinationLocation location = eventInfo.getLocation();
                        latitudes[combinedIndex] = location.getY();
                        latitudes[combinedIndex] = location.getX();
                    }

                    if (allowNotifications) {
                        Data data = new Data.Builder()
                                .putDoubleArray(AddGeofenceWorker.LATITUDES_KEY, latitudes)
                                .putDoubleArray(AddGeofenceWorker.LONGITUDES_KEY, longitudes)
                                .putStringArray(AddGeofenceWorker.GEOFENCE_LABELS_KEY, labels)
                                .putStringArray(AddGeofenceWorker.GEOFENCE_NAMES_KEY, names)
                                .build();
                        startWorker(data);
                    } else {
                        Log.d(LOG_LABEL, "User has disabled notifications; going to remove any registered geofences");
                        // start worker to remove all geofences
                        OneTimeWorkRequest.Builder workRequestBuilder = new OneTimeWorkRequest.Builder(RemoveGeofenceWorker.class);
                        Data data = new Data.Builder()
                                .putStringArray(REMOVE_GEOFENCES_KEY, labels).build();
                        workRequestBuilder.setInputData(data);
                        workRequestBuilder.addTag(REMOVE_GEOFENCE_TAG);
                        WorkRequest workRequest = workRequestBuilder.build();
                        WorkManager.getInstance().enqueue(workRequest);
                        Log.d(LOG_LABEL, "Enqueued new work request to remove all geofences");
                    }

                    return null;
                }
            }.execute();

        }
    }

    /**
     * Convenience static method to start a worker without using the broadcast receiver.
     *
     * @param destination Destination with a location to use for the geofence to add.
     */
    public static void addOneGeofence(@NonNull Destination destination) {
        addOneGeofence(destination.getLocation().getX(), destination.getLocation().getY(),
                GeofenceTransitionWorker.DESTINATION_PREFIX + destination.getId(), String.valueOf(destination.getName()));
    }

    public static void addOneGeofence(@NonNull EventInfo eventInfo) {
        DestinationLocation location = eventInfo.getLocation();
        if (location != null) {
            Event event = eventInfo.getEvent();
            addOneGeofence(location.getX(), location.getY(),
                    GeofenceTransitionWorker.EVENT_PREFIX + event.getId(), event.getName());
        } else {
            Log.e(LOG_LABEL, "Cannot add geofence for event without associated location.");
        }
    }

    public static void addOneGeofence(double x, double y, @NonNull String label, @NonNull String name) {

        double[] latitudes = {y};
        double[] longitudes = {x};
        String[] labels = {label};
        String[] names = {name};

        Data data = new Data.Builder()
                .putDoubleArray(AddGeofenceWorker.LATITUDES_KEY, latitudes)
                .putDoubleArray(AddGeofenceWorker.LONGITUDES_KEY, longitudes)
                .putStringArray(AddGeofenceWorker.GEOFENCE_LABELS_KEY, labels)
                .putStringArray(AddGeofenceWorker.GEOFENCE_NAMES_KEY, names)
                .build();

        Log.d(LOG_LABEL, "addOneGeofence");
        startWorker(data);
    }

    public static void startWorker(Data data) {
        // Start a worker to add geofences from a background thread.
        OneTimeWorkRequest.Builder workRequestBuilder = new OneTimeWorkRequest.Builder(AddGeofenceWorker.class);
        workRequestBuilder.setInputData(data);
        workRequestBuilder.addTag(ADD_GEOFENCE_TAG);
        WorkRequest workRequest = workRequestBuilder.build();
        WorkManager.getInstance().enqueue(workRequest);
        Log.d(LOG_LABEL, "Enqueued new work request to add geofence(s)");
    }
}
