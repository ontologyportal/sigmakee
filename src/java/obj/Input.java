package obj;

import java.awt.List;

public class Input {

  private List characters;
  private List childCharacters;
  private List adultCharacters;
  private List objects;
  private int background;
  private int theme;
  
  public Input() {
    characters = new List();
    childCharacters = new List();
    objects = new List();
  }

  public int getObject(final int i) {
    return Integer.parseInt(objects.getItem(i));
  }
  
  public int getCharacter(final int i) {
    return Integer.parseInt(characters.getItem(i));
  }
  
  public void addChildCharacter(final int i) {
    childCharacters.add(""+i);
    addCharacter(i);
  }
  
  public void addCharacter(final int i) {
    characters.add(""+i);
  }
  
  public void addObject(final int i) {
    objects.add(""+i);
  }
  
  public List getChildCharacters() {
    return childCharacters;
  }
  
  public List getCharacters() {
    return characters;
  }

  public void setCharacters(List characters) {
    this.characters = characters;
  }

  public List getObjects() {
    return objects;
  }

  public void setObjects(List objects) {
    this.objects = objects;
  }

  public int getBackground() {
    return background;
  }

  public void setBackground(int background) {
    this.background = background;
  }

  public void setTheme(int theme) {
    this.theme = theme;
  }
  
  public int getTheme() {
    return theme;
  }
  
}
