package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.util.Time;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import java.lang.reflect.Method;

public class MapArtDialog extends BaseDialog{
    public enum ArtStyle{TERRAIN,SORTER,CANVAS}
    public enum LayerMode{FLOORS_AND_WALLS,FLOORS_ONLY,WALLS_ONLY}
    private Fi selectedFile;
    private int origWidth=0,origHeight=0;
    private int targetWidth=100,targetHeight=100;
    private boolean keepAspect=true;
    private boolean clearBlocks=true;
    private ArtStyle style=ArtStyle.TERRAIN;
    private LayerMode layerMode=LayerMode.FLOORS_AND_WALLS;
    private Label fileLabel;
    private TextField widthField,heightField;
    private TextButton generateBtn,placeOnMapBtn;
    private Table dynamicOptionsTable;

    public MapArtDialog(){
        super(Core.bundle.get("ar.art.title","Map Art Generator"));
        addCloseButton();
        setupUI();
    }

    private void setupUI(){
        cont.clear();
        Table main=new Table();
        main.defaults().pad(4f);
        
        Table fileTable=new Table(Tex.button);
        fileLabel=new Label(Core.bundle.get("ar.art.nofile","No image selected"));
        fileTable.add(fileLabel).padRight(10f);
        fileTable.button(Core.bundle.get("ar.art.choose","Choose Image"),Icon.imageSmall,this::chooseImage).height(40f).padLeft(8f);
        main.add(fileTable).growX().row();
        
        Table styleTable=new Table(Tex.button);
        styleTable.add(Core.bundle.get("ar.art.style","Art Type:")).left().row();
        ButtonGroup<TextButton> styleGroup=new ButtonGroup<>();
        Table styleBtns=new Table();
        styleBtns.defaults().growX().height(38f).pad(2f);
        addStyleBtn(styleBtns,styleGroup,Core.bundle.get("ar.art.style.terrain","Terrain"),ArtStyle.TERRAIN);
        addStyleBtn(styleBtns,styleGroup,Core.bundle.get("ar.art.style.sorter","Sorters"),ArtStyle.SORTER);
        addStyleBtn(styleBtns,styleGroup,Core.bundle.get("ar.art.style.canvas","Canvas"),ArtStyle.CANVAS);
        styleTable.add(styleBtns).growX();
        main.add(styleTable).growX().row();
        
        Table sizeTable=new Table(Tex.button);
        sizeTable.add(Core.bundle.get("ar.art.dimensions","Target Size (in tiles):")).colspan(4).left().padBottom(4f).row();
        widthField=new TextField(String.valueOf(targetWidth));
        heightField=new TextField(String.valueOf(targetHeight));
        widthField.setFilter(TextField.TextFieldFilter.digitsOnly);
        heightField.setFilter(TextField.TextFieldFilter.digitsOnly);
        
        widthField.changed(()->{
            if(widthField.getText().isEmpty())return;
            try{
                targetWidth=Math.max(1,Integer.parseInt(widthField.getText()));
                if(keepAspect&&origWidth>0&&origHeight>0){
                    targetHeight=Math.max(1,Math.round(targetWidth*((float)origHeight/origWidth)));
                    heightField.setText(String.valueOf(targetHeight));
                }
            }catch(NumberFormatException ignored){}
        });
        heightField.changed(()->{
            if(heightField.getText().isEmpty())return;
            try{
                targetHeight=Math.max(1,Integer.parseInt(heightField.getText()));
                if(keepAspect&&origWidth>0&&origHeight>0){
                    targetWidth=Math.max(1,Math.round(targetHeight*((float)origWidth/origHeight)));
                    widthField.setText(String.valueOf(targetWidth));
                }
            }catch(NumberFormatException ignored){}
        });
        
        sizeTable.add(Core.bundle.get("ar.art.width","Width:")).right().padRight(4f);
        sizeTable.add(widthField).width(90f).padRight(10f);
        sizeTable.add(Core.bundle.get("ar.art.height","Height:")).right().padRight(4f);
        sizeTable.add(heightField).width(90f).row();
        
        Table quickSizeBtns=new Table();
        quickSizeBtns.defaults().height(34f).pad(2f).growX();
        quickSizeBtns.button(Core.bundle.get("ar.art.fitmap","Fit to Map"),()->{
            targetWidth=Vars.editor.width();
            targetHeight=Vars.editor.height();
            if(keepAspect&&origWidth>0&&origHeight>0){
                float scale=Math.min((float)Vars.editor.width()/origWidth,(float)Vars.editor.height()/origHeight);
                targetWidth=Math.max(1,Math.round(origWidth*scale));
                targetHeight=Math.max(1,Math.round(origHeight*scale));
            }
            widthField.setText(String.valueOf(targetWidth));
            heightField.setText(String.valueOf(targetHeight));
        });
        quickSizeBtns.button(Core.bundle.get("ar.art.origsize","1:1 Original"),()->{
            if(origWidth>0&&origHeight>0){
                targetWidth=Math.min(origWidth,Vars.editor.width());
                targetHeight=Math.min(origHeight,Vars.editor.height());
                widthField.setText(String.valueOf(targetWidth));
                heightField.setText(String.valueOf(targetHeight));
            }
        });
        sizeTable.add(quickSizeBtns).colspan(4).growX().padTop(4f).row();
        sizeTable.check(Core.bundle.get("ar.art.keepaspect","Keep Aspect Ratio"),keepAspect,b->keepAspect=b).colspan(4).left().padTop(4f);
        main.add(sizeTable).growX().row();
        
        dynamicOptionsTable=new Table(Tex.button);
        rebuildDynamicOptions();
        main.add(dynamicOptionsTable).growX().row();
        
        Table actions=new Table();
        actions.defaults().growX().height(46f).pad(3f);
        generateBtn=actions.button(Core.bundle.get("ar.art.generate","Generate Full/Centered"),Icon.ok,this::runGeneration).get();
        placeOnMapBtn=actions.button(Core.bundle.get("ar.art.placemode","Place & Drag on Map"),Icon.move,this::runInteractivePlace).get();
        generateBtn.setDisabled(()->selectedFile==null);
        placeOnMapBtn.setDisabled(()->selectedFile==null);
        main.add(actions).growX().padTop(6f);
        
        ScrollPane pane=new ScrollPane(main);
        cont.add(pane).maxWidth(520f);
    }

    private void addStyleBtn(Table parent,ButtonGroup<TextButton> group,String text,ArtStyle s){
        TextButton btn=new TextButton(text,Styles.togglet);
        btn.clicked(()->{
            style=s;
            rebuildDynamicOptions();
        });
        if(style==s)btn.setChecked(true);
        group.add(btn);
        parent.add(btn);
    }

    private void rebuildDynamicOptions(){
        dynamicOptionsTable.clear();
        dynamicOptionsTable.defaults().left().pad(2f);
        if(style==ArtStyle.TERRAIN){
            dynamicOptionsTable.add(Core.bundle.get("ar.art.layers","Blocks to use:")).row();
            ButtonGroup<CheckBox> grp=new ButtonGroup<>();
            addLayerCheck(dynamicOptionsTable,grp,Core.bundle.get("ar.art.layer.both","Floors + Walls (Best Colors)"),LayerMode.FLOORS_AND_WALLS);
            addLayerCheck(dynamicOptionsTable,grp,Core.bundle.get("ar.art.layer.floors","Floors Only (Walkable)"),LayerMode.FLOORS_ONLY);
            addLayerCheck(dynamicOptionsTable,grp,Core.bundle.get("ar.art.layer.walls","Walls Only"),LayerMode.WALLS_ONLY);
            dynamicOptionsTable.check(Core.bundle.get("ar.art.clearblocks","Clear existing blocks in area"),clearBlocks,b->clearBlocks=b).padTop(4f).row();
        }else{
            dynamicOptionsTable.check(Core.bundle.get("ar.art.clearfloors","Place on clean floor"),clearBlocks,b->clearBlocks=b).row();
        }
    }

    private void addLayerCheck(Table parent,ButtonGroup<CheckBox> group,String text,LayerMode m){
        CheckBox box=new CheckBox(text);
        box.clicked(()->layerMode=m);
        if(layerMode==m)box.setChecked(true);
        group.add(box);
        parent.add(box).row();
    }

    private void pickPNG(arc.func.Cons<Fi> cons){
        try{
            Class<?> fc=null;
            try{fc=Class.forName("mindustry.ui.FileChooser");}catch(Throwable ignored){}
            if(fc==null)fc=Class.forName("mindustry.ui.dialogs.FileChooser");
            Method op=null;
            for(Method m:fc.getMethods())if(m.getName().equals("open")){op=m;break;}
            if(op!=null){
                Object fci=op.getParameterTypes()[0]==String.class?op.invoke(null,"png"):op.invoke(null,(Object)new String[]{"png"});
                for(Method m:fci.getClass().getMethods())if(m.getName().equals("submit")){m.invoke(fci,cons);return;}
            }
        }catch(Throwable ignored){}
        try{
            for(Method m:mindustry.core.Platform.class.getMethods()){
                if(m.getName().equals("showFileChooser")){
                    if(m.getParameterTypes().length==3){m.invoke(Vars.platform,true,"png",cons);return;}
                    if(m.getParameterTypes().length==4){m.invoke(Vars.platform,true,"Select PNG","png",cons);return;}
                }
            }
        }catch(Throwable ignored){}
    }

    private void chooseImage(){
        pickPNG(file->Core.app.post(()->{
            try{
                Pixmap pix=new Pixmap(file);
                origWidth=pix.width;
                origHeight=pix.height;
                pix.dispose();
                selectedFile=file;
                fileLabel.setText(file.name()+" ("+origWidth+"x"+origHeight+")");
                
                float scale=Math.min((float)Vars.editor.width()/origWidth,(float)Vars.editor.height()/origHeight);
                scale=Math.min(scale,1f);
                targetWidth=Math.max(1,Math.round(origWidth*scale));
                targetHeight=Math.max(1,Math.round(origHeight*scale));
                
                widthField.setText(String.valueOf(targetWidth));
                heightField.setText(String.valueOf(targetHeight));
            }catch(Throwable e){
                Vars.ui.showErrorMessage(Core.bundle.get("ar.art.invalidimage","Failed to load image: ")+e.getMessage());
            }
        }));
    }

    private void runGeneration(){
        if(selectedFile==null)return;
        hide();
        Time.runTask(2f,()->{
            try{
                MapArtGenerator.generate(selectedFile,targetWidth,targetHeight,style,layerMode,clearBlocks);
            }catch(Throwable e){
                Vars.ui.showException(e);
            }
        });
    }

    private void runInteractivePlace(){
        if(selectedFile==null)return;
        hide();
        Time.runTask(2f,()->ArtPlaceOverlay.startPlacement(selectedFile,targetWidth,targetHeight,style,layerMode,clearBlocks));
    }
}
