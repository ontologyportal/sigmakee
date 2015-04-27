import java.util.*;
import java.io.*;
import java.nio.file.*;

class TestDataExtract{
  
    public static ArrayList<String> getAllFilenamesInDir(String dir) throws IOException {

      ArrayList<String> res = new ArrayList<String>();
      Files.walk(Paths.get(dir)).forEach(filePath -> {
        if (Files.isRegularFile(filePath)) {                
;
          if(filePath.toString().endsWith(".txt")&& !filePath.toString().contains("/") )
            res.add(filePath.getFileName().toString());
        }
      });
      return res;
    }
  /************************************************************
     */
    public static ArrayList<ArrayList<String>> extractFile(String p) {

        ArrayList<String> querys = new ArrayList<String>();
        ArrayList<String> anses = new ArrayList<String>();
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        ArrayList<String> pass=new ArrayList<String>();
        String passage="";
        boolean isPassageGot=false;
        int linenum=0;
        int lastQuery=0;
        String lastAnswer=null;
        boolean isqh=true;
        try (Scanner in = new Scanner(new FileReader(p))) {
            while (in.hasNextLine()) {
              String input=in.nextLine().trim();
              if(input.length()<2)
                continue;
              if(!isPassageGot &&input.length()>250){
                passage=input;
                pass.add(passage);
                isPassageGot=true;
                continue;
              }
              linenum++;
              if(isPassageGot && isqh && input.endsWith("?")){
                lastQuery=linenum;
                querys.add(input);
                isqh=false;
                continue;
              }
              if(lastQuery!=0 && linenum-lastQuery==2){
                lastAnswer=input;
                continue;
              }
              if(lastQuery!=0 && linenum-lastQuery==4){
                if(input.contains("Amelia") && input.contains("accurate"))
                  anses.add(lastAnswer);
                else
                  anses.add("null");
                isqh=true;
                continue;
              }
              // System.out.println(linenum+":"+input );

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // for(String s:querys)
        //   System.out.println(s);
        // for(String s:anses)
        //   System.out.println(s);
          
              
              
        boolean isGood=querys.size()==anses.size();
        if(isGood){
          res.add(querys);
          res.add(anses);
          res.add(pass);
          return res;
        }
        // System.out.println("Query size :"+ querys.size());
        // System.out.println("Answer size :"+ anses.size());
        //
        // System.out.println("Extract not good in :"+ p);
        return null;
    }
    
    // public static ArrayList<String> postprocessanswer(ArrayList<String> anses){
//       for(String s:anses){
//         s=s.trim();
//         if(s.)
//       }
//}
    
    public static void runIndir()throws IOException{
      ArrayList<String> files=getAllFilenamesInDir("");
      StringBuilder sb=new StringBuilder();
      sb.append("[\n");
      for(String file:files){
        ArrayList<ArrayList<String>> res=extractFile(file);
        if(res==null) continue;
        ArrayList<String> querys=res.get(0);
        ArrayList<String> anses=res.get(1);
        ArrayList<String> pass=res.get(2);
        String passage=pass.get(0);
        //System.out.println(passage);
        try(PrintWriter pw=new PrintWriter(new FileWriter(file))){
          pw.write(passage);
          pw.flush();
        }
        for(int i=0;i<querys.size();++i){
          String q=querys.get(i);
          String a=anses.get(i);
          sb.append("  {\n    \"file\": \"");
          sb.append(file.substring(0,file.lastIndexOf(".")));
          sb.append("\",\n    \"query\" : \"");
          sb.append(q);
          sb.append("\",\n    \"answer\" : \"");
          sb.append(a);
          sb.append("\"\n  },\n");
          
        }
      }
      String res=sb.toString();
      res=res.substring(0,res.lastIndexOf(","));
      res+="\n]";
      System.out.println(res);
 
    }
    
    public static void main(String[] args)throws IOException{
      runIndir();
      //extractFile("AmericanDriver.txt");
    }
}