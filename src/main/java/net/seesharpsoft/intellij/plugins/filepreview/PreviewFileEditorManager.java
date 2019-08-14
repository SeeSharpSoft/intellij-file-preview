package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import net.seesharpsoft.intellij.fileeditor.FileEditorManagerExWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PreviewFileEditorManager extends FileEditorManagerExWrapper {
    public static final Key<FileOpeningBehavior> FILE_OPENING_BEHAVIOR_KEY = Key.create(FileOpeningBehavior.class.getName());
    public static final Key<DocumentListener> PREVIEW_FILE_LISTENER = Key.create("DocumentListener@" + PreviewFileEditorManager.class.getName());

    enum FileOpeningBehavior {
        DEFAULT,
        PREVIEW
    }

    private final DocumentListener myDocumentListener = new DocumentListener() {
        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
            final VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
            disposePreviewFile(file);
            invokeSafe(() -> openFile(file, true));
        }
    };

    private final FileEditorManagerListener myFileEditorManagerListener = new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            file.putUserData(FILE_OPENING_BEHAVIOR_KEY, null);
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            disposePreviewFile(file);
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {

        }
    };

    private final FileEditorManagerListener.Before myFileEditorManagerBeforeListener = new FileEditorManagerListener.Before() {
        @Override
        public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            // if (PreviewProjectHandler.isPreviewFile(file)) {
            //     onBeforePreviewFileOpened(file);
            // }
        }
    };

    public PreviewFileEditorManager(@NotNull FileEditorManagerEx fileEditorManagerEx) {
        super(fileEditorManagerEx);

        final MessageBusConnection connection = fileEditorManagerEx.getProject().getMessageBus().connect();
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myFileEditorManagerListener);
        connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, myFileEditorManagerBeforeListener);
    }

    @Override
    public boolean isFileOpen(@NotNull final VirtualFile file) {
        return (file.getUserData(FILE_OPENING_BEHAVIOR_KEY) != null || file.getUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY) == null) &&
                super.isFileOpen(file);
    }

    @Override
    public void closeFile(@NotNull VirtualFile file) {
        disposePreviewFile(file);
        super.closeFile(file);
    }

    @Override
    public void closeFile(@NotNull VirtualFile file, @NotNull EditorWindow window) {
        disposePreviewFile(file);
        super.closeFile(file, window);
    }

    @NotNull
    public List<FileEditor> openEditor(@NotNull OpenFileDescriptor descriptor, boolean focusEditor) {
        disposePreviewFile(descriptor.getFile());
        return super.openEditor(descriptor, focusEditor);
    }

    @Override
    @NotNull
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@NotNull final VirtualFile file,
                                                                          boolean focusEditor,
                                                                          final boolean searchForSplitter) {
        prepareOpenFile(file);
        return super.openFileWithProviders(file, focusEditor,searchForSplitter);
    }

    @NotNull
    @Override
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@NotNull VirtualFile file,
                                                                          boolean focusEditor,
                                                                          @NotNull EditorWindow window) {
        prepareOpenFile(file);
        return super.openFileWithProviders(file,focusEditor,window);
    }

    protected void prepareOpenFile(@NotNull final VirtualFile file) {
        FileOpeningBehavior fileOpeningBehavior = file.getUserData(FILE_OPENING_BEHAVIOR_KEY);
        if (fileOpeningBehavior != null) {
            switch (fileOpeningBehavior) {
                case DEFAULT:
                    disposePreviewFile(file);
                    break;
                case PREVIEW:
                    preparePreviewFile(file);
                    break;
                default:
                    throw new UnsupportedOperationException(fileOpeningBehavior.toString());
            }
        } else {
            disposePreviewFile(file);
        }
    }

    protected void disposePreviewFile(@NotNull VirtualFile file) {
        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, null);
        DocumentListener listener = file.getUserData(PREVIEW_FILE_LISTENER);
        file.putUserData(PREVIEW_FILE_LISTENER, null);
        if (listener == null) {
            return;
        }
        FileDocumentManager.getInstance().getDocument(file).removeDocumentListener(listener);
    }

    protected void preparePreviewFile(@NotNull final VirtualFile file) {
        disposePreviewFile(file);
        file.putUserData(PreviewProjectHandler.PREVIEW_VIRTUAL_FILE_KEY, file.getName());

        if (!PreviewSettings.getInstance().isOpenEditorOnEditPreview()) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            document.addDocumentListener(myDocumentListener);
            file.putUserData(PREVIEW_FILE_LISTENER, myDocumentListener);
        }
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
        return getProject() != null && !getProject().isDisposed();
    }
}
