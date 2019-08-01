package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
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
import java.awt.event.KeyListener;
import java.util.List;
import java.util.function.Consumer;

public class PreviewProjectHandler {

    private Project myProject;
    private PreviewVirtualFile myPreviewFile;
    private AbstractProjectViewPane myProjectViewPane;
    private final KeyListener myKeyListener;

    private final TreeSelectionListener myTreeSelectionListener = treeSelectionEvent -> {
        switch (PreviewSettings.getInstance().getPreviewBehavior()) {
            case PREVIEW_BY_DEFAULT:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    openPreviewOrEditor(file);
                });
                break;
            case EXPLICIT_PREVIEW:
                consumeSelectedFile((Component) treeSelectionEvent.getSource(), file -> {
                    focusFileEditor(file);
                });
                break;
            default:
                throw new UnsupportedOperationException(String.format("case '%s' not handled", PreviewSettings.getInstance().getPreviewBehavior()));
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (PreviewSettings.getInstance().isProjectViewFocusSupport() && file instanceof PreviewVirtualFile) {
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
                    initPreviewFile((PreviewVirtualFile) file);
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

    public PreviewProjectHandler() {
        myKeyListener = new PreviewKeyListener(this);
    }

    public boolean init(@NotNull Project project, @NotNull MessageBusConnection messageBusConnection) {
        assert myProject == null : "already initialized";

        myProject = project;

        myProjectViewPane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
        if (myProjectViewPane == null) {
            return false;
        }

        myProjectViewPane.getTree().addTreeSelectionListener(myTreeSelectionListener);
        // required to support TAB key in listener - be aware of side effects...
        myProjectViewPane.getTree().setFocusTraversalKeysEnabled(false);
        myProjectViewPane.getTree().addKeyListener(myKeyListener);
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

    public void openPreviewOrEditor(VirtualFile file) {
        if (!isValid() || file == null || file.isDirectory() || !file.isValid()) {
            if (PreviewSettings.getInstance().isPreviewClosedOnEmptySelection()) {
                closePreview();
            }
            return;
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final VirtualFile fileToOpen = !fileEditorManager.isFileOpen(file) ? createAndSetPreviewFile(file) : file;
        if (fileToOpen != null) {
            invokeSafe(() -> fileEditorManager.openFile(fileToOpen, false));
        }
    }

    public boolean isCurrentlyPreviewed(VirtualFile file) {
        if (!isValid() || file == null || myPreviewFile == null) {
            return false;
        }
        return myPreviewFile.equals(file) || myPreviewFile.getSource().equals(file);
    }

    public void openFileEditor(final VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> openFileEditorWithCaretsAndSelections(file));
    }

    private void openFileEditorWithCaretsAndSelections(final VirtualFile file) {
        final VirtualFile fileToOpen = file instanceof PreviewVirtualFile ? ((PreviewVirtualFile)file).getSource() : file;
        final List<CaretState>[] caretsAndSelections = getCaretsAndSelections(file);
        invokeSafe(() -> openFileEditorsWithCaretsAndSelections(fileToOpen, caretsAndSelections));
    }

    @NotNull
    private List<CaretState>[] getCaretsAndSelections(final VirtualFile file) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final FileEditor[] sourceFileEditors = fileEditorManager.getEditors(file);
        final List<CaretState>[] caretsAndSelections = new List[sourceFileEditors.length];
        for (int i = 0; i < sourceFileEditors.length; ++i) {
            final FileEditor fileEditor = sourceFileEditors[i];
            if (fileEditor instanceof TextEditor) {
                caretsAndSelections[i] = ((TextEditor) fileEditor).getEditor().getCaretModel().getCaretsAndSelections();
            }
        }
        closePreview();
        return caretsAndSelections;
    }

    private void openFileEditorsWithCaretsAndSelections(final VirtualFile file, final List<CaretState>... caretsAndSelections) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final FileEditor[] targetFileEditors = fileEditorManager.openFile(file, true);
        for (int i = 0; i < caretsAndSelections.length && i < targetFileEditors.length; ++i) {
            final List<CaretState> caretsAndSelectionEntry = caretsAndSelections[i];
            if (caretsAndSelectionEntry == null) {
                continue;
            }
            final FileEditor fileEditor = targetFileEditors[i];
            if (fileEditor instanceof TextEditor) {
                ((TextEditor) fileEditor).getEditor().getCaretModel().setCaretsAndSelections(caretsAndSelectionEntry, false);
            }
        }
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

        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        closeFileEditor(fileEditorManager.getCurrentFile());
    }

    public void closeFileEditor(VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            closePreview();
        } else {
            FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
            invokeSafe(() -> fileEditorManager.closeFile(file, false, true));
        }
    }

    public void closePreview() {
        if (!isValid() || myPreviewFile == null) {
            return;
        }

        final PreviewVirtualFile closingPreviewFile = myPreviewFile;
        myPreviewFile = null;
        disposePreviewFile(closingPreviewFile);
        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        invokeSafeAndWait(() -> fileEditorManager.closeFile(closingPreviewFile, false, true));
    }

    public void focusFileEditor(VirtualFile file) {
        if (!isValid() || file == null) {
            return;
        }
        final VirtualFile fileToFocus = myPreviewFile != null && myPreviewFile.getSource().equals(file) ? myPreviewFile : file;
        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(myProject);
        if (fileToFocus == null || !fileEditorManager.isFileOpen(fileToFocus)) {
            return;
        }
        invokeSafe(() -> fileEditorManager.openFile(file, true));
    }

    public VirtualFile createAndSetPreviewFile(VirtualFile file) {
        if (!isValid()) {
            return null;
        }

        if (myPreviewFile != null && myPreviewFile.getSource().equals(file)) {
            return myPreviewFile;
        }
        return new PreviewVirtualFile(file);
    }

    public void invokeSafeAndWait(Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (isValid()) {
                runnable.run();
            }
        });
    }

    public void invokeSafe(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isValid()) {
                runnable.run();
            }
        });
    }

    public boolean isValid() {
        return myProject != null && !myProject.isDisposed();
    }

    protected void initPreviewFile(PreviewVirtualFile previewFile) {
        myPreviewFile = previewFile;
        invokeSafe(() -> {
            Document document = FileDocumentManager.getInstance().getDocument(previewFile.getSource());
            if (document != null) {
                PreviewFileListener listener = new PreviewFileListener(this, previewFile);
                previewFile.putUserData(PreviewFileListener.PREVIEW_FILE_LISTENER, listener);
                document.addDocumentListener(listener);
            }
        });
    }

    protected void disposePreviewFile(PreviewVirtualFile previewFile) {
        PreviewFileListener listener = previewFile.getUserData(PreviewFileListener.PREVIEW_FILE_LISTENER);

        if (listener == null) {
            return;
        }

        FileDocumentManager.getInstance().getDocument(previewFile.getSource()).removeDocumentListener(listener);
    }
}
