package com.lowdragmc.lowdraglib2.uitest.capture;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.uitest.ElementRef;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import org.jetbrains.annotations.Nullable;

/**
 * A capture asked for by a step, serviced on the <em>next</em> frame.
 *
 * <p>The one-frame delay is the structural fix for stale screenshots: a step that changes the UI and
 * captures in the same call would photograph the previous frame, because nothing has re-rendered
 * yet. Deferring guarantees the image shows the state the step just produced.
 */
public final class CaptureRequest {

    public enum Kind {
        FULL,
        ELEMENT,
        /**
         * A render target that is not the game's frame — a UI hosted in its own operating-system
         * window. Its pixels never reach the main framebuffer, so a {@link #FULL} capture cannot see
         * it at all.
         */
        SURFACE,
        /** Taken automatically when a step fails or throws. */
        ERROR
    }

    public final Kind kind;
    public final String scenarioName;
    public final String label;
    public final int stepIndex;
    public final RunReport.StepReport stepReport;
    @Nullable
    public final ElementRef element;
    /** Where to read the frame back from. {@code null} means the game's own. */
    @Nullable
    public final UISurface surface;

    public CaptureRequest(Kind kind, String scenarioName, String label, int stepIndex,
                          RunReport.StepReport stepReport, @Nullable ElementRef element) {
        this(kind, scenarioName, label, stepIndex, stepReport, element, null);
    }

    public CaptureRequest(Kind kind, String scenarioName, String label, int stepIndex,
                          RunReport.StepReport stepReport, @Nullable ElementRef element,
                          @Nullable UISurface surface) {
        this.kind = kind;
        this.scenarioName = scenarioName;
        this.label = label;
        this.stepIndex = stepIndex;
        this.stepReport = stepReport;
        this.element = element;
        this.surface = surface;
    }
}
