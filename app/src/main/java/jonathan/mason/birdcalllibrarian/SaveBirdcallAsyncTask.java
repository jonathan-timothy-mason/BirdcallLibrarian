package jonathan.mason.birdcalllibrarian;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jonathan.mason.birdcalllibrarian.Database.Birdcall;
import jonathan.mason.birdcalllibrarian.Database.BirdcallDatabase;

/**
 * Asynchronously load recording from temporary file, save to database, and
 * delete temporary file afterwards.
 * <p>AsyncTask is used safely as it keeps reference to application context and
 * does not refer to RecordActivity or RecordFragment.</p>
 */
public class SaveBirdcallAsyncTask extends AsyncTask<Void, Void, String> {
    Application mApplication;
    String mTemporaryFilename;
    Birdcall mBirdcall;

    /**
     * Constructor.
     * @param application The application.
     * @param temporaryFilename Name of temporary file from which to load
     * recording.
     * @param birdcall Birdcall to be saved to database.
     */
    public SaveBirdcallAsyncTask(Application application, String temporaryFilename, Birdcall birdcall) {
        mApplication = application;
        mTemporaryFilename = temporaryFilename;
        mBirdcall = birdcall;
    }

    /**
     * Perform task.
     * <p>Run on separate thread.</p>
     * @param noParameters Not used.
     * @return Concluding message to display to user, either success or error.
     */
    @Override
    protected String doInBackground(Void... noParameters) {
        File file = new File(mTemporaryFilename);
        if(file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                // Load recording from temporary file into memory.
                byte[] recording = new byte[(int)file.length()];
                stream.read(recording);
                mBirdcall.setRecording(recording);

                // Save birdcall, including recording into database.
                BirdcallDatabase.getInstance(mApplication).DAO().insert(mBirdcall);
            } catch (IOException e) {
                Log.e(RecordFragment.class.getSimpleName(), "doInBackground: problem loading temporary file \"" + mTemporaryFilename + ".", e);
                mApplication.getString(R.string.error_loading_temp_file); // Error message, to display to user.
            } finally {
                // Delete temporary file as it is no longer needed.
                file.delete();
            }
        }
        else {
            Log.e(RecordFragment.class.getSimpleName(), "doInBackground: temporary file \"" + mTemporaryFilename + "\" not found.");
            return mApplication.getString(R.string.error_temp_file_not_found); // Error message, to display to user.
        }

        return mApplication.getString(R.string.birdcall_saved); // Success message, to display to user.
    }

    /**
     * Let user know birdcall has been successfully saved to database, or if there
     * was an error.
     * <p>Run on main user interface thread.</p>
     * @param message Concluding message to display to user, either success or error.
     */
    @Override
    protected void onPostExecute(String message)
    {
        Toast.makeText(mApplication, message, Toast.LENGTH_LONG).show();
    }
}
