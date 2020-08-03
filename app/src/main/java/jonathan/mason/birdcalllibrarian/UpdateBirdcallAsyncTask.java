package jonathan.mason.birdcalllibrarian;

import android.app.Application;
import android.os.AsyncTask;
import android.widget.Toast;

import jonathan.mason.birdcalllibrarian.Database.Birdcall;
import jonathan.mason.birdcalllibrarian.Database.BirdcallDatabase;

/**
 * Asynchronously updated birdcall in database.
 * <p>AsyncTask is used safely as it keeps reference to application context and
 * does not refer to DetailsActivity.</p>
 */
public class UpdateBirdcallAsyncTask extends AsyncTask<Void, Void, Void> {
    Application mApplication;
    Birdcall mBirdcall;

    /**
     * Constructor.
     * @param application The application.
     * @param birdcall Birdcall to be updated in database.
     */
    public UpdateBirdcallAsyncTask(Application application, Birdcall birdcall) {
        mApplication = application;
        mBirdcall = birdcall;
    }

    /**
     * Perform task.
     * <p>Run on separate thread.</p>
     * @param noParameters Not used.
     */
    @Override
    protected Void doInBackground(Void... noParameters) {
        // Update birdcall in database.
        BirdcallDatabase.getInstance(mApplication).DAO().update(mBirdcall);
        return null;
    }

    /**
     * Let user know birdcall has been successfully updated in database.
     * <p>Run on main user interface thread.</p>
     */
    @Override
    protected void onPostExecute(Void noParameter)
    {
        // Display success message to user.
        Toast.makeText(mApplication, mApplication.getString(R.string.birdcall_saved), Toast.LENGTH_LONG).show();
    }
}
