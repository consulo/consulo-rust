/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.document.util.TextRange;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.stdext.StdextUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Must provide {@link #equals} method because it is used to track changes in the macro expansion mechanism
 */
public class RangeMap {

    public static final RangeMap EMPTY = new RangeMap(Collections.emptyList());

    private final List<MappedTextRange> myRanges;

    public RangeMap(@Nonnull List<MappedTextRange> ranges) {
        myRanges = ranges;
    }

    public RangeMap(@Nonnull MappedTextRange range) {
        myRanges = Collections.singletonList(range);
    }

    public RangeMap(@Nonnull SmartList<MappedTextRange> ranges) {
        myRanges = StdextUtil.optimizeList(ranges);
    }

    @Nonnull
    public List<MappedTextRange> getRanges() {
        return myRanges;
    }

    public boolean isEmpty() {
        return myRanges.isEmpty();
    }

    @Nullable
    public Integer mapOffsetFromExpansionToCallBody(int offset, @Nonnull StickTo stickTo) {
        switch (stickTo) {
            case LEFT:
                return mapOffsetFromExpansionToCallBody(offset, true);
            case RIGHT:
                return mapOffsetFromExpansionToCallBody(offset, false);
            case ANY: {
                Integer result = mapOffsetFromExpansionToCallBody(offset, false);
                if (result != null) return result;
                return mapOffsetFromExpansionToCallBody(offset, true);
            }
            default:
                throw new IllegalStateException("Unexpected value: " + stickTo);
        }
    }

    @Nullable
    public Integer mapOffsetFromExpansionToCallBody(int offset, boolean stickToLeft) {
        MappedTextRange found = null;
        for (MappedTextRange range : myRanges) {
            boolean matches = offset >= range.getDstOffset()
                && (offset < range.getDstEndOffset() || (stickToLeft && offset == range.getDstEndOffset()));
            if (matches) {
                if (found != null) {
                    // Not unique
                    return null;
                }
                found = range;
            }
        }
        if (found != null) {
            return found.getSrcOffset() + (offset - found.getDstOffset());
        }
        return null;
    }

    @Nonnull
    public List<Integer> mapOffsetFromCallBodyToExpansion(int offset) {
        List<Integer> result = new ArrayList<>();
        for (MappedTextRange range : myRanges) {
            if (offset >= range.getSrcOffset() && offset < range.getSrcEndOffset()) {
                result.add(range.getDstOffset() + (offset - range.getSrcOffset()));
            }
        }
        return result;
    }

    @Nonnull
    public List<MappedTextRange> mapTextRangeFromExpansionToCallBody(@Nonnull TextRange toMap) {
        List<MappedTextRange> result = new ArrayList<>();
        for (MappedTextRange range : myRanges) {
            MappedTextRange intersection = range.dstIntersection(toMap);
            if (intersection != null) {
                result.add(intersection);
            }
        }
        return result;
    }

    @Nonnull
    public List<MappedTextRange> mapMappedTextRangeFromExpansionToCallBody(@Nonnull MappedTextRange toMap) {
        List<MappedTextRange> mapped = mapTextRangeFromExpansionToCallBody(
            new TextRange(toMap.getSrcOffset(), toMap.getSrcEndOffset())
        );
        List<MappedTextRange> result = new ArrayList<>(mapped.size());
        for (MappedTextRange m : mapped) {
            result.add(new MappedTextRange(
                m.getSrcOffset(),
                toMap.getDstOffset() + (m.getDstOffset() - toMap.getSrcOffset()),
                m.getLength()
            ));
        }
        return result;
    }

    @Nonnull
    public RangeMap mapAll(@Nonnull RangeMap other) {
        List<MappedTextRange> result = new ArrayList<>();
        for (MappedTextRange range : other.myRanges) {
            result.addAll(mapMappedTextRangeFromExpansionToCallBody(range));
        }
        return new RangeMap(result);
    }

    public void writeTo(@Nonnull DataOutput data) throws IOException {
        data.writeInt(myRanges.size());
        for (MappedTextRange range : myRanges) {
            writeMappedTextRange(data, range);
        }
    }

    @Nonnull
    public static RangeMap readFrom(@Nonnull DataInput data) throws IOException {
        int size = data.readInt();
        List<MappedTextRange> ranges = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ranges.add(readMappedTextRange(data));
        }
        return new RangeMap(ranges);
    }

    private static void writeMappedTextRange(@Nonnull DataOutput data, @Nonnull MappedTextRange range) throws IOException {
        StdextUtil.writeVarInt(data, range.getSrcOffset());
        StdextUtil.writeVarInt(data, range.getDstOffset());
        StdextUtil.writeVarInt(data, range.getLength());
    }

    @Nonnull
    private static MappedTextRange readMappedTextRange(@Nonnull DataInput data) throws IOException {
        return new MappedTextRange(
            StdextUtil.readVarInt(data),
            StdextUtil.readVarInt(data),
            StdextUtil.readVarInt(data)
        );
    }

    /**
     * Adds the range to the list or merges it with the last list element if they intersect.
     * Used as an optimization to reduce the list size.
     */
    public static void mergeAdd(@Nonnull List<MappedTextRange> list, @Nonnull MappedTextRange range) {
        if (!list.isEmpty()) {
            MappedTextRange last = list.get(list.size() - 1);
            if (last.getSrcEndOffset() == range.getSrcOffset() && last.getDstEndOffset() == range.getDstOffset()) {
                list.set(list.size() - 1, new MappedTextRange(
                    last.getSrcOffset(),
                    last.getDstOffset(),
                    last.getLength() + range.getLength()
                ));
                return;
            }
        }
        list.add(range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeMap rangeMap = (RangeMap) o;
        return myRanges.equals(rangeMap.myRanges);
    }

    @Override
    public int hashCode() {
        return myRanges.hashCode();
    }

    public enum StickTo {
        LEFT, RIGHT, ANY
    }
}
