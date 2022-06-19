package android.provider;

import androidx.annotation.NonNull;

import java.util.Set;
import java.util.concurrent.Executor;

public class DeviceConfig {

    public static String getProperty(@NonNull String namespace, @NonNull String name) {
        throw new RuntimeException("Stub!");
    }

    public static void addOnPropertiesChangedListener(
            @NonNull String namespace,
            @NonNull Executor executor,
            @NonNull OnPropertiesChangedListener onPropertiesChangedListener) {
        throw new RuntimeException("Stub!");
    }

    public interface OnPropertiesChangedListener {
        /**
         * Called when one or more properties have changed, providing a Properties object with all
         * of the changed properties. This object will contain only properties which have changed,
         * not the complete set of all properties belonging to the namespace.
         *
         * @param properties Contains the complete collection of properties which have changed for a
         *                   single namespace. This includes only those which were added, updated,
         *                   or deleted.
         */
        void onPropertiesChanged(@NonNull Properties properties);
    }

    public static class Properties {
        @NonNull
        public Set<String> getKeyset() {
            throw new RuntimeException("Stub!");
        }
    }

}
