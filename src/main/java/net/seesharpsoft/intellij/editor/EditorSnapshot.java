package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import org.jetbrains.annotations.NotNull;

public abstract class EditorSnapshot<T extends FileEditor> {

    private Class<? extends FileEditor> fileEditorClass;

    protected EditorSnapshot(FileEditor fileEditor) {
        fileEditorClass = fileEditor.getClass();
    }

    public abstract boolean apply(@NotNull T fileEditor);

    public boolean accepts(@NotNull T fileEditor) {
        return fileEditorClass.isAssignableFrom(fileEditor.getClass());
    }
}
