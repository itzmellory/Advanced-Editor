package advancedredactor.upperuiutils;
import arc.*;
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
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.LeftSideUtils.*;
import advancedredactor.upperuiutils.CustomShapeManager.*;
import java.lang.reflect.*;
public class LandCopyPast{
    public enum Mode{none,copy,cut,paste}
    public static Mode mode=Mode.none;
    public static Clip clipboard;
    public static boolean copyFloor=true,copyOre=true,copyBlock=true;
    public static boolean overrideBlock=true,overrideOre=true,useEditorTeam=false;
    private static boolean selecting=false,drawingCustom=false,drawingLine=false,draggingPaste=false;
    private static int selStartX,selStartY,selEndX,selEndY;
    private static int lineStartX,lineStartY,lineEndX,lineEndY;
    private static int lastDrawX=-1,lastDrawY=-1;
    private static int pasteOriginX=0,pasteOriginY=0,dragLastX,dragLastY;
    private static boolean camLocked=false;
    private static float lockedOx,lockedOy,lockedZoom;
    private static Table pasteActionBar;
    private static Element overlayElement;
    private static MapView hookedView;
    private static EditorTool savedVanillaTool;
    private static Field extraDataField,floorDataField,overlayDataField,gestureField;
    private static final Vec2 tmpV1=new Vec2(),tmpV2=new Vec2();
    private static final Point2 tmpP1=new Point2();
    static{
        try{extraDataField=Tile.class.getDeclaredField("extraData");extraDataField.setAccessible(true);}catch(Throwable ignored){}
        try{floorDataField=Tile.class.getDeclaredField("floorData");floorDataField.setAccessible(true);}catch(Throwable ignored){}
        try{overlayDataField=Tile.class.getDeclaredField("overlayData");overlayDataField.setAccessible(true);}catch(Throwable ignored){}
        try{gestureField=MapView.class.getDeclaredField("gestureDetector");gestureField.setAccessible(true);}catch(Throwable ignored){}
    }
    public static void init(){
        Events.run(Trigger.update,LandCopyPast::update);
    }
    public static Element getOverlayElement(){
        return overlayElement;
    }
    public static int getExtra(Tile t){try{if(extraDataField!=null)return extraDataField.getInt(t);}catch(Throwable ignored){}return 0;}
    public static void setExtra(Tile t,int val){try{if(extraDataField!=null)extraDataField.setInt(t,val);}catch(Throwable ignored){}}
    public static byte getFloorData(Tile t){try{if(floorDataField!=null)return floorDataField.getByte(t);}catch(Throwable ignored){}return 0;}
    public static void setFloorData(Tile t,byte val){try{if(floorDataField!=null)floorDataField.setByte(t,val);}catch(Throwable ignored){}}
    public static byte getOverlayData(Tile t){try{if(overlayDataField!=null)return overlayDataField.getByte(t);}catch(Throwable ignored){}return 0;}
    public static void setOverlayData(Tile t,byte val){try{if(overlayDataField!=null)overlayDataField.setByte(t,val);}catch(Throwable ignored){}}
    public static void cancelMapGestures(MapView view){
        try{
            if(gestureField!=null){
                GestureDetector gd=(GestureDetector)gestureField.get(view);
                if(gd!=null)gd.cancel();
            }
        }catch(Throwable ignored){}
    }
    public static void lockCamera(MapView view){
        try{
            lockedOx=Reflect.get(MapView.class,view,"offsetx");
            lockedOy=Reflect.get(MapView.class,view,"offsety");
            lockedZoom=Reflect.get(MapView.class,view,"zoom");
            camLocked=true;
            cancelMapGestures(view);
        }catch(Throwable ignored){}
    }
    public static void unlockCamera(){
        camLocked=false;
    }
    public static void enforceCameraLock(MapView view){
        if(!camLocked||view==null)return;
        try{
            Reflect.set(MapView.class,view,"offsetx",lockedOx);
            Reflect.set(MapView.class,view,"offsety",lockedOy);
            Reflect.set(MapView.class,view,"zoom",lockedZoom);
        }catch(Throwable ignored){}
    }
    public static boolean calcGridBounds(MapView view,float[] out){
        try{
            float ox=Reflect.get(MapView.class,view,"offsetx");
            float oy=Reflect.get(MapView.class,view,"offsety");
            float zoom=Reflect.get(MapView.class,view,"zoom");
            int mw=Vars.editor.width(),mh=Vars.editor.height();
            if(mw<=0||mh<=0)return false;
            float ratio=1f/((float)mw/mh);
            float size=Math.min(view.getWidth(),view.getHeight());
            float sclw=size*zoom;
            float sclh=size*zoom*ratio;
            float localBlX=view.getWidth()/2f+ox*zoom-sclw/2f;
            float localBlY=view.getHeight()/2f+oy*zoom-sclh/2f;
            out[0]=localBlX;
            out[1]=localBlY;
            out[2]=sclw/mw;
            out[3]=sclh/mh;
            return true;
        }catch(Throwable e){
            return false;
        }
    }
    public static Point2 projectTile(MapView view,float localX,float localY){
        return projectRaw(view,localX,localY);
    }
    public static Point2 projectRaw(MapView view,float x,float y){
        try{
            float offsetx=Reflect.get(MapView.class,view,"offsetx");
            float offsety=Reflect.get(MapView.class,view,"offsety");
            float zoom=Reflect.get(MapView.class,view,"zoom");
            float ratio=1f/((float)Vars.editor.width()/Vars.editor.height());
            float size=Math.min(view.getWidth(),view.getHeight());
            float sclwidth=size*zoom;
            float sclheight=size*zoom*ratio;
            float wx=(x-view.getWidth()/2f+sclwidth/2f-offsetx*zoom)/sclwidth*Vars.editor.width();
            float wy=(y-view.getHeight()/2f+sclheight/2f-offsety*zoom)/sclheight*Vars.editor.height();
            return tmpP1.set(Mathf.floor(wx),Mathf.floor(wy));
        }catch(Throwable e){
            return tmpP1.set(0,0);
        }
    }
    public static Point2 currentMouseTile(MapView view){
        Vec2 local=view.screenToLocalCoordinates(tmpV1.set(Core.input.mouseX(),Core.input.mouseY()));
        return projectRaw(view,local.x,local.y);
    }
    public static Vec2 tileToOverlay(MapView view,Element overlay,float tx,float ty,Vec2 out){
        try{
            float offsetx=Reflect.get(MapView.class,view,"offsetx");
            float offsety=Reflect.get(MapView.class,view,"offsety");
            float zoom=Reflect.get(MapView.class,view,"zoom");
            float ratio=1f/((float)Vars.editor.width()/Vars.editor.height());
            float size=Math.min(view.getWidth(),view.getHeight());
            float sclwidth=size*zoom;
            float sclheight=size*zoom*ratio;
            float vx=(tx/Vars.editor.width())*sclwidth+offsetx*zoom-sclwidth/2f+view.getWidth()/2f;
            float vy=(ty/Vars.editor.height())*sclheight+offsety*zoom-sclheight/2f+view.getHeight()/2f;
            Vec2 stage=view.localToStageCoordinates(out.set(vx,vy));
            if(overlay!=null)return overlay.stageToLocalCoordinates(stage);
            return stage;
        }catch(Throwable e){
            return out.set(0f,0f);
        }
    }
    private static void refreshRenderer(){
        try{
            Object r=Reflect.get(Vars.editor,"renderer");
            if(r!=null)Reflect.invoke(r,"resize",new Object[]{Vars.editor.width(),Vars.editor.height()},int.class,int.class);
        }catch(Throwable ignored){}
    }
    public static void toggleMode(Mode m){setMode(mode==m?Mode.none:m);}
    public static void setMode(Mode m){
        MapView view=Vars.ui!=null&&Vars.ui.editor!=null?Vars.ui.editor.getView():null;
        if(mode==Mode.none&&m!=Mode.none&&view!=null){
            savedVanillaTool=view.getTool();
            view.setTool(EditorTool.pick);
        }else if(mode!=Mode.none&&m==Mode.none&&view!=null&&savedVanillaTool!=null){
            view.setTool(savedVanillaTool);
        }
        mode=m;
        selecting=false;
        draggingPaste=false;
        unlockCamera();
        if(m!=Mode.none)CustomShapeManager.line45Mode=false;
        if(m==Mode.paste&&clipboard!=null){
            if(view!=null){
                Point2 c=projectRaw(view,view.getWidth()*0.5f,view.getHeight()*0.5f);
                pasteOriginX=c.x-clipboard.width/2;
                pasteOriginY=c.y-clipboard.height/2;
            }else{
                pasteOriginX=(Vars.editor.width()-clipboard.width)/2;
                pasteOriginY=(Vars.editor.height()-clipboard.height)/2;
            }
            rebuildPasteBar();
        }else removePasteBar();
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            hookedView=null;
            if(overlayElement!=null){overlayElement.remove();overlayElement=null;}
            removePasteBar();
            unlockCamera();
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null)return;
        enforceCameraLock(view);
        if(mode!=Mode.none&&view.getTool()!=EditorTool.pick){
            savedVanillaTool=view.getTool();
            setMode(Mode.none);
        }
        if(overlayElement==null||overlayElement.parent!=Vars.ui.editor){
            if(overlayElement!=null)overlayElement.remove();
            overlayElement=new Element(){
                @Override public void draw(){
                    if(!Vars.ui.editor.isShown())return;
                    enforceCameraLock(view);
                    Vec2 s1=view.localToStageCoordinates(tmpV1.set(0f,0f));
                    Vec2 s2=view.localToStageCoordinates(tmpV2.set(view.getWidth(),view.getHeight()));
                    Vec2 l1=stageToLocalCoordinates(s1);
                    Vec2 l2=stageToLocalCoordinates(s2);
                    float cx=Math.min(l1.x,l2.x),cy=Math.min(l1.y,l2.y);
                    float cw=Math.abs(l2.x-l1.x),ch=Math.abs(l2.y-l1.y);
                    if(ScissorStack.push(Tmp.r1.set(cx,cy,cw,ch))){
                        StencilManager.drawOnOverlay(view,this);
                        drawOverlayContent(view,this);
                        ScissorStack.pop();
                    }
                }
            };
            overlayElement.touchable=Touchable.disabled;
            Vars.ui.editor.addChild(overlayElement);
        }
        overlayElement.setBounds(0f,0f,Vars.ui.editor.getWidth(),Vars.ui.editor.getHeight());
        if(hookedView!=view){
            hookedView=view;
            injectViewHooks(view);
        }
    }
    private static void rebuildPasteBar(){
        removePasteBar();
        if(Vars.ui==null||Vars.ui.editor==null||Vars.ui.editor.getView()==null)return;
        Group parent=Vars.ui.editor.getView().parent;
        pasteActionBar=new Table();
        pasteActionBar.setFillParent(true);
        pasteActionBar.bottom();
        pasteActionBar.touchable=Touchable.childrenOnly;
        Table box=new Table(Tex.button);
        box.margin(4f);
        box.defaults().height(42f).pad(3f);
        box.button(Core.bundle.get("upper.apply","Paste Here"),Icon.ok,Styles.flatt,()->pasteAt(pasteOriginX,pasteOriginY)).width(150f);
        box.button(Core.bundle.get("upper.cancel","Cancel"),Icon.cancel,Styles.flatt,()->setMode(Mode.none)).width(120f);
        pasteActionBar.add(box).padBottom(12f);
        parent.addChild(pasteActionBar);
    }
    private static void removePasteBar(){
        if(pasteActionBar!=null){pasteActionBar.remove();pasteActionBar=null;}
    }
    private static boolean shouldInterceptVanillaTool(EditorTool tool){
        if(tool==EditorTool.line&&(CustomShapeManager.line45Mode||CustomShapeManager.currentShape!=BrushShape.circle||Vars.editor.brushSize>20f))return true;
        if((tool==EditorTool.pencil||tool==EditorTool.eraser||tool==EditorTool.spray)&&(CustomShapeManager.currentShape!=BrushShape.circle&&!(tool==EditorTool.pencil&&tool.mode==2&&CustomShapeManager.currentShape==BrushShape.square)))return true;
        return false;
    }
    private static void injectViewHooks(MapView view){
        view.addCaptureListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||button!=KeyCode.mouseLeft)return false;
                if(StencilManager.onTouchDown(view,x,y)){
                    lockCamera(view);
                    event.cancel();
                    event.stop();
                    return true;
                }
                Point2 p=projectRaw(view,x,y);
                if(mode==Mode.copy||mode==Mode.cut){
                    selecting=true;
                    selStartX=selEndX=p.x;
                    selStartY=selEndY=p.y;
                    lockCamera(view);
                    event.cancel();
                    event.stop();
                    return true;
                }
                if(mode==Mode.paste&&clipboard!=null){
                    draggingPaste=true;
                    dragLastX=p.x;
                    dragLastY=p.y;
                    lockCamera(view);
                    event.cancel();
                    event.stop();
                    return true;
                }
                EditorTool tool=view.getTool();
                if(shouldInterceptVanillaTool(tool)){
                    if(tool==EditorTool.line){
                        drawingLine=true;
                        lineStartX=lineEndX=p.x;
                        lineStartY=lineEndY=p.y;
                    }else{
                        drawingCustom=true;
                        lastDrawX=p.x;
                        lastDrawY=p.y;
                        applyToolStamp(tool,p.x,p.y);
                    }
                    lockCamera(view);
                    event.cancel();
                    event.stop();
                    return true;
                }
                return false;
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(pointer!=0)return;
                enforceCameraLock(view);
                if(StencilManager.onTouchDragged(view,x,y)){
                    enforceCameraLock(view);
                    event.cancel();
                    event.stop();
                    return;
                }
                Point2 p=projectRaw(view,x,y);
                if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                    selEndX=p.x;
                    selEndY=p.y;
                    event.cancel();
                    event.stop();
                    return;
                }
                if(mode==Mode.paste&&draggingPaste){
                    pasteOriginX+=p.x-dragLastX;
                    pasteOriginY+=p.y-dragLastY;
                    dragLastX=p.x;
                    dragLastY=p.y;
                    event.cancel();
                    event.stop();
                    return;
                }
                if(drawingLine){
                    if(CustomShapeManager.line45Mode){
                        Point2 snapped=CustomShapeManager.snap45(lineStartX,lineStartY,p.x,p.y);
                        lineEndX=snapped.x;
                        lineEndY=snapped.y;
                    }else{
                        lineEndX=p.x;
                        lineEndY=p.y;
                    }
                    event.cancel();
                    event.stop();
                    return;
                }
                if(drawingCustom){
                    EditorTool tool=view.getTool();
                    CustomShapeManager.traceLine(lastDrawX,lastDrawY,p.x,p.y,(lx,ly)->applyToolStamp(tool,lx,ly));
                    lastDrawX=p.x;
                    lastDrawY=p.y;
                    event.cancel();
                    event.stop();
                }
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||button!=KeyCode.mouseLeft)return;
                enforceCameraLock(view);
                unlockCamera();
                if(StencilManager.onTouchUp()){
                    event.cancel();
                    event.stop();
                    return;
                }
                Point2 p=projectRaw(view,x,y);
                if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                    selEndX=p.x;
                    selEndY=p.y;
                    captureSelection(mode==Mode.cut);
                    selecting=false;
                    setMode(Mode.paste);
                    event.cancel();
                    event.stop();
                    return;
                }
                if(draggingPaste){
                    draggingPaste=false;
                    event.cancel();
                    event.stop();
                    return;
                }
                if(drawingLine){
                    EditorTool tool=view.getTool();
                    CustomShapeManager.traceLine(lineStartX,lineStartY,lineEndX,lineEndY,(lx,ly)->applyToolStamp(tool,lx,ly));
                    drawingLine=false;
                    Vars.editor.flushOp();
                    Vars.ui.editor.resetSaved();
                    event.cancel();
                    event.stop();
                    return;
                }
                if(drawingCustom){
                    drawingCustom=false;
                    Vars.editor.flushOp();
                    Vars.ui.editor.resetSaved();
                    event.cancel();
                    event.stop();
                }
            }
        });
    }
    private static void applyToolStamp(EditorTool tool,int cx,int cy){
        if(tool==EditorTool.eraser){
            boolean eraseOres=tool.mode==0;
            CustomShapeManager.iterateBrush(cx,cy,Vars.editor.brushSize,CustomShapeManager.currentShape,t->{
                if(eraseOres)t.clearOverlay();
                else t.remove();
            });
            return;
        }
        if(tool==EditorTool.spray){
            double chance=0.012;
            CustomShapeManager.iterateBrush(cx,cy,Vars.editor.brushSize,CustomShapeManager.currentShape,t->{
                if(Mathf.chance(chance))placeCurrentDrawBlock(t,false,false);
            });
            return;
        }
        boolean replace=(tool==EditorTool.pencil&&tool.mode==0)||(tool==EditorTool.line&&tool.mode==0);
        boolean forceOverlay=(tool==EditorTool.pencil&&tool.mode==3);
        Block db=Vars.editor.drawBlock;
        if(db!=null&&db.isMultiblock()){
            if(cx>=0&&cy>=0&&cx<Vars.editor.width()&&cy<Vars.editor.height())Vars.editor.drawBlocks(cx,cy);
            return;
        }
        CustomShapeManager.iterateBrush(cx,cy,Vars.editor.brushSize,CustomShapeManager.currentShape,t->placeCurrentDrawBlock(t,replace,forceOverlay));
    }
    private static void placeCurrentDrawBlock(Tile tile,boolean replace,boolean forceOverlay){
        Block db=Vars.editor.drawBlock;
        if(db==null||tile==null)return;
        if(replace&&tile.block()==Blocks.air&&!db.isFloor())return;
        if(db.isFloor()&&db!=Blocks.air){
            if(forceOverlay||db.isOverlay()){
                tile.setOverlay(db);
            }else{
                if(!(db.asFloor().wallOre&&!tile.block().solid)){
                    tile.setFloor(db.asFloor());
                    if(!(tile.overlay() instanceof OverlayFloor)&&!db.asFloor().supportsOverlay){
                        tile.setOverlay(Blocks.air);
                    }
                }
            }
        }else if(!(tile.block().isMultiblock()&&!db.isMultiblock())){
            tile.setBlock(db,Vars.editor.drawTeam,Vars.editor.rotation);
        }
    }
    private static void drawOverlayContent(MapView view,Element overlay){
        try{
            if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                int minX=Math.min(selStartX,selEndX),maxX=Math.max(selStartX,selEndX)+1;
                int minY=Math.min(selStartY,selEndY),maxY=Math.max(selStartY,selEndY)+1;
                Vec2 a=tileToOverlay(view,overlay,minX,minY,tmpV1);
                Vec2 b=tileToOverlay(view,overlay,maxX,maxY,tmpV2);
                float rx=Math.min(a.x,b.x),ry=Math.min(a.y,b.y);
                float rw=Math.abs(b.x-a.x),rh=Math.abs(b.y-a.y);
                Draw.color(Pal.accent,0.22f);
                Fill.crect(rx,ry,rw,rh);
                Draw.color(Pal.accent);
                Lines.stroke(2f);
                Lines.rect(rx,ry,rw,rh);
                Draw.reset();
            }
            if(mode==Mode.paste&&clipboard!=null){
                clipboard.ensureTexture();
                Vec2 a=tileToOverlay(view,overlay,pasteOriginX,pasteOriginY,tmpV1);
                Vec2 b=tileToOverlay(view,overlay,pasteOriginX+clipboard.width,pasteOriginY+clipboard.height,tmpV2);
                float rx=Math.min(a.x,b.x),ry=Math.min(a.y,b.y);
                float rw=Math.abs(b.x-a.x),rh=Math.abs(b.y-a.y);
                if(clipboard.previewRegion!=null){
                    Draw.color(1f,1f,1f,0.65f);
                    Draw.rect(clipboard.previewRegion,rx+rw/2f,ry+rh/2f,rw,rh);
                }
                Draw.color(draggingPaste?Color.sky:Pal.accent);
                Lines.stroke(2f);
                Lines.rect(rx,ry,rw,rh);
                Draw.reset();
            }
            if(mode==Mode.none&&!StencilManager.moveMode){
                EditorTool tool=view.getTool();
                Point2 mp=currentMouseTile(view);
                if(drawingLine&&tool==EditorTool.line){
                    CustomShapeManager.drawLineSteppedOutline(view,overlay,lineStartX,lineStartY,lineEndX,lineEndY,Vars.editor.brushSize,CustomShapeManager.currentShape);
                }else if(tool==EditorTool.pencil||tool==EditorTool.eraser||tool==EditorTool.line||tool==EditorTool.spray){
                    if(Vars.editor.brushSize>20f||CustomShapeManager.currentShape!=BrushShape.circle||CustomShapeManager.line45Mode){
                        CustomShapeManager.drawSteppedOutline(view,overlay,mp.x,mp.y,Vars.editor.brushSize,CustomShapeManager.currentShape);
                    }
                }
            }
        }catch(Throwable ignored){}
    }
    public static void captureSelection(boolean cut){
        int minX=Math.min(selStartX,selEndX);
        int minY=Math.min(selStartY,selEndY);
        int maxX=Math.max(selStartX,selEndX);
        int maxY=Math.max(selStartY,selEndY);
        if(minX>maxX||minY>maxY)return;
        int w=maxX-minX+1,h=maxY-minY+1;
        int mapW=Vars.editor.width(),mapH=Vars.editor.height();
        Clip next=new Clip(w,h);
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                int wx=minX+x,wy=minY+y;
                int idx=x+y*w;
                if(wx<0||wy<0||wx>=mapW||wy>=mapH){
                    next.floorIds[idx]=-1;
                    next.overlayIds[idx]=-1;
                    continue;
                }
                Tile t=Vars.editor.tile(wx,wy);
                if(t==null){
                    next.floorIds[idx]=-1;
                    next.overlayIds[idx]=-1;
                    continue;
                }
                next.floorIds[idx]=copyFloor?t.floorID():-1;
                next.overlayIds[idx]=copyOre?t.overlayID():-1;
                boolean center=t.isCenter()&&copyBlock;
                next.blockIds[idx]=center?t.blockID():0;
                next.isCenter[idx]=center;
                next.teamIds[idx]=(byte)t.getTeamID();
                next.rotations[idx]=(byte)(t.build!=null?t.build.rotation:0);
                next.datas[idx]=t.data;
                next.floorDatas[idx]=getFloorData(t);
                next.overlayDatas[idx]=getOverlayData(t);
                next.extraDatas[idx]=getExtra(t);
            }
        }
        if(cut){
            Vars.editor.load(()->{
                for(int x=0;x<w;x++){
                    for(int y=0;y<h;y++){
                        int wx=minX+x,wy=minY+y;
                        if(wx<0||wy<0||wx>=mapW||wy>=mapH)continue;
                        Tile t=Vars.editor.tile(wx,wy);
                        if(t==null)continue;
                        if(copyBlock&&t.isCenter())t.remove();
                        if(copyOre)t.clearOverlay();
                    }
                }
            });
            refreshRenderer();
            Vars.editor.clearOp();
        }
        if(clipboard!=null)clipboard.dispose();
        clipboard=next;
    }
    public static void pasteAt(int ox,int oy){
        if(clipboard==null)return;
        int w=clipboard.width,h=clipboard.height;
        int mapW=Vars.editor.width(),mapH=Vars.editor.height();
        Vars.editor.load(()->{
            for(int x=0;x<w;x++){
                for(int y=0;y<h;y++){
                    int tx=ox+x,ty=oy+y;
                    if(tx<0||ty<0||tx>=mapW||ty>=mapH)continue;
                    int idx=x+y*w;
                    Tile tile=Vars.editor.tile(tx,ty);
                    if(tile==null)continue;
                    short fid=clipboard.floorIds[idx];
                    if(fid>=0){
                        Block fb=Vars.content.block(fid);
                        if(fb instanceof Floor)tile.setFloor((Floor)fb);
                        tile.data=clipboard.datas[idx];
                        setFloorData(tile,clipboard.floorDatas[idx]);
                        setOverlayData(tile,clipboard.overlayDatas[idx]);
                        setExtra(tile,clipboard.extraDatas[idx]);
                    }
                    short oid=clipboard.overlayIds[idx];
                    if(oid>=0){
                        Block ob=Vars.content.block(oid);
                        if(ob!=null&&(overrideOre||tile.overlay()==Blocks.air)){
                            tile.setOverlay(ob);
                        }
                    }
                }
            }
            for(int x=0;x<w;x++){
                for(int y=0;y<h;y++){
                    int tx=ox+x,ty=oy+y;
                    if(tx<0||ty<0||tx>=mapW||ty>=mapH)continue;
                    int idx=x+y*w;
                    if(clipboard.isCenter[idx]&&clipboard.blockIds[idx]>0){
                        Block bb=Vars.content.block(clipboard.blockIds[idx]);
                        Tile tile=Vars.editor.tile(tx,ty);
                        if(bb!=null&&tile!=null&&(overrideBlock||tile.block()==Blocks.air)){
                            Team tm=useEditorTeam?Vars.editor.drawTeam:Team.get(clipboard.teamIds[idx]&0xff);
                            tile.setBlock(bb,tm,clipboard.rotations[idx]);
                        }
                    }
                }
            }
        });
        refreshRenderer();
        Vars.editor.clearOp();
        Vars.ui.editor.resetSaved();
    }
    public static void rotateClipboard(){
        if(clipboard==null)return;
        int w=clipboard.width,h=clipboard.height;
        Clip next=new Clip(h,w);
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                int src=x+y*w;
                int nx=h-1-y,ny=x;
                int dst=nx+ny*h;
                next.floorIds[dst]=clipboard.floorIds[src];
                next.overlayIds[dst]=clipboard.overlayIds[src];
                next.blockIds[dst]=clipboard.blockIds[src];
                next.isCenter[dst]=clipboard.isCenter[src];
                next.teamIds[dst]=clipboard.teamIds[src];
                next.rotations[dst]=(byte)((clipboard.rotations[src]+1)&3);
                next.datas[dst]=clipboard.datas[src];
                next.floorDatas[dst]=clipboard.floorDatas[src];
                next.overlayDatas[dst]=clipboard.overlayDatas[src];
                next.extraDatas[dst]=clipboard.extraDatas[src];
            }
        }
        clipboard.dispose();
        clipboard=next;
    }
    public static void flipX(){
        if(clipboard==null)return;
        int w=clipboard.width,h=clipboard.height;
        Clip next=new Clip(w,h);
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                int src=x+y*w;
                int dst=(w-1-x)+y*w;
                next.floorIds[dst]=clipboard.floorIds[src];
                next.overlayIds[dst]=clipboard.overlayIds[src];
                next.blockIds[dst]=clipboard.blockIds[src];
                next.isCenter[dst]=clipboard.isCenter[src];
                next.teamIds[dst]=clipboard.teamIds[src];
                byte r=clipboard.rotations[src];
                if(r==0)r=2;else if(r==2)r=0;
                next.rotations[dst]=r;
                next.datas[dst]=clipboard.datas[src];
                next.floorDatas[dst]=clipboard.floorDatas[src];
                next.overlayDatas[dst]=clipboard.overlayDatas[src];
                next.extraDatas[dst]=clipboard.extraDatas[src];
            }
        }
        clipboard.dispose();
        clipboard=next;
    }
    public static void flipY(){
        if(clipboard==null)return;
        int w=clipboard.width,h=clipboard.height;
        Clip next=new Clip(w,h);
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                int src=x+y*w;
                int dst=x+(h-1-y)*w;
                next.floorIds[dst]=clipboard.floorIds[src];
                next.overlayIds[dst]=clipboard.overlayIds[src];
                next.blockIds[dst]=clipboard.blockIds[src];
                next.isCenter[dst]=clipboard.isCenter[src];
                next.teamIds[dst]=clipboard.teamIds[src];
                byte r=clipboard.rotations[src];
                if(r==1)r=3;else if(r==3)r=1;
                next.rotations[dst]=r;
                next.datas[dst]=clipboard.datas[src];
                next.floorDatas[dst]=clipboard.floorDatas[src];
                next.overlayDatas[dst]=clipboard.overlayDatas[src];
                next.extraDatas[dst]=clipboard.extraDatas[src];
            }
        }
        clipboard.dispose();
        clipboard=next;
    }
    public static void showCopyPasteSettings(){
        BaseDialog d=new BaseDialog(Core.bundle.get("upper.settings","Copy / Paste Options"));
        Table t=new Table();
        t.defaults().left().pad(4f);
        t.add("[accent]Copy Layers[]").row();
        t.check("Copy Floor",copyFloor,v->copyFloor=v).row();
        t.check("Copy Ore / Overlay",copyOre,v->copyOre=v).row();
        t.check("Copy Blocks / Walls",copyBlock,v->copyBlock=v).row();
        t.add("[accent]Paste Rules[]").padTop(8f).row();
        t.check("Override Existing Blocks",overrideBlock,v->overrideBlock=v).row();
        t.check("Override Existing Ores",overrideOre,v->overrideOre=v).row();
        t.check("Use Active Editor Team",useEditorTeam,v->useEditorTeam=v).row();
        d.cont.add(t);
        d.addCloseButton();
        d.show();
    }
    public static class Clip{
        public final int width,height;
        public final short[] floorIds,overlayIds,blockIds;
        public final boolean[] isCenter;
        public final byte[] teamIds,rotations,datas,floorDatas,overlayDatas;
        public final int[] extraDatas;
        public Texture previewTex;
        public TextureRegion previewRegion;
        public Clip(int w,int h){
            width=w;height=h;
            int len=w*h;
            floorIds=new short[len];
            overlayIds=new short[len];
            blockIds=new short[len];
            isCenter=new boolean[len];
            teamIds=new byte[len];
            rotations=new byte[len];
            datas=new byte[len];
            floorDatas=new byte[len];
            overlayDatas=new byte[len];
            extraDatas=new int[len];
        }
        public void ensureTexture(){
            if(previewRegion!=null)return;
            Pixmap pm=new Pixmap(width,height);
            for(int x=0;x<width;x++){
                for(int y=0;y<height;y++){
                    int idx=x+y*width;
                    int col=0x00000000;
                    int extra=extraDatas[idx];
                    if(extra!=0){
                        col=extra|0xff;
                    }else if(blockIds[idx]>0){
                        Block b=Vars.content.block(blockIds[idx]);
                        if(b!=null&&b.mapColor!=null)col=b.mapColor.rgba8888();
                    }else if(overlayIds[idx]>0){
                        Block o=Vars.content.block(overlayIds[idx]);
                        if(o!=null&&o.mapColor!=null)col=o.mapColor.rgba8888();
                    }else if(floorIds[idx]>=0){
                        Block f=Vars.content.block(floorIds[idx]);
                        if(f!=null&&f.mapColor!=null)col=f.mapColor.rgba8888();
                    }
                    pm.setRaw(x,height-1-y,col);
                }
            }
            previewTex=new Texture(pm);
            previewRegion=new TextureRegion(previewTex);
            pm.dispose();
        }
        public void dispose(){
            if(previewTex!=null){
                previewTex.dispose();
                previewTex=null;
                previewRegion=null;
            }
        }
    }
            }
