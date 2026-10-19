package advancedredactor.editor;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import advancedredactor.editor.MUCore.*;
import java.lang.reflect.*;
public class MUDialogs{
    private static final ObjectSet<Dialog> hookedRules=new ObjectSet<>();
    private static final ObjectSet<Dialog> hookedResize=new ObjectSet<>();
    public static boolean renderPlanetInEditor=false;
    public static void initMods(){
        try{
            MapResizeDialog.minSize=1;
            MapResizeDialog.maxSize=3000;
        }catch(Throwable ignored){}
        Events.run(EventType.Trigger.update,()->{
            if(Core.scene==null)return;
            for(Element el:Core.scene.root.getChildren()){
                if(el instanceof CustomRulesDialog){
                    CustomRulesDialog crd=(CustomRulesDialog)el;
                    if(!hookedRules.contains(crd)){
                        hookedRules.add(crd);
                        injectRulesDialogMod(crd);
                    }
                }else if(el instanceof MapResizeDialog){
                    MapResizeDialog mrd=(MapResizeDialog)el;
                    if(!hookedResize.contains(mrd)){
                        hookedResize.add(mrd);
                        injectResizeDialogMod(mrd);
                    }
                }
            }
        });
    }
    private static void injectRulesDialogMod(CustomRulesDialog d){
        Time.runTask(2f,()->{
            try{
                Table main=(Table)MUReflect.get(d,"main");
                if(main==null)return;
                main.row();
                main.add("[accent]=== Mapping Utilities (Hidden Rules) ===[]").colspan(2).left().padTop(12f).padBottom(4f).row();
                main.button("@editor.mu.advrules",Icon.settings,Styles.flatt,MUDialogs::showAdvRulesDialog).size(260f,42f).left().pad(2f).row();
                main.button("@editor.mu.banned",Icon.cancel,Styles.flatt,MUDialogs::showBannedContentDialog).size(260f,42f).left().pad(2f).row();
                main.button("@editor.mu.planetbg",Icon.planet,Styles.flatt,MUDialogs::showPlanetBackgroundDialog).size(260f,42f).left().pad(2f).row();
            }catch(Throwable ignored){}
        });
    }
    private static void injectResizeDialogMod(MapResizeDialog d){
        Time.runTask(2f,()->{
            try{
                d.buttons.button("@editor.mu.resize",Icon.resize,()->{
                    d.hide();showResizeDialog();
                }).size(200f,54f);
            }catch(Throwable ignored){}
        });
    }
    public static void drawPlanetBackground(){
        if(!renderPlanetInEditor||Vars.state==null||Vars.state.rules.planetBackground==null)return;
        try{
            if(Vars.renderer!=null&&Vars.renderer.planets!=null){
                Vars.renderer.planets.render(Vars.state.rules.planetBackground);
            }
        }catch(Throwable ignored){}
    }
    public static void showAdvRulesDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.advrules");
        Table t=new Table();t.defaults().left().pad(3f);
        Rules r=Vars.state.rules;
        t.add("[accent]Core & Zone Rules[]").colspan(2).padTop(4f).row();
        t.check("Polygon Core Protection",r.polygonCoreProtection,b->r.polygonCoreProtection=b).colspan(2).row();
        t.check("Core Incinerates Items",r.coreIncinerates,b->r.coreIncinerates=b).colspan(2).row();
        t.check("Core Destroy Clear",r.coreDestroyClear,b->r.coreDestroyClear=b).colspan(2).row();
        t.check("Only Deposit Core",r.onlyDepositCore,b->r.onlyDepositCore=b).colspan(2).row();
        t.check("Disable Outside Area",r.disableOutsideArea,b->r.disableOutsideArea=b).colspan(2).row();
        t.check("Border Darkness",r.borderDarkness,b->r.borderDarkness=b).colspan(2).row();
        t.add("Enemy Core Build Radius:");t.field(String.valueOf(r.enemyCoreBuildRadius/8f),TextField.TextFieldFilter.floatsOnly,s->r.enemyCoreBuildRadius=Strings.parseFloat(s,r.enemyCoreBuildRadius/8f)*8f).width(140f).row();
        t.add("Drop Zone Radius:");t.field(String.valueOf(r.dropZoneRadius/8f),TextField.TextFieldFilter.floatsOnly,s->r.dropZoneRadius=Strings.parseFloat(s,r.dropZoneRadius/8f)*8f).width(140f).row();
        t.add("[accent]Combat & Unit Rules[]").colspan(2).padTop(8f).row();
        t.check("Unit Ammo Requirements",r.unitAmmo,b->r.unitAmmo=b).colspan(2).row();
        t.check("Unit Cap Variable",r.unitCapVariable,b->r.unitCapVariable=b).colspan(2).row();
        t.check("Logic Unit Build",r.logicUnitBuild,b->r.logicUnitBuild=b).colspan(2).row();
        t.check("Unit Payload Update",r.unitPayloadUpdate,b->r.unitPayloadUpdate=b).colspan(2).row();
        t.check("Possession Allowed",r.possessionAllowed,b->r.possessionAllowed=b).colspan(2).row();
        t.add("Unit Cap:");t.field(String.valueOf(r.unitCap),TextField.TextFieldFilter.digitsOnly,s->r.unitCap=Strings.parseInt(s,r.unitCap)).width(140f).row();
        t.add("[accent]World & Environment[]").colspan(2).padTop(8f).row();
        t.check("Reactor Explosions",r.reactorExplosions,b->r.reactorExplosions=b).colspan(2).row();
        t.check("Damage Explosions",r.damageExplosions,b->r.damageExplosions=b).colspan(2).row();
        t.check("Fire Enabled",r.fire,b->r.fire=b).colspan(2).row();
        t.check("Ghost Blocks",r.ghostBlocks,b->r.ghostBlocks=b).colspan(2).row();
        t.check("Hide Banned Blocks",r.hideBannedBlocks,b->r.hideBannedBlocks=b).colspan(2).row();
        t.check("Wait Wave Enemies",r.waitEnemies,b->r.waitEnemies=b).colspan(2).row();
        t.check("Show Spawns",r.showSpawns,b->r.showSpawns=b).colspan(2).row();
        t.check("Static Fog",r.staticFog,b->r.staticFog=b).colspan(2).row();
        t.add("Solar Multiplier:");t.field(String.valueOf(r.solarMultiplier),TextField.TextFieldFilter.floatsOnly,s->r.solarMultiplier=Strings.parseFloat(s,r.solarMultiplier)).width(140f).row();
        t.add("Drag Multiplier:");t.field(String.valueOf(r.dragMultiplier),TextField.TextFieldFilter.floatsOnly,s->r.dragMultiplier=Strings.parseFloat(s,r.dragMultiplier)).width(140f).row();
        t.add("Ambient Light (RRGGBBAA):");t.field(r.ambientLight.toString(),s->{try{r.ambientLight=Color.valueOf(s);}catch(Exception ignored){}}).width(140f).row();
        t.add("Static Color (RRGGBBAA):");t.field(r.staticColor.toString(),s->{try{r.staticColor=Color.valueOf(s);}catch(Exception ignored){}}).width(140f).row();
        t.add("Dynamic Color (RRGGBBAA):");t.field(r.dynamicColor.toString(),s->{try{r.dynamicColor=Color.valueOf(s);}catch(Exception ignored){}}).width(140f).row();
        t.add("[accent]All Internal Fields (Reflection)[]").colspan(2).padTop(8f).row();
        for(Field f:Rules.class.getFields()){
            if(Modifier.isStatic(f.getModifiers())||Modifier.isFinal(f.getModifiers()))continue;
            if(f.getType()==boolean.class){
                try{
                    boolean cur=f.getBoolean(r);
                    t.check("[lightgray]"+f.getName(),cur,v->{try{f.setBoolean(r,v);}catch(Exception ignored){}}).colspan(2).row();
                }catch(Exception ignored){}
            }else if(f.getType()==float.class){
                try{
                    float cur=f.getFloat(r);
                    t.add("[lightgray]"+f.getName()+":");
                    t.field(String.valueOf(cur),TextField.TextFieldFilter.floatsOnly,s->{try{f.setFloat(r,Strings.parseFloat(s,cur));}catch(Exception ignored){}}).width(140f).row();
                }catch(Exception ignored){}
            }
        }
        d.cont.pane(t).size(Math.min(Core.graphics.getWidth()*0.92f,520f),Math.min(Core.graphics.getHeight()*0.82f,540f));
        d.addCloseButton();d.show();
    }
    public static void showBannedContentDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.banned");
        final String[] query={""};
        final int[] tab={0};
        Table grid=new Table();
        Runnable rebuild=()->{
            grid.clearChildren();String q=query[0].toLowerCase().trim();int cols=0;
            if(tab[0]==0){
                for(Block b:Vars.content.blocks()){
                    if((!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q)))continue;
                    Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                        if(Vars.state.rules.bannedBlocks.contains(b))Vars.state.rules.bannedBlocks.remove(b);else Vars.state.rules.bannedBlocks.add(b);
                    }).size(40f).pad(2f).tooltip(b.localizedName+" ("+b.name+")").get();
                    btn.update(()->btn.setColor(Vars.state.rules.bannedBlocks.contains(b)?Color.scarlet:Color.white));
                    if(++cols%11==0)grid.row();
                }
            }else if(tab[0]==1){
                for(UnitType u:Vars.content.units()){
                    if(!q.isEmpty()&&!u.localizedName.toLowerCase().contains(q)&&!u.name.toLowerCase().contains(q))continue;
                    Button btn=grid.button(bt->bt.image(u.uiIcon).size(28f),Styles.flati,()->{
                        if(Vars.state.rules.bannedUnits.contains(u))Vars.state.rules.bannedUnits.remove(u);else Vars.state.rules.bannedUnits.add(u);
                    }).size(40f).pad(2f).tooltip(u.localizedName+" ("+u.name+")").get();
                    btn.update(()->btn.setColor(Vars.state.rules.bannedUnits.contains(u)?Color.scarlet:Color.white));
                    if(++cols%11==0)grid.row();
                }
            }else if(tab[0]==2){
                for(Block b:Vars.content.blocks()){
                    if(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q))continue;
                    Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                        if(Vars.state.rules.revealedBlocks.contains(b))Vars.state.rules.revealedBlocks.remove(b);else Vars.state.rules.revealedBlocks.add(b);
                    }).size(40f).pad(2f).tooltip("Reveal: "+b.localizedName).get();
                    btn.update(()->btn.setColor(Vars.state.rules.revealedBlocks.contains(b)?Color.lime:Color.gray));
                    if(++cols%11==0)grid.row();
                }
            }
        };
        d.cont.table(top->{
            top.button("Banned Blocks",Styles.flatt,()->{tab[0]=0;rebuild.run();}).size(140f,36f).pad(2f);
            top.button("Banned Units",Styles.flatt,()->{tab[0]=1;rebuild.run();}).size(140f,36f).pad(2f);
            top.button("Revealed Blocks",Styles.flatt,()->{tab[0]=2;rebuild.run();}).size(140f,36f).pad(2f);
        }).row();
        d.cont.table(act->{
            act.button("Ban All",Styles.flatt,()->{
                if(tab[0]==0)for(Block b:Vars.content.blocks())Vars.state.rules.bannedBlocks.add(b);
                else if(tab[0]==1)for(UnitType u:Vars.content.units())Vars.state.rules.bannedUnits.add(u);
            }).size(120f,32f).pad(2f);
            act.button("Clear All",Styles.flatt,()->{
                if(tab[0]==0)Vars.state.rules.bannedBlocks.clear();
                else if(tab[0]==1)Vars.state.rules.bannedUnits.clear();
                else Vars.state.rules.revealedBlocks.clear();
            }).size(120f,32f).pad(2f);
        }).row();
        d.cont.field("",t->{query[0]=t;rebuild.run();}).width(Math.min(Core.graphics.getWidth()*0.8f,420f)).padBottom(6f).row();
        rebuild.run();
        d.cont.pane(grid).size(Math.min(Core.graphics.getWidth()*0.92f,540f),Math.min(Core.graphics.getHeight()*0.7f,380f));
        d.addCloseButton();d.show();
    }
    public static void showPlanetBackgroundDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.planetbg");
        if(Vars.state.rules.planetBackground==null){
            Vars.state.rules.planetBackground=new PlanetParams();
            Vars.state.rules.planetBackground.planet=Planets.serpulo;
        }
        PlanetParams p=Vars.state.rules.planetBackground;
        Table t=new Table();t.defaults().left().pad(4f);
        t.check("Enable Planet Background (Saves to Map)",Vars.state.rules.planetBackground!=null,v->{
            Vars.state.rules.planetBackground=v?p:null;
            if(v)Vars.state.rules.dragMultiplier=1f;
        }).colspan(2).row();
        t.check("Live Preview in Editor",renderPlanetInEditor,v->renderPlanetInEditor=v).colspan(2).row();
        t.add("Planet:");
        t.button(b->b.label(()->p.planet==null?"None":p.planet.localizedName),Styles.flatt,()->{
            Seq<Planet> pl=Vars.content.planets();
            int idx=pl.indexOf(p.planet);
            p.planet=pl.get((idx+1)%pl.size);
        }).size(180f,36f).row();
        t.add("Zoom:");t.slider(0.05f,12f,0.05f,p.zoom,v->p.zoom=v).width(220f).row();
        t.add("Cam Pos X:");t.slider(-6f,6f,0.05f,p.camPos.x,v->p.camPos.x=v).width(220f).row();
        t.add("Cam Pos Y:");t.slider(-6f,6f,0.05f,p.camPos.y,v->p.camPos.y=v).width(220f).row();
        t.add("Cam Pos Z:");t.slider(0.05f,12f,0.05f,p.camPos.z,v->p.camPos.z=v).width(220f).row();
        d.cont.pane(t).size(Math.min(Core.graphics.getWidth()*0.9f,440f),360f);
        d.addCloseButton();d.show();
    }
    public static void showResizeDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.resize");
        final int[] nw={Vars.world.width()},nh={Vars.world.height()},ox={0},oy={0};
        Table t=new Table();t.defaults().pad(4f).left();
        t.add("Width (1-3000):");t.field(String.valueOf(nw[0]),TextField.TextFieldFilter.digitsOnly,s->nw[0]=Strings.parseInt(s,nw[0])).width(140f).row();
        t.add("Height (1-3000):");t.field(String.valueOf(nh[0]),TextField.TextFieldFilter.digitsOnly,s->nh[0]=Strings.parseInt(s,nh[0])).width(140f).row();
        t.add("Shift X:");t.field("0",s->ox[0]=Strings.parseInt(s,0)).width(140f).row();
        t.add("Shift Y:");t.field("0",s->oy[0]=Strings.parseInt(s,0)).width(140f).row();
        d.cont.add(t).row();
        d.buttons.button("@ok",Icon.ok,()->{
            int w=Math.max(1,Math.min(3000,nw[0])),h=Math.max(1,Math.min(3000,nh[0]));
            Vars.editor.resize(w,h,ox[0],oy[0]);
            MUEditorManager.selection=new ChunkedGridBits(w,h);
            MUEditorManager.history.clear();
            EditorOperationStack.refreshMap();
            d.hide();
        }).size(160f,50f);
        d.addCloseButton();d.show();
    }
    public static void showUIExplorerDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.uiexplorer");
        Table tree=new Table();tree.top().left();
        buildUITree(tree,Core.scene.root,0);
        d.cont.pane(tree).size(Math.min(Core.graphics.getWidth()*0.9f,620f),Math.min(Core.graphics.getHeight()*0.8f,450f));
        d.addCloseButton();d.show();
    }
    private static void buildUITree(Table t,Element e,int depth){
        StringBuilder sb=new StringBuilder();for(int i=0;i<depth;i++)sb.append("  ");
        String name=e.getClass().getSimpleName()+(e.name!=null?" ("+e.name+")":"")+" ["+Math.round(e.getWidth())+"x"+Math.round(e.getHeight())+"]";
        t.button(sb.toString()+name,Styles.flatt,()->{
            e.visible=!e.visible;
        }).growX().height(26f).left().row();
        if(e instanceof Group&&depth<7){
            for(Element c:((Group)e).getChildren())buildUITree(t,c,depth+1);
        }
    }
    public static void showJSConsoleDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.js");
        TextArea area=new TextArea("Vars.world.width()");
        Label out=new Label("");out.setWrap(true);
        d.cont.add(area).size(Math.min(Core.graphics.getWidth()*0.9f,500f),180f).pad(4f).row();
        d.cont.table(bt->{
            bt.button("Execute",Icon.play,()->out.setText(JSManager.run(area.getText()))).size(140f,40f).pad(4f);
            bt.button("Clear",Icon.trash,()->area.setText("")).size(140f,40f).pad(4f);
        }).row();
        d.cont.pane(p->p.add(out).growX().left()).size(Math.min(Core.graphics.getWidth()*0.9f,500f),140f);
        d.addCloseButton();d.show();
    }
                                                }
