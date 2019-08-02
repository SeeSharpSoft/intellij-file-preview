package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import org.jetbrains.annotations.NotNull;

public abstract class EditorStateSynchronizer<T extends FileEditor> {

    private Class<? extends FileEditor> fileEditorClass;

    public void init(@NotNull T fileEditor) {
        fileEditorClass = fileEditor.getClass();
    }

    public abstract boolean applyState(@NotNull T fileEditor);

    public boolean accepts(@NotNull T fileEditor) {
        return fileEditorClass.isAssignableFrom(fileEditor.getClass());
    }
}
