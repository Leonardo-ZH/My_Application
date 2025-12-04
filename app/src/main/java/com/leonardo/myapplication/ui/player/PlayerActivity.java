package com.leonardo.myapplication.ui.player;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.ui.PlayerView;
import com.leonardo.myapplication.R;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    public static final String EXTRA_TITLE = "extra_title";

    private static final String TAG = "PlayerActivity";

    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.player_view);
        TextView tvTitle = findViewById(R.id.tv_player_title);

        String url = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title != null) {
            tvTitle.setText(title);
        }

        if (url == null || url.trim().isEmpty()) {
            // 如果没传 url，就用一个默认的，至少看到效果
            Log.e(TAG, "视频 URL 为空，使用默认测试视频");
            url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        }

//        if (url != null && url.startsWith("http://")) {
//            url = url.replaceFirst("http://", "https://");
//        }

        Log.d(TAG, "准备播放 url = " + url);

        try {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) {
                        Log.d(TAG, "缓冲中...");
                    } else if (state == Player.STATE_READY) {
                        Log.d(TAG, "开始播放");
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "播放出错", error);
                    Toast.makeText(PlayerActivity.this,
                            "播放出错: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

            Uri uri = Uri.parse(url);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Log.e(TAG, "初始化播放器失败", e);
            Toast.makeText(this, "播放出错：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}