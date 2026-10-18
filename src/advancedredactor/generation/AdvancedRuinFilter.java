package advancedredactor.generation;
import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.ui.ModFilterOptions.*;
import java.io.Serializable;
import java.util.*;
import static mindustry.maps.filters.FilterOption.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedRuinFilter extends GenerateFilter {
    public Block replaceOverlay=Blocks.metalTiles1;
    public CenterMode centerMode=CenterMode.overlay;
    public GenerationMode generationMode=GenerationMode.structured;
    public StructurePreset structurePreset=StructurePreset.large;
    public Block structureFloor=Blocks.metalFloor;
    public Block structureMainFloor=Blocks.metalTiles1;
    public Block structureWall=Blocks.metalWall2;
    public Block structureAccentWall=Blocks.metalWall1;
    public Block structureDamageFloor=Blocks.metalFloorDamaged;
    public float autoDensity=25f;
    public float minCenterSpacing=24f;
    public RuinStep[] steps=RuinStep.defaults();
    public float scl=40f,threshold=0.5f,octaves=3f,falloff=0.5f,tilt=0f;
    public float distortScl=10f,distortMag=5f;
    private transient IntSeq centers;
    private transient CenterIndex centerIndex;
    private transient TileState[] computed;
    private transient boolean[] floorModified;
    private transient boolean[] blockModified;
    private transient boolean mapComputed;
    private transient int tilesLeft;
    private transient int computedWidth;
    private transient int computedHeight;
    private transient GenerateInput computedInput;
    @Override public String name(){return Core.bundle.get("filter.advancedruin.name","Advanced Ruins");}
    @Override public char icon(){return Iconc.blockStoneWall;}
    @Override public FilterOption[] options(){
        ensureDefaults();
        ArrayList<FilterOption> options=new ArrayList<>();
        options.add(new BlockOption("replace",()->replaceOverlay,b->replaceOverlay=b,oresFloorsOptional));
        options.add(new SliderOption("centermode",()->(float)centerMode.ordinal(),v->centerMode=CenterMode.values()[Math.round(v)%2],0f,1f,1f));
        options.add(new SliderOption("autodensity",()->autoDensity,v->autoDensity=v,0f,100f,1f));
        options.add(new SliderOption("mincenterspacing",()->minCenterSpacing,v->minCenterSpacing=v,0f,256f,1f));
        options.add(new SliderOption("generationmode",()->(float)generationMode.ordinal(),v->generationMode=GenerationMode.values()[Math.round(v)%2],0f,1f,1f));
        options.add(new SliderOption("structurepreset",()->(float)structurePreset.ordinal(),v->structurePreset=StructurePreset.values()[Math.round(v)%3],0f,2f,1f));
        options.add(new BlockOption("structurefloor",()->structureFloor,b->structureFloor=b,floorsOptional));
        options.add(new BlockOption("structuremainfloor",()->structureMainFloor,b->structureMainFloor=b,floorsOptional));
        options.add(new BlockOption("structurewall",()->structureWall,b->structureWall=b,b->b==Blocks.air||(wallsOptional.get(b)&&b.isStatic())));
        options.add(new BlockOption("structureaccentwall",()->structureAccentWall,b->structureAccentWall=b,b->b==Blocks.air||(wallsOptional.get(b)&&b.isStatic())));
        options.add(new BlockOption("structuredamagefloor",()->structureDamageFloor,b->structureDamageFloor=b,floorsOptional));
        options.add(new SliderOption("scale",()->scl,v->scl=v,1f,500f,1f));
        options.add(new SliderOption("threshold",()->threshold,v->threshold=v,0f,1f,0.01f));
        options.add(new SliderOption("octaves",()->octaves,v->octaves=v,1f,10f,1f));
        options.add(new SliderOption("falloff",()->falloff,v->falloff=v,0f,1f,0.01f));
        options.add(new SliderOption("tilt",()->tilt,v->tilt=v,-4f,4f,0.01f));
        options.add(new SliderOption("distortscale",()->distortScl,v->distortScl=v,1f,20f,0.1f));
        options.add(new SliderOption("distortmag",()->distortMag,v->distortMag=v,0f,10f,0.1f));
        return options.toArray(new FilterOption[0]);
    }
    @Override public void apply(GenerateInput input){
        if(input==null||input.width<=0||input.height<=0)return;
        int size=input.width*input.height;
        if(mapComputed&&computedInput!=input)resetComputedMap();
        if(mapComputed&&input.x==0&&input.y==0&&tilesLeft<size)resetComputedMap();
        if(!mapComputed||computedWidth!=input.width||computedHeight!=input.height){
            ensureDefaults();
            computedWidth=input.width;
            computedHeight=input.height;
            computedInput=input;
            rebuildCenters(input);
            computeMap(input);
            mapComputed=true;
            tilesLeft=size;
        }
        writeTile(input);
        if(--tilesLeft<=0)resetComputedMap();
    }
    public int centerCount(){return centers==null?0:centers.size;}
    private void computeMap(GenerateInput input){
        int width=input.width,height=input.height,size=width*height;
        computed=new TileState[size];
        floorModified=new boolean[size];
        blockModified=new boolean[size];
        for(int index=0;index<size;index++)computed[index]=readState(input,index%width,index/width);
        TileState[] baseline=copyStates(computed);
        boolean structured=usesStructuredMode(width,height);
        if(structured){
            computeStructured(baseline,width,height);
        }else if(steps!=null&&steps.length>0&&centers!=null&&centers.size>0){
            float maxRadius=maxStepRadius();
            centerIndex=new CenterIndex(width,height,maxRadius);
            for(int i=0;i<centers.size;i++){
                int center=centers.get(i);
                centerIndex.add(center%width,center/width);
            }
            for(RuinStep step:steps){
                if(step==null)continue;
                for(int x=0;x<width;x++)for(int y=0;y<height;y++){
                    if(!passesNoise(x,y))continue;
                    int index=x+y*width;
                    if(centerIndex==null||!centerIndex.hasMatch(x,y,step))continue;
                    if(centerMode==CenterMode.auto&&!isAutoProcessable(baseline[index]))continue;
                    applyStep(computed[index],baseline[index],step,index);
                }
            }
        }
        if(!structured)applyDistort(computed,baseline,width,height);
    }
    private void computeStructured(TileState[] baseline,int width,int height){
        int size=width*height;
        StructuredRuinLayout.Spec structureSpec=StructuredRuinLayout.spec(width,height,structurePreset);
        Block[] generatedFloors=new Block[size],generatedWalls=new Block[size];
        boolean[] generatedFloor=new boolean[size],generatedWall=new boolean[size],criticalFloor=new boolean[size],criticalWall=new boolean[size];
        Block staticWall=structureWallBlock(),accentWall=structureAccentWallBlock(),debrisWall=structureDebrisWallBlock();
        for(int centerIndex=0;centerIndex<centers.size;centerIndex++){
            int packed=centers.get(centerIndex),centerX=packed%width,centerY=packed/width;
            StructuredRuinLayout.Layout layout=StructuredRuinLayout.generate(width,height,centerX,centerY,seed,structurePreset);
            for(int localY=0;localY<layout.span;localY++)for(int localX=0;localX<layout.span;localX++){
                int local=localX+localY*layout.span,x=layout.originX+localX,y=layout.originY+localY;
                if(x<0||y<0||x>=width||y>=height)continue;
                int index=x+y*width;
                if(centerMode==CenterMode.auto&&!isAutoProcessable(baseline[index]))continue;
                if(layout.floor[local]){
                    boolean protectedFloor=layout.criticalFloor[local];
                    Block floor=layout.mainFloor[local]?structureMainFloor:layout.panelFloor[local]?structurePanelFloor(x,y,centerX,centerY):structureFloor;
                    if(!(floor instanceof Floor))floor=structureFloor;
                    if(!protectedFloor&&structuredDamage(x,y,centerX,centerY,structureSpec.floorDamage,0x31))floor=structureDamageFloorVariant(x,y,centerX,centerY);
                    if(floor instanceof Floor){
                        if(!generatedFloor[index]||protectedFloor||!criticalFloor[index]){
                            generatedFloors[index]=floor;
                            criticalFloor[index]=protectedFloor;
                        }
                        generatedFloor[index]=true;
                        generatedWall[index]=false;
                        generatedWalls[index]=null;
                    }
                }else if((layout.wall[local]||layout.debrisWall[local])&&!generatedFloor[index]){
                    boolean protectedWall=layout.criticalWall[local];
                    Block wall=layout.mainWall[local]?staticWall:layout.debrisWall[local]?debrisWall:accentWall;
                    if(!protectedWall&&structuredDamage(x,y,centerX,centerY,structureSpec.wallDamage,0x57))wall=Blocks.air;
                    if(wall!=null){
                        if(!generatedWall[index]||protectedWall||!criticalWall[index]){
                            generatedWalls[index]=wall;
                            criticalWall[index]=protectedWall;
                        }
                        generatedWall[index]=true;
                    }
                }
            }
        }
        for(int index=0;index<size;index++){
            if(generatedFloor[index]){
                computed[index].floor=(Floor)generatedFloors[index];
                floorModified[index]=true;
            }
            if(generatedWall[index]&&!generatedFloor[index]){
                computed[index].block=generatedWalls[index];
                blockModified[index]=true;
            }
        }
    }
    private boolean structuredDamage(int x,int y,int centerX,int centerY,float chance,int salt){
        if(chance<=0f)return false;
        float scale=finiteAtLeast(distortScl,1f),magnitude=finiteAtLeast(distortMag,0f);
        float warpedX=x+distortionNoise(salt,x,y,scale,magnitude/2f),warpedY=y+distortionNoise(salt+1,x,y,scale,magnitude/2f);
        return passesNoise(warpedX,warpedY)&&random01(seed^salt^centerX*0x632be59b^centerY*0x85157af5,x,y)<chance;
    }
    private Block structureWallBlock(){return structureWall==Blocks.air||structureWall.isStatic()?structureWall:Blocks.metalWall2;}
    private Block structureAccentWallBlock(){return structureAccentWall==Blocks.air||structureAccentWall.isStatic()?structureAccentWall:Blocks.metalWall1;}
    private Block structureDebrisWallBlock(){
        if(structureWall==Blocks.metalWall3)return structureWall;
        if(structureAccentWall==Blocks.metalWall3)return structureAccentWall;
        return Blocks.metalWall3;
    }
    private Block structurePanelFloor(int x,int y,int centerX,int centerY){
        int value=seed^x*0x9e3779b9^y*0x85ebca6b^centerX*0xc2b2ae35^centerY*0x27d4eb2d;
        value^=value>>>16;
        switch(value&3){
            case 0:return Blocks.metalTiles2;
            case 1:return Blocks.metalTiles3;
            case 2:return Blocks.metalTiles4;
            default:return Blocks.metalTiles2;
        }
    }
    private Block structureDamageFloorVariant(int x,int y,int centerX,int centerY){
        int value=seed^x*0x632be59b^y*0x85157af5^centerX*0xc2b2ae35^centerY*0x27d4eb2d;
        value^=value>>>16;
        switch(value&15){
            case 0:case 1:case 2:case 3:case 4:case 12:case 13:case 14:case 15:return structureDamageFloor;
            case 5:return Blocks.metalFloorDamaged;
            case 6:return Blocks.metalFloor2;
            case 7:return Blocks.metalFloor3;
            case 8:return Blocks.metalFloor4;
            case 9:return Blocks.metalFloor5;
            case 10:return Blocks.metalTiles8;
            default:return Blocks.metalTiles11;
        }
    }
    private void applyDistort(TileState[] state,TileState[] baseline,int width,int height){
        float scale=finiteAtLeast(distortScl,1f),magnitude=finiteAtLeast(distortMag,0f);
        if(magnitude<=0f)return;
        int size=width*height;
        TileState[] source=copyStates(state),destination=copyStates(baseline);
        boolean[] newFloorModified=new boolean[size],newBlockModified=new boolean[size];
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            int index=x+y*width;
            if(!floorModified[index]&&!blockModified[index])continue;
            int outputX=sampleCoordinate(x,distortionNoise(0,x,y,scale,magnitude/2f),width);
            int outputY=sampleCoordinate(y,distortionNoise(1,x,y,scale,magnitude/2f),height);
            int destinationIndex=outputX+outputY*width;
            if(floorModified[index]){
                destination[destinationIndex].floor=source[index].floor;
                newFloorModified[destinationIndex]=true;
            }
            if(blockModified[index]){
                destination[destinationIndex].block=source[index].block;
                newBlockModified[destinationIndex]=true;
            }
        }
        System.arraycopy(destination,0,state,0,size);
        floorModified=newFloorModified;
        blockModified=newBlockModified
