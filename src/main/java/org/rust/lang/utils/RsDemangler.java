/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.util.containers.PeekableIterator;
import com.intellij.util.containers.PeekableIteratorWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Demangle Rust compiler symbol names.
 * <p>
 * This class provides a {@code demangle} method which will return a {@code Demangle} sentinel value that can be used
 * to learn about the demangled version of a symbol name. The demangled representation will be the same as the original
 * if it doesn't look like a mangled symbol name.
 * <p>
 * Original crate: <a href="https://crates.io/crates/rustc-demangle">rustc-demangle</a>
 */
public final class RsDemangler {
    public static final RsDemangler INSTANCE = new RsDemangler();

    private static final Pattern RUST_HASH_RE = Pattern.compile("^h[0-9a-fA-F]*$");

    private static final Map<String, String> DOLLAR_TABLE;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("$SP$", "@");
        map.put("$BP$", "*");
        map.put("$RF$", "&");
        map.put("$LT$", "<");
        map.put("$GT$", ">");
        map.put("$LP$", "(");
        map.put("$RP$", ")");
        map.put("$C$", ",");
        map.put("$u7e$", "~");
        map.put("$u20$", " ");
        map.put("$u27$", "'");
        map.put("$u3d$", "=");
        map.put("$u5b$", "[");
        map.put("$u5d$", "]");
        map.put("$u7b$", "{");
        map.put("$u7d$", "}");
        map.put("$u3b$", ";");
        map.put("$u2b$", "+");
        map.put("$u21$", "!");
        map.put("$u22$", "\"");
        DOLLAR_TABLE = Collections.unmodifiableMap(map);
    }

    private RsDemangler() {
    }

    /**
     * Representation of a demangled symbol name.
     */
    public static class Demangle {
        private final String myOriginal;
        private final String myInner;
        private final String mySuffix;
        private final boolean myIsValid;
        private final int myElementsNum;

        public Demangle(@NotNull String original, @NotNull String inner, @NotNull String suffix,
                         boolean isValid, int elementsNum) {
            myOriginal = original;
            myInner = inner;
            mySuffix = suffix;
            myIsValid = isValid;
            myElementsNum = elementsNum;
        }

        @NotNull
        public String getOriginal() {
            return myOriginal;
        }

        @NotNull
        public String getInner() {
            return myInner;
        }

        @NotNull
        public String getSuffix() {
            return mySuffix;
        }

        public boolean isValid() {
            return myIsValid;
        }

        public int getElementsNum() {
            return myElementsNum;
        }

        @NotNull
        public String format() {
            return format(false);
        }

        @NotNull
        public String format(boolean skipHash) {
            if (!myIsValid) return myOriginal;

            StringBuilder sb = new StringBuilder();
            String inner = myInner;
            for (int elementIdx = 0; elementIdx < myElementsNum; elementIdx++) {
                PeekableIteratorWrapper<Character> innerIter = new PeekableIteratorWrapper<>(toCharIterator(inner));
                int i = parseInt(innerIter);
                String taken = take(innerIter, i);
                String rest = remaining(innerIter);
                inner = rest;
                rest = taken;

                if (skipHash && elementIdx + 1 == myElementsNum && isRustHash(rest)) {
                    break;
                }

                if (elementIdx != 0) {
                    sb.append("::");
                }

                if (rest.startsWith("_$")) {
                    rest = rest.substring(1);
                }

                loop:
                while (!rest.isEmpty()) {
                    if (rest.startsWith(".")) {
                        if (rest.startsWith("..")) {
                            sb.append("::");
                            rest = rest.substring(2);
                        } else {
                            sb.append(".");
                            rest = rest.substring(1);
                        }
                    } else if (rest.startsWith("$")) {
                        boolean found = false;
                        for (Map.Entry<String, String> entry : DOLLAR_TABLE.entrySet()) {
                            if (rest.startsWith(entry.getKey())) {
                                sb.append(entry.getValue());
                                rest = rest.substring(entry.getKey().length());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            sb.append(rest);
                            break loop;
                        }
                    } else {
                        int idx = -1;
                        for (int j = 0; j < rest.length(); j++) {
                            char ch = rest.charAt(j);
                            if (ch == '$' || ch == '.') {
                                idx = j;
                                break;
                            }
                        }
                        if (idx == -1) idx = rest.length();
                        sb.append(rest, 0, idx);
                        rest = rest.substring(idx);
                    }
                }
            }

            sb.append(mySuffix);
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Demangle demangle = (Demangle) o;
            return myIsValid == demangle.myIsValid &&
                myElementsNum == demangle.myElementsNum &&
                Objects.equals(myOriginal, demangle.myOriginal) &&
                Objects.equals(myInner, demangle.myInner) &&
                Objects.equals(mySuffix, demangle.mySuffix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myOriginal, myInner, mySuffix, myIsValid, myElementsNum);
        }

        @Override
        public String toString() {
            return "Demangle(original=" + myOriginal + ", inner=" + myInner +
                ", suffix=" + mySuffix + ", isValid=" + myIsValid + ", elementsNum=" + myElementsNum + ")";
        }
    }

    /**
     * De-mangles a Rust symbol into a more readable version.
     */
    @NotNull
    public Demangle demangle(@NotNull String name) {
        String text = name;

        // During ThinLTO LLVM may import and rename internal symbols
        String llvm = ".llvm.";
        int llvmIndex = text.indexOf(llvm);
        if (llvmIndex != -1) {
            String candidate = text.substring(llvmIndex + llvm.length());
            boolean allHex = true;
            for (int i = 0; i < candidate.length(); i++) {
                char ch = candidate.charAt(i);
                if (!((ch >= 'A' && ch <= 'F') || (ch >= '0' && ch <= '9') || ch == '@')) {
                    allHex = false;
                    break;
                }
            }
            if (allHex) {
                text = text.substring(0, llvmIndex);
            }
        }

        // Output like LLVM IR adds extra period-delimited words.
        String suffix = "";
        int eIndex = text.lastIndexOf("E.");
        if (eIndex != -1) {
            String head = text.substring(0, eIndex + 1);
            String tail = text.substring(eIndex + 1);
            if (isSymbolLike(tail)) {
                text = head;
                suffix = tail;
            }
        }

        boolean isValid = true;
        String inner = "";
        if (text.length() > 4 && text.startsWith("_ZN") && text.endsWith("E")) {
            inner = text.substring(3, text.length() - 1);
        } else if (text.length() > 3 && text.startsWith("ZN") && text.endsWith("E")) {
            inner = text.substring(2, text.length() - 1);
        } else if (text.length() > 5 && text.startsWith("__ZN") && text.endsWith("E")) {
            inner = text.substring(4, text.length() - 1);
        } else {
            isValid = false;
        }

        // Only work with ASCII text
        byte[] bytes = inner.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            if ((b & 0x80) != 0) {
                isValid = false;
                break;
            }
        }

        int elementsNum = 0;
        if (isValid) {
            PeekableIteratorWrapper<Character> charsIter = new PeekableIteratorWrapper<>(toCharIterator(inner));
            loop:
            while (isValid) {
                int count = parseInt(charsIter);
                if (count < 0) {
                    isValid = false;
                    break loop;
                }
                if (count == 0) {
                    isValid = !charsIter.hasNext();
                    break loop;
                }
                int taken = takeCount(charsIter, count);
                if (taken != count) {
                    isValid = false;
                } else {
                    elementsNum += 1;
                }
            }
        }

        return new Demangle(text, inner, suffix, isValid, elementsNum);
    }

    /**
     * The same as {@code demangle}, except return {@code null} if the string does not appear to be a Rust symbol.
     */
    @Nullable
    public Demangle tryDemangle(@NotNull String name) {
        Demangle sym = demangle(name);
        return sym.isValid() ? sym : null;
    }

    private static boolean isRustHash(@NotNull String text) {
        return RUST_HASH_RE.matcher(text).matches();
    }

    private static boolean isSymbolLike(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!isAsciiAlphanumeric(ch) && !isAsciiPunctuation(ch)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiAlphanumeric(char ch) {
        return (ch >= '\u0041' && ch <= '\u005A') ||
            (ch >= '\u0061' && ch <= '\u007A') ||
            (ch >= '\u0030' && ch <= '\u0039');
    }

    private static boolean isAsciiPunctuation(char ch) {
        return (ch >= '\u0021' && ch <= '\u002F') ||
            (ch >= '\u003A' && ch <= '\u0040') ||
            (ch >= '\u005B' && ch <= '\u0060') ||
            (ch >= '\u007B' && ch <= '\u007E');
    }

    private static int parseInt(@NotNull PeekableIterator<Character> iter) {
        int number = 0;
        while (iter.hasNext() && Character.isDigit(iter.peek())) {
            number = 10 * number + Character.getNumericValue(iter.next());
        }
        return number;
    }

    @NotNull
    private static String take(@NotNull Iterator<Character> iter, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (!iter.hasNext()) break;
            sb.append(iter.next());
        }
        return sb.toString();
    }

    private static int takeCount(@NotNull Iterator<Character> iter, int count) {
        int taken = 0;
        for (int i = 0; i < count; i++) {
            if (!iter.hasNext()) return taken;
            iter.next();
            taken++;
        }
        return taken;
    }

    @NotNull
    private static String remaining(@NotNull Iterator<Character> iter) {
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            sb.append(iter.next());
        }
        return sb.toString();
    }

    @NotNull
    private static Iterator<Character> toCharIterator(@NotNull String s) {
        return new Iterator<Character>() {
            private int myIndex = 0;

            @Override
            public boolean hasNext() {
                return myIndex < s.length();
            }

            @Override
            public Character next() {
                if (myIndex >= s.length()) throw new NoSuchElementException();
                return s.charAt(myIndex++);
            }
        };
    }
}
