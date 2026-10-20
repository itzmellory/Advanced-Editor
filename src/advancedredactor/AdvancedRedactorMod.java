package advancedredactor;
import arc.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import advancedredactor.ui.*;
import advancedredactor.editor.*;
public class AdvancedRedactorMod extends Mod{
    public AdvancedRedactorMod(){
        Events.on(ClientLoadEvent.class,e->{
            GeneratorUI.init();
            LeftSideUi.init();
            EditorManager.init();
        });
    }
    @Override public void loadContent(){}
}
