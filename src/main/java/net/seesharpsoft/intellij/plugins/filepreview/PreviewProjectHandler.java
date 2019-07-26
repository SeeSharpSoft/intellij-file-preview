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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import java.awt.*;

public class PreviewProjectHandler {

    private Project myProject;
    private PreviewVirtualFile myPreviewFile;

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        closePreview();
        ApplicationManager.getApplication().invokeLater(() -> openPreviewOrEditor((Component)treeSelectionEvent.getSource()));
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file,
                                   @NotNull Pair<FileEditor[], FileEditorProvider[]> editors) {
            if (!(file instanceof PreviewVirtualFile)) {
                closePreview();
            }
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!(event.getNewProvider() instanceof PreviewEditorProvider)) {
                closePreview();
            }
        }
    };

    public static final PreviewProjectHandler createIfPossible(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        PreviewProjectHandler previewProjectHandler = new PreviewProjectHandler();
        if (previewProjectHandler.init(project, messageBusConnection)) {
            return previewProjectHandler;
        }
        return null;
    }

    public boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProject = project;

        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        if (viewPane == null) {
            return false;
        }

        viewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        closePreview();

        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        if (viewPane != null) {
            viewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);
        }

        myProject = null;
    }

    public void openPreviewOrEditor(final Component tree) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        getReady(dataContext).doWhenDone(() -> TransactionGuard.submitTransaction(ApplicationManager.getApplication(), () -> {
            DataContext context = DataManager.getInstance().getDataContext(tree);
            VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
            if (file == null || file.isDirectory() || !file.isValid()) {
                return;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            if (!fileEditorManager.isFileOpen(file)) {
                file = createAndSetPreviewFile(file);
            }
            if (file != null) {
                fileEditorManager.openFile(file, false);
            }
        }));
    }

    private ActionCallback getReady(DataContext context) {
        ToolWindow toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context);
        return toolWindow != null ? toolWindow.getReady(this) : ActionCallback.DONE;
    }

    public void closePreview() {
        if (myPreviewFile != null) {
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
            fileEditorManager.closeFile(myPreviewFile, false, true);
            myPreviewFile = null;
        }
    }

    public VirtualFile createAndSetPreviewFile(VirtualFile file) {
        if (myPreviewFile == null) {
            myPreviewFile = new PreviewVirtualFile(file);
            return myPreviewFile;
        }
        return null;
    }
}
