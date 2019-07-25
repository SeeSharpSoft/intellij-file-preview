package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public class PreviewProjectComponent implements ProjectComponent {

    private Project myProject;

    private boolean initialized = false;

    private boolean previewing = false;
    private VirtualFile previewFile;

    private FileEditorManagerImpl fileEditorManager;

    public PreviewProjectComponent(Project project) {
        myProject = project;
    }

    public void projectOpened2() {
        AbstractProjectViewPane viewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();

        if (viewPane == null) {
            return;
        }

        initialized = true;

        fileEditorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
        fileEditorManager.addFileEditorManagerListener(new FileEditorManagerListener() {
            @Override
            public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file,
                                       @NotNull Pair<FileEditor[], FileEditorProvider[]> editors) {
                if (previewing) {
                    previewing = false;
                    return;
                }
                if (file == previewFile) {
                    resetPreview();
                }
            }

            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                if (event.getOldFile() == previewFile) {
                    closePreviewEditor();
                }
            }
        });

        viewPane.getTree().addTreeSelectionListener(treeSelectionEvent -> {
            Object userObject = ((DefaultMutableTreeNode)treeSelectionEvent.getPath().getLastPathComponent()).getUserObject();
            if (!(userObject instanceof PsiFileNode)) {
                return;
            }

            PsiFileNode psiFileNode = (PsiFileNode)userObject;

            closePreviewEditor();

            VirtualFile currentFile = psiFileNode.getVirtualFile();
            if (!fileEditorManager.isFileOpen(currentFile)) {
                setPreview(currentFile);
            }
            fileEditorManager.openFile(currentFile, true);

        });
    }

    public void closePreviewEditor() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        if (previewFile != null) {
            fileEditorManager.closeFile(previewFile);
        }
        resetPreview();
    }

    public void setPreview(VirtualFile file) {
        previewFile = file;
        previewing = true;
    }

    public void resetPreview() {
        previewFile = null;
        previewing = false;
    }

    @Override
    public void initComponent() {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = messageBus.connect();
        connection.subscribe(CommandListener.TOPIC, new CommandListener() {
            @Override
            public void beforeCommandFinished(@NotNull CommandEvent event) {
                if (!initialized) {
                    projectOpened2();
                }
            }
        });

    }
}
