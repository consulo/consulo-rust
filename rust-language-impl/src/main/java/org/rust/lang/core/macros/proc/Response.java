/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonDeserializer;
import org.rust.stdext.RsResult;
import org.rust.util.RsJacksonDeserializer;

import java.io.IOException;
import java.util.Objects;

/**
 * Represents a response from the proc macro expander process.
 */
public abstract class Response {

    private Response() {}

    public static final class ExpandMacro extends Response {
        @Nonnull
        private final RsResult<FlatTree, PanicMessage> myExpansion;

        public ExpandMacro(@Nonnull RsResult<FlatTree, PanicMessage> expansion) {
            myExpansion = expansion;
        }

        @Nonnull
        public RsResult<FlatTree, PanicMessage> getExpansion() {
            return myExpansion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpandMacro)) return false;
            return Objects.equals(myExpansion, ((ExpandMacro) o).myExpansion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myExpansion);
        }
    }

    public static final class ApiVersionCheck extends Response {
        private final long myVersion;

        public ApiVersionCheck(long version) {
            myVersion = version;
        }

        public long getVersion() {
            return myVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ApiVersionCheck)) return false;
            return myVersion == ((ApiVersionCheck) o).myVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myVersion);
        }
    }
}

