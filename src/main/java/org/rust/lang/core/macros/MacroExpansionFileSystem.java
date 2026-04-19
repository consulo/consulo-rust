/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.BufferExposingByteArrayInputStream;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileAttributes.CaseSensitivity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.VfsImplUtil;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link com.intellij.openapi.vfs.VirtualFileSystem} used to store macro expansions.
 */
public class MacroExpansionFileSystem extends NewVirtualFileSystem {
    private final FSDir root = new FSDir(null, "/");

    private static final String PROTOCOL = "rust-macros";
    private static final Key<Boolean> RUST_MACROS_ALLOW_WRITING = Key.create("RUST_MACROS_ALLOW_WRITING");

    @NotNull
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @NotNull
    @Override
    public String extractRootPath(@NotNull String path) {
        return "/";
    }

    @Nullable
    @Override
    public String normalize(@NotNull String path) {
        return path;
    }

    @NotNull
    @Override
    public String getCanonicallyCasedName(@NotNull VirtualFile file) {
        return file.getName();
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public void refresh(boolean asynchronous) {
        VfsImplUtil.refresh(this, asynchronous);
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@NotNull String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Nullable
    @Override
    public VirtualFile findFileByPathIfCached(@NotNull String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    public boolean isValidName(@NotNull String name) {
        return PathUtilRt.isValidFileName(name, PathUtilRt.Platform.UNIX, false, null);
    }

    @NotNull
    @Override
    public VirtualFile createChildDirectory(@Nullable Object requestor, @NotNull VirtualFile parent, @NotNull String dir) throws IOException {
        if (requestor != TrustedRequestor.INSTANCE) {
            throw new UnsupportedOperationException();
        }
        FSItem parentFsDir = convert(parent);
        if (parentFsDir == null) throw new FileNotFoundException(parent.getPath() + " (No such file or directory)");
        if (!(parentFsDir instanceof FSDir)) throw new IOException(parent.getPath() + " is not a directory");
        FSItem existingDir = ((FSDir) parentFsDir).findChild(dir);
        if (existingDir == null) {
            ((FSDir) parentFsDir).addChildDir(dir, true);
        } else if (!(existingDir instanceof FSDir)) {
            throw new IOException("Directory already contains a file named " + dir);
        }
        return new FakeVirtualFile(parent, dir);
    }

    @NotNull
    @Override
    public VirtualFile createChildFile(@Nullable Object requestor, @NotNull VirtualFile parent, @NotNull String file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public VirtualFile copyFile(@Nullable Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile(@Nullable Object requestor, @NotNull VirtualFile file) throws IOException {
        if (requestor != TrustedRequestor.INSTANCE) {
            throw new UnsupportedOperationException();
        }
        FSItem fsItem = convert(file);
        if (fsItem == null) return;
        FSDir parent = fsItem.getParent();
        if (parent == null) throw new IOException("Can't delete root (" + file.getPath() + ")");
        parent.removeChild(fsItem.getName(), true);
    }

    @Override
    public void moveFile(@Nullable Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent) throws IOException {
        if (requestor != TrustedRequestor.INSTANCE) {
            throw new UnsupportedOperationException();
        }
        if (file.getParent() == newParent) {
            throw new IOException("Cannot move file `" + file.getPath() + "` to the same directory where it is already located");
        }
        FSItem fsItem = convert(file);
        if (fsItem == null) throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        FSItem newParentFsDir = convert(newParent);
        if (newParentFsDir == null) throw new FileNotFoundException(newParent.getPath() + " (No such file or directory)");
        if (!(newParentFsDir instanceof FSDir)) throw new IOException(newParent.getPath() + " is not a directory");
        FSDir newParentDir = (FSDir) newParentFsDir;
        if (newParentDir.findChild(file.getName()) != null) {
            throw new IOException("Directory already contains a file named " + file.getName());
        }
        FSDir oldParentFsDir = fsItem.getParent();
        if (oldParentFsDir == null) throw new IOException("Can't move root (" + file.getPath() + ")");
        oldParentFsDir.removeChild(fsItem.getName(), true);
        fsItem.setParent(newParentDir);
        newParentDir.addChild(fsItem, true, false);
    }

    @Override
    public void renameFile(@Nullable Object requestor, @NotNull VirtualFile file, @NotNull String newName) throws IOException {
        if (requestor != TrustedRequestor.INSTANCE) {
            throw new UnsupportedOperationException();
        }
        FSItem fsItem = convert(file);
        if (fsItem == null) throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        FSDir parent = fsItem.getParent();
        if (parent == null) throw new IOException("Can't rename root (" + file.getPath() + ")");
        parent.removeChild(fsItem.getName(), false);
        fsItem.setName(newName);
        parent.addChild(fsItem, true, false);
    }

    @Nullable
    private FSItem convert(@NotNull VirtualFile file) {
        VirtualFile parentFile = file.getParent();
        if (parentFile == null) return root;

        FSItem parentItem = convert(parentFile);
        if (parentItem instanceof FSDir) {
            return ((FSDir) parentItem).findChild(file.getName());
        }
        return null;
    }

    @Nullable
    private FSItem convert(@NotNull String path, boolean mkdirs) {
        FSItem file = root;
        for (String segment : StringUtil.split(path, "/")) {
            if (!(file instanceof FSDir)) return null;
            FSItem child = ((FSDir) file).findChild(segment);
            if (child == null) {
                if (mkdirs) {
                    child = ((FSDir) file).addChildDir(segment, false);
                } else {
                    return null;
                }
            }
            file = child;
        }
        return file;
    }

    @Nullable
    private FSItem convert(@NotNull String path) {
        return convert(path, false);
    }

    @Override
    public boolean exists(@NotNull VirtualFile fileOrDirectory) {
        return convert(fileOrDirectory) != null;
    }

    @NotNull
    @Override
    public String[] list(@NotNull VirtualFile file) {
        FSItem fsItem = convert(file);
        if (fsItem instanceof FSDir) {
            return ((FSDir) fsItem).list();
        }
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public boolean isDirectory(@NotNull VirtualFile file) {
        return convert(file) instanceof FSDir;
    }

    @Override
    public long getTimeStamp(@NotNull VirtualFile file) {
        FSItem fsItem = convert(file);
        if (fsItem == null) return DEFAULT_TIMESTAMP;
        return fsItem.getTimestamp();
    }

    @Override
    public void setTimeStamp(@NotNull VirtualFile file, long timeStamp) {
        FSItem fsItem = convert(file);
        if (fsItem == null) return;
        fsItem.setTimestamp(timeStamp > 0 ? timeStamp : currentTimestamp());
    }

    @Override
    public boolean isWritable(@NotNull VirtualFile file) {
        return true;
    }

    @Override
    public void setWritable(@NotNull VirtualFile file, boolean writableFlag) {
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray(@NotNull VirtualFile file) throws IOException {
        FSItem fsItem = convert(file);
        if (fsItem == null) throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        if (!(fsItem instanceof FSFile)) throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        FSFile fsFile = (FSFile) fsItem;
        byte[] content = fsFile.fetchAndRemoveContent();
        if (content != null) return content;

        Pair<HashCode, Integer> hashAndVersion = ExpandedMacroStorage.extractMixHashAndMacroStorageVersion(file);
        HashCode mixHash = hashAndVersion != null ? hashAndVersion.getFirst() : null;
        int storedVersion = hashAndVersion != null ? hashAndVersion.getSecond() : -1;

        if (mixHash != null && storedVersion == ExpandedMacroStorage.MACRO_STORAGE_VERSION) {
            var expansion = MacroExpansionSharedCache.getInstance().getExpansionIfCached(mixHash);
            if (expansion != null) {
                var ok = expansion.ok();
                if (ok != null) {
                    return ok.getText().getBytes();
                }
            }
        }

        if (storedVersion != ExpandedMacroStorage.MACRO_STORAGE_VERSION) {
            // Found old version -> the file will be deleted by MacroExpansionTask
            return ArrayUtil.EMPTY_BYTE_ARRAY;
        }

        fsFile.delete();
        FileNotFoundException e = new FileNotFoundException(file.getPath() + " (Content is not provided)");
        MacroExpansionManagerUtil.MACRO_LOG.warn("The file content has already been fetched", e);
        throw e;
    }

    @NotNull
    @Override
    public InputStream getInputStream(@NotNull VirtualFile file) throws IOException {
        return new BufferExposingByteArrayInputStream(contentsToByteArray(file));
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(@NotNull VirtualFile file, @Nullable Object requestor, long modStamp, long timeStamp) throws IOException {
        if (requestor != TrustedRequestor.INSTANCE && !isWritingAllowed(file)) {
            throw new UnsupportedOperationException();
        }
        final VirtualFile theFile = file;
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                FSItem fsItem = convert(theFile);
                if (!(fsItem instanceof FSFile)) throw new IOException("The file is a directory (" + theFile.getPath() + ")");
                ((FSFile) fsItem).setLength(size());
                setTimeStamp(theFile, timeStamp);
            }
        };
    }

    @Override
    public long getLength(@NotNull VirtualFile file) {
        FSItem fsItem = convert(file);
        if (fsItem instanceof FSFile) {
            return ((FSFile) fsItem).getLength();
        }
        return DEFAULT_LENGTH;
    }

    @Nullable
    @Override
    public FileAttributes getAttributes(@NotNull VirtualFile file) {
        FSItem item = convert(file);
        if (item == null) return null;
        long length = item instanceof FSFile ? ((FSFile) item).getLength() : 0;
        boolean isDir = item instanceof FSDir;
        CaseSensitivity caseSensitivity = isDir ? CaseSensitivity.SENSITIVE : CaseSensitivity.UNKNOWN;
        return new FileAttributes(isDir, false, false, false, length, item.getTimestamp(), true, caseSensitivity);
    }

    // --- FSItem hierarchy ---

    public static abstract class FSItem {
        private volatile FSDir myParent;
        private volatile String myName;
        private volatile long myTimestamp;

        protected FSItem(@Nullable FSDir parent, @NotNull String name, long timestamp) {
            myParent = parent;
            myName = name;
            myTimestamp = timestamp;
        }

        @Nullable
        public synchronized FSDir getParent() {
            return myParent;
        }

        public synchronized void setParent(@Nullable FSDir parent) {
            myParent = parent;
        }

        @NotNull
        public synchronized String getName() {
            return myName;
        }

        public synchronized void setName(@NotNull String name) {
            myName = name;
        }

        public synchronized long getTimestamp() {
            return myTimestamp;
        }

        public synchronized void setTimestamp(long timestamp) {
            myTimestamp = timestamp;
        }

        public void bumpTimestamp() {
            setTimestamp(Math.max(currentTimestamp(), getTimestamp() + 1));
        }

        public void delete() {
            FSDir parent = getParent();
            if (parent != null) {
                parent.removeChild(getName(), true);
            }
        }

        @NotNull
        public String absolutePath() {
            StringBuilder sb = new StringBuilder();
            buildPath(sb);
            return sb.toString();
        }

        void buildPath(@NotNull StringBuilder sb) {
            FSDir parent = getParent();
            if (parent != null) {
                parent.buildPath(sb);
            }
            sb.append('/');
            sb.append(getName());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ": " + getName();
        }
    }

    public static class FSDir extends FSItem {
        protected final List<FSItem> children;

        public FSDir(@Nullable FSDir parent, @NotNull String name) {
            this(parent, name, currentTimestamp());
        }

        public FSDir(@Nullable FSDir parent, @NotNull String name, long timestamp) {
            super(parent, name, timestamp);
            children = new ArrayList<>();
        }

        /**
         * Constructor for subclasses that need to provide their own children list.
         */
        protected FSDir(@Nullable FSDir parent, @NotNull String name, long timestamp, boolean customChildren) {
            super(parent, name, timestamp);
            children = customChildren ? null : new ArrayList<>();
        }

        @NotNull
        protected List<FSItem> getChildren() {
            return children;
        }

        @NotNull
        public synchronized List<FSItem> copyChildren() {
            return new ArrayList<>(getChildren());
        }

        @Nullable
        public synchronized FSItem findChild(@NotNull String name) {
            for (FSItem child : getChildren()) {
                if (child.getName().equals(name)) {
                    return child;
                }
            }
            return null;
        }

        public synchronized void addChild(@NotNull FSItem item, boolean bump, boolean override) {
            if (item.getParent() != this) {
                throw new IllegalArgumentException("item.parent != this");
            }
            if (item.getName().isEmpty()) {
                throw new IllegalArgumentException("item.name is empty");
            }
            if (override) {
                getChildren().removeIf(it -> it.getName().equals(item.getName()));
            } else {
                for (FSItem child : getChildren()) {
                    if (child.getName().equals(item.getName())) {
                        throw new FSItemAlreadyExistsException("File `" + item.getName() + "` already exists");
                    }
                }
            }
            getChildren().add(item);
            if (bump) {
                bumpTimestamp();
            }
        }

        public void addChild(@NotNull FSItem item, boolean bump) {
            addChild(item, bump, false);
        }

        public void addChild(@NotNull FSItem item) {
            addChild(item, false, false);
        }

        @NotNull
        public FSFile addChildFile(@NotNull String name, boolean bump) {
            FSFile file = new FSFile(this, name);
            addChild(file, bump);
            return file;
        }

        @NotNull
        public FSFile addChildFile(@NotNull String name) {
            return addChildFile(name, false);
        }

        @NotNull
        public FSDir addChildDir(@NotNull String name, boolean bump) {
            FSDir dir = new FSDir(this, name);
            addChild(dir, bump);
            return dir;
        }

        @NotNull
        public FSDir addChildDir(@NotNull String name) {
            return addChildDir(name, false);
        }

        public synchronized void removeChild(@NotNull String name, boolean bump) {
            getChildren().removeIf(it -> it.getName().equals(name));
            if (bump) {
                bumpTimestamp();
            }
        }

        public void removeChild(@NotNull String name) {
            removeChild(name, false);
        }

        @NotNull
        public synchronized String[] list() {
            List<FSItem> ch = getChildren();
            String[] result = new String[ch.size()];
            for (int i = 0; i < ch.size(); i++) {
                result[i] = ch.get(i).getName();
            }
            return result;
        }

        public synchronized void clear(boolean bump) {
            getChildren().clear();
            if (bump) {
                bumpTimestamp();
            }
        }

        public void clear() {
            clear(false);
        }

        @NotNull
        public FSDir dummy() {
            return new DummyDir(getParent(), getName(), getTimestamp());
        }
    }

    public static class DummyDir extends FSDir {
        public DummyDir(@Nullable FSDir parent, @NotNull String name, long timestamp) {
            super(parent, name, timestamp, true);
        }

        public DummyDir(@Nullable FSDir parent, @NotNull String name) {
            this(parent, name, currentTimestamp());
        }

        @NotNull
        @Override
        protected List<FSItem> getChildren() {
            if (OpenApiUtil.isUnitTestMode()) {
                throw new IllegalStateException("DummyDir should not be touched!");
            }
            FSDir parent = getParent();
            if (parent != null) {
                parent.removeChild(getName(), false);
            }
            return new ArrayList<>();
        }
    }

    public static class FSFile extends FSItem {
        private volatile int myLength;
        private volatile byte[] myTempContent;

        public FSFile(@NotNull FSDir parent, @NotNull String name) {
            this(parent, name, currentTimestamp(), 0, null);
        }

        public FSFile(@NotNull FSDir parent, @NotNull String name, long timestamp, int length, @Nullable byte[] tempContent) {
            super(parent, name, timestamp);
            myLength = length;
            myTempContent = tempContent;
        }

        public synchronized int getLength() {
            return myLength;
        }

        public synchronized void setLength(int length) {
            myLength = length;
        }

        @Nullable
        public synchronized byte[] getTempContent() {
            return myTempContent;
        }

        public synchronized void setTempContent(@Nullable byte[] content) {
            myTempContent = content;
        }

        public synchronized void setContent(@NotNull byte[] content) {
            myTempContent = content;
            myLength = content.length;
            bumpTimestamp();
        }

        public synchronized void setImplicitContent(int fileSize) {
            myTempContent = null;
            myLength = fileSize;
            bumpTimestamp();
        }

        @Nullable
        public synchronized byte[] fetchAndRemoveContent() {
            byte[] tmp = myTempContent;
            myTempContent = null;
            return tmp;
        }
    }

    // --- Exception hierarchy ---

    public static class FSException extends RuntimeException {
        public FSException(@NotNull String path) {
            super(path);
        }
    }

    public static class FSItemNotFoundException extends FSException {
        public FSItemNotFoundException(@NotNull String path) {
            super(path);
        }
    }

    public static class FSItemIsNotADirectoryException extends FSException {
        public FSItemIsNotADirectoryException(@NotNull String path) {
            super(path);
        }
    }

    public static class FSItemIsADirectoryException extends FSException {
        public FSItemIsADirectoryException(@NotNull String path) {
            super(path);
        }
    }

    public static class FSItemAlreadyExistsException extends FSException {
        public FSItemAlreadyExistsException(@NotNull String path) {
            super(path);
        }
    }

    public static class IllegalPathException extends FSException {
        public IllegalPathException(@NotNull String path) {
            super(path);
        }
    }

    // --- TrustedRequestor ---

    public static class TrustedRequestor {
        public static final TrustedRequestor INSTANCE = new TrustedRequestor();
        private TrustedRequestor() {}
    }

    // --- Utility methods ---

    @NotNull
    private String[] splitFilenameAndParent(@NotNull String path) {
        int index = path.lastIndexOf('/');
        if (index < 0) throw new IllegalPathException(path);
        String pathStart = path.substring(0, index);
        String filename = path.substring(index + 1);
        return new String[]{pathStart, filename};
    }

    public void createFileWithExplicitContent(@NotNull String path, @NotNull byte[] content, boolean mkdirs) {
        createFileWithoutContent(path, mkdirs).setContent(content);
    }

    public void createFileWithExplicitContent(@NotNull String path, @NotNull byte[] content) {
        createFileWithExplicitContent(path, content, false);
    }

    public void createFileWithImplicitContent(@NotNull String path, int fileSize, boolean mkdirs) {
        createFileWithoutContent(path, mkdirs).setImplicitContent(fileSize);
    }

    public void createFileWithImplicitContent(@NotNull String path, int fileSize) {
        createFileWithImplicitContent(path, fileSize, false);
    }

    @NotNull
    public FSFile createFileWithoutContent(@NotNull String path, boolean mkdirs) {
        String[] parts = splitFilenameAndParent(path);
        String parentName = parts[0];
        String name = parts[1];
        FSItem parent = convert(parentName, mkdirs);
        if (parent == null) throw new FSItemNotFoundException(parentName);
        if (!(parent instanceof FSDir)) throw new FSItemIsNotADirectoryException(path);
        return ((FSDir) parent).addChildFile(name);
    }

    @NotNull
    public FSFile createFileWithoutContent(@NotNull String path) {
        return createFileWithoutContent(path, false);
    }

    public void setFileContent(@NotNull String path, @NotNull byte[] content) {
        FSItem item = convert(path);
        if (item == null) throw new FSItemNotFoundException(path);
        if (!(item instanceof FSFile)) throw new FSItemIsADirectoryException(path);
        ((FSFile) item).setContent(content);
    }

    public void createDirectoryIfNotExistsOrDummy(@NotNull String path) {
        String[] parts = splitFilenameAndParent(path);
        String parentName = parts[0];
        String name = parts[1];
        FSItem parent = convert(parentName);
        if (parent == null) throw new FSItemNotFoundException(parentName);
        if (!(parent instanceof FSDir)) throw new FSItemIsNotADirectoryException(path);
        FSDir parentDir = (FSDir) parent;
        FSItem child = parentDir.findChild(name);
        if (child == null || child instanceof DummyDir) {
            if (child instanceof DummyDir) {
                parentDir.removeChild(name);
            }
            parentDir.addChildDir(name, true);
        }
    }

    public void setDirectory(@NotNull String path, @NotNull FSDir dir, boolean override) {
        String[] parts = splitFilenameAndParent(path);
        String parentName = parts[0];
        String name = parts[1];
        FSItem parent = convert(parentName);
        if (parent == null) throw new FSItemNotFoundException(parentName);
        if (!(parent instanceof FSDir)) throw new FSItemIsNotADirectoryException(path);
        dir.setParent((FSDir) parent);
        dir.setName(name);
        ((FSDir) parent).addChild(dir, true, override);
    }

    public void setDirectory(@NotNull String path, @NotNull FSDir dir) {
        setDirectory(path, dir, true);
    }

    public void makeDummy(@NotNull String path) {
        FSDir dir = getDirectory(path);
        if (dir != null) {
            setDirectory(path, dir.dummy());
        }
    }

    @Nullable
    public FSDir getDirectory(@NotNull String path) {
        FSItem item = convert(path);
        return item instanceof FSDir ? (FSDir) item : null;
    }

    public void deleteFilePath(@NotNull String path) {
        String[] parts = splitFilenameAndParent(path);
        String parentName = parts[0];
        String name = parts[1];
        FSItem parent = convert(parentName);
        if (parent == null) return;
        if (!(parent instanceof FSDir)) throw new FSItemIsNotADirectoryException(path);
        ((FSDir) parent).removeChild(name, true);
    }

    public void cleanDirectoryIfExists(@NotNull String path, boolean bump) {
        FSItem dir = convert(path);
        if (dir == null) return;
        if (!(dir instanceof FSDir)) throw new FSItemIsNotADirectoryException(path);
        ((FSDir) dir).clear(bump);
    }

    public void cleanDirectoryIfExists(@NotNull String path) {
        cleanDirectoryIfExists(path, true);
    }

    public boolean exists(@NotNull String path) {
        return convert(path) != null;
    }

    // --- Static companion methods ---

    @NotNull
    public static MacroExpansionFileSystem getInstance() {
        return (MacroExpansionFileSystem) VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
    }

    @Nullable
    public static MacroExpansionFileSystem getInstanceOrNull() {
        return (MacroExpansionFileSystem) VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
    }

    public static void writeFSItem(@NotNull DataOutput data, @NotNull FSItem item) throws IOException {
        data.writeBoolean(item instanceof FSDir);
        data.writeUTF(item.getName());
        data.writeLong(item.getTimestamp());
        if (item instanceof FSDir) {
            FSDir dir = (FSDir) item;
            List<FSItem> children = dir.copyChildren();
            data.writeInt(children.size());
            for (FSItem child : children) {
                writeFSItem(data, child);
            }
        } else if (item instanceof FSFile) {
            FSFile file = (FSFile) item;
            int length = file.getLength();
            data.writeInt(length);
            byte[] content = file.getTempContent();
            boolean hasContent = content != null && content.length == length;
            data.writeBoolean(hasContent);
            if (hasContent) {
                data.write(content);
            }
        }
    }

    @NotNull
    public static FSItem readFSItem(@NotNull DataInput data, @Nullable FSDir parent) throws IOException {
        boolean isDir = data.readBoolean();
        String name = data.readUTF();
        long timestamp = data.readLong();
        if (isDir) {
            FSDir dir = new FSDir(parent, name, timestamp);
            int count = data.readInt();
            for (int i = 0; i < count; i++) {
                FSItem child = readFSItem(data, dir);
                dir.addChild(child);
            }
            return dir;
        } else {
            int length = data.readInt();
            boolean hasContent = data.readBoolean();
            byte[] content = null;
            if (hasContent) {
                content = new byte[length];
                data.readFully(content);
            }
            //noinspection DataFlowIssue
            return new FSFile(parent, name, timestamp, length, content);
        }
    }

    public static <T> T withAllowedWriting(@NotNull VirtualFile file, @NotNull java.util.function.Supplier<T> f) {
        setAllowWriting(file, true);
        try {
            return f.get();
        } finally {
            setAllowWriting(file, false);
        }
    }

    public static boolean isWritingAllowed(@NotNull VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(RUST_MACROS_ALLOW_WRITING));
    }

    private static void setAllowWriting(@NotNull VirtualFile file, boolean allow) {
        file.putUserData(RUST_MACROS_ALLOW_WRITING, allow ? Boolean.TRUE : null);
    }

    private static long currentTimestamp() {
        return System.currentTimeMillis();
    }
}
