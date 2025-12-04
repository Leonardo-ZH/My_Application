package com.leonardo.myapplication.data.repository;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.leonardo.myapplication.data.model.VideoItem;
import com.leonardo.myapplication.data.remote.RetrofitClient;
import com.leonardo.myapplication.data.remote.VideoApiService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoRepository {

    private static final String TAG = "VideoRepository";
    private final VideoApiService apiService;

    public interface VideoCallback {
        void onSuccess(List<VideoItem> list);
        void onError(Throwable t);
    }

    public VideoRepository() {
        apiService = RetrofitClient.getInstance().create(VideoApiService.class);
    }

    public void fetchVideoList(final VideoCallback callback) {
        Log.d(TAG, "fetchVideoList: start request");
        apiService.getVideos().enqueue(new Callback<List<VideoItem>>() {
            @Override
            public void onResponse(Call<List<VideoItem>> call, Response<List<VideoItem>> response) {
                Log.d(TAG, "onResponse: code = " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "onResponse: list size = " + response.body().size());
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "onResponse: not successful, use mock data");
                    // 网络返回失败时，先回调错误，再给一份本地假数据，保证列表不会空白
                    callback.onError(new Exception("Response not successful"));
                    callback.onSuccess(createMockData());
                }
            }

            @Override
            public void onFailure(Call<List<VideoItem>> call, Throwable t) {
                Log.e(TAG, "fetchVideoList onFailure", t);
                // 网络请求直接失败（比如没梯子、被墙），也用本地数据兜底
                callback.onError(t);
                callback.onSuccess(createMockData());
            }
        });
    }

    // 本地兜底数据，保证没有网络时也能看到列表
    private List<VideoItem> createMockData() {
        List<VideoItem> list = new ArrayList<>();

        // 只 mock 两条，方便你看效果；以后你可以按 JSON 真数据来写更多
        VideoItem item1 = new VideoItemBuilder()
                .setId("1")
                .setTitle("Big Buck Bunny")
                .setAuthor("Vlc Media Player")
                .setThumbnailUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Big_Buck_Bunny_thumbnail_vlc.png/1200px-Big_Buck_Bunny_thumbnail_vlc.png")
                .setViews("24,969,123")
                .setVideoUrl("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                .setIsLive(true)
                .build();

        VideoItem item2 = new VideoItemBuilder()
                .setId("2")
                .setTitle("The first Blender Open Movie from 2006")
                .setAuthor("Blender Inc.")
                .setThumbnailUrl("https://i.ytimg.com/vi_webp/gWw23EYM9VM/maxresdefault.webp")
                .setViews("24,969,123")
                .setVideoUrl("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                .setIsLive(true)
                .build();

        list.add(item1);
        list.add(item2);
        return list;
    }

    /**
     * 简单的 Builder，专门用来创建 mock 的 VideoItem，
     * 不影响你用 Gson 正常解析网络数据。
     */
    private static class VideoItemBuilder {
        private final VideoItem item = new VideoItem();

        VideoItemBuilder setId(String id) {
            setField("id", id);
            return this;
        }

        VideoItemBuilder setTitle(String title) {
            setField("title", title);
            return this;
        }

        VideoItemBuilder setAuthor(String author) {
            setField("author", author);
            return this;
        }

        VideoItemBuilder setThumbnailUrl(String url) {
            setField("thumbnailUrl", url);
            return this;
        }

        VideoItemBuilder setViews(String views) {
            setField("views", views);
            return this;
        }

        VideoItemBuilder setVideoUrl(String url) {
            setField("videoUrl", url);
            return this;
        }

        VideoItemBuilder setIsLive(boolean isLive) {
            setField("isLive", isLive);
            return this;
        }

        VideoItem build() {
            return item;
        }

        private void setField(String fieldName, Object value) {
            try {
                java.lang.reflect.Field f = VideoItem.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(item, value);
            } catch (Exception e) {
                Log.e(TAG, "setField error: " + fieldName, e);
            }
        }
    }
}