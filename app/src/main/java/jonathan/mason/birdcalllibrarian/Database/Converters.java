package jonathan.mason.birdcalllibrarian.Database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Java and SQLite type converters.
 * <p>From "Reference complex data using Room, Use type converters", Android Developers:</p>
 * <p>https://developer.android.com/training/data-storage/room/referencing-data</p>
 */
public class Converters {
    /**
     * Convert supplied SQLite date and time to Java date and time.
     * @param value SQLite date and time.
     * @return Java date and time.
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * Convert supplied Java date and time to SQLite date and time.
     * @param date Java date and time.
     * @return SQLite date and time.
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}