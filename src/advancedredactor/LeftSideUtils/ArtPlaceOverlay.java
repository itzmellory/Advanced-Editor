package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import java.lang.reflect.*;
public class ArtPlaceOverlay extends Element{
    public static boolean active=false;
    private static ArtPlaceOverlay instance;
    private static Table floatingBar;
    private static Pixmap sourcePix;
    private static Texture previewTex;
    private static int tileX=0,tileY=0,targetW=64,targetH=64;
    private static MapArtDialog.ArtStyle currentStyle;
    private static MapArtDialog.LayerMode currentLayer;
    private static boolean currentClearBlocks;
    private boolean dragging=false;
    private int dragOffX=0,dragOffY=0;
    private Method projectMethod,unprojectMethod;
    public ArtPlaceOverlay(){
        setFillParent(true);
        try{
            projectMethod=MapView.class.getDeclaredMethod("project",float.class,float.class);
            unprojectMethod=MapView.class.getDeclaredMethod("unproject",float.class,float.class);
            projectMethod.setAccessible(true);
            unprojectMethod.setAccessible(true);
        }catch(Throwable ignored){}
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(!active||previewTex==null)return false;
                Vec2 mp=unproject(x,y);
                if(mp!=null){
                    int tx=(int)mp.x;
                    int ty=(int)mp.y;
                    if(tx>=tileX&&tx<=tileX+targetW&&ty>=tileY&&ty<=tileY+targetH){
                        dragging=true;
                        dragOffX=tx-tileX;
                        dragOffY=ty-tileY;
                        event.stop();
                        return true;
                    }
                }
                return false;
            }
            @Override
            public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(dragging){
                    Vec2 mp=unproject(x,y);
                    if(mp!=null){
                        tileX=Mathf.clamp((int)mp.x-dragOffX,0,Math.max(0,Vars.editor.width()-targetW));
                        tileY=Mathf.clamp((int)mp.y-dragOffY,0,Math.max(0,Vars.editor.height()-targetH));
                    }
                }
            }
            @Override
            public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                dragging=false;
            }
        });
    }
    public static void startPlacement(Fi file,int w,int h,MapArtDialog.ArtStyle style,MapArtDialog.LayerMode layer,boolean clearBlocks){
        if(Vars.ui==null||Vars.ui.editor==null)return;
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        try{
            if(sourcePix!=null)sourcePix.dispose();
            if(previewTex!=null)previewTex.dispose();
            sourcePix=new Pixmap(file);
            previewTex=new Texture(sourcePix);
        }catch(Throwable e){
            Vars.ui.showException(e);
            return;
        }
        targetW=Mathf.clamp(w,1,Vars.editor.width());
        targetH=Mathf.clamp(h,1,Vars.editor.height());
        tileX=Math.max(0,(Vars.editor.width()-targetW)/2);
        tileY=Math.max(0,(Vars.editor.height()-targetH)/2);
        currentStyle=style;
        currentLayer=layer;
        currentClearBlocks=clearBlocks;
        active=true;
        if(instance==null||instance.parent!=view.parent){
            if(instance!=null)instance.remove();
            instance=new ArtPlaceOverlay();
            int idx=view.parent.getChildren().indexOf(view,true);
            view.parent.addChildAt(idx+1,instance);
        }
        buildBar();
    }
    private static void buildBar(){
        if(floatingBar!=null)floatingBar.remove();
        floatingBar=new Table(Tex.pane);
        floatingBar.margin(6f);
        floatingBar.label(()->Core.bundle.format("ar.art.draghint",tileX,tileY,targetW,targetH)).padRight(8f);
        floatingBar.button("-10%",Styles.flatt,()->{
            targetW=Math.max(4,(int)(targetW*0.9f));
            targetH=Math.max(4,(int)(targetH*0.9f));
        }).size(60f,34f).padRight(4f);
        floatingBar.button("+10%",Styles.flatt,()->{
            targetW=Math.min(Vars.editor.width(),(int)(targetW*1.1f));
            targetH=Math.min(Vars.editor.height(),(int)(targetH*1.1f));
        }).size(60f,34f).padRight(6f);
        floatingBar.button(Core.bundle.get("ar.art.applyhere","Stamp Here"),Icon.okSmall,Styles.flatt,()->{
            MapArtGenerator.generateAt(sourcePix,tileX,tileY,targetW,targetH,currentStyle,currentLayer,currentClearBlocks);
            stopPlacement();
        }).height(34f).padRight(4f);
        floatingBar.button(Core.bundle.get("ar.art.cancel","Cancel"),Icon.cancelSmall,Styles.flatt,ArtPlaceOverlay::stopPlacement).height(34f);
        floatingBar.pack();
        Vars.ui.editor.addChild(floatingBar);
        floatingBar.update(()->{
            floatingBar.visible=active;
            floatingBar.setPosition(Core.graphics.getWidth()/2f,Core.graphics.getHeight()-Scl.scl(65f),Align.top);
        });
    }
    public static void stopPlacement(){
        active=false;
        if(floatingBar!=null)floatingBar.visible=false;
    }
    private Vec2 project(float tx,float ty){
        try{
            return (Vec2)projectMethod.invoke(Vars.ui.editor.getView(),tx,ty);
        }catch(Throwable e){
            return null;
        }
    }
    private Vec2 unproject(float sx,float sy){
        try{
            return (Vec2)unprojectMethod.invoke(Vars.ui.editor.getView(),sx,sy);
        }catch(Throwable e){
            return null;
        }
    }
    @Override
    public void act(float delta){
        super.act(delta);
        touchable=(active&&previewTex!=null)?Touchable.enabled:Touchable.disabled;
    }
    @Override
    public void draw(){
        if(!active||previewTex==null)return;
        Vec2 p0=project(0,0);
        Vec2 p1=project(1,1);
        if(p0==null||p1==null)return;
        float unit=p1.x-p0.x;
        float rx=p0.x+tileX*unit;
        float ry=p0.y+tileY*unit;
        float rw=targetW*unit;
        float rh=targetH*unit;
        Draw.color(1f,1f,1f,0.75f);
        Draw.rect(new TextureRegion(previewTex),rx+rw/2f,ry+rh/2f,rw,rh);
        Lines.stroke(2.5f,Pal.accent);
        Lines.rect(rx,ry,rw,rh);
        Draw.reset();
    }
}
