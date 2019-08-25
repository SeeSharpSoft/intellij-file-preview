package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class PreviewUtil {
    private PreviewUtil() {
        // util
    }

    public static final Key<DocumentListener> PREVIEW_DOCUMENT_LISTENER = Key.create(PreviewUtil.class.getName() + "$PREVIEW_DOCUMENT_LISTENER_INSTANCE");

    public static boolean isPreviewed(VirtualFile file) {
        return file != null && file.getUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY) != null;
    }

    public static void disposePreview(@NotNull final Project project, final VirtualFile file) {
        disposePreview(project, file, true);
    }

    public static void disposePreview(@NotNull final Project project, final VirtualFile file, final boolean updateRepresentation) {
        if (!isValid(project) || file == null) {
            return;
        }

        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, null);
        final DocumentListener documentListener = file.getUserData(PREVIEW_DOCUMENT_LISTENER);
        file.putUserData(PREVIEW_DOCUMENT_LISTENER, null);
        if (documentListener != null) {
            FileDocumentManager.getInstance().getDocument(file).removeDocumentListener(documentListener);
        }
        if (updateRepresentation) {
            FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file);
        }
    }

    public static void preparePreview(@NotNull final Project project, final VirtualFile file) {
        if (isValid(project) && (file == null || isPreviewed(file))) {
            return;
        }

        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, file.getName());

        if (PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                DocumentListener documentListener = new PreviewDocumentListener(project);
                document.addDocumentListener(documentListener);
                file.putUserData(PREVIEW_DOCUMENT_LISTENER, documentListener);
            }
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
