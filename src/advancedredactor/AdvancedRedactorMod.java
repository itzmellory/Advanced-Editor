package advancedredactor;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;

public class AdvancedRedactorMod extends Mod {

    public AdvancedRedactorMod() {
        Events.on(ClientLoadEvent.class, e -> {
            if (!Core.settings.getBool("vi-redactor-warning-shown", false)) {
                Time.runTask(10f, () -> {
                    BaseDialog dialog = new BaseDialog("Warning");
                    dialog.cont.add("Данный мод является собственностью клана VI.").wrap().width(300f).center().pad(10f);
                    dialog.cont.row();
                    
                    dialog.cont.button("Принять", () -> {
                        Core.settings.put("vi-redactor-warning-shown", true);
                        Core.settings.forceSave();
                        dialog.hide();
                    }).size(150f, 50f).pad(10f);
                    
                    dialog.show();
                });
            }
        });
    }

    @Override
    public void loadContent() {
    }
}
