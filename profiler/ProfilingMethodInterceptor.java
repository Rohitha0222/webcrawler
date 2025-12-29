package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;

    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = Objects.requireNonNull(delegate);
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // Handle Object methods correctly
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(delegate, args);
        }

        boolean profiled = method.isAnnotationPresent(Profiled.class);
        Instant start = null;

        if (profiled) {
            start = clock.instant();
        }

        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            // IMPORTANT: rethrow the original exception
            throw e.getCause();
        } finally {
            if (profiled) {
                Duration duration = Duration.between(start, clock.instant());
                state.record(delegate.getClass(), method, duration);
            }
        }
    }
}
