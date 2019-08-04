package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class PreviewFileListener implements DocumentListener {

    public static final Key<PreviewFileListener> PREVIEW_FILE_LISTENER = Key.create(PreviewFileListener.class.getName());

    private final PreviewProjectHandler myPreviewProjectHandler;
    private final PreviewVirtualFile myPreviewFile;

    public PreviewFileListener(PreviewProjectHandler previewProjectHandler, PreviewVirtualFile previewFile) {
        myPreviewProjectHandler = previewProjectHandler;
        myPreviewFile = previewFile;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        myPreviewProjectHandler.openFileEditor(myPreviewFile);
    }

}
