package txtReader;

import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class Chapter_GUI{
    ArrayList<String> text;
    Queue<String> title;
    Queue<String> dealed_text;
    ArrayList<VBox> pages;
    int has_chapter;
    double width;
    double height;
    double text_width;
    double title_width;
    Chapter_GUI(ArrayList<String> t,double w,double h,double tw,int ha){
        text=t;
        width=w;
        height=h;
        text_width=tw;
        has_chapter=ha;
        title_width=15.5;
        pages=new ArrayList<>();
        title=new LinkedList<>();
        dealed_text=new LinkedList<>();
        draw();
    }
    void setText_width(double tw){
        text_width=tw;
        draw();
    }
    void setSize(double w,double h){
        width=w;
        height=h;
        draw();
    }
    void clear(){
        title.clear();
        dealed_text.clear();
        pages.clear();
    }
    void cutText(){
        int row= (int) (width/text_width);
        String t;
        for(int i=has_chapter;i<text.size();i++){
            t=text.get(i);
            while (t.length()>row){
                dealed_text.offer(t.substring(0,row));
                t=t.substring(row);
            }
            if(t.length()>0)
                dealed_text.offer(t);
        }
        if (has_chapter==0)
            return;
        t=text.get(0);
        while (t.length()>row){
            title.offer(t.substring(0,row));
            t=t.substring(row);
        }
        if(t.length()>0)
            title.offer(t);
    }//行末句号
    VBox paint(){
        VBox vBox=new VBox(10);//行间距
        vBox.setPrefWidth(width);
        vBox.setPrefHeight(height);
        vBox.setStyle("-fx-background-color:rgba(244,244,244,0)");
        double h=height;
        if(has_chapter==1){
            Font font1=new Font(title_width);
            while (!title.isEmpty()){
                if(h<title_width){
                    return vBox;
                }
                String t=title.peek();
                Text text=new Text(t);
                text.setFont(font1);
                vBox.getChildren().add(text);
                title.poll();
                h-=text.getBoundsInLocal().getHeight();
                h-=10;
            }
            has_chapter=0;
        }
        Font font2=new Font(text_width);
        while (!dealed_text.isEmpty()){
            if (h<text_width){
                return vBox;
            }
            String t=dealed_text.peek();
            Text text=new Text(t);
            //vBox不接受两个一样的引用
            text.setFont(font2);
            vBox.getChildren().add(text);
            dealed_text.poll();
            h-=text.getBoundsInLocal().getHeight();
            h-=10;
        }
        return vBox;
    }
    void draw(){
        clear();
        cutText();
        while (!title.isEmpty()||!dealed_text.isEmpty()){
            pages.add(paint());
        }
    }
    VBox getPage(int num){
        return pages.get(num);
    }
    int getPage_num(){
        return pages.size();
    }
}
