package advancedredactor.editor;
import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.editor.MUCore.*;
public class MUTools{
    public enum EditorMode{vanilla,navigation,blocks}
    public enum ToolMode{none,brushRect,brushCircle,selectRect,pick,line,fill}
    public static EditorMode editorMode=EditorMode.blocks;
    public static ToolMode currentTool=ToolMode.none;
    public static int brushSize=3,brushW=3,brushH=3;
    public static boolean replaceMode=false,squareBrush=false,drawFloor=true,drawBlock=true,drawOverlay=true,respectSelection=true;
    public static abstract class BlocksBrush{
        public abstract void each(int cx,int cy,Intc2 cons);
    }
    public static class RectBrush extends BlocksBrush{
        public int width,height;public boolean circle;
        public RectBrush(int w,int h,boolean c){width=w;height=h;circle=c;}
        @Override public void each(int cx,int cy,Intc2 cons){
            int rx=Math.max(0,width-1),ry=Math.max(0,height-1);
            for(int dx=-rx;dx<=rx;dx++)for(int dy=-ry;dy<=ry;dy++){
                if(circle&&rx>0&&ry>0){
                    float nx=(float)dx/rx,ny=(float)dy/ry;
                    if(nx*nx+ny*ny>1.08f)continue;
                }
                cons.get(cx+dx,cy+dy);
            }
        }
    }
    public static abstract class BlocksAction{public abstract void execute(BlocksTilesOperation op);}
    public static class BlocksDrawAction extends BlocksAction{
        public final int x,y;public final boolean erase;public final BlocksBrush brush;
        public BlocksDrawAction(int x,int y,boolean erase,BlocksBrush b){this.x=x;this.y=y;this.erase=erase;brush=b;}
        @Override public void execute(BlocksTilesOperation op){
            Block db=erase?Blocks.air:Vars.editor.drawBlock;
            if(db==null)return;
            brush.each(x,y,(wx,wy)->{
                Tile t=Vars.world.tile(wx,wy);if(t==null)return;
                if(respectSelection&&!MUEditorManager.selection.isEmpty()&&!MUEditorManager.selection.get(wx,wy))return;
                op.captureBefore(wx,wy);
                if(erase){
                    if(drawBlock)t.setBlock(Blocks.air);
                    if(drawOverlay)t.setOverlay(Blocks.air);
                }else if(db.isFloor()&&!db.isOverlay()){
                    if(drawFloor&&(!replaceMode||t.floor()!=Blocks.air))t.setFloor(db.asFloor());
                }else if(db.isOverlay()){
                    if(drawOverlay&&(!replaceMode||t.overlay()!=Blocks.air))t.setOverlay(db);
                }else{
                    if(drawBlock&&(!replaceMode||t.block()!=Blocks.air)){
                        if(db.isMultiblock()){
                            int offset=(db.size-1)/2;
                            for(int bx=0;bx<db.size;bx++)for(int by=0;by<db.size;by++)op.captureBefore(wx-offset+bx,wy-offset+by);
                        }
                        t.setBlock(db,Vars.editor.drawTeam,Vars.editor.rotation);
                    }
                }
            });
        }
    }
    public static class BlocksCliffsAction extends BlocksAction{
        @Override public void execute(BlocksTilesOperation op){
            int w=Vars.world.width(),h=Vars.world.height();
            boolean hasSel=respectSelection&&!MUEditorManager.selection.isEmpty();
            for(int x=0;x<w;x++)for(int y=0;y<h;y++){
                if(hasSel&&!MUEditorManager.selection.get(x,y))continue;
                Tile t=Vars.world.tile(x,y);
                if(t!=null&&t.block()==Blocks.air){
                    int rotation=0;boolean near=false;
                    for(int i=0;i<8;i++){
                        Point2 p=Geometry.d8[i];Tile other=Vars.world.tile(x+p.x,y+p.y);
                        if(other!=null&&other.block().isStatic()&&other.block()!=Blocks.cliff&&other.block()!=Blocks.air){
                            rotation|=(1<<i);near=true;
                        }
                    }
                    if(near){
                        op.captureBefore(x,y);
                        t.setBlock(Blocks.cliff);
                        t.data=(byte)rotation;
                    }
                }
            }
            for(int x=0;x<w;x++)for(int y=0;y<h;y++){
                if(hasSel&&!MUEditorManager.selection.get(x,y))continue;
                Tile t=Vars.world.tile(x,y);
                if(t!=null&&t.block()!=Blocks.air&&t.block()!=Blocks.cliff&&t.block().isStatic()){
                    op.captureBefore(x,y);
                    t.setBlock(Blocks.air);
                }
            }
        }
    }
    public static class BlocksSelectionAction{
        public static void selectRect(int x1,int y1,int x2,int y2,boolean add,boolean sub){
            ChunkedGridBits before=MUEditorManager.selection.copy();
            if(!add&&!sub)MUEditorManager.selection.clearAll();
            MUEditorManager.selection.setArea(x1,y1,x2,y2,!sub);
            MUEditorManager.history.push(new BlocksSelectionOperation(before,MUEditorManager.selection));
        }
        public static void selectAll(){
            ChunkedGridBits before=MUEditorManager.selection.copy();
            MUEditorManager.selection.setArea(0,0,Vars.world.width()-1,Vars.world.height()-1,true);
            MUEditorManager.history.push(new BlocksSelectionOperation(before,MUEditorManager.selection));
        }
        public static void invertSelection(){
            ChunkedGridBits before=MUEditorManager.selection.copy();
            int w=Vars.world.width(),h=Vars.world.height();
            for(int x=0;x<w;x++)for(int y=0;y<h;y++)MUEditorManager.selection.set(x,y,!before.get(x,y));
            MUEditorManager.history.push(new BlocksSelectionOperation(before,MUEditorManager.selection));
        }
    }
    public static void applyBrush(int cx,int cy,boolean erase,BlocksTilesOperation op){
        RectBrush b=new RectBrush(brushSize,brushSize,currentTool==ToolMode.brushCircle);
        new BlocksDrawAction(cx,cy,erase,b).execute(op);
    }
    public static void applyLine(int x1,int y1,int x2,int y2,boolean erase){
        BlocksTilesOperation op=new BlocksTilesOperation();
        Bresenham2.line(x1,y1,x2,y2,(x,y)->applyBrush(x,y,erase,op));
        op.commit();
    }
    public static void pickTile(int x,int y){
        Tile t=Vars.world.tile(x,y);if(t==null)return;
        if(t.block()!=Blocks.air&&t.block()!=Blocks.cliff){
            Vars.editor.drawBlock=t.block();
            Vars.editor.drawTeam=t.team();
            Vars.editor.rotation=t.build!=null?t.build.rotation:0;
        }else if(t.overlay()!=Blocks.air)Vars.editor.drawBlock=t.overlay();
        else Vars.editor.drawBlock=t.floor();
    }
    public static void selectRect(int x1,int y1,int x2,int y2,boolean add){
        BlocksSelectionAction.selectRect(x1,y1,x2,y2,add,Core.input.keyDown(arc.input.KeyCode.altLeft));
    }
    public static void copySelection(){
        MUEditorManager.clipboard.clear();
        if(MUEditorManager.selection.isEmpty())return;
        final int[] bounds={Vars.world.width(),Vars.world.height(),0,0};
        MUEditorManager.selection.each((x,y)->{
            bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.min(bounds[1],y);
            bounds[2]=Math.max(bounds[2],x);bounds[3]=Math.max(bounds[3],y);
        });
        MUEditorManager.clipW=bounds[2]-bounds[0]+1;MUEditorManager.clipH=bounds[3]-bounds[1]+1;
        MUEditorManager.selection.each((x,y)->{
            Tile t=Vars.world.tile(x,y);
            if(t!=null){
                TileData td=TileData.fromSafe(t);
                td.x=x-bounds[0];td.y=y-bounds[1];
                MUEditorManager.clipboard.add(td);
            }
        });
    }
    public static void rotateClipboard(boolean cw){
        if(MUEditorManager.clipboard.isEmpty())return;
        int w=MUEditorManager.clipW,h=MUEditorManager.clipH;
        for(TileData td:MUEditorManager.clipboard){
            int nx=cw?td.y:(h-1-td.y),ny=cw?(w-1-td.x):td.x;
            td.x=nx;td.y=ny;
            td.rotation=(short)((td.rotation+(cw?3:1))&3);
        }
        MUEditorManager.clipW=h;MUEditorManager.clipH=w;
    }
    public static void flipClipboard(boolean horiz){
        if(MUEditorManager.clipboard.isEmpty())return;
        int w=MUEditorManager.clipW,h=MUEditorManager.clipH;
        for(TileData td:MUEditorManager.clipboard){
            if(horiz){td.x=w-1-td.x;if(td.rotation%2==0)td.rotation=(short)((td.rotation+2)&3);}
            else{td.y=h-1-td.y;if(td.rotation%2==1)td.rotation=(short)((td.rotation+2)&3);}
        }
    }
    public static void cutSelection(){
        copySelection();
        if(MUEditorManager.clipboard.isEmpty())return;
        BlocksTilesOperation op=new BlocksTilesOperation();
        MUEditorManager.selection.each((x,y)->{
            Tile t=Vars.world.tile(x,y);
            if(t!=null){
                op.captureBefore(x,y);
                if(drawBlock)t.setBlock(Blocks.air);
                if(drawOverlay)t.setOverlay(Blocks.air);
            }
        });
        op.commit();
    }
    public static void pasteClipboard(int targetX,int targetY){
        if(MUEditorManager.clipboard.isEmpty())return;
        BlocksTilesOperation op=new BlocksTilesOperation();
        int ox=targetX-MUEditorManager.clipW/2,oy=targetY-MUEditorManager.clipH/2;
        for(TileData td:MUEditorManager.clipboard){
            int wx=ox+td.x,wy=oy+td.y;
            Tile t=Vars.world.tile(wx,wy);
            if(t!=null){
                op.captureBefore(wx,wy);
                if(td.block!=null&&td.block.isMultiblock()){
                    int off=(td.block.size-1)/2;
                    for(int bx=0;bx<td.block.size;bx++)for(int by=0;by<td.block.size;by++)op.captureBefore(wx-off+bx,wy-off+by);
                }
                TileData copy=new TileData(wx,wy,drawFloor?td.floor:t.floor(),drawBlock?td.block:t.block(),drawOverlay?td.overlay:t.overlay(),td.rotation,td.team,td.data,td.config);
                copy.applyTo(t);
            }
        }
        op.commit();
    }
    public static void fillSelection(){
        Block b=Vars.editor.drawBlock;
        if(b==null||MUEditorManager.selection.isEmpty())return;
        BlocksTilesOperation op=new BlocksTilesOperation();
        MUEditorManager.selection.each((x,y)->{
            Tile t=Vars.world.tile(x,y);
            if(t!=null){
                op.captureBefore(x,y);
                if(b.isFloor()&&!b.isOverlay()){if(drawFloor)t.setFloor(b.asFloor());}
                else if(b.isOverlay()){if(drawOverlay)t.setOverlay(b);}
                else if(drawBlock)t.setBlock(b,Vars.editor.drawTeam,Vars.editor.rotation);
            }
        });
        op.commit();
    }
    public static void generateAutoCliffs(){
        BlocksTilesOperation op=new BlocksTilesOperation();
        new BlocksCliffsAction().execute(op);
        op.commit();
    }
}
