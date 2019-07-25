package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.impl.ProjectViewPaneSelectionHelper;
import com.intellij.ide.projectView.impl.ProjectViewToolWindowFactory;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.List;

public class PreviewFileSelectionHelper extends ProjectViewPaneSelectionHelper {
    @Nullable
    @Override
    protected List<? extends TreePath> computeAdjustedPaths(@NotNull SelectionDescriptor selectionDescriptor) {
        System.out.println(selectionDescriptor.targetVirtualFile);
        return null;
    }
}
