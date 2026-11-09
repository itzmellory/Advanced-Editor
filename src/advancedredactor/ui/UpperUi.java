package advancedredactor.ui;
import arc.*;
import arc.math.geom.*;
import arc.scene.*;
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
        float[] bounds=getAvailableBounds(view);
        float width=Math.max(1f,bounds[2]-bounds[0]);
        if(topBar==null||topBar.parent!=view.parent||lastView!=view||Math.abs(lastWidth-width)>1f||Math.abs(lastHeight-view.getHeight())>1f)buildTopBar(view,width);
        positionTopBar(view,bounds);
    }
    private static float[] getAvailableBounds(MapView view){
        Vec2 viewBottom=view.localToStageCoordinates(new Vec2(0f,0f));
        Vec2 viewTop=view.localToStageCoordinates(new Vec2(view.getWidth(),view.getHeight()));
        float left=viewBottom.x;
        float right=viewTop.x;
        float center=(left+right)/2f;
        Element branch=view;
        Group group=view.parent;
        while(group!=null){
            for(Element element:group.getChildren()){
                if(element==branch||element==topBar||!element.visible)continue;
                if(element.getWidth()<=0f||element.getHeight()<view.getHeight()*0.35f)continue;
                Vec2 elementBottom=element.localToStageCoordinates(new Vec2(0f,0f));
                Vec2 elementTop=element.localToStageCoordinates(new Vec2(element.getWidth(),element.getHeight()));
                float overlap=Math.min(viewTop.y,elementTop.y)-Math.max(viewBottom.y,elementBottom.y);
                if(overlap<view.getHeight()*0.3f)continue;
                float elementCenter=(elementBottom.x+elementTop.x)/2f;
                if(elementCenter<center&&elementTop.x>left&&elementTop.x<=center&&elementBottom.x<center)left=Math.max(left,elementTop.x);
                if(elementCenter>center&&elementBottom.x<right&&elementBottom.x>=center&&elementTop.x>center)right=Math.min(right,elementBottom.x);
            }
            branch=group;
            group=group.parent;
        }
        Vec2 localBottom=view.parent.stageToLocalCoordinates(new Vec2(left,viewBottom.y));
        Vec2 localTop=view.parent.stageToLocalCoordinates(new Vec2(right,viewTop.y));
        return new float[]{localBottom.x,localBottom.y,localTop.x,localTop.y};
    }
    private static void removeTopBar(){
        if(topBar!=null){topBar.remove();topBar=null;}
        lastView=null;
        lastWidth=-1f;
        lastHeight=-1f;
    }
    private static void positionTopBar(MapView view,float[] bounds){
        if(topBar==null||topBar.parent!=view.parent)return;
        float width=Math.max(1f,bounds[2]-bounds[0]);
        topBar.setSize(width,topBar.getPrefHeight());
        topBar.setPosition(bounds[0],view.y+view.getHeight()-topBar.getHeight()-4f);
    }
    private static void buildTopBar(MapView view,float width){
        if(topBar!=null)topBar.remove();
        lastView=view;
        lastWidth=width;
        lastHeight=view.getHeight();
        Group parent=view.parent;
        topBar=new Table();
        topBar.setFillParent(false);
        topBar.setClip(true);
        topBar.top().left();
        topBar.touchable=Touchable.childrenOnly;
        Table bar=new Table(Tex.button);
        bar.margin(4f);
        bar.left();
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
        topBar.add(bar).width(width).left();
        topBar.pack();
        topBar.setSize(width,topBar.getPrefHeight());
        parent.addChild(topBar);
    }
}
