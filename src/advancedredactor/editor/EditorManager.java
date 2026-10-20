package advancedredactor.editor;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import advancedredactor.editor.EditorCore.*;
import advancedredactor.editor.EditorTools.*;
import java.lang.reflect.*;
public class EditorManager{
    public static EditorOperationStack history=new EditorOperationStack();
    public static ChunkedGridBits selection=new ChunkedGridBits(500,500);
    public static Seq<TileData> clipboard=new Seq<>();
    public static int clipW=0,clipH=0;
    private static Window window;
    private static boolean active=false,pastePreview=false;
    private static int dragStartX=-1,dragStartY=-1;
    private static BlocksTilesOperation activeDrawOp=null;
    private static final Point2 tmpPt=new Point2();
    private static Method projectMethod;
    public static void init(){
        try{
            projectMethod=MapView.class.getDeclaredMethod("project",float.class,float.class);
            projectMethod.setAccessible(true);
        }catch(Throwable ignored){}
        EditorDialogs.initMods();
        Events.run(Trigger.update,EditorManager::update);
        Events.run(Trigger.draw,EditorManager::drawOverlay);
    }
    private static Point2 mouseTile(){
        if(projectMethod!=null&&Vars.ui!=null&&Vars.ui.editor!=null&&Vars.ui.editor.getView()!=null){
            try{
                Point2 p=(Point2)projectMethod.invoke(Vars.ui.editor.getView(),Core.input.mouseX(),Core.input.mouseY());
                if(p!=null)return tmpPt.set(p.x,p.y);
            }catch(Throwable ignored){}
        }
        Vec2 m=Core.input.mouseWorld();
        return tmpPt.set((int)(m.x/8f),(int)(m.y/8f));
    }
    private static void update(){
        boolean inEditor=Vars.ui!=null&&Vars.ui.editor!=null&&Vars.ui.editor.isShown();
        if(inEditor&&!active){
            selection=new ChunkedGridBits(Math.max(1,Vars.world.width()),Math.max(1,Vars.world.height()));
            buildWindow();active=true;
        }else if(!inEditor&&active){
            if(window!=null)window.remove();
            active=false;
        }
        if(!active)return;
        if(selection.width!=Vars.world.width()||selection.height!=Vars.world.height()){
            selection=new ChunkedGridBits(Math.max(1,Vars.world.width()),Math.max(1,Vars.world.height()));
        }
        handleHotkeys();
        handleMouse();
    }
    private static void handleHotkeys(){
        if(Core.scene.hasField())return;
        boolean ctrl=Core.input.keyDown(KeyCode.controlLeft)||Core.input.keyDown(KeyCode.controlRight);
        if(ctrl&&Core.input.keyTap(KeyCode.z))history.undo();
        if(ctrl&&Core.input.keyTap(KeyCode.y))history.redo();
        if(ctrl&&Core.input.keyTap(KeyCode.a))BlocksSelectionAction.selectAll();
        if(ctrl&&Core.input.keyTap(KeyCode.i))BlocksSelectionAction.invertSelection();
        if(ctrl&&Core.input.keyTap(KeyCode.c)){EditorTools.copySelection();Vars.ui.showInfoToast("Copied "+clipboard.size+" tiles",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.x)){EditorTools.cutSelection();Vars.ui.showInfoToast("Cut selection",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.v)){
            Point2 pt=mouseTile();
            EditorTools.pasteClipboard(pt.x,pt.y);
        }
        if(Core.input.keyTap(KeyCode.r)&&!clipboard.isEmpty())EditorTools.rotateClipboard(!Core.input.keyDown(KeyCode.shiftLeft));
        if(Core.input.keyTap(KeyCode.f)&&!clipboard.isEmpty())EditorTools.flipClipboard(!Core.input.keyDown(KeyCode.shiftLeft));
    }
    private static void handleMouse(){
        if(Core.scene.hasMouse()&&Core.scene.hit(Core.input.mouseX(),Core.input.mouseY(),true)!=Vars.ui.editor.getView())return;
        Point2 pt=mouseTile();
        int tx=pt.x,ty=pt.y;
        if(pastePreview){
            if(Core.input.keyTap(KeyCode.mouseLeft)){
                EditorTools.pasteClipboard(tx,ty);
                pastePreview=false;
            }else if(Core.input.keyTap(KeyCode.mouseRight)){
                pastePreview=false;
            }
            return;
        }
        if(EditorTools.currentTool==ToolMode.none)return;
        if(EditorTools.currentTool==ToolMode.pick&&Core.input.keyTap(KeyCode.mouseLeft)){
            EditorTools.pickTile(tx,ty);return;
        }
        if(EditorTools.currentTool==ToolMode.selectRect||EditorTools.currentTool==ToolMode.line){
            if(Core.input.keyTap(KeyCode.mouseLeft)){dragStartX=tx;dragStartY=ty;}
            if(!Core.input.keyDown(KeyCode.mouseLeft)&&dragStartX!=-1){
                if(EditorTools.currentTool==ToolMode.selectRect)EditorTools.selectRect(dragStartX,dragStartY,tx,ty,Core.input.keyDown(KeyCode.shiftLeft));
                else EditorTools.applyLine(dragStartX,dragStartY,tx,ty,Core.input.keyDown(KeyCode.altLeft));
                dragStartX=-1;dragStartY=-1;
            }
            return;
        }
        if(EditorTools.currentTool==ToolMode.brushRect||EditorTools.currentTool==ToolMode.brushCircle){
            boolean left=Core.input.keyDown(KeyCode.mouseLeft),right=Core.input.keyDown(KeyCode.mouseRight);
            if(left||right){
                if(activeDrawOp==null)activeDrawOp=new BlocksTilesOperation();
                EditorTools.applyBrush(tx,ty,right,activeDrawOp);
            }else if(activeDrawOp!=null){
                activeDrawOp.commit();activeDrawOp=null;
            }
        }
    }
    private static void drawOverlay(){
        if(!active)return;
        EditorDialogs.drawPlanetBackground();
        if(!selection.isEmpty()){
            Draw.color(Color.cyan,0.26f);
            selection.each((x,y)->Fill.rect(x*8f+4f,y*8f+4f,8f,8f));
            Draw.reset();
        }
        Point2 pt=mouseTile();
        int tx=pt.x,ty=pt.y;
        if(dragStartX!=-1&&dragStartY!=-1){
            Draw.color(Pal.accent,0.45f);
            if(EditorTools.currentTool==ToolMode.selectRect){
                int minX=Math.min(dragStartX,tx),maxX=Math.max(dragStartX,tx),minY=Math.min(dragStartY,ty),maxY=Math.max(dragStartY,ty);
                float w=(maxX-minX+1)*8f,h=(maxY-minY+1)*8f;
                Fill.rect(minX*8f+w/2f,minY*8f+h/2f,w,h);
            }else if(EditorTools.currentTool==ToolMode.line){
                Lines.stroke(2f);Lines.line(dragStartX*8f+4f,dragStartY*8f+4f,tx*8f+4f,ty*8f+4f);
            }
            Draw.reset();
        }
        if(pastePreview&&!clipboard.isEmpty()){
            int ox=tx-clipW/2,oy=ty-clipH/2;
            Draw.color(Color.lime,0.45f);
            for(TileData td:clipboard){
                if(td.block!=Blocks.air||td.floor!=Blocks.air)Fill.rect((ox+td.x)*8f+4f,(oy+td.y)*8f+4f,8f,8f);
            }
            Draw.reset();
        }else if(EditorTools.currentTool==ToolMode.brushRect||EditorTools.currentTool==ToolMode.brushCircle){
            Draw.color(Pal.accent,0.7f);Lines.stroke(1.5f);
            float sz=(EditorTools.brushSize*2-1)*8f;
            if(EditorTools.currentTool==ToolMode.brushRect)Lines.rect(tx*8f+4f-sz/2f,ty*8f+4f-sz/2f,sz,sz);
            else Lines.circle(tx*8f+4f,ty*8f+4f,sz/2f);
            Draw.reset();
        }
    }
    private static void buildWindow(){
        if(window!=null)window.remove();
        window=new Window("Mapping Utilities",210f,460f);
        window.data.closable=false;
        Table t=new Table();t.defaults().size(90f,32f).pad(1.5f);
        t.button("@editor.mu.tool.none",Styles.flatt,()->{EditorTools.currentTool=ToolMode.none;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.none&&!pastePreview));
        t.button("@editor.mu.tool.rect",Styles.flatt,()->{EditorTools.currentTool=ToolMode.brushRect;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.brushRect)).row();
        t.button("@editor.mu.tool.circle",Styles.flatt,()->{EditorTools.currentTool=ToolMode.brushCircle;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.brushCircle));
        t.button("@editor.mu.tool.line",Styles.flatt,()->{EditorTools.currentTool=ToolMode.line;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.line)).row();
        t.button("@editor.mu.tool.select",Styles.flatt,()->{EditorTools.currentTool=ToolMode.selectRect;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.selectRect));
        t.button("@editor.mu.tool.pick",Styles.flatt,()->{EditorTools.currentTool=ToolMode.pick;pastePreview=false;}).update(b->b.setChecked(EditorTools.currentTool==ToolMode.pick)).row();
        t.button("@editor.mu.selectall",Styles.flatt,BlocksSelectionAction::selectAll);
        t.button("@editor.mu.invert",Styles.flatt,BlocksSelectionAction::invertSelection).row();
        t.button("@editor.mu.clear",Styles.flatt,()->selection.clearAll()).colspan(2).growX().row();
        t.button("@editor.mu.undo",Styles.flatt,()->history.undo()).disabled(b->!history.canUndo());
        t.button("@editor.mu.redo",Styles.flatt,()->history.redo()).disabled(b->!history.canRedo()).row();
        t.button("@editor.mu.copy",Styles.flatt,EditorTools::copySelection);
        t.button("@editor.mu.paste",Styles.flatt,()->pastePreview=!pastePreview).update(b->b.setChecked(pastePreview)).row();
        t.button("@editor.mu.cut",Styles.flatt,EditorTools::cutSelection);
        t.button("@editor.mu.fill",Styles.flatt,EditorTools::fillSelection).row();
        t.button("@editor.mu.rot",Styles.flatt,()->EditorTools.rotateClipboard(true));
        t.button("@editor.mu.flip",Styles.flatt,()->EditorTools.flipClipboard(true)).row();
        Label bsl=new Label("Brush Size: "+EditorTools.brushSize);
        t.add(bsl).colspan(2).left().padTop(4f).row();
        t.slider(1f,100f,1f,EditorTools.brushSize,s->{EditorTools.brushSize=(int)s;Vars.editor.brushSize=s;bsl.setText("Brush Size: "+(int)s);}).colspan(2).width(175f).row();
        t.check("Replace Mode",EditorTools.replaceMode,b->EditorTools.replaceMode=b).colspan(2).left().row();
        t.check("Draw Floor",EditorTools.drawFloor,b->EditorTools.drawFloor=b).colspan(2).left().row();
        t.check("Draw Block",EditorTools.drawBlock,b->EditorTools.drawBlock=b).colspan(2).left().row();
        t.check("Draw Overlay",EditorTools.drawOverlay,b->EditorTools.drawOverlay=b).colspan(2).left().padBottom(4f).row();
        t.button("@editor.mu.cliffs",Styles.flatt,EditorTools::generateAutoCliffs).colspan(2).growX().row();
        t.button("@editor.mu.advrules",Styles.flatt,EditorDialogs::showAdvRulesDialog).colspan(2).growX().row();
        t.button("@editor.mu.banned",Styles.flatt,EditorDialogs::showBannedContentDialog);
        t.button("@editor.mu.planetbg",Styles.flatt,EditorDialogs::showPlanetBackgroundDialog).row();
        t.button("@editor.mu.resize",Styles.flatt,EditorDialogs::showResizeDialog);
        t.button("@editor.mu.uiexplorer",Styles.flatt,EditorDialogs::showUIExplorerDialog).row();
        t.button("@editor.mu.js",Styles.flatt,EditorDialogs::showJSConsoleDialog).colspan(2).growX().row();
        ScrollPane sp=new ScrollPane(t);sp.setScrollingDisabled(true,false);
        window.body.add(sp).maxHeight(Core.graphics.getHeight()*0.68f);
        window.rebuildWindow();
        Core.scene.add(window);
        window.setPosition(Core.graphics.getWidth()-window.getWidth()-10f,Core.graphics.getHeight()-window.getHeight()-10f);
    }
            }
