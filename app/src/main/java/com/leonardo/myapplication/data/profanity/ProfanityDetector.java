// app/src/main/java/com/leonardo/myapplication/data/profanity/ProfanityDetector.java
package com.leonardo.myapplication.data.profanity;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ProfanityDetector {

    // 示例敏感词集合（你可以替换为自己的词表）
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
            "脏话1", "脏话2", "脏话3",   // 占位示例：请用你自己的词
            "xxx", "yyy"              // 英文占位
    ));

    /**
     * 对一整段 transcript 做简单检测
     */
    public static ProfanityResult detect(String transcript) {
        ProfanityResult result = new ProfanityResult();

        if (TextUtils.isEmpty(transcript)) {
            result.setEmotionTag("平静");
            return result;
        }

        // 统一小写，方便英文检测
        String lower = transcript.toLowerCase(Locale.ROOT);

        // 这里为了简单，把中文/英文都按空格和标点做粗略分词
        // 实际可以替换为更精细的中文分词、CLAM 的 token 列表等
        String normalized = lower
                .replaceAll("[\\p{Punct}，。？！；：（）【】、\"“”]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            result.setEmotionTag("平静");
            return result;
        }

        String[] tokens = normalized.split(" ");
        int strongCount = 0;

        for (String token : tokens) {
            if (BAD_WORDS.contains(token)) {
                result.addWord(token);
                strongCount++;
            }
        }

        // 简单情绪规则：
        // 0 次：平静；1~3 次：激动；>3 次：愤怒
        if (result.getTotalCount() == 0) {
            result.setEmotionTag("平静");
        } else if (strongCount <= 3) {
            result.setEmotionTag("激动");
        } else {
            result.setEmotionTag("愤怒");
        }

        return result;
    }
}