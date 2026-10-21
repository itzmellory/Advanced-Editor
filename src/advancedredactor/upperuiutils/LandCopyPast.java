package advancedredactor.upperuiutils;
import arc.*;
import arc.func.*;
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
import advancedredactor.upperuiutils.CustomShapeManager.*;
import java.lang.reflect.*;
public class LandCopyPast{
    public enum Mode{none,copy,cut,paste}
    public static Mode mode=Mode.none;
    public static Clip clipboard;
    public static boolean copyFloor=true,copyOre=true,copyBlock=true;
    public static boolean overrideBlock=true,overrideOre=true,useEditorTeam=false;
    private static boolean selecting=false,drawingCustom=false,drawingLine=false;
    private static int selStartX,selStartY,selEndX,selEndY;
    private static int lineStartX,lineStartY,lineEndX,lineEndY;
    private static int lastDrawX=-1,lastDrawY=-1;
    private static boolean mobileDragMode=false,mobileDragging=false;
    private static int pasteOriginX=0,pasteOriginY=0,dragStartTileX,dragStartTileY,dragStartOriginX,dragStartOriginY;
    private static Table mobilePasteBar;
    private static Element hookedView;
    private static Field extraDataField;
    static{
        try{
            extraDataField=Tile.class.getDeclaredField("extraData");
            extraDataField.setAccessible(true);
        }catch(Throwable ignored){}
    }
    public static void init(){
        Events.run(Trigger.update,LandCopyPast::update);
    }
    public static int getExtra(Tile t){
        try{if(extraDataField!=null)return extraDataField.getInt(t);}catch(Throwable ignored){}
        return 0;
    }
    public static void setExtra(Tile t,int val){
        try{if(extraDataField!=null)extraDataField.setInt(t,val);}catch(Throwable ignored){}
    }
    public static void toggleMode(Mode m){
        setMode(mode==m?Mode.none:m);
    }
    public static void setMode(Mode m){
        mode=m;
        selecting=false;
        mobileDragging=false;
        if(m!=Mode.none)CustomShapeManager.line45Mode=false;
        if(m==Mode.paste&&clipboard!=null){
            pasteOriginX=Math.max(0,(Vars.editor.width()-clipboard.width)/2);
            pasteOriginY=Math.max(0,(Vars.editor.height()-clipboard.height)/2);
            rebuildMobilePasteBar();
        }else{
            removeMobilePasteBar();
        }
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.editor==null||!Vars.ui.editor.isShown()){
            hookedView=null;
            removeMobilePasteBar();
            return;
        }
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        boolean hasOverlay=false;
        for(Element e:view.parent.getChildren()){
            if("landcopypast-overlay".equals(e.name)){
                hasOverlay=true;
                break;
            }
        }
        if(hookedView!=view||!hasOverlay){
            hookedView=view;
            injectViewHooks(view);
        }
    }
    private static void rebuildMobilePasteBar(){
        removeMobilePasteBar();
        if(Vars.ui==null||Vars.ui.editor==null||Vars.ui.editor.getView()==null)return;
        Group parent=Vars.ui.editor.getView().parent;
        mobilePasteBar=new Table();
        mobilePasteBar.setFillParent(true);
        mobilePasteBar.bottom();
        mobilePasteBar.touchable=Touchable.childrenOnly;
        Table box=new Table(Tex.button);
        box.margin(4f);
        box.defaults().height(42f).pad(3f);
        TextButton dragBtn=box.button(Core.bundle.get("upper.dragbtn","Move / Drag"),Icon.move,Styles.flatTogglet,()->mobileDragMode=!mobileDragMode).width(145f).get();
        dragBtn.update(()->dragBtn.setChecked(mobileDragMode));
        box.button(Core.bundle.get("upper.apply","Paste Here"),Icon.ok,Styles.flatt,()->pasteAt(pasteOriginX,pasteOriginY)).width(135f);
        box.button(Core.bundle.get("upper.cancel","Cancel"),Icon.cancel,Styles.flatt,()->setMode(Mode.none)).width(110f);
        mobilePasteBar.add(box).padBottom(12f);
        parent.addChild(mobilePasteBar);
    }
    private static void removeMobilePasteBar(){
        if(mobilePasteBar!=null){
            mobilePasteBar.remove();
            mobilePasteBar=null;
        }
    }
    private static Point2 project(MapView view,float x,float y){
        try{
            Float ox=(Float)Reflect.get(view,"offsetx");
            Float oy=(Float)Reflect.get(view,"offsety");
            Float zoom=(Float)Reflect.get(view,"zoom");
            if(ox==null||oy==null||zoom==null)return new Point2(0,0);
            float mapW=Vars.editor.width(),mapH=Vars.editor.height();
            float size=Math.min(view.getWidth(),view.getHeight());
            float scl=size/Math.max(mapW,mapH)*zoom;
            float cx=view.getWidth()/2f+ox*zoom;
            float cy=view.getHeight()/2f+oy*zoom;
            float mapBlX=cx-mapW*scl/2f;
            float mapBlY=cy-mapH*scl/2f;
            int tx=Mathf.floor((x-mapBlX)/scl);
            int ty=Mathf.floor((y-mapBlY)/scl);
            return new Point2(tx,ty);
        }catch(Throwable e){
            return new Point2(0,0);
        }
    }
    private static Point2 mouseTile(MapView view){
        Vec2 local=view.screenToLocalCoordinates(Tmp.v1.set(Core.input.mouseX(),Core.input.mouseY()));
        return project(view,local.x,local.y);
    }
    private static boolean shouldInterceptVanillaTool(EditorTool tool){
        if(tool==EditorTool.line&&(CustomShapeManager.line45Mode||CustomShapeManager.currentShape!=BrushShape.circle||Vars.editor.brushSize>20f))return true;
        if((tool==EditorTool.pencil||tool==EditorTool.eraser||tool==EditorTool.spray)&&(CustomShapeManager.currentShape!=BrushShape.circle&& !(tool==EditorTool.pencil&&tool.mode==2&&CustomShapeManager.currentShape==BrushShape.square)))return true;
        return false;
    }
    private static void injectViewHooks(MapView view){
        Group parent=view.parent;
        Element overlay=new Element(){
            @Override public void draw(){
                if(!Vars.ui.editor.isShown())return;
                if(ScissorStack.push(Tmp.r1.set(view.x,view.y,view.getWidth(),view.getHeight()))){
                    drawOverlayContent(view);
                    ScissorStack.pop();
                }
            }
        };
        overlay.name="landcopypast-overlay";
        overlay.setFillParent(true);
        overlay.touchable=Touchable.disabled;
        parent.addChild(overlay);
        view.addCaptureListener(new InputListener(){
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||button!=KeyCode.mouseLeft)return false;
                Point2 p=project(view,x,y);
                if(mode==Mode.copy||mode==Mode.cut){
                    selecting=true;
                    selStartX=selEndX=Mathf.clamp(p.x,0,Vars.editor.width()-1);
                    selStartY=selEndY=Mathf.clamp(p.y,0,Vars.editor.height()-1);
                    event.stop();
                    return true;
                }
                if(mode==Mode.paste&&clipboard!=null){
                    if(Vars.mobile||mobileDragMode){
                        if(mobileDragMode){
                            mobileDragging=true;
                            dragStartTileX=p.x;
                            dragStartTileY=p.y;
                            dragStartOriginX=pasteOriginX;
                            dragStartOriginY=pasteOriginY;
                            event.stop();
                            return true;
                        }
                        return false;
                    }else{
                        pasteOriginX=p.x-clipboard.width/2;
                        pasteOriginY=p.y-clipboard.height/2;
                        pasteAt(pasteOriginX,pasteOriginY);
                        event.stop();
                        return true;
                    }
                }
                EditorTool tool=view.getTool();
                if(shouldInterceptVanillaTool(tool)){
                    if(tool==EditorTool.line){
                        drawingLine=true;
                        lineStartX=lineEndX=Mathf.clamp(p.x,0,Vars.editor.width()-1);
                        lineStartY=lineEndY=Mathf.clamp(p.y,0,Vars.editor.height()-1);
                    }else{
                        drawingCustom=true;
                        lastDrawX=p.x;
                        lastDrawY=p.y;
                        applyToolStamp(tool,p.x,p.y);
                    }
                    event.stop();
                    return true;
                }
                return false;
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                if(pointer!=0)return;
                Point2 p=project(view,x,y);
                if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                    selEndX=Mathf.clamp(p.x,0,Vars.editor.width()-1);
                    selEndY=Mathf.clamp(p.y,0,Vars.editor.height()-1);
                    event.stop();
                    return;
                }
                if(mode==Mode.paste&&mobileDragging){
                    pasteOriginX=dragStartOriginX+(p.x-dragStartTileX);
                    pasteOriginY=dragStartOriginY+(p.y-dragStartTileY);
                    event.stop();
                    return;
                }
                if(drawingLine){
                    int tx=Mathf.clamp(p.x,0,Vars.editor.width()-1);
                    int ty=Mathf.clamp(p.y,0,Vars.editor.height()-1);
                    if(CustomShapeManager.line45Mode){
                        Point2 snapped=CustomShapeManager.snap45(lineStartX,lineStartY,tx,ty);
                        lineEndX=snapped.x;
                        lineEndY=snapped.y;
                    }else{
                        lineEndX=tx;
                        lineEndY=ty;
                    }
                    event.stop();
                    return;
                }
                if(drawingCustom){
                    EditorTool tool=view.getTool();
                    CustomShapeManager.traceLine(lastDrawX,lastDrawY,p.x,p.y,(lx,ly)->applyToolStamp(tool,lx,ly));
                    lastDrawX=p.x;
                    lastDrawY=p.y;
                    event.stop();
                }
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,KeyCode button){
                if(pointer!=0||button!=KeyCode.mouseLeft)return;
                Point2 p=project(view,x,y);
                if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                    selEndX=Mathf.clamp(p.x,0,Vars.editor.width()-1);
                    selEndY=Mathf.clamp(p.y,0,Vars.editor.height()-1);
                    captureSelection(mode==Mode.cut);
                    selecting=false;
                    setMode(Mode.paste);
                    event.stop();
                    return;
                }
                if(mobileDragging){
                    mobileDragging=false;
                    event.stop();
                    return;
                }
                if(drawingLine){
                    EditorTool tool=view.getTool();
                    CustomShapeManager.traceLine(lineStartX,lineStartY,lineEndX,lineEndY,(lx,ly)->applyToolStamp(tool,lx,ly));
                    drawingLine=false;
                    Vars.editor.flushOp();
                    Vars.ui.editor.resetSaved();
                    event.stop();
                    return;
                }
                if(drawingCustom){
                    drawingCustom=false;
                    Vars.editor.flushOp();
                    Vars.ui.editor.resetSaved();
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
            Vars.editor.drawBlocks(cx,cy);
            return;
        }
        CustomShapeManager.iterateBrush(cx,cy,Vars.editor.brushSize,CustomShapeManager.currentShape,t->{
            placeCurrentDrawBlock(t,replace,forceOverlay);
        });
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
    private static void drawOverlayContent(MapView view){
        try{
            Float ox=(Float)Reflect.get(view,"offsetx");
            Float oy=(Float)Reflect.get(view,"offsety");
            Float zoom=(Float)Reflect.get(view,"zoom");
            if(ox==null||oy==null||zoom==null)return;
            float mapW=Vars.editor.width(),mapH=Vars.editor.height();
            if(mapW<=0||mapH<=0)return;
            float size=Math.min(view.getWidth(),view.getHeight());
            float scl=size/Math.max(mapW,mapH)*zoom;
            float cx=view.x+view.getWidth()/2f+ox*zoom;
            float cy=view.y+view.getHeight()/2f+oy*zoom;
            float mapBlX=cx-mapW*scl/2f;
            float mapBlY=cy-mapH*scl/2f;
            if(selecting&&(mode==Mode.copy||mode==Mode.cut)){
                int minX=Math.min(selStartX,selEndX),maxX=Math.max(selStartX,selEndX)+1;
                int minY=Math.min(selStartY,selEndY),maxY=Math.max(selStartY,selEndY)+1;
                float rx=mapBlX+minX*scl,ry=mapBlY+minY*scl;
                float rw=(maxX-minX)*scl,rh=(maxY-minY)*scl;
                Draw.color(Pal.accent,0.22f);
                Fill.crect(rx,ry,rw,rh);
                Draw.color(Pal.accent);
                Lines.stroke(2f);
                Lines.rect(rx,ry,rw,rh);
                Draw.reset();
            }
            if(mode==Mode.paste&&clipboard!=null){
                int gx=pasteOriginX,gy=pasteOriginY;
                if(!Vars.mobile&&!mobileDragMode){
                    Point2 mp=mouseTile(view);
                    gx=mp.x-clipboard.width/2;
                    gy=mp.y-clipboard.height/2;
                }
                clipboard.ensureTexture();
                float rx=mapBlX+gx*scl,ry=mapBlY+gy*scl;
                float rw=clipboard.width*scl,rh=clipboard.height*scl;
                if(clipboard.previewRegion!=null){
                    Draw.color(1f,1f,1f,0.65f);
                    Draw.rect(clipboard.previewRegion,rx+rw/2f,ry+rh/2f,rw,rh);
                }
                Draw.color(mobileDragMode?Color.sky:Pal.accent);
                Lines.stroke(2f);
                Lines.rect(rx,ry,rw,rh);
                Draw.reset();
            }
            if(mode==Mode.none){
                EditorTool tool=view.getTool();
                Point2 mp=mouseTile(view);
                if(drawingLine&&tool==EditorTool.line){
                    CustomShapeManager.drawLineSteppedOutline(view,lineStartX,lineStartY,lineEndX,lineEndY,Vars.editor.brushSize,CustomShapeManager.currentShape);
                }else if(tool==EditorTool.pencil||tool==EditorTool.eraser||tool==EditorTool.line||tool==EditorTool.spray){
                    if(Vars.editor.brushSize>20f||CustomShapeManager.currentShape!=BrushShape.circle||CustomShapeManager.line45Mode){
                        CustomShapeManager.drawSteppedOutline(view,mp.x,mp.y,Vars.editor.brushSize,CustomShapeManager.currentShape);
                    }
                }
            }
        }catch(Throwable ignored){}
    }
    public static void captureSelection(boolean cut){
        int minX=Math.max(0,Math.min(selStartX,selEndX));
        int minY=Math.max(0,Math.min(selStartY,selEndY));
        int maxX=Math.min(Vars.editor.width()-1,Math.max(selStartX,selEndX));
        int maxY=Math.min(Vars.editor.height()-1,Math.max(selStartY,selEndY));
        if(minX>maxX||minY>maxY)return;
        int w=maxX-minX+1,h=maxY-minY+1;
        Clip next=new Clip(w,h);
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                Tile t=Vars.editor.tile(minX+x,minY+y);
                if(t==null)continue;
                int idx=x+y*w;
                next.floorIds[idx]=copyFloor?t.floorID():-1;
                next.overlayIds[idx]=copyOre?t.overlayID():-1;
                boolean center=t.isCenter()&&copyBlock;
                next.blockIds[idx]=center?t.blockID():0;
                next.isCenter[idx]=center;
                next.teamIds[idx]=(byte)t.getTeamID();
                next.rotations[idx]=(byte)(t.build!=null?t.build.rotation:0);
                next.datas[idx]=t.data;
                next.floorDatas[idx]=t.floorData;
                next.overlayDatas[idx]=t.overlayData;
                next.extraDatas[idx]=getExtra(t);
            }
        }
        if(cut){
            Vars.editor.load(()->{
                for(int x=0;x<w;x++){
                    for(int y=0;y<h;y++){
                        Tile t=Vars.editor.tile(minX+x,minY+y);
                        if(t==null)continue;
                        if(copyBlock&&t.isCenter())t.remove();
                        if(copyOre)t.clearOverlay();
                    }
                }
            });
            Vars.editor.renderer.resize(Vars.editor.width(),Vars.editor.height());
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
                        tile.floorData=clipboard.floorDatas[idx];
                        tile.overlayData=clipboard.overlayDatas[idx];
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
        Vars.editor.renderer.resize(mapW,mapH);
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
        t.check("Touch Drag Mode (Mobile/PC)",mobileDragMode,v->{
            mobileDragMode=v;
            if(mode==Mode.paste)rebuildMobilePasteBar();
        }).row();
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
