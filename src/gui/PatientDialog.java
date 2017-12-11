package gui;

import jade.wrapper.StaleProxyException;
import logic.agents.Patient;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;

public class PatientDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField exams;
    private JSlider criticality;
    private JSlider severity;
    private JTextField agentName;
    private TestDialog parent;

    public PatientDialog(TestDialog parent) {
        this.parent = parent;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        double criticality = (double) this.criticality.getValue()/100.0;
        double severity = (double) this.severity.getValue()/100.0;
        String[] exams = this.exams.getText().split(" ");
        Object[] args = new Object[exams.length+2];
        args[0] = severity;
        args[1] = criticality;
        for(int i = 0; i < exams.length; i++) {
            args[2+i] = exams[i];
        }
        try {
            parent.mainContainer.createNewAgent(agentName.getText(), Patient.class.getName(), args);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

}
