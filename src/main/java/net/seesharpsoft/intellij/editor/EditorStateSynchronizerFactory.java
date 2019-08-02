package net.seesharpsoft.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class EditorStateSynchronizerFactory {

    private static final Map<Class, Class<? extends EditorStateSynchronizer>> SYNC_PROVIDER;

    static {
        SYNC_PROVIDER = new HashMap<>();
        SYNC_PROVIDER.put(TextEditor.class, TextEditorStateSynchronizer.class);
    }

    private static final EditorStateSynchronizerFactory INSTANCE = new EditorStateSynchronizerFactory();

    public static EditorStateSynchronizerFactory getInstance() {
        return INSTANCE;
    }

    private EditorStateSynchronizerFactory() {
        // Utility
    }

    public <T extends FileEditor> Class<? extends EditorStateSynchronizer> findSynchronizer(@NotNull T fileEditor) {
        Class targetClass = fileEditor.getClass();
        return SYNC_PROVIDER.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(targetClass))
                .map(entry -> entry.getValue())
                .findFirst().orElse(null);
    }

    public <T extends FileEditor> EditorStateSynchronizer<T> create(T fileEditor) {
        if (fileEditor == null) {
            return null;
        }

        Class<? extends EditorStateSynchronizer> syncClass = findSynchronizer(fileEditor);
        if (syncClass == null) {
            return null;
        }

        try {
            EditorStateSynchronizer synchronizer = syncClass.getDeclaredConstructor().newInstance();
            synchronizer.init(fileEditor);
            return synchronizer;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exc) {
            throw new RuntimeException(exc);
        }
    }
}
