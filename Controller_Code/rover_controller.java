import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.awt.image.*;
import javax.imageio.*;

public class rover_controller extends JPanel implements ChangeListener, KeyEventDispatcher, WindowListener 
{
  //handle to instance of this class
  static rover_controller contentPane;
  //port to connect to
  static int port = 5270;
  //-1 signals that UDP will not be used
  static int dataport = -1;
  //whether or not the bytes need to be reversed to be understood on the C++ side
  //0 means do not reverse
  static int rev = 0;
  //name of the host
  static String host = "192.168.1.108";
  //handle to the client socket
  static Client myClient;
  //size of command
  static int size=6;
  //command array (byte 1: speed of forward/reverse, byte 2: direction of forward/reverse, byte 3: sensitivity of left/right,
  //byte 4: direction of left/right, byte 5:up/down for camera, byte 6:lights on/off)
  static byte command[] = new byte[size];
  //handles to sliders
  JSlider speed;
  JSlider sensitivity;
  
  
  
  //runs on startup
  public static void main( String args[] ) throws IOException 
  {  
    //make a new client socket connection
    myClient = new Client(port, dataport, host, rev);   
    
    //set initial command values for speed and sensitivity
    command[0]=50;
    command[2]=20;
    
//create the UI in a seperate thread for thread safety
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
    
    //run communication in a seperate thread, so UI never hangs
    Thread communication=new Thread() {
      public void run() {
        communicate();
      }
    };
    communication.start();
  }
  
  
  
  
  //constructor
  public rover_controller() {
    //setup UI layout
    super(new FlowLayout(FlowLayout.CENTER,60,10));
    
    //initialize sliders and changeListener
    speed = new JSlider(JSlider.HORIZONTAL, 10, 100, 50);
    sensitivity = new JSlider(JSlider.HORIZONTAL, 5, 100, 20);
    speed.addChangeListener(this);
    sensitivity.addChangeListener(this);
    
    //make the sliders unfocusable so arrow keys won't change them
    speed.setFocusable(false);
    sensitivity.setFocusable(false);
    
    //make labels for sliders
    Hashtable speedLabels = new Hashtable();
    speedLabels.put( new Integer( 10 ), new JLabel("Slow") );
    speedLabels.put( new Integer( 100 ), new JLabel("Fast") );
    speed.setLabelTable( speedLabels );
    speed.setBackground(Color.white);
    speed.setPaintLabels(true);
    JLabel speedLabel = new JLabel("Speed");
    
    Hashtable sensitivityLabels = new Hashtable();
    sensitivityLabels.put( new Integer( 5 ), new JLabel("MIN") );
    sensitivityLabels.put( new Integer( 100 ), new JLabel("MAX") );
    sensitivity.setLabelTable( sensitivityLabels );
    sensitivity.setBackground(Color.white);
    sensitivity.setPaintLabels(true);
    JLabel sensitivityLabel = new JLabel("Steering Sensitivity");
    
    //add a keyboard event listener
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(this);
    
    //add the components to the pane
    add(speedLabel);
    add(speed);
    add(sensitivityLabel);
    add(sensitivity);
  }
  
  
  
  
  
  //catches any keypresses
  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    //key presses continue to be called while a key is held down
    if (e.getID() == KeyEvent.KEY_PRESSED) {
      //if it's the right arrow key, change the L/R direction to 1(right)
      if(e.getKeyCode()==KeyEvent.VK_RIGHT){
        command[3]=1;
      }
      //if it's the left arrow key, change the L/R direction to 2(left)
      if(e.getKeyCode()==KeyEvent.VK_LEFT){
        command[3]=2;
      }
      //if it's the up arrow key, change the FWD/REV direction to 1(forward)
      if(e.getKeyCode()==KeyEvent.VK_UP){
        command[1]=1;
      }
      //if it's the down arrow key, change the FWD/REV direction to 2(reverse)
      if(e.getKeyCode()==KeyEvent.VK_DOWN){
        command[1]=2;
      }
      //if it's the Q key, change the camera direction to 1(up)
      if(e.getKeyCode()==KeyEvent.VK_Q){
        command[4]=1;
      }
      //if it's the Z key, change the camera direction to 2(down)
      if(e.getKeyCode()==KeyEvent.VK_Z){
        command[4]=2;
      }
    }
    
    //key relased is called when a key is no longer pressed
    if (e.getID() == KeyEvent.KEY_RELEASED) {
      //if it's the right or left arrow key, change the L/R direction to 0(straight)
      if(e.getKeyCode()==KeyEvent.VK_RIGHT || e.getKeyCode()==KeyEvent.VK_LEFT){
        command[3]=0;
      }
      //if it's the up or down arrow key, change the FWD/REV direction to 0(still)
      if(e.getKeyCode()==KeyEvent.VK_UP || e.getKeyCode()==KeyEvent.VK_DOWN){
        command[1]=0;
      }
      //if it's the Q or Z key, change the camera direction to 0(still)
      if(e.getKeyCode()==KeyEvent.VK_Q || e.getKeyCode()==KeyEvent.VK_Z){
        command[4]=0;
      }
      //if it's the L key, toggle the lights
      if(e.getKeyCode()==KeyEvent.VK_L){
        if(command[5]==0){
          command[5]=1;
        }
        else{
          command[5]=0;
        }
      }
    }
    return false;
  }
  
  
  
  
  
  
  
  //sets up arts of the GUI and displays it
  private static void createAndShowGUI() {
    //Create and set up the window.
    JFrame frame = new JFrame("Raspberry Rover");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    //create and set up the content pane
    contentPane = new rover_controller();
    contentPane.setPreferredSize(new Dimension(320, 160));
    contentPane.setBackground(Color.white);
    frame.setContentPane(contentPane);
    
    //Display the window.
    frame.pack();
    frame.setVisible(true);    
  }
  
  
  
  
  
  
  
  public static void communicate(){
    //loop forever
    while(true){
      try{
        //send command
        myClient.SendBytes(command,size);
      }
      catch(IOException IE){
        //if disconnected, close socket and window
        try{
          myClient.Close();
          System.exit(0);
        }
        catch(IOException IEX){}
      }
    }
  }
  
  
  
  
  //repaints the window
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
  }
  
  
  
  
  
  
  //listens for changes on sliders
  public void stateChanged(ChangeEvent e) {
    //check which slider changed
    //update appropriate command byte
    if(e.getSource()==speed){
      command[0]=(byte)speed.getValue();
    }
    else{
      command[2]=(byte)sensitivity.getValue();
    }
  }
  
  
  
  
  
  
  
  //window listeners
  //All have to be implemented, but only windowClosing is needed
  //closes the socket when the program is exited
  public void windowClosing(WindowEvent e) {
    try{   
      myClient.Close(); 
    }
    catch(IOException IE){}
  }
  public void windowClosed(WindowEvent e) {
  }
  
  public void windowOpened(WindowEvent e) {
  }
  
  public void windowIconified(WindowEvent e) {
  }
  
  public void windowDeiconified(WindowEvent e) {
  }
  
  public void windowActivated(WindowEvent e) {
  }
  
  public void windowDeactivated(WindowEvent e) {
  }
  
  public void windowGainedFocus(WindowEvent e) {
  }
  
  public void windowLostFocus(WindowEvent e) {
  }
  
  public void windowStateChanged(WindowEvent e) {
  }
}
