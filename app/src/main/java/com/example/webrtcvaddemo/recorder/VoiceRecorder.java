package com.example.webrtcvaddemo.recorder;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

public class VoiceRecorder {

    private static final String TAG = VoiceRecorder.class.getSimpleName();

    private AudioCallback callback;
    private AudioRecord audioRecord;
    private Thread thread;
    private boolean isListening;

    private int sampleRate;
    private int frameSize;

    public VoiceRecorder(AudioCallback callback) {
        this.callback = callback;
    }

    public void start(int sampleRate, int frameSize) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        stop();

        audioRecord = createAudioRecord();
        if (audioRecord != null) {
            isListening = true;
            audioRecord.startRecording();

            thread = new Thread(new ProcessVoice());
            thread.start();
        }
    }

    public void stop() {
        isListening = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    @SuppressLint("MissingPermission")
    private AudioRecord createAudioRecord() {
        try {
            int minBufferSize = Math.max(
                    AudioRecord.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    2 * frameSize
            );

            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
            );

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                return audioRecord;
            } else {
                audioRecord.release();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error can't create AudioRecord ", e);
        }
        return null;
    }

    private class ProcessVoice implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int size = frameSize;

            while (!Thread.interrupted() && isListening) {
                short[] buffer = new short[size];
                if (audioRecord != null) {
                    audioRecord.read(buffer, 0, buffer.length);
                    callback.onAudio(buffer);
                }
            }
        }
    }

    public interface AudioCallback {
        void onAudio(short[] audioData);
    }
}
