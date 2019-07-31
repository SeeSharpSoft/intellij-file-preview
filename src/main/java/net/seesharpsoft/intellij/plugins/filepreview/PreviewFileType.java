package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PreviewFileType implements FileType {

    public static final PreviewFileType INSTANCE = new PreviewFileType();

    public static final Icon FILE = IconLoader.getIcon("/media/icons/fileTypeIcon.png");

    @NotNull
    @Override
    public String getName() {
        return "Quick File Preview";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Quick File Preview";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "@_'-'";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return FILE;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return null;
    }
}
