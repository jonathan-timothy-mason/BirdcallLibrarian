package jonathan.mason.birdcalllibrarian.Database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

/**
 * ViewModel to load and keep specified birdcall in memory through life cycle.
 */
public class DetailsActivityViewModel extends AndroidViewModel {

    /**
     * Constructor.
     * @param application The app.
     * @param birdcallId Id of birdcall to load.
     */
    public DetailsActivityViewModel(Application application, int birdcallId) {
        super(application);

        mBirdcall = BirdcallDatabase.getInstance(application).DAO().loadBirdcall(birdcallId);
    }

    private LiveData<Birdcall> mBirdcall;
    /**
     * Get birdcall.
     * @return Birdcall.
     */
    public LiveData<Birdcall> getBirdcall() {
        return mBirdcall;
    }
}
