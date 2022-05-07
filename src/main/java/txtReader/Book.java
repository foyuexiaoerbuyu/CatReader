package txtReader;

import javafx.concurrent.Task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Chapter implements Serializable{
    long pos;//起始位置
    long end;//结束位置
    String content;//章节名
    public Chapter(long p,long e,String c){
        pos=p;
        content=c;
        end=e;
    }
}

class Book implements Serializable {
    String name;
    String filepath;
    ArrayList<Chapter> chapters;
    int has_chapter;
    String charset="GBK";
    public Book(String n,String p){
        name=n;
        filepath=p;
        read();
    }
    private String convertCode(String a){
        String res="";
        try {
            res=new String(a.getBytes("ISO-8859-1"),charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return res;
    }
    private static String getFilecharset(String sourceFile) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                return charset; //文件编码为 ANSI
            } else if (first3Bytes[0] == (byte) 0xFF
                    && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE"; //文件编码为 Unicode
                //todo:似乎无法解码
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE"; //文件编码为 Unicode big endian
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8"; //文件编码为 UTF-8
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF) // 单独出现BF以下的，也算是GBK
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) // 双字节 (0xC0 - 0xDF)
                            // (0x80
                            // - 0xBF),也可能在GB编码内
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }
    private void read (){
        String r="\\s*(第[0-9|零一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾百千万]+[章卷回话]" +
                "|[c|C]hapter.*[0-9]|☆、.*|[上中下终]卷" +
                "|卷[0-9|零一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾]+" +
                "|[Ll][Vv].[0-9]+|－Quiz [0-9]+|[引子|楔子|序|序章][\n\\s]).*";
        Pattern p=Pattern.compile(r);
        chapters=new ArrayList<>();
        try {
            RandomAccessFile in=new RandomAccessFile(filepath,"r");
            charset=getFilecharset(filepath);
            long start=0;
            in.seek(start);
            String t;
            long pre_title=0,pre_pos=0,now_pos=0;
            String now_text=null,title=null;
            in.seek(start);
            while((t=in.readLine())!=null){
                if(Thread.currentThread().isInterrupted())
                    return;
                now_pos=in.getFilePointer();
                now_text=convertCode(t);
                if(!now_text.contains("。")&&!now_text.contains("：“")){
                    now_text=now_text.replaceAll("　"," ").trim();
                    Matcher m=p.matcher(now_text);
                    if(m.matches()) {
                        if (pre_title == 0 && now_pos != in.length()) {
                            chapters.add(new Chapter(pre_title, pre_pos, "前言"));
                        } else {
                            chapters.add(new Chapter(pre_title, pre_pos, title));
                        }
                        pre_title = pre_pos;
                        title = now_text;
                    }
                }
                pre_pos=now_pos;
            }
            if(chapters.size()==0){
                has_chapter=0;
                in.seek(start);
                long pre=0,now=0,cnt=0;
                while(in.readLine()!=null){
                    cnt++;
                    if(cnt==50){
                        now=in.getFilePointer();
                        chapters.add(new Chapter(pre,now,""));
                        pre=now;
                        cnt=0;
                    }
                }
                if(pre!=in.length()){
                    chapters.add(new Chapter(pre,in.length(),""));
                }
            }
            else {
                has_chapter=1;
                chapters.add(new Chapter(pre_title, in.length(), title));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    public ArrayList<String> getChapter(int num){
        ArrayList<String> text=new ArrayList<>();
        try {
            RandomAccessFile in=new RandomAccessFile(filepath,"r");
            long start=chapters.get(num).pos;
            long end=chapters.get(num).end;
            in.seek(start);
            while (in.getFilePointer()<end){
                String t=in.readLine();
                String k=convertCode(t);
                k=k.replaceAll("　"," ");
                k=k.trim();
                text.add(k); //去中文首尾空格
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }
    public int getChapter_num(){
        return chapters.size();
    }
    public String getChapterName(int num){
        return chapters.get(num).content;
    }
}

class BookTemp implements Serializable{
    String name;
    String path;
    int chapter_num;
    int page_num;
    BookTemp(String n, String p, int cn, int pn){
        name=n;
        path=p;
        chapter_num=cn;
        page_num=pn;
    }
    String getName(){return name;}
    String getPath(){return path;}
    int getChapter_num(){return chapter_num;}
    int getPage_num(){return page_num;}
}
