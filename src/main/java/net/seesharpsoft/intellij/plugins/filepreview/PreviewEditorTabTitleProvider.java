package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviewEditorTabTitleProvider implements EditorTabTitleProvider {
    @Nullable
    @Override
    public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        String title = null;
        if (PreviewUtil.isPreviewed(file)) {
            title = String.format(PreviewSettings.getInstance().getPreviewTabTitlePattern(), file.getPresentableName());
        }
        return title == null || title.isEmpty() ? null : title;
    }
}
