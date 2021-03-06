package netizens.bank.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.Class;
import java.lang.reflect.Field;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import netizens.bank.Main;
import netizens.bank.utils.Debug;
import netizens.bank.utils.JSON;
import netizens.bank.hardware.Hardware;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.SwingUtilities;


/**
 * UI.java
 *
 * This class is responsible for handling the User Interface in its entirety.
 **/
public class UI{
  private int guiWidth, guiHeight;
  private JFrame gui;
  private HashMap<String, JSONArray> allDisplays;
  private String mode;
  private ArrayList<JLabel> labels;
  private String inputBuffer;

  private String userPin;
  private String fingerprintMatch = "";
  public int userAmount = 5000;

  private boolean firstRun = true;
  private KeyAdapter keyAdapter;

  /**
   * getVariable()
   *
   * Allows the retrieval of a specified public variable in the code, returned
   * in the form of an object that can be cast to it's original type. The
   * assumption is that the user of the object knows everything about the
   * object beforehand as information cannot be inferred with this method.
   *
   * @param n The name of the variable.
   * @return The object to be returned if found, else NULL.
   **/
  private Object getVariable(String n){
    Class<?> objClass = UI.this.getClass();
    Field[] fields = objClass.getFields();
    for(Field field : fields){
      String name = field.getName();
      System.out.println("field -> " + name);
      /* If we have a match, return it's object */
      if(name.equals(n)){
        try{
          return field.get(UI.this);
        }catch(IllegalAccessException e){
          /* Do nothing */
        }
      }
    }
    return null;
  }

  /**
   * UI()
   *
   * The constructor for the UI class, responsible for initialising all values
   * required and starting the UI.
   **/
  public UI(JSONTokener mainJSON){
    /* Read UI JSON from configuration */
    JSONObject mainObj = (new JSONObject(mainJSON)).getJSONObject("main");
    /* Get the list of settings files */
    JSONObject settingsObj = mainObj.getJSONObject("settings");
    /* Get the window settings filename */
    String windowPath = settingsObj.getString("window");
    Debug.println("windowPath -> " + windowPath);
    /* Load window settings JSON file */
    JSONObject windowObj = (new JSONObject(JSON.getJSONTokener(windowPath))).getJSONObject("window");

    /* Load displays from array */
    JSONArray displays = windowObj.getJSONArray("displays");
    /* Create HashMap */
    allDisplays = new HashMap<String, JSONArray>();
    /* Create ArrayList */
    labels = new ArrayList<JLabel>();
    /* Initialise text buffer */
    inputBuffer = "";
    /* Iterate over displays */
    for(int x = 0; x < displays.length(); x++){
      /* Single display object */
      JSONObject disp = displays.getJSONObject(x);
      /* Add display window */
      allDisplays.put(disp.getString("name"), disp.getJSONArray("elems"));
    }

    /* Setup GUI based on values read */
    gui = new JFrame();
    gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    gui.setTitle(windowObj.getString("title"));
    guiWidth = windowObj.getInt("width");
    guiHeight = windowObj.getInt("height");
    gui.setSize(guiWidth, guiHeight);
    gui.setLocationRelativeTo(null);
    /* Do not resize once setup */
    gui.setResizable(false);
    /* Don't display any window rubbish */
    gui.setUndecorated(false);
    /* Stop the GUI trying to do it's own layout management */
    gui.getContentPane().setLayout(null);

    /* Load in main display */
    loadDisplay("main");

    /* Set back colour */
    gui.getContentPane().setBackground(Colour.cast(windowObj.getString("back")));
    /* Finally display */
    gui.setVisible(true);
  }

  /**
   * loadDisplay()
   *
   * Load the display loaded in from predefined list.
   *
   * @param name The name of the display to be loaded.
   **/
  private void loadDisplay(String name){
    /* Destroy any registered JLabels */
    for(JLabel jl : labels){
      /* Get list of listeners */
      MouseListener[] mls = jl.getMouseListeners();
      /* Iterate over listeners */
      for(int x = 0; x < mls.length; x++){
        /* Remove the listener */
        jl.removeMouseListener(mls[x]);
      }
      /* Remove the JLabel from the panel */
      gui.remove(jl);
    }
    /* Get the requested window */
    JSONArray disp = allDisplays.get(name);
    /* Set the current mode */
    mode = name;
    /* Iterate over elements of display */
    for(int x = 0; x < disp.length(); x++){
      /* Get the elements */
      JSONObject elems = disp.getJSONObject(x);
      /* Get the type to decide what to do next */
      String type = elems.getString("type");
      /* Get the font object */
      JSONObject font = elems.getJSONObject("font");
      /* Pre-define variables used more than once */
      JLabel jLabel;
      switch(type){
        case "label" :
          String labelText = elems.getString("text");
          /* Check if text starts with dollar */
          if(
            labelText.charAt(0) == '$' &&
            labelText.charAt(labelText.length() - 1) == '$'
          ){
            labelText = "" + (int)getVariable(labelText.substring(1, labelText.length() - 1));
          }
          /* Create a JLabel and add text to it */
          jLabel = new JLabel(labelText, SwingConstants.CENTER);
          /* Must set opaque for the background colour to work */
          jLabel.setOpaque(true);
          /* Read and set the colours */
          jLabel.setForeground(Colour.cast(elems.getString("colour")));
          jLabel.setBackground(Colour.cast(elems.getString("back")));
          /* Set the size */
          setSize(jLabel, elems);
          /* Set the font of the label */
          setFont(jLabel, font);
          /* Add the label to the frame */
          gui.add(jLabel);
          /* Add JLabel to array */
          labels.add(jLabel);
          break;
        case "input" :
          /* Create a JLabel and add text to it */
          jLabel = new JLabel(elems.getString("text"), SwingConstants.CENTER);
          /* Must set opaque for the background colour to work */
          jLabel.setOpaque(true);
          /* Read and set the colours */
          jLabel.setForeground(Colour.cast(elems.getString("colour")));
          jLabel.setBackground(Colour.cast(elems.getString("back")));
          /* Set the size */
          setSize(jLabel, elems);
          /* Set the font of the label */
          setFont(jLabel, font);
          /* Add mouse listener */
          jLabel.addMouseListener(
            new MouseAdapter(){
              @Override
              public void mouseClicked(MouseEvent me){
                super.mouseClicked(me);
                addText(elems.getString("input"));
              }
            }
          );
          /* Add the label to the frame */
          gui.add(jLabel);
          /* Add JLabel to array */
          labels.add(jLabel);
          break;
        case "link" :
          /* Create a JLabel and add text to it */
          jLabel = new JLabel(elems.getString("text"), SwingConstants.CENTER);
          /* Must set opaque for the background colour to work */
          jLabel.setOpaque(true);
          /* Read and set the colours */
          jLabel.setForeground(Colour.cast(elems.getString("colour")));
          jLabel.setBackground(Colour.cast(elems.getString("back")));
          /* Set the size */
          setSize(jLabel, elems);
          /* Set the font of the label */
          setFont(jLabel, font);
          /* Add mouse listener */
          jLabel.addMouseListener(
            new MouseAdapter(){
              @Override
              public void mouseClicked(MouseEvent me){
                super.mouseClicked(me);
                loadDisplay(elems.getString("location"));
              }
            }
          );
          /* Add the label to the frame */
          gui.add(jLabel);
          /* Add JLabel to array */
          labels.add(jLabel);
          break;
        default :
          System.err.println("`" + type + "` type not specified.");
          Main.exit(Main.EXIT_STATUS.ERROR);
          break;
      }
    }
    /* Validate process */
    if(gui.isVisible()){
      gui.revalidate();
      gui.paint(gui.getGraphics());
    }
    
    if(!"pin".equals(name)){
      gui.removeKeyListener(keyAdapter);
    }
    keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if("pin".equals(name)){
            addText(e.getKeyChar() + "");
          }
        }
    };
    gui.addKeyListener(keyAdapter);

    if("card".equals(name)){
      String cardMatch = Hardware.getRFIDHash();
      Debug.println(cardMatch);
      loadDisplay("pin");
    }

    if("biometric".equals(name)){
      fingerprintMatch = Hardware.getFingerHash();
      Debug.println(fingerprintMatch);
      loadDisplay("checking");
    }

    if("checking".equals(name)){
      try{
        Thread.sleep(3000);
      }catch(Exception e){}
      addText("");
    }
  }

  /**
   * setFont()
   *
   * Set the font of the label with the parameters defined in the JSON
   * if no parameters are defined, then the font will fill the label
   *
   * @param jLabel The label to style the text of.
   * @param font The font information to style the text with
   **/
  private void setFont(JLabel jLabel, JSONObject font){
    Font labelFont = jLabel.getFont();
    /* initialise the font variables */
    String fontName = font.getString("name");
    int fontStyle;
    int fontSize = font.getInt("size");
    /* if the font name is blank, use the default one */
    if("".equals(fontName)){
      fontName = labelFont.getName();
    }
    /* if the style is blank, use the plain font */
    switch(font.getString("style").toUpperCase()){
      case "BOLD":
        fontStyle = Font.BOLD;
        break;
      case "ITALIC":
        fontStyle = Font.ITALIC;
        break;
      case "BOLD+ITALIC":
        fontStyle = Font.BOLD + Font.ITALIC;
        break;
      case "PLAIN": /* Falls through */
      default:
        fontStyle = Font.PLAIN;
        break;
    }
    /* If the size is -1, then automatically fill the label with the largest font that will fit */
    if(fontSize < 0){
      /* NOTE: http://stackoverflow.com/questions/2715118/how-to-change-the-size-of-the-font-of-a-jlabel-to-take-the-maximum-size#2715279 */
      String labelText = jLabel.getText();
      int stringWidth = jLabel.getFontMetrics(labelFont).stringWidth(labelText);
      double widthRatio = (double)(jLabel.getWidth()) / (double)stringWidth;
      int newFontSize = (int)(labelFont.getSize() * widthRatio);
      fontSize = Math.min(newFontSize, jLabel.getHeight());
    }
    jLabel.setFont(new Font(fontName, fontStyle, fontSize));
  }

  /**
   * setSize()
   *
   * Set the size of the label depending in the screen size.
   *
   * @param jLabel The label to set the size on.
   * @param elem The config to be read.
   **/
  private void setSize(JLabel jLabel, JSONObject elems){
    jLabel.setBounds(
      (int)(elems.getDouble("left") * guiWidth),
      (int)(elems.getDouble("top") * guiHeight),
      (int)(elems.getDouble("width") * guiWidth),
      (int)(elems.getDouble("height") * guiHeight)
    );
  }

  /**
   * addText()
   *
   * Adds a text through a specific method which may do checks.
   *
   * @param text The text to be added.
   **/
  public void addText(String text){
    /* Check input */
    switch(text){
      case "CANCEL" :
        /* Load the next screen */
        loadDisplay("card");
      case "CLEAR" :
        /* Clear the text buffer */
        inputBuffer = "";
        break;
      default :
        /* Add the text to the buffer */
        inputBuffer += text.trim();
        break;
    }
    /* Debug text to terminal */
    Debug.println("mode -> " + mode);
    Debug.println("text -> " + text);
    Debug.println("inputBuffer -> " + inputBuffer);
    /* Process the text */
    switch(mode){
      case "pin" :
        /* Check if we have pin */
        if(inputBuffer.length() >= 4){
          /* Get the pin digits */
          String pin = inputBuffer.substring(0, 4);
          /* Save the user pin */
          userPin = pin;
          /* Debug the pin to the terminal */
          Debug.println(userPin);
          /* Clear the text buffer */
          inputBuffer = "";
          /* Load the next screen */
          loadDisplay("biometric");
        }
        break;
      case "checking" :
        /* TODO: Correctly communicate with server. */
        /* TODO: Remove below hack. */
        inputBuffer = "";
        if("1234".equals(userPin) && "MATCH".equals(fingerprintMatch)){
          loadDisplay("services");
        }else{
          loadDisplay("errormsg");
        }
        break;
      case "selectammount" :
        /* Cast input to number */
        int select = Integer.parseInt(inputBuffer);
        /* Loose money (unchecked) */
        userAmount -= select;
        /* Clear input buffer */
        inputBuffer = "";
        /* Load new display */
        loadDisplay("wouldyoulikeareciept");
        break;
    }
  }
}
