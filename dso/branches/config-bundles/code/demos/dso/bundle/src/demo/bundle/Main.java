/*
@COPYRIGHT@
*/
package demo.bundle;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.util.Date;

class Main extends JFrame {

  private String label = null;
  
  Main() {
    super("Bundle Demo");

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setDefaultLookAndFeelDecorated(true);

    if (label == null) label = (new Date()).toString();
    JLabel header = new JLabel(label, JLabel.CENTER);
    getContentPane().add(header);
    setSize(500, 200);
  }

  public static void main(String[] args) {
    (new Main()).setVisible(true);
  }
}
