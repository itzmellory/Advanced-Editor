package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.ui.ModFilterOptions.*;
import static mindustry.maps.filters.FilterOption.*;
public class AdvancedCircuitFilter extends GenerateFilter{
    public Block targetFloor=Blocks.air;
    public Block baseFloor=Blocks.darkPanel3;
    public Block traceFloor=Blocks.metalFloor2;
    public Block zoneFloor=Blocks.slag;
    public Block wallBlock=Blocks.air;
    public float gridScale=14f,lineWidth=1f,zoneChance=28f,zoneScale=38f,diagonalBias=0.45f,nodeDensity=35f;
    public boolean clearWalls=false,useTargetOnly=false;
    @Override public String name(){return Core.bundle.get("filter.advancedcircuit.name","Advanced Circuit");}
    @Override public char icon(){return Iconc.blockLogicProcessor;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new BlockOption("targetfloor",()->targetFloor,b->targetFloor=b,floorsOptional),
            new ToggleOption("usetargetonly",()->useTargetOnly,v->useTargetOnly=v),
            new BlockOption("basefloor",()->baseFloor,b->baseFloor=b,advancedredactor.ui.ModFilterOptions.floorsOnly),
            new BlockOption("tracefloor",()->traceFloor,b->traceFloor=b,advancedredactor.ui.ModFilterOptions.floorsOnly),
            new BlockOption("zonefloor",()->zoneFloor,b->zoneFloor=b,floorsOptional),
            new BlockOption("wall",()->wallBlock,b->wallBlock=b,wallsOptional),
            new SliderOption("gridscale",()->gridScale,v->gridScale=v,6f,48f,1f),
            new SliderOption("linewidth",()->lineWidth,v->lineWidth=v,1f,5f,1f),
            new SliderOption("zonescale",()->zoneScale,v->zoneScale=v,12f,120f,1f),
            new SliderOption("zonechance",()->zoneChance,v->zoneChance=v,0f,85f,1f),
            new SliderOption("diagonalbias",()->diagonalBias,v->diagonalBias=v,0f,1f,0.05f),
            new SliderOption("nodedensity",()->nodeDensity,v->nodeDensity=v,0f,100f,1f),
            new ToggleOption("clearwalls",()->clearWalls,v->clearWalls=v)
        };
    }
    @Override public void apply(GenerateInput in){
        if(useTargetOnly&&targetFloor!=Blocks.air&&in.floor!=targetFloor)return;
        int x=in.x,y=in.y;
        int gs=Math.max(6,Math.round(gridScale));
        int lw=Math.max(1,Math.round(lineWidth));
        float zSc=Math.max(12f,zoneScale);
        float chamX=x+((y/4)*4)*diagonalBias*0.35f;
        float chamY=y-((x/4)*4)*diagonalBias*0.35f;
        float zNoise=(float)Simplex.noise2d(seed+101,2,0.5f,1f/zSc,Math.floor(chamX/4f)*4f,Math.floor(chamY/4f)*4f);
        float diagCut=(float)Simplex.noise2d(seed+202,1,0.5f,1f/(zSc*0.7f),(x+y)*0.5f,(x-y)*0.5f);
        float combinedZone=zNoise*0.75f+diagCut*0.25f;
        float zThresh=1f-(zoneChance/100f);
        if(zoneFloor!=Blocks.air&&combinedZone>zThresh){
            float edgeDist=(combinedZone-zThresh)*zSc;
            if(edgeDist>2.4f){
                in.floor=zoneFloor;
                if(clearWalls&&!in.block.synthetic())in.block=Blocks.air;
                return;
            }else if(edgeDist>1.1f){
                in.floor=traceFloor;
                if(wallBlock!=Blocks.air&&!in.block.synthetic())in.block=wallBlock;
                else if(clearWalls&&!in.block.synthetic())in.block=Blocks.air;
                return;
            }
        }
        in.floor=baseFloor;
        int cellX=x/gs,cellY=y/gs;
        int lx=x%gs,ly=y%gs;
        int half=gs/2;
        int h=hash(seed,cellX,cellY);
        int mode=h&7;
        boolean isTrace=false;
        int tol=lw;
        if(mode==0||mode==1){
            if(Math.abs(lx-half)<tol||Math.abs(ly-half)<tol)isTrace=true;
        }else if(mode==2||mode==3){
            int d1=lx-ly;
            if(Math.abs(d1)<tol||(lx<half&&Math.abs(ly-half)<tol)||(ly>half&&Math.abs(lx-half)<tol))isTrace=true;
        }else if(mode==4||mode==5){
            int d2=(gs-1-lx)-ly;
            if(Math.abs(d2)<tol||(lx>half&&Math.abs(ly-half)<tol)||(ly>half&&Math.abs(lx-half)<tol))isTrace=true;
        }else{
            int sub=Math.max(3,gs/3);
            if(lx%sub<lw&&ly>=lw&&ly<gs-lw)isTrace=true;
            if(ly%sub<lw&&lx>=lw&&lx<gs-lw)isTrace=true;
        }
        int pad=Math.max(2,gs/4);
        if((h>>>4)%100<Math.round(nodeDensity)){
            if(Math.abs(lx-half)<=pad&&Math.abs(ly-half)<=pad){
                boolean border=Math.abs(lx-half)==pad||Math.abs(ly-half)==pad;
                boolean center=Math.abs(lx-half)<=1&&Math.abs(ly-half)<=1;
                isTrace=border||center;
            }
        }
        if(isTrace){
            in.floor=traceFloor;
            if(wallBlock!=Blocks.air&&!in.block.synthetic())in.block=wallBlock;
            else if(clearWalls&&!in.block.synthetic())in.block=Blocks.air;
        }else if(clearWalls&&!in.block.synthetic()){
            in.block=Blocks.air;
        }
    }
    private static int hash(int seed,int x,int y){
        int v=seed^x*0x9e3779b9^y*0x85ebca6b;
        v^=v>>>16;v*=0x7feb352d;v^=v>>>15;
        return v&0x7fffffff;
    }
}
