package com.udacity.webcrawler.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of a web crawl.
 */
public final class CrawlResult {

    private final Map<String, Integer> wordCounts;
    private final int urlsVisited;

    private CrawlResult(Map<String, Integer> wordCounts, int urlsVisited) {
        this.wordCounts = Collections.unmodifiableMap(new LinkedHashMap<>(wordCounts));
        this.urlsVisited = urlsVisited;
    }

    public Map<String, Integer> getWordCounts() {
        return wordCounts;
    }

    public int getUrlsVisited() {
        return urlsVisited;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CrawlResult.
     */
    public static class Builder {
        private final Map<String, Integer> wordCounts = new LinkedHashMap<>();
        private int urlsVisited;

        public Builder addWordCount(String word, int count) {
            wordCounts.put(word, count);
            return this;
        }

        public Builder setWordCounts(Map<String, Integer> wordCounts) {
            this.wordCounts.clear();
            this.wordCounts.putAll(wordCounts);
            return this;
        }

        public Builder setUrlsVisited(int urlsVisited) {
            this.urlsVisited = urlsVisited;
            return this;
        }

        public CrawlResult build() {
            return new CrawlResult(wordCounts, urlsVisited);
        }
    }
}
