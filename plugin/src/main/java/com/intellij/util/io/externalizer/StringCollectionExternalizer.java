package com.intellij.util.io.externalizer;

import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** IntelliJ-compat stub — externalizes a list of strings. */
public final class StringCollectionExternalizer {
    public static final DataExternalizer<List<String>> STRING_LIST_EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, List<String> value) throws IOException {
            DataInputOutputUtil.writeINT(out, value.size());
            for (String s : value) {
                IOUtil.writeUTF(out, s);
            }
        }

        @Override
        public List<String> read(DataInput in) throws IOException {
            int size = DataInputOutputUtil.readINT(in);
            List<String> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(IOUtil.readUTF(in));
            }
            return result;
        }
    };

    private StringCollectionExternalizer() {}
}
