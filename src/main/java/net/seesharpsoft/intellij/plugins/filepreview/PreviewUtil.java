package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.Consumer;

public final class PreviewUtil {
    private PreviewUtil() {
        // util
    }

    public static final Key<DocumentListener> PREVIEW_DOCUMENT_LISTENER = Key.create(PreviewUtil.class.getName() + "$PREVIEW_DOCUMENT_LISTENER_INSTANCE");
    // marking of files that are opened/closed due to interactions that requires further handling
    public static final Key<Boolean> REQUIRES_PREVIEW_HANDLING = Key.create(PreviewUtil.class.getName() + "$HANDLED_BY_PREVIEW");
    public static final Key<Boolean> SOURCE_WINDOW_IS_AUTO_HIDE = Key.create(PreviewUtil.class.getName() + "$SOURCE_WINDOW_IS_AUTO_HIDE");

    public static boolean isPreviewed(final VirtualFile file) {
        return file != null && file.getUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY) != null;
    }

    public static void disposePreview(final Project project, final VirtualFile file) {
        disposePreview(project, file, true);
    }

    public static void disposePreview(final Project project, final VirtualFile file, final boolean updateRepresentation) {
        if (!isValid(project) || file == null || !isPreviewed(file)) {
            return;
        }

        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, null);

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

    public static void preparePreview(final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null || isPreviewed(file)) {
            return;
        }

        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, file.getName());

        if (PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                DocumentListener documentListener = new PreviewDocumentListener(project);
                document.addDocumentListener(documentListener);
                document.putUserData(PREVIEW_DOCUMENT_LISTENER, documentListener);
            }
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

    public static VirtualFile getFileFromDataContext(@NotNull final DataContext dataContext) {
        return CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    }

    public static boolean isEditorSelected(@NotNull final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null || file.isDirectory() || !file.isValid()) {
            return false;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
        FileEditor selectedFileEditor = fileEditorManager.getSelectedEditor(file);
        return selectedEditor != null && selectedEditor.equals(selectedFileEditor);
    }

    public static boolean isProjectTreeFocused(@NotNull final Project project) {
        return getCurrentProjectViewPane(project).getTree().hasFocus();
    }

    private static void openPreviewOrEditor(@NotNull final Project project, final VirtualFile file, final boolean requestFocus) {
        if (!isValid(project) || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection() && !isAutoScrollFromSource(project)) {
                closeOtherPreviews(project, file);
            }
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final boolean requireFocus;
        if (!fileEditorManager.isFileOpen(file)) {
            PreviewUtil.preparePreview(project, file);
            requireFocus = true;
        } else {
            requireFocus = !isEditorSelected(project, file);
        }

        invokeSafeAndWait(project, () -> {
            fileEditorManager.openFile(file, requestFocus);
            if (requireFocus) {
                focusProjectView(project);
            }
        });
    }

    public static synchronized void openPreviewOrEditor(@NotNull final Project project, final Component component, final boolean requestFocus) {
        consumeDataContext(component, dataContext -> {
            openPreviewOrEditor(project, getGotoFile(project, getFileFromDataContext(dataContext)), requestFocus);
        });
    }

    public static void openPreviewOrEditor(@NotNull final Project project, final Component component) {
        openPreviewOrEditor(project, component, false);
    }

    public static synchronized void closeFileEditor(final Project project, final VirtualFile file) {
        if (!isValid(project) || file == null) {
            return;
        }
        final ToolWindow projectViewToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (projectViewToolWindow == null) {
            return;
        }
        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        assert fileEditorManager.isFileOpen(file);
        invokeSafeAndWait(project, () -> {
            // Project panel auto-hides after file selecting #50
            file.putUserData(SOURCE_WINDOW_IS_AUTO_HIDE, projectViewToolWindow.isAutoHide());
            projectViewToolWindow.setAutoHide(false);
            fileEditorManager.closeFile(file);
        });
    }

    public static void closeAllPreviews(@NotNull final Project project) {
        closeOtherPreviews(project, null);
    }

    public static void closeOtherPreviews(@NotNull final Project project, @Nullable final VirtualFile currentPreview) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (VirtualFile file : fileEditorManager.getOpenFiles()) {
            if (isPreviewed(file) && !file.equals(currentPreview)) {
                closeFileEditor(project, file);
            }
        }
    }

    public static void consumeDataContext(final Component component, final Consumer<DataContext> dataContextConsumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(component);
        getReady(dataContext, dataContextConsumer).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            dataContextConsumer.accept(DataManager.getInstance().getDataContext(component));
        }));
    }

    public static void consumeSelectedFile(final Component tree, Consumer<VirtualFile> consumer) {
        consumeDataContext(tree, context -> consumer.accept(getFileFromDataContext(context)));
    }

    private static ActionCallback getReady(DataContext context, Object requester) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(requester) : ActionCallback.DONE;
    }

    public static boolean isValid(Project project) {
        return project != null && !project.isDisposed();
    }

    public static void cleanupClosedFile(final Project project, @NotNull final VirtualFile file) {
        // Project panel auto-hides after file selecting #50
        if (file.getUserData(SOURCE_WINDOW_IS_AUTO_HIDE) != null && file.getUserData(SOURCE_WINDOW_IS_AUTO_HIDE)) {
            final ToolWindow projectViewToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
            if (projectViewToolWindow != null) {
                projectViewToolWindow.setAutoHide(true);
            }
        }
        file.putUserData(SOURCE_WINDOW_IS_AUTO_HIDE, null);

        if (file.getUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING) == null) {
            return;
        }
        unmarkPreviewHandling(file);
        focusProjectView(project);
    }

    public static void focusProjectView(final Project project) {
        AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane(project);
        if (currentProjectViewPane == null) {
            return;
        }
        PreviewUtil.invokeSafe(project, () -> currentProjectViewPane.getTree().grabFocus());
    }

    public static void toggleMarkPreviewHandling(VirtualFile file, Consumer<VirtualFile> onMark, Consumer<VirtualFile> onUnmark) {
        if (file != null) {
            if (file.getUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING) == null) {
                // if not marked, the selection change is not
                markPreviewHandling(file);
                if (onMark != null) {
                    onMark.accept(file);
                }
            } else {
                unmarkPreviewHandling(file);
                if (onUnmark != null) {
                    onUnmark.accept(file);
                }
            }
        }
    }

    public static void toggleMarkPreviewHandling(VirtualFile file) {
        toggleMarkPreviewHandling(file, null, null);
    }

    public static void markPreviewHandling(@NotNull VirtualFile file) {
        if (!file.isDirectory()) {
            file.putUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING, true);
        }
    }

    public static void unmarkPreviewHandling(@NotNull VirtualFile file) {
        if (!file.isDirectory()) {
            file.putUserData(PreviewUtil.REQUIRES_PREVIEW_HANDLING, null);
        }
    }

    public static ProjectView getProjectView(final Project project) {
        return ProjectView.getInstance(project);
    }

    public static AbstractProjectViewPane getCurrentProjectViewPane(final Project project) {
        return getProjectView(project).getCurrentProjectViewPane();
    }

    public static boolean isAutoScrollToSource(final Project project) {
        return getProjectView(project).isAutoscrollToSource(getCurrentProjectViewPane(project).getId());
    }

    public static boolean isAutoScrollFromSource(final Project project) {
        return getProjectView(project).isAutoscrollFromSource(getCurrentProjectViewPane(project).getId());
    }
}
