package advancedredactor.ui;
import arc.func.*;
import arc.struct.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import advancedredactor.generation.*;
import java.lang.reflect.*;
public class GeneratorUI{
    @SuppressWarnings("unchecked")
    public static void init(){
        try{
            Field f=Maps.class.getDeclaredField("allFilterTypes");
            f.setAccessible(true);
            Object val=f.get(null);
            if(val instanceof Prov[]){
                Prov<GenerateFilter>[] orig=(Prov<GenerateFilter>[])val;
                Seq<Prov<GenerateFilter>> combined=Seq.with(orig);
                combined.add(AdvancedRotationFilter::new);
                combined.add(AdvancedRuinFilter::new);
                combined.add(AdvancedTerrainFilter::new);
                combined.add(AdvancedWaterFilter::new);
                try{
                    Field modField=Field.class.getDeclaredField("modifiers");
                    modField.setAccessible(true);
                    modField.setInt(f,f.getModifiers()&~Modifier.FINAL);
                }catch(Throwable ignored){}
                try{
                    Field accField=Field.class.getDeclaredField("accessFlags");
                    accField.setAccessible(true);
                    accField.setInt(f,f.getModifiers()&~Modifier.FINAL);
                }catch(Throwable ignored){}
                f.set(null,combined.toArray(Prov.class));
            }
        }catch(Throwable ignored){}
    }
}
