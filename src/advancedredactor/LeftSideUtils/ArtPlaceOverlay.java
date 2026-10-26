package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import advancedredactor.upperuiutils.*;
import java.lang.reflect.*;
public class ArtPlaceOverlay{
    public static Texture texture;
    public static TextureRegion region;
    public static Pixmap currentPix;
    public static Fi currentFile;
    public static int targetW=100,targetH=100;
    public static float offsetX=0f,offsetY=0f;
    public static boolean enabled=false,moveMode=false;
    private static Table mobileBar;
    private static boolean dragging=false;
    private static int dragLastX,dragLastY;
    private static EditorTool savedTool;
    private static final Vec2 tmpA=new Vec2(),tmpB=new Vec2();
    private static MapArtDialog.ArtStyle currentStyle;
    private static MapArtDialog.LayerMode currentLayer;
    private static boolean currentClearBlocks;
    public static void init(){
        Events.run(Trigger.update,ArtPlaceOverlay::update);
    }
    public static void startPlacement(Fi file,int w,int h,MapArtDialog.ArtStyle style,MapArtDialog.LayerMode layer,boolean clearBlocks){
        currentStyle=style;
        currentLayer=layer;
        currentClearBlocks=clearBlocks;
        try{
            Pixmap src=new Pixmap(file);
            targetW=Math.max(1,w);targetH=Math.max(1,h);
            currentPix=scalePixmap(src,targetW,targetH);
            src.dispose();
            if(texture!=null)texture.dispose();
            texture=new Texture(currentPix);
            region=new TextureRegion(texture);
            currentFile=file;
            enabled=true;
            offsetX=(Vars.editor.width()-targetW)/2f;
            offsetY=(Vars.editor.height()-targetH)/2f;
            setMoveMode(true);
        }catch(Exception e){Vars.ui.showException(e);}
    }
    public static void stopPlacement(){
        enabled=false;
        setMoveMode(false);
        if(texture!=null){texture.dispose();texture=null;region=null;}
        if(currentPix!=null){currentPix.dispose();currentPix=null;}
        currentFile=null;
    }
    public static void setMoveMode(boolean v){
        MapView view=Vars.ui!=null&&Vars.ui.editor!=null?Vars.ui.editor.getView():null;
        if(!moveMode&&v&&view!=null){
            savedTool=view.getTool();
            view.setTool(EditorTool.pick);
        }else if(moveMode&&!v&&view!=null&&savedTool!=null){
            view.setTool(savedTool);
        }
        moveMode=v;
        dragging=false;
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            if(mobileBar!=null){mobileBar.remove();mobileBar=null;}
            dragging=false;
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        if(moveMode&&view.getTool()!=EditorTool.pick){
            savedTool=view.getTool();
            moveMode=false;
        }
        if(enabled&&region!=null&&(mobileBar==null||mobileBar.parent!=view.parent)){
            rebuildMobileBar(view.parent);
        }else if((!enabled||region==null)&&mobileBar!=null){
            mobileBar.remove();
            mobileBar=null;
        }
    }
    private static void rebuildMobileBar(Group parent){
        if(mobileBar!=null)mobileBar.remove();
        mobileBar=new Table(Tex.pane);
        mobileBar.margin(6f);
        mobileBar.label(()->Core.bundle.format("ar.art.draghint",(int)offsetX,(int)offsetY,targetW,targetH)).padRight(8f);
        mobileBar.button(Core.bundle.get("ar.art.applyhere","Stamp Here"),Icon.okSmall,Styles.flatt,()->{
            MapArtGenerator.generateAt(currentPix,(int)offsetX,(int)offsetY,targetW,targetH,currentStyle,currentLayer,currentClearBlocks);
            stopPlacement();
        }).height(34f).padRight(4f);
        mobileBar.button(Core.bundle.get("ar.art.cancel","Cancel"),Icon.cancelSmall,Styles.flatt,ArtPlaceOverlay::stopPlacement).height(34f);
        mobileBar.pack();
        parent.addChild(mobileBar);
        mobileBar.update(()->{
            mobileBar.setPosition(Core.graphics.getWidth()/2f,Core.graphics.getHeight()-Scl.scl(75f),Align.top);
            mobileBar.visible=enabled;
        });
    }
    public static void drawOnOverlay(MapView view,Element overlay){
        if(!enabled||region==null)return;
        try{
            Vec2 a=LandCopyPast.tileToOverlay(view,overlay,offsetX,offsetY,tmpA);
            Vec2 b=LandCopyPast.tileToOverlay(view,overlay,offsetX+targetW,offsetY+targetH,tmpB);
            float rx=Math.min(a.x,b.x),ry=Math.min(a.y,b.y);
            float rw=Math.abs(b.x-a.x),rh=Math.abs(b.y-a.y);
            Draw.color(1f,1f,1f,0.78f);
            Draw.rect(region,rx+rw/2f,ry+rh/2f,rw,rh);
            Lines.stroke(2.5f,Pal.accent);
            Lines.rect(rx,ry,rw,rh);
            Draw.reset();
        }catch(Throwable ignored){}
    }
    public static boolean onTouchDown(MapView view,float x,float y){
        if(!enabled||region==null||!moveMode)return false;
        Point2 p=LandCopyPast.projectRaw(view,x,y);
        dragging=true;
        dragLastX=p.x;
        dragLastY=p.y;
        return true;
    }
    public static boolean onTouchDragged(MapView view,float x,float y){
        if(!dragging)return false;
        Point2 p=LandCopyPast.projectRaw(view,x,y);
        offsetX+=p.x-dragLastX;
        offsetY+=p.y-dragLastY;
        dragLastX=p.x;
        dragLastY=p.y;
        return true;
    }
    public static boolean onTouchUp(){
        if(!dragging)return false;
        dragging=false;
        return true;
    }
    public static Pixmap scalePixmap(Pixmap src,int w,int h){
        Pixmap dst=new Pixmap(w,h);
        int sw=src.width,sh=src.height;
        for(int y=0;y<h;y++){
            int sy=Math.min(sh-1,(y*sh)/h);
            for(int x=0;x<w;x++)dst.setRaw(x,y,src.getRaw(Math.min(sw-1,(x*sw)/w),sy));
        }
        return dst;
    }
}
