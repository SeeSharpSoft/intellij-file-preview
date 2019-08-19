package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class PreviewUtil {
    private PreviewUtil() {
        // util
    }

    public static final Key<DocumentListener> PREVIEW_DOCUMENT_LISTENER = Key.create(PreviewUtil.class.getName() + "$PREVIEW_DOCUMENT_LISTENER_INSTANCE");

    private static final DocumentListener PREVIEW_DOCUMENT_LISTENER_INSTANCE = new DocumentListener() {
        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
            final VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
            disposePreviewFile(file);
            // invokeSafe(() -> openFile(file, true));
        }
    };

    public static boolean isPreviewFile(VirtualFile file) {
        return file != null && file.getUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY) != null;
    }

    public static void disposePreviewFile(final VirtualFile file) {
        if (file == null) {
            return;
        }

        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, null);
        DocumentListener listener = file.getUserData(PREVIEW_DOCUMENT_LISTENER);
        file.putUserData(PREVIEW_DOCUMENT_LISTENER, null);
        if (listener == null) {
            return;
        }
        FileDocumentManager.getInstance().getDocument(file).removeDocumentListener(listener);
    }

    public static void preparePreviewFile(final VirtualFile file) {
        if (file == null) {
            return;
        }

        PreviewUtil.disposePreviewFile(file);
        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, file.getName());

        if (!PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            document.addDocumentListener(PREVIEW_DOCUMENT_LISTENER_INSTANCE);
            file.putUserData(PREVIEW_DOCUMENT_LISTENER, PREVIEW_DOCUMENT_LISTENER_INSTANCE);
        }
    }

    public static void invokeSafeAndWait(Project project, Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (isValid(project)) {
                runnable.run();
            }
        });
    }

    public static void invokeSafe(Project project, Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isValid(project)) {
                runnable.run();
            }
        });
    }

    public static boolean isValid(Project project) {
        return project != null && !project.isDisposed();
    }
}
