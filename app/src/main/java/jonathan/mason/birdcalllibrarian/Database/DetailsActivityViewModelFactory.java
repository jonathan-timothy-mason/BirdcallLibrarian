package jonathan.mason.birdcalllibrarian.Database;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Factory to create DetailsActivityViewModel to load specified birdcall.
 */
public class DetailsActivityViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {
    private final int mBirdcallId;
    private final Application mApplication;

    /**
     * Constructor.
     * @param application The app.
     * @param birdcallId Id of birdcall to load.
     */
    public DetailsActivityViewModelFactory(Application application, int birdcallId) {
        super(application);

        mApplication = application;
        mBirdcallId = birdcallId;
    }

    /**
     * Create DetailsActivityViewModel to load specified birdcall.
     * @param modelClass DetailsActivityViewModel class information.
     * @param <T> DetailsActivityViewModel type.
     * @return Instance of new DetailsActivityViewModel.
     */
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new DetailsActivityViewModel(mApplication, mBirdcallId);
    }
}
