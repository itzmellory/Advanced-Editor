package advancedredactor.ui;
import arc.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.game.EventType.Trigger;
import mindustry.gen.*;
import mindustry.ui.*;
import advancedredactor.utils.MapScreenshotUtility;
public class MapScreenshotHud{
    private static Table container;
    private static Table btnBox;
    private static final Vec2 tmp=new Vec2();
    public static void init(){
        Events.run(Trigger.update,MapScreenshotHud::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.hudGroup==null)return;
        if(container==null||container.parent!=Vars.ui.hudGroup){
            if(container!=null)container.remove();
            container=new Table();
            container.setFillParent(true);
            container.top().left();
            container.touchable=Touchable.childrenOnly;
            btnBox=new Table(Tex.buttonEdge3);
            btnBox.button("",Styles.cleart,MapScreenshotUtility::capture).size(46f,46f);
            btnBox.pack();
            container.addChild(btnBox);
            Vars.ui.hudGroup.addChild(container);
        }
        boolean show=Vars.state.isGame()&&(Vars.ui.hudfrag==null||Vars.ui.hudfrag.shown);
        container.visible=show;
        if(!show)return;
        Element wavesEditor=Vars.ui.hudGroup.find("waves/editor");
        Element buttons=wavesEditor instanceof Group?((Group)wavesEditor).find("buttons"):null;
        if(buttons!=null&&buttons.visible){
            tmp.set(buttons.getWidth(),0f);
            buttons.localToStageCoordinates(tmp);
            container.stageToLocalCoordinates(tmp);
            btnBox.setSize(Scl.scl(48f),buttons.getHeight()>10f?buttons.getHeight():Scl.scl(48f));
            btnBox.setPosition(tmp.x,tmp.y);
        }else{
            float w=Scl.scl(48f);
            btnBox.setSize(w,w);
            btnBox.setPosition(Scl.scl(240f),Core.graphics.getHeight()-w);
        }
        container.toFront();
    }
}
