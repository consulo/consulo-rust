/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

/**
 * Automatically inserts/deletes matching '#' characters for raw string literals.
 */
public final class RustRawLiteralHashesBalancer {

    private RustRawLiteralHashesBalancer() {
    }

    /** @deprecated Use {@link org.rust.ide.typing.RsRawLiteralHashesInserter} directly. */
    @Deprecated
    public static class RsRawLiteralHashesInserter extends org.rust.ide.typing.RsRawLiteralHashesInserter {
    }

    /** @deprecated Use {@link org.rust.ide.typing.RsRawLiteralHashesDeleter} directly. */
    @Deprecated
    public static class RsRawLiteralHashesDeleter extends org.rust.ide.typing.RsRawLiteralHashesDeleter {
    }
}
