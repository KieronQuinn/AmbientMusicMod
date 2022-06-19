package android.hardware;

import androidx.annotation.NonNull;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SensorPrivacyManager.class)
public class SensorPrivacyManagerHidden {

    public void addSensorPrivacyListener(int sensor, OnSensorPrivacyChangedListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void removeSensorPrivacyListener(int sensor, @NonNull OnSensorPrivacyChangedListener listener) {
        throw new RuntimeException("Stub!");
    }

    public boolean isSensorPrivacyEnabled(int sensor) {
        throw new RuntimeException("Stub!");
    }

    public interface OnSensorPrivacyChangedListener {
        /**
         * Callback invoked when the sensor privacy state changes.
         *
         * @param sensor  the sensor whose state is changing
         * @param enabled true if sensor privacy is enabled, false otherwise.
         */
        void onSensorPrivacyChanged(int sensor, boolean enabled);
    }

}
