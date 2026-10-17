package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;
import java.util.Arrays;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedWaterFilter extends GenerateFilter {
    public float waterCoverage=35f,waterScale=90f,contourComplexity=0.55f,edgeBias=0f,shoalWidth=2f,shallowWidth=5f,depthWarpScale=0.02f,depthWarpMag=3f,shoalFragmentation=0f,shallowFragmentation=0f;
    public Block shoalFloor=Blocks.sandWater,shallowFloor=Blocks.water,deepFloor=Blocks.deepwater;
    public boolean naturalCleanup=true;
    private transient volatile CacheEntry cache;
    @Override public String name(){return Core.bundle.get("filter.advancedwater.name","Natural Water");}
    @Override public char icon(){return Iconc.liquidWater;}
    @Override public FilterOption[] options(){
        ensureDefaultFloors();
        return new FilterOption[]{
            new SliderOption("watercoverage",()->waterCoverage,v->waterCoverage=v,0f,100f,1f),
            new SliderOption("waterscale",()->waterScale,v->waterScale=v,4f,300f,1f),
            new SliderOption("contourcomplexity",()->contourComplexity,v->contourComplexity=v,0f,1f,0.01f),
            new SliderOption("edgebias",()->edgeBias,v->edgeBias=v,-1f,1f,0.01f),
            new SliderOption("shoalwidth",()->shoalWidth,v->shoalWidth=v,0f,30f,1f),
            new SliderOption("shallowwidth",()->shallowWidth,v->shallowWidth=v,0f,50f,1f),
            new SliderOption("depthwarpscale",()->depthWarpScale,v->depthWarpScale=v,0.005f,0.2f,0.005f),
            new SliderOption("depthwarpmag",()->depthWarpMag,v->depthWarpMag=v,0f,10f,0.5f),
            new SliderOption("shoalfragmentation",()->shoalFragmentation,v->shoalFragmentation=v,0f,1f,0.01f),
            new SliderOption("shallowfragmentation",()->shallowFragmentation,v->shallowFragmentation=v,0f,1f,0.01f),
            new BlockOption("shoalfloor",()->shoalFloor,v->shoalFloor=v,floorsOnly),
            new BlockOption("shallowfloor",()->shallowFloor,v->shallowFloor=v,floorsOnly),
            new BlockOption("deepfloor",()->deepFloor,v->deepFloor=v,floorsOnly),
            new ToggleOption("naturalcleanup",()->naturalCleanup,v->naturalCleanup=v)
        };
    }
    @Override public void apply(GenerateInput in){
        ensureDefaultFloors();
        CacheEntry entry=ensureCache(in.width,in.height);
        int x=Math.max(0,Math.min(entry.width-1,in.x));
        int y=Math.max(0,Math.min(entry.height-1,in.y));
        byte layer=entry.layers[x+y*entry.width];
        if(layer==0) return;
        in.floor=layer==1?validFloor(shoalFloor,Blocks.sandWater):layer==2?validFloor(shallowFloor,Blocks.water):validFloor(deepFloor,Blocks.deepwater);
        if(!in.block.synthetic()) in.block=Blocks.air;
        if(in.overlay!=Blocks.spawn) in.overlay=Blocks.air;
    }
    private CacheEntry ensureCache(int w,int h){
        CacheKey k=new CacheKey(w,h,seed,waterCoverage,waterScale,contourComplexity,edgeBias,shoalWidth,shallowWidth,depthWarpScale,depthWarpMag,shoalFragmentation,shallowFragmentation,naturalCleanup,blockName(shoalFloor),blockName(shallowFloor),blockName(deepFloor));
        CacheEntry c=cache;
        if(c!=null&&c.key.equals(k)) return c;
        synchronized(this){
            c=cache;
            if(c!=null&&c.key.equals(k)) return c;
            byte[] l=generateLayers(w,h);
            c=new CacheEntry(k,w,h,l);
            cache=c;
            return c;
        }
    }
    private void ensureDefaultFloors(){
        if(!(shoalFloor instanceof Floor)) shoalFloor=Blocks.sandWater;
        if(!(shallowFloor instanceof Floor)) shallowFloor=Blocks.water;
        if(!(deepFloor instanceof Floor)) deepFloor=Blocks.deepwater;
    }
    private Block validFloor(Block v,Block f){return v instanceof Floor?v:f;}
    private String blockName(Block b){return b==null?"":b.name;}
    private float clamp(float v,float min,float max){return Float.isNaN(v)||Float.isInfinite(v)?min:Math.max(min,Math.min(max,v));}
    private byte[] generateLayers(int w,int h){
        int area=w*h;
        float cov=clamp(waterCoverage/100f,0f,1f);
        int tw=Math.max(0,Math.min(area,Math.round(area*cov)));
        int pad=Math.max(8,Math.min(64,(int)Math.ceil(Math.max(0f,shoalWidth)+Math.max(0f,shallowWidth))+4));
        int pw=w+pad*2,ph=h+pad*2;
        float[] f=new float[pw*ph];
        float sc=Math.max(4f,waterScale),cx=clamp(contourComplexity,0f,1f),eb=clamp(edgeBias,-1f,1f);
        float ws=sc*(1.25f-cx*0.35f),wm=sc*(0.07f+cx*0.24f),ds=Math.max(3f,sc*(0.22f+cx*0.18f));
        int oct=3+Math.round(cx*3f);
        float per=0.46f+cx*0.12f,hm=Math.max(1f,Math.min(w,h)*0.5f);
        for(int py=0;py<ph;py++){
            int y=py-pad;
            for(int px=0;px<pw;px++){
                int x=px-pad;
                float nx=(float)Simplex.noise2d(seed+0x2f31,2,0.55f,1f/ws,x+10f,y+10f)*2f-1f;
                float ny=(float)Simplex.noise2d(seed-0x5a17,2,0.55f,1f/ws,x+10f,y+10f)*2f-1f;
                float sx=x+nx*wm,sy=y+ny*wm;
                float base=(float)Simplex.noise2d(seed,oct,per,1f/sc,sx+10f,sy+10f)*2f-1f;
                float det=(float)Simplex.noise2d(seed+0x714b,2,0.5f,1f/ds,sx+10f,sy+10f)*2f-1f;
                float me=Math.min(Math.min(x,w-1-x),Math.min(y,h-1-y));
                float ei=1f-clamp(me/hm,0f,1f);
                f[px+py*pw]=base+det*(0.12f+cx*0.34f)-eb*ei*1.15f;
            }
        }
        float[] cv=new float[w*h];
        for(int y=0;y<h;y++) System.arraycopy(f,pad+(y+pad)*pw,cv,y*w,w);
        Arrays.sort(cv);
        boolean[] wt=new boolean[pw*ph];
        if(tw>=area) Arrays.fill(wt,true);
        else if(tw>0){
            if(!naturalCleanup){
                float th=cv[Math.max(0,Math.min(cv.length-1,Math.round(cv.length*cov)))];
                for(int i=0;i<f.length;i++) wt[i]=f[i]<=th;
            }else{
                int tol=Math.max(1,(int)Math.ceil(w*(double)h*0.03));
                float low=0f,high=1f,frac=cov;
                boolean[] best=null;int bestD=Integer.MAX_VALUE;
                for(int a=0;a<8;a++){
                    int idx=Math.max(0,Math.min(cv.length-1,Math.round(cv.length*frac)));
                    float th=cv[idx];
                    boolean[] cwt=new boolean[f.length];
                    for(int i=0;i<f.length;i++) cwt[i]=f[i]<=th;
                    naturalize(cwt,pw,ph,sc);
                    fillEnclosed(cwt,pw,pad,w,h);
                    int cw=0;
                    for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(cwt[pad+x+(pad+y)*pw]) cw++;
                    int d=Math.abs(cw-tw);
                    if(best==null||d<bestD){best=cwt;bestD=d;}
                    if(d<=tol) break;
                    if(cw>tw) high=frac;else low=frac;
                    frac=(low+high)*0.5f;
                }
                wt=best;
            }
        }
        byte[] pl=distanceLayers(wt,pw,ph,seed,Math.max(0f,shoalWidth),Math.max(0f,shallowWidth),Math.max(0.005f,depthWarpScale),Math.max(0f,depthWarpMag),clamp(shoalFragmentation,0f,1f),clamp(shallowFragmentation,0f,1f));
        byte[] l=new byte[area];
        for(int y=0;y<h;y++) System.arraycopy(pl,pad+(y+pad)*pw,l,y*w,w);
        return l;
    }
    private void naturalize(boolean[] w,int pw,int ph,float sc){
        int ms=Math.max(2,Math.min(12,Math.round(sc*0.08f)));
        rmSmall(w,pw,ph,ms);
        closeW(w,pw,ph);openW(w,pw,ph);
        smooth(w,pw,ph);smooth(w,pw,ph);
        prune(w,pw,ph,true,8);prune(w,pw,ph,false,8);prune(w,pw,ph,true,4);
        rmSmall(w,pw,ph,ms);
    }
    private void rmSmall(boolean[] w,int pw,int ph,int ms){
        boolean[] v=new boolean[w.length];
        int[] q=new int[w.length];
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int s=0;s<w.length;s++){
            if(!w[s]||v[s]) continue;
            int hd=0,tl=0;q[tl++]=s;v[s]=true;
            while(hd<tl){
                int i=q[hd++];
                int cx=i%pw,cy=i/pw;
                for(int d=0;d<4;d++){
                    int nx=cx+dx[d],ny=cy+dy[d];
                    if(nx>=0&&nx<pw&&ny>=0&&ny<ph){
                        int ni=nx+ny*pw;
                        if(w[ni]&&!v[ni]){v[ni]=true;q[tl++]=ni;}
                    }
                }
            }
            if(tl<ms) for(int i=0;i<tl;i++) w[q[i]]=false;
        }
    }
    private void fillEnclosed(boolean[] wt,int pw,int pad,int w,int h){
        boolean[] v=new boolean[w*h];
        int[] q=new int[w*h];
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int s=0;s<v.length;s++){
            int sx=s%w,sy=s/w,ps=sx+pad+(sy+pad)*pw;
            if(wt[ps]||v[s]) continue;
            int hd=0,tl=0;boolean te=false;q[tl++]=s;v[s]=true;
            while(hd<tl){
                int i=q[hd++];
                int cx=i%w,cy=i/w;
                if(cx==0||cy==0||cx==w-1||cy==h-1) te=true;
                for(int d=0;d<4;d++){
                    int nx=cx+dx[d],ny=cy+dy[d];
                    if(nx>=0&&nx<w&&ny>=0&&ny<h){
                        int ni=nx+ny*w,pni=nx+pad+(ny+pad)*pw;
                        if(!wt[pni]&&!v[ni]){v[ni]=true;q[tl++]=ni;}
                    }
                }
            }
            if(!te) for(int i=0;i<tl;i++) wt[(q[i]%w)+pad+((q[i]/w)+pad)*pw]=true;
        }
    }
    private void closeW(boolean[] w,int pw,int ph){boolean[] d=dilate(w,pw,ph);System.arraycopy(erode(d,pw,ph),0,w,0,w.length);}
    private void openW(boolean[] w,int pw,int ph){boolean[] e=erode(w,pw,ph);System.arraycopy(dilate(e,pw,ph),0,w,0,w.length);}
    private boolean[] dilate(boolean[] w,int pw,int ph){
        boolean[] r=w.clone();
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int y=0;y<ph;y++) for(int x=0;x<pw;x++){
            int i=x+y*pw;if(w[i]) continue;
            for(int d=0;d<4;d++){
                int nx=x+dx[d],ny=y+dy[d];
                if(nx>=0&&nx<pw&&ny>=0&&ny<ph&&w[nx+ny*pw]){r[i]=true;break;}
            }
        }
        return r;
    }
    private boolean[] erode(boolean[] w,int pw,int ph){
        boolean[] r=w.clone();
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int y=0;y<ph;y++) for(int x=0;x<pw;x++){
            int i=x+y*pw;if(!w[i]) continue;
            for(int d=0;d<4;d++){
                int nx=x+dx[d],ny=y+dy[d];
                if(nx<0||nx>=pw||ny<0||ny>=ph||!w[nx+ny*pw]){r[i]=false;break;}
            }
        }
        return r;
    }
    private void smooth(boolean[] w,int pw,int ph){
        boolean[] n=w.clone();
        for(int y=0;y<ph;y++) for(int x=0;x<pw;x++){
            int nb=0;
            for(int oy=-1;oy<=1;oy++) for(int ox=-1;ox<=1;ox++){
                if(ox==0&&oy==0) continue;
                int nx=x+ox,ny=y+oy;
                if(nx>=0&&nx<pw&&ny>=0&&ny<ph&&w[nx+ny*pw]) nb++;
            }
            int i=x+y*pw;
            if(w[i]&&nb<=2) n[i]=false;
            if(!w[i]&&nb>=6) n[i]=true;
        }
        System.arraycopy(n,0,w,0,w.length);
    }
    private void prune(boolean[] w,int pw,int ph,boolean val,int mp){
        for(int p=0;p<mp;p++){
            boolean[] n=w.clone();boolean ch=false;
            for(int y=1;y+1<ph;y++) for(int x=1;x+1<pw;x++){
                int i=x+y*pw;if(w[i]!=val) continue;
                int m=0;
                if(w[i-1]==val) m++;if(w[i+1]==val) m++;
                if(w[i-pw]==val) m++;if(w[i+pw]==val) m++;
                if(m<=1){n[i]=!val;ch=true;}
            }
            System.arraycopy(n,0,w,0,w.length);
            if(!ch) break;
        }
    }
    private byte[] distanceLayers(boolean[] w,int pw,int ph,int sd,float shW,float shaW,float ws,float wm,float shF,float shaF){
        byte[] l=new byte[w.length];
        int[] d=new int[w.length];
        int inf=pw+ph+2;
        for(int i=0;i<w.length;i++){
            int x=i%pw,y=i/pw;
            d[i]=w[i]?(x==0||y==0||x==pw-1||y==ph-1?1:inf):0;
        }
        for(int y=0;y<ph;y++) for(int x=0;x<pw;x++){
            int i=x+y*pw;if(!w[i]) continue;
            if(x>0) d[i]=Math.min(d[i],d[i-1]+1);
            if(y>0) d[i]=Math.min(d[i],d[i-pw]+1);
        }
        for(int y=ph-1;y>=0;y--) for(int x=pw-1;x>=0;x--){
            int i=x+y*pw;if(!w[i]) continue;
            if(x+1<pw) d[i]=Math.min(d[i],d[i+1]+1);
            if(y+1<ph) d[i]=Math.min(d[i],d[i+pw]+1);
        }
        int sho=Math.max(1,Math.round(shW)),sha=sho+Math.max(0,Math.round(shaW));
        for(int i=0;i<w.length;i++){
            if(!w[i]) continue;
            int bl=d[i]<=sho?1:d[i]<=sha?2:3;
            int lr=bl;
            if(wm>0.001f){
                int x=i%pw,y=i/pw;
                float wrp=(float)Simplex.noise2d(sd+777,1,1f,1f/ws,x+10f,y+10f)*2f-1f;
                float sft=d[i]+wrp*wm;
                int tl=sft<=sho?1:sft<=sha?2:3;
                lr=bl==1?Math.min(tl,2):bl==3?Math.max(tl,2):tl;
            }
            if(lr==1&&shF>0.001f&&sho>0){
                float t=(float)d[i]/sho,kn=(float)Simplex.noise2d(sd+111,1,1f,0.04f,i%pw+10f,i/pw+10f)*2f-1f;
                if(kn<t*shF-1f) lr=0;
            }else if(lr==2&&shaF>0.001f&&sha>sho){
                float t=(float)(d[i]-sho)/(sha-sho),kn=(float)Simplex.noise2d(sd+222,1,1f,0.04f,i%pw+10f,i/pw+10f)*2f-1f;
                if(kn<t*shaF-1f) lr=1;
            }
            l[i]=(byte)lr;
        }
        return l;
    }
    private static final class CacheEntry{
        final CacheKey key;final int width,height;final byte[] layers;
        CacheEntry(CacheKey k,int w,int h,byte[] l){key=k;width=w;height=h;layers=l;}
    }
    private static final class CacheKey{
        final int w,h,s,cov,scl,cx,eb,sw,shw,dws,dwm,sf,shf;
        final boolean nc;final String sfN,shfN,dfN;
        CacheKey(int w,int h,int s,float cov,float scl,float cx,float eb,float sw,float shw,float dws,float dwm,float sf,float shf,boolean nc,String sfN,String shfN,String dfN){
            this.w=w;this.h=h;this.s=s;this.cov=Float.floatToIntBits(cov);this.scl=Float.floatToIntBits(scl);
            this.cx=Float.floatToIntBits(cx);this.eb=Float.floatToIntBits(eb);this.sw=Float.floatToIntBits(sw);
            this.shw=Float.floatToIntBits(shw);this.dws=Float.floatToIntBits(dws);this.dwm=Float.floatToIntBits(dwm);
            this.sf=Float.floatToIntBits(sf);this.shf=Float.floatToIntBits(shf);this.nc=nc;
            this.sfN=sfN;this.shfN=shfN;this.dfN=dfN;
        }
        @Override public boolean equals(Object o){
            if(this==o) return true;
            if(!(o instanceof CacheKey k)) return false;
            return w==k.w&&h==k.h&&s==k.s&&cov==k.cov&&scl==k.scl&&cx==k.cx&&eb==k.eb&&sw==k.sw&&shw==k.shw&&dws==k.dws&&dwm==k.dwm&&sf==k.sf&&shf==k.shf&&nc==k.nc&&sfN.equals(k.sfN)&&shfN.equals(k.shfN)&&dfN.equals(k.dfN);
        }
        @Override public int hashCode(){return w*31+h*31+s*31+cov;}
    }
}
