/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class Profiling {
    private Profiling() {
    }

    public interface RsWatch {
        @Nonnull String getName();
        @Nonnull AtomicLong getTotalNs();
    }

    public static class RsStopWatch implements RsWatch {
        private final String myName;
        private final AtomicLong myTotalNs = new AtomicLong(0);

        public RsStopWatch(@Nonnull String name) {
            this.myName = name;
            WATCHES.register(this);
        }

        @Override @Nonnull public String getName() { return myName; }
        @Override @Nonnull public AtomicLong getTotalNs() { return myTotalNs; }

        public <T> T measure(@Nonnull Supplier<T> block) {
            long start = System.nanoTime();
            T result = block.get();
            myTotalNs.addAndGet(System.nanoTime() - start);
            return result;
        }
    }

    public static class RsReentrantStopWatch implements RsWatch {
        private final String myName;
        private final AtomicLong myTotalNs = new AtomicLong(0);
        private final AtomicBoolean myStarted = new AtomicBoolean(false);
        private final ThreadLocal<Integer> myNesting = ThreadLocal.withInitial(() -> 0);

        public RsReentrantStopWatch(@Nonnull String name) {
            this.myName = name;
            WATCHES.register(this);
        }

        @Override @Nonnull public String getName() { return myName; }
        @Override @Nonnull public AtomicLong getTotalNs() { return myTotalNs; }

        public void start() {
            myStarted.set(true);
        }

        public <T> T measure(@Nonnull Supplier<T> block) {
            int v = myNesting.get();
            myNesting.set(v + 1);
            boolean isFirst = v == 0;

            T result;
            if (isFirst && myStarted.get()) {
                long start = System.nanoTime();
                result = block.get();
                myTotalNs.addAndGet(System.nanoTime() - start);
            } else {
                result = block.get();
            }

            myNesting.set(myNesting.get() - 1);
            return result;
        }
    }

    private static final class WATCHES {
        private static final Set<RsWatch> registered = ConcurrentHashMap.newKeySet();

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nWatches:");
                List<RsWatch> sorted = new ArrayList<>(registered);
                sorted.sort(Comparator.comparingLong(w -> -w.getTotalNs().get()));
                for (RsWatch watch : sorted) {
                    long ms = watch.getTotalNs().get() / 1_000_000;
                    System.out.printf("  %-4d ms %s%n", ms, watch.getName());
                }
            }));
        }

        static void register(@Nonnull RsWatch watch) {
            registered.add(watch);
        }
    }
}
