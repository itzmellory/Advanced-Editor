package advancedredactor.LeftSideUtils;
import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import java.lang.reflect.*;
public class FogOfWarManager{
    public static boolean enabled=false;
    public static Team team=Team.sharded;
    public static float alpha=0.65f;
    private static final IntSet visibleTiles=new IntSet();
    private static Element overlayElement;
    private static Method projectMethod;
    static{
        try{
            projectMethod=MapView.class.getDeclaredMethod("project",float.class,float.class);
            projectMethod.setAccessible(true);
        }catch(Throwable ignored){}
    }
    public static void attachOverlay(){
        if(Vars.ui==null||Vars.ui.editor==null)return;
        MapView view=Vars.ui.editor.getView();
        if(view==null||view.parent==null)return;
        if(overlayElement!=null&&overlayElement.parent==view.parent)return;
        if(overlayElement!=null)overlayElement.remove();
        overlayElement=new Element(){
            {
                setFillParent(true);
                touchable=Touchable.disabled;
            }
            @Override
            public void draw(){
                if(!enabled||Vars.editor==null)return;
                Vec2 p0=project(0,0);
                Vec2 p1=project(1,1);
                if(p0==null||p1==null)return;
                float unit=p1.x-p0.x;
                int w=Vars.editor.width();
                int h=Vars.editor.height();
                Draw.color(0f,0f,0f,alpha);
                for(int x=0;x<w;x++){
                    for(int y=0;y<h;y++){
                        if(!visibleTiles.contains(Point2.pack(x,y))){
                            Fill.rect(p0.x+(x+0.5f)*unit,p0.y+(y+0.5f)*unit,unit,unit);
                        }
                    }
                }
                Draw.reset();
            }
        };
        int idx=view.parent.getChildren().indexOf(view,true);
        view.parent.addChildAt(idx+1,overlayElement);
    }
    private static Vec2 project(float tx,float ty){
        try{
            return (Vec2)projectMethod.invoke(Vars.ui.editor.getView(),tx,ty);
        }catch(Throwable e){
            return null;
        }
    }
    public static void rebuild(){
        visibleTiles.clear();
        if(!enabled||Vars.editor==null)return;
        int w=Vars.editor.width();
        int h=Vars.editor.height();
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                Tile t=Vars.editor.tile(x,y);
                if(t!=null&&t.block()!=Blocks.air&&t.team()==team&&t.isCenter()){
                    float rad=t.block().fogRadius>0?t.block().fogRadius:(t.block() instanceof CoreBlock?45f:18f);
                    int irad=(int)Math.ceil(rad);
                    for(int dx=-irad;dx<=irad;dx++){
                        for(int dy=-irad;dy<=irad;dy++){
                            if(dx*dx+dy*dy<=rad*rad){
                                int nx=x+dx,ny=y+dy;
                                if(nx>=0&&ny>=0&&nx<w&&ny<h){
                                    visibleTiles.add(Point2.pack(nx,ny));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public static void showDialog(){
        attachOverlay();
        BaseDialog d=new BaseDialog(Core.bundle.get("ar.fog.title","Fog of War Previewer"));
        d.addCloseButton();
        Table cont=d.cont;
        cont.defaults().width(360f).pad(4f);
        Table box=new Table(Tex.button);
        box.margin(10f);
        box.check(Core.bundle.get("ar.fog.enable","Enable Fog of War Preview"),enabled,b->{
            enabled=b;
            rebuild();
        }).left().growX().row();
        Table teams=new Table();
        teams.add(Core.bundle.get("ar.fog.team","Team:")).padRight(6f);
        for(Team t:new Team[]{Team.sharded,Team.crux,Team.malis,Team.green,Team.blue}){
            teams.button(b->b.add("[#"+t.color.toString()+"]"+t.name),Styles.flatToggleMenut,()->{
                team=t;
                rebuild();
            }).checked(b->team==t).size(62f,32f).pad(2f);
        }
        box.add(teams).left().padTop(6f).row();
        Table alphaTbl=new Table();
        Label aLbl=new Label(Core.bundle.format("ar.fog.opacity",(int)(alpha*100)));
        Slider aSlider=new Slider(20f,95f,5f,false);
        aSlider.setValue(alpha*100f);
        aSlider.moved(v->{
            alpha=v/100f;
            aLbl.setText(Core.bundle.format("ar.fog.opacity",(int)v));
        });
        alphaTbl.add(aLbl).left().row();
        alphaTbl.add(aSlider).growX().padTop(4f);
        box.add(alphaTbl).growX().padTop(6f).row();
        box.button(Core.bundle.get("ar.fog.refresh","Recalculate Visibility"),Icon.refreshSmall,FogOfWarManager::rebuild).growX().height(38f).padTop(8f);
        cont.add(box);
        d.show();
    }
}
