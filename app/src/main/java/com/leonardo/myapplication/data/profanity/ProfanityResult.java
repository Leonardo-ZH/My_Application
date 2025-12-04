// app/src/main/java/com/leonardo/myapplication/data/profanity/ProfanityResult.java
package com.leonardo.myapplication.data.profanity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProfanityResult {

    // 总命中次数（一个词出现多次要累加）
    private int totalCount;

    // 每个词对应出现次数
    private final Map<String, Integer> wordCountMap = new HashMap<>();

    // 简单情绪标签，例如：平静 / 激动 / 愤怒
    private String emotionTag = "平静";

    public void addWord(String word) {
        if (word == null || word.isEmpty()) return;
        totalCount++;
        int old = wordCountMap.containsKey(word) ? wordCountMap.get(word) : 0;
        wordCountMap.put(word, old + 1);
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Map<String, Integer> getWordCountMap() {
        return Collections.unmodifiableMap(wordCountMap);
    }

    public Set<String> getAllWords() {
        return wordCountMap.keySet();
    }

    public String getEmotionTag() {
        return emotionTag;
    }

    public void setEmotionTag(String emotionTag) {
        this.emotionTag = emotionTag;
    }
}