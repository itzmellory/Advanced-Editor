package advancedredactor.utils;
import arc.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import java.lang.reflect.*;
public class MapSizeUnlocker{
    public static void init(){
        unlockStaticBounds();
        Events.run(Trigger.update,MapSizeUnlocker::unlockDialogs);
    }
    private static void unlockStaticBounds(){
        try{
            Field minF=MapEditorDialog.class.getDeclaredField("minSize");
            Field maxF=MapEditorDialog.class.getDeclaredField("maxSize");
            setFinalStaticInt(minF,1);
            setFinalStaticInt(maxF,Integer.MAX_VALUE);
        }catch(Throwable ignored){}
    }
    private static void unlockDialogs(){
        if(Vars.ui==null||Vars.ui.editor==null)return;
        try{
            Field rF=MapEditorDialog.class.getDeclaredField("resizeDialog");
            rF.setAccessible(true);
            Object rd=rF.get(Vars.ui.editor);
            if(rd!=null){
                Class<?> c=rd.getClass();
                for(String name:new String[]{"minSize","maxSize"}){
                    try{
                        Field f=c.getDeclaredField(name);
                        f.setAccessible(true);
                        f.setInt(rd,name.equals("minSize")?1:Integer.MAX_VALUE);
                    }catch(Throwable ignored){}
                }
            }
        }catch(Throwable ignored){}
    }
    private static void setFinalStaticInt(Field field,int newValue){
        try{
            field.setAccessible(true);
            try{
                Field modField=Field.class.getDeclaredField("modifiers");
                modField.setAccessible(true);
                modField.setInt(field,field.getModifiers()&~Modifier.FINAL);
            }catch(Throwable ignored){}
            try{
                Field accField=Field.class.getDeclaredField("accessFlags");
                accField.setAccessible(true);
                accField.setInt(field,field.getModifiers()&~Modifier.FINAL);
            }catch(Throwable ignored){}
            field.setInt(null,newValue);
        }catch(Throwable ignored){}
    }
}
