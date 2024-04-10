package com.example.webrtcvaddemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.webrtcvaddemo.recorder.VoiceRecorder;
import com.konovalov.vad.yamnet.SoundCategory;
import com.konovalov.vad.yamnet.Vad;
import com.konovalov.vad.yamnet.VadYamnet;
import com.konovalov.vad.yamnet.config.FrameSize;
import com.konovalov.vad.yamnet.config.Mode;
import com.konovalov.vad.yamnet.config.SampleRate;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements VoiceRecorder.AudioCallback {
    private VadYamnet vad;
    private VoiceRecorder recorder;
    // 採樣率
    private final SampleRate DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K;
    // 音訊幀
    private final FrameSize DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_243;
    // 模式
    private final Mode DEFAULT_MODE = Mode.NORMAL;
    // 判斷講話跟沉默的時間
    private final int DEFAULT_SILENCE_DURATION_MS = 30;
    private final int DEFAULT_SPEECH_DURATION_MS = 30;
    private TextView speechTextView;
    private Button recordingButton, stopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 建立 vad 塞入預設參數
        vad = Vad.builder()
                .setContext(this)
                .setSampleRate(DEFAULT_SAMPLE_RATE)
                .setFrameSize(DEFAULT_FRAME_SIZE)
                .setMode(DEFAULT_MODE)
                .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
                .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
                .build();

        // 初始化 VoiceRecorder
        recorder = new VoiceRecorder(this);

        // 初始化物件
        initView();

        // 錄音按鈕點擊事件
        recordingButton.setOnClickListener(view -> {
            startRecording();
        });

        // 暫停錄音按鈕點擊事件
        stopButton.setOnClickListener(view -> {
            stopRecording();
        });

        // 確認是否有通過錄音權限許可
        activateRecordingButtonWithPermissionCheck();
    }

    private void startRecording() {
        // 調用 recorder 中的 start 並傳入 音訊幀 跟 採樣率 開始錄音
        recorder.start(vad.getSampleRate().getValue(), vad.getFrameSize().getValue());
    }

    private void stopRecording() {
        // 調用 recorder 中的 stop 暫停錄音
        speechTextView.setText("暫停錄音");
        recorder.stop();
    }

    // 確認是否有通過錄音權限許可
    private void activateRecordingButtonWithPermissionCheck() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
    }

    // 初始化物件
    private void initView() {
        speechTextView = findViewById(R.id.textView);
        recordingButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
    }

    // 偵測有沒有講話的聲音，有就讓 textView 顯示 speech detected，反之 noise detected
    @Override
    public void onAudio(short[] audioData) {
        String speech = "Speech";
        SoundCategory soundCategory = vad.classifyAudio(speech, audioData);

        runOnUiThread(() -> {
            if (soundCategory.getLabel().equals(speech)) {
                speechTextView.setText(R.string.speech_detected);
            } else {
                speechTextView.setText(R.string.noise_detected);
            }
        });
    }

    // 釋放資源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.stop();
        vad.close();
    }
}