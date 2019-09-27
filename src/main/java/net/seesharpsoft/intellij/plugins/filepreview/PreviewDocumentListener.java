package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class PreviewDocumentListener implements DocumentListener {
    protected final Project myProject;

    public PreviewDocumentListener(Project project) {
        myProject = project;
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        final VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
        PreviewUtil.setStateOpened(myProject, file);
    }
}
