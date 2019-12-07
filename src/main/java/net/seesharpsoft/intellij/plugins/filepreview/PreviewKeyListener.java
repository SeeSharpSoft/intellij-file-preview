package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.project.Project;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PreviewKeyListener implements KeyListener {

    private final Project myProject;

    public PreviewKeyListener(final Project project) {
        myProject = project;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                PreviewUtil.closeAllPreviews(myProject);
                break;
            case KeyEvent.VK_SPACE:
                PreviewUtil.openPreviewOrEditor(myProject, (Component) e.getSource());
                break;
            case KeyEvent.VK_TAB:
//                PreviewUtil.consumeSelectedFile((Component) e.getSource(), file -> {
//                    PreviewUtil.focusFileEditor(file, true);
//                });
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
