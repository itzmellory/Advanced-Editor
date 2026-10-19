package advancedredactor;
import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.mod.*;
import advancedredactor.generation.*;
import advancedredactor.ui.*;
import advancedredactor.editor.*;
public class AdvancedRedactorMod extends Mod{
    public AdvancedRedactorMod(){
        Events.on(ClientLoadEvent.class,e->{
            registerFilters();
            GeneratorUI.init();
            MUEditorManager.init();
        });
    }
    private void registerFilters(){
        try{
            Prov<GenerateFilter>[] current=Maps.allFilterTypes;
            Seq<Prov<GenerateFilter>> list=Seq.with(current);
            list.add(AdvancedTerrainFilter::new);
            list.add(AdvancedWaterFilter::new);
            list.add(AdvancedRuinFilter::new);
            Reflect.set(Maps.class,"allFilterTypes",list.toArray(Prov.class));
        }catch(Exception ignored){}
    }
    @Override public void loadContent(){}
}
