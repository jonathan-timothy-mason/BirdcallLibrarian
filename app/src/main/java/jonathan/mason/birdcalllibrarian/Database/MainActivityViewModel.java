package jonathan.mason.birdcalllibrarian.Database;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import jonathan.mason.birdcalllibrarian.DetailsActivity;

/**
 * ViewModel to load and keep birdcalls in memory through life cycle.
 */
public class MainActivityViewModel extends AndroidViewModel {

    /**
     * Constructor.
     * @param application The app.
     */
    public MainActivityViewModel(Application application) {
        super(application);

        mBirdcalls = BirdcallDatabase.getInstance(getApplication()).DAO().load();
    }

    private LiveData<List<Birdcall>> mBirdcalls;
    /**
     * Get birdcalls.
     * @return Birdcalls.
     */
    public LiveData<List<Birdcall>> getBirdcalls() {
        return mBirdcalls;
    }
}
