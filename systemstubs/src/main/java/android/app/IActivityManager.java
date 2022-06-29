package android.app;

import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;

public interface IActivityManager extends IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection
    {
        public static IActivityManager asInterface(android.os.IBinder binder)
        {
            throw new RuntimeException("Stub!");
        }
    }

    int bindIsolatedService(
            IApplicationThread caller,
            IBinder token,
            Intent service,
            String resolvedType,
            IServiceConnection connection,
            int flags,
            String instanceName,
            String callingPackage,
            int userId);

    //Android 13
    int bindServiceInstance(
            IApplicationThread caller,
            IBinder token,
            Intent service,
            String resolvedType,
            IServiceConnection connection,
            int flags,
            String instanceName,
            String callingPackage,
            int userId);

    boolean unbindService(IServiceConnection serviceConnection);

}
