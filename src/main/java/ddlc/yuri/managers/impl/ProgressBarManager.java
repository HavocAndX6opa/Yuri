package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.utils.render.progress.ProgressBarEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProgressBarManager {

    private static final List<ProgressBarEntry> entries = new CopyOnWriteArrayList<>();

    @EventHook
    public void onRender2D(Render2DEvent event) {
        for (ProgressBarEntry entry : entries) {
            entry.tick();
            entry.render();
        }
        entries.removeIf(ProgressBarEntry::shouldRemove);
    }

    public static ProgressBarEntry add(float progress, float x, float y) {
        ProgressBarEntry entry = new ProgressBarEntry(progress, x, y);
        entries.add(entry);
        return entry;
    }

    public static void remove(ProgressBarEntry entry) {
        if (entry != null) {
            entry.setFadingOut(true);
        }
    }
}
