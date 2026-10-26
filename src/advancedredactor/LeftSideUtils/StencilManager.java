package advancedredactor.LeftSideUtils;

import advancedredactor.upperuiutils.LandCopyPast;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.TextField.TextFieldFilter;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Platform;
import mindustry.editor.EditorTool;
import mindustry.editor.MapView;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import java.lang.reflect.Method;

public class StencilManager {
public static Texture texture;
public static TextureRegion region;
public static Fi currentFile;
public static int targetW = 100;
public static int targetH = 100;
public static float offsetX = 0.0F;
public static float offsetY = 0.0F;
public static float opacity = 0.45F;
public static boolean enabled = true;
public static boolean moveMode = false;
private static Table mobileBar;
private static boolean dragging = false;
private static int dragLastX;
private static int dragLastY;
private static EditorTool savedTool;
private static final Vec2 tmpA = new Vec2();
private static final Vec2 tmpB = new Vec2();

public static void init() {
Events.run(Trigger.update, StencilManager::update);
}

public static void setMoveMode(boolean v) {
MapView view = Vars.ui != null && Vars.ui.editor != null ? Vars.ui.editor.getView() : null;
if (!moveMode && v && view != null) {
savedTool = view.getTool();
view.setTool(EditorTool.pick);
} else if (moveMode && !v && view != null && savedTool != null) {
view.setTool(savedTool);
}
moveMode = v;
dragging = false;
}

private static void update() {
if (Vars.ui != null && Vars.ui.editor != null && Vars.ui.editor.isShown()) {
MapView view = Vars.ui.editor.getView();
if (view != null && view.parent != null) {
if (moveMode && view.getTool() != EditorTool.pick) {
savedTool = view.getTool();
moveMode = false;
}
if (!enabled || region == null || mobileBar != null && mobileBar.parent == view.parent) {
if ((!enabled || region == null) && mobileBar != null) {
mobileBar.remove();
mobileBar = null;
}
} else {
rebuildMobileBar(view.parent);
}
}
} else {
if (mobileBar != null) {
mobileBar.remove();
mobileBar = null;
}
dragging = false;
}
}

private static void rebuildMobileBar(Group parent) {
if (mobileBar != null) {
mobileBar.remove();
}
mobileBar = new Table();
mobileBar.setFillParent(true);
mobileBar.bottom().left();
mobileBar.touchable = Touchable.childrenOnly;
Table box = new Table(Tex.button);
box.margin(4.0F);
TextButton moveBtn = (TextButton)box.button(Core.bundle.get("editor.stencil.movemode", "Move Stencil"), Icon.move, Styles.flatTogglet, () -> {
setMoveMode(!moveMode);
}).size(150.0F, 42.0F).get();
moveBtn.update(() -> {
moveBtn.setChecked(moveMode);
});
mobileBar.add(box).padLeft(8.0F).padBottom(8.0F);
parent.addChild(mobileBar);
}

public static void drawOnOverlay(MapView view, Element overlay) {
if (enabled && region != null) {
try {
Vec2 a = LandCopyPast.tileToOverlay(view, overlay, offsetX, offsetY, tmpA);
Vec2 b = LandCopyPast.tileToOverlay(view, overlay, offsetX + (float)targetW, offsetY + (float)targetH, tmpB);
float rx = Math.min(a.x, b.x);
float ry = Math.min(a.y, b.y);
float rw = Math.abs(b.x - a.x);
float rh = Math.abs(b.y - a.y);
Draw.color(1.0F, 1.0F, 1.0F, opacity);
Draw.rect(region, rx + rw / 2.0F, ry + rh / 2.0F, rw, rh);
if (moveMode) {
Lines.stroke(2.0F, dragging ? Color.sky : Pal.accent);
Lines.rect(rx, ry, rw, rh);
}
Draw.reset();
} catch (Throwable var8) {
}
}
}

public static boolean onTouchDown(MapView view, float x, float y) {
if (enabled && region != null && moveMode) {
Point2 p = LandCopyPast.projectRaw(view, x, y);
dragging = true;
dragLastX = p.x;
dragLastY = p.y;
return true;
} else {
return false;
}
}

public static boolean onTouchDragged(MapView view, float x, float y) {
if (!dragging) {
return false;
} else {
Point2 p = LandCopyPast.projectRaw(view, x, y);
offsetX += (float)(p.x - dragLastX);
offsetY += (float)(p.y - dragLastY);
dragLastX = p.x;
dragLastY = p.y;
return true;
}
}

public static boolean onTouchUp() {
if (!dragging) {
return false;
} else {
dragging = false;
return true;
}
}

public static void loadStencil(Fi file, int w, int h) {
try {
Pixmap src = new Pixmap(file);
targetW = Math.max(1, w);
targetH = Math.max(1, h);
Pixmap scaled = scalePixmap(src, targetW, targetH);
src.dispose();
if (texture != null) {
texture.dispose();
}
texture = new Texture(scaled);
region = new TextureRegion(texture);
scaled.dispose();
currentFile = file;
enabled = true;
} catch (Exception var5) {
Vars.ui.showException("@editor.errorload", var5);
}
}

public static Pixmap scalePixmap(Pixmap src, int w, int h) {
Pixmap dst = new Pixmap(w, h);
int sw = src.width;
int sh = src.height;
for(int y = 0; y < h; ++y) {
int sy = Math.min(sh - 1, y * sh / h);
for(int x = 0; x < w; ++x) {
dst.setRaw(x, y, src.getRaw(Math.min(sw - 1, x * sw / w), sy));
}
}
return dst;
}

private static void pickPNG(Cons<Fi> cons) {
try {
Class<?> fc = null;
try {
fc = Class.forName("mindustry.ui.FileChooser");
} catch (Throwable ignored) {}
if (fc == null) {
try {
fc = Class.forName("mindustry.ui.dialogs.FileChooser");
} catch (Throwable ignored) {}
}
if (fc != null) {
Method openMethod = null;
for (Method m : fc.getMethods()) {
if (m.getName().equals("open") && m.getParameterCount() == 1) {
openMethod = m;
break;
}
}
if (openMethod != null) {
Object fci;
Class<?> paramType = openMethod.getParameterTypes()[0];
if (paramType == String.class) {
fci = openMethod.invoke(null, "png");
} else if (paramType == String[].class) {
fci = openMethod.invoke(null, (Object) new String[]{"png"});
} else {
fci = null;
}
if (fci != null) {
for (Method m : fci.getClass().getMethods()) {
if (m.getName().equals("submit") && m.getParameterCount() == 1) {
m.invoke(fci, cons);
return;
}
}
}
}
}
} catch (Throwable t) {
Log.err("FileChooser reflection failed", t);
}
try {
Method best = null;
for (Method m : Platform.class.getMethods()) {
if (m.getName().equals("showFileChooser")) {
int pc = m.getParameterCount();
if (pc == 3 || pc == 4) {
best = m;
break;
}
}
}
if (best != null) {
int pc = best.getParameterCount();
if (pc == 3) {
best.invoke(Vars.platform, true, "png", cons);
} else {
best.invoke(Vars.platform, true, "Select PNG", "png", cons);
}
return;
}
} catch (Throwable t) {
Log.err("Platform file chooser failed", t);
}
try {
Method m = Platform.class.getMethod("showFileChooser", boolean.class, String.class, Cons.class);
m.invoke(Vars.platform, true, "png", cons);
return;
} catch (Throwable ignored) {}
try {
Method m = Platform.class.getMethod("showFileChooser", boolean.class, String.class, String.class, Cons.class);
m.invoke(Vars.platform, true, "Select PNG", "png", cons);
return;
} catch (Throwable ignored) {}
Core.app.post(() -> Vars.ui.showErrorMessage("File chooser is not available on this platform."));
}

public static void showDialog() {
BaseDialog d = new BaseDialog(Core.bundle.get("editor.stencil.title", "Stencil Setup"));
Table t = new Table();
Runnable[] rebuild = new Runnable[1];
rebuild[0] = () -> {
t.clearChildren();
t.defaults().pad(4.0F).left();
if (targetW <= 0) {
targetW = Vars.editor.width();
}
if (targetH <= 0) {
targetH = Vars.editor.height();
}
if (region != null) {
Table prevBox = new Table(Tex.pane);
Image img = new Image(new TextureRegionDrawable(region));
img.setScaling(Scaling.fit);
prevBox.add(img).size(160.0F, 160.0F).pad(4.0F);
t.add(prevBox).colspan(2).center().padBottom(6.0F).row();
}
t.add(Core.bundle.get("editor.stencil.width", "Width (tiles):"));
TextField wf = (TextField)t.field(String.valueOf(targetW), TextFieldFilter.digitsOnly, (s) -> {
targetW = Math.max(1, Strings.parseInt(s, targetW));
if (currentFile != null) {
loadStencil(currentFile, targetW, targetH);
}
}).width(140.0F).get();
t.row();
t.add(Core.bundle.get("editor.stencil.height", "Height (tiles):"));
TextField hf = (TextField)t.field(String.valueOf(targetH), TextFieldFilter.digitsOnly, (s) -> {
targetH = Math.max(1, Strings.parseInt(s, targetH));
if (currentFile != null) {
loadStencil(currentFile, targetW, targetH);
}
}).width(140.0F).get();
t.row();
t.button(Core.bundle.get("editor.stencil.fitmap", "Fit Map Size"), Styles.flatt, () -> {
targetW = Vars.editor.width();
targetH = Vars.editor.height();
wf.setText(String.valueOf(targetW));
hf.setText(String.valueOf(targetH));
if (currentFile != null) {
loadStencil(currentFile, targetW, targetH);
}
}).colspan(2).growX().height(36.0F).row();
t.button(Core.bundle.get("editor.stencil.selectpng", "Select PNG..."), Icon.fileImage, Styles.flatt, () -> {
pickPNG((file) -> {
Core.app.post(() -> {
loadStencil(file, targetW, targetH);
rebuild[0].run();
});
});
}).colspan(2).growX().height(42.0F).row();
t.add(Core.bundle.get("editor.stencil.opacity", "Opacity:"));
t.slider(0.05F, 1.0F, 0.05F, opacity, (v) -> {
opacity = v;
}).width(160.0F).row();
t.add(Core.bundle.get("editor.stencil.offsetx", "Offset X:"));
TextField oxf = (TextField)t.field(Strings.fixed(offsetX, 0), (s) -> {
offsetX = Strings.parseFloat(s, offsetX);
}).width(140.0F).get();
t.row();
t.add(Core.bundle.get("editor.stencil.offsety", "Offset Y:"));
TextField oyf = (TextField)t.field(Strings.fixed(offsetY, 0), (s) -> {
offsetY = Strings.parseFloat(s, offsetY);
}).width(140.0F).get();
t.row();
t.button(Core.bundle.get("editor.stencil.center", "Center on Map"), Styles.flatt, () -> {
offsetX = (float)(Vars.editor.width() - targetW) / 2.0F;
offsetY = (float)(Vars.editor.height() - targetH) / 2.0F;
oxf.setText(Strings.fixed(offsetX, 0));
oyf.setText(Strings.fixed(offsetY, 0));
}).colspan(2).growX().height(36.0F).row();
t.check(Core.bundle.get("editor.stencil.enable", "Show Stencil"), enabled, (v) -> {
enabled = v;
}).colspan(2).left().row();
t.check(Core.bundle.get("editor.stencil.movemode", "Drag / Move Mode"), moveMode, StencilManager::setMoveMode).colspan(2).left().row();
t.button(Core.bundle.get("editor.stencil.clear", "Clear Stencil"), Icon.trash, Styles.flatt, () -> {
if (texture != null) {
texture.dispose();
texture = null;
region = null;
}
currentFile = null;
setMoveMode(false);
rebuild[0].run();
}).colspan(2).growX().height(38.0F).padTop(6.0F).row();
};
rebuild[0].run();
d.cont.pane(t).maxHeight((float)Core.graphics.getHeight() * 0.82F);
d.addCloseButton();
d.show();
}
}
