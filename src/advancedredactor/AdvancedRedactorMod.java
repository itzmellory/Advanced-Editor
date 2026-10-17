package advancedredactor;

import arc.Events;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

public class AdvancedRedactorMod extends Mod {

    public AdvancedRedactorMod() {
        Events.on(ClientLoadEvent.class, e -> {
            
        });
    }

    @Override
    public void loadContent() {
    }
}
