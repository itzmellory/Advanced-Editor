package advancedredactor;
import arc.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import advancedredactor.ui.*;
public class AdvancedRedactorMod extends Mod{
    public AdvancedRedactorMod(){
        Events.on(ClientLoadEvent.class,e->{
            GeneratorUI.init();
            LeftSideUi.init();
        });
    }
    @Override public void loadContent(){}
}
