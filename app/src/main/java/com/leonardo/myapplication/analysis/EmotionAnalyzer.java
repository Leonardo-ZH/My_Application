package com.leonardo.myapplication.analysis;

public class EmotionAnalyzer {

    public enum Emotion {
        CALM, EXCITED, ANGRY
    }

    public Emotion inferEmotion(String transcript, int profanityCount) {
        if (profanityCount > 0) {
            return Emotion.ANGRY;
        }
        if (transcript != null && transcript.contains("!")) {
            return Emotion.EXCITED;
        }
        return Emotion.CALM;
    }
}