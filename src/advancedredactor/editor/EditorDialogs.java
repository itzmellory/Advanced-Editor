package advancedredactor.editor;
import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import advancedredactor.editor.EditorCore.*;
import java.lang.reflect.*;
public class EditorDialogs{
    private static final ObjectSet<Dialog> hookedRules=new ObjectSet<>();
    private static final ObjectSet<Dialog> hookedResize=new ObjectSet<>();
    private static final ObjectSet<Dialog> hookedInfo=new ObjectSet<>();
    public static boolean renderPlanetInEditor=false;
    public static void initMods(){
        try{
            MapResizeDialog.minSize=1;
            MapResizeDialog.maxSize=3000;
        }catch(Throwable ignored){}
        try{
            if(Vars.ui!=null&&Vars.ui.settings!=null&&Vars.ui.settings.game!=null){
                Vars.ui.settings.game.checkPref("mu-planet-bg-render",false,v->renderPlanetInEditor=v);
                renderPlanetInEditor=Core.settings.getBool("mu-planet-bg-render",false);
            }
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
                }else if(el instanceof MapInfoDialog){
                    MapInfoDialog mid=(MapInfoDialog)el;
                    if(!hookedInfo.contains(mid)){
                        hookedInfo.add(mid);
                        injectMapInfoDialogMod(mid);
                    }
                }
            }
        });
    }
    private static void injectRulesDialogMod(CustomRulesDialog d){
        Time.runTask(2f,()->{
            try{
                Table main=(Table)ReflectUtil.get(d,"main");
                Rules rules=(Rules)ReflectUtil.get(d,"rules");
                if(main==null||rules==null)return;
                main.row();
                main.add("[accent]=== Mapping Utilities ===[]").colspan(2).left().padTop(12f).padBottom(4f).row();
                main.button("@editor.mu.advrules",Icon.settings,Styles.flatt,()->showAdvRulesDialog(rules)).size(260f,42f).left().pad(2f).row();
                main.button("@editor.mu.banned",Icon.cancel,Styles.flatt,()->showBannedContentDialog(rules)).size(260f,42f).left().pad(2f).row();
                main.button("@editor.mu.planetbg",Icon.planet,Styles.flatt,()->showPlanetBackgroundDialog(rules)).size(260f,42f).left().pad(2f).row();
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
    private static void injectMapInfoDialogMod(MapInfoDialog d){
        Time.runTask(2f,()->{
            try{
                d.buttons.button("@editor.mu.advrules",Icon.settings,()->showAdvRulesDialog(Vars.state.rules)).size(200f,54f);
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
    private static void addRuleCheck(Table t,Rules r,String field,String label){
        if(!ReflectUtil.hasField(Rules.class,field))return;
        t.check(label,ReflectUtil.getBool(r,field,false),v->ReflectUtil.set(r,field,v)).colspan(2).left().row();
    }
    private static void addRuleFloat(Table t,Rules r,String field,String label,float scl){
        if(!ReflectUtil.hasField(Rules.class,field))return;
        t.add(label+": ").left();
        float cur=ReflectUtil.getFloat(r,field,0f)/scl;
        t.field(String.valueOf(cur),TextField.TextFieldFilter.floatsOnly,s->ReflectUtil.set(r,field,Strings.parseFloat(s,cur)*scl)).width(140f).left().row();
    }
    private static void addRuleInt(Table t,Rules r,String field,String label){
        if(!ReflectUtil.hasField(Rules.class,field))return;
        t.add(label+": ").left();
        int cur=ReflectUtil.getInt(r,field,0);
        t.field(String.valueOf(cur),TextField.TextFieldFilter.digitsOnly,s->ReflectUtil.set(r,field,Strings.parseInt(s,cur))).width(140f).left().row();
    }
    private static void addRuleColor(Table t,Rules r,String field,String label){
        Object colObj=ReflectUtil.get(r,field);
        if(!(colObj instanceof Color))return;
        Color col=(Color)colObj;
        t.add(label+" (RRGGBBAA): ").left();
        t.field(col.toString(),s->{try{ReflectUtil.set(r,field,Color.valueOf(s));}catch(Throwable ignored){}}).width(140f).left().row();
    }
    public static void showAdvRulesDialog(){showAdvRulesDialog(Vars.state.rules);}
    public static void showAdvRulesDialog(Rules r){
        BaseDialog d=new BaseDialog("@editor.mu.advrules");
        Table t=new Table();t.defaults().left().pad(3f);
        t.add("[accent]General & Core Rules[]").colspan(2).padTop(4f).row();
        addRuleCheck(t,r,"polygonCoreProtection","Polygon Core Protection");
        addRuleCheck(t,r,"coreIncinerates","Core Incinerates Items");
        addRuleCheck(t,r,"coreDestroyClear","Core Destroy Clear");
        addRuleCheck(t,r,"onlyDepositCore","Only Deposit Core");
        addRuleCheck(t,r,"disableOutsideArea","Disable Outside Area");
        addRuleCheck(t,r,"borderDarkness","Border Darkness");
        addRuleCheck(t,r,"cleanupDeadTeams","Cleanup Dead Teams");
        addRuleCheck(t,r,"placeRangeCheck","Place Range Check");
        addRuleFloat(t,r,"enemyCoreBuildRadius","Enemy Core Build Radius",8f);
        addRuleFloat(t,r,"dropZoneRadius","Drop Zone Radius",8f);
        t.add("[accent]Units & Combat[]").colspan(2).padTop(8f).row();
        addRuleCheck(t,r,"unitAmmo","Unit Ammo Requirements");
        addRuleCheck(t,r,"unitCapVariable","Unit Cap Variable");
        addRuleCheck(t,r,"logicUnitBuild","Logic Unit Build");
        addRuleCheck(t,r,"logicUnitControl","Logic Unit Control");
        addRuleCheck(t,r,"unitPayloadUpdate","Unit Payload Update");
        addRuleCheck(t,r,"possessionAllowed","Possession Allowed");
        addRuleCheck(t,r,"allowEditWorldProcessors","Allow Edit World Processors");
        addRuleInt(t,r,"unitCap","Base Unit Cap");
        addRuleFloat(t,r,"unitBuildSpeedMultiplier","Unit Build Speed Multiplier",1f);
        addRuleFloat(t,r,"unitCostMultiplier","Unit Cost Multiplier",1f);
        addRuleFloat(t,r,"unitDamageMultiplier","Unit Damage Multiplier",1f);
        addRuleFloat(t,r,"unitHealthMultiplier","Unit Health Multiplier",1f);
        addRuleFloat(t,r,"unitCrashDamageMultiplier","Unit Crash Damage Multiplier",1f);
        t.add("[accent]Environment & Waves[]").colspan(2).padTop(8f).row();
        addRuleCheck(t,r,"reactorExplosions","Reactor Explosions");
        addRuleCheck(t,r,"damageExplosions","Damage Explosions");
        addRuleCheck(t,r,"fire","Fire Enabled");
        addRuleCheck(t,r,"ghostBlocks","Ghost Blocks");
        addRuleCheck(t,r,"hideBannedBlocks","Hide Banned Blocks");
        addRuleCheck(t,r,"waitEnemies","Wait Wave Enemies");
        addRuleCheck(t,r,"showSpawns","Show Spawns");
        addRuleCheck(t,r,"randomWaveAI","Random Wave AI");
        addRuleCheck(t,r,"staticFog","Static Fog");
        addRuleCheck(t,r,"fog","Fog of War");
        addRuleCheck(t,r,"lighting","Ambient Lighting");
        addRuleFloat(t,r,"solarMultiplier","Solar Multiplier",1f);
        addRuleFloat(t,r,"dragMultiplier","Drag Multiplier",1f);
        addRuleColor(t,r,"ambientLight","Ambient Light");
        addRuleColor(t,r,"staticColor","Static Fog Color");
        addRuleColor(t,r,"dynamicColor","Dynamic Fog Color");
        t.add("[accent]All Rules Fields (Auto-Reflect)[]").colspan(2).padTop(8f).row();
        for(Field f:Rules.class.getFields()){
            if(Modifier.isStatic(f.getModifiers())||Modifier.isFinal(f.getModifiers()))continue;
            try{
                if(f.getType()==boolean.class){
                    boolean cur=f.getBoolean(r);
                    t.check("[lightgray]"+f.getName(),cur,v->{try{f.setBoolean(r,v);}catch(Throwable ignored){}}).colspan(2).left().row();
                }else if(f.getType()==float.class){
                    float cur=f.getFloat(r);
                    t.add("[lightgray]"+f.getName()+": ").left();
                    t.field(String.valueOf(cur),TextField.TextFieldFilter.floatsOnly,s->{try{f.setFloat(r,Strings.parseFloat(s,cur));}catch(Throwable ignored){}}).width(140f).left().row();
                }else if(f.getType()==int.class){
                    int cur=f.getInt(r);
                    t.add("[lightgray]"+f.getName()+": ").left();
                    t.field(String.valueOf(cur),TextField.TextFieldFilter.digitsOnly,s->{try{f.setInt(r,Strings.parseInt(s,cur));}catch(Throwable ignored){}}).width(140f).left().row();
                }
            }catch(Throwable ignored){}
        }
        d.cont.pane(t).size(Math.min(Core.graphics.getWidth()*0.92f,520f),Math.min(Core.graphics.getHeight()*0.82f,540f));
        d.addCloseButton();d.show();
    }
    public static void showBannedContentDialog(){showBannedContentDialog(Vars.state.rules);}
    public static void showBannedContentDialog(Rules rules){
        BaseDialog d=new BaseDialog("@editor.mu.banned");
        final String[] query={""};
        final int[] tab={0};
        Table grid=new Table();
        Runnable rebuild=()->{
            grid.clearChildren();String q=query[0].toLowerCase().trim();int cols=0;
            if(tab[0]==0){
                for(Block b:Vars.content.blocks()){
                    if(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q))continue;
                    Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                        if(rules.bannedBlocks.contains(b))rules.bannedBlocks.remove(b);else rules.bannedBlocks.add(b);
                    }).size(40f).pad(2f).tooltip(b.localizedName+" ("+b.name+")").get();
                    btn.update(()->btn.setColor(rules.bannedBlocks.contains(b)?Color.scarlet:Color.white));
                    if(++cols%11==0)grid.row();
                }
            }else if(tab[0]==1){
                for(UnitType u:Vars.content.units()){
                    if(!q.isEmpty()&&!u.localizedName.toLowerCase().contains(q)&&!u.name.toLowerCase().contains(q))continue;
                    Button btn=grid.button(bt->bt.image(u.uiIcon).size(28f),Styles.flati,()->{
                        if(rules.bannedUnits.contains(u))rules.bannedUnits.remove(u);else rules.bannedUnits.add(u);
                    }).size(40f).pad(2f).tooltip(u.localizedName+" ("+u.name+")").get();
                    btn.update(()->btn.setColor(rules.bannedUnits.contains(u)?Color.scarlet:Color.white));
                    if(++cols%11==0)grid.row();
                }
            }else if(tab[0]==2){
                for(Block b:Vars.content.blocks()){
                    if(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q))continue;
                    Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                        if(rules.revealedBlocks.contains(b))rules.revealedBlocks.remove(b);else rules.revealedBlocks.add(b);
                    }).size(40f).pad(2f).tooltip("Reveal: "+b.localizedName).get();
                    btn.update(()->btn.setColor(rules.revealedBlocks.contains(b)?Color.lime:Color.gray));
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
            act.button("Select All",Styles.flatt,()->{
                if(tab[0]==0)for(Block b:Vars.content.blocks())rules.bannedBlocks.add(b);
                else if(tab[0]==1)for(UnitType u:Vars.content.units())rules.bannedUnits.add(u);
                else for(Block b:Vars.content.blocks())rules.revealedBlocks.add(b);
            }).size(110f,32f).pad(2f);
            act.button("Clear All",Styles.flatt,()->{
                if(tab[0]==0)rules.bannedBlocks.clear();
                else if(tab[0]==1)rules.bannedUnits.clear();
                else rules.revealedBlocks.clear();
            }).size(110f,32f).pad(2f);
            act.button("Invert",Styles.flatt,()->{
                if(tab[0]==0)for(Block b:Vars.content.blocks()){if(rules.bannedBlocks.contains(b))rules.bannedBlocks.remove(b);else rules.bannedBlocks.add(b);}
                else if(tab[0]==1)for(UnitType u:Vars.content.units()){if(rules.bannedUnits.contains(u))rules.bannedUnits.remove(u);else rules.bannedUnits.add(u);}
                else for(Block b:Vars.content.blocks()){if(rules.revealedBlocks.contains(b))rules.revealedBlocks.remove(b);else rules.revealedBlocks.add(b);}
            }).size(110f,32f).pad(2f);
        }).row();
        d.cont.field("",t->{query[0]=t;rebuild.run();}).width(Math.min(Core.graphics.getWidth()*0.8f,420f)).padBottom(6f).row();
        rebuild.run();
        d.cont.pane(grid).size(Math.min(Core.graphics.getWidth()*0.92f,540f),Math.min(Core.graphics.getHeight()*0.7f,380f));
        d.addCloseButton();d.show();
    }
    public static void showPlanetBackgroundDialog(){showPlanetBackgroundDialog(Vars.state.rules);}
    public static void showPlanetBackgroundDialog(Rules rules){
        BaseDialog d=new BaseDialog("@editor.mu.planetbg");
        if(rules.planetBackground==null){
            rules.planetBackground=new PlanetParams();
            rules.planetBackground.planet=Planets.serpulo;
        }
        PlanetParams p=rules.planetBackground;
        Table t=new Table();t.defaults().left().pad(4f);
        t.check("Enable Planet Background",rules.planetBackground!=null,v->{
            rules.planetBackground=v?p:null;
        }).colspan(2).row();
        t.check("Render Planet in Editor",renderPlanetInEditor,v->{
            renderPlanetInEditor=v;Core.settings.put("mu-planet-bg-render",v);
        }).colspan(2).row();
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
            EditorManager.selection=new ChunkedGridBits(w,h);
            EditorManager.history.clear();
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
