package net.seesharpsoft.intellij.plugins.filepreview;

public interface PreviewViewHandler {
    void register(PreviewProjectHandler previewProjectHandler);

    void unregister(PreviewProjectHandler previewProjectHandler);
}
