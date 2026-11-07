package advancedredactor;
import arc.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import advancedredactor.ui.*;
import advancedredactor.LeftSideUtils.*;
import advancedredactor.upperuiutils.*;
import advancedredactor.utils.*;
public class AdvancedRedactorMod extends Mod{
    public AdvancedRedactorMod(){
        Events.on(ClientLoadEvent.class,e->{
            MapSizeUnlocker.init();
            GeneratorUI.init();
            LeftSideUi.init();
            StencilManager.init();
            LandCopyPast.init();
            UpperUi.init();
        });
    }
    @Override public void loadContent(){}
}
