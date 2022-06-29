package android.media.musicrecognition;

/**
 * Interface from {@link MusicRecognitionService} to system to pass attribution tag.
 */
oneway interface IMusicRecognitionAttributionTagCallback {
  void onAttributionTag(in String attributionTag);
}