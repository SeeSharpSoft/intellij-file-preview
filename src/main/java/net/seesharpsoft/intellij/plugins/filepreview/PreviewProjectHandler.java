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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;

public class PreviewProjectHandler {

    public static final Key<String> PREVIEW_VIRTUAL_FILE_KEY = Key.create(PreviewProjectHandler.class.getName());

    private Project myProject;
    private AbstractProjectViewPane myProjectViewPane;
    private final AnAction myReopenClosedTabAction;
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
        switch (PreviewSettings.getInstance().getPreviewBehavior()) {
            case PREVIEW_BY_DEFAULT:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    openPreviewOrEditor(file);
                });
                break;
            case EXPLICIT_PREVIEW:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    focusFileEditor(file, false);
                });
                break;
            default:
                throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
        }
    };

    private final MouseListener myTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);

            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> PreviewUtil.disposePreview(selectedFile));
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
            PreviewUtil.disposePreview(file);

            if (PreviewSettings.getInstance().isProjectViewFocusSupport()) {
                invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW)) {
                consumeSelectedFile(myProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || !PreviewUtil.isPreviewed(file)) {
                        closePreviews();
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
                    closePreviews();
                }
            });
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
        myReopenClosedTabAction = ActionManager.getInstance().getAction("ReopenClosedTab");
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

        closePreviews();

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        if (myProjectViewPane != null) {
            myProjectViewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
            myProjectViewPane.getTree().removeKeyListener(myTreeKeyListener);
            myProjectViewPane.getTree().removeMouseListener(myTreeMouseListener);
        }

        myProject = null;
    }

    public void openPreviewOrEditor(VirtualFile file) {
        if (!isValid() || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                closePreviews();
            }
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        if (!fileEditorManager.isFileOpen(file)) {
            PreviewUtil.preparePreview(file);
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

    public void closePreviews() {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        for (VirtualFile file : fileEditorManager.getOpenFiles()) {
            if (PreviewUtil.isPreviewed(file)) {
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
