package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TextEditorSnapshot extends EditorSnapshot<TextEditor> {

    private List<CaretState> myCaretsAndSelections;
    private int myRelativeCaretPosition;

    public TextEditorSnapshot(@NotNull TextEditor fileEditor) {
        super(fileEditor);
        myCaretsAndSelections = new ArrayList<>(fileEditor.getEditor().getCaretModel().getCaretsAndSelections());
        myRelativeCaretPosition = EditorUtil.calcRelativeCaretPosition(fileEditor.getEditor());
    }

    @Override
    public boolean apply(@NotNull TextEditor fileEditor) {
        final Editor editor = fileEditor.getEditor();
        editor.getCaretModel().setCaretsAndSelections(myCaretsAndSelections, false);
        Runnable scrollingRunnable = () -> {
            if (!editor.isDisposed()) {
                editor.getScrollingModel().disableAnimation();
                if (myRelativeCaretPosition != Integer.MAX_VALUE) {
                    EditorUtil.setRelativeCaretPosition(editor, myRelativeCaretPosition);
                }
                editor.getScrollingModel().enableAnimation();
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            scrollingRunnable.run();
        } else {
            UiNotifyConnector.doWhenFirstShown(editor.getContentComponent(), scrollingRunnable);
        }
        return true;
    }
}
