/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.containers.PeekableIteratorWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LcovCoverageReport {

    private final Map<String, List<LineHits>> info = new HashMap<>();

    @NotNull
    public Set<Map.Entry<String, List<LineHits>>> getRecords() {
        return info.entrySet();
    }

    public void mergeFileReport(@Nullable String basePath, @NotNull String filePath, @NotNull List<LineHits> report) {
        File file = new File(filePath);
        if (!file.isAbsolute() && basePath != null) {
            file = new File(basePath, filePath);
        }
        String normalizedFilePath = FileUtilRt.toSystemIndependentName(file.getPath());
        List<LineHits> oldReport = info.get(normalizedFilePath);
        List<LineHits> normalized = normalizeLineHitsList(report);
        List<LineHits> result = (oldReport == null) ? normalized : doMerge(oldReport, report);
        info.put(normalizedFilePath, result);
    }

    public static class LineHits {
        private final int lineNumber;
        private int hits;

        public LineHits(int lineNumber, int hits) {
            this.lineNumber = lineNumber;
            this.hits = hits;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getHits() {
            return hits;
        }

        public void addHits(int hitCount) {
            if (hits == -1) {
                return;
            } else if (hitCount == -1) {
                hits = hitCount;
            } else {
                hits += hitCount;
            }
        }
    }

    public static final class Serialization {

        private static final String SOURCE_FILE_PREFIX = "SF:";
        private static final String LINE_HIT_PREFIX = "DA:";
        private static final String END_OF_RECORD = "end_of_record";

        private Serialization() {
        }

        @NotNull
        public static LcovCoverageReport readLcov(@NotNull File lcovFile, @Nullable String localBaseDir) throws IOException {
            LcovCoverageReport report = new LcovCoverageReport();
            String[] currentFileName = {null};
            @SuppressWarnings("unchecked")
            List<LineHits>[] lineDataList = new List[]{null};
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lcovFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(SOURCE_FILE_PREFIX)) {
                        currentFileName[0] = line.substring(SOURCE_FILE_PREFIX.length());
                        lineDataList[0] = new ArrayList<>();
                    } else if (line.startsWith(LINE_HIT_PREFIX)) {
                        Objects.requireNonNull(lineDataList[0]);
                        String valuesStr = line.substring(LINE_HIT_PREFIX.length());
                        String[] values = valuesStr.split(",");
                        // Drop last while empty
                        int end = values.length;
                        while (end > 0 && values[end - 1].isEmpty()) {
                            end--;
                        }
                        if (end != 2) {
                            throw new IllegalStateException("Expected 2 values, got " + end);
                        }
                        int lineNum;
                        try {
                            lineNum = Integer.parseInt(values[0]);
                        } catch (NumberFormatException e) {
                            continue; // equivalent to return@forEachLine
                        }
                        int hitCount;
                        try {
                            hitCount = Integer.parseInt(values[1]);
                        } catch (NumberFormatException e) {
                            hitCount = -1;
                        }
                        LineHits lineHits = new LineHits(lineNum, hitCount);
                        lineDataList[0].add(lineHits);
                    } else if (END_OF_RECORD.equals(line)) {
                        report.mergeFileReport(
                            localBaseDir,
                            Objects.requireNonNull(currentFileName[0]),
                            Objects.requireNonNull(lineDataList[0])
                        );
                        currentFileName[0] = null;
                        lineDataList[0] = null;
                    }
                }
            }
            if (lineDataList[0] != null) {
                throw new IllegalStateException("Unexpected end of lcov data");
            }
            return report;
        }

        @NotNull
        public static LcovCoverageReport readLcov(@NotNull File lcovFile) throws IOException {
            return readLcov(lcovFile, null);
        }

        public static void writeLcov(@NotNull LcovCoverageReport report, @NotNull File outputFile) throws IOException {
            try (PrintWriter out = new PrintWriter(outputFile)) {
                for (Map.Entry<String, List<LineHits>> entry : report.info.entrySet()) {
                    String filePath = entry.getKey();
                    List<LineHits> fileLineHits = entry.getValue();
                    out.print(SOURCE_FILE_PREFIX);
                    out.println(filePath);
                    for (LineHits lineHits : fileLineHits) {
                        out.print(LINE_HIT_PREFIX);
                        out.print(lineHits.getLineNumber());
                        out.print(',');
                        out.println(lineHits.getHits());
                    }
                    out.println(END_OF_RECORD);
                }
            }
        }
    }

    @NotNull
    private static List<LineHits> normalizeLineHitsList(@NotNull List<LineHits> lineHits) {
        Set<Integer> seen = new HashSet<>();
        return lineHits.stream()
            .sorted(Comparator.comparingInt(LineHits::getLineNumber))
            .filter(lh -> seen.add(lh.getLineNumber()))
            .collect(Collectors.toList());
    }

    @NotNull
    private static List<LineHits> doMerge(@NotNull List<LineHits> list1, @NotNull List<LineHits> list2) {
        List<LineHits> result = new ArrayList<>();
        PeekableIteratorWrapper<LineHits> iter1 = new PeekableIteratorWrapper<>(list1.iterator());
        PeekableIteratorWrapper<LineHits> iter2 = new PeekableIteratorWrapper<>(list2.iterator());
        while (iter1.hasNext() && iter2.hasNext()) {
            LineHits head1 = iter1.peek();
            LineHits head2 = iter2.peek();
            LineHits next;
            if (head1.getLineNumber() < head2.getLineNumber()) {
                next = iter1.next();
            } else if (head1.getLineNumber() > head2.getLineNumber()) {
                next = iter2.next();
            } else {
                head1.addHits(head2.getHits());
                iter1.next();
                iter2.next();
                next = head1;
            }
            result.add(next);
        }
        iter1.forEachRemaining(result::add);
        iter2.forEachRemaining(result::add);
        return result;
    }
}
