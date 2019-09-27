package net.seesharpsoft.intellij.plugins.filepreview;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.todo.TodoView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class PreviewStartupActivity implements StartupActivity, DumbAware {

    protected ConcurrentMap<Project, PreviewProjectHandler> myPreviewHandlerMap = new ConcurrentHashMap<>();

    public static final Map<String, Function<Project, JTree>> SUPPORTED_TOOLWINDOWS_WITH_TREES;

    static {
        SUPPORTED_TOOLWINDOWS_WITH_TREES = new HashMap<>();
        SUPPORTED_TOOLWINDOWS_WITH_TREES.put(
                ToolWindowId.PROJECT_VIEW,
                project -> ProjectView.getInstance(project).getCurrentProjectViewPane().getTree()
        );
        SUPPORTED_TOOLWINDOWS_WITH_TREES.put(
                ToolWindowId.TODO_VIEW,
                project -> null
        );
        // Arrays.asList(
        //         ,
        //         ToolWindowId.FAVORITES_VIEW,
        //         ToolWindowId.FIND,
        //         ToolWindowId.TODO_VIEW,
        //         ToolWindowId.TASKS
        // );
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
        });
    }

    protected void dispose(Project project, MessageBusConnection connection) {
        PreviewProjectHandler projectHandler = myPreviewHandlerMap.get(project);
        if (projectHandler != null) {
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

    protected boolean isInitialized(Project project) {
        return myPreviewHandlerMap.containsKey(project);
    }

    protected void registerToolWindow(Project project, String toolWindowId) {
        if (!isInitialized(project) || !SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet().contains(toolWindowId)) {
            return;
        }

        PreviewProjectHandler previewProjectHandler = myPreviewHandlerMap.get(project);
        previewProjectHandler.registerTreeHandlers(SUPPORTED_TOOLWINDOWS_WITH_TREES.get(toolWindowId).apply(project));
    }

    protected void unregisterToolWindow(Project project, String toolWindowId) {
        if (!isInitialized(project) || !SUPPORTED_TOOLWINDOWS_WITH_TREES.keySet().contains(toolWindowId)) {
            return;
        }

        PreviewProjectHandler previewProjectHandler = myPreviewHandlerMap.get(project);
        previewProjectHandler.unregisterTreeHandlers(SUPPORTED_TOOLWINDOWS_WITH_TREES.get(toolWindowId).apply(project));
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
                // ToolWindow toolWindow = ToolWindowManager.getInstance(activityProject).getToolWindow(id);
                // if (toolWindow != null && !isInitialized(activityProject)) {
                //     initialize(activityProject, connection);
                // }
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
