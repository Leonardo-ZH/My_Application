// app/src/main/java/com/leonardo/myapplication/ui/livecheck/LiveCheckActivity.java
package com.leonardo.myapplication.ui.livecheck;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.leonardo.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业二：口播脏话 + 情绪检测（多模态：文本(中英) + 声音响度 + 简单画面亮度）
 */
public class LiveCheckActivity extends AppCompatActivity {

    private static final String TAG = "LiveCheckActivity";

    private PlayerView playerView;
    private ExoPlayer player;

    private TextView tvStatus;
    private TextView tvSubtitle;
    private TextView tvProfanitySummary;
    private TextView tvEmotion;
    private Button btnDetect;

    // ====== 声音响度相关（通过 Visualizer 采样） ======
    private Visualizer visualizer;
    // 最近一次采集到的平均波形幅度
    private int currentAmplitude = 0;
    // 播放期间观测到的最大幅度，用于判断是否“很吵”
    private int maxAmplitude = 0;
    // 记录一段时间内的响度历史，做平滑与更鲁棒的判断
    private final List<Integer> amplitudeHistory = new ArrayList<>();
    private static final int HISTORY_MAX_SIZE = 30; // 记录最近 30 帧

    // ====== 简单画面亮度特征（通过每秒抓一帧 PlayerView） ======
    private int currentBrightness = 0; // 0 ~ 255 左右
    private final Handler brightnessHandler = new Handler(Looper.getMainLooper());
    private final Runnable brightnessTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (playerView != null && playerView.getWidth() > 0 && playerView.getHeight() > 0) {
                    int w = playerView.getWidth();
                    int h = playerView.getHeight();
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bmp);
                    playerView.draw(canvas);

                    long sum = 0L;
                    int step = 16; // 隔点采样，避免太耗性能
                    int count = 0;
                    for (int x = 0; x < w; x += step) {
                        for (int y = 0; y < h; y += step) {
                            int color = bmp.getPixel(x, y);
                            int r = (color >> 16) & 0xFF;
                            int g = (color >> 8) & 0xFF;
                            int b = color & 0xFF;
                            int lum = (r + g + b) / 3; // 简单亮度估计
                            sum += lum;
                            count++;
                        }
                    }
                    if (count > 0) {
                        currentBrightness = (int) (sum / count);
                    }
                    bmp.recycle();
                    Log.d(TAG, "currentBrightness = " + currentBrightness);
                }
            } catch (Exception ignore) {
            }

            // 每秒采一次，并实时刷新状态
            updateLiveStatus();
            brightnessHandler.postDelayed(this, 1000);
        }
    };

    // ====== 实时状态刷新控制 ======
    private long lastStatusUpdateTime = 0L; // 上一次更新状态时间戳

    // ====== 持续检测控制（从点击按钮开始直到视频结束） ======
    private final Handler detectionHandler = new Handler(Looper.getMainLooper());
    private boolean isDetecting = false;
    private final Runnable detectionTask = new Runnable() {
        @Override
        public void run() {
            if (!isDetecting) return;

            // 有字幕/文稿时用文本+声音+画面；如果你想只依赖声音+画面，可以把 script 设为 ""
            String script = getDemoScript();
            // String script = ""; // 完全无文本时可以改成这样

            DetectionResult result;
            if (script != null && !script.trim().isEmpty()) {
                result = detectProfanityAndEmotion(script, maxAmplitude, currentAmplitude);
            } else {
                result = detectEmotionFromAudioAndVideoOnly(maxAmplitude, currentAmplitude);
            }

            runOnUiThread(() -> {
                tvSubtitle.setText(result.highlightedSubtitle);
                tvProfanitySummary.setText(
                        "文本通道：" + result.textEmotionLabel + "\n" +
                                "脏话次数：" + result.profanityCount +
                                "（仅在有文本时有效）\n" +
                                "声音响度评估：" + result.loudnessLabel +
                                "（max=" + result.maxAmplitude + ", current=" + result.currentAmplitude + ")\n" +
                                "情绪累计：" + result.overallEmotionLabel
                );
                tvEmotion.setText("综合情绪：" + result.finalEmotionLabel);
            });

            // 每秒检测一次
            detectionHandler.postDelayed(this, 1000);
        }
    };

    // ====== 全局情绪累积（不会被每一帧刷新掉） ======
    // 0 = 平静，1 = 轻微激动，2 = 明显激动，3 = 非常激动（多模态都很强）
    private int maxArousalScoreSoFar = 0;
    private String maxArousalLabelSoFar = "整体偏平静（尚未检测到明显激动片段）";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_check);

        initViews();
        initPlayer();
        initButton();
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        tvStatus = findViewById(R.id.tv_status);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvProfanitySummary = findViewById(R.id.tv_profanity_summary);
        tvEmotion = findViewById(R.id.tv_emotion);
        btnDetect = findViewById(R.id.btn_detect);
    }

    /**
     * 初始化 ExoPlayer，播放本地 res/raw/live_clip.mp4
     */
    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // 播放 res/raw 下的本地视频 live_clip.mp4
        // 确保路径：app/src/main/res/raw/live_clip.mp4
        Uri videoUri = RawResourceDataSource.buildRawResourceUri(R.raw.live_clip);
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    Log.d(TAG, "视频缓冲中...");
                    tvStatus.setText("状态：视频缓冲中...");
                } else if (state == Player.STATE_READY) {
                    Log.d(TAG, "视频开始播放");
                    tvStatus.setText("状态：正在播放（点击按钮后开始持续检测，直到视频结束）");
                    // 视频 ready 后再开始做亮度采样
                    startBrightnessSampling();
                } else if (state == Player.STATE_ENDED) {
                    Log.d(TAG, "播放结束");
                    tvStatus.setText("状态：播放结束，已停止检测");
                    // 播放结束时停止持续检测
                    isDetecting = false;
                    detectionHandler.removeCallbacks(detectionTask);
                }
            }

            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                Log.d(TAG, "onAudioSessionIdChanged: " + audioSessionId);
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    setupVisualizer(audioSessionId);
                }
            }
        });

        player.prepare();
        player.play();
    }

    private void startBrightnessSampling() {
        brightnessHandler.removeCallbacks(brightnessTask);
        brightnessHandler.post(brightnessTask);
    }

    private void stopBrightnessSampling() {
        brightnessHandler.removeCallbacks(brightnessTask);
    }

    /**
     * 使用 Visualizer 采集当前播放器的音频波形，估计“响度”
     */
    private void setupVisualizer(int audioSessionId) {
        releaseVisualizer();

        try {
            visualizer = new Visualizer(audioSessionId);
            int[] range = Visualizer.getCaptureSizeRange();
            visualizer.setCaptureSize(range[1]);

            int rate = Visualizer.getMaxCaptureRate() / 2;
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    int sum = 0;
                    for (byte b : waveform) {
                        sum += Math.abs(b);
                    }
                    int avg = waveform.length == 0 ? 0 : sum / waveform.length;
                    currentAmplitude = avg;
                    if (avg > maxAmplitude) {
                        maxAmplitude = avg;
                    }

                    amplitudeHistory.add(avg);
                    if (amplitudeHistory.size() > HISTORY_MAX_SIZE) {
                        amplitudeHistory.remove(0);
                    }

                    // 声音每次采样也实时刷新状态
                    updateLiveStatus();
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    // 本作业暂不使用频谱
                }
            }, rate, true, false);

            visualizer.setEnabled(true);
            Log.d(TAG, "Visualizer 已启用，开始采集声音幅度");
        } catch (Exception e) {
            Log.e(TAG, "初始化 Visualizer 失败", e);
            Toast.makeText(this, "无法开启声音响度分析：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 实时状态栏刷新（基于当前音量 + 峰值 + 画面亮度）
     */
    private void updateLiveStatus() {
        long now = System.currentTimeMillis();
        // 避免刷新太频繁，这里限制至少 500ms 才更新一次 UI
        if (now - lastStatusUpdateTime < 500) {
            return;
        }
        lastStatusUpdateTime = now;

        runOnUiThread(() -> {
            String liveEmotion = estimateLiveArousalLabel(currentAmplitude, maxAmplitude, currentBrightness);
            String text = "实时状态：音量=" + currentAmplitude
                    + "，峰值=" + maxAmplitude
                    + "，亮度=" + currentBrightness
                    + "，预判：" + liveEmotion;
            tvStatus.setText(text);
        });
    }

    /**
     * 轻量级实时情绪估计，只用于状态栏展示
     */
    private String estimateLiveArousalLabel(int curAmp, int maxAmp, int brightness) {
        int effectiveAmp = Math.max(curAmp, maxAmp);

        // 适当降低阈值，让检测更敏感一些（通常 avg 在 20~60 之间）
        boolean veryLoud = effectiveAmp > 90;
        boolean loud = effectiveAmp > 60;
        boolean veryBright = brightness > 180;

        if (veryLoud || (loud && veryBright)) {
            return "偏激动 / 高亢";
        } else if (loud || veryBright) {
            return "略有波动";
        } else {
            return "整体偏平静";
        }
    }

    /**
     * 点击按钮后，开始“从现在起持续检测直到视频结束”
     */
    private void initButton() {
        btnDetect.setOnClickListener(v -> {
            if (!isDetecting) {
                isDetecting = true;

                // 每次开始新一轮检测时，把累积状态和历史响度重置
                maxArousalScoreSoFar = 0;
                maxArousalLabelSoFar = "整体偏平静（尚未检测到明显激动片段）";
                maxAmplitude = 0;
                amplitudeHistory.clear();

                tvStatus.setText("状态：已开始持续检测（直到视频结束）");
                detectionHandler.removeCallbacks(detectionTask);
                detectionHandler.post(detectionTask);
            }
        });
    }

    /**
     * 示例脚本：中英混合的口播稿（方便测试规则）
     */
    private String getDemoScript() {
        return "今天这款游戏真的让我有点气炸了! Teammates keep feeding, " +
                "I'm so mad right now, this is really trash play, " +
                "我都快崩溃了, 真的受不了这种节奏!";
    }

    // ================= 整体检测逻辑：脏话 + 文本情绪 + 声音响度 + 画面亮度 =================

    private static class DetectionResult {
        String highlightedSubtitle;
        int profanityCount;
        int maxAmplitude;
        int currentAmplitude;

        String textEmotionLabel;   // 纯文本推断情绪
        String loudnessLabel;      // 纯声音响度推断
        String finalEmotionLabel;  // 当前片段的多模态结果
        String overallEmotionLabel; // 截止目前为止的“历史整体情绪”
    }

    /**
     * 多模态情绪识别：文本(支持中英混合) + 声音响度 + 简单画面亮度
     */
    private DetectionResult detectProfanityAndEmotion(String text, int maxAmp, int curAmp) {
        DetectionResult result = new DetectionResult();
        result.highlightedSubtitle = text;
        result.maxAmplitude = maxAmp;
        result.currentAmplitude = curAmp;

        // 1. 文本脏话检测（中英混合关键词）
        String[] profanityWords = new String[]{
                // 中文
                "操", "妈的", "傻逼", "狗屁", "滚", "垃圾", "废物",
                // 英文
                "fuck", "shit", "bitch", "asshole", "idiot", "stupid",
                "trash", "noob", "wtf"
        };
        int profanityCount = 0;
        String lower = text.toLowerCase();
        for (String w : profanityWords) {
            if (lower.contains(w.toLowerCase())) {
                profanityCount++;
            }
        }
        result.profanityCount = profanityCount;

        // 2. 文本情绪检测（中英混合生气词 + 感叹号）
        String[] angryWords = new String[]{
                // 中文
                "气炸", "气死", "受不了", "崩溃", "烦死", "火大", "爆炸", "生气", "怒", "炸毛",
                // 英文
                "angry", "mad", "pissed", "tilted", "so bad", "so stupid",
                "trash team", "i can't stand", "can't stand this", "why always"
        };
        int exclamationCount = countChar(text, '!');
        boolean hasAngryWord = containsAny(lower, angryWords);

        int textScore; // 0 = 平静，1 = 激动
        String textEmotion;
        if (profanityCount >= 1 || exclamationCount >= 2 || hasAngryWord) {
            textEmotion = "愤怒 / 激动（基于文本，中英混合）";
            textScore = 1;
        } else {
            textEmotion = "平静 / 正常讨论（基于文本，中英混合）";
            textScore = 0;
        }
        result.textEmotionLabel = textEmotion;

        // 3. 声音响度检测（基于 Visualizer 波形幅度 + 历史均值），降低阈值让检测更敏感
        int historySum = 0;
        for (int v : amplitudeHistory) {
            historySum += v;
        }
        int historyAvg = amplitudeHistory.isEmpty() ? 0 : historySum / amplitudeHistory.size();
        int effectiveAmp = Math.max(maxAmp, Math.max(historyAvg, curAmp));

        String loudness;
        int soundScore; // 0=平静，1=中等激动，2=高激动

        if (effectiveAmp > 90) {
            loudness = "声音非常大，极可能在喊叫或激动说话";
            soundScore = 2;
        } else if (effectiveAmp > 60) {
            loudness = "音量偏大，怀疑存在较强情绪波动";
            soundScore = 1;
        } else if (effectiveAmp > 30) {
            loudness = "音量中等，可能略有起伏";
            soundScore = 0;
        } else {
            loudness = "音量较小，更偏向平静或低落";
            soundScore = 0;
        }
        result.loudnessLabel = loudness;

        // 4. 简单画面亮度通道
        String visionLabel;
        int visionScore; // 0=信息不明显，1=可能较激烈
        if (currentBrightness > 180) {
            visionLabel = "画面整体偏亮，可能是团战/特效场景";
            visionScore = 1;
        } else if (currentBrightness < 60) {
            visionLabel = "画面偏暗，可能处于读条/切换界面";
            visionScore = 0;
        } else {
            visionLabel = "画面亮度中等，信息不明显";
            visionScore = 0;
        }
        Log.d(TAG, "vision brightness = " + currentBrightness + ", label=" + visionLabel);

        // 5. 多模态融合：文本 + 声音 + 画面
        int totalArousal = soundScore + visionScore + textScore;

        String currentLabel;
        if (totalArousal >= 3) {
            currentLabel = "高置信度高唤醒情绪（文本 + 声音 + 画面均偏激动）";
        } else if (totalArousal == 2) {
            currentLabel = "较强情绪波动（多路模态支持）";
        } else if (totalArousal == 1) {
            currentLabel = "可能略为激动（部分模态提示）";
        } else {
            currentLabel = "整体偏平静（当前片段各模态都较温和）";
        }

        // ========= 关键：把“历史最高激动程度”累积下来 =========
        int currentScoreLevel;
        if (totalArousal >= 3) {
            currentScoreLevel = 3;
        } else if (totalArousal == 2) {
            currentScoreLevel = 2;
        } else if (totalArousal == 1) {
            currentScoreLevel = 1;
        } else {
            currentScoreLevel = 0;
        }

        if (currentScoreLevel > maxArousalScoreSoFar) {
            maxArousalScoreSoFar = currentScoreLevel;
            maxArousalLabelSoFar = currentLabel;
        }

        result.finalEmotionLabel = "当前片段：" + currentLabel;
        result.overallEmotionLabel = maxArousalLabelSoFar;

        Log.d(TAG, "detectProfanityAndEmotion >> textScore=" + textScore
                + ", soundScore=" + soundScore
                + ", visionScore=" + visionScore
                + ", historyAvg=" + historyAvg
                + ", maxAmp=" + maxAmp
                + ", curAmp=" + curAmp
                + ", totalArousal=" + totalArousal
                + ", maxArousalScoreSoFar=" + maxArousalScoreSoFar);

        return result;
    }

    /**
     * 没有字幕/台词时：只用 声音响度 + 画面亮度 估计情绪
     */
    private DetectionResult detectEmotionFromAudioAndVideoOnly(int maxAmp, int curAmp) {
        DetectionResult result = new DetectionResult();
        result.maxAmplitude = maxAmp;
        result.currentAmplitude = curAmp;

        result.highlightedSubtitle = "（当前检测未使用文本，仅基于声音响度和画面亮度进行情绪推断）";
        result.profanityCount = 0;
        result.textEmotionLabel = "无文本信号";

        int historySum = 0;
        for (int v : amplitudeHistory) {
            historySum += v;
        }
        int historyAvg = amplitudeHistory.isEmpty() ? 0 : historySum / amplitudeHistory.size();
        int effectiveAmp = Math.max(maxAmp, Math.max(historyAvg, curAmp));

        String loudness;
        int soundScore;
        if (effectiveAmp > 90) {
            loudness = "声音非常大，极可能在喊叫或激动说话";
            soundScore = 2;
        } else if (effectiveAmp > 60) {
            loudness = "音量偏大，怀疑存在较强情绪波动";
            soundScore = 1;
        } else if (effectiveAmp > 30) {
            loudness = "音量中等，可能略有起伏";
            soundScore = 0;
        } else {
            loudness = "音量较小，更偏向平静或低落";
            soundScore = 0;
        }
        result.loudnessLabel = loudness;

        String visionLabel;
        int visionScore;
        if (currentBrightness > 180) {
            visionLabel = "画面整体偏亮，可能是团战/特效场景";
            visionScore = 1;
        } else if (currentBrightness < 60) {
            visionLabel = "画面偏暗，可能处于读条/切换界面";
            visionScore = 0;
        } else {
            visionLabel = "画面亮度中等，信息不明显";
            visionScore = 0;
        }
        Log.d(TAG, "[audio-only mode] vision brightness = " + currentBrightness + ", label=" + visionLabel);

        int totalArousal = soundScore + visionScore; // 没有文本，仅两路

        String currentLabel;
        if (totalArousal >= 2) {
            currentLabel = "高置信度高唤醒情绪（声音或画面明显激动）";
        } else if (totalArousal == 1) {
            currentLabel = "可能略为激动（单一模态提示）";
        } else {
            currentLabel = "整体偏平静（当前片段声音与画面都较温和）";
        }

        int currentScoreLevel;
        if (totalArousal >= 2) {
            currentScoreLevel = 2;
        } else if (totalArousal == 1) {
            currentScoreLevel = 1;
        } else {
            currentScoreLevel = 0;
        }

        if (currentScoreLevel > maxArousalScoreSoFar) {
            maxArousalScoreSoFar = currentScoreLevel;
            maxArousalLabelSoFar = currentLabel;
        }

        result.finalEmotionLabel = "当前片段：" + currentLabel;
        result.overallEmotionLabel = maxArousalLabelSoFar;

        Log.d(TAG, "detectEmotionFromAudioAndVideoOnly >> soundScore=" + soundScore
                + ", visionScore=" + visionScore
                + ", historyAvg=" + historyAvg
                + ", maxAmp=" + maxAmp
                + ", curAmp=" + curAmp
                + ", totalArousal=" + totalArousal
                + ", maxArousalScoreSoFar=" + maxArousalScoreSoFar);

        return result;
    }

    private int countChar(String text, char c) {
        int cnt = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) cnt++;
        }
        return cnt;
    }

    private boolean containsAny(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ================= 生命周期：释放资源 =================

    private void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
            } catch (Throwable ignore) {
            }
            visualizer.release();
            visualizer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
        stopBrightnessSampling();
        releaseVisualizer();

        // 页面不可见时停止持续检测
        isDetecting = false;
        detectionHandler.removeCallbacks(detectionTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        stopBrightnessSampling();
        releaseVisualizer();

        isDetecting = false;
        detectionHandler.removeCallbacks(detectionTask);
    }
}