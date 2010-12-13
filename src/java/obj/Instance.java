package obj;

import java.awt.List;
import java.util.ArrayList;

public class Instance {

  private String name;
  private ArrayList attributes;
  
  public Instance(String name) {
    this.name = name;
    attributes = new ArrayList();
  }
  
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public ArrayList getAttributes() {
    return attributes;
  }
  public void setAttributes(ArrayList attributes) {
    this.attributes = attributes;
  }
  
  public void addAttribute(String attribute) {
    this.attributes.add(attribute);
  }
}
