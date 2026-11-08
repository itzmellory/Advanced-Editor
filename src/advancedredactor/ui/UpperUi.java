package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
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
    public static void init(){
        Events.run(Trigger.update,UpperUi::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            if(topBar!=null){
                topBar.remove();
                topBar=null;
            }
            lastView=null;
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        if(topBar==null||topBar.parent!=view.parent||lastView!=view)buildTopBar(view);
        if(topBar!=null){
            topBar.setSize(view.getWidth(),topBar.getHeight());
            topBar.setPosition(view.x,view.y+view.getHeight()-topBar.getHeight()-64f);
        }
    }
    private static void buildTopBar(MapView view){
        if(topBar!=null)topBar.remove();
        lastView=view;
        Group parent=view.parent;
        float availableWidth=Math.max(1f,view.getWidth()-8f);
        if(view.getWidth()<=0f)availableWidth=Math.max(1f,Core.graphics.getWidth()*0.65f);
        int shapeCount=BrushShape.values().length;
        int buttonCount=8+shapeCount+1;
        float buttonSize=Math.max(18f,Math.min(42f,(availableWidth-24f)/buttonCount));
        float buttonHeight=Math.max(24f,Math.min(42f,buttonSize));
        float padding=Math.max(0f,buttonSize/21f);
        topBar=new Table();
        topBar.setFillParent(false);
        topBar.top();
        topBar.touchable=Touchable.childrenOnly;
        Table bar=new Table(Tex.button);
        bar.margin(Math.max(2f,buttonSize*0.095f));
        bar.defaults().size(buttonSize,buttonHeight).pad(padding);
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
        bar.image(Tex.whiteui).width(Math.max(1f,buttonSize*0.045f)).height(buttonHeight*0.8f).padLeft(padding).padRight(padding);
        for(BrushShape shape:BrushShape.values()){
            TextButton shapeButton=bar.button(shape.symbol,Styles.flatTogglet,()->CustomShapeManager.currentShape=shape).tooltip(shape.title).size(buttonSize,buttonHeight).get();
            shapeButton.update(()->shapeButton.setChecked(CustomShapeManager.currentShape==shape));
        }
        bar.image(Tex.whiteui).width(Math.max(1f,buttonSize*0.045f)).height(buttonHeight*0.8f).padLeft(padding).padRight(padding);
        TextButton angleButton=bar.button("45°",Styles.flatTogglet,()->{
            CustomShapeManager.line45Mode=!CustomShapeManager.line45Mode;
            if(CustomShapeManager.line45Mode){
                LandCopyPast.setMode(Mode.none);
                view.setTool(EditorTool.line);
            }
        }).tooltip(Core.bundle.get("upper.line45","Strict 45° Line Mode")).size(buttonSize*1.2f,buttonHeight).get();
        angleButton.update(()->angleButton.setChecked(CustomShapeManager.line45Mode));
        ScrollPane pane=new ScrollPane(bar,Styles.noBarPane);
        pane.setScrollingDisabled(false,true);
        pane.setOverscroll(false,false);
        pane.setFadeScrollBars(false);
        topBar.add(pane).width(availableWidth).maxWidth(availableWidth).padTop(4f).center();
        topBar.pack();
        topBar.setSize(view.getWidth(),topBar.getHeight());
        topBar.setPosition(view.x,view.y+view.getHeight()-topBar.getHeight()-64f);
        parent.addChild(topBar);
    }
}
