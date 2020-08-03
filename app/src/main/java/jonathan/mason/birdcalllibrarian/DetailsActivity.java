package jonathan.mason.birdcalllibrarian;

import android.media.MediaPlayer;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import jonathan.mason.birdcalllibrarian.Database.Birdcall;
import jonathan.mason.birdcalllibrarian.Database.DetailsActivityViewModel;
import jonathan.mason.birdcalllibrarian.Database.DetailsActivityViewModelFactory;

/**
 * Screen of app for playing birdcall and viewing or editing its details.
 */
public class DetailsActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    /**
     * Key(s) for storing data in bundle.
     */
    private static final String IS_PLAYING = "IS_PLAYING";
    private static final String CURRENT_POSITION = "CURRENT_POSITION";
    private static final String CHANGED = "CHANGED";

    /**
     * Default zoom level of map.
     * <p>Zoomed in quite a lot.</p>
     */
    private static final float DEFAULT_ZOOM_LEVEL = 14.0f;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.play_fab) FloatingActionButton mPlayFAB;
    @BindView(R.id.constraint_layout) ConstraintLayout mConstraintLayout;
    @BindView(R.id.species) EditText mSpecies;
    @BindView(R.id.title) EditText mTitle;
    @BindView(R.id.notes) EditText mNotes;
    @BindView(R.id.date_and_time) TextView mDateAndTime;
    @BindView(R.id.lat_long) TextView mLatLong;

    private Birdcall mBirdcall;
    private MediaPlayer mMediaPlayer;
    private GoogleMap mMap;

    private Boolean mSavedIsPlaying;
    private int mSavedCurrentPosition;

    private Boolean mChanged;

    /**
     * Perform initialisation of activity, including ViewModel to load birdcall.
     * @param savedInstanceState Saved state of activity: playback state and position.
     * @exception RuntimeException Thrown if intent does not contain "Birdcall.SELECTED_BIRDCALL_ID"
     * key within extra data or if its ID is invalid.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // From answer to "How to stop Soft keyboard showing automatically when focus is changed
        // (OnStart event)" by MobileCushion:
        // https://stackoverflow.com/questions/5221622/how-to-stop-soft-keyboard-showing-automatically-when-focus-is-changed-onstart-e.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (this.getIntent().hasExtra(Birdcall.SELECTED_BIRDCALL_ID) == false)
            throw new RuntimeException("No birdcall ID passed to " + this.toString() + ".");
        int birdcallId = this.getIntent().getIntExtra(Birdcall.SELECTED_BIRDCALL_ID, -1);
        if(birdcallId < 0)
            throw new RuntimeException("Invalid birdcall ID passed to " + this.toString() + ".");

        // Retrieve views.
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mPlayFAB.setOnClickListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup map.
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            /**
             * Handle map ready to set up location of birdcall.
             * <p>Birdcall may not have been loaded yet.</p>
             */
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                setLocationOnMap();
            }
        });

        // Restore state and position of MediaPlayer.
        mSavedIsPlaying = false;
        mSavedCurrentPosition = 0;
        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(IS_PLAYING))
                mSavedIsPlaying = savedInstanceState.getBoolean(IS_PLAYING, false);
            if(savedInstanceState.containsKey(CURRENT_POSITION))
                mSavedCurrentPosition = savedInstanceState.getInt(CURRENT_POSITION, 0);
        }

        // Restore state of changed flag.
        mChanged = false;
        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey(CHANGED))
                mChanged = savedInstanceState.getBoolean(CHANGED, false);
        }

        this.setupViewModel(birdcallId);
    }

    /**
     * Setup up ViewModel to load and cache birdcall specified by "birdcallId" on separate
     * thread for lifetime of activity.
     * <p>Based on exercise "T09b.10-Exercise-AddViewModelToAddTaskActivity" of Lesson 4,
     * Android Architecture Components, Developing Android Apps: Part 3 by Jose.</p>
     * @param birdcallId ID of birdcall to load.
     */
    private void setupViewModel(int birdcallId) {
        DetailsActivityViewModelFactory factory = new DetailsActivityViewModelFactory(this.getApplication(), birdcallId);
        DetailsActivityViewModel viewModel = new ViewModelProvider(this, factory).get(DetailsActivityViewModel.class);
        viewModel.getBirdcall().observe(this, new Observer<Birdcall>() {
            /**
             * Handle loading of birdcall from database.
             * @param birdcall Loaded birdcall.
             */
            @Override
            public void onChanged(@Nullable Birdcall birdcall) {
                mBirdcall = birdcall;

                // Stop observing as ViewModel and LiveData only used for one-off load,
                // there can't be any more changes at this stage.
                viewModel.getBirdcall().removeObserver(this);

                // Set up location of birdcall on map (map may not be ready yet).
                setLocationOnMap();

                // Set controls of screen with details of birdcall.
                setBirdcallDetails();

                // Avoid editText listeners "hearing" their own initialisation
                // with details of birdcall, by adding listeners afterwards.
                setupEditTextWatchers();

                // Resume playing birdcall, if it was playing.
                DetailsActivity.this.resumeMediaPlayer();
            }
        });
    }

    /**
     * Add handlers to species, title and notes editText to handle changes.
     * <p>Avoid editText listeners "hearing" their own initialisation with
     * details of birdcall, by adding listeners afterwards.
     * </p>
     */
    private void setupEditTextWatchers() {
        // Species.
        mSpecies.addTextChangedListener(new TextWatcher() {
            /**
             * Must be implemented, but not used.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            /**
             * Handle change of species.
             * @param s New entire text of editText.
             * @param start Index of first character at which change was made.
             * @param before Number of characters replaced from "start".
             * @param count Number of characters added from "start".
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If called whilst restoring state after configuration change, ignore.
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    mBirdcall.setSpecies(s.toString());
                    mChanged = true;
                }
            }

            /**
             * Must be implemented, but not used.
             */
            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Title.
        mTitle.addTextChangedListener(new TextWatcher() {
            /**
             * Must be implemented, but not used.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            /**
             * Handle change of title.
             * @param s New entire text of editText.
             * @param start Index of first character at which change was made.
             * @param before Number of characters replaced from "start".
             * @param count Number of characters added from "start".
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If called whilst restoring state after configuration change, ignore.
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    mBirdcall.setTitle(s.toString());
                    mChanged = true;
                }
            }

            /**
             * Must be implemented, but not used.
             */
            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Notes.
        mNotes.addTextChangedListener(new TextWatcher() {
            /**
             * Must be implemented, but not used.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            /**
             * Handle change of notes.
             * @param s New entire text of editText.
             * @param start Index of first character at which change was made.
             * @param before Number of characters replaced from "start".
             * @param count Number of characters added from "start".
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If called whilst restoring state after configuration change, ignore.
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    mBirdcall.setNotes(s.toString());
                    mChanged = true;
                }
            }

            /**
             * Must be implemented, but not used.
             */
            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Override to create menu for screen.
     * @param menu Menu being created.
     * @return True to display menu, otherwise false.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
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
            // Run animations when exiting using up button.
            // From answer to "Shared element transition when using ActionBar Back button" by Pei:
            // https://stackoverflow.com/questions/37713793/shared-element-transition-when-using-actionbar-back-button.
            // And answer to "Use of HomeAsUp in activity B to A do not use bundle" by Mahmoud A:
            // https://knowledge.udacity.com/questions/249672
            finishAfterTransition();
            return true;
        }
        else if (id == R.id.action_update_birdcall) {
            // Save birdcall, if changed.
            this.saveBirdcall();
            return true;
        }
        else if (id == R.id.action_play_birdcall) {
            // Play birdcall, if not already playing.
            this.startMediaPlayer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Set location of birdcall on map, if birdcall has been loaded, and map
     * is ready.
     */
    private void setLocationOnMap() {
        if((mMap != null) && (mBirdcall != null)) {
            LatLng location = new LatLng(mBirdcall.getLatitude(), mBirdcall.getLongitude());
            mMap.addMarker(new MarkerOptions().position(location));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM_LEVEL));
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
        }
    }

    /**
     * Set controls of screen with details of birdcall.
     */
    private void setBirdcallDetails() {
        mSpecies.setText(mBirdcall.getSpecies());
        mTitle.setText(mBirdcall.getTitle());
        mNotes.setText(mBirdcall.getNotes());
        mDateAndTime.setText(DateFormat.getInstance().format(mBirdcall.getDateAndTime()));
        mLatLong.setText(getString(R.string.lat_long, mBirdcall.getLatitude(), mBirdcall.getLongitude()));
    }

    /**
     * Override to save state and position of MediaPlayer, and whether birdcall has
     * been edited.
     * @param outState Saved state of activity.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(CHANGED, mChanged);

        if(mMediaPlayer != null) {
            mSavedIsPlaying = mMediaPlayer.isPlaying();
            mSavedCurrentPosition = mMediaPlayer.getCurrentPosition();
            outState.putBoolean(IS_PLAYING, mSavedIsPlaying);
            outState.putInt(CURRENT_POSITION, mSavedCurrentPosition);
        }
    }

    /**
     * Override to resume playing of birdcall, if it was playing.
     */
    @Override
    protected void onStart() {
        super.onStart();

        this.resumeMediaPlayer();
    }

    /**
     * Override to pause playing of birdcall, if it was playing,
     * and update birdcall in database, if changed.
     */
    @Override
    protected void onStop() {
        super.onStop();

        this.stopMediaPlayer();

        // Save birdcall, if changed.
        if(!this.isChangingConfigurations())
            this.saveBirdcall();
    }

    /**
     * Play birdcall from beginning.
     */
    private void startMediaPlayer() {
        this.startMediaPlayer(0);
    }

    /**
     * Play or resume birdcall from position specified by "currentPosition".
     * @param currentPosition Position at which to start playing birdcall.
     */
    private void startMediaPlayer(int currentPosition) {
        if (mBirdcall != null) {
            if (mMediaPlayer == null) {
                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setDataSource(new ByteArrayMediaDataSource(mBirdcall.getRecording()));
                    mMediaPlayer.prepare();
                    if(currentPosition > 0)
                        mMediaPlayer.seekTo(currentPosition);
                    mMediaPlayer.start();
                    Toast.makeText(this, getString(R.string.playing_birdcall), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(this.getClass().getSimpleName(), "startMediaPlayer: error preparing to play.", e);
                    Toast.makeText(this, this.getString(R.string.error_preparing_to_play), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Resume playing birdcall from saved position.
     */
    private void resumeMediaPlayer() {
        if (mBirdcall != null) {
            if (mMediaPlayer == null) {
                if (mSavedIsPlaying) {
                    this.startMediaPlayer(mSavedCurrentPosition);

                    // Once used to resume playing, clear saved state.
                    mSavedIsPlaying = false;
                    mSavedCurrentPosition = 0;
                }
            }
        }
    }

    /**
     * Stop or pause playing of birdcall, releasing MediaPlayer.
     */
    private void stopMediaPlayer() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Update birdcall in database using AsyncTask, if it has changed.
     * <p>AsyncTask is used safely as it keeps reference to application context
     * and does not refer to DetailsActivity.</p>
     */
    private void saveBirdcall() {
        if(mChanged) {
            mChanged = false;
            new UpdateBirdcallAsyncTask(this.getApplication(), mBirdcall).execute();
        }
    }

    /***********************************
     * Implement View.OnClickListener. *
     ***********************************/

    /**
     * Handle click of play FAB to play birdcall.
     * @param view Play FAB.
     */
    @Override
    public void onClick(View view) {
        this.startMediaPlayer();
    }

    /***********************************************
     * Implement MediaPlayer.OnCompletionListener. *
     ***********************************************/

    /**
     * Handle completion of playing birdcall to release MediaPlayer.
     * @param mp Instance of MediaPlayer; not used.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        this.stopMediaPlayer();
    }

    /******************************************
     * Implement MediaPlayer.OnErrorListener. *
     ******************************************/

    /**
     * Handle MediaPlayer error event.
     * @param mp Instance of MediaPlayer; not used.
     * @param what Type of error.
     * @param extra Extra code about error.
     * @return False to indicate error unhandled.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(this.getClass().getSimpleName(), "onError: type = " + what + ", extra = " + extra + ".");
        Toast.makeText(this, this.getString(R.string.error_during_playback), Toast.LENGTH_LONG).show();
        return false;
    }
}
