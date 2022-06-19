package com.kieronquinn.app.ambientmusicmod.utils.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfers a sparsearray with lists having {@link AccessibilityWindowInfo}s across an IPC.
 * The key of this sparsearray is display Id.
 */
public final class WindowListSparseArray
        extends SparseArray<List<AccessibilityWindowInfo>> implements Parcelable {

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<WindowListSparseArray> CREATOR =
            new Creator<WindowListSparseArray>() {
                public WindowListSparseArray createFromParcel(
                        Parcel source) {
                    throw new RuntimeException("Stub!");
                }

                public WindowListSparseArray[] newArray(int size) {
                    throw new RuntimeException("Stub!");
                }
            };
}