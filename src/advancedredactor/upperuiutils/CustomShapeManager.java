package advancedredactor.upperuiutils;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
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
    private static final Vec2 tmpA=new Vec2(),tmpB=new Vec2();
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
        int adx=Math.abs(dx),ady=Math.abs(dy);
        if(adx>ady*2.414f){
            y2=y1;
        }else if(ady>adx*2.414f){
            x2=x1;
        }else{
            int d=Math.max(adx,ady);
            x2=x1+d*(dx>=0?1:-1);
            y2=y1+d*(dy>=0?1:-1);
        }
        return new Point2(x2,y2);
    }
    public static void traceLine(int x1,int y1,int x2,int y2,Intc2 cons){
        int dx=Math.abs(x2-x1),sx=x1<x2?1:-1;
        int dy=-Math.abs(y2-y1),sy=y1<y2?1:-1;
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
        drawSteppedOutline(view,LandCopyPast.getOverlayElement(),cx,cy,radius,shape);
    }
    public static void drawSteppedOutline(MapView view,Element overlay,int cx,int cy,float radius,BrushShape shape){
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
        drawMaskEdges(view,overlay,maskCache);
    }
    public static void drawLineSteppedOutline(MapView view,int x1,int y1,int x2,int y2,float radius,BrushShape shape){
        drawLineSteppedOutline(view,LandCopyPast.getOverlayElement(),x1,y1,x2,y2,radius,shape);
    }
    public static void drawLineSteppedOutline(MapView view,Element overlay,int x1,int y1,int x2,int y2,float radius,BrushShape shape){
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
        drawMaskEdges(view,overlay,maskCache);
    }
    public static void drawMaskEdges(MapView view,IntSet mask){
        drawMaskEdges(view,LandCopyPast.getOverlayElement(),mask);
    }
    public static void drawMaskEdges(MapView view,Element overlay,IntSet mask){
        try{
            Vec2 o0=LandCopyPast.tileToOverlay(view,overlay,0f,0f,tmpA);
            Vec2 o1=LandCopyPast.tileToOverlay(view,overlay,1f,1f,tmpB);
            float x0=o0.x,y0=o0.y;
            float tileW=o1.x-x0,tileH=o1.y-y0;
            if(Math.abs(tileW)<=0.0001f||Math.abs(tileH)<=0.0001f)return;
            Draw.color(Pal.accent);
            Lines.stroke(Scl.scl(1.8f));
            mask.each(packed->{
                int x=Point2.x(packed),y=Point2.y(packed);
                float px=x0+x*tileW;
                float py=y0+y*tileH;
                if(!mask.contains(Point2.pack(x-1,y)))Lines.line(px,py,px,py+tileH);
                if(!mask.contains(Point2.pack(x+1,y)))Lines.line(px+tileW,py,px+tileW,py+tileH);
                if(!mask.contains(Point2.pack(x,y-1)))Lines.line(px,py,px+tileW,py);
                if(!mask.contains(Point2.pack(x,y+1)))Lines.line(px,py+tileH,px+tileW,py+tileH);
            });
            Draw.reset();
        }catch(Throwable ignored){}
    }
}
