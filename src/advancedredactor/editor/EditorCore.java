package advancedredactor.editor;
import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
public class EditorCore{
    @Retention(RetentionPolicy.RUNTIME)public @interface NoCopy{}
    @Retention(RetentionPolicy.RUNTIME)public @interface RequireScl{}
    public static class ReflectUtil{
        public static Object get(Object obj,String name){
            if(obj==null)return null;
            try{Field f=findField(obj.getClass(),name);if(f!=null){f.setAccessible(true);return f.get(obj);}}catch(Throwable ignored){}
            return null;
        }
        public static void set(Object obj,String name,Object val){
            if(obj==null)return;
            try{Field f=findField(obj.getClass(),name);if(f!=null){f.setAccessible(true);f.set(obj,val);}}catch(Throwable ignored){}
        }
        public static boolean hasField(Class<?> c,String name){return findField(c,name)!=null;}
        public static boolean getBool(Object obj,String name,boolean def){
            Object v=get(obj,name);return v instanceof Boolean?(Boolean)v:def;
        }
        public static float getFloat(Object obj,String name,float def){
            Object v=get(obj,name);return v instanceof Number?((Number)v).floatValue():def;
        }
        public static int getInt(Object obj,String name,int def){
            Object v=get(obj,name);return v instanceof Number?((Number)v).intValue():def;
        }
        public static Object invoke(Object obj,String name){
            if(obj==null)return null;
            try{Method m=findMethod(obj.getClass(),name);if(m!=null){m.setAccessible(true);return m.invoke(obj);}}catch(Throwable ignored){}
            return null;
        }
        public static Field findField(Class<?> c,String name){
            while(c!=null){try{return c.getDeclaredField(name);}catch(Throwable e){c=c.getSuperclass();}}return null;
        }
        public static Method findMethod(Class<?> c,String name){
            while(c!=null){
                for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==0)return m;
                c=c.getSuperclass();
            }return null;
        }
        public static void copyFields(Object from,Object to){
            if(from==null||to==null)return;
            Class<?> c=from.getClass();
            while(c!=null&&c!=Object.class){
                for(Field f:c.getDeclaredFields()){
                    if(Modifier.isStatic(f.getModifiers())||f.isAnnotationPresent(NoCopy.class))continue;
                    try{f.setAccessible(true);Field tf=findField(to.getClass(),f.getName());if(tf!=null&&!Modifier.isFinal(tf.getModifiers())){tf.setAccessible(true);tf.set(to,f.get(from));}}catch(Throwable ignored){}
                }
                c=c.getSuperclass();
            }
        }
    }
    public static class FilesUtil{
        public static Fi dir(){Fi f=Core.settings.getDataDirectory().child("mapping-utilities");f.mkdirs();return f;}
        public static Fi scriptsDir(){Fi f=dir().child("scripts");f.mkdirs();return f;}
    }
    public static class JsonUtil{
        public static final Json json=new Json();
        public static String toJson(Object o){try{return json.toJson(o);}catch(Throwable e){return "{}";}}
        public static <T> T fromJson(Class<T> t,String s){try{return json.fromJson(t,s);}catch(Throwable e){return null;}}
    }
    public static class JSManager{
        public static final Seq<String> history=new Seq<>();
        public static String run(String script){
            if(script==null||script.trim().isEmpty())return "";
            history.add(script);
            try{return String.valueOf(Vars.mods.getScripts().runConsole(script));}catch(Throwable t){return "[scarlet]"+t.toString();}
        }
    }
    public static class ChunkedGridBits{
        public final int width,height,chunksW,chunksH;
        private final long[][] chunks;
        private int count=0;
        public ChunkedGridBits(int w,int h){
            width=Math.max(1,w);height=Math.max(1,h);
            chunksW=(width+7)>>3;chunksH=(height+7)>>3;
            chunks=new long[chunksW][chunksH];
        }
        public boolean inBounds(int x,int y){return x>=0&&y>=0&&x<width&&y<height;}
        public boolean get(int x,int y){
            if(!inBounds(x,y))return false;
            int cx=x>>3,cy=y>>3,bit=((x&7)<<3)|(y&7);
            return (chunks[cx][cy]&(1L<<bit))!=0;
        }
        public void set(int x,int y,boolean b){
            if(!inBounds(x,y))return;
            int cx=x>>3,cy=y>>3,bit=((x&7)<<3)|(y&7);
            long mask=1L<<bit,old=chunks[cx][cy];
            if(b){if((old&mask)==0){chunks[cx][cy]=old|mask;count++;}}
            else{if((old&mask)!=0){chunks[cx][cy]=old&~mask;count--;}}
        }
        public void set(int x,int y){set(x,y,true);}
        public void clear(int x,int y){set(x,y,false);}
        public void setArea(int x1,int y1,int x2,int y2,boolean b){
            int minX=Math.max(0,Math.min(x1,x2)),maxX=Math.min(width-1,Math.max(x1,x2));
            int minY=Math.max(0,Math.min(y1,y2)),maxY=Math.min(height-1,Math.max(y1,y2));
            for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)set(x,y,b);
        }
        public void clearAll(){
            for(int i=0;i<chunksW;i++)for(int j=0;j<chunksH;j++)chunks[i][j]=0L;
            count=0;
        }
        public boolean isEmpty(){return count<=0;}
        public int size(){return count;}
        public ChunkedGridBits copy(){
            ChunkedGridBits c=new ChunkedGridBits(width,height);
            for(int i=0;i<chunksW;i++)System.arraycopy(chunks[i],0,c.chunks[i],0,chunksH);
            c.count=count;return c;
        }
        public void each(Intc2 cons){
            if(count<=0)return;
            for(int cx=0;cx<chunksW;cx++)for(int cy=0;cy<chunksH;cy++){
                long word=chunks[cx][cy];if(word==0L)continue;
                int bx=cx<<3,by=cy<<3;
                for(int bit=0;bit<64;bit++){
                    if((word&(1L<<bit))!=0){
                        int x=bx+(bit>>3),y=by+(bit&7);
                        if(x<width&&y<height)cons.get(x,y);
                    }
                }
            }
        }
    }
    public static class TileData{
        public int x,y;public Block floor,block,overlay;public short rotation,team;public byte data;public Object config;
        public TileData(int x,int y,Block f,Block b,Block o,short r,short t,byte d,Object cfg){
            this.x=x;this.y=y;floor=f;block=b;overlay=o;rotation=r;team=t;data=d;config=cfg;
        }
        public static TileData fromSafe(Tile t){
            if(t==null)return new TileData(0,0,Blocks.air,Blocks.air,Blocks.air,(short)0,(short)0,(byte)0,null);
            boolean center=t.build!=null&&t.build.tile==t;
            Block blk=t.block();
            if(t.build!=null&&!center)blk=Blocks.air;
            return new TileData(t.x,t.y,t.floor(),blk,t.overlay(),(short)(t.build!=null?t.build.rotation:0),(short)t.getTeamID(),t.data,center?t.build.config():null);
        }
        public void applyTo(Tile t){
            if(t==null)return;
            if(floor instanceof Floor)t.setFloor((Floor)floor);
            t.setOverlay(overlay!=null?overlay:Blocks.air);
            if(block!=null){
                if(block==Blocks.cliff){t.setBlock(Blocks.cliff);t.data=data;}
                else if(block!=Blocks.air||t.build==null||t.build.tile==t){
                    t.setBlock(block,Team.get(team),rotation);
                    t.data=data;
                    if(t.build!=null&&config!=null)t.build.configureAny(config);
                }
            }
        }
    }
    public interface EditorOperation{void undo();void redo();}
    public static class EditorOperationStack{
        private final Seq<EditorOperation> stack=new Seq<>();
        private int index=-1;
        public int maxSize=120;
        public void push(EditorOperation op){
            if(op==null)return;
            if(index<stack.size-1)stack.truncate(index+1);
            stack.add(op);index++;
            if(stack.size>maxSize){stack.remove(0);index--;}
        }
        public void undo(){if(canUndo()){stack.get(index).undo();index--;refreshMap();}}
        public void redo(){if(canRedo()){index++;stack.get(index).redo();refreshMap();}}
        public boolean canUndo(){return index>=0&&index<stack.size;}
        public boolean canRedo(){return index<stack.size-1;}
        public void clear(){stack.clear();index=-1;}
        public static void refreshMap(){
            try{if(Vars.editor!=null&&Vars.editor.renderer!=null)Vars.editor.renderer.recache();}catch(Throwable ignored){}
            try{
                if(Vars.renderer!=null&&Vars.renderer.blocks!=null&&Vars.renderer.blocks.floor!=null){
                    ReflectUtil.invoke(Vars.renderer.blocks.floor,"clearTiles");
                    ReflectUtil.invoke(Vars.renderer.blocks.floor,"reload");
                }
            }catch(Throwable ignored){}
        }
    }
    public static class BlocksSelectionOperation implements EditorOperation{
        public final ChunkedGridBits before,after;
        public BlocksSelectionOperation(ChunkedGridBits b,ChunkedGridBits a){before=b.copy();after=a.copy();}
        @Override public void undo(){EditorManager.selection=before.copy();}
        @Override public void redo(){EditorManager.selection=after.copy();}
    }
    public static class BlocksTilesOperation implements EditorOperation{
        public static class TilesState{
            public final Seq<TileData> tiles=new Seq<>();
            public void apply(){
                for(TileData td:tiles)if(td.block==Blocks.air){Tile t=Vars.world.tile(td.x,td.y);if(t!=null)t.setBlock(Blocks.air);}
                for(TileData td:tiles){Tile t=Vars.world.tile(td.x,td.y);if(t!=null)td.applyTo(t);}
            }
        }
        public final IntMap<TileData> captured=new IntMap<>();
        public final TilesState before=new TilesState(),after=new TilesState();
        public void captureBefore(int x,int y){
            Tile t=Vars.world.tile(x,y);
            if(t==null)return;
            int k=x+y*Vars.world.width();
            if(!captured.containsKey(k))captured.put(k,TileData.fromSafe(t));
        }
        public void commit(){
            if(captured.isEmpty())return;
            for(IntMap.Entry<TileData> e:captured.entries()){
                before.tiles.add(e.value);
                Tile t=Vars.world.tile(e.value.x,e.value.y);
                if(t!=null)after.tiles.add(TileData.fromSafe(t));
            }
            captured.clear();
            EditorManager.history.push(this);
            EditorOperationStack.refreshMap();
        }
        @Override public void undo(){before.apply();}
        @Override public void redo(){after.apply();}
    }
    public static class UIObjectData{
        public String name;public Element element;
        public UIObjectData(String n,Element e){name=n;element=e;}
    }
    public static class ButtonData extends UIObjectData{
        public Drawable icon;public Runnable action;public Boolp checked;
        public ButtonData(String n,Drawable i,Runnable a,Boolp c){super(n,null);icon=i;action=a;checked=c;}
    }
    public static class CellData{
        public Cell<?> cell;public Element element;
        public CellData(Cell<?> c){cell=c;element=c!=null?c.get():null;}
    }
    public static class TableData extends UIObjectData{
        public final Seq<CellData> cells=new Seq<>();
        public TableData(String n,Table t){super(n,t);if(t!=null)for(Cell<?> c:t.getCells())cells.add(new CellData(c));}
    }
    public static class WindowData extends UIObjectData{
        public float x,y,width,height;public boolean minimized,closable=true;
        public WindowData(String n,float w,float h){super(n,null);width=w;height=h;}
    }
    public static class Window extends Table{
        public final WindowData data;
        public final Table titleBar=new Table(),body=new Table();
        private float dragX,dragY;
        public Window(String title,float w,float h){
            super(Styles.black6);
            data=new WindowData(title,w,h);
            top().left();
            titleBar.background(Tex.whiteui);titleBar.setColor(Pal.darkerGray);
            titleBar.margin(4f);
            titleBar.add(title).style(Styles.outlineLabel).color(Pal.accent).growX().left().padLeft(4f);
            titleBar.button(b->b.label(()->data.minimized?"+":"-"),Styles.flatt,()->{
                data.minimized=!data.minimized;rebuildWindow();
            }).size(26f,26f).padRight(2f);
            if(data.closable){
                titleBar.button(Icon.cancelSmall,Styles.flati,this::remove).size(26f,26f);
            }
            titleBar.addListener(new InputListener(){
                @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,KeyCode button){dragX=x;dragY=y;toFront();return true;}
                @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                    setPosition(Mathf.clamp(Window.this.x+x-dragX,0,Core.graphics.getWidth()-getWidth()),Mathf.clamp(Window.this.y+y-dragY,0,Core.graphics.getHeight()-getHeight()));
                }
            });
            rebuildWindow();
        }
        public void rebuildWindow(){
            clearChildren();
            add(titleBar).growX().height(32f).row();
            if(!data.minimized)add(body).grow().pad(4f);
            pack();
        }
    }
                  }
