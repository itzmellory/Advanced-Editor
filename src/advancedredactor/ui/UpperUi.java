package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import advancedredactor.upperuiutils.*;
import advancedredactor.upperuiutils.LandCopyPast.*;
import advancedredactor.upperuiutils.CustomShapeManager.*;
public class UpperUi{
    private static Table topBar;
    private static MapView lastView;
    private static float lastWidth=-1f;
    private static float lastHeight=-1f;
    public static void init(){
        Events.run(Trigger.update,UpperUi::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            removeTopBar();
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null||view.getWidth()<=0f||view.getHeight()<=0f)return;
        float width=view.getWidth(),height=view.getHeight();
        if(topBar==null||topBar.parent!=view.parent||lastView!=view||Math.abs(lastWidth-width)>1f||Math.abs(lastHeight-height)>1f)buildTopBar(view);
        positionTopBar(view);
    }
    private static void removeTopBar(){
        if(topBar!=null){topBar.remove();topBar=null;}
        lastView=null;
        lastWidth=-1f;
        lastHeight=-1f;
    }
    private static void positionTopBar(MapView view){
        if(topBar==null||topBar.parent!=view.parent)return;
        topBar.setSize(view.getWidth(),topBar.getPrefHeight());
        topBar.setPosition(view.x,view.y+view.getHeight()-topBar.getHeight()-4f);
    }
    private static void buildTopBar(MapView view){
        if(topBar!=null)topBar.remove();
        Group parent=view.parent;
        lastView=view;
        lastWidth=view.getWidth();
        lastHeight=view.getHeight();
        topBar=new Table();
        topBar.setFillParent(false);
        topBar.top().left();
        topBar.touchable=Touchable.childrenOnly;
        Table bar=new Table(Tex.button);
        bar.margin(4f);
        bar.defaults().size(42f,42f).pad(2f);
        ImageButton copyBtn=bar.button(Icon.copy,Styles.squareTogglei,()->LandCopyPast.toggleMode(Mode.copy)).tooltip(Core.bundle.get("upper.copy","Copy Terrain")).get();
        copyBtn.update(()->copyBtn.setChecked(LandCopyPast.mode==Mode.copy));
        ImageButton cutBtn=bar.button(Icon.trash,Styles.squareTogglei,()->LandCopyPast.toggleMode(Mode.cut)).tooltip(Core.bundle.get("upper.cut","Cut Terrain")).get();
        cutBtn.update(()->cutBtn.setChecked(LandCopyPast.mode==Mode.cut));
        ImageButton pasteBtn=bar.button(Icon.paste,Styles.squareTogglei,()->LandCopyPast.toggleMode(Mode.paste)).tooltip(Core.bundle.get("upper.paste","Paste Terrain")).get();
        pasteBtn.setDisabled(()->LandCopyPast.clipboard==null);
        pasteBtn.update(()->pasteBtn.setChecked(LandCopyPast.mode==Mode.paste));
        bar.button(Icon.rotate,Styles.flati,LandCopyPast::rotateClipboard).tooltip(Core.bundle.get("upper.rotate","Rotate")).disabled(b->LandCopyPast.clipboard==null);
        bar.button(Icon.flipX,Styles.flati,LandCopyPast::flipX).tooltip(Core.bundle.get("upper.flipx","Flip X")).disabled(b->LandCopyPast.clipboard==null);
        bar.button(Icon.flipY,Styles.flati,LandCopyPast::flipY).tooltip(Core.bundle.get("upper.flipy","Flip Y")).disabled(b->LandCopyPast.clipboard==null);
        bar.button(Icon.save,Styles.flati,TerrainSchematicsDialog::show).tooltip(Core.bundle.get("upper.schematics","Terrain Schematics"));
        bar.button(Icon.settings,Styles.flati,LandCopyPast::showCopyPasteSettings).tooltip(Core.bundle.get("upper.settings","Copy/Paste Options"));
        bar.image(Tex.whiteui).width(2f).height(34f).padLeft(4f).padRight(4f);
        for(BrushShape shape:BrushShape.values()){
            TextButton shapeButton=bar.button(shape.symbol,Styles.flatTogglet,()->CustomShapeManager.currentShape=shape).tooltip(shape.title).size(42f,42f).get();
            shapeButton.update(()->shapeButton.setChecked(CustomShapeManager.currentShape==shape));
        }
        bar.image(Tex.whiteui).width(2f).height(34f).padLeft(4f).padRight(4f);
        TextButton angleButton=bar.button("45°",Styles.flatTogglet,()->{
            CustomShapeManager.line45Mode=!CustomShapeManager.line45Mode;
            if(CustomShapeManager.line45Mode){
                LandCopyPast.setMode(Mode.none);
                view.setTool(EditorTool.line);
            }
        }).tooltip(Core.bundle.get("upper.line45","Strict 45° Line Mode")).size(52f,42f).get();
        angleButton.update(()->angleButton.setChecked(CustomShapeManager.line45Mode));
        ScrollPane pane=new ScrollPane(bar,Styles.noBarPane);
        pane.setScrollingDisabled(false,true);
        pane.setOverscroll(false,false);
        pane.setFadeScrollBars(false);
        topBar.add(pane).width(view.getWidth()).padTop(6f).left();
        topBar.pack();
        topBar.setSize(view.getWidth(),topBar.getPrefHeight());
        parent.addChild(topBar);
        positionTopBar(view);
    }
}
