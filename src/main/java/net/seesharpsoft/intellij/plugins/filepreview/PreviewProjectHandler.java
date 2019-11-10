package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class PreviewProjectHandler {

    private Project myProject;
    private AbstractProjectViewPane myProjectViewPane;

    private final List<JTree> registeredTrees = new ArrayList<>();

    private final KeyListener myTreeKeyListener;

    private final PropertyChangeListener mySettingsPropertyChangeListener = evt -> {
        switch (evt.getPropertyName()) {
            case "ProjectViewToggleOneClick":
                registeredTrees.forEach(tree -> tree.setToggleClickCount((boolean) evt.getNewValue() ? 1 : 2));
                break;
            default:
                // nothing to do yet
                break;
        }
    };

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        JTree tree = (JTree) treeSelectionEvent.getSource();
        PreviewUtil.openSource(myProject, tree);
//        openOrFocusSelectedFile((Component) treeSelectionEvent.getSource());
    };

    private final MouseListener myTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            super.mouseClicked(mouseEvent);

            switch (mouseEvent.getClickCount()) {
                case 1:
                    // one-click behavior is handled by myTreeSelectionListener
                    break;
                case 2:
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                        PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> PreviewUtil.setStateOpened(myProject, PreviewUtil.getGotoFile(myProject, selectedFile)));
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            focusProjectViewTreeIfNeeded();
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            focusProjectViewTreeIfNeeded();
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!PreviewUtil.isPreviewed(event.getNewFile())) {
                PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), file -> {
                    if (PreviewSettings.getInstance().isPreviewClosedOnTabChange() || !PreviewUtil.isPreviewed(file)) {
                        PreviewUtil.closeAllPreviews(myProject);
                    }
                });
            }
        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//            PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> {
//                if (PreviewUtil.isPreviewed(file) ||
//                        (selectedFile != null && selectedFile.equals(file))) {
//                    PreviewUtil.closeOtherPreviews(myProject, file);
//                }
//            });
        }

        @Override
        public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            invokeSafe(() -> PreviewUtil.setStateUndefined(myProject, file));
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
        myTreeKeyListener = new PreviewKeyListener(myProject);
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

        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);

        final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        for (VirtualFile virtualFile : fileEditorManager.getOpenFiles()) {
            PreviewUtil.setStateOpened(getProject(), virtualFile, false);
        }

        return true;
    }

    public void dispose() {
        assert myProject != null : "not initialized yet";

        PreviewUtil.closeAllPreviews(myProject);

        PreviewSettings previewSettings = PreviewSettings.getInstance();
        previewSettings.removePropertyChangeListener(mySettingsPropertyChangeListener);

        unregisterAllTreeHandlers();

        myProject = null;
    }

    public void registerTreeHandlers(@NotNull final JTree tree) {
        tree.setToggleClickCount(PreviewSettings.getInstance().isProjectViewToggleOneClick() ? 1 : 2);
        tree.addTreeSelectionListener(myTreeSelectionListener);
        tree.addKeyListener(myTreeKeyListener);
        tree.addMouseListener(myTreeMouseListener);
        registeredTrees.add(tree);
    }

    public void unregisterAllTreeHandlers() {
        for (JTree tree : new ArrayList<>(registeredTrees)) {
            unregisterTreeHandlers(tree);
        }
    }

    public void unregisterTreeHandlers(@NotNull final JTree tree) {
        if (!areTreeHandlersRegistered(tree)) {
            throw new UnsupportedOperationException("can not unregister unregistered tree");
        }

        registeredTrees.remove(tree);
        tree.removeTreeSelectionListener(myTreeSelectionListener);
        tree.removeKeyListener(myTreeKeyListener);
        tree.removeMouseListener(myTreeMouseListener);
    }

    public boolean areTreeHandlersRegistered(@NotNull final JTree tree) {
        return registeredTrees.contains(tree);
    }

    protected boolean shouldProjectViewTreeFocused() {
        ProjectView projectView = ProjectView.getInstance(myProject);
        return PreviewSettings.getInstance().isProjectViewFocusSupport() && !projectView.isAutoscrollFromSource(projectView.getCurrentViewId());
    }

    protected void focusProjectViewTreeIfNeeded() {
        if (shouldProjectViewTreeFocused()) {
            invokeSafe(() -> myProjectViewPane.getTree().grabFocus());
        }
    }

    protected void focusComponentIfSelectedFileIsNotOpen(final Component component) {
        PreviewUtil.consumeSelectedFile(component, selectedFile -> {
            if (selectedFile == null) {
                return;
            }
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            final VirtualFile gotoFile = PreviewUtil.getGotoFile(myProject, selectedFile);
            if (gotoFile != null && !fileEditorManager.isFileOpen(gotoFile)) {
                component.requestFocus();
            }
        });
    }

    public void openOrFocusSelectedFile(final Component component) {
        // - "Open declaration source in the same tab" is focus based (#29) - ensure that component has focus
        // - "Autoscroll from Source" triggers this function as well when switching tabs (#44) - focus shouldn't change
//        focusComponentIfSelectedFileIsNotOpen(component);
//        invokeSafe(() -> {
                    PreviewUtil.openSource(myProject, component);
//        });
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

    public Project getProject() {
        return myProject;
    }
}
