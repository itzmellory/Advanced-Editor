package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
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
        if(Vars.ui==null||Vars.ui.hudGroup==null||Core.scene==null)return;
        if(button==null||button.parent!=Vars.ui.hudGroup){
            if(button!=null)button.remove();
            button=new Table();
            button.touchable=Touchable.childrenOnly;
            TextButton capture=button.button("",Styles.flatTogglet,MapScreenshotUtility::capture).size(42f,42f).get();
            capture.setChecked(true);
            capture.update(()->capture.setChecked(true));
            Vars.ui.hudGroup.addChild(button);
        }
        button.setPosition(Core.scene.marginLeft+200f,Core.graphics.getHeight()-Core.scene.marginTop-44f);
    }
}
