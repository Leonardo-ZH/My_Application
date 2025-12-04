// app/src/main/java/com/leonardo/myapplication/data/profanity/ProfanityHighlighter.java
package com.leonardo.myapplication.data.profanity;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;

import java.util.Set;

public class ProfanityHighlighter {

    /**
     * 将 transcript 中的敏感词高亮显示
     */
    public static Spannable highlight(@NonNull String transcript,
                                      @NonNull Set<String> badWords) {
        SpannableString span = new SpannableString(transcript);
        if (badWords.isEmpty()) {
            return span;
        }

        // 逐个词在原文中查找并高亮（简单实现，适合 demo）
        for (String word : badWords) {
            if (word == null || word.isEmpty()) continue;

            int start = 0;
            while (true) {
                start = transcript.indexOf(word, start);
                if (start == -1) break;
                int end = start + word.length();

                // 标红并加粗
                span.setSpan(
                        new ForegroundColorSpan(0xFFFF0000), // red
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                span.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                start = end; // 继续向后查找
            }
        }
        return span;
    }
}