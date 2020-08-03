package jonathan.mason.birdcalllibrarian.Database;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

/**
 * Birdcall database; a singleton.
 */
@Database(entities = {Birdcall.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class BirdcallDatabase extends RoomDatabase {

    private final static Object LOCK = new Object();
    private static BirdcallDatabase mInstance = null;

    /**
     * Implement singleton, creating single instance of birdcall database, if not created,
     * or simply retrieving previously created instance.
     * @param application Application with which to create database, if necessary.
     * @return Birdcall database.
     */
    public static BirdcallDatabase getInstance(Application application) {
        if (mInstance == null) {
            synchronized (LOCK) {
                mInstance = Room.databaseBuilder(application, BirdcallDatabase.class, BirdcallDatabase.class.getSimpleName()).build();
            }
        }

        return mInstance;
    }

    /**
     * Birdcall database DAO interface.
     */
    @Dao
    public interface BirdcallDatabaseDAO {

        /**
         * Get all birdcalls.
         * @return List of birdcalls.
         */
        @Query("SELECT * FROM Birdcalls ORDER BY DateAndTime DESC")
        LiveData<List<Birdcall>> load();

        @Query("SELECT * FROM Birdcalls WHERE Id = :id")
        LiveData<Birdcall> loadBirdcall(int id);

        /**
         * Insert birdcall into database.
         * @param birdcall Birdcall to insert.
         */
        @Insert
        void insert(Birdcall birdcall);

        /**
         * Update birdcall in database.
         * @param birdcall Birdcall to update.
         */
        @Update
        void update(Birdcall birdcall);

        /**
         * Delete birdcall from database.
         * @param birdcall Birdcall to delete.
         */
        @Delete
        void delete(Birdcall birdcall);
    }

    /**
     * Get instance of birdcall database DAO.
     * @return Instance of birdcall database DAO.
     */
    public abstract BirdcallDatabaseDAO DAO();
}
