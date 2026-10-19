package advancedredactor.editor;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.editor.MUCore.*;
public class MUTools{
    public enum ToolMode{none,brushRect,brushCircle,selectRect,pick}
    public static ToolMode currentTool=ToolMode.none;
    public static int brushSize=3;
    public static boolean replaceMode=false;
    public static void applyBrush(int cx,int cy,boolean erase,BlocksTilesOperation op){
        Block drawBlock=erase?Blocks.air:Vars.editor.drawBlock;
        if(drawBlock==null)return;
        int r=brushSize-1;
        for(int dx=-r;dx<=r;dx++)for(int dy=-r;dy<=r;dy++){
            if(currentTool==ToolMode.brushCircle&&dx*dx+dy*dy>r*r+0.5f)continue;
            int wx=cx+dx,wy=cy+dy;
            Tile t=Vars.world.tile(wx,wy);
            if(t==null)continue;
            if(!MUEditorManager.selection.isEmpty()&&!MUEditorManager.selection.get(wx,wy))continue;
            op.captureBefore(wx,wy);
            if(erase){
                t.setBlock(Blocks.air);t.setOverlay(Blocks.air);
            }else if(drawBlock.isFloor()&&!drawBlock.isOverlay()){
                if(!replaceMode||t.floor()!=Blocks.air)t.setFloor(drawBlock.asFloor());
            }else if(drawBlock.isOverlay()){
                t.setOverlay(drawBlock);
            }else{
                t.setBlock(drawBlock,Vars.editor.drawTeam,Vars.editor.rotation);
            }
        }
    }
    public static void pickTile(int x,int y){
        Tile t=Vars.world.tile(x,y);
        if(t==null)return;
        if(t.block()!=Blocks.air)Vars.editor.drawBlock=t.block();
        else if(t.overlay()!=Blocks.air)Vars.editor.drawBlock=t.overlay();
        else Vars.editor.drawBlock=t.floor();
    }
    public static void selectRect(int x1,int y1,int x2,int y2,boolean additive){
        ChunkedGridBits before=MUEditorManager.selection.copy();
        if(!additive)MUEditorManager.selection.clearAll();
        int minX=Math.min(x1,x2),maxX=Math.max(x1,x2),minY=Math.min(y1,y2),maxY=Math.max(y1,y2);
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)MUEditorManager.selection.set(x,y,true);
        MUEditorManager.history.push(new BlocksSelectionOperation(before,MUEditorManager.selection));
    }
    public static void copySelection(){
        MUEditorManager.clipboard.clear();
        int w=Vars.world.width(),h=Vars.world.height();
        int minX=w,minY=h,maxX=0,maxY=0;
        boolean any=false;
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(MUEditorManager.selection.get(x,y)){
                any=true;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
            }
        }
        if(!any)return;
        MUEditorManager.clipW=maxX-minX+1;MUEditorManager.clipH=maxY-minY+1;
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++){
            if(MUEditorManager.selection.get(x,y)){
                Tile t=Vars.world.tile(x,y);
                if(t!=null){
                    TileData td=TileData.fromSafe(t);
                    td.x=x-minX;td.y=y-minY;
                    MUEditorManager.clipboard.add(td);
                }
            }
        }
    }
    public static void cutSelection(){
        copySelection();
        if(MUEditorManager.clipboard.isEmpty())return;
        BlocksTilesOperation op=new BlocksTilesOperation();
        int w=Vars.world.width(),h=Vars.world.height();
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(MUEditorManager.selection.get(x,y)){
                Tile t=Vars.world.tile(x,y);
                if(t!=null){op.captureBefore(x,y);t.setBlock(Blocks.air);t.setOverlay(Blocks.air);}
            }
        }
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
                TileData copy=new TileData(wx,wy,td.floor,td.block,td.overlay,td.rotation,td.team,td.config);
                copy.applyTo(t);
            }
        }
        op.commit();
    }
    public static void fillSelection(){
        Block b=Vars.editor.drawBlock;
        if(b==null||MUEditorManager.selection.isEmpty())return;
        BlocksTilesOperation op=new BlocksTilesOperation();
        int w=Vars.world.width(),h=Vars.world.height();
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(MUEditorManager.selection.get(x,y)){
                Tile t=Vars.world.tile(x,y);
                if(t!=null){
                    op.captureBefore(x,y);
                    if(b.isFloor()&&!b.isOverlay())t.setFloor(b.asFloor());
                    else if(b.isOverlay())t.setOverlay(b);
                    else t.setBlock(b,Vars.editor.drawTeam,Vars.editor.rotation);
                }
            }
        }
        op.commit();
    }
    public static void generateAutoCliffs(){
        BlocksTilesOperation op=new BlocksTilesOperation();
        int w=Vars.world.width(),h=Vars.world.height();
        boolean hasSel=!MUEditorManager.selection.isEmpty();
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(hasSel&&!MUEditorManager.selection.get(x,y))continue;
            Tile t=Vars.world.tile(x,y);
            if(t!=null&&t.block()==Blocks.air){
                int mask=0;
                for(int i=0;i<8;i++){
                    Point2 p=Geometry.d8[i];
                    Tile n=Vars.world.tile(x+p.x,y+p.y);
                    if(n!=null&&n.block()!=Blocks.air&&n.block().isStatic()&&n.block()!=Blocks.cliff){mask|=(1<<i);}
                }
                if(mask!=0){
                    op.captureBefore(x,y);
                    t.setBlock(Blocks.cliff);
                    t.data=(byte)mask;
                }
            }
        }
        op.commit();
    }
}
