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
    public static Fi currentFile;
    public static int targetW=100,targetH=100;
    public static float offsetX=0f,offsetY=0f,opacity=0.45f;
    public static boolean enabled=true,moveMode=false;
    private static Element hookedView;
    private static Field offsetXField,offsetYField,zoomField;
    private static Method projectMethod;
    private static boolean dragging=false;
    private static float dragStartWorldX,dragStartWorldY,startOffsetX,startOffsetY;
    public static void init(){
        try{
            offsetXField=MapView.class.getDeclaredField("offsetx");
            offsetYField=MapView.class.getDeclaredField("offsety");
            zoomField=MapView.class.getDeclaredField("zoom");
            projectMethod=MapView.class.getDeclaredMethod("project",float.class,float.class);
            offsetXField.setAccessible(true);
            offsetYField.setAccessible(true);
            zoomField.setAccessible(true);
            projectMethod.setAccessible(true);
        }catch(Throwable ignored){}
        Events.run(Trigger.update,StencilManager::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown())return;
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        if(hookedView!=view){
            hookedView=view;
            injectOverlayElement(view);
        }
    }
    private static void injectOverlayElement(MapView view){
        Group parent=view.parent;
        Element overlay=new Element(){
            @Override public void draw(){
                if(!enabled||texture==null||!Vars.ui.editor.isShown())return;
                try{
                    float ox=offsetXField.getFloat(view);
                    float oy=offsetYField.getFloat(view);
                    float zoom=zoomField.getFloat(view);
                    float size=Math.min(view.getWidth(),view.getHeight());
                    float mapW=Vars.editor.width(),mapH=Vars.editor.height();
                    if(mapW<=0||mapH<=0)return;
                    float centerx=view.x+view.getWidth()/2f+ox*zoom;
                    float centery=view.y+view.getHeight()/2f+oy*zoom;
                    float scl=size/Math.max(mapW,mapH)*zoom;
                    float rx=centerx-mapW/2f*scl+offsetX*scl;
                    float ry=centery-mapH/2f*scl+offsetY*scl;
                    float rw=targetW*scl;
                    float rh=targetH*scl;
                    if(ScissorStack.push(Tmp.r1.set(view.x,view.y,view.getWidth(),view.getHeight()))){
                        Draw.color(1f,1f,1f,opacity);
                        Draw.rect(Draw.wrap(texture),rx+rw/2f,ry+rh/2f,rw,rh);
                        if(moveMode){
                            Lines.stroke(2f,Pal.accent);
                            Lines.rect(rx,ry,rw,rh);
                        }
                        Draw.reset();
                        ScissorStack.pop();
                    }
                }catch(Throwable ignored){}
            }
        };
        overlay.setFillParent(true);
        overlay.touchable=Touchable.disabled;
        parent.addChild(overlay);
        view.addCaptureListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(!enabled||texture==null||!moveMode||button!=KeyCode.mouseLeft)return false;
                Point2 p=project(view,x,y);
                if(p!=null){
                    dragging=true;
                    dragStartWorldX=p.x;
                    dragStartWorldY=p.y;
                    startOffsetX=offsetX;
                    startOffsetY=offsetY;
                    event.stop();
                    return true;
                }
                return false;
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(!dragging)return;
                Point2 p=project(view,x,y);
                if(p!=null){
                    offsetX=startOffsetX+(p.x-dragStartWorldX);
                    offsetY=startOffsetY+(p.y-dragStartWorldY);
                }
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                dragging=false;
            }
        });
    }
    private static Point2 project(MapView view,float x,float y){
        try{if(projectMethod!=null)return(Point2)projectMethod.invoke(view,x,y);}catch(Throwable ignored){}
        return null;
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
            Class<?> fc=Class.forName("mindustry.ui.dialogs.FileChooser");
            Method m=fc.getMethod("open",String.class);
            Object fci=m.invoke(null,"png");
            Method sub=fci.getClass().getMethod("submit",arc.func.Cons.class);
            sub.invoke(fci,cons);
            return;
        }catch(Throwable ignored){}
        try{
            for(Method m:Vars.platform.getClass().getMethods()){
                if(m.getName().equals("showFileChooser")){
                    if(m.getParameterTypes().length==3){
                        m.invoke(Vars.platform,true,"png",cons);return;
                    }else if(m.getParameterTypes().length==4){
                        m.invoke(Vars.platform,true,"Select PNG","png",cons);return;
                    }
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
            pickPNG(file->{
                Core.app.post(()->loadStencil(file,targetW,targetH));
            });
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
            if(texture!=null){texture.dispose();texture=null;}
            currentFile=null;
            moveMode=false;
        }).colspan(2).growX().height(38f).padTop(6f).row();
        d.cont.pane(t).maxHeight(Core.graphics.getHeight()*0.8f);
        d.addCloseButton();
        d.show();
    }
}
