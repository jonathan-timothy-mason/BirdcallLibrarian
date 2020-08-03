package jonathan.mason.birdcalllibrarian;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import jonathan.mason.birdcalllibrarian.Database.Birdcall;

/**
 * No-UI fragment, whose instance is to be retained, as a home for a MediaRecorder
 * instance, to allow recording across configuration changes.
 * <p>See https://knowledge.udacity.com/questions/199426.</p>
 */
public class RecordFragment extends Fragment implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    /**
     * Recording states of RecordFragment.
     */
    public enum RecordingStates {
        /**
         * Recording not yet started.
         */
        NotStarted,

        /**
         * Recording in progress.
         */
        Started,

        /**
         * Recording completed.
         */
        Ended
    }

    /**
     * Listener for notification of start and stop of recording.
     */
    public interface BirdcallRecordingListener {
        /**
         * Handle start of recording.
         */
        void onRecordingStarted();

        /**
         * Handle stop of recording.
         */
        void onRecordingStopped();
    }

    /**
     * Tag to identify fragment.
     */
    public static final String TAG = "RECORD";

    /**
     * Maximum size of recording in bytes.
     * <p>For audio quality used, this allowed a recording of approximately 10 minutes.</p>
     */
    public static final int MAX_FILESIZE_BYTES = 1000000;

    private MediaRecorder mMediaRecorder;
    private String mTemporaryFilename;
    private double mLatitude;
    private double mLongitude;
    private Boolean mLocationRetrieved;
    private BirdcallRecordingListener mBirdcallRecordingListener;

    /**
     * Override to ensure activity is suitable for use with fragment.
     * @param context Not used.
     * @exception ClassCastException Thrown if activity does not implement "BirdcallRecordingListener"
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Make sure activity implements BirdcallRecordingListener.
        try {
            mBirdcallRecordingListener = (BirdcallRecordingListener)this.getActivity();
        }
        catch(ClassCastException e) {
            throw new ClassCastException(this.getActivity().toString() + " does not implement RecordFragment.BirdcallRecordingListener.");
        }
    }

    /**
     * Override to initialise fragment to be retained.
     * @param savedInstanceState Saved state of fragment; not used.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        mRecordingState = RecordingStates.NotStarted;

        // Attempt to get location as soon as possible, in case it needs to be requested.
        // Default to latitude 0, longitude 0, which is somewhere in the Atlantic ocean.
        mLatitude = 0;
        mLongitude = 0;
        mLocationRetrieved = false;
        this.getLocation();
    }

    /**
     * Get location, first attempting to get last location, if available, otherwise,
     * requesting it, which can take a few seconds.
     * <p>From "Get Current location using FusedLocationProviderClient in Android" by Droid By Me:
     * https://medium.com/@droidbyme/get-current-location-using-fusedlocationproviderclient-in-android-cb7ebf5ab88e.
     * And "Get the last known location", Android Developers:
     * https://developer.android.com/training/location/retrieve-current.</p>
     */
    private void getLocation() {
        // Attempt to get last location.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getContext());
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            /**
             * Handle retrieval of last recorded location.
             * @param location Last recorded location.
             */
            @Override
            public void onSuccess(Location location) {
                // A location may not have been recorded.
                if (location != null) {
                    // If the location was recorded less than an hour ago, use it.
                    // From answer to "How to convert nanoseconds to seconds using the TimeUnit enum?" by pythonquick:
                    // https://stackoverflow.com/questions/924208/how-to-convert-nanoseconds-to-seconds-using-the-timeunit-enum.
                    if(TimeUnit.MINUTES.convert(SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos(), TimeUnit.NANOSECONDS) < 60) {
                        mLatitude = location.getLatitude();
                        mLongitude = location.getLongitude();
                        mLocationRetrieved = true;
                        return;
                    }
                }

                // If no location has been recorded, request it.
                requestLocation(fusedLocationClient);
            }
        });
    }

    /**
     * Request location.
     * <p>From "Get Current location using FusedLocationProviderClient in Android" by Droid By Me:
     * https://medium.com/@droidbyme/get-current-location-using-fusedlocationproviderclient-in-android-cb7ebf5ab88e.
     * And "Change location settings", Android Developers:
     * https://developer.android.com/training/location/change-location-settings.</p>
     * @param fusedLocationClient Fused location provider.
     */
    private void requestLocation(FusedLocationProviderClient fusedLocationClient) {
        Application application = this.getActivity().getApplication();

        // Create a single location request.
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Requires Location, Settings, Mode to be "High accuracy" and if there is no internet connection, "Wi-Fi scanning" to be enabled.
        locationRequest.setNumUpdates(1);

        // Check if current settings (Settings, Location) of device allow location request.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(getContext());
        client.checkLocationSettings(builder.build()).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            /**
             * Handle completion of check, with device settings correct for type of location request.
             * @param locationSettingsResponse Details of result of check.
             */
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // Make location request.
                fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    /**
                     * Handle completion of location request.
                     * @param locationResult Current location.
                     */
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // Ensure location result has a location.
                        if ((locationResult == null) || (locationResult.getLocations().size() <= 0) || (locationResult.getLocations().get(0) == null)) {
                            Log.e(RecordFragment.class.getSimpleName(), "requestLocation: retrieved result empty.");
                            Toast.makeText(application, getString(R.string.error_location_request_result_empty), Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Use retrieved current location.
                        Location location = locationResult.getLocations().get(0);
                        mLatitude = location.getLatitude();
                        mLongitude = location.getLongitude();
                        mLocationRetrieved = true;

                    }
                }, null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            /**
             * Handle completion of check, with device settings incorrect for type of location
             * request.
             * <p>The startResolutionForResult function of ResolvableApiException could be used to
             * show a dialog to request an automatic settings change, but this would interfere with
             * the recording. Instead, a less obtrusive message, explaining the required settings
             * change, is to be shown.</p>
             * @param e Error to do with incorrect device settings.
             */
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    Log.e(RecordFragment.class.getSimpleName(), "requestLocation: required setting(s) disabled.", e);
                    Toast.makeText(application, getString(R.string.error_location_update_settings_disabled), Toast.LENGTH_LONG).show();
                }
                else {
                    Log.e(RecordFragment.class.getSimpleName(), "requestLocation: required setting(s) incorrect.", e);
                    Toast.makeText(application, getString(R.string.error_location_update_settings_incorrect), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Override to indicate no-UI fragment by returning null. Usually root view of
     * fragment is inflated/created here.
     * @param inflater Inflater to inflate fragment; not used.
     * @param container Parent view for fragment; not used.
     * @param savedInstanceState Saved state of fragment; not used.
     * @return Null as this no-UI fragment has no root view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    /**
     * Override to start recording.
     * <p>From Android 9 (API 28) microphone is only available when app is in foreground.</p>.
     */
    @Override
    public void onResume() {
        super.onResume();

        this.startRecording();
    }

    /**
     * Override to stop recording if app is about to go in the background, but not for
     * configuration change.
     * <p>From Android 9 (API 28) microphone is only available when app is in foreground.</p>.
     * <p>According to "Android P feature spotlight: Google confirms idle apps can't access
     * microphone or camera" by Corbin Davenport, www.androidpolice.com (see link below),
     * the microphone records silence if the app goes into the background. There may therfore
     * be a slight silence if there is a configuration change whilst recording.
     * https://www.androidpolice.com/2018/03/07/android-p-feature-spotlight-google-confirms-idle-apps-cant-access-microphone-camera/</p>
     */
    @Override
    public void onPause() {
        super.onPause();

        // Don't end recording just for a configuration change.
        // "isChangingConfigurations" from answer to "how to check if android going to recreate
        // activity or destroy?" by Chrisvin Jem: https://stackoverflow.com/questions/57026507/how-to-check-if-android-going-to-recreate-activity-or-destroy
        if(!this.getActivity().isChangingConfigurations())
            this.stopRecording();
    }

    private RecordingStates mRecordingState;
    /**
     * Get recording state of RecordFragment.
     * @return Recording state.
     */
    public RecordingStates getRecordingState() {
        return mRecordingState;
    }

    /**
     * Create and initialise MediaRecorder and start recording.
     * <p>For details of audio formats etc., see: https://developer.android.com/guide/topics/media/media-formats.html</p>
     */
    private void startRecording() {
        if(mRecordingState == RecordingStates.NotStarted) {
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mMediaRecorder.setOnInfoListener(this);
                mMediaRecorder.setMaxFileSize(MAX_FILESIZE_BYTES);

                try {
                    mTemporaryFilename = File.createTempFile(Birdcall.TEMP_FILE_PREFIX, ".3gp", this.getContext().getCacheDir()).getPath();
                    mMediaRecorder.setOutputFile(mTemporaryFilename);
                    mMediaRecorder.prepare();
                    mMediaRecorder.start(); // Recording is now about to start.
                    mRecordingState = RecordingStates.Started;
                    mBirdcallRecordingListener.onRecordingStarted();
                } catch (IOException e) {
                    Log.e(RecordFragment.class.getSimpleName(), "startRecording: error preparing to record.", e);
                    Toast.makeText(this.getContext(), this.getString(R.string.error_preparing_to_record), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Stop recording, release MediaRecorder, save birdcall to database and
     * close RecordActivity screen.
     */
    public void stopRecording() {
        if(mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mRecordingState = RecordingStates.Ended;
            mBirdcallRecordingListener.onRecordingStopped();

            // Load recording and save to database using AsyncTask (AsyncTask is used
            // safely as it keeps reference to application context and does not refer
            // to RecordActivity or RecordFragment).
            Birdcall birdcall = new Birdcall(null, Birdcall.getDefaultTitle(this.getContext()), new Date(), mLongitude, mLatitude, null, null);
            new SaveBirdcallAsyncTask(this.getActivity().getApplication(), mTemporaryFilename, birdcall).execute();

            // Tell user location not available.
            if(mLocationRetrieved == false) {
                Log.e(RecordFragment.class.getSimpleName(), "stopRecording: location not available.");
                Toast.makeText(this.getContext(), getString(R.string.error_location_not_available), Toast.LENGTH_LONG).show();
            }

            // Update any widgets to show last recorded birdcall.
            Widget.updateAllAppWidgets(this.getContext(), AppWidgetManager.getInstance(this.getContext()), birdcall);
        }
    }

    /**
     * Handle MediaRecorder warning event to stop recording if it gets too big.
     * @param mr Instance of MediaRecorder; not used.
     * @param what Type of warning.
     * @param extra Extra code; not used.
     */
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            Log.e(RecordFragment.class.getSimpleName(), "onInfo: maximum file size reached.");
            Toast.makeText(this.getContext(), this.getString(R.string.warning_max_file_size_reached), Toast.LENGTH_LONG).show();

            this.stopRecording();
        }
    }

    /**
     * Handle MediaRecorder error event.
     * @param mr instance of MediaRecorder; not used.
     * @param what type of error.
     * @param extra extra code about error.
     */
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(this.getClass().getSimpleName(), "onError: type = " + what + ", extra = " + extra + ".");
        Toast.makeText(this.getContext(), this.getString(R.string.error_during_recording), Toast.LENGTH_LONG).show();
    }
}
