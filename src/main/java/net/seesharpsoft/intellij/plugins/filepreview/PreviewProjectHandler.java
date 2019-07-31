package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public class PreviewProjectHandler {

    private Project myProject;
    private PreviewVirtualFile myPreviewFile;
    private AbstractProjectViewPane myProjectViewPane;

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        openPreviewOrEditor();
    };

    private final KeyListener myKeyListener = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
            switch (e.getKeyCode()) {
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    consumeSelectedFile((Component) e.getSource(), file -> {
                        closeFileEditor(file);
                    });
                    break;
                case KeyEvent.VK_SPACE:
                    openPreviewOrEditor();
                    break;
                case KeyEvent.VK_TAB:
                    consumeSelectedFile((Component) e.getSource(), file -> {
                        focusFile(file);
                    });
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
            }
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (file instanceof PreviewVirtualFile) {
                ApplicationManager.getApplication().invokeLater(() -> myProjectViewPane.getTree().grabFocus());
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            ApplicationManager.getApplication().invokeLater(() -> myProjectViewPane.getTree().grabFocus());
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!(event.getNewProvider() instanceof PreviewEditorProvider)) {
                closePreview();
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (!(file instanceof PreviewVirtualFile) || !file.equals(myPreviewFile)) {
                closePreview();
                if (file instanceof PreviewVirtualFile) {
                    myPreviewFile = (PreviewVirtualFile) file;
                }
            }
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

    public boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProject = project;

        myProjectViewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        if (myProjectViewPane == null) {
            return false;
        }

        myProjectViewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        if (PreviewSettings.getInstance().isQuickNavigationKeyListenerEnabled()) {
            myProjectViewPane.getTree().setFocusTraversalKeysEnabled(false);
            myProjectViewPane.getTree().addKeyListener(myKeyListener);
        }
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);
        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        closePreview();

        if (myProjectViewPane != null) {
            myProjectViewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
            myProjectViewPane.getTree().removeKeyListener(myKeyListener);
        }

        myProject = null;
    }

    public void openPreviewOrEditor() {
        final Component tree = myProjectViewPane.getTree();
        consumeSelectedFile(tree, file -> {
            if (file == null || file.isDirectory() || !file.isValid()) {
                if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                    closePreview();
                }
                return;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            final VirtualFile fileToOpen = !fileEditorManager.isFileOpen(file) ? createAndSetPreviewFile(file) : file;
            if (fileToOpen != null) {
                ApplicationManager.getApplication().invokeLater(() -> fileEditorManager.openFile(fileToOpen, false));
            }
        });
    }

    protected void consumeSelectedFile(final Component tree, Consumer<VirtualFile> consumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        getReady(dataContext).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            DataContext context = DataManager.getInstance().getDataContext(tree);
            VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
            consumer.accept(file);
        }));
    }

    private ActionCallback getReady(DataContext context) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(this) : ActionCallback.DONE;
    }

    public void closeFileEditor(VirtualFile file) {
        if (file == null) {
            return;
        }
        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            closePreview();
        } else {
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
            ApplicationManager.getApplication().invokeLater(() -> fileEditorManager.closeFile(file, false, true));
        }
    }

    public void closePreview() {
        if (myPreviewFile != null && !myProject.isDisposed()) {
            final VirtualFile closingPreviewFile = myPreviewFile;
            myPreviewFile = null;
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
            ApplicationManager.getApplication().invokeLater(() -> fileEditorManager.closeFile(closingPreviewFile, false, true));
        }
    }

    public void focusFile(VirtualFile file) {
        final VirtualFile fileToFocus = myPreviewFile != null && myPreviewFile.getSource().equals(file) ? myPreviewFile : file;
        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        if (fileToFocus == null || !fileEditorManager.isFileOpen(fileToFocus)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> fileEditorManager.openFile(fileToFocus, true));
    }

    public VirtualFile createAndSetPreviewFile(VirtualFile file) {
        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            return myPreviewFile;
        }
        return new PreviewVirtualFile(file);
    }
}
