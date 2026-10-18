package advancedredactor.generation;
import arc.struct.IntSeq;
import arc.util.noise.Simplex;
import arc.Core;
import mindustry.content.Blocks;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;
import advancedredactor.ui.ModFilterOptions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static mindustry.maps.filters.FilterOption.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedRuinFilter extends GenerateFilter {
    public Block replaceOverlay=Blocks.coreZone;
    public float centerModeVal=1f,generationModeVal=1f,structurePresetVal=2f;
    public Block structureFloor=Blocks.metalFloor,structureMainFloor=Blocks.darkPanel1;
    public Block structureWall=Blocks.darkMetal,structureAccentWall=Blocks.darkMetal,structureDamageFloor=Blocks.metalFloorDamaged;
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
    @Override public String name(){return Core.bundle.get("filter.advancedruin.name","Advanced Ruins");}
    @Override public char icon(){return Iconc.blockScrapWall;}
    @Override public FilterOption[] options(){
        ensureDefaults();
        ArrayList<FilterOption> o=new ArrayList<>();
        o.add(new BlockOption("replace",()->replaceOverlay,b->replaceOverlay=b,oresFloorsOptional));
        o.add(new SliderOption("centermode",()->centerModeVal,v->centerModeVal=v,0f,1f,1f));
        o.add(new SliderOption("autodensity",()->autoDensity,v->autoDensity=v,0f,100f,1f));
        o.add(new SliderOption("mincenterspacing",()->minCenterSpacing,v->minCenterSpacing=v,0f,256f,1f));
        o.add(new SliderOption("generationmode",()->generationModeVal,v->generationModeVal=v,0f,1f,1f));
        o.add(new SliderOption("structurepreset",()->structurePresetVal,v->structurePresetVal=v,0f,2f,1f));
        o.add(new BlockOption("structurefloor",()->structureFloor,b->structureFloor=b,floorsOptional));
        o.add(new BlockOption("structuremainfloor",()->structureMainFloor,b->structureMainFloor=b,floorsOptional));
        o.add(new BlockOption("structurewall",()->structureWall,b->structureWall=b,b->b==Blocks.air||wallsOptional.get(b)&&b.isStatic()));
        o.add(new BlockOption("structureaccentwall",()->structureAccentWall,b->structureAccentWall=b,b->b==Blocks.air||wallsOptional.get(b)&&b.isStatic()));
        o.add(new BlockOption("structuredamagefloor",()->structureDamageFloor,b->structureDamageFloor=b,floorsOptional));
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
        if(in==null||in.width<=0||in.height<=0) return;
        int size=in.width*in.height;
        if(mapComputed&&computedInput!=in) resetComputedMap();
        if(mapComputed&&in.x==0&&in.y==0&&tilesLeft<size) resetComputedMap();
        if(!mapComputed||computedWidth!=in.width||computedHeight!=in.height){
            ensureDefaults();
            computedWidth=in.width;computedHeight=in.height;computedInput=in;
            rebuildCenters(in);computeMap(in);
            mapComputed=true;tilesLeft=size;
        }
        writeTile(in);
        if(--tilesLeft<=0) resetComputedMap();
    }
    private void computeMap(GenerateInput in){
        int w=in.width,h=in.height,size=w*h;
        computed=new TileState[size];floorModified=new boolean[size];blockModified=new boolean[size];
        for(int i=0;i<size;i++) computed[i]=readState(in,i%w,i/w);
        TileState[] bl=copyStates(computed);
        boolean struct=generationModeVal>=0.5f&&Math.min(w,h)>=24;
        if(struct) computeStructured(bl,w,h);
        else if(steps!=null&&steps.length>0&&centers!=null&&centers.size>0){
            centerIndex=new CenterIndex(w,h,maxStepRadius());
            for(int i=0;i<centers.size;i++) centerIndex.add(centers.get(i)%w,centers.get(i)/w);
            for(RuinStep step:steps){
                if(step==null) continue;
                for(int x=0;x<w;x++) for(int y=0;y<h;y++){
                    if(!passesNoise(x,y)) continue;
                    int i=x+y*w;
                    if(centerIndex==null||!centerIndex.hasMatch(x,y,step)) continue;
                    if(centerModeVal>=0.5f&&!isAutoCenterCandidate(bl[i])) continue;
                    applyStep(computed[i],bl[i],step,i);
                }
            }
        }
        if(!struct) applyDistort(computed,bl,w,h);
    }
    private void computeStructured(TileState[] bl,int w,int h){
        int size=w*h;
        int preset=(int)Math.max(0,Math.min(2,structurePresetVal));
        StructuredRuinLayout.Spec sp=StructuredRuinLayout.spec(w,h,preset);
        Block[] gf=new Block[size],gw=new Block[size];
        boolean[] gfb=new boolean[size],gwb=new boolean[size],cf=new boolean[size],cw=new boolean[size];
        Block sw=structureWall==Blocks.air||structureWall.isStatic()?structureWall:Blocks.darkMetal;
        Block aw=structureAccentWall==Blocks.air||structureAccentWall.isStatic()?structureAccentWall:Blocks.darkMetal;
        Block dw=structureWall==Blocks.scrapWall?structureWall:structureAccentWall==Blocks.scrapWall?structureAccentWall:Blocks.scrapWall;
        for(int ci=0;ci<centers.size;ci++){
            int cX=centers.get(ci)%w,cY=centers.get(ci)/w;
            StructuredRuinLayout.Layout l=StructuredRuinLayout.generate(w,h,cX,cY,seed,preset);
            for(int ly=0;ly<l.span;ly++) for(int lx=0;lx<l.span;lx++){
                int loc=lx+ly*l.span,x=l.originX+lx,y=l.originY+ly;
                if(x<0||y<0||x>=w||y>=h) continue;
                int i=x+y*w;
                if(centerModeVal>=0.5f&&!isAutoCenterCandidate(bl[i])) continue;
                if(l.floor[loc]){
                    boolean p=l.criticalFloor[loc];
                    Block f=l.mainFloor[loc]?structureMainFloor:l.panelFloor[loc]?structurePanelFloor(x,y,cX,cY):structureFloor;
                    if(!(f instanceof Floor)) f=structureFloor;
                    if(!p&&structuredDamage(x,y,cX,cY,sp.fd,0x31)) f=structureDamageFloorVariant(x,y,cX,cY);
                    if(f instanceof Floor){
                        if(!gfb[i]||p||!cf[i]){gf[i]=f;cf[i]=p;}
                        gfb[i]=true;gwb[i]=false;gw[i]=null;
                    }
                }else if((l.wall[loc]||l.debrisWall[loc])&&!gfb[i]){
                    boolean p=l.criticalWall[loc];
                    Block wl=l.mainWall[loc]?sw:l.debrisWall[loc]?dw:aw;
                    if(!p&&structuredDamage(x,y,cX,cY,sp.wd,0x57)) wl=Blocks.air;
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
        if(c<=0f) return false;
        float s=Math.max(distortScl,1f),m=Math.max(distortMag,0f);
        return passesNoise(x+distortionNoise(salt,x,y,s,m/2f),y+distortionNoise(salt+1,x,y,s,m/2f))&&random01(seed^salt^cX*0x632be59b^cY*0x85157af5,x,y)<c;
    }
    private Block structurePanelFloor(int x,int y,int cX,int cY){
        int v=seed^x*0x9e3779b9^y*0x85ebca6b^cX*0xc2b2ae35^cY*0x27d4eb2d;
        v^=v>>>16;
        switch(v&3){case 0:return Blocks.darkPanel1;case 1:return Blocks.darkPanel2;case 2:return Blocks.darkPanel3;default:return Blocks.darkPanel1;}
    }
    private Block structureDamageFloorVariant(int x,int y,int cX,int cY){
        int v=seed^x*0x632be59b^y*0x85157af5^cX*0xc2b2ae35^cY*0x27d4eb2d;
        v^=v>>>16;
        switch(v&15){
            case 5:return Blocks.metalFloorDamaged;case 6:return Blocks.metalFloor2;case 7:return Blocks.metalFloor3;case 8:return Blocks.metalFloor4;
            case 9:return Blocks.metalFloor5;case 10:return Blocks.darkPanel1;case 11:return Blocks.darkPanel4;default:return structureDamageFloor;
        }
    }
    private void applyDistort(TileState[] s,TileState[] bl,int w,int h){
        float sc=Math.max(distortScl,1f),m=Math.max(distortMag,0f);
        if(m<=0f) return;
        TileState[] src=copyStates(s),dst=copyStates(bl);
        boolean[] nf=new boolean[w*h],nb=new boolean[w*h];
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            int i=x+y*w;
            if(!floorModified[i]&&!blockModified[i]) continue;
            int ox=Math.max(0,Math.min(w-1,(int)(x+distortionNoise(0,x,y,sc,m/2f))));
            int oy=Math.max(0,Math.min(h-1,(int)(y+distortionNoise(1,x,y,sc,m/2f))));
            int di=ox+oy*w;
            if(floorModified[i]){dst[di].floor=src[i].floor;nf[di]=true;}
            if(blockModified[i]){dst[di].block=src[i].block;nb[di]=true;}
        }
        System.arraycopy(dst,0,s,0,w*h);
        floorModified=nf;blockModified=nb;
    }
    private void writeTile(GenerateInput in){
        int x=Math.max(0,Math.min(in.width-1,in.x)),y=Math.max(0,Math.min(in.height-1,in.y)),i=x+y*in.width;
        if(computed==null||(!floorModified[i]&&!blockModified[i])) return;
        TileState st=computed[i];
        if(floorModified[i]&&st.floor!=null){
            in.floor=st.floor;
            Block ov=in.overlay==null?Blocks.air:in.overlay;
            if(!st.floor.hasSurface()&&ov instanceof OreBlock&&ov instanceof Floor&&((Floor)ov).needsSurface) in.overlay=Blocks.air;
        }
        if(!blockModified[i]) return;
        Block cb=in.block==null?Blocks.air:in.block;
        if(cb.synthetic()) return;
        if(st.block==Blocks.air) in.block=Blocks.air;
        else if(st.block!=null&&!st.block.synthetic()) in.block=st.block;
    }
    private void rebuildCenters(GenerateInput in){
        if(centers==null) centers=new IntSeq();
        centers.clear();
        if(centerModeVal>=0.5f){
            float d=Math.max(0f,Math.min(autoDensity,100f))/100f,spc=Math.max(minCenterSpacing,0f);
            boolean st=generationModeVal>=0.5f&&Math.min(in.width,in.height)>=24;
            int pr=(int)Math.max(0,Math.min(2,structurePresetVal));
            StructuredRuinLayout.Spec sp=st?StructuredRuinLayout.spec(in.width,in.height,pr):null;
            float esp=Math.max(spc,st?sp.sp:0f);
            CenterIndex sel=new CenterIndex(in.width,in.height,(int)Math.max(1,Math.ceil(esp)));
            StructureIndex occ=st?new StructureIndex(in.width,in.height,sp.sp):null;
            int sx=(seed^0x13579bdf)&0x7fffffff%in.width,sy=(seed^0x2468ace0)&0x7fffffff%in.height;
            out:for(int ox=0;ox<in.width;ox++) for(int oy=0;oy<in.height;oy++){
                int x=(sx+ox)%in.width,y=(sy+oy)%in.height;
                if(!isAutoCenterCandidate(readState(in,x,y))) continue;
                if(d<=0f||(d<1f&&random01(seed,x,y)>d)) continue;
                if(esp>0f&&sel.hasNearby(x,y,esp)) continue;
                StructuredRuinLayout.Bounds b=null;
                if(st){
                    b=StructuredRuinLayout.bounds(in.width,in.height,x,y,pr);
                    if(!b.contains(in.width,in.height)||occ.conflicts(b)) continue;
                }
                centers.add(x+y*in.width);sel.add(x,y);
                if(st) occ.add(b);
                if(st&&pr==2) break out;
            }
            return;
        }
        Block m=replaceOverlay==null?Blocks.coreZone:replaceOverlay;
        if(m==null) return;
        for(int x=0;x<in.width;x++) for(int y=0;y<in.height;y++){
            if(in.x==x&&in.y==y&&in.overlay==m) centers.add(x+y*in.width);
        }
    }
    private boolean isAutoCenterCandidate(TileState s){return s.floor!=null&&!s.floor.isLiquid&&s.block!=null&&!s.block.synthetic();}
    private void applyStep(TileState s,TileState bl,RuinStep st,int i){
        if(st.floor!=null&&st.floor==Blocks.air){s.floor=bl.floor;floorModified[i]=false;}
        else if(st.floor!=null&&st.floor!=Blocks.air&&st.floor instanceof Floor){s.floor=(Floor)st.floor;floorModified[i]=true;}
        if(st.removeWall||(st.wall!=null&&st.wall==Blocks.air)){s.block=Blocks.air;blockModified[i]=true;}
        else if(st.wall!=null&&st.wall!=Blocks.air){s.block=st.wall;blockModified[i]=true;}
    }
    private boolean passesNoise(float x,float y){
        float sc=Math.max(scl,1f),vl=Float.isFinite(threshold)?threshold:0.5f;
        float oc=Math.max(octaves,1f),pe=Math.max(0f,Math.min(1f,Float.isFinite(falloff)?falloff:0.5f));
        float sk=Float.isFinite(tilt)?tilt:0f;
        return Simplex.noise2d(seed,oc,pe,1f/sc,x+10f,y+x*sk+10f)<=vl;
    }
    private float distortionNoise(int so,float x,float y,float sc,float mg){return Simplex.noise2d(seed+so,1f,0f,1f/sc,x+10f,y+10f)*mg;}
    private float maxStepRadius(){float m=0f;if(steps!=null)for(RuinStep s:steps)if(s!=null&&Float.isFinite(s.radius))m=Math.max(m,s.radius);return m;}
    private TileState readState(GenerateInput in,int x,int y){
        if(x==in.x&&y==in.y){
            TileState s=new TileState();s.block=in.block==null?Blocks.air:in.block;
            s.floor=in.floor instanceof Floor?(Floor)in.floor:(Blocks.air instanceof Floor?(Floor)Blocks.air:null);
            s.overlay=in.overlay==null?Blocks.air:in.overlay;return s;
        }
        TileState s=new TileState();s.block=Blocks.air;s.floor=Blocks.air instanceof Floor?(Floor)Blocks.air:null;s.overlay=Blocks.air;return s;
    }
    private void ensureDefaults(){
        if(replaceOverlay==null) replaceOverlay=Blocks.air;
        if(structureFloor==null) structureFloor=Blocks.metalFloor;
        if(structureMainFloor==null) structureMainFloor=Blocks.darkPanel1;
        if(structureWall==null) structureWall=Blocks.darkMetal;
        if(structureAccentWall==null) structureAccentWall=Blocks.darkMetal;
        if(structureDamageFloor==null) structureDamageFloor=Blocks.metalFloorDamaged;
        if(steps==null) steps=RuinStep.defaults();
    }
    private void resetComputedMap(){mapComputed=false;tilesLeft=0;computed=null;floorModified=null;blockModified=null;centerIndex=null;computedInput=null;}
    private static TileState[] copyStates(TileState[] st){TileState[] c=new TileState[st.length];for(int i=0;i<st.length;i++){c[i]=new TileState();c[i].block=st[i].block;c[i].floor=st[i].floor;c[i].overlay=st[i].overlay;}return c;}
    private static float random01(int sd,int x,int y){int v=sd^x*0x9e3779b9;v=(v<<13)|(v>>>19);v^=y*0x85ebca6b;v=(v<<17)|(v>>>15);v*=0xc2b2ae35;v^=v>>>16;return(v&0x7fffffff)/2147483648f;}
    private static class TileState{Block block;Floor floor;Block overlay;}
    private static final class CenterIndex{
        final int w,cs,cw,ch;final IntSeq[] b;
        CenterIndex(int wid,int hgt,float r){w=wid;cs=Math.max(1,(int)Math.ceil(Math.max(0f,r)));cw=Math.max(1,(wid+cs-1)/cs);ch=Math.max(1,(hgt+cs-1)/cs);b=new IntSeq[cw*ch];}
        void add(int x,int y){int bi=Math.max(0,Math.min(cw-1,x/cs))+Math.max(0,Math.min(ch-1,y/cs))*cw;if(b[bi]==null)b[bi]=new IntSeq();b[bi].add(x+y*w);}
        boolean hasNearby(int x,int y,float r){
            if(r<=0f) return false;
            int mx=Math.max(0,(int)Math.floor((x-r)/cs)),xx=Math.min(cw-1,(int)Math.floor((x+r)/cs));
            int my=Math.max(0,(int)Math.floor((y-r)/cs)),xy=Math.min(ch-1,(int)Math.floor((y+r)/cs));
            float sq=r*r;
            for(int bx=mx;bx<=xx;bx++) for(int by=my;by<=xy;by++){
                IntSeq v=b[bx+by*cw];if(v==null) continue;
                for(int i=0;i<v.size;i++){float dx=x-v.get(i)%w,dy=y-v.get(i)/w;if(dx*dx+dy*dy<sq) return true;}
            }return false;
        }
        boolean hasMatch(int x,int y,RuinStep st){
            float r=st==null||!Float.isFinite(st.radius)?0f:Math.max(0f,st.radius);
            int mx=Math.max(0,(int)Math.floor((x-r)/cs)),xx=Math.min(cw-1,(int)Math.floor((x+r)/cs));
            int my=Math.max(0,(int)Math.floor((y-r)/cs)),xy=Math.min(ch-1,(int)Math.floor((y+r)/cs));
            for(int bx=mx;bx<=xx;bx++) for(int by=my;by<=xy;by++){
                IntSeq v=b[bx+by*cw];if(v==null) continue;
                for(int i=0;i<v.size;i++) if(st.matches(v.get(i)%w,v.get(i)/w,x,y)) return true;
            }return false;
        }
    }
    private static final class StructureIndex{
        final int cs,cw,ch;final IntSeq[] b;final List<StructuredRuinLayout.Bounds> bd=new ArrayList<>();
        StructureIndex(int w,int h,int c){cs=Math.max(1,c);cw=Math.max(1,(w+cs-1)/cs);ch=Math.max(1,(h+cs-1)/cs);b=new IntSeq[cw*ch];}
        void add(StructuredRuinLayout.Bounds v){
            int i=bd.size();bd.add(v);
            for(int x=Math.max(0,v.minX/cs);x<=Math.min(cw-1,v.maxX/cs);x++) for(int y=Math.max(0,v.minY/cs);y<=Math.min(ch-1,v.maxY/cs);y++){
                if(b[x+y*cw]==null)b[x+y*cw]=new IntSeq();b[x+y*cw].add(i);
            }
        }
        boolean conflicts(StructuredRuinLayout.Bounds v){
            for(int x=Math.max(0,v.minX/cs);x<=Math.min(cw-1,v.maxX/cs);x++) for(int y=Math.max(0,v.minY/cs);y<=Math.min(ch-1,v.maxY/cs);y++){
                IntSeq bk=b[x+y*cw];if(bk==null) continue;
                for(int i=0;i<bk.size;i++) if(v.intersects(bd.get(bk.get(i)))) return true;
            }return false;
        }
    }
    public static class RuinStep{
        public float radius;public Block floor,wall;public boolean removeWall;public int stepMode=2;
        public RuinStep(){}
        public RuinStep(float r,Block f,Block w){radius=r;floor=f;wall=w;}
        public boolean matches(int cx,int cy,int x,int y){
            if(!Float.isFinite(radius)||radius<0f) return false;
            float dx=x-cx,dy=y-cy;
            switch(stepMode){case 0:return Math.sqrt(dx*dx+dy*dy)<=radius;case 1:return Math.abs(dx)+Math.abs(dy)<=radius;default:return Math.max(Math.abs(dx),Math.abs(dy))<=radius;}
        }
        public boolean preservesFloor(){return floor!=null&&floor==Blocks.air;}
        public boolean hasFloor(){return floor!=null&&floor!=Blocks.air;}
        public boolean removesWall(){return removeWall||wall!=null&&wall==Blocks.air;}
        public boolean hasWall(){return wall!=null&&wall!=Blocks.air;}
        public static RuinStep floor(float r,Block f){return new RuinStep(r,f,null);}
        public static RuinStep wall(float r,Block w){return new RuinStep(r,null,w);}
        public static RuinStep removeWall(float r){RuinStep s=new RuinStep(r,Blocks.air,null);s.removeWall=true;return s;}
        public static RuinStep[] defaults(){return new RuinStep[]{floor(6f,Blocks.metalFloor),floor(1f,Blocks.metalFloor3),wall(4f,Blocks.darkMetal),removeWall(3f)};}
    }
    static final class StructuredRuinLayout{
        static Spec spec(int w,int h,int p){
            int ss=Math.min(w,h);float rt;int ms,mra,xra,mr,xr,cw,mw;float wd,fd;
            switch(p){
                case 0:rt=0.22f;ms=22;mra=32;xra=80;mr=4;xr=7;cw=2;mw=4;wd=0.20f;fd=0.12f;break;
                case 1:rt=0.34f;ms=34;mra=64;xra=180;mr=7;xr=12;cw=3;mw=5;wd=0.24f;fd=0.16f;break;
                default:rt=0.80f;ms=80;mra=96;xra=320;mr=12;xr=18;cw=3;mw=7;wd=0.28f;fd=0.20f;break;
            }
            int sp=Math.max(8,Math.min(ss,Math.max(ms,Math.round(ss*rt))));
            float rsc=Math.max(1f,sp/96f);
            int sma=Math.max(16,Math.round(mra*rsc)),sxa=Math.max(sma,Math.round(xra*rsc));
            return new Spec(sp,sma,sxa,mr,xr,cw,mw,wd,fd);
        }
        static Bounds bounds(int w,int h,int cx,int cy,int p){Spec s=spec(w,h,p);int ox=cx-s.sp/2,oy=cy-s.sp/2;return new Bounds(ox,oy,ox+s.sp-1,oy+s.sp-1);}
        static Layout generate(int w,int h,int cx,int cy,int sd,int p){
            Spec sp=spec(w,h,p);int spn=sp.sp,ox=cx-spn/2,oy=cy-spn/2;
            long mx=sd*0x9e3779b97f4a7c15L;mx^=cx*0xc2b2ae3d27d4eb4fL;mx^=cy*0x165667b19e3779f9L;mx^=(p+1)*0x94d049bb133111ebL;mx^=mx>>>30;mx*=0xbf58476d1ce4e5b9L;mx^=mx>>>27;mx*=0x94d049bb133111ebL;
            Random r=new Random(mx^(mx>>>31));
            boolean[] f=new boolean[spn*spn],cf=new boolean[spn*spn],mf=new boolean[spn*spn];
            List<Room> rm=new ArrayList<>(),ma=new ArrayList<>();
            Room hl=cr(r,sp,spn,spn/2,spn/2);rm.add(hl);cv(f,cf,spn,hl,true);
            int rt=sp.mr+r.nextInt(sp.xr-sp.mr+1),mrt=Math.min(rt-1,p==2?4:3);ma.add(hl);
            if(p==2){
                for(int s=0;s<4;s++){Room pm=cpr(r,sp,spn,s,rm);if(pm==null)continue;rm.add(pm);cv(f,cf,spn,pm,true);cc(f,cf,mf,spn,hl,pm,sp.mw,r,true);ma.add(pm);}
            }
            for(int i=rm.size();i<rt;i++){
                boolean mr=i<=mrt;Room pr=mr?(ma.size()>1&&r.nextFloat()<0.65f?ma.get(ma.size()-1):ma.get(r.nextInt(ma.size()))):rm.get(r.nextInt(rm.size()));
                Room cd=null;
                for(int a=0;a<48&&cd==null;a++){Room nx=car(r,sp,spn,pr,r.nextInt(4));if(nx!=null&&!ol(nx,rm))cd=nx;}
                if(cd==null)for(int a=0;a<160&&cd==null;a++){Room nx=cr(r,sp,spn,2+r.nextInt(Math.max(1,spn-4)),2+r.nextInt(Math.max(1,spn-4)));if(nx!=null&&!ol(nx,rm))cd=nx;}
                if(cd==null)continue;
                rm.add(cd);cv(f,cf,spn,cd,false);cc(f,cf,mf,spn,pr,cd,mr?Math.max(4,sp.mw-(i>1?1:0)):sp.cw,r,mr);
                if(mr) ma.add(cd);
            }
            for(int c=0;c<Math.max(1,rm.size()/5);c++){
                Room f1=rm.get(1+r.nextInt(Math.max(1,rm.size()-1))),s2=rm.get(r.nextInt(rm.size()));
                if(f1!=s2) cc(f,cf,mf,spn,f1,s2,sp.cw,r,false);
            }
            if(!rc(f,spn,rm)) for(int i=1;i<rm.size();i++) cc(f,cf,mf,spn,rm.get(i),hl,sp.cw,r,false);
            ccf(f,cf,mf,spn,rm,r,p);boolean[] pf=bpf(f,cf,mf,spn,rm,r),wl=bw(f,spn),mw=bmw(mf,wl,spn),dw=bdw(f,wl,mf,spn,r),cw=bcw(cf,wl,spn);
            int rot=r.nextInt(4);boolean mir=r.nextBoolean();
            boolean[] tf=new boolean[spn*spn],twl=new boolean[spn*spn],tmf=new boolean[spn*spn],tmw=new boolean[spn*spn],tpf=new boolean[spn*spn],tdw=new boolean[spn*spn],tcf=new boolean[spn*spn],tcw=new boolean[spn*spn];
            for(int y=0;y<spn;y++) for(int x=0;x<spn;x++){
                int tx=x,ty=y;if(mir)tx=spn-1-tx;
                for(int t=0;t<rot;t++){int nx=spn-1-ty;ty=tx;tx=nx;}
                int si=x+y*spn,ti=tx+ty*spn;
                tf[ti]=f[si];twl[ti]=wl[si];tmf[ti]=mf[si];tmw[ti]=mw[si];tpf[ti]=pf[si];tdw[ti]=dw[si];tcf[ti]=cf[si];tcw[ti]=cw[si];
            }
            return new Layout(ox,oy,spn,tf,twl,tmf,tmw,tpf,tdw,tcf,tcw);
        }
        static Room cr(Random r,Spec sp,int spn,int cx,int cy){int[] s=rs(r,sp,spn);int w=s[0],h=s[1];if(w<4||h<4||w>spn-2||h>spn-2)return null;int l=Math.max(1,Math.min(spn-w-1,cx-w/2)),t=Math.max(1,Math.min(spn-h-1,cy-h/2));return new Room(l,t,l+w-1,t+h-1,r.nextInt(7));}
        static int[] rs(Random r,Spec sp,int spn){
            int md=Math.max(4,Math.min(spn-4,Math.max(14,Math.min(48,Math.round(spn*0.18f))))),w=0,h=0;
            for(int a=0;a<40;a++){int cw=4+r.nextInt(Math.max(1,md-3)),ch=4+r.nextInt(Math.max(1,md-3)),ar=cw*ch;if(ar>=sp.sma&&ar<=sp.sxa&&cw<=ch*2&&ch<=cw*2){w=cw;h=ch;break;}}
            if(w==0){int t=(sp.sma+sp.sxa)/2;w=Math.max(4,Math.min(md,(int)Math.sqrt(t)));h=Math.max(4,Math.min(md,(t+w-1)/w));}return new int[]{w,h};
        }
        static Room cpr(Random r,Spec sp,int spn,int sd,List<Room> rm){
            for(int a=0;a<64;a++){
                int[] sz=rs(r,sp,spn);int w=sz[0],h=sz[1],cx,cy,jt=Math.max(3,spn/5),of=Math.round((r.nextFloat()*2f-1f)*jt);
                switch(sd){case 0:cx=spn/2+of;cy=1+h/2;break;case 1:cx=spn-2-w/2;cy=spn/2+of;break;case 2:cx=spn/2+of;cy=spn-2-h/2;break;default:cx=1+w/2;cy=spn/2+of;break;}
                if(w<4||h<4||w>spn-2||h>spn-2)continue;int l=Math.max(1,Math.min(spn-w-1,cx-w/2)),t=Math.max(1,Math.min(spn-h-1,cy-h/2));Room c=new Room(l,t,l+w-1,t+h-1,r.nextInt(7));
                if(!ol(c,rm)) return c;
            }return null;
        }
        static Room car(Random r,Spec sp,int spn,Room pr,int sd){
            int[] sz=rs(r,sp,spn);int w=sz[0],h=sz[1],g=2+r.nextInt(3),l,t,of;
            switch(sd){case 0:t=pr.t-g-h;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.w()/3));l=pr.cx()-w/2+of;break;case 1:l=pr.r+g;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.h()/3));t=pr.cy()-h/2+of;break;case 2:t=pr.b+g;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.w()/3));l=pr.cx()-w/2+of;break;default:l=pr.l-g-w;of=Math.round((r.nextFloat()*2f-1f)*Math.max(2,pr.h()/3));t=pr.cy()-h/2+of;break;}
            if(l<1||t<1||l+w>=spn-1||t+h>=spn-1) return null;return new Room(l,t,l+w-1,t+h-1,r.nextInt(7));
        }
        static boolean ol(Room c,List<Room> rm){for(Room r:rm)if(!(c.r+1<r.l||r.r+1<c.l||c.b+1<r.t||r.b+1<c.t))return true;return false;}
        static void cv(boolean[] f,boolean[] cf,int spn,Room r,boolean isC){for(int y=r.t;y<=r.b;y++)for(int x=r.l;x<=r.r;x++)if(r.cn(x,y)){int i=x+y*spn;f[i]=true;if(isC||r.ic(x,y))cf[i]=true;}}
        static void cc(boolean[] f,boolean[] cf,boolean[] mf,int spn,Room f1,Room s2,int w,Random r,boolean mr){
            int sd;if(s2.b<f1.t)sd=0;else if(s2.l>f1.r)sd=1;else if(s2.t>f1.b)sd=2;else if(s2.r<f1.l)sd=3;else{int dx=s2.cx()-f1.cx(),dy=s2.cy()-f1.cy();sd=Math.abs(dx)>=Math.abs(dy)?dx>=0?1:3:dy>=0?2:0;}
            int[] st=dw(f1,sd,r),en=dw(s2,(sd+2)%4,r);
            int dx=en[0]-st[0],dy=en[1]-st[1];float sp=0.35f+r.nextFloat()*0.30f;
            if(Math.abs(dx)>=Math.abs(dy)){int mx=Math.round(st[0]+dx*sp);ch(f,cf,mf,spn,st[0],mx,st[1],w,mr);cvr(f,cf,mf,spn,st[1],en[1],mx,w,mr);ch(f,cf,mf,spn,mx,en[0],en[1],w,mr);}
            else{int my=Math.round(st[1]+dy*sp);cvr(f,cf,mf,spn,st[1],my,st[0],w,mr);ch(f,cf,mf,spn,st[0],en[0],my,w,mr);cvr(f,cf,mf,spn,my,en[1],en[0],w,mr);}
        }
        static int[] dw(Room rm,int sd,Random r){
            boolean hz=sd==0||sd==2;int mn=hz?rm.l+1:rm.t+1,mx=hz?rm.r-1:rm.b-1,pr=mn+r.nextInt(Math.max(1,mx-mn+1));
            for(int of=0;of<=Math.max(rm.w(),rm.h());of++){
                int[] cd={pr-of,pr+of};
                for(int c:cd){if(c<mn||c>mx)continue;int x=hz?c:sd==1?rm.r:rm.l,y=hz?sd==2?rm.b:rm.t:c;if(rm.cn(x,y))return new int[]{x,y};}
            }return new int[]{hz?rm.cx():sd==1?rm.r:rm.l,hz?sd==2?rm.b:rm.t:rm.cy()};
        }
        static void ch(boolean[] f,boolean[] cf,boolean[] mf,int spn,int fx,int tx,int y,int w,boolean mr){int l=Math.min(fx,tx),rg=Math.max(fx,tx),h=w/2;for(int x=l;x<=rg;x++)for(int o=-h;o<w-h;o++)mk(f,cf,mf,spn,x,y+o,mr);}
        static void cvr(boolean[] f,boolean[] cf,boolean[] mf,int spn,int fy,int ty,int x,int w,boolean mr){int t=Math.min(fy,ty),b=Math.max(fy,ty),h=w/2;for(int y=t;y<=b;y++)for(int o=-h;o<w-h;o++)mk(f,cf,mf,spn,x+o,y,mr);}
        static void mk(boolean[] f,boolean[] cf,boolean[] mf,int spn,int x,int y,boolean mr){if(x<1||y<1||x>=spn-1||y>=spn-1)return;int i=x+y*spn;f[i]=true;cf[i]=true;if(mr)mf[i]=true;}
        static void ccf(boolean[] f,boolean[] cf,boolean[] mf,int spn,List<Room> rm,Random r,int p){
            float c=p==0?0.10f:p==1?0.15f:0.20f;int at=spn*(p==0?2:p==1?3:5);
            for(int a=0;a<at;a++){
                if(r.nextFloat()>c)continue;
                int x=2+r.nextInt(Math.max(1,spn-4)),y=2+r.nextInt(Math.max(1,spn-4)),w=1+r.nextInt(p==0?2:3),h=1+r.nextInt(3);
                boolean tv=false;boolean ok=true;
                for(int ly=y;ly<y+h&&ly<spn-1;ly++)for(int lx=x;lx<x+w&&lx<spn-1;lx++){
                    int i=lx+ly*spn;if(!f[i]||cf[i]||mf[i]||(lx>0&&mf[lx-1+ly*spn])||(lx+1<spn&&mf[lx+1+ly*spn])||(ly>0&&mf[lx+(ly-1)*spn])||(ly+1<spn&&mf[lx+(ly+1)*spn])){ok=false;break;}
                    if(lx<=1||ly<=1||lx>=spn-2||ly>=spn-2||!f[i-1]||!f[i+1]||!f[i-spn]||!f[i+spn])tv=true;
                }
                if(ok&&tv)for(int ly=y;ly<y+h&&ly<spn-1;ly++)for(int lx=x;lx<x+w&&lx<spn-1;lx++){f[lx+ly*spn]=false;mf[lx+ly*spn]=false;}
            }
            int st=rm.get(0).cx()+rm.get(0).cy()*spn;if(!f[st])for(int i=0;i<f.length;i++)if(f[i]&&cf[i]){st=i;break;}
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
        static boolean rc(boolean[] f,int spn,List<Room> rm){
            if(rm.isEmpty())return false;boolean[] v=new boolean[f.length];int[] q=new int[f.length];int hd=0,tl=0,st=rm.get(0).cx()+rm.get(0).cy()*spn;
            if(!f[st])return false;q[tl++]=st;v[st]=true;
            while(hd<tl){
                int cr=q[hd++],x=cr%spn,y=cr/spn;
                if(x>0&&f[cr-1]&&!v[cr-1]){v[cr-1]=true;q[tl++]=cr-1;}
                if(x+1<spn&&f[cr+1]&&!v[cr+1]){v[cr+1]=true;q[tl++]=cr+1;}
                if(y>0&&f[cr-spn]&&!v[cr-spn]){v[cr-spn]=true;q[tl++]=cr-spn;}
                if(y+1<spn&&f[cr+spn]&&!v[cr+spn]){v[cr+spn]=true;q[tl++]=cr+spn;}
            }
            for(Room r:rm)if(!v[r.cx()+r.cy()*spn])return false;return true;
        }
        static boolean[] bpf(boolean[] f,boolean[] cf,boolean[] mf,int spn,List<Room> rm,Random r){
            boolean[] p=new boolean[f.length];
            for(int i=1;i<rm.size();i++){
                Room ro=rm.get(i);int pc=1+r.nextInt(2);
                for(int a=0;a<pc;a++){
                    int pw=Math.max(2,Math.min(ro.w(),3+r.nextInt(5))),ph=Math.max(2,Math.min(ro.h(),2+r.nextInt(4)));
                    int l=ro.l+r.nextInt(Math.max(1,ro.w()-pw+1)),t=ro.t+r.nextInt(Math.max(1,ro.h()-ph+1));
                    for(int y=t;y<t+ph;y++)for(int x=l;x<l+pw;x++){int c=x+y*spn;if(f[c]&&!mf[c]&&(!cf[c]||r.nextBoolean()))p[c]=true;}
                }
            }return p;
        }
        static boolean[] bw(boolean[] f,int spn){boolean[] w=new boolean[f.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(!f[i])w[i]=f[i-1]||f[i+1]||f[i-spn]||f[i+spn];}return w;}
        static boolean[] bmw(boolean[] mf,boolean[] w,int spn){boolean[] mw=new boolean[w.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(w[i])mw[i]=mf[i-1]||mf[i+1]||mf[i-spn]||mf[i+spn];}return mw;}
        static boolean[] bcw(boolean[] cf,boolean[] w,int spn){boolean[] cw=new boolean[w.length];for(int y=1;y<spn-1;y++)for(int x=1;x<spn-1;x++){int i=x+y*spn;if(w[i])cw[i]=cf[i-1]||cf[i+1]||cf[i-spn]||cf[i+spn];}return cw;}
        static boolean[] bdw(boolean[] f,boolean[] w,boolean[] mf,int spn,Random r){
            boolean[] d=new boolean[f.length];
            for(int y=2;y<spn-2;y++)for(int x=2;x<spn-2;x++){
                int i=x+y*spn;if(f[i]||w[i]||mf[i-1]||mf[i+1]||mf[i-spn]||mf[i+spn])continue;
                if((w[i-1]||w[i+1]||w[i-spn]||w[i+spn])&&r.nextFloat()<0.24f)d[i]=true;
            }return d;
        }
        static class Spec{
            final int sp,sma,sxa,mr,xr,cw,mw;final float wd,fd;
            Spec(int s,int mn,int mx,int mir,int mar,int c,int m,float w,float f){sp=s;sma=mn;sxa=mx;mr=mir;xr=mar;cw=c;mw=m;wd=w;fd=f;}
        }
        static class Bounds{
            final int minX,minY,maxX,maxY;Bounds(int mnX,int mnY,int mxX,int mxY){minX=mnX;minY=mnY;maxX=mxX;maxY=mxY;}
            boolean contains(int w,int h){return minX>=0&&minY>=0&&maxX<w&&maxY<h;}
            boolean intersects(Bounds o){return minX<=o.maxX&&maxX>=o.minX&&minY<=o.maxY&&maxY>=o.minY;}
        }
        static class Layout{
            final int originX,originY,span;final boolean[] floor,wall,mainFloor,mainWall,panelFloor,debrisWall,criticalFloor,criticalWall;
            Layout(int ox,int oy,int s,boolean[] f,boolean[] w,boolean[] mf,boolean[] mw,boolean[] pf,boolean[] dw,boolean[] cf,boolean[] cw){originX=ox;originY=oy;span=s;floor=f;wall=w;mainFloor=mf;mainWall=mw;panelFloor=pf;debrisWall=dw;criticalFloor=cf;criticalWall=cw;}
        }
        static class Room{
            final int l,t,r,b,s;Room(int left,int top,int right,int bottom,int shape){l=left;t=top;r=right;b=bottom;s=shape;}
            int cx(){return(l+r)/2;}int cy(){return(t+b)/2;}int w(){return r-l+1;}int h(){return b-t+1;}
            boolean ic(int x,int y){return Math.abs(x-cx())<=1&&Math.abs(y-cy())<=1;}
            boolean cn(int x,int y){
                int lx=x-l,ly=y-t,wid=w(),hgt=h();if(s==0||wid<6||hgt<6)return true;
                boolean tl=lx<2&&ly<2,tr=lx>=wid-2&&ly<2,bl=lx<2&&ly>=hgt-2,br=lx>=wid-2&&ly>=hgt-2;
                if(s==1)return!(tl||br);if(s==2)return!(tr||bl);if(s==3)return!(tl||tr);if(s==4)return!(bl||br);
                if(s==5)return!(lx==0&&ly>=hgt/3&&ly<=hgt*2/3)&&!(lx==wid-1&&ly>=hgt/4&&ly<=hgt*3/4);
                return!(ly==0&&lx>=wid/3&&lx<=wid*2/3)&&!(ly==hgt-1&&lx>=wid/4&&lx<=wid*3/4);
            }
        }
    }
                            }
