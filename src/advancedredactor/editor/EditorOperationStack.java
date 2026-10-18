package advancedredactor.editor;

import arc.struct.Seq;

public class EditorOperationStack {
    private final Seq<EditorOperation> history = new Seq<>();
    private int index = -1;

    public void push(EditorOperation op) {
        if (index < history.size - 1) {
            history.truncate(index + 1);
        }
        history.add(op);
        index++;
    }

    public void undo() {
        if (index >= 0) {
            history.get(index).undo();
            index--;
        }
    }

    public void redo() {
        if (index < history.size - 1) {
            index++;
            history.get(index).redo();
        }
    }

    public void clear() {
        history.clear();
        index = -1;
    }

    public boolean canUndo() {
        return index >= 0;
    }

    public boolean canRedo() {
        return index < history.size - 1;
    }
}
