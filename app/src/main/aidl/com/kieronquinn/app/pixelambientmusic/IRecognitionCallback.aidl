package com.kieronquinn.app.pixelambientmusic;

import com.kieronquinn.app.pixelambientmusic.model.RecognitionResult;
import com.kieronquinn.app.pixelambientmusic.model.RecognitionFailure;
import com.kieronquinn.app.pixelambientmusic.model.RecognitionMetadata;

interface IRecognitionCallback {

    oneway void onRecordingStarted();
    oneway void onRecognitionStarted();
    oneway void onRecognitionSucceeded(in RecognitionResult result, in RecognitionMetadata metadata);
    oneway void onRecognitionFailed(in RecognitionFailure result);

}