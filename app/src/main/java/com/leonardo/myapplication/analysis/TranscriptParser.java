package com.leonardo.myapplication.analysis;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TranscriptParser {

    public String readTranscriptFromAssets(Context context, String fileName) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 简单拼接所有文本
                builder.append(line).append(" ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}