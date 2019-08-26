package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;
import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.PREVIEW_BY_DEFAULT;

public class PreviewProjectHandler {

    public static final Key<String> PREVIEW_VIRTUAL_FILE_KEY = Key.create(PreviewProjectHandler.class.getName());

    private Project myProject;
    private AbstractProjectViewPane myProjectViewPane;
    private final KeyListener myTreeKeyListener;

    private final PropertyChangeListener mySettingsPropertyChangeListener = evt -> {
        switch (evt.getPropertyName()) {
            case "ProjectViewToggleOneClick":
                myProjectViewPane.getTree().setToggleClickCount((boolean) evt.getNewValue() ? 1 : 2);
                break;
            case "QuickNavigationKeyListenerEnabled":
                myProjectViewPane.getTree().setFocusTraversalKeysEnabled(!(boolean) evt.getNewValue());
                break;
            default:
                // nothing to do yet
                break;
        }
    };

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        openOrFocusSelectedFile((Component)treeSelectionEvent.getSource());
    };

    private final MouseListener myTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            super.mouseClicked(mouseEvent);

            if (mouseEvent.getClickCount() == 1 && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                openOrFocusSelectedFile((Component)mouseEvent.getSource());
            } else if (mouseEvent.getClickCount() == 2 && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> PreviewUtil.disposePreview(myProject, selectedFile));
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (PreviewSettings.getInstance().isProjectViewFocusSupport()) {
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (PreviewSettings.getInstance().isProjectViewFocusSupport()) {
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW) && !PreviewUtil.isPreviewed(event.getNewFile())) {
                consumeSelectedFile(myProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || !PreviewUtil.isPreviewed(file)) {
                        closeAllPreviews();
                    }
                });
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> {
                if (PreviewUtil.isPreviewed(file) ||
                        (selectedFile != null && selectedFile.equals(file) && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW))) {
                    closeOtherPreviews(file);
                }
            });
        }

        @Override
        public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            invokeSafe(() -> PreviewUtil.disposePreview(myProject, file));
        }
    };

    public static final PreviewProjectHandler createIfPossible(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert !project.isDisposed() : "project should not be disposed";

        PreviewProjectHandler previewProjectHandler = new PreviewProjectHandler();
        if (previewProjectHandler.init(project, messageBusConnection)) {
            return previewProjectHandler;
        }
        return null;
    }

    protected PreviewProjectHandler() {
        myTreeKeyListener = new PreviewKeyListener(this);
    }

    protected boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (myProjectViewPane == null) {
            return false;
        }

        myProject = project;

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.addPropertyChangeListener(mySettingsPropertyChangeListener);

        myProjectViewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        myProjectViewPane.getTree().addKeyListener(myTreeKeyListener);
        myProjectViewPane.getTree().addMouseListener(myTreeMouseListener);

        // 'false' required to support TAB key in listener - be aware of side effects...
        myProjectViewPane.getTree().setFocusTraversalKeysEnabled(!previewSettings.isQuickNavigationKeyListenerEnabled());
        myProjectViewPane.getTree().setToggleClickCount(previewSettings.isProjectViewToggleOneClick() ? 1 : 2);

        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);

        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        closeAllPreviews();

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        if (myProjectViewPane != null) {
            myProjectViewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
            myProjectViewPane.getTree().removeKeyListener(myTreeKeyListener);
            myProjectViewPane.getTree().removeMouseListener(myTreeMouseListener);
        }

        myProject = null;
    }

    public void openOrFocusSelectedFile(final Component component) {
        if (!IdeFocusManager.getInstance(myProject).getFocusOwner().equals(component)) {
            IdeFocusManager.getInstance(myProject).requestFocus(component, true);
            return;
        }

        switch (PreviewSettings.getInstance().getPreviewBehavior()) {
            case PREVIEW_BY_DEFAULT:
                consumeSelectedFile(component, file -> {
                    openPreviewOrEditor(file);
                });
                break;
            case EXPLICIT_PREVIEW:
                consumeSelectedFile(component, file -> {
                    focusFileEditor(file, false);
                });
                break;
            default:
                throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
        }
    }

    public void openPreviewOrEditor(VirtualFile file) {
        if (!isValid() || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                closeAllPreviews();
            }
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        if (!fileEditorManager.isFileOpen(file)) {
            PreviewUtil.preparePreview(myProject, file);
        }
        invokeSafeAndWait(() -> fileEditorManager.openFile(file, false));
    }

    public void consumeSelectedFile(final Component tree, Consumer<VirtualFile> consumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        getReady(dataContext).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            DataContext context = DataManager.getInstance().getDataContext(tree);
            final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
            consumer.accept(file);
        }));
    }

    private ActionCallback getReady(DataContext context) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(this) : ActionCallback.DONE;
    }

    public void closeCurrentFileEditor() {
        if (!isValid()) {
            return;
        }

        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(myProject);
        closeFileEditor(fileEditorManager.getCurrentFile());
    }

    public void closeFileEditor(VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(myProject);
        invokeSafeAndWait(() -> fileEditorManager.closeFile(file));
    }

    public void closeAllPreviews() {
        closeOtherPreviews(null);
    }

    public void closeOtherPreviews(@Nullable final VirtualFile currentPreview) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        for (VirtualFile file : fileEditorManager.getOpenFiles()) {
            if (PreviewUtil.isPreviewed(file) && !file.equals(currentPreview)) {
                closeFileEditor(file);
            }
        }
    }

    public void focusFileEditor(VirtualFile file, boolean focusEditor) {
        if (!isValid() || file == null) {
            return;
        }
        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(myProject);
        if (!fileEditorManager.isFileOpen(file)) {
            return;
        }
        invokeSafe(() -> fileEditorManager.openFile(file, focusEditor));
    }

    public void invokeSafeAndWait(Runnable runnable) {
        PreviewUtil.invokeSafeAndWait(myProject, runnable);
    }

    public void invokeSafe(Runnable runnable) {
        PreviewUtil.invokeSafe(myProject, runnable);
    }

    public boolean isValid() {
        return PreviewUtil.isValid(myProject);
    }

}
