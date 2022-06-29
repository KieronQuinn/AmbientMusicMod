package android.media.musicrecognition;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class RecognitionRequest implements Parcelable {

    protected RecognitionRequest(Parcel in) {
    }

    public static final Creator<RecognitionRequest> CREATOR = new Creator<RecognitionRequest>() {
        @Override
        public RecognitionRequest createFromParcel(Parcel in) {
            return new RecognitionRequest(in);
        }

        @Override
        public RecognitionRequest[] newArray(int size) {
            return new RecognitionRequest[size];
        }
    };

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public AudioAttributes getAudioAttributes() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public AudioFormat getAudioFormat() {
        throw new RuntimeException("Stub!");
    }

    public int getCaptureSession() {
        throw new RuntimeException("Stub!");
    }

    @SuppressWarnings("MethodNameUnits")
    public int getMaxAudioLengthSeconds() {
        throw new RuntimeException("Stub!");
    }

    public int getIgnoreBeginningFrames() {
        throw new RuntimeException("Stub!");
    }

}
