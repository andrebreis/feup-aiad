package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by Inês on 10/12/2017.
 */
public class GUI extends JFrame {

    JPanel leftPanel;
    JPanel rightPanel;
    JButton addPatient;
    JButton addResource;
    JTextArea hospital;
    JTextField time;
    JButton inc;
    JButton ok;
    JButton cancel;

    JPanel patientPanel;
    JPanel resourcePanel;

    public static void main(String[] args){
        new GUI();
    }

    public GUI(){

        this.setSize(700,400);

        Toolkit tk = Toolkit.getDefaultToolkit();

        Dimension dim = tk.getScreenSize();
        int xPos = (dim.width / 2) - (this.getWidth() / 2);
        int yPos = (dim.height / 2) - (this.getHeight() / 2);
        this.setLocation(xPos,yPos);
        this.setResizable(false);


        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setTitle("Scheduling");

        JPanel thePanel = new JPanel(new BorderLayout());

        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new EmptyBorder(30, 50, 30, 95));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        thePanel.add( leftPanel, BorderLayout.WEST );
        thePanel.add(rightPanel);
        leftPanel.setBackground(Color.black);
        rightPanel.setBackground(Color.black);

        rightPanel.add(Box.createRigidArea(new Dimension(0,60)));
        addPatient = new JButton("Add patient");
        rightPanel.add(addPatient);

        rightPanel.add(Box.createRigidArea(new Dimension(0,75)));
        addResource = new JButton("Add resource");
        rightPanel.add(addResource);

        rightPanel.add(Box.createRigidArea(new Dimension(0,75)));

        time = new JTextField(" Enter time (min)");
        time.setMaximumSize(new Dimension(220, 30));
        rightPanel.add(time);

        rightPanel.add(Box.createRigidArea(new Dimension(0,15)));
        inc = new JButton(" + ");


        rightPanel.add(inc);


        hospital = new JTextArea();
        hospital.setPreferredSize(new Dimension(300,300));
        hospital.setVisible(true);
        hospital.setText("Hospital cenas");
        leftPanel.add(hospital,BorderLayout.CENTER);


        //PATIENT PANEL

        patientPanel = new JPanel();
        patientPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        patientPanel.setBackground(Color.black);
        patientPanel.setPreferredSize(new Dimension(250,200));
        patientPanel.setBorder(new EmptyBorder(10,20,10,10));

        JLabel critLabel = new JLabel("Criticality");
        JTextField critText = new JTextField(7);
        JLabel sevLabel = new JLabel("Severity");
        JTextField sevText = new JTextField(7);
        JLabel examsLabel = new JLabel("Exams");
        JTextField examsText = new JTextField(7);


        critLabel.setDisplayedMnemonic(KeyEvent.VK_C);
        critLabel.setLabelFor(critText);
        sevLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        sevLabel.setLabelFor(sevText);
        examsLabel.setDisplayedMnemonic(KeyEvent.VK_E);
        examsLabel.setLabelFor(examsText);


        patientPanel.add(critLabel);
        patientPanel.add(critText);
        patientPanel.add(Box.createRigidArea(new Dimension(700,15)));
        patientPanel.add(sevLabel);
        patientPanel.add(sevText);
        patientPanel.add(Box.createRigidArea(new Dimension(700,15)));
        patientPanel.add(examsLabel);
        patientPanel.add(examsText);
        patientPanel.add(Box.createRigidArea(new Dimension(700,25)));

        ok = new JButton("    Ok    ");
        cancel = new JButton("Cancel");

        patientPanel.add(ok);
        patientPanel.add(cancel);


        //RESOURCE PANEL

        resourcePanel = new JPanel();
        resourcePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        resourcePanel.setBackground(Color.black);
        resourcePanel.setPreferredSize(new Dimension(250,100));
        resourcePanel.setBorder(new EmptyBorder(10,10,20,10));

        JLabel examsRLabel = new JLabel("Exams");
        JTextField examsRText = new JTextField(7);

        examsRLabel.setDisplayedMnemonic(KeyEvent.VK_E);
        examsRLabel.setLabelFor(examsRText);

        resourcePanel.add(examsRLabel);
        resourcePanel.add(examsRText);
        resourcePanel.add(Box.createRigidArea(new Dimension(700,25)));

        ok = new JButton("    Ok    ");
        cancel = new JButton("Cancel");

        resourcePanel.add(ok);
        resourcePanel.add(cancel);


        this.add(thePanel);
        this.setVisible(true);


        addPatient.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent actionEvent) {System.out.print("OI");
             changePanel(thePanel,patientPanel);
          }
        });

        addResource.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {System.out.print("OI");
                changePanel(thePanel,resourcePanel);
            }
        });
    }




    public void changePanel(JPanel oldPanel, JPanel newPanel){
        this.remove(oldPanel);
        this.setContentPane(newPanel);
        this.pack();
        this.validate();
        this.repaint();
        }

}
