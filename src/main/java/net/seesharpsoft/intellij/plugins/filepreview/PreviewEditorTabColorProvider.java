package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class PreviewEditorTabColorProvider implements EditorTabColorProvider {
    @Nullable
    @Override
    public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
        if (PreviewUtil.isPreviewed(file)) {
            return PreviewSettings.getInstance().getPreviewTabColor();
        }
        return null;
    }
}
