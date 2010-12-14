package edu.dlsu.SUMOs.main;

import java.awt.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import edu.dlsu.SUMOs.util.ReadWriteTextFile;
import edu.dlsu.SUMOs.util.Xml;


public class XmlParser {

  public static ArrayList parseXml(String result) {
//    System.out.println(result);
    ArrayList<String> results = new ArrayList<String>();
    
    try {
      if(PictureEditor.isPrint) {
        System.out.println("result: ");
      }
      File file = new File("temp.xml");
      file.createNewFile();
      ReadWriteTextFile.setContents(file, result);
      Xml queryResponse = new Xml("temp.xml", "queryResponse");
      for (Xml answer : queryResponse.children("answer")) {
        try {
          Xml bindingSet = answer.child("bindingSet");
          Xml binding = bindingSet.child("binding");
          Xml var = binding.child("var");
          if(!results.contains(var.string("value"))) {
            results.add(var.string("value"));
          }
        } catch (Exception e) {
          if(!results.contains(answer.string("result"))) {
            results.add(answer.string("result"));
          }
        }
      }
      if(PictureEditor.isPrint)
      for(int i=0;i<results.size();i++) {
        System.out.println("\t"+ results.get(i));
      }
      return results;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}
