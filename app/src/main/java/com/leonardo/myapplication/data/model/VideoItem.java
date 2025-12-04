package com.leonardo.myapplication.data.model;

import com.google.gson.annotations.SerializedName;

public class VideoItem {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("duration")
    private String duration;

    @SerializedName("uploadTime")
    private String uploadTime;

    @SerializedName("views")
    private String views;

    @SerializedName("author")
    private String author;

    @SerializedName("videoUrl")
    private String videoUrl;

    @SerializedName("description")
    private String description;

    @SerializedName("subscriber")
    private String subscriber;

    @SerializedName("isLive")
    private boolean isLive;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getDuration() {
        return duration;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public String getViews() {
        return views;
    }

    public String getAuthor() {
        return author;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getSubscriber() {
        return subscriber;
    }

    public boolean isLive() {
        return isLive;
    }
}