package advancedredactor.LeftSideUtils;
import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import advancedredactor.LeftSideUtils.MapArtDialog.*;
import java.lang.reflect.*;
public class MapArtGenerator{
    public static void generate(Fi file,int targetW,int targetH,ArtStyle style,LayerMode layerMode,boolean useAll,ObjectSet<Block> allowed){
        try{
            Pixmap raw=new Pixmap(file);
            int w=Math.max(1,targetW),h=Math.max(1,targetH);
            Pixmap img=StencilManager.scalePixmap(raw,w,h);
            raw.dispose();
            if(Vars.editor.width()!=w||Vars.editor.height()!=h)Vars.editor.resize(w,h,0,0);
            if(style==ArtStyle.colored)applyColoredArt(img,w,h,layerMode);
            else applyTerrainArt(img,w,h,layerMode,useAll,allowed);
            img.dispose();
            Vars.editor.clearOp();
        }catch(Exception e){
            Vars.ui.showException("@editor.errorload",e);
        }
    }
    private static void setExtra(Tile t,int val){
        try{
            Field f=t.getClass().getField("extraData");
            f.set(t,val);
        }catch(Throwable ignored){
            try{
                Field f2=t.getClass().getDeclaredField("extraData");
                f2.setAccessible(true);
                f2.set(t,val);
            }catch(Throwable ignored2){}
        }
    }
    private static void applyColoredArt(Pixmap img,int w,int h,LayerMode mode){
        Block cFloorRaw=Vars.content.block("colored-floor");
        Block cWallRaw=Vars.content.block("colored-wall");
        final Block cFloor=(cFloorRaw==null)?Blocks.stone:cFloorRaw;
        final Block cWall=(cWallRaw==null)?Blocks.stoneWall:cWallRaw;
        Vars.editor.load(()->{
            for(int ty=0;ty<h;ty++){
                int sy=h-1-ty;
                for(int x=0;x<w;x++){
                    Tile t=Vars.editor.tile(x,ty);
                    if(t==null)continue;
                    int rgba=img.getRaw(x,sy);
                    int r=(rgba>>>24)&0xff;
                    int g=(rgba>>>16)&0xff;
                    int b=(rgba>>>8)&0xff;
                    int a=rgba&0xff;
                    if(a<255){
                        float alpha=a/255f;
                        r=Math.round(r*alpha);
                        g=Math.round(g*alpha);
                        b=Math.round(b*alpha);
                    }
                    int extra=(r<<24)|(g<<16)|(b<<8)|1;
                    t.setOverlay(Blocks.air);
                    if(mode==LayerMode.floorOnly){
                        if(cFloor instanceof Floor)t.setFloor((Floor)cFloor);
                        t.setBlock(Blocks.air);
                    }else if(mode==LayerMode.wallOnly){
                        t.setBlock(a>32?cWall:Blocks.air);
                    }else{
                        if(cFloor instanceof Floor)t.setFloor((Floor)cFloor);
                        float lum=0.299f*r+0.587f*g+0.114f*b;
                        t.setBlock((a>32&&lum<95f)?cWall:Blocks.air);
                    }
                    setExtra(t,extra);
                }
            }
        });
    }
    private static void applyTerrainArt(Pixmap img,int w,int h,LayerMode mode,boolean useAll,ObjectSet<Block> allowed){
        Seq<Floor> floors=new Seq<>();
        Seq<Block> walls=new Seq<>();
        for(Block b:Vars.content.blocks()){
            if(!MapArtDialog.isEligibleTerrainBlock(b))continue;
            if(!useAll&&!allowed.contains(b))continue;
            if(b.isFloor()&&b instanceof Floor)floors.add((Floor)b);
            else if(!b.isFloor()&&b.isStatic())walls.add(b);
        }
        if(floors.isEmpty())floors.add((Floor)Blocks.stone);
        if(walls.isEmpty())walls.add(Blocks.stoneWall);
        Vars.editor.load(()->{
            for(int ty=0;ty<h;ty++){
                int sy=h-1-ty;
                for(int x=0;x<w;x++){
                    Tile t=Vars.editor.tile(x,ty);
                    if(t==null)continue;
                    int rgba=img.getRaw(x,sy);
                    int r=(rgba>>>24)&0xff;
                    int g=(rgba>>>16)&0xff;
                    int b=(rgba>>>8)&0xff;
                    int a=rgba&0xff;
                    t.setOverlay(Blocks.air);
                    if(a<25){
                        t.setBlock(Blocks.air);
                        continue;
                    }
                    if(mode==LayerMode.floorOnly){
                        t.setFloor(bestFloor(floors,r,g,b));
                        t.setBlock(Blocks.air);
                    }else if(mode==LayerMode.wallOnly){
                        t.setBlock(bestBlock(walls,r,g,b));
                    }else{
                        Floor bf=bestFloor(floors,r,g,b);
                        Block bw=bestBlock(walls,r,g,b);
                        float df=colorDist(bf.mapColor,r,g,b);
                        float dw=colorDist(bw.mapColor,r,g,b);
                        t.setFloor(bf);
                        t.setBlock(dw<df*0.92f?bw:Blocks.air);
                    }
                }
            }
        });
    }
    private static Floor bestFloor(Seq<Floor> list,int r,int g,int b){
        Floor best=list.first();
        float min=Float.MAX_VALUE;
        for(int i=0;i<list.size;i++){
            Floor f=list.get(i);
            float d=colorDist(f.mapColor,r,g,b);
            if(d<min){min=d;best=f;}
        }
        return best;
    }
    private static Block bestBlock(Seq<Block> list,int r,int g,int b){
        Block best=list.first();
        float min=Float.MAX_VALUE;
        for(int i=0;i<list.size;i++){
            Block bl=list.get(i);
            float d=colorDist(bl.mapColor,r,g,b);
            if(d<min){min=d;best=bl;}
        }
        return best;
    }
    private static float colorDist(Color c,int r,int g,int b){
        float cr=c.r*255f-r;
        float cg=c.g*255f-g;
        float cb=c.b*255f-b;
        return cr*cr*0.3f+cg*cg*0.59f+cb*cb*0.11f;
    }
}
