package advancedredactor.ui;

import advancedredactor.LeftSideUtils.MapArtDialog;
import advancedredactor.LeftSideUtils.StencilManager;
import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.editor.MapEditor;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class LeftSideUi {
private static final ObjectSet<Table> hookedBrushTables = new ObjectSet();
private static final ObjectSet<Table> hookedMidTables = new ObjectSet();

public static void init() {
Events.run(Trigger.update, () -> {
if (Vars.ui != null && Vars.ui.editor != null) {
if (!Vars.ui.editor.isShown()) {
hookedBrushTables.clear();
hookedMidTables.clear();
} else {
injectLeftControls(Vars.ui.editor);
}
}
});
}

private static void injectLeftControls(Element root) {
if (root instanceof Slider) {
Slider sl = (Slider)root;
if (Math.abs(sl.getMaxValue() - (float)(MapEditor.brushSizes.length - 1)) < 0.1F && sl.parent instanceof Stack && sl.parent.parent instanceof Table) {
Table brushTable = (Table)sl.parent.parent;
if (!hookedBrushTables.contains(brushTable)) {
hookedBrushTables.add(brushTable);
rebuildBrushBox(brushTable, (Stack)sl.parent, sl);
}
if (brushTable.parent instanceof Table) {
Table midTable = (Table)brushTable.parent;
if (!hookedMidTables.contains(midTable)) {
hookedMidTables.add(midTable);
appendBottomUtils(midTable);
}
}
}
}
if (root instanceof Group) {
for(Element e : ((Group)root).getChildren()) {
injectLeftControls(e);
}
}
}

private static void rebuildBrushBox(Table parent, Stack stack, Slider slider) {
try {
Seq<Element> saved = new Seq();
for(Element e : parent.getChildren()) {
saved.add(e);
}
parent.clearChildren();
for(int i = 0; i < saved.size(); ++i) {
Element el = (Element)saved.get(i);
if (el == stack) {
parent.add(el).growX().padTop(4.0F).row();
Table inputBox = new Table();
inputBox.left();
inputBox.add(Core.bundle.get("editor.brushsize.input", "Brush:")).style(Styles.outlineLabel).padRight(4.0F);
TextField field = new TextField(Strings.fixed(Vars.editor.brushSize, 1));
field.setFilter((f, c) -> Character.isDigit(c) || c == '.');
field.changed(() -> {
float val = Strings.parseFloat(field.getText(), -1.0F);
if (val >= 0.5F && val <= 500.0F) {
Vars.editor.brushSize = val;
}
});
slider.moved((v) -> {
int idx = Math.max(0, Math.min(MapEditor.brushSizes.length - 1, (int)v));
Vars.editor.brushSize = MapEditor.brushSizes[idx];
field.setText(Strings.fixed(Vars.editor.brushSize, 1));
});
inputBox.add(field).width(80.0F).height(32.0F);
parent.add(inputBox).growX().padTop(4.0F).padBottom(6.0F).row();
} else {
parent.add(el).growX().row();
}
}
} catch (Exception var8) {
}
}

private static void appendBottomUtils(Table mid) {
try {
mid.row();
Table utils = new Table();
utils.defaults().growX().height(40.0F).padTop(3.0F).padBottom(3.0F);
utils.button(Core.bundle.get("editor.stencil.btn", "Stencil"), Icon.imageSmall, Styles.flatt, StencilManager::showDialog).row();
utils.button(Core.bundle.get("editor.mapart.btn", "Pixel Art"), Icon.terrain, Styles.flatt, MapArtDialog::show).row();
Table wrapper = new Table();
wrapper.add(utils).growX().top();
mid.add(wrapper).growX().row();
} catch (Exception var2) {
}
}
}
