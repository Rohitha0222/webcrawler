package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass);
        Objects.requireNonNull(delegate);

        boolean hasProfiledMethod =
                Arrays.stream(klass.getMethods())
                        .anyMatch(method -> method.isAnnotationPresent(Profiled.class));

        if (!hasProfiledMethod) {
            throw new IllegalArgumentException(
                    "No methods annotated with @Profiled in " + klass.getName());
        }

        Object proxy =
                Proxy.newProxyInstance(
                        klass.getClassLoader(),
                        new Class<?>[]{klass},
                        new ProfilingMethodInterceptor(clock, delegate, state)
                );

        return klass.cast(proxy);
    }

    @Override
    public void writeData(Path path) {
        try (Writer writer =
                     Files.newBufferedWriter(
                             path,
                             java.nio.file.StandardOpenOption.CREATE,
                             java.nio.file.StandardOpenOption.APPEND)) {
            writeData(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
