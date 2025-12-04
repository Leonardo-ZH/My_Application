package com.leonardo.myapplication.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfanityDetector {

    private final List<String> badWords;

    public ProfanityDetector() {
        // 你可以把这里换成中文/英文的脏话集合
        badWords = Arrays.asList("fuck", "shit", "damn");
    }

    public Result analyze(String transcript) {
        int count = 0;
        List<String> hitWords = new ArrayList<>();

        if (transcript == null) return new Result(0, hitWords);

        String lower = transcript.toLowerCase();
        for (String word : badWords) {
            if (lower.contains(word)) {
                count++;
                hitWords.add(word);
            }
        }
        return new Result(count, hitWords);
    }

    public static class Result {
        public final int count;
        public final List<String> words;
        public Result(int c, List<String> w) {
            this.count = c;
            this.words = w;
        }
    }
}