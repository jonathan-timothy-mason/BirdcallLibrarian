package jonathan.mason.birdcalllibrarian.Database;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.Date;

import jonathan.mason.birdcalllibrarian.R;

/**
 * Class represents a birdcall.
 */
@Entity(tableName = "Birdcalls")
public class Birdcall {
    /**
     * Key for storing state in bundles.
     */
    public static final String SELECTED_BIRDCALL_ID = "SELECTED_BIRDCALL_ID";

    /**
     * Temporary filename prefix for recording.
     */
    public static final String TEMP_FILE_PREFIX = "birdcall";

    /**
     * Get default birdcall title based upon whether it is morning, afternoon,
     * evening or night.
     * <p>From answer to "Showing Morning, afternoon, evening, night message based on Time in java"
     * by SMA: https://stackoverflow.com/questions/27589701/showing-morning-afternoon-evening-night-message-based-on-time-in-java</p>
     * @param context Context for loading string resources.
     * @return Default title.
     */
    public static String getDefaultTitle(Context context) {
        Calendar c = Calendar.getInstance();
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);

        String timeDescription;
        if(hourOfDay >= 21 || hourOfDay < 4)
            timeDescription = context.getString(R.string.default_birdcall_title_night);
        else if(hourOfDay >= 18)
            timeDescription = context.getString(R.string.default_birdcall_title_evening);
        else if(hourOfDay >= 12)
            timeDescription = context.getString(R.string.default_birdcall_title_afternoon);
        else // From 4am.
            timeDescription = context.getString(R.string.default_birdcall_title_morning);

        return context.getString(R.string.default_birdcall_title, timeDescription);
    }

    /**
     * Constructor.
     * <p>For use by app when first creating birdcall before saving for first time.</p>
     * @param species Species of bird.
     * @param title Title of birdcall.
     * @param dateAndTime Date and time of birdcall.
     * @param longitude Longitude of birdcall.
     * @param latitude Latitude of birdcall.
     * @param notes Notes about bird.
     * @param recording Recorded birdcall.
     */
    @Ignore
    public Birdcall(String species, String title, Date dateAndTime, double longitude, double latitude, String notes, byte[] recording)
    {
        mSpecies = species;
        mTitle = title;
        mDateAndTime = dateAndTime;
        mLongitude = longitude;
        mLatitude = latitude;
        mNotes = notes;
        mRecording = recording;
    }

    /**
     * Constructor.
     * <p>For use by Room.</p>
     * @param id Auto-generated ID of birdcall.
     * @param species Species of bird.
     * @param title Title of birdcall.
     * @param dateAndTime Date and time of birdcall.
     * @param longitude Longitude of birdcall.
     * @param latitude Latitude of birdcall.
     * @param notes Notes about bird.
     * @param recording Recorded birdcall.
     */
    public Birdcall(int id, String species, String title, Date dateAndTime, double longitude, double latitude, String notes, byte[] recording)
    {
        mId = id;
        mSpecies = species;
        mTitle = title;
        mDateAndTime = dateAndTime;
        mLongitude = longitude;
        mLatitude = latitude;
        mNotes = notes;
        mRecording = recording;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Id")
    private int mId;
    /**
     * Get ID of birdcall.
     * @return ID of birdcall.
     */
    public int getId()
    {
        return mId;
    }

    @ColumnInfo(name = "Title")
    private String mTitle;
    /**
     * Get title of birdcall.
     * @return Title of birdcall.
     */
    public String getTitle()
    {
        return mTitle;
    }

    /**
     * Set title of birdcall.
     * @param title Title of birdcall.
     */
    public void setTitle(String title) { mTitle = title; }

    @ColumnInfo(name = "Species")
    private String mSpecies;
    /**
     * Get species of bird.
     * @return Species of bird.
     */
    public String getSpecies()
    {
        return mSpecies;
    }

    /**
     * Set species of bird.
     * @param species Species of bird.
     */
    public void setSpecies(String species) { mSpecies = species; }

    @ColumnInfo(name = "DateAndTime")
    private Date mDateAndTime;
    /**
     * Get date and time of birdcall.
     * @return Date and time of birdcall.
     */
    public Date getDateAndTime()
    {
        return mDateAndTime;
    }

    @ColumnInfo(name = "Longitude")
    private double mLongitude;
    /**
     * Get longitude of birdcall.
     * @return Longitude of birdcall.
     */
    public double getLongitude()
    {
        return mLongitude;
    }

    @ColumnInfo(name = "Latitude")
    private double mLatitude;
    /**
     * Get latitude of birdcall.
     * @return Latitude of birdcall.
     */
    public double getLatitude()
    {
        return mLatitude;
    }

    @ColumnInfo(name = "Notes")
    private String mNotes;
    /**
     * Get notes about bird.
     * @return Notes about bird.
     */
    public String getNotes()
    {
        return mNotes;
    }

    /**
     * Set notes about bird.
     * @param notes Notes about bird.
     */
    public void setNotes(String notes) { mNotes = notes; }

    @ColumnInfo(name = "Recording", typeAffinity = ColumnInfo.BLOB)
    private byte[] mRecording;
    /**
     * Get recorded birdcall.
     * <p>Mapping byte[] to SQLite BLOB from answer to "Room library won't accept byte array" by Pinakin:
     * https://stackoverflow.com/questions/45184504/room-library-wont-accept-byte-array.</p>
     * @return Recorded birdcall.
     */
    public byte[] getRecording()
    {
        return mRecording;
    }

    /**
     * Set recorded birdcall.
     * @param recording Recorded birdcall.
     */
    public void setRecording(byte[] recording) {
        mRecording = recording;
    }
}
