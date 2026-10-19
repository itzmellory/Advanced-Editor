package advancedredactor.editor;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import advancedredactor.editor.MUCore.*;
import advancedredactor.editor.MUTools.*;
public class MUEditorManager{
    public static EditorOperationStack history=new EditorOperationStack();
    public static ChunkedGridBits selection=new ChunkedGridBits(500,500);
    public static Seq<TileData> clipboard=new Seq<>();
    public static int clipW=0,clipH=0;
    private static Window window;
    private static boolean active=false,pastePreview=false;
    private static int dragStartX=-1,dragStartY=-1;
    private static BlocksTilesOperation activeDrawOp=null;
    public static void init(){
        MUDialogs.initMods();
        Events.run(Trigger.update,MUEditorManager::update);
        Events.run(Trigger.draw,MUEditorManager::drawOverlay);
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
        if(ctrl&&Core.input.keyTap(KeyCode.c)){MUTools.copySelection();Vars.ui.showInfoToast("Copied "+clipboard.size+" tiles",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.x)){MUTools.cutSelection();Vars.ui.showInfoToast("Cut selection",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.v)){
            Vec2 m=Core.input.mouseWorld();
            MUTools.pasteClipboard((int)(m.x/8f),(int)(m.y/8f));
        }
        if(Core.input.keyTap(KeyCode.r)&&!clipboard.isEmpty())MUTools.rotateClipboard(!Core.input.keyDown(KeyCode.shiftLeft));
        if(Core.input.keyTap(KeyCode.f)&&!clipboard.isEmpty())MUTools.flipClipboard(!Core.input.keyDown(KeyCode.shiftLeft));
    }
    private static void handleMouse(){
        if(Core.scene.hasMouse())return;
        Vec2 m=Core.input.mouseWorld();
        int tx=(int)(m.x/8f),ty=(int)(m.y/8f);
        if(pastePreview){
            if(Core.input.keyTap(KeyCode.mouseLeft)){
                MUTools.pasteClipboard(tx,ty);
                pastePreview=false;
            }else if(Core.input.keyTap(KeyCode.mouseRight)){
                pastePreview=false;
            }
            return;
        }
        if(MUTools.currentTool==ToolMode.none)return;
        if(MUTools.currentTool==ToolMode.pick&&Core.input.keyTap(KeyCode.mouseLeft)){
            MUTools.pickTile(tx,ty);return;
        }
        if(MUTools.currentTool==ToolMode.selectRect||MUTools.currentTool==ToolMode.line){
            if(Core.input.keyTap(KeyCode.mouseLeft)){dragStartX=tx;dragStartY=ty;}
            if(!Core.input.keyDown(KeyCode.mouseLeft)&&dragStartX!=-1){
                if(MUTools.currentTool==ToolMode.selectRect)MUTools.selectRect(dragStartX,dragStartY,tx,ty,Core.input.keyDown(KeyCode.shiftLeft));
                else MUTools.applyLine(dragStartX,dragStartY,tx,ty,Core.input.keyDown(KeyCode.altLeft));
                dragStartX=-1;dragStartY=-1;
            }
            return;
        }
        if(MUTools.currentTool==ToolMode.brushRect||MUTools.currentTool==ToolMode.brushCircle){
            boolean left=Core.input.keyDown(KeyCode.mouseLeft),right=Core.input.keyDown(KeyCode.mouseRight);
            if(left||right){
                if(activeDrawOp==null)activeDrawOp=new BlocksTilesOperation();
                MUTools.applyBrush(tx,ty,right,activeDrawOp);
            }else if(activeDrawOp!=null){
                activeDrawOp.commit();activeDrawOp=null;
            }
        }
    }
    private static void drawOverlay(){
        if(!active)return;
        MUDialogs.drawPlanetBackground();
        if(!selection.isEmpty()){
            Draw.color(Color.cyan,0.26f);
            selection.each((x,y)->Fill.rect(x*8f+4f,y*8f+4f,8f,8f));
            Draw.reset();
        }
        Vec2 m=Core.input.mouseWorld();
        int tx=(int)(m.x/8f),ty=(int)(m.y/8f);
        if(dragStartX!=-1&&dragStartY!=-1){
            Draw.color(Pal.accent,0.45f);
            if(MUTools.currentTool==ToolMode.selectRect){
                int minX=Math.min(dragStartX,tx),maxX=Math.max(dragStartX,tx),minY=Math.min(dragStartY,ty),maxY=Math.max(dragStartY,ty);
                float w=(maxX-minX+1)*8f,h=(maxY-minY+1)*8f;
                Fill.rect(minX*8f+w/2f,minY*8f+h/2f,w,h);
            }else if(MUTools.currentTool==ToolMode.line){
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
        }else if(MUTools.currentTool==ToolMode.brushRect||MUTools.currentTool==ToolMode.brushCircle){
            Draw.color(Pal.accent,0.7f);Lines.stroke(1.5f);
            float sz=(MUTools.brushSize*2-1)*8f;
            if(MUTools.currentTool==ToolMode.brushRect)Lines.rect(tx*8f+4f-sz/2f,ty*8f+4f-sz/2f,sz,sz);
            else Lines.circle(tx*8f+4f,ty*8f+4f,sz/2f);
            Draw.reset();
        }
    }
    private static void buildWindow(){
        if(window!=null)window.remove();
        window=new Window("Mapping Utilities",210f,460f);
        window.data.closable=false;
        Table t=new Table();t.defaults().size(90f,32f).pad(1.5f);
        t.button("@editor.mu.tool.none",Styles.flatt,()->{MUTools.currentTool=ToolMode.none;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.none&&!pastePreview));
        t.button("@editor.mu.tool.rect",Styles.flatt,()->{MUTools.currentTool=ToolMode.brushRect;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.brushRect)).row();
        t.button("@editor.mu.tool.circle",Styles.flatt,()->{MUTools.currentTool=ToolMode.brushCircle;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.brushCircle));
        t.button("@editor.mu.tool.line",Styles.flatt,()->{MUTools.currentTool=ToolMode.line;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.line)).row();
        t.button("@editor.mu.tool.select",Styles.flatt,()->{MUTools.currentTool=ToolMode.selectRect;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.selectRect));
        t.button("@editor.mu.tool.pick",Styles.flatt,()->{MUTools.currentTool=ToolMode.pick;pastePreview=false;}).update(b->b.setChecked(MUTools.currentTool==ToolMode.pick)).row();
        t.button("@editor.mu.selectall",Styles.flatt,BlocksSelectionAction::selectAll);
        t.button("@editor.mu.invert",Styles.flatt,BlocksSelectionAction::invertSelection).row();
        t.button("@editor.mu.clear",Styles.flatt,()->selection.clearAll()).colspan(2).growX().row();
        t.button("@editor.mu.undo",Styles.flatt,()->history.undo()).disabled(b->!history.canUndo());
        t.button("@editor.mu.redo",Styles.flatt,()->history.redo()).disabled(b->!history.canRedo()).row();
        t.button("@editor.mu.copy",Styles.flatt,MUTools::copySelection);
        t.button("@editor.mu.paste",Styles.flatt,()->pastePreview=!pastePreview).update(b->b.setChecked(pastePreview)).row();
        t.button("@editor.mu.cut",Styles.flatt,MUTools::cutSelection);
        t.button("@editor.mu.fill",Styles.flatt,MUTools::fillSelection).row();
        t.button("@editor.mu.rot",Styles.flatt,()->MUTools.rotateClipboard(true));
        t.button("@editor.mu.flip",Styles.flatt,()->MUTools.flipClipboard(true)).row();
        Label bsl=new Label("Brush Size: "+MUTools.brushSize);
        t.add(bsl).colspan(2).left().padTop(4f).row();
        t.slider(1f,40f,1f,MUTools.brushSize,s->{MUTools.brushSize=(int)s;bsl.setText("Brush Size: "+(int)s);}).colspan(2).width(175f).row();
        t.check("Replace Mode",MUTools.replaceMode,b->MUTools.replaceMode=b).colspan(2).left().row();
        t.check("Draw Floor",MUTools.drawFloor,b->MUTools.drawFloor=b).colspan(2).left().row();
        t.check("Draw Block",MUTools.drawBlock,b->MUTools.drawBlock=b).colspan(2).left().row();
        t.check("Draw Overlay",MUTools.drawOverlay,b->MUTools.drawOverlay=b).colspan(2).left().padBottom(4f).row();
        t.button("@editor.mu.cliffs",Styles.flatt,MUTools::generateAutoCliffs).colspan(2).growX().row();
        t.button("@editor.mu.advrules",Styles.flatt,MUDialogs::showAdvRulesDialog).colspan(2).growX().row();
        t.button("@editor.mu.banned",Styles.flatt,MUDialogs::showBannedContentDialog);
        t.button("@editor.mu.planetbg",Styles.flatt,MUDialogs::showPlanetBackgroundDialog).row();
        t.button("@editor.mu.resize",Styles.flatt,MUDialogs::showResizeDialog);
        t.button("@editor.mu.uiexplorer",Styles.flatt,MUDialogs::showUIExplorerDialog).row();
        t.button("@editor.mu.js",Styles.flatt,MUDialogs::showJSConsoleDialog).colspan(2).growX().row();
        ScrollPane sp=new ScrollPane(t);sp.setScrollingDisabled(true,false);
        window.body.add(sp).maxHeight(Core.graphics.getHeight()*0.68f);
        window.rebuildWindow();
        Core.scene.add(window);
        window.setPosition(Core.graphics.getWidth()-window.getWidth()-10f,Core.graphics.getHeight()-window.getHeight()-10f);
    }
}
