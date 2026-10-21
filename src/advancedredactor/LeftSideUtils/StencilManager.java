package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import java.lang.reflect.*;
public class StencilManager{
    public static Texture texture;
    public static TextureRegion region;
    public static Fi currentFile;
    public static int targetW=100,targetH=100;
    public static float offsetX=0f,offsetY=0f,opacity=0.45f;
    public static boolean enabled=true,moveMode=false;
    private static Element hookedView;
    private static boolean dragging=false;
    private static float dragStartWorldX,dragStartWorldY,startOffsetX,startOffsetY;
    public static void init(){
        Events.run(Trigger.update,StencilManager::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            hookedView=null;
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        boolean hasOverlay=false;
        for(Element e:view.parent.getChildren()){
            if("stencil-overlay".equals(e.name)){
                hasOverlay=true;
                break;
            }
        }
        if(hookedView!=view||!hasOverlay){
            hookedView=view;
            injectOverlayElement(view);
        }
    }
    private static void injectOverlayElement(MapView view){
        Group parent=view.parent;
        Element overlay=new Element(){
            @Override public void draw(){
                if(!enabled||region==null||!Vars.ui.editor.isShown())return;
                try{
                    Float ox=(Float)Reflect.get(view,"offsetx");
                    Float oy=(Float)Reflect.get(view,"offsety");
                    Float zoom=(Float)Reflect.get(view,"zoom");
                    if(ox==null||oy==null||zoom==null)return;
                    float mapW=Vars.editor.width(),mapH=Vars.editor.height();
                    if(mapW<=0||mapH<=0)return;
                    float size=Math.min(view.getWidth(),view.getHeight());
                    float scl=size/Math.max(mapW,mapH)*zoom;
                    float cx=view.getWidth()/2f+ox*zoom;
                    float cy=view.getHeight()/2f+oy*zoom;
                    float mapBlX=cx-mapW*scl/2f;
                    float mapBlY=cy-mapH*scl/2f;
                    float stencilX=view.x+mapBlX+offsetX*scl;
                    float stencilY=view.y+mapBlY+offsetY*scl;
                    float rw=targetW*scl;
                    float rh=targetH*scl;
                    Draw.color(1f,1f,1f,opacity);
                    Draw.rect(region,stencilX+rw/2f,stencilY+rh/2f,rw,rh);
                    if(moveMode){
                        Lines.stroke(2f,Pal.accent);
                        Lines.rect(stencilX,stencilY,rw,rh);
                    }
                    Draw.reset();
                }catch(Throwable ignored){}
            }
        };
        overlay.name="stencil-overlay";
        parent.addChild(overlay);
        view.addCaptureListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(!enabled||region==null||!moveMode||button!=KeyCode.mouseLeft)return false;
                try{
                    Float ox=(Float)Reflect.get(view,"offsetx");
                    Float oy=(Float)Reflect.get(view,"offsety");
                    Float zoom=(Float)Reflect.get(view,"zoom");
                    if(ox==null)return false;
                    float mapW=Vars.editor.width(),mapH=Vars.editor.height();
                    float size=Math.min(view.getWidth(),view.getHeight());
                    float scl=size/Math.max(mapW,mapH)*zoom;
                    float cx=view.getWidth()/2f+ox*zoom;
                    float cy=view.getHeight()/2f+oy*zoom;
                    float mapBlX=cx-mapW*scl/2f;
                    float mapBlY=cy-mapH*scl/2f;
                    float tx=(x-mapBlX)/scl;
                    float ty=(y-mapBlY)/scl;
                    dragging=true;
                    dragStartWorldX=tx;
                    dragStartWorldY=ty;
                    startOffsetX=offsetX;
                    startOffsetY=offsetY;
                    event.stop();
                    return true;
                }catch(Throwable ignored){return false;}
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(!dragging)return;
                try{
                    Float ox=(Float)Reflect.get(view,"offsetx");
                    Float oy=(Float)Reflect.get(view,"offsety");
                    Float zoom=(Float)Reflect.get(view,"zoom");
                    float mapW=Vars.editor.width(),mapH=Vars.editor.height();
                    float size=Math.min(view.getWidth(),view.getHeight());
                    float scl=size/Math.max(mapW,mapH)*zoom;
                    float cx=view.getWidth()/2f+ox*zoom;
                    float cy=view.getHeight()/2f+oy*zoom;
                    float mapBlX=cx-mapW*scl/2f;
                    float mapBlY=cy-mapH*scl/2f;
                    float tx=(x-mapBlX)/scl;
                    float ty=(y-mapBlY)/scl;
                    offsetX=startOffsetX+(tx-dragStartWorldX);
                    offsetY=startOffsetY+(ty-dragStartWorldY);
                }catch(Throwable ignored){}
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                dragging=false;
            }
        });
    }
    public static void loadStencil(Fi file,int w,int h){
        try{
            Pixmap src=new Pixmap(file);
            targetW=Math.max(1,w);
            targetH=Math.max(1,h);
            Pixmap scaled=scalePixmap(src,targetW,targetH);
            src.dispose();
            if(texture!=null)texture.dispose();
            texture=new Texture(scaled);
            region=new TextureRegion(texture);
            scaled.dispose();
            currentFile=file;
            enabled=true;
        }catch(Exception e){
            Vars.ui.showException("@editor.errorload",e);
        }
    }
    public static Pixmap scalePixmap(Pixmap src,int w,int h){
        Pixmap dst=new Pixmap(w,h);
        int sw=src.width,sh=src.height;
        for(int y=0;y<h;y++){
            int sy=Math.min(sh-1,(y*sh)/h);
            for(int x=0;x<w;x++){
                int sx=Math.min(sw-1,(x*sw)/w);
                dst.setRaw(x,y,src.getRaw(sx,sy));
            }
        }
        return dst;
    }
    private static void pickPNG(arc.func.Cons<Fi> cons){
        try{
            Class<?> fc=null;
            try{fc=Class.forName("mindustry.ui.FileChooser");}catch(Throwable ignored){}
            if(fc==null)fc=Class.forName("mindustry.ui.dialogs.FileChooser");
            Method op=null;
            for(Method m:fc.getMethods())if(m.getName().equals("open")){op=m;break;}
            if(op!=null){
                Object fci=op.getParameterTypes()[0]==String.class?op.invoke(null,"png"):op.invoke(null,(Object)new String[]{"png"});
                for(Method m:fci.getClass().getMethods())if(m.getName().equals("submit")){m.invoke(fci,cons);return;}
            }
        }catch(Throwable ignored){}
        try{
            for(Method m:mindustry.core.Platform.class.getMethods()){
                if(m.getName().equals("showFileChooser")){
                    if(m.getParameterTypes().length==3){m.invoke(Vars.platform,true,"png",cons);return;}
                    if(m.getParameterTypes().length==4){m.invoke(Vars.platform,true,"Select PNG","png",cons);return;}
                }
            }
        }catch(Throwable ignored){}
    }
    public static void showDialog(){
        BaseDialog d=new BaseDialog(Core.bundle.get("editor.stencil.title","Stencil Setup"));
        Table t=new Table();
        t.defaults().pad(4f).left();
        if(targetW<=0)targetW=Vars.editor.width();
        if(targetH<=0)targetH=Vars.editor.height();
        t.add(Core.bundle.get("editor.stencil.width","Width (tiles):"));
        TextField wf=t.field(String.valueOf(targetW),TextField.TextFieldFilter.digitsOnly,s->{
            targetW=Math.max(1,Strings.parseInt(s,targetW));
            if(currentFile!=null)loadStencil(currentFile,targetW,targetH);
        }).width(140f).get();
        t.row();
        t.add(Core.bundle.get("editor.stencil.height","Height (tiles):"));
        TextField hf=t.field(String.valueOf(targetH),TextField.TextFieldFilter.digitsOnly,s->{
            targetH=Math.max(1,Strings.parseInt(s,targetH));
            if(currentFile!=null)loadStencil(currentFile,targetW,targetH);
        }).width(140f).get();
        t.row();
        t.button(Core.bundle.get("editor.stencil.fitmap","Fit Map Size"),Styles.flatt,()->{
            targetW=Vars.editor.width();
            targetH=Vars.editor.height();
            wf.setText(String.valueOf(targetW));
            hf.setText(String.valueOf(targetH));
            if(currentFile!=null)loadStencil(currentFile,targetW,targetH);
        }).colspan(2).growX().height(36f).row();
        t.button(Core.bundle.get("editor.stencil.selectpng","Select PNG..."),Icon.fileImage,Styles.flatt,()->{
            pickPNG(file->Core.app.post(()->loadStencil(file,targetW,targetH)));
        }).colspan(2).growX().height(42f).row();
        t.add(Core.bundle.get("editor.stencil.opacity","Opacity:"));
        t.slider(0.05f,1f,0.05f,opacity,v->opacity=v).width(160f).row();
        t.add(Core.bundle.get("editor.stencil.offsetx","Offset X:"));
        TextField oxf=t.field(Strings.fixed(offsetX,0),s->offsetX=Strings.parseFloat(s,offsetX)).width(140f).get();
        t.row();
        t.add(Core.bundle.get("editor.stencil.offsety","Offset Y:"));
        TextField oyf=t.field(Strings.fixed(offsetY,0),s->offsetY=Strings.parseFloat(s,offsetY)).width(140f).get();
        t.row();
        t.button(Core.bundle.get("editor.stencil.center","Center on Map"),Styles.flatt,()->{
            offsetX=(Vars.editor.width()-targetW)/2f;
            offsetY=(Vars.editor.height()-targetH)/2f;
            oxf.setText(Strings.fixed(offsetX,0));
            oyf.setText(Strings.fixed(offsetY,0));
        }).colspan(2).growX().height(36f).row();
        t.check(Core.bundle.get("editor.stencil.enable","Show Stencil"),enabled,v->enabled=v).colspan(2).left().row();
        t.check(Core.bundle.get("editor.stencil.movemode","Drag / Move Mode"),moveMode,v->moveMode=v).colspan(2).left().row();
        t.button(Core.bundle.get("editor.stencil.clear","Clear Stencil"),Icon.trash,Styles.flatt,()->{
            if(texture!=null){texture.dispose();texture=null;region=null;}
            currentFile=null;
            moveMode=false;
        }).colspan(2).growX().height(38f).padTop(6f).row();
        d.cont.pane(t).maxHeight(Core.graphics.getHeight()*0.8f);
        d.addCloseButton();
        d.show();
    }
}
