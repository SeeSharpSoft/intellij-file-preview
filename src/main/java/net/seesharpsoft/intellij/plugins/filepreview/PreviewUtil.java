package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;

import java.awt.*;
import java.util.function.Consumer;

public final class PreviewUtil {
    private PreviewUtil() {
        // util
    }

    enum PreviewState {
        UNDEFINED,
        PREVIEWED,
        OPENED
    }

    public static final Key<PreviewState> PREVIEW_VIRTUAL_FILE_KEY = Key.create(PreviewProjectHandler.class.getName());
    public static final Key<DocumentListener> PREVIEW_DOCUMENT_LISTENER = Key.create(PreviewUtil.class.getName() + "$PREVIEW_DOCUMENT_LISTENER_INSTANCE");

    public static boolean isPreviewed(final VirtualFile file) {
        return file != null && file.getUserData(PREVIEW_VIRTUAL_FILE_KEY) == PreviewState.PREVIEWED;
    }

    public static boolean isOpened(VirtualFile file) {
        return file != null && file.getUserData(PREVIEW_VIRTUAL_FILE_KEY) == PreviewState.OPENED;
    }

    public static void setStateUndefined(@NotNull final Project project, final VirtualFile file) {
        setStateOpened(project, file);
        file.putUserData(PREVIEW_VIRTUAL_FILE_KEY, PreviewState.UNDEFINED);
    }

    public static void setStateOpened(@NotNull final Project project, final VirtualFile file) {
        setStateOpened(project, file, true);
    }

    public static void setStateOpened(@NotNull final Project project, final VirtualFile file, final boolean updateRepresentation) {
        if (!isValid(project) || (file == null || isOpened(file))) {
            return;
        }

        file.putUserData(PREVIEW_VIRTUAL_FILE_KEY, PreviewState.OPENED);

        final Document document = FileDocumentManager.getInstance().getDocument(file);
        final DocumentListener documentListener = document == null ? null : document.getUserData(PREVIEW_DOCUMENT_LISTENER);
        if (documentListener != null) {
            document.putUserData(PREVIEW_DOCUMENT_LISTENER, null);
            document.removeDocumentListener(documentListener);
        }

        if (updateRepresentation) {
            FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file);
        }
    }

    public static void setStatePreviewed(@NotNull final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null || isPreviewed(file) || isOpened(file)) {
            return;
        }

        file.putUserData(PREVIEW_VIRTUAL_FILE_KEY, PreviewState.PREVIEWED);

        if (PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                DocumentListener documentListener = new PreviewDocumentListener(project);
                document.addDocumentListener(documentListener);
                document.putUserData(PREVIEW_DOCUMENT_LISTENER, documentListener);
            }
        }
    }

    public static void setNextState(@NotNull final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null || isOpened(file)) {
            return;
        }

        if (isPreviewed(file)) {
            setStateOpened(project, file);
        } else {
            setStatePreviewed(project, file);
        }
    }

    public static VirtualFile getGotoFile(final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null) {
            return null;
        }

        PsiElement element = PsiManager.getInstance(project).findFile(file);
        if (element != null) {
            PsiElement navElement = element.getNavigationElement();
            navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(element, navElement);
            if (navElement != null && navElement.getContainingFile() != null) {
                return navElement.getContainingFile().getVirtualFile();
            }
        }
        return file;
    }

    public static void invokeSafeAndWait(final Project project, final Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (isValid(project)) {
                runnable.run();
            }
        });
    }

    public static void invokeSafe(final Project project, final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isValid(project)) {
                runnable.run();
            }
        });
    }

    public static boolean isValid(Project project) {
        return project != null && !project.isDisposed();
    }

    public static void consumeSelectedFile(final Component tree, Consumer<VirtualFile> consumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        getReady(dataContext, consumer).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            DataContext context = DataManager.getInstance().getDataContext(tree);
            final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
            consumer.accept(file);
        }));
    }

    private static ActionCallback getReady(DataContext context, Object requester) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(requester) : ActionCallback.DONE;
    }
}
