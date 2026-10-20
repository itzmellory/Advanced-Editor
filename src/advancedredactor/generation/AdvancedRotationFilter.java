package advancedredactor.generation;
import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import java.lang.reflect.*;
public class AdvancedRotationFilter extends GenerateFilter{
    public boolean rotate4Way=true;
    public boolean flipDirection=false;
    private static transient Method tileMethod;
    static{
        try{
            tileMethod=GenerateInput.class.getDeclaredMethod("tile",float.class,float.class);
            tileMethod.setAccessible(true);
        }catch(Exception ignored){}
    }
    @Override public String name(){return Core.bundle.get("filter.advancedrotation.name","Advanced Rotation");}
    @Override public char icon(){return Iconc.refresh;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new ToggleOption("rotate4way",()->rotate4Way,v->rotate4Way=v),
            new ToggleOption("flipdirection",()->flipDirection,v->flipDirection=v)
        };
    }
    @Override public boolean isPost(){return true;}
    @Override public boolean isBuffered(){return true;}
    @Override public void apply(GenerateInput in){
        int w=in.width,h=in.height;
        int x=in.x,y=in.y;
        int hx=w/2,hy=h/2;
        if(rotate4Way){
            boolean top=y>=hy;
            boolean right=x>=hx;
            if(!flipDirection){
                if(top&&!right)return;
                int sx=x,sy=y,steps=0;
                if(top&&right){
                    sx=w-1-y;
                    sy=x;
                    steps=3;
                }else if(!top&&right){
                    sx=w-1-x;
                    sy=h-1-y;
                    steps=2;
                }else{
                    sx=y;
                    sy=h-1-x;
                    steps=1;
                }
                copyRotated(in,sx,sy,steps);
            }else{
                if(!top&&!right)return;
                int sx=x,sy=y,steps=0;
                if(top&&!right){
                    sx=h-1-y;
                    sy=x;
                    steps=1;
                }else if(top&&right){
                    sx=w-1-x;
                    sy=h-1-y;
                    steps=2;
                }else{
                    sx=y;
                    sy=w-1-x;
                    steps=3;
                }
                copyRotated(in,sx,sy,steps);
            }
        }else{
            if(y>hy||(y==hy&&x<hx))return;
            int sx=w-1-x,sy=h-1-y;
            copyRotated(in,sx,sy,2);
        }
    }
    private void copyRotated(GenerateInput in,int sx,int sy,int steps){
        sx=Math.max(0,Math.min(in.width-1,sx));
        sy=Math.max(0,Math.min(in.height-1,sy));
        Tile src=readTile(in,sx,sy);
        if(src==null)return;
        in.floor=src.floor();
        if(!in.block.synthetic()&&!src.block().synthetic()){
            in.block=src.block();
        }
        in.overlay=src.overlay();
        Tile dst=readTile(in,in.x,in.y);
        if(dst!=null&&src.build!=null&&dst.build!=null){
            dst.build.rotation=(src.build.rotation+steps)&3;
        }
    }
    @Override public void draw(Image image){
        super.draw(image);
        float w=image.getWidth(),h=image.getHeight();
        float x=image.x,y=image.y;
        Lines.stroke(Scl.scl(2f),Pal.accent);
        Lines.line(x,y+h/2f,x+w,y+h/2f);
        if(rotate4Way){
            Lines.line(x+w/2f,y,x+w/2f,y+h);
        }
        Draw.reset();
    }
    private static Tile readTile(GenerateInput in,int x,int y){
        try{if(tileMethod!=null)return(Tile)tileMethod.invoke(in,(float)x,(float)y);}catch(Exception ignored){}
        return null;
    }
}
