package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.*;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public class PreviewProjectComponent implements ProjectComponent {

    private Project myProject;

    private boolean initialized = false;

    private PreviewVirtualFile previewFile;

    private FileEditorManager fileEditorManager;

    public PreviewProjectComponent(Project project) {
        myProject = project;
    }

    public void initializeProjectViewHandler() {
        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();

        if (viewPane == null) {
            return;
        }

        initialized = true;

        fileEditorManager = FileEditorManager.getInstance(myProject);
        viewPane.getTree().addTreeSelectionListener(treeSelectionEvent -> {
            closePreview();

            Object userObject = ((DefaultMutableTreeNode)treeSelectionEvent.getPath().getLastPathComponent()).getUserObject();
            if (!(userObject instanceof ProjectViewNode)) {
                return;
            }
            ProjectViewNode projectViewNode = (ProjectViewNode)userObject;
            VirtualFile currentFile = projectViewNode.getVirtualFile();
            if (currentFile.isDirectory() || !currentFile.isValid()) {
                return;
            }

            if (!fileEditorManager.isFileOpen(currentFile)) {
                currentFile = openPreview(currentFile);
            }
            fileEditorManager.openFile(currentFile, false);
        });
    }

    public void closePreview() {
        if (previewFile != null) {
            fileEditorManager.closeFile(previewFile);
            previewFile = null;
        }
    }

    public VirtualFile openPreview(VirtualFile file) {
        previewFile = new PreviewVirtualFile(file);
        return previewFile;
    }

    @Override
    public void initComponent() {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = messageBus.connect();
        connection.subscribe(CommandListener.TOPIC, new CommandListener() {
            @Override
            public void beforeCommandFinished(@NotNull CommandEvent event) {
                if (!initialized) {
                    initializeProjectViewHandler();
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
