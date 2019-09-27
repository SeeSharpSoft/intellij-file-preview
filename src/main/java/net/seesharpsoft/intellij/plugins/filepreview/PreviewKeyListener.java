package net.seesharpsoft.intellij.plugins.filepreview;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PreviewKeyListener implements KeyListener {

    private PreviewProjectHandler myPreviewProjectHandler;

    public PreviewKeyListener(PreviewProjectHandler previewProjectHandler) {
        myPreviewProjectHandler = previewProjectHandler;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                if (PreviewSettings.getInstance().isQuickNavigationKeyListenerEnabled()) {
                    myPreviewProjectHandler.closeCurrentFileEditor();
                } else {
                    myPreviewProjectHandler.closeAllPreviews();
                }
                break;
            case KeyEvent.VK_SPACE:
                PreviewUtil.consumeSelectedFile((Component) e.getSource(), file -> {
                    myPreviewProjectHandler.openPreviewOrEditor(file);
                });
                break;
            case KeyEvent.VK_TAB:
                if (PreviewSettings.getInstance().isQuickNavigationKeyListenerEnabled()) {
                    PreviewUtil.consumeSelectedFile((Component) e.getSource(), file -> {
                        myPreviewProjectHandler.focusFileEditor(file, true);
                    });
                }
                break;
            default:
                // ignore
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
