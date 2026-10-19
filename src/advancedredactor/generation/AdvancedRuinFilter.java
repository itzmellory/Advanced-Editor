package advancedredactor.generation;
import arc.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.ui.ModFilterOptions.*;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import static mindustry.maps.filters.FilterOption.*;
public class AdvancedRuinFilter extends GenerateFilter{
    public Block replaceOverlay=Blocks.coreZone;
    public CenterMode centerMode=CenterMode.auto;
    public GenerationMode generationMode=GenerationMode.structured;
    public StructurePreset structurePreset=StructurePreset.large;
    public Block structureFloor=Blocks.metalFloor,structureMainFloor=Blocks.darkPanel1;
    public Block structureWall=Blocks.darkMetal,structureAccentWall=Blocks.duneWall,structureDamageFloor=Blocks.metalFloorDamaged;
    public float autoDensity=25f,minCenterSpacing=24f;
    public RuinStep[] steps=RuinStep.defaults();
    public float scl=40f,threshold=0.5f,octaves=3f,falloff=0.5f,tilt=0f,distortScl=10f,distortMag=5f;
    private transient IntSeq centers;
    private transient CenterIndex centerIndex;
    private transient TileState[] computed;
    private transient boolean[] floorModified,blockModified;
    private transient boolean mapComputed;
    private transient int tilesLeft,computedWidth,computedHeight;
    private transient GenerateInput computedInput;
    private static transient Method tileMethod;
    static{
        try{
            tileMethod=GenerateInput.class.getDeclaredMethod("tile",float.class,float.class);
            tileMethod.setAccessible(true);
        }catch(Exception ignored){}
    }
    @Override public String name(){return Core.bundle.get("filter.advancedruin.name","Advanced Ruins");}
    @Override public char icon(){return Iconc.blockScrapWall;}
    @Override public FilterOption[] options(){
        ensureDefaults();
        ArrayList<FilterOption> o=new ArrayList<>();
        o.add(new BlockOption("replace",()->replaceOverlay,b->replaceOverlay=b,oresFloorsOptional));
        o.add(new CenterModeOption(this));
        o.add(new SliderOption("autodensity",()->autoDensity,v->autoDensity=v,0f,100f,1f));
        o.add(new SliderOption("mincenterspacing",()->minCenterSpacing,v->minCenterSpacing=v,0f,256f,1f));
        o.add(new GenerationModeOption(this));
        if(generationMode==GenerationMode.structured){
            o.add(new StructurePresetOption(this));
            o.add(new BlockOption("structurefloor",()->structureFloor,b->structureFloor=b,floorsOptional));
            o.add(new BlockOption("structuremainfloor",()->structureMainFloor,b->structureMainFloor=b,floorsOptional));
            o.add(new BlockOption("structurewall",()->structureWall,b->structureWall=b,b->b==Blocks.air||wallsOptional.get(b)&&b.isStatic()));
            o.add(new BlockOption("structureaccentwall",()->structureAccentWall,b->structureAccentWall=b,b->b==Blocks.air||wallsOptional.get(b)&&b.isStatic()));
            o.add(new BlockOption("structuredamagefloor",()->structureDamageFloor,b->structureDamageFloor=b,floorsOptional));
        }else{
            o.add(new StepsEditOption(this));
        }
        o.add(new SliderOption("scale",()->scl,v->scl=v,1f,500f,1f));
        o.add(new SliderOption("threshold",()->threshold,v->threshold=v,0f,1f,0.01f));
        o.add(new SliderOption("octaves",()->octaves,v->octaves=v,1f,10f,1f));
        o.add(new SliderOption("falloff",()->falloff,v->falloff=v,0f,1f,0.01f));
        o.add(new SliderOption("tilt",()->tilt,v->tilt=v,-4f,4f,0.01f));
        o.add(new SliderOption("distortscale",()->distortScl,v->distortScl=v,1f,20f,0.1f));
        o.add(new SliderOption("distortmag",()->distortMag,v->distortMag=v,0f,10f,0.1f));
        return o.toArray(new FilterOption[0]);
    }
    @Override public void apply(GenerateInput in){
        if(in==null||in.width<=0||in.height<=0)return;
        int size=in.width*in.height;
        if(mapComputed&&computedInput!=in)resetComputedMap();
        if(mapComputed&&in.x==0&&in.y==0&&tilesLeft<size)resetComputedMap();
        if(!mapComputed||computedWidth!=in.width||computedHeight!=in.height){
            ensureDefaults();
            computedWidth=in.width;computedHeight=in.height;computedInput=in;
            rebuildCenters(in);computeMap(in);
            mapComputed=true;tilesLeft=size;
        }
        writeTile(in);
        if(--tilesLeft<=0)resetComputedMap();
    }
    private void computeMap(GenerateInput in){
        int w=in.width,h=in.height,size=w*h;
        computed=new TileState[size];floorModified=new boolean[size];blockModified=new boolean[size];
        for(int i=0;i<size;i++)computed[i]=readState(in,i%w,i/w);
        TileState[] bl=copyStates(computed);
        boolean struct=usesStructuredMode(w,h);
        if(struct){
            computeStructured(bl,w,h);
        }else if(steps!=null&&steps.length>0&&centers!=null&&centers.size>0){
            centerIndex=new CenterIndex(w,h,maxStepRadius());
            for(int i=0;i<centers.size;i++)centerIndex.add(centers.get(i)%w,centers.get(i)/w);
            for(RuinStep step:steps){
                if(step==null)continue;
                for(int x=0;x<w;x++)for(int y=0;y<h;y++){
                    if(!passesNoise(x,y))continue;
                    int i=x+y*w;
                    if(centerIndex==null||!centerIndex.hasMatch(x,y,step))continue;
                    if(centerMode==CenterMode.auto&&!isAutoProcessable(bl[i]))continue;
                    applyStep(computed[i],bl[i],step,i);
                }
            }
        }
        if(!struct)applyDistort(computed,bl,w,h);
    }
    private void computeStructured(TileState[] bl,int w,int h){
        int size=w*h;
        StructuredRuinLayout.Spec sp=StructuredRuinLayout.spec(w,h,structurePreset);
        Block[] gf=new Block[size],gw=new Block[size];
        boolean[] gfb=new boolean[size],gwb=new boolean[size],cf=new boolean[size],cw=new boolean[size];
        Block sw=structureWallBlock(),aw=structureAccentWallBlock(),dw=structureDebrisWallBlock();
        for(int ci=0;ci<centers.size;ci++){
            int cX=centers.get(ci)%w,cY=centers.get(ci)/w;
            StructuredRuinLayout.Layout l=StructuredRuinLayout.generate(w,h,cX,cY,seed,structurePreset);
            for(int ly=0;ly<l.span;ly++)for(int lx=0;lx<l.span;lx++){
                int loc=lx+ly*l.span,x=l.originX+lx,y=l.originY+ly;
                if(x<0||y<0||x>=w||y>=h)continue;
                int i=x+y*w;
                if(centerMode==CenterMode.auto&&!isAutoProcessable(bl[i]))continue;
                if(l.floor[loc]){
                    boolean p=l.criticalFloor[loc];
                    Block f=l.mainFloor[loc]?structureMainFloor:l.panelFloor[loc]?structurePanelFloor(x,y,cX,cY):structureFloor;
                    if(!(f instanceof Floor))f=structureFloor;
                    if(!p&&structuredDamage(x,y,cX,cY,sp.floorDamage,0x31))f=structureDamageFloorVariant(x,y,cX,cY);
                    if(f instanceof Floor){
                        if(!gfb[i]||p||!cf[i]){gf[i]=f;cf[i]=p;}
                        gfb[i]=true;gwb[i]=false;gw[i]=null;
                    }
                }else if((l.wall[loc]||l.debrisWall[loc])&&!gfb[i]){
                    boolean p=l.criticalWall[loc];
                    Block wl=l.mainWall[loc]?sw:l.debrisWall[loc]?dw:aw;
                    if(!p&&structuredDamage(x,y,cX,cY,sp.wallDamage,0x57))wl=Blocks.air;
                    if(wl!=null){
                        if(!gwb[i]||p||!cw[i]){gw[i]=wl;cw[i]=p;}
                        gwb[i]=true;
                    }
                }
            }
        }
        for(int i=0;i<size;i++){
            if(gfb[i]){computed[i].floor=(Floor)gf[i];floorModified[i]=true;}
            if(gwb[i]&&!gfb[i]){computed[i].block=gw[i];blockModified[i]=true;}
        }
    }
    private boolean structuredDamage(int x,int y,int cX,int cY,float c,int salt){
        if(c<=0f)return false;
        float s=finiteAtLeast(distortScl,1f),m=finiteAtLeast(distortMag,0f);
        float wx=x+distortionNoise(salt,x,y,s,m/2f),wy=y+distortionNoise(salt+1,x,y,s,m/2f);
        return passesNoise(wx,wy)&&random01(seed^salt^cX*0x632be59b^cY*0x85157af5,x,y)<c;
    }
    private Block structureWallBlock(){return structureWall==Blocks.air||structureWall.isStatic()?structureWall:Blocks.darkMetal;}
    private Block structureAccentWallBlock(){return structureAccentWall==Blocks.air||structureAccentWall.isStatic()?structureAccentWall:Blocks.duneWall;}
    private Block structureDebrisWallBlock(){
        if(structureWall==Blocks.scrapWall)return structureWall;
        if(structureAccentWall==Blocks.scrapWall)return structureAccentWall;
        return Blocks.scrapWall;
    }
    private Block structurePanelFloor(int x,int y,int cX,int cY){
        int v=seed^x*0x9e3779b9^y*0x85ebca6b^cX*0xc2b2ae35^cY*0x27d4eb2d;
        v^=v>>>16;
        switch(v&3){case 0:return Blocks.darkPanel2;case 1:return Blocks.darkPanel3;case 2:return Blocks.darkPanel4;default:return Blocks.darkPanel2;}
    }
    private Block structureDamageFloorVariant(int x,int y,int cX,int cY){
        int v=seed^x*0x632be59b^y*0x85157af5^cX*0xc2b2ae35^cY*0x27d4eb2d;
        v^=v>>>16;
        switch(v&15){
            case 5:return Blocks.metalFloorDamaged;case 6:return Blocks.metalFloor2;case 7:return Blocks.metalFloor3;case 8:return Blocks.metalFloor4;
            case 9:return Blocks.metalFloor5;case 10:return Blocks.darkPanel5;case 11:return Blocks.darkPanel6;default:return structureDamageFloor;
        }
    }
    private void applyDistort(TileState[] s,TileState[] bl,int w,int h){
        float sc=finiteAtLeast(distortScl,1f),m=finiteAtLeast(distortMag,0f);
        if(m<=0f)return;
        int size=w*h;
        TileState[] src=copyStates(s),dst=copyStates(bl);
        boolean[] nf=new boolean[size],nb=new boolean[size];
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            int i=x+y*w;
            if(!floorModified[i]&&!blockModified[i])continue;
            int ox=sampleCoordinate(x,distortionNoise(0,x,y,sc,m/2f),w);
            int oy=sampleCoordinate(y,distortionNoise(1,x,y,sc,m/2f),h);
            int di=ox+oy*w;
            if(floorModified[i]){dst[di].floor=src[i].floor;nf[di]=true;}
            if(blockModified[i]){dst[di].block=src[i].block;nb[di]=true;}
        }
        System.arraycopy(dst,0,s,0,size);
        floorModified=nf;blockModified=nb;
    }
    private void writeTile(GenerateInput in){
        int x=clampCoordinate(in.x,in.width),y=clampCoordinate(in.y,in.height),i=x+y*in.width;
        if(computed==null||(!floorModified[i]&&!blockModified[i]))return;
        TileState st=computed[i];
        if(floorModified[i]&&st.floor!=null){
            in.floor=st.floor;
            Block ov=in.overlay==null?Blocks.air:in.overlay;
            if(!st.floor.hasSurface()&&ov instanceof OreBlock&&ov instanceof Floor&&((Floor)ov).needsSurface)in.overlay=Blocks.air;
        }
        if(!blockModified[i])return;
        Block cb=in.block==null?Blocks.air:in.block;
        if(cb.synthetic())return;
        if(st.block==Blocks.air)in.block=Blocks.air;
        else if(st.block!=null&&!st.block.synthetic())in.block=st.block;
    }
    private void rebuildCenters(GenerateInput in){
        if(centers==null)centers=new IntSeq();
        centers.clear();
        if(centerMode==CenterMode.auto){
            rebuildAutomaticCenters(in);
            return;
        }
        Block m=replaceOverlay==null?Blocks.coreZone:replaceOverlay;
        if(m==null)return;
        for(int x=0;x<in.width;x++)for(int y=0;y<in.height;y++){
            Tile t=readTile(in,x,y);
            if(t!=null&&t.overlay()==m)centers.add(x+y*in.width);
            else if(t==null&&x==in.x&&y==in.y&&in.overlay==m)centers.add(x+y*in.width);
        }
    }
    private void rebuildAutomaticCenters(GenerateInput in){
        float d=clamp(autoDensity,0f,100f)/100f,spc=finiteAtLeast(minCenterSpacing,0f);
        boolean st=usesStructuredMode(in.width,in.height);
        StructuredRuinLayout.Spec sp=st?StructuredRuinLayout.spec(in.width,in.height,structurePreset):null;
        float esp=Math.max(spc,st?sp.span:0f);
        CenterIndex sel=new CenterIndex(in.width,in.height,Math.max(1,(int)Math.ceil(esp)));
        StructureIndex occ=st?new StructureIndex(in.width,in.height,sp.span):null;
        int sx=positiveHash(seed,0x13579bdf,0)%in.width,sy=positiveHash(seed,0x2468ace0,1)%in.height;
        boolean singleLarge=st&&structurePreset==StructurePreset.large;
        outer:for(int ox=0;ox<in.width;ox++){
            int x=(sx+ox)%in.width;
            for(int oy=0;oy<in.height;oy++){
                int y=(sy+oy)%in.height;
                TileState state=readState(in,x,y);
                if(!isAutoCenterCandidate(state))continue;
                if(d<=0f||(d<1f&&random01(seed,x,y)>d))continue;
                if(esp>0f&&sel.hasNearby(x,y,esp))continue;
                StructuredRuinLayout.Bounds b=null;
                if(st){
                    b=StructuredRuinLayout.bounds(in.width,in.height,x,y,structurePreset);
                    if(!b.contains(in.width,in.height)||occ.conflicts(b))continue;
                }
                centers.add(x+y*in.width);sel.add(x,y);
                if(st)occ.add(b);
                if(singleLarge)break outer;
            }
        }
    }
    private boolean isAutoCenterCandidate(TileState s){return s.floor!=null&&!s.floor.isLiquid&&s.block!=null&&!s.block.synthetic();}
    private boolean isAutoProcessable(TileState s){return isAutoCenterCandidate(s);}
    private void applyStep(TileState s,TileState bl,RuinStep st,int i){
        if(st.preservesFloor()){s.floor=bl.floor;floorModified[i]=false;}
        else if(st.hasFloor()&&st.floor instanceof Floor){s.floor=(Floor)st.floor;floorModified[i]=true;}
        if(st.removesWall()){s.block=Blocks.air;blockModified[i]=true;}
        else if(st.hasWall()&&st.wall!=null){s.block=st.wall;blockModified[i]=true;}
    }
    private boolean passesNoise(int x,int y){return passesNoise((float)x,(float)y);}
    private boolean passesNoise(float x,float y){
        float sc=finiteAtLeast(scl,1f),vl=finiteOr(threshold,0.5f),oc=finiteAtLeast(octaves,1f),pe=clamp(finiteOr(falloff,0.5f),0f,1f),sk=finiteOr(tilt,0f);
        return noise(x,y+x*sk,sc,1f,oc,pe)<=vl;
    }
    private float distortionNoise(int so,float x,float y,float sc,float mg){return Simplex.noise2d(seed+so,1f,0f,1f/sc,x+10f,y+10f)*mg;}
    private float maxStepRadius(){float m=0f;if(steps!=null)for(RuinStep s:steps)if(s!=null&&Float.isFinite(s.radius))m=Math.max(m,s.radius);return m;}
    private TileState readState(GenerateInput in,int x,int y){
        Tile t=readTile(in,x,y);
        if(t!=null)return TileState.from(t);
        if(x==in.x&&y==in.y)return TileState.from(in);
        return TileState.empty();
    }
    private static Tile readTile(GenerateInput in,int x,int y){
        try{
            if(tileMethod!=null)return(Tile)tileMethod.invoke(in,(float)x,(float)y);
        }catch(Exception ignored){}
        if(Vars.world!=null&&Vars.world.tiles!=null)return Vars.world.tile(x,y);
        return null;
    }
    private void ensureDefaults(){
        if(replaceOverlay==null)replaceOverlay=Blocks.coreZone;
        if(centerMode==null)centerMode=CenterMode.auto;
        if(generationMode==null)generationMode=GenerationMode.structured;
        if(structurePreset==null)structurePreset=StructurePreset.large;
        if(structureFloor==null)structureFloor=Blocks.metalFloor;
        if(structureMainFloor==null)structureMainFloor=Blocks.darkPanel1;
        if(structureWall==null)structureWall=Blocks.darkMetal;
        if(structureAccentWall==null)structureAccentWall=Blocks.duneWall;
        if(structureDamageFloor==null)structureDamageFloor=Blocks.metalFloorDamaged;
        if(steps==null)steps=RuinStep.defaults();
    }
    private boolean usesStructuredMode(int w,int h){return generationMode==GenerationMode.structured&&Math.min(w,h)>=24;}
    private void resetComputedMap(){mapComputed=false;tilesLeft=0;computed=null;floorModified=null;blockModified=null;centerIndex=null;computedInput=null;}
    private static TileState[] copyStates(TileState[] st){TileState[] c=new TileState[st.length];for(int i=0;i<st.length;i++)c[i]=st[i].copy();return c;}
    private static int sampleCoordinate(int c,float o,int s){return clampCoordinate((int)(c+o),s);}
    private static int clampCoordinate(int c,int s){return Math.max(0,Math.min(s-1,c));}
    private static float clamp(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
    private static float finiteOr(float v,float fb){return Float.isFinite(v)?v:fb;}
    private static float finiteAtLeast(float v,float mn){return Math.max(mn,finiteOr(v,mn));}
    private static int positiveHash(int sd,int slt,int ax){int v=sd^slt^ax*0x9e3779b9;v^=v>>>16;v*=0x7feb352d;v^=v>>>15;return v&0x7fffffff;}
    private static float random01(int sd,int x,int y){int v=sd;v^=x*0x9e3779b9;v=Integer.rotateLeft(v,13);v^=y*0x85ebca6b;v=Integer.rotateLeft(v,17);v*=0xc2b2ae35;v^=v>>>16;return(v&0x7fffffff)/2147483648f;}
    private static class TileState{
        Block block;Floor floor;Block overlay;
        static TileState from(Tile t){TileState s=new TileState();s.block=t.block();s.floor=t.floor();s.overlay=t.overlay();return s;}
        static TileState from(GenerateInput in){TileState s=new TileState();s.block=in.block==null?Blocks.air:in.block;s.floor=in.floor instanceof Floor?(Floor)in.floor:Blocks.stone.asFloor();s.overlay=in.overlay==null?Blocks.air:in.overlay;return s;}
        static TileState empty(){TileState s=new TileState();s.block=Blocks.air;s.floor=Blocks.stone.asFloor();s.overlay=Blocks.air;return s;}
        TileState copy(){TileState s=new TileState();s.block=block;s.floor=floor;s.overlay=overlay;return s;}
    }
    private static final class CenterIndex{
        final int w,cs,cw,ch;final IntSeq[] b;
        CenterIndex(int wid,int hgt,float r){w=wid;cs=Math.max(1,(int)Math.ceil(Math.max(0f,r)));cw=Math.max(1,(wid+cs-1)/cs);ch=Math.max(1,(hgt+cs-1)/cs);b=new IntSeq[cw*ch];}
        void add(int x,int y){int bi=Math.max(0,Math.min(cw-1,x/cs))+Math.max(0,Math.min(ch-1,y/cs))*cw;if(b[bi]==null)b[bi]=new IntSeq();b[bi].add(x+y*w);}
        boolean hasNearby(int x,int y,float r){
            if(r<=0f)return false;
            int mx=Math.max(0,(int)Math.floor((x-r)/cs)),xx=Math.min(cw-1,(int)Math.floor((x+r)/cs));
            int my=Math.max(0,(int)Math.floor((y-r)/cs)),xy=Math.min(ch-1,(int)Math.floor((y+r)/cs));
            float sq=r*r;
            for(int bx=mx;bx<=xx;bx++)for(int by=my;by<=xy;by++){
                IntSeq v=b[bx+by*cw];if(v==null)continue;
                for(int i=0;i<v.size;i++){float dx=x-v.get(i)%w,dy=y-v.get(i)/w;if(dx*dx+dy*dy<sq)return true;}
            }return false;
        }
        boolean hasMatch(int x,int y,RuinStep st){
            float r=st==null||!Float.isFinite(st.radius)?0f:Math.max(0f,st.radius);
            int mx=Math.max(0,(int)Math.floor((x-r)/cs)),xx=Math.min(cw-1,(int)Math.floor((x+r)/cs));
            int my=Math.max(0,(int)Math.floor((y-r)/cs)),xy=Math.min(ch-1,(int)Math.floor((y+r)/cs));
            for(int bx=mx;bx<=xx;bx++)for(int by=my;by<=xy;by++){
                IntSeq v=b[bx+by*cw];if(v==null)continue;
                for(int i=0;i<v.size;i++)if(st.matches(v.get(i)%w,v.get(i)/w,x,y))return true;
            }return false;
        }
    }
    private static final class StructureIndex{
        final int cs,cw,ch;final IntSeq[] b;final List<StructuredRuinLayout.Bounds> bd=new ArrayList<>();
        StructureIndex(int w,int h,int c){cs=Math.max(1,c);cw=Math.max(1,(w+cs-1)/cs);ch=Math.max(1,(h+cs-1)/cs);b=new IntSeq[cw*ch];}
        void add(StructuredRuinLayout.Bounds v){
            int i=bd.size();bd.add(v);
            for(int x=Math.max(0,v.minX/cs);x<=Math.min(cw-1,v.maxX/cs);x++)for(int y=Math.max(0,v.minY/cs);y<=Math.min(ch-1,v.maxY/cs);y++){
                if(b[x+y*cw]==null)b[x+y*cw]=new IntSeq();b[x+y*cw].add(i);
            }
        }
        boolean conflicts(StructuredRuinLayout.Bounds v){
            for(int x=Math.max(0,v.minX/cs);x<=Math.min(cw-1,v.maxX/cs);x++)for(int y=Math.max(0,v.minY/cs);y<=Math.min(ch-1,v.maxY/cs);y++){
                IntSeq bk=b[x+y*cw];if(bk==null)continue;
                for(int i=0;i<bk.size;i++)if(v.intersects(bd.get(bk.get(i))))return true;
            }return false;
        }
    }
    public enum CenterMode{overlay,auto;public CenterMode next(){return this==overlay?auto:overlay;}}
    public enum GenerationMode{scatter,structured;public GenerationMode next(){return this==scatter?structured:scatter;}}
    public enum StructurePreset{small,medium,large;public StructurePreset next(){StructurePreset[] v=values();return v[(ordinal()+1)%v.length];}}
    public static class CenterModeOption extends FilterOption{
        final AdvancedRuinFilter f;public CenterModeOption(AdvancedRuinFilter f){this.f=f;}
        @Override public void build(Table t){
            t.add("@filter.option.centermode").growX().left().style(Styles.outlineLabel);
            t.button(b->b.label(()->Core.bundle.get("filter.option.centermode."+f.centerMode.name(),f.centerMode.name())),Styles.flatt,()->{
                f.centerMode=f.centerMode.next();if(changed!=null)changed.run();
            }).size(110f,32f).right().row();
        }
    }
    public static class GenerationModeOption extends FilterOption{
        final AdvancedRuinFilter f;public GenerationModeOption(AdvancedRuinFilter f){this.f=f;}
        @Override public void build(Table t){
            t.add("@filter.option.generationmode").growX().left().style(Styles.outlineLabel);
            t.button(b->b.label(()->Core.bundle.get("filter.option.generationmode."+f.generationMode.name(),f.generationMode.name())),Styles.flatt,()->{
                f.generationMode=f.generationMode.next();if(changed!=null)changed.run();
            }).size(110f,32f).right().row();
        }
    }
    public static class StructurePresetOption extends FilterOption{
        final AdvancedRuinFilter f;public StructurePresetOption(AdvancedRuinFilter f){this.f=f;}
        @Override public void build(Table t){
            t.add("@filter.option.structurepreset").growX().left().style(Styles.outlineLabel);
            t.button(b->b.label(()->Core.bundle.get("filter.option.structurepreset."+f.structurePreset.name(),f.structurePreset.name())),Styles.flatt,()->{
                f.structurePreset=f.structurePreset.next();if(changed!=null)changed.run();
            }).size(110f,32f).right().row();
        }
    }
    public static class StepsEditOption extends FilterOption{
        final AdvancedRuinFilter f;public StepsEditOption(AdvancedRuinFilter f){this.f=f;}
        @Override public void build(Table t){
            t.button("@filter.option.ruinsteps",Styles.flatt,this::openDialog).colspan(2).growX().height(36f).padVertical(2f).row();
        }
        private void openDialog(){
            BaseDialog d=new BaseDialog("@filter.option.ruinsteps");
            Table list=new Table();
            Runnable[] rebuild=new Runnable[1];
            rebuild[0]=()->{
                list.clearChildren();
                if(f.steps==null)f.steps=RuinStep.defaults();
                for(int idx=0;idx<f.steps.length;idx++){
                    final int i=idx;final RuinStep st=f.steps[i];
                    Table row=new Table(Tex.button);row.margin(6f);
                    row.add("#"+(i+1)).width(30f);
                    row.button(b->b.image(st.floor==null?Blocks.air.uiIcon:st.floor.uiIcon).size(24f),()->pickBlock(floorsOptional,b->{st.floor=b;rebuild[0].run();if(changed!=null)changed.run();})).size(36f).pad(2f);
                    row.button(b->b.image(st.removeWall?Icon.cancel:new arc.scene.style.TextureRegionDrawable(st.wall==null?Blocks.air.uiIcon:st.wall.uiIcon)).size(24f),()->pickBlock(b->b==Blocks.air||wallsOptional.get(b),b->{if(b==Blocks.air){st.wall=null;st.removeWall=true;}else{st.wall=b;st.removeWall=false;}rebuild[0].run();if(changed!=null)changed.run();})).size(36f).pad(2f);
                    row.button(st.stepMode.name(),Styles.flatt,()->{st.stepMode=st.stepMode.next();rebuild[0].run();if(changed!=null)changed.run();}).size(90f,32f).pad(2f);
                    Slider sl=new Slider(0f,32f,0.5f,false);sl.setValue(st.radius);
                    Label rl=new Label(Strings.fixed(st.radius,1));
                    sl.moved(v->{st.radius=v;rl.setText(Strings.fixed(v,1));if(changed!=null)changed.run();});
                    row.add(sl).width(110f).pad(2f);row.add(rl).width(35f);
                    row.button(Icon.trash,Styles.flati,()->{
                        ArrayList<RuinStep> l=new ArrayList<>(Arrays.asList(f.steps));
                        l.remove(i);f.steps=l.toArray(new RuinStep[0]);
                        rebuild[0].run();if(changed!=null)changed.run();
                    }).size(32f);
                    list.add(row).growX().pad(2f).row();
                }
            };
            rebuild[0].run();
            d.cont.pane(list).width(520f).height(340f).row();
            d.cont.table(bt->{
                bt.button("@add",Icon.add,()->{
                    ArrayList<RuinStep> l=new ArrayList<>(Arrays.asList(f.steps));
                    l.add(RuinStep.floor(4f,Blocks.metalFloor));
                    f.steps=l.toArray(new RuinStep[0]);
                    rebuild[0].run();if(changed!=null)changed.run();
                }).size(140f,40f).pad(4f);
                bt.button("Reset",Icon.refresh,()->{
                    f.steps=RuinStep.defaults();
                    rebuild[0].run();if(changed!=null)changed.run();
                }).size(140f,40f).pad(4f);
            });
            d.addCloseButton();d.show();
        }
        private void pickBlock(Boolf<Block> filter,Cons<Block> cons){
            BaseDialog bd=new BaseDialog("");
            bd.cont.pane(t->{
                int c=0;
                for(Block b:Vars.content.blocks()){
                    if(!filter.get(b))continue;
                    t.button(btn->btn.image(b.uiIcon).size(28f),Styles.flati,()->{cons.get(b);bd.hide();}).size(38f).pad(2f);
                    if(++c%10==0)t.row();
                }
            }).size(440f,320f);
            bd.addCloseButton();bd.show();
        }
    }
    public static class RuinStep implements Serializable{
        public float radius;public Block floor,wall;public boolean removeWall;public StepMode stepMode=StepMode.chebyshev;
        public RuinStep(){}
        public RuinStep(float r,Block f,Block w){radius=r;floor=f;wall=w;}
        public RuinStep copy(){RuinStep s=new RuinStep(radius,floor,wall);s.removeWall=removeWall;s.stepMode=stepMode;return s;}
        public boolean hasFloor(){return floor!=null&&floor!=Blocks.air;}
        public boolean preservesFloor(){return floor!=null&&floor==Blocks.air;}
        public boolean hasWall(){return wall!=null&&wall!=Blocks.air&&!removeWall;}
        public boolean removesWall(){return removeWall||(wall!=null&&wall==Blocks.air);}
        public boolean matches(int cx,int cy,int x,int y){
            if(!Float.isFinite(radius)||radius<0f)return false;
            float dx=x-cx,dy=y-cy;
            StepMode m=stepMode==null?StepMode.chebyshev:stepMode;
            switch(m){case geometric:return Math.sqrt(dx*dx+dy*dy)<=radius;case manhattan:return Math.abs(dx)+Math.abs(dy)<=radius;default:return Math.max(Math.abs(dx),Math.abs(dy))<=radius;}
        }
        public static RuinStep floor(float r,Block f){return new RuinStep(r,f,null);}
        public static RuinStep wall(float r,Block w){return new RuinStep(r,null,w);}
        public static RuinStep removeWall(float r){RuinStep s=new RuinStep(r,Blocks.air,null);s.removeWall=true;return s;}
        public static RuinStep[] defaults(){return new RuinStep[]{floor(6f,Blocks.metalFloor),floor(1f,Blocks.metalFloor3),wall(4f,Blocks.darkMetal),removeWall(3f)};}
        public enum StepMode{geometric,manhattan,chebyshev;public StepMode next(){StepMode[] v=values();return v[(ordinal()+1)%v.length];}}
    }
    static final class StructuredRuinLayout{
        static Spec spec(int mapWidth,int mapHeight,StructurePreset preset){
            int shortSide=Math.min(mapWidth,mapHeight);
            StructurePreset safePreset=preset==null?StructurePreset.large:preset;
            float ratio,wallDamage,floorDamage;
            int minimumSpan,minimumRoomArea,maximumRoomArea,minimumRooms,maximumRooms,corridorWidth,mainCorridorWidth;
            switch(safePreset){
                case small:ratio=0.22f;minimumSpan=22;minimumRoomArea=32;maximumRoomArea=80;minimumRooms=4;maximumRooms=7;corridorWidth=2;mainCorridorWidth=4;wallDamage=0.20f;floorDamage=0.12f;break;
                case medium:ratio=0.34f;minimumSpan=34;minimumRoomArea=64;maximumRoomArea=180;minimumRooms=7;maximumRooms=12;corridorWidth=3;mainCorridorWidth=5;wallDamage=0.24f;floorDamage=0.16f;break;
                default:ratio=0.80f;minimumSpan=80;minimumRoomArea=96;maximumRoomArea=320;minimumRooms=12;maximumRooms=18;corridorWidth=3;mainCorridorWidth=7;wallDamage=0.28f;floorDamage=0.20f;break;
            }
            int targetSpan=Math.round(shortSide*ratio);
            int span=Math.max(8,Math.min(shortSide,Math.max(minimumSpan,targetSpan)));
            float roomScale=Math.max(1f,span/96f);
            int scaledMin=Math.max(16,Math.round(minimumRoomArea*roomScale));
            int scaledMax=Math.max(scaledMin,Math.round(maximumRoomArea*roomScale));
            return new Spec(span,scaledMin,scaledMax,minimumRooms,maximumRooms,corridorWidth,mainCorridorWidth,wallDamage,floorDamage);
        }
        static Bounds bounds(int mapWidth,int mapHeight,int centerX,int centerY,StructurePreset preset){
            Spec s=spec(mapWidth,mapHeight,preset);
            int ox=centerX-s.span/2,oy=centerY-s.span/2;
            return new Bounds(ox,oy,ox+s.span-1,oy+s.span-1);
        }
        static Layout generate(int mapWidth,int mapHeight,int centerX,int centerY,int seed,StructurePreset preset){
            Spec spec=spec(mapWidth,mapHeight,preset);
            StructurePreset lp=preset==null?StructurePreset.large:preset;
            int span=spec.span,ox=centerX-span/2,oy=centerY-span/2;
            Random random=new Random(mixSeed(seed,centerX,centerY,lp));
            boolean[] floor=new boolean[span*span],criticalFloor=new boolean[span*span],mainFloor=new boolean[span*span];
            List<Room> rooms=new ArrayList<>(),mainAnchors=new ArrayList<>();
            Room hall=createRoom(random,spec,span,span/2,span/2);
            rooms.add(hall);carveRoom(floor,criticalFloor,span,hall,true);
            int roomTarget=spec.minimumRooms+random.nextInt(spec.maximumRooms-spec.minimumRooms+1);
            int mainRoomTarget=Math.min(roomTarget-1,lp==StructurePreset.large?4:3);
            mainAnchors.add(hall);
            if(lp==StructurePreset.large){
                for(int side=0;side<4;side++){
                    Room p=createPerimeterRoom(random,spec,span,side,rooms);
                    if(p==null)continue;
                    rooms.add(p);carveRoom(floor,criticalFloor,span,p,true);
                    carveRoomConnection(floor,criticalFloor,mainFloor,span,hall,p,spec.mainCorridorWidth,random,true);
                    mainAnchors.add(p);
                }
            }
            for(int idx=rooms.size();idx<roomTarget;idx++){
                boolean mr=idx<=mainRoomTarget;
                Room parent=chooseParent(rooms,mainAnchors,random,mr);
                Room cand=null;
                for(int att=0;att<48&&cand==null;att++){
                    Room nx=createAdjacentRoom(random,spec,span,parent,random.nextInt(4));
                    if(nx!=null&&!overlaps(nx,rooms,1))cand=nx;
                }
                if(cand==null){
                    for(int att=0;att<160&&cand==null;att++){
                        int x=2+random.nextInt(Math.max(1,span-4)),y=2+random.nextInt(Math.max(1,span-4));
                        Room nx=createRoom(random,spec,span,x,y);
                        if(nx!=null&&!overlaps(nx,rooms,1))cand=nx;
                    }
                }
                if(cand==null)continue;
                rooms.add(cand);carveRoom(floor,criticalFloor,span,cand,false);
                int w=mr?Math.max(4,spec.mainCorridorWidth-(idx>1?1:0)):spec.corridorWidth;
                carveRoomConnection(floor,criticalFloor,mainFloor,span,parent,cand,w,random,mr);
                if(mr)mainAnchors.add(cand);
            }
            int extra=Math.max(1,rooms.size()/5);
            for(int c=0;c<extra;c++){
                Room f1=rooms.get(1+random.nextInt(Math.max(1,rooms.size()-1))),s2=rooms.get(random.nextInt(rooms.size()));
                if(f1!=s2)carveRoomConnection(floor,criticalFloor,mainFloor,span,f1,s2,spec.corridorWidth,random,false);
            }
            if(!roomsConnected(floor,span,rooms)){
                for(int i=1;i<rooms.size();i++)carveRoomConnection(floor,criticalFloor,mainFloor,span,rooms.get(i),hall,spec.corridorWidth,random,false);
            }
            collapseNonCriticalFloor(floor,criticalFloor,mainFloor,span,rooms,random,lp);
            boolean[] panelFloor=buildPanelFloors(floor,criticalFloor,mainFloor,span,rooms,random);
            boolean[] wall=buildWalls(floor,span);
            boolean[] mainWall=buildMainWalls(mainFloor,wall,span);
            boolean[] debrisWall=buildDebrisWalls(floor,wall,mainFloor,span,random);
            boolean[] criticalWall=buildCriticalWalls(criticalFloor,wall,span);
            return transform(new Layout(ox,oy,span,floor,wall,mainFloor,mainWall,panelFloor,debrisWall,criticalFloor,criticalWall),random);
        }
        static Room createRoom(Random r,Spec sp,int spn,int cx,int cy){int[] s=roomSize(r,sp,spn);return roomAt(r,spn,cx,cy,s[0],s[1]);}
        static int[] roomSize(Random r,Spec sp,int spn){
            int md=Math.max(4,Math.min(spn-4,Math.max(14,Math.min(48,Math.round(spn*0.18f))))),w=0,h=0;
            for(int a=0;a<40;a++){
                int cw=4+r.nextInt(Math.max(1,md-3)),ch=4+r.nextInt(Math.max(1,md-3)),ar=cw*ch;
                if(ar>=sp.minimumRoomArea&&ar<=sp.maximumRoomArea&&cw<=ch*2&&ch<=cw*2){w=cw;h=ch;break;}
            }
            if(w==0){int t=(sp.minimumRoomArea+sp.maximumRoomArea)/2;w=Math.max(4,Math.min(md,(int)Math.sqrt(t)));h=Math.max(4,Math.min(md,(t+w-1)/w));}
            return new int[]{w,h};
        }
        static Room roomAt(Random r,int spn,int cx,int cy,int w,int h){
            if(w<4||h<4||w>spn-2||h>spn-2)return null;
            int l=Math.max(1,Math.min(spn-w-1,cx-w/2)),t=Math.max(1,Math.min(spn-h-1,cy-h/2));
            return new Room(l,t,l+w-1,t+h-1,r.nextInt(7));
        }
        static Room createPerimeterRoom(Random r,Spec sp,int spn,int sd,List<Room> rm){
            for(int a=0;a<64;a++){
                int[] sz=roomSize(r,sp,spn);int w=sz[0],h=sz[1],cx,cy,jt=Math.max(3,spn/5),of=Math.round((r.nextFloat()*2f-1f)*jt);
                switch(sd){case 0:cx=spn/2+of;cy=1+h/2;break;case 1:cx=spn-2-w/2;cy=spn/2+of;break;case 2:cx=spn/2+of;cy=spn-2-h/2;break;default:cx=1+w/2;cy=spn/2+of;break;}
                Room c=roomAt(r,spn,cx,cy,w,h);
                if(c!=null&&!overlaps(c,rm,1))return c;
            }return null;
        }
        static Room createAdjacentRoom(Random r,Spec sp,int spn,Room pr,int sd){
            int[] sz=roomSize(r,sp,spn);int w=sz[0],h=sz[1],g=2+r.nextInt(3),l,t,of;
            switch(sd){
                case 0:t=pr.top-g-h;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.width()/3));l=pr.centerX()-w/2+of;break;
                case 1:l=pr.right+g;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.height()/3));t=pr.centerY()-h/2+of;break;
                case 2:t=pr.bottom+g;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.width()/3));l=pr.centerX()-w/2+of;break;
                default:l=pr.left-g-w;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.height()/3));t=pr.centerY()-h/2+of;break;
            }
            if(l<1||t<1||l+w>=spn-1||t+h>=spn-1)return null;
            return new Room(l,t,l+w-1,t+h-1,r.nextInt(7));
        }
        static Room chooseParent(List<Room> rm,List<Room> ma,Random r,boolean mr){
            if(mr){if(ma.size()>1&&r.nextFloat()<0.65f)return ma.get(ma.size()-1);return ma.get(r.nextInt(ma.size()));}
            return rm.get(r.nextInt(rm.size()));
        }
        static boolean overlaps(Room c,List<Room> rm,int pad){
            for(Room r:rm)if(!(c.right+pad<r.left||r.right+pad<c.left||c.bottom+pad<r.top||r.bottom+pad<c.top))return true;
            return false;
        }
        static void carveRoom(boolean[] f,boolean[] cf,int spn,Room r,boolean isC){
            for(int y=r.top;y<=r.bottom;y++)for(int x=r.left;x<=r.right;x++)if(r.contains(x,y)){int i=x+y*spn;f[i]=true;if(isC||r.isCore(x,y))cf[i]=true;}
        }
        static void carveRoomConnection(boolean[] f,boolean[] cf,boolean[] mf,int spn,Room f1,Room s2,int w,Random r,boolean mr){
            int sd;if(s2.bottom<f1.top)sd=0;else if(s2.left>f1.right)sd=1;else if(s2.top>f1.bottom)sd=2;else if(s2.right<f1.left)sd=3;else{int dx=s2.centerX()-f1.centerX(),dy=s2.centerY()-f1.centerY();sd=Math.abs(dx)>=Math.abs(dy)?dx>=0?1:3:dy>=0?2:0;}
            int[] st=doorway(f1,sd,r),en=doorway(s2,(sd+2)%4,r);
            int dx=en[0]-st[0],dy=en[1]-st[1];float sp=0.35f+r.nextFloat()*0.30f;
            if(Math.abs(dx)>=Math.abs(dy)){int mx=Math.round(st[0]+dx*sp);ch(f,cf,mf,spn,st[0],mx,st[1],w,mr);cvr(f,cf,mf,spn,st[1],en[1],mx,w,mr);ch(f,cf,mf,spn,mx,en[0],en[1],w,mr);}
            else{int my=Math.round(st[1]+dy*sp);cvr(f,cf,mf,spn,st[1],my,st[0],w,mr);ch(f,cf,mf,spn,st[0],en[0],my,w,mr);cvr(f,cf,mf,spn,my,en[1],en[0],w,mr);}
        }
        static int[] doorway(Room rm,int sd,Random r){
            boolean hz=sd==0||sd==2;int mn=hz?rm.left+1:rm.top+1,mx=hz?rm.right-1:rm.bottom-1,pr=mn+r.nextInt(Math.max(1,mx-mn+1));
            for(int of=0;of<=Math.max(rm.width(),rm.height());of++){
                int[] cd={pr-of,pr+of};
                for(int c:cd){if(c<mn||c>mx)continue;int x=hz?c:sd==1?rm.right:rm.left,y=hz?sd==2?rm.bottom:rm.top:c;if(rm.contains(x,y))return new int[]{x,y};}
            }return new int[]{hz?rm.centerX():sd==1?rm.right:rm.left,hz?sd==2?rm.bottom:rm.top:rm.centerY()};
        }
        static void ch(boolean[] f,boolean[] cf,boolean[] mf,int spn,int fx,int tx,int y,int w,boolean mr){int l=Math.min(fx,tx),rg=Math.max(fx,tx),h=w/2;for(int x=l;x<=rg;x++)for(int o=-h;o<w-h;o++)mk(f,cf,mf,spn,x,y+o,mr);}
        static void cvr(boolean[] f,boolean[] cf,boolean[] mf,int spn,int fy,int ty,int x,int w,boolean mr){int t=Math.min(fy,ty),b=Math.max(fy,ty),h=w/2;for(int y=t;y<=b;y++)for(int o=-h;o<w-h;o++)mk(f,cf,mf,spn,x+o,y,mr);}
        static void mk(boolean[] f,boolean[] cf,boolean[] mf,int spn,int x,int y,boolean mr){if(x<1||y<1||x>=spn-1||y>=spn-1)return;int i=x+y*spn;f[i]=true;cf[i]=true;if(mr)mf[i]=true;}
        static void collapseNonCriticalFloor(boolean[] f,boolean[] cf,boolean[] mf,int spn,List<Room> rm,Random r,StructurePreset p){
            float c=p==StructurePreset.small?0.10f:p==StructurePreset.medium?0.15f:0.20f;
            int at=spn*(p==StructurePreset.small?2:p==StructurePreset.medium?3:5);
            for(int a=0;a<at;a++){
                if(r.nextFloat()>c)continue;
                int x=2+r.nextInt(Math.max(1,spn-4)),y=2+r.nextInt(Math.max(1,spn-4)),w=1+r.nextInt(p==StructurePreset.small?2:3),h=1+r.nextInt(3);
                boolean tv=false,ok=true;
                for(int ly=y;ly<y+h&&ly<spn-1;ly++)for(int lx=x;lx<x+w&&lx<spn-1;lx++){
                    int i=lx+ly*spn;if(!f[i]||cf[i]||mf[i]||(lx>0&&mf[lx-1+ly*spn])||(lx+1<spn&&mf[lx+1+ly*spn])||(ly>0&&mf[lx+(ly-1)*spn])||(ly+1<spn&&mf[lx+(ly+1)*spn])){ok=false;break;}
                    if(lx<=1||ly<=1||lx>=spn-2||ly>=spn-2||!f[i-1]||!f[i+1]||!f[i-spn]||!f[i+spn])tv=true;
                }
                if(ok&&tv)for(int ly=y;ly<y+h&&ly<spn-1;ly++)for(int lx=x;lx<x+w&&lx<spn-1;lx++){f[lx+ly*spn]=false;mf[lx+ly*spn]=false;}
            }
            int st=rm.get(0).centerX()+rm.get(0).centerY()*spn;if(!f[st])for(int i=0;i<f.length;i++)if(f[i]&&cf[i]){st=i;break;}
            boolean[] v=new boolean[f.length];int[] q=new int[f.length];int hd=0,tl=0;
            if(f[st]){q[tl++]=st;v[st]=true;}
            while(hd<tl){
                int cr=q[hd++],x=cr%spn,y=cr/spn;
                if(x>0&&f[cr-1]&&!v[cr-1]){v[cr-1]=true;q[tl++]=cr-1;}
                if(x+1<spn&&f[cr+1]&&!v[cr+1]){v[cr+1]=true;q[tl++]=cr+1;}
                if(y>0&&f[cr-spn]&&!v[cr-spn]){v[cr-spn]=true;q[tl++]=cr-spn;}
                if(y+1<spn&&f[cr+spn]&&!v[cr+spn]){v[cr+spn]=true;q[tl++]=cr+spn;}
            }
            for(int i=0;i<f.length;i++)if(f[i]&&!v[i]){f[i]=false;cf[i]=false;mf[i]=false;}
        }
        static boolean roomsConnected(boolean[] f,int spn,List<Room> rm){
            if(rm.isEmpty())return false;boolean[] v=new boolean[f.length];int[] q=new int[f.length];int hd=0,tl=0,st=rm.get(0).centerX()+rm.get(0).centerY()*spn;
            if(!f[st])return false;q[tl++]=st;v[st]=true;
            while(hd<tl){
                int cr=q[hd++],x=cr%spn,y=cr/spn;
                if(x>0&&f[cr-1]&&!v[cr-1]){v[cr-1]=true;q[tl++]=cr-1;}
                if(x+1<spn&&f[cr+1]&&!v[cr+1]){v[cr+1]=true;q[tl++]=cr+1;}
                if(y>0&&f[cr-spn]&&!v[cr-spn]){v[cr-spn]=true;q[tl++]=cr-spn;}
                if(y+1<spn&&f[cr+spn]&&!v[cr+spn]){v[cr+spn]=true;q[tl++]=cr+spn;}
            }
            for(Room r:rm)if(!v[r.centerX()+r.centerY()*spn])return false;return true;
        }
        static boolean[] buildPanelFloors(boolean[] f,boolean[] cf,boolean[] mf,int spn,List<Room> rm,Random r){
            boolean[] p=new boolean[f.length];
            for(int i=1;i<rm.size();i++){
                Room ro=rm.get(i);int pc=1+r.nextInt(2);
                for(int a=0;a<pc;a++){
                    int pw=Math.max(2,Math.min(ro.width(),3+r.nextInt(5))),ph=Math.max(2,Math.min(ro.height(),2+r.nextInt(4)));
                    int l=ro.left+r.nextInt(Math.max(1,ro.width()-pw+1)),t=ro.top+r.nextInt(Math.max(1,ro.height()-ph+1));
                    for(int y=t;y<t+ph;y++)for(int x=l;x<l+pw;x++){int c=x+y*spn;if(f[c]&&!mf[c]&&(!cf[c]||r.nextBoolean()))p[c]=true;}
                }
            }return p;
        }
        static boolean[] buildWalls(boolean[] f,int spn){boolean[] w=new boolean[f.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(!f[i])w[i]=f[i-1]||f[i+1]||f[i-spn]||f[i+spn];}return w;}
        static boolean[] buildMainWalls(boolean[] mf,boolean[] w,int spn){boolean[] mw=new boolean[w.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(w[i])mw[i]=mf[i-1]||mf[i+1]||mf[i-spn]||mf[i+spn];}return mw;}
        static boolean[] buildCriticalWalls(boolean[] cf,boolean[] w,int spn){boolean[] cw=new boolean[w.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(w[i])cw[i]=cf[i-1]||cf[i+1]||cf[i-spn]||cf[i+spn];}return cw;}
        static boolean[] buildDebrisWalls(boolean[] f,boolean[] w,boolean[] mf,int spn,Random r){
            boolean[] d=new boolean[f.length];
            for(int y=2;y<spn-2;y++)for(int x=2;x<spn-2;x++){
                int i=x+y*spn;if(f[i]||w[i]||mf[i-1]||mf[i+1]||mf[i-spn]||mf[i+spn])continue;
                if((w[i-1]||w[i+1]||w[i-spn]||w[i+spn])&&r.nextFloat()<0.24f)d[i]=true;
            }return d;
        }
        static Layout transform(Layout s,Random r){
            int spn=s.span,rot=r.nextInt(4);boolean mir=r.nextBoolean();
            boolean[] tf=new boolean[spn*spn],twl=new boolean[spn*spn],tmf=new boolean[spn*spn],tmw=new boolean[spn*spn],tpf=new boolean[spn*spn],tdw=new boolean[spn*spn],tcf=new boolean[spn*spn],tcw=new boolean[spn*spn];
            for(int y=0;y<spn;y++)for(int x=0;x<spn;x++){
                int tx=x,ty=y;if(mir)tx=spn-1-tx;
                for(int t=0;t<rot;t++){int nx=spn-1-ty;ty=tx;tx=nx;}
                int si=x+y*spn,ti=tx+ty*spn;
                tf[ti]=s.floor[si];twl[ti]=s.wall[si];tmf[ti]=s.mainFloor[si];tmw[ti]=s.mainWall[si];tpf[ti]=s.panelFloor[si];tdw[ti]=s.debrisWall[si];tcf[ti]=s.criticalFloor[si];tcw[ti]=s.criticalWall[si];
            }
            return new Layout(s.originX,s.originY,spn,tf,twl,tmf,tmw,tpf,tdw,tcf,tcw);
        }
        static long mixSeed(int sd,int cx,int cy,StructurePreset p){
            long v=sd*0x9e3779b97f4a7c15L;v^=cx*0xc2b2ae3d27d4eb4fL;v^=cy*0x165667b19e3779f9L;v^=(p==null?2:p.ordinal()+1)*0x94d049bb133111ebL;
            v^=v>>>30;v*=0xbf58476d1ce4e5b9L;v^=v>>>27;v*=0x94d049bb133111ebL;return v^(v>>>31);
        }
        static final class Spec{
            final int span,minimumRoomArea,maximumRoomArea,minimumRooms,maximumRooms,corridorWidth,mainCorridorWidth;final float wallDamage,floorDamage;
            Spec(int s,int mnA,int mxA,int mnR,int mxR,int cw,int mw,float wd,float fd){span=s;minimumRoomArea=mnA;maximumRoomArea=mxA;minimumRooms=mnR;maximumRooms=mxR;corridorWidth=cw;mainCorridorWidth=mw;wallDamage=wd;floorDamage=fd;}
        }
        static final class Bounds{
            final int minX,minY,maxX,maxY;Bounds(int mnX,int mnY,int mxX,int mxY){minX=mnX;minY=mnY;maxX=mxX;maxY=mxY;}
            boolean contains(int w,int h){return minX>=0&&minY>=0&&maxX<w&&maxY<h;}
            boolean intersects(Bounds o){return minX<=o.maxX&&maxX>=o.minX&&minY<=o.maxY&&maxY>=o.minY;}
        }
        static final class Layout{
            final int originX,originY,span;final boolean[] floor,wall,mainFloor,mainWall,panelFloor,debrisWall,criticalFloor,criticalWall;
            Layout(int ox,int oy,int s,boolean[] f,boolean[] w,boolean[] mf,boolean[] mw,boolean[] pf,boolean[] dw,boolean[] cf,boolean[] cw){originX=ox;originY=oy;span=s;floor=f;wall=w;mainFloor=mf;mainWall=mw;panelFloor=pf;debrisWall=dw;criticalFloor=cf;criticalWall=cw;}
        }
        static final class Room{
            final int left,top,right,bottom,shape;Room(int l,int t,int r,int b,int s){left=l;top=t;right=r;bottom=b;shape=s;}
            int centerX(){return(left+right)/2;}int centerY(){return(top+bottom)/2;}int width(){return right-left+1;}int height(){return bottom-top+1;}
            boolean isCore(int x,int y){return Math.abs(x-centerX())<=1&&Math.abs(y-centerY())<=1;}
            boolean contains(int x,int y){
                int lx=x-left,ly=y-top,wid=width(),hgt=height();if(shape==0||wid<6||hgt<6)return true;
                boolean tl=lx<2&&ly<2,tr=lx>=wid-2&&ly<2,bl=lx<2&&ly>=hgt-2,br=lx>=wid-2&&ly>=hgt-2;
                if(shape==1)return!(tl||br);if(shape==2)return!(tr||bl);if(shape==3)return!(tl||tr);if(shape==4)return!(bl||br);
                if(shape==5)return!(lx==0&&ly>=hgt/3&&ly<=hgt*2/3)&&!(lx==wid-1&&ly>=hgt/4&&ly<=hgt*3/4);
                return!(ly==0&&lx>=wid/3&&lx<=wid*2/3)&&!(ly==hgt-1&&lx>=wid/4&&lx<=wid*3/4);
            }
        }
    }
        }
