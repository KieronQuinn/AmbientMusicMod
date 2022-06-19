package com.kieronquinn.app.pixelambientmusic;

import com.kieronquinn.app.pixelambientmusic.IRecognitionCallback;
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange;
import com.kieronquinn.app.pixelambientmusic.model.RecognitionResult;
import com.kieronquinn.app.pixelambientmusic.model.RecognitionCallbackMetadata;

interface IRecognitionService {

    boolean ping();
    String addRecognitionCallback(in IRecognitionCallback callback, in RecognitionCallbackMetadata callbackMetadata);
    boolean removeRecognitionCallback(String id);
    void requestRecognition();
    void requestOnDemandRecognition();
    oneway void onConfigChanged(in List<String> configNames);

    oneway void updateSettingsState(in SettingsStateChange change);
    oneway void clearAlbumArtCache();

}