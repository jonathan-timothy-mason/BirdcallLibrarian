package jonathan.mason.birdcalllibrarian;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Screen of app for showing acknowledgments.
 */
public class AcknowledgmentsActivity extends AppCompatActivity {
    /**
     * Perform default initialisation of activity.
     * @param savedInstanceState Saved state of activity: not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acknowledgments);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        return super.onOptionsItemSelected(item);
    }
}
