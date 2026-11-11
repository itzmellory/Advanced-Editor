package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.game.EventType.Trigger;
import mindustry.ui.*;
import advancedredactor.utils.MapScreenshotUtility;
public class MapScreenshotHud{
    private static Table button;
    public static void init(){
        Events.run(Trigger.update,MapScreenshotHud::update);
    }
    private static void update(){
        if(Vars.ui==null||Vars.ui.hudGroup==null||!Vars.state.isGame())return;
        Element buttonsEl=Vars.ui.hudGroup.find("buttons");
        if(buttonsEl instanceof Table){
            Table buttons=(Table)buttonsEl;
            if(buttons.find("ar-map-screenshot")==null){
                buttons.table(Styles.black6,t->{
                    t.name="ar-map-screenshot";
                    t.button("",Styles.cleart,MapScreenshotUtility::capture).size(46.4f);
                }).top().left();
            }
            return;
        }
        if(button==null||button.parent!=Vars.ui.hudGroup){
            if(button!=null)button.remove();
            button=new Table(Styles.black6);
            button.name="ar-map-screenshot-fallback";
            button.button("",Styles.cleart,MapScreenshotUtility::capture).size(46.4f);
            button.pack();
            Vars.ui.hudGroup.addChild(button);
        }
        button.visible=Vars.ui.hudfrag!=null&&Vars.ui.hudfrag.shown;
        button.setPosition(Scl.scl(232f),Core.graphics.getHeight()-Scl.scl(46.4f));
    }
}
