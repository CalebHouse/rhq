/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.helpers.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.helpers.ui.ManagedHive.Validation;

/** Is a basic ui that generates a very simple managed
 *  graphical resource that can be managed/monitored
 *  by an RHQ plugin.
 *  
 *  Simulates a bee hive where:
 *  - can configure i)current bee population count
 *  -               ii)swarm time response
 *  - can monitor  i)bee population count
 *  -              ii)whether hive is angry
 *  - can execute operations and 
 *  -              i)shake the hive to upset the bees
 *  -              ii)temporarily adds a few more bees
 *   
 *  PIQL for process identification used in discovery
 *  
 *  @author Simeon Pinder
 */
public class ManagedHive extends JFrame {

    static Logger LOG = Logger.getLogger(ManagedHive.class.getName());
    /******************* Startup/initialization & Components *************/
    /** Simple command line launch mechanism
     * @param args
     */
    public static void main(String[] args) {
        new ManagedHive();
    }

    public ManagedHive() {
        //create ui layout
        initializeUi();
        //initial hive setup
        for (int i = 0; i < basePopulation; i++) {
            addBee();
            try {
                Thread.sleep(3);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        //now start the remote api
        initializeRemoteApi();
    }

    /******************* Management capabilities **************************/

    private void initializeRemoteApi() {
        RemoteApi.remoteApiServer(9876);
    }

    /******************* UI Logic & Components **************************/
    private int space = 7;//horizontal spacing between components
    protected static int basePopulation = 50;//resident hive population
    //swarm time should be (2 or 3)* 30s to allow RHQ to clearly collect angry status.
    protected static int swarmTime = 60 * 1000;//ms. 
    protected static Hive hiveComponent;
    protected static Random generator = new Random(System.currentTimeMillis());
    protected static int beeWidth = 15;
    protected static int beeHeight = 15;
    protected static JTextField currentPopulation;
    protected static ManagedHive CONTROLLER = null;
    protected static Runnable angryTimer = null;
    protected static int angryPackSize = 50;
    protected static int beeAdditionAmount = 13;
    protected static JLabel mood = null;
    protected static JLabel miEnabled = null;
    protected static JTextField swarmTimeDisplayField;
    protected static JTextField populationBaseField;
    protected static JTextField swarmTimeUpdateField;
    protected static JTextField beeCountUpdateField;
    protected static JButton updateConfiguration;
    protected static ObjectMapper mapper = new ObjectMapper();
    protected static int defaultRemoteApiPort = 9876;

    public enum Validation {
        POPULATION_BASE(50, 2500, "Population Base"), SWARM_TIME(30000, (60000 * 30), "Swarm Time"), BEES_TO_ADD(5,
            150, "Number of bees");
        private int lowest;
        private int highest;
        private String name;
        private JTextField field_value = null;

        public int getLowest() {
            return lowest;
        }

        public int getHighest() {
            return highest;
        }

        public JTextField getField() {
            return field_value;
        }

        public void setField(JTextField field) {
            field_value = field;
        }

        public String getValidationDetails() {
            String validationErrorMessage = name + " values must be an integer >= '" + getLowest() + "' and <= '"
                + getHighest()
                + "'.";
            return validationErrorMessage;
        }

        private Validation(int lowest, int highest, String name) {
            this.lowest = lowest;
            this.highest = highest;
            this.name = name;
        }
    };

    protected static Vector<Validation> validationList = new Vector<Validation>();
    static {
        for (Validation v : Validation.values()) {
            validationList.add(v);
        }
    }

    /** Responsible for putting together the layout components.
     * 
     */
    private void initializeUi() {

        setTitle("Managed Hive:");//titling
        //ui organization
        getContentPane().setLayout(new BorderLayout());
        // top panel definition
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(LineBorder.createGrayLineBorder());

        //monitor row
        JPanel monitorRow = new JPanel();
        monitorRow.setLayout(new BoxLayout(monitorRow, BoxLayout.X_AXIS));
        TitledBorder titledBorder1 = new TitledBorder("Realtime Values:");
        titledBorder1.setTitleColor(Color.gray);
        monitorRow.setBorder(titledBorder1);
        top.add(monitorRow);

        //operation row
        JPanel operationsRow = new JPanel();
        operationsRow.setLayout(new BoxLayout(operationsRow, BoxLayout.X_AXIS));
        TitledBorder operationsBorder = new TitledBorder("Operations:");
        operationsBorder.setTitleColor(Color.gray);
        operationsRow.setBorder(operationsBorder);
        top.add(operationsRow);

        {
            //monitor row shows current state of the hive
            JLabel currentPopulationLabel = new JLabel("Current Bee count");
            monitorRow.add(currentPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            currentPopulation = new JTextField("" + basePopulation);
            currentPopulation.setEditable(false);
            monitorRow.add(currentPopulation);
            monitorRow.add(Box.createHorizontalStrut(space));

            JLabel maxPopulationLabel = new JLabel("Swarm Time Left(ms)");
            monitorRow.add(maxPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            swarmTimeDisplayField = new JTextField("" + swarmTime);
            swarmTimeDisplayField.setEditable(false);
            monitorRow.add(swarmTimeDisplayField);
            monitorRow.add(Box.createHorizontalStrut(space));//spacer

            mood = new JLabel();
            mood.setOpaque(true);
            mood.setBackground(Color.green);
            mood.setText("Calm");
            monitorRow.add(mood);
            monitorRow.add(Box.createHorizontalStrut(space));
            //managed interface reporting
            miEnabled = new JLabel();
            miEnabled.setOpaque(true);
            miEnabled.setBackground(Color.gray);
            miEnabled.setText("Remote Api(disabled)");
            monitorRow.add(miEnabled);
        }

        //Shake hive button
        JButton shake = new JButton("Shake Hive");
        shake.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeShakeOperation();
            }
        });

        //add some more bees operation
        JButton addNBees = new JButton("Add Bees");
        addNBees.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeAddBeeOperation();
            }
        });

        {//populate the operations row.
            JTextField instructions = new JTextField(
                "*Use these operations to modify the number of bees protecting the hive.*");
            instructions.setEditable(false);
            operationsRow.add(instructions);
            operationsRow.add(Box.createHorizontalStrut(space));
            operationsRow.add(shake);
            monitorRow.add(Box.createHorizontalStrut(space));
            operationsRow.add(addNBees);
        }

        //configuration row
        JPanel configurationRow = new JPanel();
        configurationRow.setLayout(new BoxLayout(configurationRow, BoxLayout.X_AXIS));
        TitledBorder configurationBorder = new TitledBorder("Configuration:");
        configurationBorder.setTitleColor(Color.gray);
        configurationRow.setBorder(configurationBorder);
        top.add(configurationRow);
        
        {//populate the configuration row.
         //population base
            JLabel basePopulationLabel = new JLabel("Base population");
            configurationRow.add(basePopulationLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            populationBaseField = new JTextField("" + basePopulation);
            populationBaseField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.POPULATION_BASE) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(populationBaseField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(populationBaseField);

            //swarm time
            JLabel swarmTimeLabel = new JLabel("Swarm Time(ms)");
            configurationRow.add(swarmTimeLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            swarmTimeUpdateField = new JTextField("" + swarmTime);
            swarmTimeUpdateField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.SWARM_TIME) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(swarmTimeUpdateField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(swarmTimeUpdateField);

            //add bee amount 
            JLabel beeAdditionAmountLabel = new JLabel("Bees to add");
            configurationRow.add(beeAdditionAmountLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            beeCountUpdateField = new JTextField("" + beeAdditionAmount);
            beeCountUpdateField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.BEES_TO_ADD) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(beeCountUpdateField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(beeCountUpdateField);

            //update button
            updateConfiguration = new JButton("Update configuration");
            updateConfiguration.setEnabled(false);
            updateConfiguration.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    //validate defaults and apply updates if possible.
                    final ArrayList<String> validationMessages = new ArrayList<String>();
                    for (Validation rule : validationList) {
                        JTextField field = rule.getField();
                        if (rule.getField() == null)
                            return;//bail if field not set
                        String newValueString = field.getText();
                        int newValue = -1;
                        try {
                            newValue = Integer.valueOf(newValueString);
                            //apply rules
                            if ((newValue >= rule.getLowest()) && (newValue <= rule.getHighest())) {
                                switch (rule) {
                                case POPULATION_BASE:
                                    basePopulation = newValue;
                                    break;
                                case SWARM_TIME:
                                    swarmTime = newValue;
                                    break;
                                case BEES_TO_ADD:
                                    beeAdditionAmount = newValue;
                                    break;
                                default:
                                    break;
                                }
                            } else {
                                //generate validation message.
                                validationMessages.add(rule.getValidationDetails());
                            }
                        } catch (NumberFormatException nfe) {
                            //generate validation message.
                            validationMessages.add(rule.getValidationDetails());
                        }
                    }
                    //kick off population adjustments if any
                    ManagedHive.hiveComponent.removeBee();
                    //disable the update configuration button.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {

                            //display validation messages and reset fields
                            if (!validationMessages.isEmpty()) {
                                String message = "The following validation errors were detected:";
                                for (String m : validationMessages) {
                                    message += m;
                                }
                                message += "Resetting fields to previous selections.";
                                //custom title, warning icon
                                JOptionPane.showMessageDialog(CONTROLLER, message, "Input validation:",
                                    JOptionPane.WARNING_MESSAGE);
                                //reset
                                populationBaseField.setText(basePopulation + "");
                                swarmTimeUpdateField.setText(swarmTime + "");
                                beeCountUpdateField.setText(beeAdditionAmount + "");
                            }
                            //disable edit button
                            updateConfiguration.setEnabled(false);
                        }
                    });
                }
            });
            configurationRow.add(updateConfiguration);
        }

        // center
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        // build center panel
        buildCenterPanel(center);

        // final component layout
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        this.setSize(650, 500);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);

        //assigned shared reference.
        CONTROLLER = this;
    }

    private void buildCenterPanel(final JPanel center) {
        hiveComponent = new Hive();
        hiveComponent.setBorder(new LineBorder(Color.black));
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                center.add(hiveComponent);
            }
        });
    }

    /** Bundles up the add Bee operation semantics
     */
    protected static void executeAddBeeOperation() {
        //retrieve addition amount
        int addAmount = beeAdditionAmount;
        for (int i = 0; i < addAmount; i++) {
            addBee();
        }
    }

    /** Bundles up the shake operation semantics
     */
    protected static void executeShakeOperation() {
        for (int i = 0; i < angryPackSize; i++) {
            addBee();
        }
        //kick the hive into angry mode and set angry timer
        if (angryTimer == null) {
            angryTimer = new SwarmTimer();
            Thread t = new Thread(angryTimer);
            t.start();
            //speed up the bees.
            BeeFlight.setDelay(2);
            //update the ui to reflect hive mood.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.mood.setText("Angry!");
                    ManagedHive.mood.setBackground(Color.red);
                }
            });
        } else {//reset angry timer
            SwarmTimer swarmResponseManager = (SwarmTimer) angryTimer;
            swarmResponseManager.setExpireTime(swarmTime);
        }
    }

    /**
     * Adds a bouncing ball to the canvas and starts a thread to make it bounce
     */
    public static void addBee() {
        Bee b = null;
        //tweak the start position
        int newX = generator.nextInt(BeeFlight.delta);
        int newY = generator.nextInt(BeeFlight.delta);
        b = new Bee(newX, newY);
        hiveComponent.add(b);
        Runnable r = new BeeFlight(b, hiveComponent);
        Thread t = new Thread(r);
        t.start();
    }
}

class Hive extends JComponent {
    //entire hive population.
    private static Vector<Bee> population = new Vector<Bee>();

    public int getCurrentPopulation() {
        return population.size();
    }

    public void add(Bee b) {
        synchronized (population) {
            population.add(b);
        }
    }

    public void removeBee() {
        synchronized (population) {
            if (population.size() > 0) {
                population.remove(0);
            }
            //if population falls below basePopulation level then add another bee
            if (population.size() < ManagedHive.basePopulation) {
                int delta = ManagedHive.basePopulation - population.size();
                for (int i = 0; i <= delta; i++) {//replenish
                    ManagedHive.addBee();
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        synchronized (population) {

            for (Bee b : population) {
                g2.fill(b.getShape());
            }
            //update the fields
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.currentPopulation.setText(ManagedHive.hiveComponent.getCurrentPopulation() + "");
                    if (ManagedHive.angryTimer != null) {
                        ManagedHive.swarmTimeDisplayField.setText(((SwarmTimer) ManagedHive.angryTimer).getExpireTime() + "");
                    } else {
                        ManagedHive.swarmTimeDisplayField.setText("0");
                    }
                }
            });
        }
    }
}

/** Represents the cartesion/graphical components to
 *  define a bee.
 */
class Bee {

    //properties of typical cartesion component. 
    private int xWidth = ManagedHive.beeWidth;
    private int yWidth = ManagedHive.beeHeight;

    //cartesion components
    private double x = 0;
    private double y = 0;

    //movement delta
    private double dx = 1;
    private double dy = 1;

    public Bee(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    /**
     * Defines the shape of the bee at each call.
     */
    public Ellipse2D getShape() {
        //randomly change dimensions to simulate flying bee
        int nextX = ManagedHive.generator.nextInt(ManagedHive.beeWidth);
        int nextY = ManagedHive.generator.nextInt(ManagedHive.beeHeight);
        if (nextX < 1)
            nextX = 1;
        if (nextY < 1)
            nextY = 1;
        if ((x > 0) && (y > 0)) {
            return new Ellipse2D.Double(x, y, nextX, nextY);
        } else {
            return new Ellipse2D.Double(x, y, 0, 0);
        }
    }

    //return to invisible
    public void clear() {
        xWidth = 0;
        xWidth = 0;
        ManagedHive.hiveComponent.removeBee();
    }

    /**
     * Moves the bee along. Change directions if it hits the side.
     */
    public void move(Rectangle2D bounds) {
        //the new position of the point
        x += dx;
        y += dy;
        //if the new X would result in out of the box then reverse.
        if (x < bounds.getMinX()) {
            x = bounds.getMinX();//hit the side
            dx = -dx; //bounce
        }
        if (x + xWidth >= bounds.getMaxX()) {
            x = bounds.getMaxX() - xWidth;//hit the side
            dx = -dx;//bounce
        }

        //if the new Y would result in out of the box then reverse.
        if (y < bounds.getMinY()) {
            y = bounds.getMinY();
            dy = -dy;//bounce
        }
        if (y + yWidth >= bounds.getMaxY()) {
            y = bounds.getMaxY() - yWidth;
            dy = -dy;//bounce
        }
    }
}

/**
* A thread for animating the bee's flight.
*/
class BeeFlight implements Runnable {
    //attributes
    private Bee bee;

    private Component component;

    public static final int STEPS = 10000;

    public static int DELAY = 5;

    public static int getDelay() {
        return DELAY;
    }

    public static void setDelay(int delay) {
        if ((delay >= 2) || (delay <= 6)) {//2 <delay <= 6  >
            DELAY = delay;
        }//otherwise ignore
    }

    public static int delta = 300;

    //operations
    public BeeFlight(Bee aBee, Component aComponent) {
        bee = aBee;
        component = aComponent;
    }

    public void run() {
        try {
            for (int i = 1; i <= STEPS; i++) {
                bee.move(component.getBounds());
                component.repaint();
                Thread.sleep(DELAY);
            }
            //kill the bee.
            bee.clear();
            component.repaint();

        } catch (InterruptedException e) {
        }
    }
}

//swarm anger timer
class SwarmTimer implements Runnable {
    private static int timeToLive;

    public SwarmTimer() {
        timeToLive = ManagedHive.swarmTime;//default to 1 minute
    }

    @Override
    public void run() {
        try {
            while (timeToLive > 0) {
                Thread.sleep(1000);//sleep for a second
                timeToLive = timeToLive - 1000;
            }
            //reset visual hive state flags
            //update the fields
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.mood.setText("Calm");
                    ManagedHive.mood.setBackground(Color.green);
                    //speed up the bees.
                    BeeFlight.setDelay(5);
                }
            });
            //null out angrySwarm
            ManagedHive.angryTimer = null;
        } catch (InterruptedException e) {
        }
    }

    public void setExpireTime(int swarmTime) {
        //only accept swarm times less than 10 mins and greater then 1 min(s).
        if ((swarmTime >= Validation.SWARM_TIME.getLowest()) || (swarmTime <= Validation.SWARM_TIME.getHighest())) {
            timeToLive = swarmTime;
        } else {
            ManagedHive.LOG.log(Level.WARNING, "New value '" + swarmTime + "' is not acceptable. "
                + Validation.SWARM_TIME.getValidationDetails());
        }
    }

    public static int getExpireTime() {
        return timeToLive;
    }
}

/* Listener for JTextFields that re-enable the JButton on chance.
 */
class ConfigurationFieldsListener implements DocumentListener {
    @Override
    public void removeUpdate(DocumentEvent e) {
        ManagedHive.updateConfiguration.setEnabled(true);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        ManagedHive.updateConfiguration.setEnabled(true);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        ManagedHive.updateConfiguration.setEnabled(true);
    }
}

/**
 * Responsible for running the remote api server and defining
 * the protocol for interacting with the management server.
 *   -every line to server and client should postpend a newline character. Ex. \n.
 *      multiline json with newline characters are not supported. Use spaces or tabs.
 *   -a request to server with no json body is assumed to be a request for current state
 *   -a request with json content is assumed request to update state to values passed in.
 *       Note: server side validation may still not accepte invalid values.
 *   -by setting the action field to "Shake" or "Add", case insensitive is assumed a 
 *       request to execute that action.
 *   -only ONE of following request paths is possible for each operation 
 *      i)request for current state : (empty request)
 *      ii)request for operation or : (non empty request WITH action field set)
 *      iii)request to update configuration : (non empty request WITHOUT action field set)
 */
class RemoteApi {
    private static ServerSocket apiHandler = null;
    private static String host = "localhost";
    private static int port = ManagedHive.defaultRemoteApiPort;//default port
    private static boolean continueToRun = true;

    //supported operations.
    public enum Operation {
        Shake, Add
    };

    public void updateServer(boolean newState) {
        continueToRun = newState;
    }
    public static void remoteApiServer(int port){
        //try to launch the requested port if not get random available one
        try {
            apiHandler = new ServerSocket(port);
        } catch (IOException ioe) {
            try {
                apiHandler = new ServerSocket();
            } catch (IOException e) {//if fails here give up, and spit out stack trace
                e.printStackTrace();
            }//have OS select free on for us.
            port = apiHandler.getLocalPort();
        }

        while (continueToRun) {
            Socket connectionSocket;
            try {
                connectionSocket = apiHandler.accept();
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
                    connectionSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                String clientRequest = inFromClient.readLine();
                System.out.println("[Server] Received: " + clientRequest);
                //generate response. Empty request then return state otherwise attempt to load new state
                clientRequest = clientRequest.trim();
                String response = "";
                if (clientRequest.isEmpty()) {//empty request, return state
                    ApplicationState state = new ApplicationState();
                    response = ManagedHive.mapper.writeValueAsString(state);
                } else {//request for state update.
                    ApplicationState state = ManagedHive.mapper.readValue(clientRequest, ApplicationState.class);
                    //todo: apply validation 
                    //todo: spinder apply state.
                    //detect operations request
                    String action = state.getAction();
                    action = action.trim();
                    if (!action.isEmpty()) {
                        //attempt to locate valid action
                        Operation operation = null;
                        for (Operation op : Operation.values()) {
                            if (op.name().equalsIgnoreCase(action)) {
                                operation = op;
                            }
                        }
                        //action on that action
                        if (operation != null) {
                            switch (operation) {
                            case Shake:
                                ManagedHive.executeShakeOperation();
                                break;

                            case Add:
                                ManagedHive.executeAddBeeOperation();
                                break;

                            default:
                                break;
                            }
                        }
                    } else {
                        //todo: insert validation actions.
                    }
                }
                //append newLine
                response += "\n";
                outToClient.writeBytes(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Class bundles up the current application state for serialization to JSON.
 */
class ApplicationState {
    /** Constructor will query the relevant properties at instantiation.
     */
    public ApplicationState() {//populates current state
        //real time
        currentBeePopulation = ManagedHive.hiveComponent.getCurrentPopulation();
        swarmTimeLeft = ((SwarmTimer) ManagedHive.angryTimer).getExpireTime();
        hiveAngry = (swarmTimeLeft > 0) ? true : false;
        //current configuration settings
        beePopulationBase = ManagedHive.basePopulation;
        swarmTimeBase = ManagedHive.swarmTime;
        beesToAdd = ManagedHive.angryPackSize;
        action = "";//empty as only useful when executing operations
    }

    public int getCurrentBeePopulation() {
        return currentBeePopulation;
    }

    public void setCurrentBeePopulation(int currentBeePopulation) {
        this.currentBeePopulation = currentBeePopulation;
    }

    public int getSwarmTimeLeft() {
        return swarmTimeLeft;
    }

    public void setSwarmTimeLeft(int swarmTimeLeft) {
        this.swarmTimeLeft = swarmTimeLeft;
    }

    public boolean isHiveAngry() {
        return hiveAngry;
    }

    public void setHiveAngry(boolean hiveAngry) {
        this.hiveAngry = hiveAngry;
    }

    public int getBeePopulationBase() {
        return beePopulationBase;
    }

    public void setBeePopulationBase(int beePopulationBase) {
        this.beePopulationBase = beePopulationBase;
    }

    public int getSwarmTimeBase() {
        return swarmTimeBase;
    }

    public void setSwarmTimeBase(int swarmTimeBase) {
        this.swarmTimeBase = swarmTimeBase;
    }

    public int getBeesToAdd() {
        return beesToAdd;
    }

    public void setBeesToAdd(int beesToAdd) {
        this.beesToAdd = beesToAdd;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    private int currentBeePopulation;
    private int swarmTimeLeft;
    private boolean hiveAngry;
    private int beePopulationBase;
    private int swarmTimeBase;
    private int beesToAdd;
    private String action;
}