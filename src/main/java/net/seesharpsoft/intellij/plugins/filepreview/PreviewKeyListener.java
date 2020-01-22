package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PreviewKeyListener implements KeyListener {

    private final Project myProject;

    public PreviewKeyListener(@NotNull final Project project) {
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
            default:
                // ignore
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
