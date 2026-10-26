package advancedredactor.LeftSideUtils;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.util.Time;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import java.lang.reflect.Method;

public class MapArtDialog extends BaseDialog {
    public enum ArtStyle {TERRAIN, SORTER, CANVAS}
    public enum LayerMode {FLOORS_AND_WALLS, FLOORS_ONLY, WALLS_ONLY}
    
    private Fi selectedFile;
    private int origWidth = 0, origHeight = 0;
    private int targetWidth = 100, targetHeight = 100;
    private boolean keepAspect = true;
    private boolean clearBlocks = true;
    
    private ArtStyle style = ArtStyle.TERRAIN;
    private LayerMode layerMode = LayerMode.FLOORS_AND_WALLS;
    
    private Label fileLabel;
    private TextField widthField, heightField;
    private Table dynamicOptionsTable;

    public MapArtDialog() {
        super(Core.bundle.get("editor.mapart.title", "Pixel Art Generator"));
        addCloseButton();
        setupUI();
    }

    private void setupUI() {
        cont.clear();
        Table main = new Table();
        main.defaults().pad(4f);
        
        Table fileTable = new Table(Tex.button);
        fileLabel = new Label(Core.bundle.get("editor.mapart.nofile", "No PNG selected"));
        fileTable.add(fileLabel).padRight(10f);
        fileTable.button(Core.bundle.get("editor.mapart.selectpng", "Select PNG..."), Icon.fileImage, this::chooseImage).height(40f).padLeft(8f);
        main.add(fileTable).growX().row();
        
        Table styleTable = new Table(Tex.button);
        styleTable.add(Core.bundle.get("editor.mapart.type", "Generation Type:")).left().row();
        ButtonGroup<TextButton> styleGroup = new ButtonGroup<>();
        Table styleBtns = new Table();
        styleBtns.defaults().growX().height(38f).pad(2f);
        addStyleBtn(styleBtns, styleGroup, Core.bundle.get("editor.mapart.type.terrain", "Vanilla Terrain"), ArtStyle.TERRAIN);
        addStyleBtn(styleBtns, styleGroup, Core.bundle.get("editor.mapart.type.colored", "Colored Blocks"), ArtStyle.SORTER);
        addStyleBtn(styleBtns, styleGroup, "Canvas", ArtStyle.CANVAS);
        styleTable.add(styleBtns).growX();
        main.add(styleTable).growX().row();
        
        Table sizeTable = new Table(Tex.button);
        widthField = new TextField(String.valueOf(targetWidth));
        heightField = new TextField(String.valueOf(targetHeight));
        widthField.setFilter(TextField.TextFieldFilter.digitsOnly);
        heightField.setFilter(TextField.TextFieldFilter.digitsOnly);
        
        widthField.changed(() -> {
            if(widthField.getText().isEmpty()) return;
            try {
                targetWidth = Math.max(1, Integer.parseInt(widthField.getText()));
                if(keepAspect && origWidth > 0 && origHeight > 0) {
                    targetHeight = Math.max(1, Math.round(targetWidth * ((float)origHeight / origWidth)));
                    heightField.setText(String.valueOf(targetHeight));
                }
            } catch(NumberFormatException ignored) {}
        });
        
        heightField.changed(() -> {
            if(heightField.getText().isEmpty()) return;
            try {
                targetHeight = Math.max(1, Integer.parseInt(heightField.getText()));
                if(keepAspect && origWidth > 0 && origHeight > 0) {
                    targetWidth = Math.max(1, Math.round(targetHeight * ((float)origWidth / origHeight)));
                    widthField.setText(String.valueOf(targetWidth));
                }
            } catch(NumberFormatException ignored) {}
        });
        
        sizeTable.add(Core.bundle.get("editor.mapart.width", "Width (1-3000):")).right().padRight(4f);
        sizeTable.add(widthField).width(90f).padRight(10f);
        sizeTable.add(Core.bundle.get("editor.mapart.height", "Height (1-3000):")).right().padRight(4f);
        sizeTable.add(heightField).width(90f).row();
        
        sizeTable.button(Core.bundle.get("editor.stencil.fitmap", "Fit Map Size"), () -> {
            targetWidth = Vars.editor.width();
            targetHeight = Vars.editor.height();
            if(keepAspect && origWidth > 0 && origHeight > 0) {
                float scale = Math.min((float)Vars.editor.width() / origWidth, (float)Vars.editor.height() / origHeight);
                targetWidth = Math.max(1, Math.round(origWidth * scale));
                targetHeight = Math.max(1, Math.round(origHeight * scale));
            }
            widthField.setText(String.valueOf(targetWidth));
            heightField.setText(String.valueOf(targetHeight));
        }).colspan(4).growX().padTop(4f).row();
        
        sizeTable.check("Keep Aspect Ratio", keepAspect, b -> keepAspect = b).colspan(4).left().padTop(4f);
        main.add(sizeTable).growX().row();
        
        dynamicOptionsTable = new Table(Tex.button);
        rebuildDynamicOptions();
        main.add(dynamicOptionsTable).growX().row();
        
        TextButton generateBtn = main.button(Core.bundle.get("editor.mapart.generate", "Generate Art on Map"), Icon.ok, () -> {
            if(selectedFile == null) return;
            hide();
            Time.runTask(2f, () -> {
                try {
                    MapArtGenerator.generate(selectedFile, targetWidth, targetHeight, style, layerMode, clearBlocks);
                } catch(Throwable e) {
                    Vars.ui.showException(e);
                }
            });
        }).growX().height(46f).padTop(6f).get();
        
        generateBtn.setDisabled(() -> selectedFile == null);
        
        ScrollPane pane = new ScrollPane(main);
        cont.add(pane).maxWidth(520f);
    }

    private void addStyleBtn(Table parent, ButtonGroup<TextButton> group, String text, ArtStyle s) {
        TextButton btn = new TextButton(text, Styles.togglet);
        btn.clicked(() -> {
            style = s;
            rebuildDynamicOptions();
        });
        if(style == s) btn.setChecked(true);
        group.add(btn);
        parent.add(btn);
    }

    private void rebuildDynamicOptions() {
        dynamicOptionsTable.clear();
        dynamicOptionsTable.defaults().left().pad(2f);
        
        if(style == ArtStyle.TERRAIN) {
            dynamicOptionsTable.add(Core.bundle.get("editor.mapart.layers", "Layer Mode:")).row();
            ButtonGroup<CheckBox> grp = new ButtonGroup<>();
            addLayerCheck(dynamicOptionsTable, grp, Core.bundle.get("editor.mapart.layer.both", "Floor + Walls"), LayerMode.FLOORS_AND_WALLS);
            addLayerCheck(dynamicOptionsTable, grp, Core.bundle.get("editor.mapart.layer.floor", "Floor Only"), LayerMode.FLOORS_ONLY);
            addLayerCheck(dynamicOptionsTable, grp, Core.bundle.get("editor.mapart.layer.wall", "Walls Only"), LayerMode.WALLS_ONLY);
            dynamicOptionsTable.check("Clear blocks in area", clearBlocks, b -> clearBlocks = b).padTop(4f).row();
        } else {
            dynamicOptionsTable.check("Clear blocks in area", clearBlocks, b -> clearBlocks = b).row();
        }
    }

    private void addLayerCheck(Table parent, ButtonGroup<CheckBox> group, String text, LayerMode m) {
        CheckBox box = new CheckBox(text);
        box.clicked(() -> layerMode = m);
        if(layerMode == m) box.setChecked(true);
        group.add(box);
        parent.add(box).row();
    }

    private void pickPNG(arc.func.Cons<Fi> cons) {
        try {
            Class<?> fc = null;
            try { fc = Class.forName("mindustry.ui.FileChooser"); } catch (Throwable ignored) {}
            if (fc == null) fc = Class.forName("mindustry.ui.dialogs.FileChooser");
            Method op = null;
            for (Method m : fc.getMethods()) if (m.getName().equals("open")) { op = m; break; }
            if (op != null) {
                Object fci = op.getParameterTypes()[0] == String.class ? op.invoke(null, "png") : op.invoke(null, (Object)new String[]{"png"});
                for (Method m : fci.getClass().getMethods()) if (m.getName().equals("submit")) { m.invoke(fci, cons); return; }
            }
        } catch (Throwable ignored) {}
        try {
            for (Method m : mindustry.core.Platform.class.getMethods()) {
                if (m.getName().equals("showFileChooser")) {
                    if (m.getParameterTypes().length == 3) { m.invoke(Vars.platform, true, "png", cons); return; }
                    if (m.getParameterTypes().length == 4) { m.invoke(Vars.platform, true, "Select PNG", "png", cons); return; }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void chooseImage() {
        pickPNG(file -> Core.app.post(() -> {
            try {
                Pixmap pix = new Pixmap(file);
                origWidth = pix.width;
                origHeight = pix.height;
                pix.dispose();
                selectedFile = file;
                fileLabel.setText(file.name() + " (" + origWidth + "x" + origHeight + ")");
                
                float scale = Math.min((float)Vars.editor.width() / origWidth, (float)Vars.editor.height() / origHeight);
                scale = Math.min(scale, 1f);
                targetWidth = Math.max(1, Math.round(origWidth * scale));
                targetHeight = Math.max(1, Math.round(origHeight * scale));
                
                widthField.setText(String.valueOf(targetWidth));
                heightField.setText(String.valueOf(targetHeight));
            } catch(Throwable e) {
                Vars.ui.showErrorMessage("Failed to load image: " + e.getMessage());
            }
        }));
    }
}
