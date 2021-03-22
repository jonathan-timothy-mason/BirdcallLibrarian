package jonathan.mason.birdcalllibrarian;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jonathan.mason.birdcalllibrarian.Database.Birdcall;
import jonathan.mason.birdcalllibrarian.Database.BirdcallDatabase;
import jonathan.mason.birdcalllibrarian.Database.MainActivityViewModel;

/**
 * Main screen of app, displaying birdcalls.
 *
 * To do:
 * Mandatory
 * Generate new Google Maps API key and release new version of app.
 *
 * Desirable
 * Make Google map navigable using retained fragment.
 */
public class MainActivity extends AppCompatActivity implements BirdcallsAdapter.BirdcallSelectionListener {
    @BindView(R.id.birdcalls_recycler_view) RecyclerView mBirdcallsRecyclerView;
    @BindView(R.id.record_fab) FloatingActionButton mRecordFAB;
    @BindView(R.id.toolbar) Toolbar mToolbar;

    /**
     * Perform initialisation of activity, including ViewModel to load birdcalls
     * into RecyclerView.
     * @param savedInstanceState Saved state of activity; not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve views.
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mRecordFAB.setOnClickListener(new View.OnClickListener() {
            /**
             * Handle click of record FAB to start recording.
             * @param view Record FAB.
             */
            @Override
            public void onClick(View view) {
                showRecordActivity();
            }
        });

        // Set up to use GridLayoutManager (BirdcallsAdapter created after birdcalls
        // loaded).
        int numberColumns = this.getResources().getInteger(R.integer.number_columns);
        mBirdcallsRecyclerView.setLayoutManager(new GridLayoutManager(this, numberColumns));

        this.addSwipeHelper();
        this.setupViewModel();
    }

    /**
     * Setup up ViewModel to load and cache birdcalls on separate thread for lifetime
     * of activity.
     */
    private void setupViewModel() {
        MainActivityViewModel viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        viewModel.getBirdcalls().observe(this, new Observer<List<Birdcall>>() {
            /**
             * Handle loading of birdcalls from database.
             * @param birdcalls Loaded birdcalls.
             */
            @Override
            public void onChanged(@Nullable List<Birdcall> birdcalls) {
                // Update RecyclerView.
                MainActivity.this.setRecyclerViewAdapter(birdcalls);
            }
        });
    }

    /**
     * Add ItemTouchHelper to RecyclerView to delete birdcall when swiping horizontally.
     * <p>Based on exercise "T09b.05-Exercise-DeleteTask" of Lesson 4, Android
     * Architecture Components, Developing Android Apps: Part 3 by Jose.</p>
     */
    private void addSwipeHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            /**
             * Handle moving of items; not used.
             * @param recyclerView RecyclerView.
             * @param viewHolder Moved view holder.
             * @param target Destination view holder.
             * @return False, as move not implemented.
             */
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) { return false; }

            /**
             * Handle horizontal swipe to delete birdcall.
             * @param viewHolder Swiped view holder.
             * @param swipeDir Direction of swipe; not used.
             */
            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                BirdcallsAdapter adapter = (BirdcallsAdapter)mBirdcallsRecyclerView.getAdapter();

                // Get swiped birdcall and its position.
                int position = viewHolder.getAdapterPosition();
                Birdcall birdcall = adapter.getBirdcalls().get(position);

                // Remove swiped birdcall from collection of birdcalls and notify adapter, to
                // allow it to animate remaining items up (birdcall not actually deleted from
                // database yet).
                // From answer to "Android RecyclerView ItemTouchHelper revert swipe and restore view holder"
                // by jimmy0251:
                // https://stackoverflow.com/questions/31787272/android-recyclerview-itemtouchhelper-revert-swipe-and-restore-view-holder
                adapter.getBirdcalls().remove(position);
                adapter.notifyItemRemoved(position);

                // Display Snackbar, offering user chance to undo.
                Snackbar.make(viewHolder.itemView, getResources().getString(R.string.birdcall_deleted), Snackbar.LENGTH_LONG).setAction(getResources().getString(R.string.birdcall_undo_delete), new View.OnClickListener() {
                    /**
                     * Handle click of Snackbar action to undo deletion.
                     * @param view Clicked Snackbar action button.
                     */
                    @Override
                    public void onClick(View view) {
                        // Insert removed birdcall back into collection of birdcalls and notify adapter, to
                        // allow it to animate shifting of other items.
                        adapter.getBirdcalls().add(position, birdcall);
                        adapter.notifyItemInserted(position);

                        // Ensure inserted birdcall is visible, in case it appears above or below
                        // other items.
                        // From answer to "How to use RecyclerView.scrollToPosition() to move the position to the top of current view?"
                        // by yugidroid:
                        // https://stackoverflow.com/questions/33328806/how-to-use-recyclerview-scrolltoposition-to-move-the-position-to-the-top-of-cu/33329765
                        mBirdcallsRecyclerView.getLayoutManager().scrollToPosition(position);
                    }
                }).addCallback(new Snackbar.Callback() {
                    /**
                     * Handle showing of Snackbar; not used.
                     * @param snackbar The Snackbar.
                     */
                    @Override
                    public void onShown(Snackbar snackbar) {
                        super.onShown(snackbar);
                    }

                    /**
                     * Handle closing of Snackbar to delete birdcall from database.
                     * <p>From answer to "How to dismiss a Snackbar using it's own Action button?" by a.black13:
                     * https://stackoverflow.com/questions/30729312/how-to-dismiss-a-snackbar-using-its-own-action-button.</p>
                     * @param snackbar The Snackbar.
                     * @param event Flag indicating how Snackbar was closed.
                     */
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);

                        // If Snackbar wasn't closed by an action (undo), delete birdcall from database.
                        if (event != DISMISS_EVENT_ACTION) {
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                /**
                                 * Task to executed on separate thread.
                                 */
                                @Override
                                public void run() {
                                    BirdcallDatabase.getInstance(MainActivity.this.getApplication()).DAO().delete(birdcall);
                                }
                            });
                        }
                    }
                }).show();
            }
        }).attachToRecyclerView(mBirdcallsRecyclerView);
    }

    /**
     * Add birdcalls to adapter and then to RecyclerView.
     * @param birdcalls Birdcalls to add.
     */
    private void setRecyclerViewAdapter(List<Birdcall> birdcalls) {
        // Create birdcalls adapter (set after birdcalls loaded to begin displaying).
        BirdcallsAdapter birdcallsAdapter = new BirdcallsAdapter(birdcalls,this);

        // Set adapter of RecycleView (this causes it to update itself).
        mBirdcallsRecyclerView.setAdapter(birdcallsAdapter);
    }

    /**
     * Override to create menu for screen.
     * @param menu Menu being created.
     * @return True to display menu, otherwise false.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        if (id == R.id.action_record) {
            // Start recording.
            this.showRecordActivity();
            return true;
        }
        else if (id == R.id.action_acknowledgments) {
            Intent intent = new Intent(this, AcknowledgmentsActivity.class);

            // From "Activity Enter and Exit" of Lesson 4, Meaningful Motion, Material Design
            // for Android Developers by Nick Butcher.
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, bundle);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show RecordActivity to start recording.
     */
    private void showRecordActivity() {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RecordActivity.LAUNCHED_BY_WIDGET, false); // Indicate NOT launched by widget as extra data in intent.

        // From "Activity Enter and Exit" of Lesson 4, Meaningful Motion, Material Design
        // for Android Developers by Nick Butcher.
        Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle();

        startActivity(intent, bundle);
    }

    /*********************************************************
     * Implement BirdcallsAdapter.BirdcallSelectionListener. *
     *********************************************************/

    /**
     * Handle selection of birdcall to show DetailsActivity screen.
     * @param selectedBirdcall Selected birdcall.
     */
    public void onBirdcallSelected(Birdcall selectedBirdcall)
    {
        this.showDetailsActivity(selectedBirdcall);
    }

    /**
     * Show details of "selectedBirdcall" in DetailsActivity screen.
     * @param selectedBirdcall Birdcall to be shown.
     */
    private void showDetailsActivity(Birdcall selectedBirdcall) {
        Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
        intent.putExtra(Birdcall.SELECTED_BIRDCALL_ID, selectedBirdcall.getId()); // Pass ID of selected birdcall as extra data in intent.

        // From "Activity Enter and Exit" of Lesson 4, Meaningful Motion, Material Design
        // for Android Developers by Nick Butcher.
        Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

        startActivity(intent, bundle);
    }
}
