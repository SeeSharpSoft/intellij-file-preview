package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

public class PreviewStartupActivity implements StartupActivity {

    private Project myProject;
    private PreviewVirtualFile myPreviewFile;
    private boolean initialized = false;

    private final TreeSelectionListener myTreeSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
            closePreview();

            Object userObject = ((DefaultMutableTreeNode)treeSelectionEvent.getPath().getLastPathComponent()).getUserObject();
            if (!(userObject instanceof ProjectViewNode)) {
                return;
            }
            ProjectViewNode projectViewNode = (ProjectViewNode)userObject;
            VirtualFile currentFile = projectViewNode.getVirtualFile();
            if (currentFile == null || currentFile.isDirectory() || !currentFile.isValid()) {
                return;
            }

            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            if (!fileEditorManager.isFileOpen(currentFile)) {
                currentFile = getPreviewFile(currentFile);
            }
            ApplicationManager.getApplication().assertIsDispatchThread();
            fileEditorManager.openFile(currentFile, false);
        }
    };

    public void startup() {
        if (initialized) {
            return;
        }

        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        assert viewPane != null;

        viewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        initialized = true;
    }

    public void shutdown() {
        closePreview();

        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        assert viewPane != null;

        viewPane.getTree().removeTreeSelectionListener(myTreeSelectionListener);

        initialized = false;
    }

    public void closePreview() {
        if (myPreviewFile != null) {
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
            ApplicationManager.getApplication().assertIsDispatchThread();
            fileEditorManager.closeFile(myPreviewFile, false, true);
            myPreviewFile = null;
        }
    }

    public VirtualFile getPreviewFile(VirtualFile file) {
        assert myPreviewFile == null;

        myPreviewFile = new PreviewVirtualFile(file);
        return myPreviewFile;
    }

    @Override
    public void runActivity(@NotNull Project project) {
        myProject = project;

        startup();

        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = messageBus.connect();

        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowRegistered(@NotNull String id) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    startup();
                }
            }

            @Override
            public void toolWindowUnregistered(@NotNull String id, @NotNull ToolWindow toolWindow) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    shutdown();
                }
            }
        });

        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosingBeforeSave(@NotNull Project project) {
                if (myProject.equals(project)) {
                    shutdown();
                }
            }
        });

        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
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
        });
    }
}
