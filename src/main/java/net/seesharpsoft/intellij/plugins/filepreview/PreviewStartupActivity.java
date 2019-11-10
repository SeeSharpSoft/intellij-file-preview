package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import net.seesharpsoft.intellij.plugins.filepreview.viewhandler.ProjectToolWindowHandler;
import net.seesharpsoft.intellij.plugins.filepreview.viewhandler.VcsToolWindowHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PreviewStartupActivity implements StartupActivity, DumbAware {

    protected ConcurrentMap<Project, PreviewProjectHandler> myPreviewHandlerMap = new ConcurrentHashMap<>();

    public static final Map<String, PreviewViewHandler> SUPPORTED_TOOLWINDOWS_WITH_TREES;

    static {
        SUPPORTED_TOOLWINDOWS_WITH_TREES = new HashMap<>();
        SUPPORTED_TOOLWINDOWS_WITH_TREES.put(ToolWindowId.PROJECT_VIEW, new ProjectToolWindowHandler());
        SUPPORTED_TOOLWINDOWS_WITH_TREES.put(ToolWindowId.VCS, new VcsToolWindowHandler());
//        SUPPORTED_TOOLWINDOWS_WITH_TREES.put(
//                ToolWindowId.TODO_VIEW,
//                project -> null
//        );
        // Arrays.asList(
        //         ,
        //         ToolWindowId.FAVORITES_VIEW,
        //         ToolWindowId.FIND,
        //         ToolWindowId.TODO_VIEW,
        //         ToolWindowId.TASKS
        // );
    }

    protected void initializeOpenFiles(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (VirtualFile virtualFile : fileEditorManager.getOpenFiles()) {
            PreviewUtil.setStateOpened(project, virtualFile);
        }
    }

    protected void initialize(Project project, MessageBusConnection connection) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isInitialized(project) || project.isDisposed()) {
                return;
            }

            PreviewProjectHandler projectHandler = PreviewProjectHandler.createIfPossible(project, connection);
            if (projectHandler != null) {
                myPreviewHandlerMap.put(project, projectHandler);
                registerAllToolWindows(project);
            }

            initializeOpenFiles(project);
        });
    }

    protected void dispose(Project project, MessageBusConnection connection) {
        PreviewProjectHandler projectHandler = myPreviewHandlerMap.get(project);
        if (projectHandler != null) {
            unregisterAllToolWindows(project);
            myPreviewHandlerMap.remove(project);
            projectHandler.dispose();
            connection.disconnect();
            connection.dispose();
        }
    }

    protected void registerAllToolWindows(Project project) {
        for (String toolWindowId : SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet()) {
            registerToolWindow(project, toolWindowId);
        }
    }

    protected void unregisterAllToolWindows(Project project) {
        for (String toolWindowId : SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet()) {
            unregisterToolWindow(project, toolWindowId);
        }
    }

    protected boolean isInitialized(Project project) {
        return myPreviewHandlerMap.containsKey(project);
    }

    protected void registerToolWindow(Project project, String toolWindowId) {
        if (!isInitialized(project) || !SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet().contains(toolWindowId)) {
            return;
        }
        PreviewProjectHandler previewProjectHandler = myPreviewHandlerMap.get(project);
        SUPPORTED_TOOLWINDOWS_WITH_TREES.get(toolWindowId).register(previewProjectHandler);
    }

    protected void unregisterToolWindow(Project project, String toolWindowId) {
        if (!isInitialized(project) || !SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet().contains(toolWindowId)) {
            return;
        }

        PreviewProjectHandler previewProjectHandler = myPreviewHandlerMap.get(project);
        SUPPORTED_TOOLWINDOWS_WITH_TREES.get(toolWindowId).unregister(previewProjectHandler);
    }

    @Override
    public void runActivity(@NotNull Project activityProject) {
        if (isInitialized(activityProject)) {
            return;
        }

        MessageBusConnection connection = activityProject.getMessageBus().connect();
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowRegistered(@NotNull String id) {
                 ToolWindow toolWindow = ToolWindowManager.getInstance(activityProject).getToolWindow(id);
                 if (toolWindow != null && !isInitialized(activityProject)) {
                     initialize(activityProject, connection);
                 }
                registerToolWindow(activityProject, id);
            }

            @Override
            public void toolWindowUnregistered(@NotNull String id, @NotNull ToolWindow toolWindow) {
                if (ToolWindowId.PROJECT_VIEW.equals(id)) {
                    dispose(activityProject, connection);
                }
                unregisterToolWindow(activityProject, id);
            }

            @Override
            public void stateChanged() {
                ToolWindow window = ToolWindowManager.getInstance(activityProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
                if (window != null && !isInitialized(activityProject)) {
                    initialize(activityProject, connection);
                }
            }
        });
        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosingBeforeSave(@NotNull Project project) {
                if (activityProject.equals(project)) {
                    dispose(activityProject, connection);
                }
            }
        });
        // try to register at startup
        initialize(activityProject, connection);
    }
}
