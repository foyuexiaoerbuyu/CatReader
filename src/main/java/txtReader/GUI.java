package txtReader;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


/*
* 摸鱼用小说阅读器
* @author:capterlliar
* @version:1.0
* 结构：
* background(pane)->hbox(0.9)->pane(0.9/0.5)->page(pane)
*                            ->catalog(0.03)
*                            ->listview(0.47)
*                 ->slider(0.05)
*
*
*/

public class GUI extends Application {
    Chapter_GUI chapter;
    Book book;
    Pane background;
    HBox hBox;
    Pane pane;
    Stage stage;
    Slider slider;
    Pane catalog;
    ListView<Label> listView;
    double prefsize=500;
    double back_height=500;
    double back_width=500;
    double stage_X;
    double stage_Y;
    //以下数值为比例
    double pane_width1=0.97;
    double pane_width2=0.5;
    double catalog_width=0.03;
    double listview_width=0.47;

    String background_color="251,251,251,";
    double background_opacity=0.5;
    int page_num=0;//第几页
    int chapter_num=0;//第几章
    int has_chapter;//是否有章节标题
    int has_catalog=0;//当前是否展示目录
    int is_loading=0;
    String name;
    String path;
    //String test="/src/main";
    String test="";
    String cssfile="file:/"+System.getProperty("user.dir")+test+"/resources"+"/GUI.css";
    String tempfile=System.getProperty("user.dir")+test+"/resources"+"/lastview";
    void warning(String string){
        Label label=new Label(string);
        label.getStylesheets().add(cssfile);
        label.getStyleClass().add("content");
        pane.getChildren().add(label);
        Timeline t = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(label.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(4), new KeyValue(label.opacityProperty(), 0))
        );
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }
    void turnPage() {
        if (chapter==null)
            return;
        if (page_num < 0) {
            chapter_num--;
            if (chapter_num < 0) {
                warning("没有前一章啦");
                chapter_num=0;
            }
            else {
                getText(false);
                pane.getChildren().clear();
                pane.getChildren().add(chapter.getPage(page_num));
            }
        } else if (page_num >= chapter.getPage_num()) {
            chapter_num++;
            if (chapter_num >= book.getChapter_num()) {
                warning("没有后续啦");
                chapter_num--;
            }
            else {
                getText(true);
                pane.getChildren().clear();
                pane.getChildren().add(chapter.getPage(page_num));
            }
        } else {
            pane.getChildren().clear();
            pane.getChildren().add(chapter.getPage(page_num));
        }
        System.gc();
    }
    void getPane(){
        pane=new Pane();
        pane.setStyle("-fx-background-color:rgba(244,244,244,0)");
        pane.prefHeightProperty().bind(hBox.heightProperty());
        pane.prefWidthProperty().bind(hBox.widthProperty().multiply(pane_width1));
        pane.setOnMouseClicked(e->{
            if (is_loading==1)
                return;
            if(stage_X==stage.getX()&&stage_Y==stage.getY()) {
                double x = e.getSceneX();
                if (x < pane.getWidth() / 2)
                    page_num--;
                if (x >= pane.getWidth() / 2)
                    page_num++;
                turnPage();
            }
            else {
                stage_X = stage.getX();
                stage_Y = stage.getY();
            }
            e.consume();
        });
    }
    void gethBox(){
        hBox= new HBox(10);
        hBox.setStyle("-fx-background-color:rgba(244,244,244,0)");
        hBox.layoutXProperty().bind(background.widthProperty().multiply(5).divide(100));
        hBox.layoutYProperty().bind(background.heightProperty().multiply(5).divide(100));
        //tips:hbox不接受layoutXProperty
        hBox.prefHeightProperty().bind(background.heightProperty().multiply(0.9));
        hBox.prefWidthProperty().bind(background.widthProperty().multiply(0.9));
    }
    void getBackground(){
        background=new Pane();
        background.setStyle("-fx-background-color:rgba("+background_color+background_opacity+")");
        background.setPrefHeight(prefsize);
        background.setPrefWidth(prefsize);
        background.setOnMouseReleased(mouseEvent -> {
            //根据窗口大小判断是否改变文字还是翻页
            if(back_width!=background.getWidth()||back_height!=background.getHeight()) {
                back_width=background.getWidth();
                back_height=background.getHeight();
                if (chapter==null||is_loading==1)
                    return;
                chapter.setSize(pane.getWidth(), pane.getHeight());
                page_num=Math.min(page_num,chapter.getPage_num()-1);
                pane.getChildren().clear();
                pane.getChildren().add(chapter.getPage(page_num));
                mouseEvent.consume();
            }
        });
    }

    Button getCloseButton(){
        Button button=new Button("x");
        button.getStylesheets().add(cssfile);
        button.getStyleClass().add("button");
        button.prefHeightProperty().bind(background.heightProperty().multiply(0.05));
        button.prefWidthProperty().bind(background.widthProperty().multiply(0.1));
        button.layoutXProperty().bind(background.widthProperty().multiply(0.9));
        button.setOnMouseClicked(e->{
            saveTemp();
            stage.close();
            e.consume();
        });
        return button;
    }
    Button getFileButton(){
        Button button=new Button("打开文件");
        button.getStylesheets().add(cssfile);
        button.getStyleClass().add("button");
        button.prefHeightProperty().bind(background.heightProperty().multiply(0.05));
        button.prefWidthProperty().bind(background.widthProperty().multiply(0.15));
        button.layoutXProperty().bind(background.widthProperty().multiply(0.75));
        button.setOnMouseClicked(e->{
            if (is_loading==1)
                return;
            FileChooser fileChooser=new FileChooser();
            File selected=fileChooser.showOpenDialog(stage);
            if (selected!=null) {
                name = selected.getName();
                path = selected.getPath();
                chapter_num = 0;
                page_num = 0;
                book=null;
                chapter=null;
                if(has_catalog==1)
                    closeCatalog();
                listView=null;
                initText();
                e.consume();
            }
        });
        return button;
    }
    Button getHidenButton(){
        Button button=new Button("缩小！");
        button.getStylesheets().add(cssfile);
        button.getStyleClass().add("button");
        button.prefHeightProperty().bind(background.heightProperty().multiply(0.05));
        button.prefWidthProperty().bind(background.widthProperty().multiply(0.15));
        button.layoutXProperty().bind(background.widthProperty().multiply(0.6));
        button.setOnMouseClicked(e->{
            ((Stage)((Button)e.getSource()).getScene().getWindow()).setIconified(true);
            e.consume();
        });
        return button;
    }

    void getSlider(){
        slider=new Slider(0.1,1,0.5);
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.prefHeightProperty().bind(background.heightProperty().multiply(0.02));
        slider.prefWidthProperty().bind(background.widthProperty().multiply(0.2));
        slider.layoutYProperty().bind(background.heightProperty().multiply(0.96));
        slider.layoutXProperty().bind(background.widthProperty().multiply(0.7));
        slider.valueProperty().addListener((ov,oldval,newval)->{
            background.setStyle("-fx-background-color:rgba("+background_color+newval+")");
        });
        background.getChildren().add(slider);
    }
    void getListView() {
        if(book==null)
            return;
        ObservableList<Label> list = FXCollections.observableArrayList();
        for (int i = 0; i < book.getChapter_num(); i++) {
            list.add(new Label(book.getChapterName(i)));
        }
        listView=null;
        listView = new ListView<>();
        listView.setItems(list);
        listView.prefHeightProperty().bind(hBox.heightProperty());
        listView.prefWidthProperty().bind(hBox.widthProperty().multiply(listview_width));
        listView.getStyleClass().add("list");
        listView.getStylesheets().add(cssfile);
        listView.setFixedCellSize(20);
        listView.getSelectionModel().selectedIndexProperty().addListener((ov, oldval, newval) -> {
            if (is_loading==1)
                return;
            if (oldval.equals(-1))
                return;
            pane.getChildren().clear();
            chapter = new Chapter_GUI(book.getChapter(newval.intValue()), pane.getWidth(),
                    pane.getHeight(), 14.5, has_chapter);
            pane.getChildren().add(chapter.getPage(0));
            page_num=0;
            chapter_num=newval.intValue();
            System.gc();
        });
    }
    void closeCatalog(){//
        hBox.getChildren().remove(1);
        pane.prefWidthProperty().bind(hBox.widthProperty().multiply(pane_width1));
        if (chapter==null) {
            has_catalog=0;
            return;
        }
        chapter.setSize(pane.getPrefWidth(), pane.getHeight());
        pane.getChildren().clear();
        page_num=Math.min(page_num,chapter.getPage_num()-1);
        pane.getChildren().add(chapter.getPage(page_num));
        has_catalog=0;
    }
    void getCatalog(){
        catalog=new Pane();
        catalog.getStyleClass().add("catalog");
        catalog.getStylesheets().add(cssfile);
        catalog.prefHeightProperty().bind(hBox.heightProperty());
        catalog.prefWidthProperty().bind(hBox.widthProperty().multiply(catalog_width));
        catalog.setOnMouseClicked(e->{
            if(has_catalog==0) {
                if (is_loading==1)
                    return;
                if(listView==null)
                    getListView();
                if(chapter!=null) {
                    pane.prefWidthProperty().bind(hBox.widthProperty().multiply(pane_width2));
                    chapter.setSize(pane.getPrefWidth(), pane.getHeight());
                    pane.getChildren().clear();
                    page_num = Math.min(page_num, chapter.getPage_num() - 1);
                    pane.getChildren().add(chapter.getPage(page_num));
                    hBox.getChildren().add(1, listView);
                    has_catalog = 1;
                    listView.getSelectionModel().select(chapter_num);
                    listView.scrollTo(chapter_num);
                }
            }
            else {
                closeCatalog();
            }
        });
    }

    void getText(boolean is_firstPage){
        ArrayList<String> arrayList=book.getChapter(chapter_num);
        chapter=null;
        chapter=new Chapter_GUI(
                arrayList,pane.getWidth(),pane.getHeight(),
                14.5,has_chapter);
        if(is_firstPage) page_num=0;
        else page_num=chapter.getPage_num()-1;
    }
    void showText(){
        if(book==null){
            return;
            //todo:显示提示
        }
        has_chapter = book.has_chapter;
        if(pane.getWidth()==0||pane.getHeight()==0) {
            chapter = new Chapter_GUI(
                    book.getChapter(chapter_num), prefsize * 0.9 * 0.9, prefsize * 0.9,
                    14.5, has_chapter);
        }
        else {
            chapter=new Chapter_GUI(
                    book.getChapter(chapter_num),pane.getPrefWidth(),pane.getHeight(),
                    14.5,has_chapter);
        }
        page_num=Math.min(page_num,chapter.getPage_num()-1);
        pane.getChildren().clear();
        pane.getChildren().add(chapter.getPage(page_num));
        System.gc();
    }
    void initText() {
        //todo:修改字体大小功能
        String output=System.getProperty("user.dir")+test+"\\resources\\"+name;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(output));
            book = (Book) in.readObject();
            in.close();
            showText();
        } catch (FileNotFoundException e) {
            AtomicBoolean isLoaded= new AtomicBoolean(true);
            pane.getChildren().clear();
            pane.getChildren().add(new Text("Loading..."));
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    //子线程获取主线程变量
                    is_loading=1;
                    book = new Book(name, path);
                    if(!isLoaded.get())
                        return null;
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(output));
                    out.writeObject(book);
                    out.close();
                    is_loading=0;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (isLoaded.get())
                                showText();
                        }
                    });
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.start();
            task.setOnCancelled(event->{
                thread.interrupt();
                book=null;
                pane.getChildren().clear();
                pane.getChildren().add(new Text("Stopped successfully ~"));
            });
            Button stop =new Button("停止加载");
            stop.setLayoutY(20);
            stop.setOnMouseClicked(event->{
                isLoaded.set(false);
                task.cancel();
            });
            pane.getChildren().add(stop);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    void saveTemp(){
        //todo:可改至数据库后台
        if(book==null)
            return;
        if(is_loading==1)
            return;
        BookTemp b=new BookTemp(name,path,chapter_num,page_num);
        try {
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(tempfile));
            out.writeObject(b);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void readTemp(){
        try {
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(tempfile));
            //java io 默认项目目录
            BookTemp b= (BookTemp) in.readObject();
            in.close();
            name=b.getName();
            path=b.getPath();
            chapter_num=b.getChapter_num();
            page_num=b.getPage_num();
            b=null;
            initText();
        } catch (FileNotFoundException e){
            warning("还没有打开书哦qwq");
        } catch (IOException|ClassNotFoundException e){
            e.printStackTrace();
        }
    }
    //打开新书后大小不对  点目录会消失
    public void start(Stage stage) {
        this.stage=stage;
        cssfile=cssfile.replaceAll("\\\\","/");
        tempfile=tempfile.replaceAll("\\\\","/");
        //正则和java各转义一次

        getBackground();
        gethBox();
        getPane();
        getSlider();
        getCatalog();
        hBox.getChildren().add(catalog);
        hBox.getChildren().add(pane);
        background.getChildren().add(hBox);
        background.getChildren().add(getCloseButton());
        background.getChildren().add(getFileButton());
        background.getChildren().add(getHidenButton());

        readTemp();

        Scene scene=new Scene(background);
        scene.setFill(null);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (is_loading==1)
                return;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.DOWN)
                page_num++;
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.UP)
                page_num--;
            turnPage();
            e.consume();
        });

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/cat.png")));
        //getResource加/意为根目录，不加为当前目录
        DragUtil.addDragListener(stage,pane);
        DrawUtil.addDrawFunc(stage,background);
        stage.show();
        System.gc();
        stage_X=stage.getX();
        stage_Y=stage.getY();
    }
    public static void main(String[] args) throws Exception {
        Application.launch(args);
    }
}

