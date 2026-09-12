/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.ObjectStubSerializer;
import consulo.language.psi.stub.ObjectStubSerializerProvider;
import consulo.language.psi.stub.StubElementTypeHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes every Rust stub serializer known to the platform before the serializer registry is sealed.
 * Instantiating each element type here is what puts the stub types into the registry; a type created
 * later cannot be looked up by external id and any stub referring to it fails to deserialize.
 */
@ExtensionImpl
public class RsStubElementTypeHolder extends StubElementTypeHolder<RsElementTypes> {
    @Nullable
    @Override
    public String getExternalIdPrefix() {
        // Element ids are taken from the serializers themselves: RsElementTypes also holds
        // plain (non-stub) element types, which have no external id at all.
        return null;
    }

    @Nonnull
    @Override
    public List<ObjectStubSerializerProvider> loadSerializers() {
        List<ObjectStubSerializerProvider> result = new ArrayList<>();
        add(result, RsFileStub.Type);
        for (Field field : RsElementTypes.class.getFields()) {
            Object value;
            try {
                value = field.get(null);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read " + field, e);
            }
            add(result, value);
        }
        return result;
    }

    private static void add(@Nonnull List<ObjectStubSerializerProvider> result, @Nullable Object value) {
        if (value instanceof ObjectStubSerializer) {
            result.add(new Provider((ObjectStubSerializer<?, ?>)value));
        }
    }

    private static final class Provider implements ObjectStubSerializerProvider {
        private final ObjectStubSerializer<?, ?> mySerializer;

        private Provider(@Nonnull ObjectStubSerializer<?, ?> serializer) {
            mySerializer = serializer;
        }

        @Override
        public String getExternalId() {
            return mySerializer.getExternalId();
        }

        @Override
        public boolean isLazy() {
            return false;
        }

        @Override
        public ObjectStubSerializer getObjectStubSerializer() {
            return mySerializer;
        }
    }
}
