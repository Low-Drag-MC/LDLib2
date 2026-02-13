package com.lowdragmc.lowdraglib2.configurator;

public interface EditAction {
    static EditAction of(Runnable execute, Runnable undo) {
        return new EditAction() {
            @Override
            public void execute() {
                execute.run();
            }

            @Override
            public void undo() {
                undo.run();
            }
        };
    }

    void execute();

    void undo();

    default EditAction mergeExecuteAfter(EditAction other) {
        return new EditAction() {
            @Override
            public void execute() {
                other.execute();
                EditAction.this.execute();
            }

            @Override
            public void undo() {
                EditAction.this.undo();
                other.undo();
            }
        };
    }

    default EditAction mergeExecuteBefore(EditAction other) {
        return new EditAction() {
            @Override
            public void execute() {
                EditAction.this.execute();
                other.execute();
            }

            @Override
            public void undo() {
                other.undo();
                EditAction.this.undo();
            }
        };
    }
}
