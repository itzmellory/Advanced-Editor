package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import advancedredactor.utils.MapScreenshotUtility;
public class MapScreenshotHud{
    public static void init(){
        Events.on(ClientLoadEvent.class,e->setupUI());
        Events.on(WorldLoadEvent.class,e->setupUI());
        Events.run(Trigger.update,MapScreenshotHud::setupUI);
    }
    private static void setupUI(){
        if(Core.scene==null)return;
        Element el=Core.scene.find("mobile buttons");
        if(!(el instanceof Table))el=Core.scene.find("buttons");
        if(el instanceof Table){
            Table select=(Table)el;
            if(select.find("ar-map-screenshot")==null){
                select.image().color(Pal.gray).width(2f).padLeft(2f).padRight(2f).fillY();
                TextButton btn=select.button("",Styles.flatTogglet,MapScreenshotUtility::capture).name("ar-map-screenshot").size(46f).get();
                btn.update(()->btn.setChecked(true));
                select.invalidateHierarchy();
                select.pack();
            }
        }
    }
}
