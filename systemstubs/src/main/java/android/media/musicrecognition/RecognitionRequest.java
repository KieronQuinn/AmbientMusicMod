package android.media.musicrecognition;

import android.os.Parcel;
import android.os.Parcelable;

public class RecognitionRequest implements Parcelable {

    //Stub

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
}
