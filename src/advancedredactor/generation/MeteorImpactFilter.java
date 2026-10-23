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
public class MeteorImpactFilter extends GenerateFilter{
    public Block craterFloor=Blocks.sand;
    public Block coreFloor=Blocks.basalt;
    public Block meltFloor=Blocks.magmarock;
    public Block rimWall=Blocks.duneWall;
    public Block ore=Blocks.oreThorium;
    public float count=3f;
    public float radius=26f;
    public float spikiness=0.45f;
    public float oreChance=0.22f;
    @Override public String name(){return Core.bundle.get("filter.meteorimpact.name","Meteor Impacts");}
    @Override public char icon(){return Iconc.blockThoriumWallLarge;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new BlockOption("craterFloor",()->craterFloor,b->craterFloor=b,floorsOnly),
            new BlockOption("coreFloor",()->coreFloor,b->coreFloor=b,floorsOnly),
            new BlockOption("meltFloor",()->meltFloor,b->meltFloor=b,floorsOnly),
            new BlockOption("rimWall",()->rimWall,b->rimWall=b,wallsOnly),
            new BlockOption("ore",()->ore,b->ore=b,oresOnly),
            new SliderOption("count",()->count,f->count=f,1f,15f,1f),
            new SliderOption("radius",()->radius,f->radius=f,10f,80f,1f),
            new SliderOption("spikiness",()->spikiness,f->spikiness=f,0.1f,1f,0.05f),
            new SliderOption("oreChance",()->oreChance,f->oreChance=f,0f,0.6f,0.02f)
        };
    }
    @Override public void apply(GenerateInput in){
        int num=Math.max(1,Math.round(count));
        float nDetail=(float)Simplex.noise2d(seed+11,3,0.6f,1f/5f,in.x,in.y);
        float nFine=(float)Simplex.noise2d(seed+23,2,0.5f,1f/2.2f,in.x,in.y);
        for(int i=0;i<num;i++){
            long cSeed=seed+i*9973L+1337L;
            float margin=radius*0.8f;
            float cx=margin+Mathf.randomSeed(cSeed,0f,Math.max(1f,in.width-margin*2f));
            float cy=margin+Mathf.randomSeed(cSeed+1,0f,Math.max(1f,in.height-margin*2f));
            float rScale=Mathf.randomSeed(cSeed+2,0.75f,1.25f);
            float R=radius*rScale;
            float dx=in.x-cx,dy=in.y-cy;
            float dst=Mathf.sqrt(dx*dx+dy*dy);
            if(dst>R*1.35f)continue;
            float ang=Angles.angle(cx,cy,in.x,in.y)*Mathf.degRad;
            float rayNoise=(float)Simplex.noise2d(seed+i*31,3,0.65f,1.8f,Mathf.cos(ang)*3.5f,Mathf.sin(ang)*3.5f);
            float warpR=R*(1f+rayNoise*0.28f*spikiness+nDetail*0.12f);
            if(dst>warpR)continue;
            float norm=dst/warpR;
            in.floor=craterFloor;
            in.block=Blocks.air;
            float coreLimit=0.36f+rayNoise*0.22f*spikiness;
            if(norm<coreLimit){
                in.floor=(norm<coreLimit*0.55f&&nFine>0.08f)?meltFloor:coreFloor;
                if(nFine>-0.1f&&norm<coreLimit*0.85f){
                    in.overlay=ore;
                }
            }else if(norm>0.76f&&norm<0.98f){
                if(rayNoise+nDetail*0.55f>-0.18f){
                    in.block=rimWall;
                }
            }else{
                boolean spikeRay=rayNoise>0.32f&&norm<0.74f&&(Math.abs(nDetail)>0.2f);
                if(spikeRay){
                    in.floor=coreFloor;
                    if(nFine>0.25f)in.block=rimWall;
                }else if(Mathf.chance(oreChance*0.35f)&&nFine>0.32f){
                    in.overlay=ore;
                }
            }
        }
    }
}
