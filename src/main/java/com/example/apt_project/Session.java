package com.example.apt_project;

import java.util.concurrent.atomic.AtomicInteger;

public class Session {
    private String id;
    private String viewer_code;
    private String editor_code;
    private AtomicInteger viewer_count;
    private AtomicInteger editor_count;

    public Session() {}

    public Session(String id, String viewer_code, String editor_code) {
        this.id = id;
        this.viewer_code = viewer_code;
        this.editor_code = editor_code;
        this.viewer_count = new AtomicInteger(1);
        this.editor_count = new AtomicInteger(1);
    }

    public String getId() {
        return id;
    }

    public String getViewer_code() {
        return viewer_code;
    }

    public String getEditor_code() {
        return editor_code;
    }

    public int getViewer_count() {
        return viewer_count.get();
    }

    public int getEditor_count() {
        return editor_count.get();
    }

    public void incrementViewerCount() {
        viewer_count.incrementAndGet();
    }

    public void incrementEditorCount() {
        editor_count.incrementAndGet();
    }

    public void decrementViewerCount() {
        viewer_count.decrementAndGet();
    }

    public void decrementEditorCount() {
        editor_count.decrementAndGet();
    }

}