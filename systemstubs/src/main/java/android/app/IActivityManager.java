package android.app;

import android.os.IInterface;

public interface IActivityManager extends IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection
    {
        public static IActivityManager asInterface(android.os.IBinder obj)
        {
            throw new RuntimeException("Stub!");
        }
    }

}
