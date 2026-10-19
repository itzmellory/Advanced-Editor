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
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import advancedredactor.editor.MUCore.*;
public class MUDialogs{
    public static void showAdvRulesDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.advrules");
        Table t=new Table();t.defaults().left().pad(2f);
        Rules r=Vars.state.rules;
        t.check("Polygon Core Protection",r.polygonCoreProtection,b->r.polygonCoreProtection=b).row();
        t.add("Enemy Core Build Radius:");t.field(String.valueOf(r.enemyCoreBuildRadius),TextField.TextFieldFilter.floatsOnly,s->r.enemyCoreBuildRadius=Strings.parseFloat(s,r.enemyCoreBuildRadius)).row();
        t.check("Core Incinerates Items",r.coreIncinerates,b->r.coreIncinerates=b).row();
        t.check("Hide Banned Blocks",r.hideBannedBlocks,b->r.hideBannedBlocks=b).row();
        t.check("Disable Outside Area",r.disableOutsideArea,b->r.disableOutsideArea=b).row();
        t.check("Ghost Blocks",r.ghostBlocks,b->r.ghostBlocks=b).row();
        t.check("Reactor Explosions",r.reactorExplosions,b->r.reactorExplosions=b).row();
        t.check("Logic Unit Build",r.logicUnitBuild,b->r.logicUnitBuild=b).row();
        t.check("Fire Enabled",r.fire,b->r.fire=b).row();
        t.check("Damage Explosions",r.damageExplosions,b->r.damageExplosions=b).row();
        t.check("Unit Ammo Requirements",r.unitAmmo,b->r.unitAmmo=b).row();
        t.check("Only Deposit Core",r.onlyDepositCore,b->r.onlyDepositCore=b).row();
        t.add("Drop Zone Radius:");t.field(String.valueOf(r.dropZoneRadius),TextField.TextFieldFilter.floatsOnly,s->r.dropZoneRadius=Strings.parseFloat(s,r.dropZoneRadius)).row();
        t.add("Ambient Light (Hex):");t.field(r.ambientLight.toString(),s->{try{r.ambientLight=Color.valueOf(s);}catch(Exception ignored){}}).row();
        d.cont.pane(t).size(Math.min(Core.graphics.getWidth()*0.9f,400f),Math.min(Core.graphics.getHeight()*0.8f,500f));
        d.addCloseButton();d.show();
    }
    public static void showBannedContentDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.banned");
        final String[] query={""};Table grid=new Table();
        Runnable rebuild=()->{
            grid.clearChildren();String q=query[0].toLowerCase();int cols=0;
            for(Block b:Vars.content.blocks()){
                if(!b.canBeBuilt()||(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q)))continue;
                Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                    if(Vars.state.rules.bannedBlocks.contains(b))Vars.state.rules.bannedBlocks.remove(b);else Vars.state.rules.bannedBlocks.add(b);
                }).size(40f).pad(2f).get();
                btn.update(()->btn.setColor(Vars.state.rules.bannedBlocks.contains(b)?Color.scarlet:Color.white));
                if(++cols%12==0)grid.row();
            }
            grid.row();cols=0;
            for(UnitType u:Vars.content.units()){
                if(u.internal||(!q.isEmpty()&&!u.localizedName.toLowerCase().contains(q)&&!u.name.toLowerCase().contains(q)))continue;
                Button btn=grid.button(bt->bt.image(u.uiIcon).size(28f),Styles.flati,()->{
                    if(Vars.state.rules.bannedUnits.contains(u))Vars.state.rules.bannedUnits.remove(u);else Vars.state.rules.bannedUnits.add(u);
                }).size(40f).pad(2f).get();
                btn.update(()->btn.setColor(Vars.state.rules.bannedUnits.contains(u)?Color.scarlet:Color.white));
                if(++cols%12==0)grid.row();
            }
        };
        d.cont.field("",t->{query[0]=t;rebuild.run();}).width(Math.min(Core.graphics.getWidth()*0.8f,400f)).padBottom(8f).row();
        rebuild.run();
        d.cont.pane(grid).size(Math.min(Core.graphics.getWidth()*0.9f,560f),Math.min(Core.graphics.getHeight()*0.8f,400f));
        d.addCloseButton();d.show();
    }
    public static void showPlanetBackgroundDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.planetbg");
        PlanetParams p=Vars.state.rules.planetBackground!=null?Vars.state.rules.planetBackground:new PlanetParams();
        d.cont.check("Enable Planet Background",Vars.state.rules.planetBackground!=null,v->{Vars.state.rules.planetBackground=v?p:null;}).colspan(2).left().pad(4f).row();
        d.cont.add("Planet:").left();
        d.cont.button(b->b.label(()->p.planet==null?"None":p.planet.localizedName),Styles.flatt,()->{
            Seq<Planet> pl=Vars.content.planets().select(x->x.visible);int idx=pl.indexOf(p.planet);p.planet=pl.get((idx+1)%pl.size);
        }).size(180f,36f).pad(4f).row();
        d.cont.add("Zoom:").left();d.cont.slider(0.1f,10f,0.1f,p.zoom,v->p.zoom=v).width(220f).pad(4f).row();
        d.cont.add("Cam Pos X:").left();d.cont.slider(-4f,4f,0.1f,p.camPos.x,v->p.camPos.x=v).width(220f).pad(4f).row();
        d.cont.add("Cam Pos Y:").left();d.cont.slider(-4f,4f,0.1f,p.camPos.y,v->p.camPos.y=v).width(220f).pad(4f).row();
        d.cont.add("Cam Pos Z:").left();d.cont.slider(0.1f,10f,0.1f,p.camPos.z,v->p.camPos.z=v).width(220f).pad(4f).row();
        d.addCloseButton();d.show();
    }
    public static void showResizeDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.resize");
        final int[] nw={Vars.world.width()},nh={Vars.world.height()},ox={0},oy={0};
        d.cont.add("Width:").left();d.cont.field(String.valueOf(nw[0]),TextField.TextFieldFilter.digitsOnly,s->nw[0]=Strings.parseInt(s,nw[0])).width(140f).pad(4f).row();
        d.cont.add("Height:").left();d.cont.field(String.valueOf(nh[0]),TextField.TextFieldFilter.digitsOnly,s->nh[0]=Strings.parseInt(s,nh[0])).width(140f).pad(4f).row();
        d.cont.add("Offset X:").left();d.cont.field("0",s->ox[0]=Strings.parseInt(s,0)).width(140f).pad(4f).row();
        d.cont.add("Offset Y:").left();d.cont.field("0",s->oy[0]=Strings.parseInt(s,0)).width(140f).pad(4f).row();
        d.buttons.button("@ok",Icon.ok,()->{
            int w=Math.max(10,Math.min(2000,nw[0])),h=Math.max(10,Math.min(2000,nh[0]));
            Seq<TileData> old=new Seq<>();
            for(int x=0;x<Vars.world.width();x++)for(int y=0;y<Vars.world.height();y++){Tile t=Vars.world.tile(x,y);if(t!=null)old.add(TileData.fromSafe(t));}
            Vars.editor.resize(w,h,0,0);
            for(TileData td:old){Tile t=Vars.world.tile(td.x+ox[0],td.y+oy[0]);if(t!=null)td.applyTo(t);}
            MUEditorManager.selection=new ChunkedGridBits(w,h);MUEditorManager.history.clear();EditorOperationStack.refreshMap();
            d.hide();
        }).size(160f,50f);
        d.addCloseButton();d.show();
    }
    public static void showUIExplorerDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.uiexplorer");
        Table tree=new Table();tree.top().left();
        buildUITree(tree,Core.scene.root,0);
        d.cont.pane(tree).size(620f,450f);
        d.addCloseButton();d.show();
    }
    private static void buildUITree(Table t,Element e,int depth){
        String pad=new String(new char[depth]).replace("\0","  ");
        String name=e.getClass().getSimpleName()+(e.name!=null?" ("+e.name+")":"")+" ["+Math.round(e.getWidth())+"x"+Math.round(e.getHeight())+"]";
        t.add(pad+name).left().row();
        if(e instanceof Group&&depth<6){
            for(Element c:((Group)e).getChildren())buildUITree(t,c,depth+1);
        }
    }
    public static void showJSConsoleDialog(){
        BaseDialog d=new BaseDialog("@editor.mu.js");
        TextArea area=new TextArea("Vars.world.width()");
        Label out=new Label("");out.setWrap(true);
        d.cont.add(area).size(Math.min(Core.graphics.getWidth()*0.9f,500f),180f).pad(4f).row();
        d.cont.button("Execute",Icon.play,()->{
            try{out.setText(String.valueOf(Vars.mods.getScripts().runConsole(area.getText())));}
            catch(Throwable ex){out.setText("[scarlet]"+ex.toString());}
        }).size(160f,40f).pad(4f).row();
        d.cont.pane(p->p.add(out).growX().left()).size(Math.min(Core.graphics.getWidth()*0.9f,500f),140f);
        d.addCloseButton();d.show();
    }
}
