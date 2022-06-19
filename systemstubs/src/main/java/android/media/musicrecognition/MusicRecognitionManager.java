package android.media.musicrecognition;

import android.annotation.SuppressLint;
import android.media.MediaMetadata;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

public class MusicRecognitionManager {

    /**
     * Error code provided by RecognitionCallback#onRecognitionFailed()
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {RECOGNITION_FAILED_UNKNOWN,
                    RECOGNITION_FAILED_NOT_FOUND,
                    RECOGNITION_FAILED_NO_CONNECTIVITY,
                    RECOGNITION_FAILED_SERVICE_UNAVAILABLE,
                    RECOGNITION_FAILED_SERVICE_KILLED,
                    RECOGNITION_FAILED_TIMEOUT,
                    RECOGNITION_FAILED_AUDIO_UNAVAILABLE})
    public @interface RecognitionFailureCode {
    }

    /** Catchall error code. */
    public static final int RECOGNITION_FAILED_UNKNOWN = -1;
    /** Recognition was performed but no result could be identified. */
    public static final int RECOGNITION_FAILED_NOT_FOUND = 1;
    /** Recognition failed because the server couldn't be reached. */
    public static final int RECOGNITION_FAILED_NO_CONNECTIVITY = 2;
    /**
     * Recognition was not possible because the application which provides it is not available (for
     * example, disabled).
     */
    public static final int RECOGNITION_FAILED_SERVICE_UNAVAILABLE = 3;
    /** Recognition failed because the recognizer was killed. */
    public static final int RECOGNITION_FAILED_SERVICE_KILLED = 5;
    /** Recognition attempt timed out. */
    public static final int RECOGNITION_FAILED_TIMEOUT = 6;
    /** Recognition failed due to an issue with obtaining an audio stream. */
    public static final int RECOGNITION_FAILED_AUDIO_UNAVAILABLE = 7;

    /** Callback interface for the caller of this api. */
    public interface RecognitionCallback {
        /**
         * Should be invoked by receiving app with the result of the search.
         *
         * @param recognitionRequest original request that started the recognition
         * @param result result of the search
         * @param extras extra data to be supplied back to the caller. Note that all
         *               executable parameters and file descriptors would be removed from the
         *               supplied bundle
         */
        void onRecognitionSucceeded(@NonNull RecognitionRequest recognitionRequest,
                                    @NonNull MediaMetadata result,
                                    @SuppressLint("NullableCollection")
                                    @Nullable Bundle extras);

        /**
         * Invoked when the search is not successful (possibly but not necessarily due to error).
         *
         * @param recognitionRequest original request that started the recognition
         * @param failureCode failure code describing reason for failure
         */
        void onRecognitionFailed(@NonNull RecognitionRequest recognitionRequest,
                                 @RecognitionFailureCode int failureCode);

        /**
         * Invoked by the system once the audio stream is closed either due to error, reaching the
         * limit, or the remote service closing the stream.  Always called per
         * #beingStreamingSearch() invocation.
         */
        void onAudioStreamClosed();
    }

    public void beginStreamingSearch(
            @NonNull RecognitionRequest recognitionRequest,
            @NonNull Executor callbackExecutor,
            @NonNull RecognitionCallback callback) {
        throw new RuntimeException("Stub!");
    }

}
