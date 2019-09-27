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
import org.jetbrains.annotations.Nullable;

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

import static net.seesharpsoft.intellij.plugins.filepreview.PreviewSettings.PreviewBehavior.EXPLICIT_PREVIEW;

public class PreviewProjectHandler {

    private Project myProject;
    private AbstractProjectViewPane myProjectViewPane;

    private final List<JTree> registeredTrees = new ArrayList<>();

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
        openOrFocusSelectedFile((Component) treeSelectionEvent.getSource());
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
                        PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> PreviewUtil.disposePreview(myProject, PreviewUtil.getGotoFile(myProject, selectedFile)));
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
            if (!PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW) && !PreviewUtil.isPreviewed(event.getNewFile())) {
                PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), file -> {
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
            PreviewUtil.consumeSelectedFile(myProjectViewPane.getTree(), selectedFile -> {
                if (PreviewUtil.isPreviewed(file) ||
                        (selectedFile != null && selectedFile.equals(file) && !PreviewSettings.getInstance().getPreviewBehavior().equals(EXPLICIT_PREVIEW))) {
                    closeOtherPreviews(file);
                }
            });
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

        // registerTreeHandlers(myProjectViewPane.getTree());

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

        unregisterAllTreeHandlers();

        myProject = null;
    }

    public void registerTreeHandlers(@NotNull final JTree tree) {
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

    public void openOrFocusSelectedFile(final Component component) {
        // "Open declaration source in the same tab" is focus based (#29) - ensure that component has focus
        component.requestFocus();
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
        consumeSelectedFile(component, selectedFile -> {
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
        focusComponentIfSelectedFileIsNotOpen(component);
        invokeSafe(() -> {
            switch (PreviewSettings.getInstance().getPreviewBehavior()) {
                case PREVIEW_BY_DEFAULT:
                    PreviewUtil.consumeSelectedFile(component, file -> {
                        openPreviewOrEditor(PreviewUtil.getGotoFile(myProject, file));
                    });
                    break;
                case EXPLICIT_PREVIEW:
                    PreviewUtil.consumeSelectedFile(component, file -> {
                        focusFileEditor(PreviewUtil.getGotoFile(myProject, file), false);
                    });
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
            }
        });
    }

    public void openPreviewOrEditor(final VirtualFile file) {
        if (!isValid() || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                closeAllPreviews();
            }
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        if (!fileEditorManager.isFileOpen(file)) {
            PreviewUtil.setStatePreviewed(myProject, file);
        }
        invokeSafeAndWait(() -> fileEditorManager.openFile(file, false));
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
