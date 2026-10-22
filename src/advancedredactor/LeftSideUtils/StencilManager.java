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
import arc.scene.style.*;
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
import advancedredactor.upperuiutils.*;
import java.lang.reflect.*;
public class StencilManager{
    public static Texture texture;
    public static TextureRegion region;
    public static Fi currentFile;
    public static int targetW=100,targetH=100;
    public static float offsetX=0f,offsetY=0f,opacity=0.45f;
    public static boolean enabled=true,moveMode=false;
    private static Element hookedView;
    private static Table mobileBar;
    private static boolean dragging=false;
    private static int dragLastX,dragLastY;
    private static EditorTool savedTool;
    public static void init(){
        Events.run(Trigger.update,StencilManager::update);
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
            hookedView=null;
            if(mobileBar!=null){mobileBar.remove();mobileBar=null;}
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        if(moveMode&&view.getTool()!=EditorTool.pick){
            savedTool=view.getTool();
            moveMode=false;
        }
        boolean hasOverlay=false;
        for(Element e:view.parent.getChildren()){if("stencil-overlay".equals(e.name)){hasOverlay=true;break;}}
        if(hookedView!=view||!hasOverlay){hookedView=view;injectOverlayElement(view);}
        if(enabled&&region!=null&&(mobileBar==null||mobileBar.parent!=view.parent)){
            rebuildMobileBar(view.parent);
        }else if((!enabled||region==null)&&mobileBar!=null){
            mobileBar.remove();
            mobileBar=null;
        }
    }
    private static void rebuildMobileBar(Group parent){
        if(mobileBar!=null)mobileBar.remove();
        mobileBar=new Table();
        mobileBar.setFillParent(true);
        mobileBar.bottom().left();
        mobileBar.touchable=Touchable.childrenOnly;
        Table box=new Table(Tex.button);
        box.margin(4f);
        TextButton moveBtn=box.button(Core.bundle.get("editor.stencil.movemode","Move Stencil"),Icon.move,Styles.flatTogglet,()->setMoveMode(!moveMode)).size(150f,42f).get();
        moveBtn.update(()->moveBtn.setChecked(moveMode));
        mobileBar.add(box).padLeft(8f).padBottom(8f);
        parent.addChild(mobileBar);
    }
    private static void injectOverlayElement(MapView view){
        Group parent=view.parent;
        Element overlay=new Element(){
            @Override public void draw(){
                if(!enabled||region==null||!Vars.ui.editor.isShown())return;
                try{
                    float[] g=new float[4];
                    if(!LandCopyPast.calcGridBounds(view,g))return;
                    float x0=view.x+g[0],y0=view.y+g[1],tileW=g[2],tileH=g[3];
                    float stencilX=x0+offsetX*tileW,stencilY=y0+offsetY*tileH;
                    float rw=targetW*tileW,rh=targetH*tileH;
                    if(ScissorStack.push(Tmp.r1.set(view.x,view.y,view.getWidth(),view.getHeight()))){
                        Draw.color(1f,1f,1f,opacity);
                        Draw.rect(region,stencilX+rw/2f,stencilY+rh/2f,rw,rh);
                        if(moveMode){
                            Lines.stroke(2f,dragging?Color.sky:Pal.accent);
                            Lines.rect(stencilX,stencilY,rw,rh);
                        }
                        Draw.reset();
                        ScissorStack.pop();
                    }
                }catch(Throwable ignored){}
            }
        };
        overlay.name="stencil-overlay";
        overlay.setFillParent(true);
        overlay.touchable=Touchable.disabled;
        parent.addChild(overlay);
        view.addCaptureListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||!enabled||region==null||!moveMode||button!=KeyCode.mouseLeft)return false;
                Point2 p=LandCopyPast.projectTile(view,x,y);
                dragging=true;
                dragLastX=p.x;
                dragLastY=p.y;
                LandCopyPast.cancelMapGestures(view);
                event.cancel();
                event.stop();
                return true;
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(pointer!=0||!dragging)return;
                Point2 p=LandCopyPast.projectTile(view,x,y);
                offsetX+=p.x-dragLastX;
                offsetY+=p.y-dragLastY;
                dragLastX=p.x;
                dragLastY=p.y;
                LandCopyPast.cancelMapGestures(view);
                event.cancel();
                event.stop();
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||!dragging)return;
                dragging=false;
                event.cancel();
                event.stop();
            }
        });
    }
    public static void loadStencil(Fi file,int w,int h){
        try{
            Pixmap src=new Pixmap(file);
            targetW=Math.max(1,w);targetH=Math.max(1,h);
            Pixmap scaled=scalePixmap(src,targetW,targetH);
            src.dispose();
            if(texture!=null)texture.dispose();
            texture=new Texture(scaled);
            region=new TextureRegion(texture);
            scaled.dispose();
            currentFile=file;enabled=true;
        }catch(Exception e){Vars.ui.showException("@editor.errorload",e);}
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
        Runnable[] rebuild=new Runnable[1];
        rebuild[0]=()->{
            t.clearChildren();
            t.defaults().pad(4f).left();
            if(targetW<=0)targetW=Vars.editor.width();
            if(targetH<=0)targetH=Vars.editor.height();
            if(region!=null){
                Table prevBox=new Table(Tex.pane);
                Image img=new Image(new TextureRegionDrawable(region));
                img.setScaling(Scaling.fit);
                prevBox.add(img).size(160f,160f).pad(4f);
                t.add(prevBox).colspan(2).center().padBottom(6f).row();
            }
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
                targetW=Vars.editor.width();targetH=Vars.editor.height();
                wf.setText(String.valueOf(targetW));hf.setText(String.valueOf(targetH));
                if(currentFile!=null)loadStencil(currentFile,targetW,targetH);
            }).colspan(2).growX().height(36f).row();
            t.button(Core.bundle.get("editor.stencil.selectpng","Select PNG..."),Icon.fileImage,Styles.flatt,()->{
                pickPNG(file->Core.app.post(()->{
                    loadStencil(file,targetW,targetH);
                    rebuild[0].run();
                }));
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
                offsetX=(Vars.editor.width()-targetW)/2f;offsetY=(Vars.editor.height()-targetH)/2f;
                oxf.setText(Strings.fixed(offsetX,0));oyf.setText(Strings.fixed(offsetY,0));
            }).colspan(2).growX().height(36f).row();
            t.check(Core.bundle.get("editor.stencil.enable","Show Stencil"),enabled,v->enabled=v).colspan(2).left().row();
            t.check(Core.bundle.get("editor.stencil.movemode","Drag / Move Mode"),moveMode,StencilManager::setMoveMode).colspan(2).left().row();
            t.button(Core.bundle.get("editor.stencil.clear","Clear Stencil"),Icon.trash,Styles.flatt,()->{
                if(texture!=null){texture.dispose();texture=null;region=null;}
                currentFile=null;
                setMoveMode(false);
                rebuild[0].run();
            }).colspan(2).growX().height(38f).padTop(6f).row();
        };
        rebuild[0].run();
        d.cont.pane(t).maxHeight(Core.graphics.getHeight()*0.82f);
        d.addCloseButton();
        d.show();
    }
}
