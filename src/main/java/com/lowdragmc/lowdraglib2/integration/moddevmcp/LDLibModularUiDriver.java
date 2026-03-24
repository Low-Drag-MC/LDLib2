package com.lowdragmc.lowdraglib2.integration.moddevmcp;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.*;

import java.util.List;
import java.util.Map;

public final class LDLibModularUiDriver implements UiDriver {

    @Override
    public DriverDescriptor descriptor() {
        return null;
    }

    @Override
    public boolean matches(UiContext context) {
        return false;
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        return null;
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return List.of();
    }

    @Override
    public OperationResult<Map<String, Object>> capture(UiContext context, CaptureRequest request) {
        return UiDriver.super.capture(context, request);
    }

    @Override
    public OperationResult<Map<String, Object>> action(UiContext context, UiActionRequest request) {
        return UiDriver.super.action(context, request);
    }

    @Override
    public OperationResult<List<UiTarget>> inspectAt(UiContext context, int x, int y) {
        return UiDriver.super.inspectAt(context, x, y);
    }

    @Override
    public OperationResult<TooltipSnapshot> tooltip(UiContext context, TargetSelector selector) {
        return UiDriver.super.tooltip(context, selector);
    }

    @Override
    public UiInteractionState interactionState(UiContext context) {
        return UiDriver.super.interactionState(context);
    }

    @Override
    public UiResolveResult resolve(UiContext context, UiResolveRequest request) {
        return UiDriver.super.resolve(context, request);
    }

    @Override
    public UiActionabilityResult checkActionability(UiContext context, UiTarget target, String action) {
        return UiDriver.super.checkActionability(context, target, action);
    }

    @Override
    public UiWaitResult waitFor(UiContext context, UiWaitRequest request) {
        return UiDriver.super.waitFor(context, request);
    }

    @Override
    public OperationResult<Map<String, Object>> runIntent(UiContext context, String intent, Map<String, Object> arguments) {
        return UiDriver.super.runIntent(context, intent, arguments);
    }

    @Override
    public UiInspectResult inspect(UiContext context, SnapshotOptions options) {
        return UiDriver.super.inspect(context, options);
    }
}