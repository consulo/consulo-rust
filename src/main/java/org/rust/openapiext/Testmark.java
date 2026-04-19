/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.function.Supplier;

import static junit.framework.TestCase.fail;

/**
 * Testmarks allow easy navigation between code and tests.
 * That is, to find a relevant test for some piece of logic,
 * and to find the precise condition which is supposed to be
 * tested by a particular test. In a sense, testmarks are
 * a hand-made artisanal code-coverage tool.
 *
 * <p>To use a testmark, first define a static {@link Testmark} subclass:
 * <pre>
 * public static final class EmptyFrobnicator extends Testmark {}
 * </pre>
 *
 * <p>Then, use {@link #hit()} in the "production" code:
 * <pre>
 * if (myFrobnicator.isEmpty()) {
 *     EmptyFrobnicator.INSTANCE.hit();
 *     handleEmptyFrobnicator();
 * }
 * </pre>
 */
public class Testmark implements TestmarkPred {

    private volatile TestmarkState state = TestmarkState.NEW;
    private volatile Throwable hitAt = null;

    @NotNull
    public String getName() {
        String canonical = getClass().getCanonicalName();
        return canonical != null ? canonical : getClass().getName();
    }

    public void hit() {
        if (state == TestmarkState.NOT_HIT) {
            state = TestmarkState.HIT;
            hitAt = new Throwable();
        }
    }

    @TestOnly
    @Override
    public <T> T checkHit(@NotNull Supplier<T> f) {
        return checkHit(TestmarkState.HIT, f);
    }

    @TestOnly
    @Override
    public <T> T checkNotHit(@NotNull Supplier<T> f) {
        return checkHit(TestmarkState.NOT_HIT, f);
    }

    @TestOnly
    private <T> T checkHit(@NotNull TestmarkState expected, @NotNull Supplier<T> f) {
        if (state != TestmarkState.NEW) {
            throw new IllegalStateException("Testmark state should be NEW but was " + state);
        }
        try {
            state = TestmarkState.NOT_HIT;
            T result = f.get();
            if (state != expected) {
                if (expected == TestmarkState.HIT) {
                    fail("Testmark `" + getName() + "` not hit");
                } else {
                    String attachment = ExceptionUtil.getThrowableText(hitAt);
                    fail("Testmark `" + getName() + "` hit at " + attachment + ";");
                }
            }
            return result;
        } finally {
            state = TestmarkState.NEW;
        }
    }

    /**
     * Returns true if the given boolean is false, and hits the testmark.
     */
    public boolean hitOnFalse(boolean b) {
        if (!b) hit();
        return b;
    }

    /**
     * Returns true if the given boolean is true, and hits the testmark.
     */
    public boolean hitOnTrue(boolean b) {
        if (b) hit();
        return b;
    }

    /**
     * Returns a negated {@link TestmarkPred} where checkHit and checkNotHit are swapped.
     */
    @TestOnly
    @NotNull
    public TestmarkPred not() {
        Testmark self = this;
        return new TestmarkPred() {
            @Override
            public <T> T checkHit(Supplier<T> f) {
                return self.checkNotHit(f);
            }

            @Override
            public <T> T checkNotHit(Supplier<T> f) {
                return self.checkHit(f);
            }
        };
    }

    private enum TestmarkState {
        NEW, NOT_HIT, HIT
    }
}
