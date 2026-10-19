package advancedredactor.editor;
import arc.struct.*;
import mindustry.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
public class MUCore{
    public static class ChunkedGridBits{
        public final int width,height;
        private final IntSet set=new IntSet();
        public ChunkedGridBits(int w,int h){width=w;height=h;}
        public void set(int x,int y,boolean b){if(x<0||y<0||x>=width||y>=height)return;if(b)set.add(x+y*width);else set.remove(x+y*width);}
        public void set(int x,int y){set(x,y,true);}
        public void clear(int x,int y){set(x,y,false);}
        public boolean get(int x,int y){return x>=0&&y>=0&&x<width&&y<height&&set.contains(x+y*width);}
        public void clearAll(){set.clear();}
        public boolean isEmpty(){return set.isEmpty();}
        public int size(){return set.size;}
        public IntSet.IntSetIterator iterator(){return set.iterator();}
        public ChunkedGridBits copy(){
            ChunkedGridBits c=new ChunkedGridBits(width,height);
            c.set.addAll(set);return c;
        }
    }
    public static class TileData{
        public int x,y;public Block floor,block,overlay;public short rotation,team;public Object config;
        public TileData(int x,int y,Block f,Block b,Block o,short r,short t,Object cfg){
            this.x=x;this.y=y;floor=f;block=b;overlay=o;rotation=r;team=t;config=cfg;
        }
        public static TileData from(Tile t){
            return new TileData(t.x,t.y,t.floor(),t.block(),t.overlay(),(short)t.build.rotation,(short)t.getTeamID(),t.build!=null?t.build.config():null);
        }
        public static TileData fromSafe(Tile t){
            return new TileData(t.x,t.y,t.floor(),t.block(),t.overlay(),(short)(t.build!=null?t.build.rotation:0),(short)t.getTeamID(),t.build!=null?t.build.config():null);
        }
        public void applyTo(Tile t){
            if(t==null)return;
            if(floor instanceof Floor)t.setFloor((Floor)floor);
            if(overlay instanceof Floor)t.setOverlay(overlay);
            if(block!=null){
                t.setBlock(block,mindustry.game.Team.get(team),rotation);
                if(t.build!=null&&config!=null)t.build.configureAny(config);
            }
        }
    }
    public interface EditorOperation{void undo();void redo();}
    public static class EditorOperationStack{
        private final Seq<EditorOperation> stack=new Seq<>();
        private int index=-1;
        public void push(EditorOperation op){
            if(index<stack.size-1)stack.truncate(index+1);
            stack.add(op);index++;
            if(stack.size>100){stack.remove(0);index--;}
        }
        public void undo(){if(canUndo()){stack.get(index).undo();index--;refreshMap();}}
        public void redo(){if(canRedo()){index++;stack.get(index).redo();refreshMap();}}
        public boolean canUndo(){return index>=0;}
        public boolean canRedo(){return index<stack.size-1;}
        public void clear(){stack.clear();index=-1;}
        public static void refreshMap(){if(Vars.renderer!=null&&Vars.renderer.blocks!=null)Vars.renderer.blocks.floor.clearTiles();}
    }
    public static class BlocksSelectionOperation implements EditorOperation{
        public final ChunkedGridBits before,after;
        public BlocksSelectionOperation(ChunkedGridBits b,ChunkedGridBits a){before=b.copy();after=a.copy();}
        @Override public void undo(){MUEditorManager.selection=before.copy();}
        @Override public void redo(){MUEditorManager.selection=after.copy();}
    }
    public static class BlocksTilesOperation implements EditorOperation{
        public final IntMap<TileData> before=new IntMap<>(),after=new IntMap<>();
        public void captureBefore(int x,int y){
            Tile t=Vars.world.tile(x,y);
            if(t!=null){int k=x+y*Vars.world.width();if(!before.containsKey(k))before.put(k,TileData.fromSafe(t));}
        }
        public void commit(){
            for(IntMap.Entry<TileData> e:before.entries()){
                Tile t=Vars.world.tile(e.value.x,e.value.y);
                if(t!=null)after.put(e.key,TileData.fromSafe(t));
            }
            if(!before.isEmpty())MUEditorManager.history.push(this);
            EditorOperationStack.refreshMap();
        }
        @Override public void undo(){for(TileData td:before.values())td.applyTo(Vars.world.tile(td.x,td.y));}
        @Override public void redo(){for(TileData td:after.values())td.applyTo(Vars.world.tile(td.x,td.y));}
    }
}
