package advancedredactor.upperuiutils;
import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.graphics.*;
import mindustry.world.*;
public class CustomShapeManager{
    public enum BrushShape{
        circle("●","Circle"),
        square("■","Square"),
        diamond("◆","Diamond"),
        cross("✚","Cross");
        public final String symbol,title;
        BrushShape(String s,String t){symbol=s;title=t;}
    }
    public static BrushShape currentShape=BrushShape.circle;
    public static boolean line45Mode=false;
    private static final IntSet maskCache=new IntSet();
    public static boolean inside(int dx,int dy,float radius,BrushShape shape){
        if(radius<=1f)return dx==0&&dy==0;
        switch(shape){
            case square:return Math.abs(dx)<=radius-0.5f&&Math.abs(dy)<=radius-0.5f;
            case diamond:return Math.abs(dx)+Math.abs(dy)<=radius;
            case cross:{
                float arm=Math.max(0f,(radius-1f)/3f);
                return(Math.abs(dx)<=arm&&Math.abs(dy)<=radius)||(Math.abs(dy)<=arm&&Math.abs(dx)<=radius);
            }
            default:return Mathf.within(dx,dy,radius-0.5f+0.0001f);
        }
    }
    public static void iterateBrush(int cx,int cy,float radius,BrushShape shape,Cons<Tile> cons){
        int clamped=Math.max(1,(int)Math.ceil(radius));
        int w=Vars.editor.width(),h=Vars.editor.height();
        for(int rx=-clamped;rx<=clamped;rx++){
            for(int ry=-clamped;ry<=clamped;ry++){
                if(inside(rx,ry,radius,shape)){
                    int wx=cx+rx,wy=cy+ry;
                    if(wx>=0&&wy>=0&&wx<w&&wy<h){
                        Tile t=Vars.editor.tile(wx,wy);
                        if(t!=null)cons.get(t);
                    }
                }
            }
        }
    }
    public static Point2 snap45(int x1,int y1,int x2,int y2){
        int dx=x2-x1,dy=y2-y1;
        if(dx==0&&dy==0)return new Point2(x1,y1);
        float angle=Angles.angle(x1,y1,x2,y2);
        int oct=Math.round(angle/45f)%8;
        int dist=Math.max(Math.abs(dx),Math.abs(dy));
        int ex=x1,ey=y1;
        switch(oct){
            case 0:ex=x1+dist;ey=y1;break;
            case 1:ex=x1+dist;ey=y1+dist;break;
            case 2:ex=x1;ey=y1+dist;break;
            case 3:ex=x1-dist;ey=y1+dist;break;
            case 4:ex=x1-dist;ey=y1;break;
            case 5:ex=x1-dist;ey=y1-dist;break;
            case 6:ex=x1;ey=y1-dist;break;
            case 7:ex=x1+dist;ey=y1-dist;break;
        }
        ex=Mathf.clamp(ex,0,Vars.editor.width()-1);
        ey=Mathf.clamp(ey,0,Vars.editor.height()-1);
        return new Point2(ex,ey);
    }
    public static void traceLine(int x1,int y1,int x2,int y2,Intc2 cons){
        int dx=Math.abs(x2-x1),sx=x1<x2?1:-1;
        int dy=-Math.abs(y2-y1),sy=y1<y2 ? 1 : -1;
        int err=dx+dy,x=x1,y=y1;
        while(true){
            cons.get(x,y);
            if(x==x2&&y==y2)break;
            int e2=2*err;
            if(e2>=dy){err+=dy;x+=sx;}
            if(e2<=dx){err+=dx;y+=sy;}
        }
    }
    public static void drawSteppedOutline(MapView view,int cx,int cy,float radius,BrushShape shape){
        maskCache.clear();
        int clamped=Math.max(1,(int)Math.ceil(radius));
        int w=Vars.editor.width(),h=Vars.editor.height();
        for(int rx=-clamped;rx<=clamped;rx++){
            for(int ry=-clamped;ry<=clamped;ry++){
                if(inside(rx,ry,radius,shape)){
                    int wx=cx+rx,wy=cy+ry;
                    if(wx>=0&&wy>=0&&wx<w&&wy<h)maskCache.add(Point2.pack(wx,wy));
                }
            }
        }
        drawMaskEdges(view,maskCache);
    }
    public static void drawLineSteppedOutline(MapView view,int x1,int y1,int x2,int y2,float radius,BrushShape shape){
        maskCache.clear();
        int clamped=Math.max(1,(int)Math.ceil(radius));
        int w=Vars.editor.width(),h=Vars.editor.height();
        traceLine(x1,y1,x2,y2,(lx,ly)->{
            for(int rx=-clamped;rx<=clamped;rx++){
                for(int ry=-clamped;ry<=clamped;ry++){
                    if(inside(rx,ry,radius,shape)){
                        int wx=lx+rx,wy=ly+ry;
                        if(wx>=0&&wy>=0&&wx<w&&wy<h)maskCache.add(Point2.pack(wx,wy));
                    }
                }
            }
        });
        drawMaskEdges(view,maskCache);
    }
    public static void drawMaskEdges(MapView view,IntSet mask){
        try{
            Float ox=(Float)Reflect.get(view,"offsetx");
            Float oy=(Float)Reflect.get(view,"offsety");
            Float zoom=(Float)Reflect.get(view,"zoom");
            if(ox==null||oy==null||zoom==null)return;
            float mapW=Vars.editor.width(),mapH=Vars.editor.height();
            if(mapW<=0||mapH<=0)return;
            float size=Math.min(view.getWidth(),view.getHeight());
            float scl=size/Math.max(mapW,mapH)*zoom;
            float cx=view.x+view.getWidth()/2f+ox*zoom;
            float cy=view.y+view.getHeight()/2f+oy*zoom;
            float mapBlX=cx-mapW*scl/2f;
            float mapBlY=cy-mapH*scl/2f;
            Draw.color(Pal.accent);
            Lines.stroke(Scl.scl(1.8f));
            mask.each(packed->{
                int x=Point2.x(packed),y=Point2.y(packed);
                float px=mapBlX+x*scl;
                float py=mapBlY+y*scl;
                if(!mask.contains(Point2.pack(x-1,y)))Lines.line(px,py,px,py+scl);
                if(!mask.contains(Point2.pack(x+1,y)))Lines.line(px+scl,py,px+scl,py+scl);
                if(!mask.contains(Point2.pack(x,y-1)))Lines.line(px,py,px+scl,py);
                if(!mask.contains(Point2.pack(x,y+1)))Lines.line(px,py+scl,px+scl,py+scl);
            });
            Draw.reset();
        }catch(Throwable ignored){}
    }
}
