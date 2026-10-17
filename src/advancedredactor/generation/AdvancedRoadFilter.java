package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedRoadFilter extends GenerateFilter{
    public Block roadFloor=Blocks.darkPanel3,bridgeFloor=Blocks.metalFloor,bridgeWall=Blocks.darkMetal;
    public float nodesCount=15f,roadWidth=2f,bridgeWidth=3f,warpScale=40f,warpMag=10f;
    private transient Segment[] segs;
    private transient int lw,lh,ls=-1;
    private transient float ln;
    @Override public String name(){return Core.bundle.get("filter.advancedroad.name","Advanced Roads");}
    @Override public char icon(){return Iconc.blockDarkPanel3;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new BlockOption("roadfloor",()->roadFloor,v->roadFloor=v,floorsOnly),
            new BlockOption("bridgefloor",()->bridgeFloor,v->bridgeFloor=v,floorsOnly),
            new BlockOption("bridgewall",()->bridgeWall,v->bridgeWall=v,b->b==Blocks.air||wallsOnly.get(b)),
            new SliderOption("nodescount",()->nodesCount,v->nodesCount=v,2f,50f,1f),
            new SliderOption("roadwidth",()->roadWidth,v->roadWidth=v,1f,10f,0.5f),
            new SliderOption("bridgewidth",()->bridgeWidth,v->bridgeWidth=v,1f,15f,0.5f),
            new SliderOption("warpscale",()->warpScale,v->warpScale=v,5f,100f,1f),
            new SliderOption("warpmag",()->warpMag,v->warpMag=v,0f,30f,1f)
        };
    }
    private void buildGraph(int w,int h){
        if(segs!=null&&ls==seed&&lw==w&&lh==h&&ln==nodesCount)return;
        ls=seed;lw=w;lh=h;ln=nodesCount;
        int n=(int)nodesCount;Rand r=new Rand(seed);Seq<Vec2> pts=new Seq<>();
        for(int i=0;i<n;i++)pts.add(new Vec2(r.random(w),r.random(h)));
        Seq<Segment> s=new Seq<>();
        for(int i=0;i<n;i++)for(int j=i+1;j<n;j++){
            Vec2 p1=pts.get(i),p2=pts.get(j);boolean ok=true;float d2=p1.dst2(p2);
            for(int k=0;k<n;k++){
                if(k==i||k==j)continue;
                if(pts.get(k).dst2(p1)<d2&&pts.get(k).dst2(p2)<d2){ok=false;break;}
            }
            if(ok)s.add(new Segment(p1.x,p1.y,p2.x,p2.y));
        }
        segs=s.toArray(Segment.class);
    }
    private float dst(float px,float py,float x1,float y1,float x2,float y2){
        float l2=(x1-x2)*(x1-x2)+(y1-y2)*(y1-y2);
        if(l2==0)return Mathf.dst(px,py,x1,y1);
        float t=Math.max(0,Math.min(1,((px-x1)*(x2-x1)+(py-y1)*(y2-y1))/l2));
        return Mathf.dst(px,py,x1+t*(x2-x1),y1+t*(y2-y1));
    }
    @Override public void apply(GenerateInput in){
        buildGraph(in.width,in.height);
        float wx=in.x,wy=in.y;
        if(warpMag>0){
            wx+=Simplex.noise2d(seed,2,0.5f,1f/warpScale,in.x,in.y)*warpMag;
            wy+=Simplex.noise2d(seed+1,2,0.5f,1f/warpScale,in.x,in.y)*warpMag;
        }
        float md=Float.MAX_VALUE;
        for(Segment s:segs)md=Math.min(md,dst(wx,wy,s.x1,s.y1,s.x2,s.y2));
        boolean wtr=in.floor!=null&&in.floor.isLiquid;
        float rw=wtr?bridgeWidth:roadWidth;
        if(md<=rw){
            in.floor=wtr?bridgeFloor:roadFloor;in.block=Blocks.air;
            if(wtr&&bridgeWall!=Blocks.air&&bridgeWall!=null&&md>rw-1.2f)in.block=bridgeWall;
        }
    }
    private static class Segment{
        float x1,y1,x2,y2;
        Segment(float a,float b,float c,float d){x1=a;y1=b;x2=c;y2=d;}
    }
}
