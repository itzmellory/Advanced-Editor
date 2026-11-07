package advancedredactor;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.maps.filters.GenerateFilter;
import mindustry.mod.Mod;
import advancedredactor.editor.EditorManager;
import advancedredactor.generation.AdvancedRotationFilter;
import advancedredactor.generation.AdvancedRuinFilter;
import advancedredactor.generation.AdvancedTerrainFilter;
import advancedredactor.generation.AdvancedWaterFilter;
import advancedredactor.ui.GeneratorUI;
import advancedredactor.ui.LeftSideUi;
import advancedredactor.ui.UpperUi;
import advancedredactor.utils.MapSizeUnlocker;
import arc.func.Prov;
import arc.struct.Seq;
public class AdvancedRedactorMod extends Mod {
    public AdvancedRedactorMod() {
        Events.on(ClientLoadEvent.class, e -> {
            MapSizeUnlocker.unlock();
            registerFilters();
            EditorManager.init();
            GeneratorUI.init();
            LeftSideUi.init();
            UpperUi.init();
        });
    }
    @SuppressWarnings("unchecked")
    private void registerFilters() {
        try {
            var field = Vars.maps.getClass().getDeclaredField("allFilterTypes");
            field.setAccessible(true);
            var filters = (Seq<Prov<? extends GenerateFilter>>) field.get(Vars.maps);
            filters.add(AdvancedTerrainFilter::new);
            filters.add(AdvancedWaterFilter::new);
            filters.add(AdvancedRuinFilter::new);
            filters.add(AdvancedRotationFilter::new);
        } catch (Exception ignored) {}
    }
}
