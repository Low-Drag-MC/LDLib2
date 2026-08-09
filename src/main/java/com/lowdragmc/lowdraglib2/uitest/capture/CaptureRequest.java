package com.lowdragmc.lowdraglib2.uitest.capture;

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

    public CaptureRequest(Kind kind, String scenarioName, String label, int stepIndex,
                          RunReport.StepReport stepReport, @Nullable ElementRef element) {
        this.kind = kind;
        this.scenarioName = scenarioName;
        this.label = label;
        this.stepIndex = stepIndex;
        this.stepReport = stepReport;
        this.element = element;
    }
}
