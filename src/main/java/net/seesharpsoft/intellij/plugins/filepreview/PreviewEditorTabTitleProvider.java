package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviewEditorTabTitleProvider implements EditorTabTitleProvider {

    public static final String NAME_PREFIX = "<<";
    public static final String NAME_SUFFIX = ">>";

    @Nullable
    @Override
    public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        String previewVirtualFile = file.getUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY);
        if (previewVirtualFile != null) {
            return String.format("%s%s%s", NAME_PREFIX, file.getName(), NAME_SUFFIX);
        }
        return null;
    }
}
