package com.example.fishstock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.List;
import java.util.Locale;

/**
 * Text to speech for the puzzle and record screens: says a list of sentences slowly, with a pause
 * after each, and tells the caller (on the main thread) when it has finished or was stopped.
 */
public final class Speaker {
  /** A little slower than normal speech, so each square sinks in. */
  private static final float RATE = 0.8f;
  private static final String LAST = "fishstock_last";

  private final Handler main = new Handler(Looper.getMainLooper());
  private final TextToSpeech tts;

  private boolean ready;
  private boolean failed;
  private boolean speaking;
  private Runnable onDone;
  private int session;

  public Speaker(Context context) {
    tts = new TextToSpeech(context.getApplicationContext(), this::onInit);
    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override
      public void onStart(String id) {
      }

      @Override
      public void onDone(String id) {
        if (id != null && id.startsWith(LAST)) finished(id);
      }

      @Override
      public void onError(String id) {
        if (id != null && id.startsWith(LAST)) finished(id);
      }

      @Override
      public void onStop(String id, boolean interrupted) {
        if (id != null && id.startsWith(LAST)) finished(id);
      }
    });
  }

  private void onInit(int status) {
    if (status != TextToSpeech.SUCCESS) {
      failed = true;
      return;
    }
    int r = tts.setLanguage(Locale.getDefault());
    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
      r = tts.setLanguage(Locale.US);
    }
    failed = r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED;
    tts.setSpeechRate(RATE);
    ready = !failed;
  }

  /** False until the phone's speech engine has started (or if it has none). */
  public boolean isReady() {
    return ready;
  }

  /** True if the phone has no usable text-to-speech for English. */
  public boolean isUnavailable() {
    return failed;
  }

  public boolean isSpeaking() {
    return speaking;
  }

  /**
   * Says each sentence in turn with pauseMs of silence after it (none after the last), replacing
   * anything being said. onDone runs on the main thread when it ends or is stopped.
   */
  public void say(List<String> sentences, long pauseMs, Runnable onDone) {
    stop();
    if (!ready || sentences.isEmpty()) {
      if (onDone != null) main.post(onDone);
      return;
    }
    session++;
    this.onDone = onDone;
    speaking = true;
    for (int i = 0; i < sentences.size(); i++) {
      boolean last = i == sentences.size() - 1;
      String id = (last ? LAST + session : "s" + session + "_" + i);
      tts.speak(sentences.get(i), TextToSpeech.QUEUE_ADD, null, id);
      if (!last && pauseMs > 0) tts.playSilentUtterance(pauseMs, TextToSpeech.QUEUE_ADD, "p" + session + "_" + i);
    }
  }

  /** Stops at once; the pending onDone still runs. */
  public void stop() {
    if (!speaking) return;
    speaking = false;
    Runnable r = onDone;
    onDone = null;
    tts.stop();
    if (r != null) main.post(r);
  }

  public void shutdown() {
    main.removeCallbacksAndMessages(null);
    onDone = null;
    speaking = false;
    tts.stop();
    tts.shutdown();
  }

  private void finished(String id) {
    main.post(() -> {
      if (!id.equals(LAST + session) || !speaking) return;
      speaking = false;
      Runnable r = onDone;
      onDone = null;
      if (r != null) r.run();
    });
  }
}
