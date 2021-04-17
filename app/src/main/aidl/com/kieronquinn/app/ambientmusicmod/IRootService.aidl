// IRootService.aidl
package com.kieronquinn.app.ambientmusicmod;

import android.os.ParcelUuid;

interface IRootService {

    int getModelState(in ParcelUuid uuid);

}