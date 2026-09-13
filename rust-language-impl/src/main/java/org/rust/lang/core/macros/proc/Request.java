/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonSerializer;
import org.rust.util.RsJacksonSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Represents a request to be sent to the proc macro expander process.
 */
public abstract class Request {

    private Request() {}

    public static final class ExpandMacro extends Request {
        @Nonnull
        private final FlatTree myMacroBody;
        @Nonnull
        private final String myMacroName;
        @Nullable
        private final FlatTree myAttributes;
        @Nonnull
        private final String myLib;
        @Nonnull
        private final List<List<String>> myEnv;
        @Nullable
        private final String myCurrentDir;

        public ExpandMacro(
            @Nonnull FlatTree macroBody,
            @Nonnull String macroName,
            @Nullable FlatTree attributes,
            @Nonnull String lib,
            @Nonnull List<List<String>> env,
            @Nullable String currentDir
        ) {
            myMacroBody = macroBody;
            myMacroName = macroName;
            myAttributes = attributes;
            myLib = lib;
            myEnv = env;
            myCurrentDir = currentDir;
        }

        @Nonnull
        public FlatTree getMacroBody() {
            return myMacroBody;
        }

        @Nonnull
        public String getMacroName() {
            return myMacroName;
        }

        @Nullable
        public FlatTree getAttributes() {
            return myAttributes;
        }

        @Nonnull
        public String getLib() {
            return myLib;
        }

        @Nonnull
        public List<List<String>> getEnv() {
            return myEnv;
        }

        @Nullable
        public String getCurrentDir() {
            return myCurrentDir;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpandMacro)) return false;
            ExpandMacro that = (ExpandMacro) o;
            return Objects.equals(myMacroBody, that.myMacroBody)
                && Objects.equals(myMacroName, that.myMacroName)
                && Objects.equals(myAttributes, that.myAttributes)
                && Objects.equals(myLib, that.myLib)
                && Objects.equals(myEnv, that.myEnv)
                && Objects.equals(myCurrentDir, that.myCurrentDir);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMacroBody, myMacroName, myAttributes, myLib, myEnv, myCurrentDir);
        }
    }

    public static final ApiVersionCheck API_VERSION_CHECK = new ApiVersionCheck();

    public static final class ApiVersionCheck extends Request {
        private ApiVersionCheck() {}
    }
}

