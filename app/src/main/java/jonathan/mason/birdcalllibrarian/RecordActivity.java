package jonathan.mason.birdcalllibrarian;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import jonathan.mason.birdcalllibrarian.Database.Birdcall;

/**
 * Recording screen of app, solely for recording birdcalls.
 * <p>Recording is delegated to no-UI "RecordFragment".</p>
 */
public class RecordActivity extends AppCompatActivity implements View.OnClickListener, RecordFragment.BirdcallRecordingListener {
    /**
     * Key(s) for storing data in bundle.
     */
    public static final String LAUNCHED_BY_WIDGET = "LAUNCHED_BY_WIDGET";

    /**
     * Arbitrary ID to be used by this app to request permission to record audio.
     */
    private static final int REQUEST_RECORD_AUDIO_AND_LOCATION_PERMISSIONS = 200;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.stop_fab) FloatingActionButton mStopFAB;
    @BindView(R.id.record_on_off_air) TextView mOnOffAir;
    @BindView(R.id.record_title) TextView mTitle;
    @BindView(R.id.record_date_and_time) TextView mDateAndTime;

    private boolean mPermssionsGranted;
    private RecordFragment mRecordFragment;
    ObjectAnimator mRecordingAnimator;

    /**
     * Perform initialisation of activity, RecordFragment and handle requesting permission
     * to record audio.
     * @param savedInstanceState Saved state of activity: not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // Retrieve views.
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mStopFAB.setOnClickListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check if app has been granted permission to record audio and access location.
        mPermssionsGranted = (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) &&
                (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        if (mPermssionsGranted == false) {
            // No, so request them.
            // Based on answer to "Why is my onResume being called twice?" by user924:
            // https://stackoverflow.com/questions/16026756/why-is-my-onresume-being-called-twice.
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_RECORD_AUDIO_AND_LOCATION_PERMISSIONS);
        }
        else {
            // Yes, so create fragment.
            this.createFragment();
        }

        // Set default title and date and time.
        mTitle.setText(Birdcall.getDefaultTitle(this));
        mDateAndTime.setText(DateFormat.getInstance().format(new Date()));

        // Setup on-air animator.
        // From "Create a Blink Effect on Android" by Sylvain Saurel:
        // https://medium.com/@ssaurel/create-a-blink-effect-on-android-3c76b5e0e36b
        mRecordingAnimator = ObjectAnimator.ofInt(mOnOffAir, "backgroundColor", Color.TRANSPARENT, this.getResources().getColor(R.color.on_air_background, null)); // On-air red is brighter.
        mRecordingAnimator.setDuration(1000);
        mRecordingAnimator.setEvaluator(new ArgbEvaluator());
        mRecordingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mRecordingAnimator.setRepeatCount(Animation.INFINITE);
    }

    /**
     * Handle result of permission requests and create RecordFragment, if allowed.
     * <p>Based on "MediaRecorder overview", "Sample code" of Android Developers:
     * https://developer.android.com/guide/topics/media/mediarecorder</p>
     * @param requestCode Arbitrary ID used by this app to request permission to record audio
     * and access location.
     * @param permissions Permissions requested; not used.
     * @param grantResults Results of request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Empty grantResults array corresponds to another attempt to request permissions
        // having been made, and can be ignored.
        if(grantResults.length > 0) {
            if ((requestCode == REQUEST_RECORD_AUDIO_AND_LOCATION_PERMISSIONS) && (grantResults.length >= 3)) {
                mPermssionsGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    (grantResults[1] == PackageManager.PERMISSION_GRANTED) &&
                    (grantResults[2] == PackageManager.PERMISSION_GRANTED);

                // If all granted, create fragment.
                if (mPermssionsGranted) {
                    this.createFragment();
                    return;
                }
            }

            // Denied, so close activity screen.
            Toast.makeText(this, this.getString(R.string.permissions_denied), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Create or retrieve existing instance of RecordFragment.
     */
    private void createFragment() {
        // Attempt to retrieve retained instance of fragment.
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        mRecordFragment = (RecordFragment)fragmentManager.findFragmentByTag(RecordFragment.TAG);
        if(mRecordFragment == null) {
            // Fragment does not exist, so create one.
            mRecordFragment = new RecordFragment();
            fragmentManager.beginTransaction().add(mRecordFragment, RecordFragment.TAG).commit();
        }
    }

    /**
     * Override to create menu for screen.
     * @param menu Menu being created.
     * @return True to display menu, otherwise false.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_record, menu);
        return true;
    }

    /**
     * Override to handle menu selection.
     * @param item Selected menu item.
     * @return True if menu selection was handled, otherwise false.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (item.getItemId() == android.R.id.home) {
            Intent intent = this.getIntent();
            if(intent.hasExtra(LAUNCHED_BY_WIDGET) && !intent.getBooleanExtra(LAUNCHED_BY_WIDGET, false)) {
                // Run animations when exiting using up button (but not if launched from widget).
                // From answer to "Shared element transition when using ActionBar Back button" by Pei:
                // https://stackoverflow.com/questions/37713793/shared-element-transition-when-using-actionbar-back-button.
                // And answer to "Use of HomeAsUp in activity B to A do not use bundle" by Mahmoud A:
                // https://knowledge.udacity.com/questions/249672
                finishAfterTransition();
                return true;
            }
        }
        else if (id == R.id.action_stop_recording) {
            // If recording, stop and save.
            mRecordFragment.stopRecording();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Override to resume recording animation, if necessary.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if(mPermssionsGranted)
            this.onResumeRecording();
    }

    /***********************************
     * Implement View.OnClickListener. *
     ***********************************/

    /**
     * Handle click of stop FAB to stop recording.
     * @param view Stop FAB.
     */
    @Override
    public void onClick(View view) {
        mRecordFragment.stopRecording();
    }

    /*******************************************************
     * Implement RecordFragment.BirdcallRecordingListener. *
     *******************************************************/

    /**
     * Override to start recording animation.
     */
    @Override
    public void onRecordingStarted() {
        this.setTitle(getString(R.string.title_activity_started));
        mOnOffAir.setText(getString(R.string.record_on_air));
        mRecordingAnimator.start();
    }

    /**
     * Override to stop recording animation.
     */
    @Override
    public void onRecordingStopped() {
        this.setTitle(getString(R.string.title_activity_ended));
        mOnOffAir.setText(getString(R.string.record_off_air));
        mRecordingAnimator.end();
        mOnOffAir.setBackgroundColor(this.getResources().getColor(R.color.off_air_background, null)); // Off-air red is duller.
    }

    /**
     * Resume recording animation.
     * <p>Not part of BirdcallRecordingListener, but closely related to its handlers.</p>
     */
    private void onResumeRecording() {
        if (mRecordFragment.getRecordingState() == RecordFragment.RecordingStates.Started) {
            this.setTitle(getString(R.string.title_activity_started));
            mOnOffAir.setText(getString(R.string.record_on_air));
            mRecordingAnimator.start();
        }
        else if (mRecordFragment.getRecordingState() == RecordFragment.RecordingStates.Ended)
        {
            this.setTitle(getString(R.string.title_activity_ended));
        }
    }
}
