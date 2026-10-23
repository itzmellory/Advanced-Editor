package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class RichOreClusterFilter extends GenerateFilter{
    public Block ore=Blocks.oreTitanium;
    public Block targetFloor=Blocks.air;
    public Block patchFloor=Blocks.dacite;
    public Block rimFloor=Blocks.basalt;
    public float scale=26f;
    public float threshold=0.38f;
    public float rimSize=0.14f;
    public float oreDensity=0.75f;
    @Override public String name(){return Core.bundle.get("filter.richorecluster.name","Ore Clusters");}
    @Override public char icon(){return Iconc.itemTitanium;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new BlockOption("ore",()->ore,b->ore=b,oresOnly),
            new BlockOption("targetFloor",()->targetFloor,b->targetFloor=b,floorsOptional),
            new BlockOption("patchFloor",()->patchFloor,b->patchFloor=b,floorsOnly),
            new BlockOption("rimFloor",()->rimFloor,b->rimFloor=b,floorsOnly),
            new SliderOption("scale",()->scale,f->scale=f,8f,120f,1f),
            new SliderOption("threshold",()->threshold,f->threshold=f,0.05f,0.85f,0.02f),
            new SliderOption("rimSize",()->rimSize,f->rimSize=f,0f,0.35f,0.02f),
            new SliderOption("oreDensity",()->oreDensity,f->oreDensity=f,0.1f,1f,0.05f)
        };
    }
    @Override public void apply(GenerateInput in){
        if(targetFloor!=Blocks.air&&in.floor!=targetFloor)return;
        float nMain=(float)Simplex.noise2d(seed,3,0.55f,1f/scale,in.x,in.y);
        float nWarp=(float)Simplex.noise2d(seed+77,2,0.5f,1f/(scale*0.35f),in.x,in.y)*0.12f;
        float val=nMain+nWarp;
        if(val>threshold){
            in.block=Blocks.air;
            if(val>threshold+rimSize){
                in.floor=patchFloor;
                float nOre=(float)Simplex.noise2d(seed+199,2,0.6f,1f/4.5f,in.x,in.y);
                if(nOre>1f-oreDensity*1.6f){
                    in.overlay=ore;
                }
            }else{
                in.floor=rimFloor;
            }
        }
    }
}
