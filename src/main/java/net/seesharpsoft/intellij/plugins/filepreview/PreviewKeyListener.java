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
                myPreviewProjectHandler.closeCurrentFileEditor();
                break;
            case KeyEvent.VK_SPACE:
                myPreviewProjectHandler.consumeSelectedFile((Component)e.getSource(), file -> {
                    myPreviewProjectHandler.openPreviewOrEditor(file);
                });
                break;
            case KeyEvent.VK_TAB:
                myPreviewProjectHandler.consumeSelectedFile((Component) e.getSource(), file -> {
                    myPreviewProjectHandler.focusFile(file);
                });
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
