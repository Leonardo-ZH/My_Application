package com.leonardo.myapplication.data.remote;

import com.leonardo.myapplication.data.model.VideoItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface VideoApiService {

    @GET("poudyalanil/ca84582cbeb4fc123a13290a586da925/raw/14a27bd0bcd0cd323b35ad79cf3b493dddf6216b/videos.json")
    Call<List<VideoItem>> getVideos();
}