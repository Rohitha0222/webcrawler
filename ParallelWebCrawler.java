package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parallel implementation of {@link WebCrawler}.
 */
final class ParallelWebCrawler implements WebCrawler {

    private final Clock clock;
    private final PageParserFactory parserFactory;
    private final Duration timeout;
    private final int popularWordCount;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final ForkJoinPool pool;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            PageParserFactory parserFactory,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls,
            @TargetParallelism int threadCount) {

        this.clock = clock;
        this.parserFactory = parserFactory;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.pool = new ForkJoinPool(threadCount);
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);

        ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

        pool.invoke(new CrawlTask(
                startingUrls,
                maxDepth,
                deadline,
                visitedUrls,
                wordCounts
        ));

        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    /** Root task */
    private final class CrawlTask extends RecursiveAction {

        private final List<String> urls;
        private final int depth;
        private final Instant deadline;
        private final Set<String> visitedUrls;
        private final ConcurrentMap<String, Integer> wordCounts;

        CrawlTask(
                List<String> urls,
                int depth,
                Instant deadline,
                Set<String> visitedUrls,
                ConcurrentMap<String, Integer> wordCounts) {

            this.urls = urls;
            this.depth = depth;
            this.deadline = deadline;
            this.visitedUrls = visitedUrls;
            this.wordCounts = wordCounts;
        }

        @Override
        protected void compute() {
            invokeAll(
                    urls.stream()
                            .map(url -> new SingleUrlTask(
                                    url,
                                    depth,
                                    deadline,
                                    visitedUrls,
                                    wordCounts))
                            .collect(Collectors.toList())
            );
        }
    }

    /** Task for a single URL */
    private final class SingleUrlTask extends RecursiveAction {

        private final String url;
        private final int depth;
        private final Instant deadline;
        private final Set<String> visitedUrls;
        private final ConcurrentMap<String, Integer> wordCounts;

        SingleUrlTask(
                String url,
                int depth,
                Instant deadline,
                Set<String> visitedUrls,
                ConcurrentMap<String, Integer> wordCounts) {

            this.url = url;
            this.depth = depth;
            this.deadline = deadline;
            this.visitedUrls = visitedUrls;
            this.wordCounts = wordCounts;
        }

        @Override
        protected void compute() {
            if (depth == 0 || clock.instant().isAfter(deadline)) {
                return;
            }

            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }

            if (!visitedUrls.add(url)) {
                return;
            }

            PageParser.Result result = parserFactory.get(url).parse();

            result.getWordCounts()
                    .forEach((word, count) ->
                            wordCounts.merge(word, count, Integer::sum));

            invokeAll(
                    result.getLinks().stream()
                            .map(link -> new SingleUrlTask(
                                    link,
                                    depth - 1,
                                    deadline,
                                    visitedUrls,
                                    wordCounts))
                            .collect(Collectors.toList())
            );
        }
    }

    /** 🔥 THIS FIXES YOUR RUNTIME ERROR */
    @Override
    public int getMaxParallelism() {
        return Integer.MAX_VALUE;
    }
}

