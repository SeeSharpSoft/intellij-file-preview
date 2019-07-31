package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import net.seesharpsoft.common.io.DummyOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PreviewVirtualFile extends VirtualFile {
    public static final String NAME_PREFIX = "<<";
    public static final String NAME_SUFFIX = ">>";

    private VirtualFile mySource;

    public PreviewVirtualFile(VirtualFile source) {
        this.mySource = source;
    }

    public VirtualFile getSource() {
        return mySource;
    }

    @NotNull
    @Override
    public String getName() {
        return String.format("%s%s%s", NAME_PREFIX, getSource().getName(), NAME_SUFFIX);
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return getSource().getFileSystem();
    }

    @NotNull
    @Override
    public String getPath() {
        return String.format("%s - Just a preview of '%s'", PreviewFileType.INSTANCE.getDefaultExtension(), getSource().getName());
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return mySource.getParent();
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return DummyOutputStream.DUMMY_OUTPUT_STREAM;
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        return getSource().getTimeStamp();
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
        getSource().refresh(asynchronous, recursive, postRunnable);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public long getModificationStamp() {
        return getSource().getModificationStamp();
    }

    @NotNull
    public FileType getFileType() {
        return PreviewFileType.INSTANCE;
    }
}
