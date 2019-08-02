package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TextEditorStateSynchronizer<T extends TextEditor> extends EditorStateSynchronizer<T> {

    private List<CaretState> myCaretsAndSelections;
    private int myRelativeCaretPosition;

    @Override
    public void init(@NotNull T fileEditor) {
        super.init(fileEditor);
        myCaretsAndSelections = new ArrayList<>(fileEditor.getEditor().getCaretModel().getCaretsAndSelections());
        myRelativeCaretPosition = EditorUtil.calcRelativeCaretPosition(fileEditor.getEditor());
    }

    @Override
    public boolean applyState(@NotNull T fileEditor) {
        final Editor editor = fileEditor.getEditor();
        editor.getCaretModel().setCaretsAndSelections(myCaretsAndSelections, false);
        Runnable scrollingRunnable = () -> {
            if (!editor.isDisposed()) {
                editor.getScrollingModel().disableAnimation();
                if (myRelativeCaretPosition != Integer.MAX_VALUE) {
                    EditorUtil.setRelativeCaretPosition(editor, myRelativeCaretPosition);
                }
                // if (!exactState) editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                // editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                editor.getScrollingModel().enableAnimation();
            }
        };

        // scrollingRunnable.run();
        //
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            scrollingRunnable.run();
        } else {
            UiNotifyConnector.doWhenFirstShown(editor.getContentComponent(), scrollingRunnable);
        }
        return true;
    }
}
