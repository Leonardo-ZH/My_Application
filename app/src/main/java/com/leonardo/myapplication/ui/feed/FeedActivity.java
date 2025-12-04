package com.leonardo.myapplication.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leonardo.myapplication.R;
import com.leonardo.myapplication.data.model.VideoItem;
import com.leonardo.myapplication.data.repository.VideoRepository;
import com.leonardo.myapplication.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业一：顶部标题 + 两列瀑布流卡片
 */
public class FeedActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageView ivBack;
    private TextView tvTitleBar;

    private VideoAdapter adapter;
    private VideoRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        initViews();

        repository = new VideoRepository();
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        ivBack = findViewById(R.id.iv_back);
        tvTitleBar = findViewById(R.id.tv_title_bar);

        if (tvTitleBar != null) {
            tvTitleBar.setText("LIVE · MLBB: Zetian");
        }

        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        adapter = new VideoAdapter(new ArrayList<VideoItem>(), this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        repository.fetchVideoList(new VideoRepository.VideoCallback() {
            @Override
            public void onSuccess(List<VideoItem> list) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                adapter.updateData(list);
            }

            @Override
            public void onError(Throwable t) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(FeedActivity.this,
                        "加载失败: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVideoClick(VideoItem item) {
        if (item == null) return;

        String url = item.getVideoUrl();  // 注意使用 getVideoUrl()
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "视频地址缺失，无法播放", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, url);
        intent.putExtra(PlayerActivity.EXTRA_TITLE, item.getTitle());
        startActivity(intent);
    }
}