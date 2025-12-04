package com.leonardo.myapplication.ui.feed;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.leonardo.myapplication.R;
import com.leonardo.myapplication.data.model.VideoItem;
import com.leonardo.myapplication.ui.player.PlayerActivity;

/**
 * 两列卡片的 ViewHolder
 */
public class VideoViewHolder extends RecyclerView.ViewHolder {

    ImageView ivThumb;
    ImageView ivAvatar;
    TextView tvTitle;
    TextView tvNick;
    TextView tvTag;
    TextView tvViewerCount;

    public VideoViewHolder(@NonNull View itemView) {
        super(itemView);
        ivThumb = itemView.findViewById(R.id.iv_thumb);
        ivAvatar = itemView.findViewById(R.id.iv_avatar);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvNick = itemView.findViewById(R.id.tv_nick);
        tvTag = itemView.findViewById(R.id.tv_tag);
        tvViewerCount = itemView.findViewById(R.id.tv_viewer_count);
    }

    public void bind(VideoItem item) {
        if (item == null) return;

        // 标题
        tvTitle.setText(item.getTitle());

        // 昵称（频道名称）
        tvNick.setText(item.getAuthor());

        // LIVE 标签
        if (item.isLive()) {
            tvTag.setText("LIVE");
            tvTag.setVisibility(View.VISIBLE);
        } else {
            tvTag.setText("");
            tvTag.setVisibility(View.INVISIBLE);
        }

        // 观看人数，如 "24,969,123"
        tvViewerCount.setText(item.getViews());

        // 封面
        Glide.with(itemView.getContext())
                .load(item.getThumbnailUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(ivThumb);

        // 头像：如果暂时没有单独头像，就先用封面代替
        Glide.with(itemView.getContext())
                .load(item.getThumbnailUrl())
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(ivAvatar);

        // ========= 关键：点击卡片，跳转到 PlayerActivity =========
        itemView.setOnClickListener(v -> {
            if (item.getVideoUrl() == null || item.getVideoUrl().trim().isEmpty()) {
                Toast.makeText(
                        itemView.getContext(),
                        "当前视频地址为空，无法播放",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            android.content.Context context = itemView.getContext();
            android.content.Intent intent =
                    new android.content.Intent(context, com.leonardo.myapplication.ui.player.PlayerActivity.class);

            // 只要这两个就是你现在 PlayerActivity 用到的
            intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, item.getVideoUrl());
            intent.putExtra(PlayerActivity.EXTRA_TITLE, item.getTitle());

            // 以后如果你在 PlayerActivity 里加字段，也可以继续 putExtra 其它信息：
            // intent.putExtra("extra_author", item.getAuthor());
            // intent.putExtra("extra_views", item.getViews());
            // intent.putExtra("extra_desc", item.getDescription());
            // intent.putExtra("extra_subscriber", item.getSubscriber());
            // intent.putExtra("extra_is_live", item.isLive());

            context.startActivity(intent);
        });
    }
}