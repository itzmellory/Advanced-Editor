package advancedredactor.generation;
import arc.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import java.lang.reflect.*;
import static mindustry.maps.filters.FilterOption.*;
public class AdvancedBlendFilter extends GenerateFilter{
    public float radius=2f;
    public Block block=Blocks.sand,floor=Blocks.sand,onlyon=Blocks.stone,ignore=Blocks.air;
    public boolean inverseIgnore=true;
    private static transient Method tileMethod;
    static{
        try{
            tileMethod=GenerateInput.class.getDeclaredMethod("tile",float.class,float.class);
            tileMethod.setAccessible(true);
        }catch(Exception ignored){}
    }
    @Override public String name(){return Core.bundle.get("filter.advancedblend.name","Advanced Blend");}
    @Override public char icon(){return Iconc.blockSandWater;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("radius",()->radius,f->radius=f,1f,12f,1f),
            new BlockOption("block",()->block,b->block=b,anyOptional),
            new BlockOption("floor",()->floor,b->floor=b,floorsOptional),
            new BlockOption("onlyon",()->onlyon,b->onlyon=b,floorsOptional),
            new BlockOption("ignore",()->ignore,b->ignore=b,floorsOptional),
            new ToggleOption("inverseignore",()->inverseIgnore,b->inverseIgnore=b)
        };
    }
    @Override public boolean isBuffered(){return true;}
    @Override public void apply(GenerateInput in){
        if(in.floor==block||block==Blocks.air)return;
        if(inverseIgnore){
            if(onlyon!=Blocks.air&&in.floor!=onlyon)return;
        }else{
            if(in.floor==ignore)return;
        }
        int rad=(int)radius;
        boolean found=false;
        outer:for(int x=-rad;x<=rad;x++){
            for(int y=-rad;y<=rad;y++){
                if(x*x+y*y>rad*rad)continue;
                Tile tile=readTile(in,in.x+x,in.y+y);
                if(tile!=null&&(tile.floor()==block||tile.block()==block||tile.overlay()==block)){
                    found=true;
                    break outer;
                }
            }
        }
        if(found)in.floor=floor;
    }
    private static Tile readTile(GenerateInput in,int x,int y){
        try{if(tileMethod!=null)return(Tile)tileMethod.invoke(in,(float)x,(float)y);}catch(Exception ignored){}
        return null;
    }
}
