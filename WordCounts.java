package com.udacity.webcrawler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 */
final class WordCounts {

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided {@link WordCountComparator}, and includes only the top
     * {@param popularWordCount} words and counts.
     */
    static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

        return wordCounts.entrySet()
                .stream()
                .sorted(new WordCountComparator())
                .limit(popularWordCount)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * A {@link Comparator} that sorts word count pairs correctly:
     *
     * <ol>
     *   <li>Higher frequency first</li>
     *   <li>Longer word first if frequencies are equal</li>
     *   <li>Alphabetical order if both frequency and length are equal</li>
     * </ol>
     */
    private static final class WordCountComparator
            implements Comparator<Map.Entry<String, Integer>> {

        @Override
        public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
            if (!a.getValue().equals(b.getValue())) {
                return b.getValue() - a.getValue();
            }
            if (a.getKey().length() != b.getKey().length()) {
                return b.getKey().length() - a.getKey().length();
            }
            return a.getKey().compareTo(b.getKey());
        }
    }

    private WordCounts() {
        // This class cannot be instantiated
    }
}
