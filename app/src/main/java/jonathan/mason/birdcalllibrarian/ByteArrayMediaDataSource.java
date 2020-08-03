package jonathan.mason.birdcalllibrarian;

import android.media.MediaDataSource;

/**
 * In memory data source for MediaPlayer.
 * <p>From "Implementing your own Android MediaDataSource" by Mark Ian Jackson:
 * https://medium.com/@jacks205/implementing-your-own-android-mediadatasource-e67adb070731.
 * And answer to "Android - Playing mp3 from byte[]" by krishnakumarp:
 * https://stackoverflow.com/questions/1972027/android-playing-mp3-from-byte.</p>
 */
public class ByteArrayMediaDataSource extends MediaDataSource {

    private byte[] mData;

    /**
     * Constructor.
     * @param data Data to be played.
     */
    public ByteArrayMediaDataSource(byte[] data) {
        mData = data;
    }

    /**
     * Read "size" bytes of data from point specified by "position" into supplied "buffer"
     * at point specified by "offset".
     * @param position Point at which to read data.
     * @param buffer Buffer into which to write data.
     * @param offset Point in buffer at which to write data.
     * @param size Number of bytes to read.
     * @return Number of bytes read or -1 on error or end of data.
     */
    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) {
        // If position is beyond end of byte array, indicate end of data.
        if (position >= mData.length)
            return -1;

        // If size of data at specified position would go beyond end of data,
        // adjust size to be read.
        if ((position + size) > mData.length)
            size -= (position + size) - mData.length;

        // Read data.
        System.arraycopy(mData, (int)position, buffer, offset, size);

        // Return bytes read.
        return size;
    }

    /**
     * Get total number of bytes of data.
     * @return Total number of bytes of data.
     */
    @Override
    public long getSize() {
        return mData.length;
    }

    /**
     * Must implement, but not needed.
     */
    @Override
    public void close() { }
}
