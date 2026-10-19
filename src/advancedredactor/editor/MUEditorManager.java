package advancedredactor.editor;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import advancedredactor.editor.MUCore.*;
import advancedredactor.editor.MUTools.*;
public class MUEditorManager{
    public static EditorOperationStack history=new EditorOperationStack();
    public static ChunkedGridBits selection=new ChunkedGridBits(500,500);
    public static Seq<TileData> clipboard=new Seq<>();
    public static int clipW=0,clipH=0;
    private static Table panel;
    private static boolean active=false,minimized=false;
    private static int dragStartX=-1,dragStartY=-1;
    private static BlocksTilesOperation activeDrawOp=null;
    public static void init(){
        Events.run(Trigger.update,MUEditorManager::update);
        Events.run(Trigger.draw,MUEditorManager::drawOverlay);
    }
    private static void update(){
        boolean inEditor=Vars.ui!=null&&Vars.ui.editor!=null&&Vars.ui.editor.isShown();
        if(inEditor&&!active){
            selection=new ChunkedGridBits(Math.max(1,Vars.world.width()),Math.max(1,Vars.world.height()));
            buildPanel();active=true;
        }else if(!inEditor&&active){
            if(panel!=null)panel.remove();
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
        if(ctrl&&Core.input.keyTap(KeyCode.c)){MUTools.copySelection();Vars.ui.showInfoToast("Copied "+clipboard.size+" tiles",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.x)){MUTools.cutSelection();Vars.ui.showInfoToast("Cut selection",1.5f);}
        if(ctrl&&Core.input.keyTap(KeyCode.v)){
            Vec2 m=Core.input.mouseWorld();
            MUTools.pasteClipboard((int)(m.x/8f),(int)(m.y/8f));
        }
    }
    private static void handleMouse(){
        if(MUTools.currentTool==ToolMode.none||Core.scene.hasMouse())return;
        Vec2 m=Core.input.mouseWorld();
        int tx=(int)(m.x/8f),ty=(int)(m.y/8f);
        if(MUTools.currentTool==ToolMode.pick&&Core.input.keyTap(KeyCode.mouseLeft)){
            MUTools.pickTile(tx,ty);return;
        }
        if(MUTools.currentTool==ToolMode.selectRect){
            if(Core.input.keyTap(KeyCode.mouseLeft)){dragStartX=tx;dragStartY=ty;}
            if(!Core.input.keyDown(KeyCode.mouseLeft)&&dragStartX!=-1){
                MUTools.selectRect(dragStartX,dragStartY,tx,ty,Core.input.keyDown(KeyCode.shiftLeft));
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
        if(!active||selection.isEmpty())return;
        Draw.color(Color.cyan,0.28f);
        int w=Vars.world.width(),h=Vars.world.height();
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(selection.get(x,y))Fill.rect(x*8f+4f,y*8f+4f,8f,8f);
        }
        Draw.reset();
    }
    private static void buildPanel(){
        if(panel!=null)panel.remove();
        panel=new Table(Styles.black6);
        panel.top().right().margin(6f);
        Table header=new Table();
        header.add("[accent]Mapping Utilities[]").growX().left().padRight(8f);
        header.button(minimized?"+":"-",Styles.flatt,()->{minimized=!minimized;buildPanel();}).size(28f,28f);
        panel.add(header).growX().row();
        if(!minimized){
            Table tools=new Table();
            tools.defaults().size(85f,32f).pad(1.5f);
            tools.button("@editor.mu.tool.none",Styles.flatt,()->MUTools.currentTool=ToolMode.none).update(b->b.setChecked(MUTools.currentTool==ToolMode.none));
            tools.button("@editor.mu.tool.rect",Styles.flatt,()->MUTools.currentTool=ToolMode.brushRect).update(b->b.setChecked(MUTools.currentTool==ToolMode.brushRect)).row();
            tools.button("@editor.mu.tool.circle",Styles.flatt,()->MUTools.currentTool=ToolMode.brushCircle).update(b->b.setChecked(MUTools.currentTool==ToolMode.brushCircle));
            tools.button("@editor.mu.tool.select",Styles.flatt,()->MUTools.currentTool=ToolMode.selectRect).update(b->b.setChecked(MUTools.currentTool==ToolMode.selectRect)).row();
            tools.button("@editor.mu.tool.pick",Styles.flatt,()->MUTools.currentTool=ToolMode.pick).update(b->b.setChecked(MUTools.currentTool==ToolMode.pick));
            tools.button("@editor.mu.clear",Styles.flatt,()->selection.clearAll()).row();
            tools.button("@editor.mu.undo",Styles.flatt,()->history.undo()).disabled(b->!history.canUndo());
            tools.button("@editor.mu.redo",Styles.flatt,()->history.redo()).disabled(b->!history.canRedo()).row();
            tools.button("@editor.mu.copy",Styles.flatt,MUTools::copySelection);
            tools.button("@editor.mu.paste",Styles.flatt,()->{Vec2 m=Core.input.mouseWorld();MUTools.pasteClipboard((int)(m.x/8f),(int)(m.y/8f));}).row();
            tools.button("@editor.mu.cut",Styles.flatt,MUTools::cutSelection);
            tools.button("@editor.mu.fill",Styles.flatt,MUTools::fillSelection).row();
            tools.button("@editor.mu.cliffs",Styles.flatt,MUTools::generateAutoCliffs).colspan(2).growX().row();
            tools.button("@editor.mu.banned",Styles.flatt,MUDialogs::showBannedContentDialog);
            tools.button("@editor.mu.planetbg",Styles.flatt,MUDialogs::showPlanetBackgroundDialog).row();
            tools.button("@editor.mu.resize",Styles.flatt,MUDialogs::showResizeDialog);
            tools.button("@editor.mu.uiexplorer",Styles.flatt,MUDialogs::showUIExplorerDialog).row();
            tools.button("@editor.mu.js",Styles.flatt,MUDialogs::showJSConsoleDialog).colspan(2).growX().row();
            panel.add(tools).row();
        }
        Core.scene.add(panel);
        panel.pack();
        panel.setPosition(Core.graphics.getWidth()-10f,Core.graphics.getHeight()-10f,Align.topRight);
    }
          }
