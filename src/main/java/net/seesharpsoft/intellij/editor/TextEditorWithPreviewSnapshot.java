package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import org.jetbrains.annotations.NotNull;

public class TextEditorWithPreviewSnapshot extends EditorSnapshot<TextEditorWithPreview> {

    private TextEditorSnapshot myTextEditorSnapshot;

    public TextEditorWithPreviewSnapshot(@NotNull TextEditorWithPreview fileEditor) {
        super(fileEditor);
        myTextEditorSnapshot = new TextEditorSnapshot(getTextEditor(fileEditor));
    }

    @Override
    public boolean apply(@NotNull TextEditorWithPreview fileEditor) {
        return myTextEditorSnapshot.apply(getTextEditor(fileEditor));
    }

    protected TextEditor getTextEditor(@NotNull TextEditorWithPreview textEditorWithPreview) {
        return (TextEditor)textEditorWithPreview.getCurrentLocation().getEditor();
    }
}
