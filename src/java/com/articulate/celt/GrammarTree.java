package com.articulate.celt;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.net.URL;
import java.io.IOException;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;

import com.articulate.sigma.*;

/** *************************************************************
 * This code is copyright Articulate Software (c) 2003.  
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * in any writings, briefings, publications, presentations, or 
 * other representations of any software which incorporates, builds on, or uses this 
 * code.  Please cite the following article in any publication with references:
 * 
 * Pease, A., (2003). The Sigma Ontology Development Environment, 
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.
 * 
 * A JTree-based editor for creating a grammar.  The grammar can
 * be saved or loaded from an XML-formatted file.  Nodes in the grammar
 * contain information that is stored in the GrammarNode class
 * and persisted as values in an XML "node" tag.  
 * 
 * There are a number of interdependencies in this class.  Each field in
 * GrammarNode needs to be supported in the XML, which is handled in the
 * load() and save() methods.  Some fields and values in GrammarNode must be
 * supported by icons defined in initialize() and MyRenderer.  GrammarNode
 * fields are edited in a dialog defined by createNodeEditPanel() and
 * actions from that dialog are handled in the actionPerformed() method.
 */
public class GrammarTree extends JPanel implements TreeSelectionListener, ActionListener {
    
    private JTree tree;
    private JDialog dialog;
    private static JFrame frame;
    public DefaultMutableTreeNode top;
    private static boolean useSystemLookAndFeel = true;
    private JPopupMenu popup;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();
    protected DefaultTreeModel treeModel;
    private Hashtable grammarItems = new Hashtable();
    private boolean newNode = false; // Whether the user is adding a new node, or editing and existing one.
    public static String sentence;
    
    /** *************************************************************
     */
    public GrammarTree() {

        super(new GridLayout(1,0));
        System.setProperty("user.dir","C:\\Program Files\\Apache Tomcat 4.0\\");
        System.out.println("INFO in GrammarTree(): Set user.dir to: " + System.getProperty("user.dir"));
        KBmanager.getMgr();
        //Create the nodes.
        top = new DefaultMutableTreeNode(new GrammarNode("Empty","",true,"",GrammarNode.ONE_TO_MANY));
        initialize(top);
        try {
            WordNet.initOnce();
        }
        catch (IOException ioe) {
            System.out.println("Error in GrammarTree(): Error intializing WordNet: " + ioe.getMessage());
        }
        WordNet.wn.readWordFrequencies();
        WordNet.wn.readSenseIndex();
       // System.out.println("INFO in GrammarTree(): Word sense: " + 
       //    WordNet.wn.findWordSense("run","The run by Jamaican Bob Marley set a new world record."));
    }
    
    /** *************************************************************
     * Initialize the JTree with the given TreeNode.
     */
    private void initialize(TreeNode top) {

        //Create a tree that allows one selection at a time.
        String directory = System.getProperty("user.dir");
        directory = directory + "KBs" + File.separator;
        System.out.println("INFO in GrammarTree.initialize(): Using directory: " + directory);
        treeModel = new DefaultTreeModel(top);
        treeModel.addTreeModelListener(new MyTreeModelListener());

        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);

        ImageIcon orNodeIcon = new ImageIcon(directory + "red.gif");
        ImageIcon fileIcon = new ImageIcon(directory + "textIcon.gif");
        ImageIcon wnIcon = new ImageIcon(directory + "wnIcon.gif");
        ImageIcon starIcon = new ImageIcon(directory + "star.gif");
        ImageIcon plusIcon = new ImageIcon(directory + "plus.gif");
        ImageIcon optionalIcon = new ImageIcon(directory + "questionMark.gif");
        if (orNodeIcon != null && fileIcon != null && wnIcon != null && 
            starIcon != null && plusIcon != null && optionalIcon != null) 
            tree.setCellRenderer(new MyRenderer(orNodeIcon,fileIcon,wnIcon,starIcon,plusIcon,optionalIcon));
        else
            System.out.println("Error in GrammarTree.initialize(): One or more icons not found.");
        
        //Create the scroll pane and add the tree to it. 
        JScrollPane treeView = new JScrollPane(tree);
        tree.setEditable(true);
        treeView.setPreferredSize(new Dimension(500, 300));

        add(treeView);
    }

    /** *************************************************************
     * Listen for events on the tree model.
     */
    private class MyTreeModelListener implements TreeModelListener {

        public void treeNodesChanged(TreeModelEvent e) {

            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode)
                     (e.getTreePath().getLastPathComponent());

            /*
             * If the event lists children, then the changed
             * node is the child of the node we've already
             * gotten.  Otherwise, the changed node and the
             * specified node are the same.
             */
            try {
                int index = e.getChildIndices()[0];
                node = (DefaultMutableTreeNode)
                       (node.getChildAt(index));
            } catch (NullPointerException exc) {}

            //System.out.println("The user has finished editing the node.");
            //System.out.println("New value: " + node.getUserObject());
        }
        public void treeNodesInserted(TreeModelEvent e) {
        }
        public void treeNodesRemoved(TreeModelEvent e) {
        }
        public void treeStructureChanged(TreeModelEvent e) {
        }
    }

    /** *************************************************************
     * Create the main menu.
     */
    private JMenuBar createMenu() {

        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("File");
        menuBar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("load");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menuItem = new JMenuItem("save");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menuItem = new JMenuItem("parse");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        return menuBar;
    }

    /** *************************************************************
     * Create the popup menu.
     */
    private void createPopupMenu() {

        popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Add");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Delete");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Edit");
        menuItem.addActionListener(this);
        popup.add(menuItem);
    
        //Add listener to components that can bring up popup menus.
        MouseListener popupListener = new PopupListener();
        tree.addMouseListener(popupListener);
    }

    /** *************************************************************
     * Supports display of each node in the grammar.
     */
    private class MyRenderer extends DefaultTreeCellRenderer {

        Icon orNodeIcon;
        Icon fileIcon;
        Icon wnIcon;
        Icon starIcon;
        Icon plusIcon;
        Icon optional;
    
        public MyRenderer(Icon icon, Icon icon2, Icon icon3, Icon icon4, Icon icon5, Icon icon6) {
            orNodeIcon = icon;
            fileIcon = icon2;
            wnIcon = icon3;
            starIcon = icon4;
            plusIcon = icon5;
            optional = icon6;
        }
    
        public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {
    
            GrammarNode nInfo = null;

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            nInfo = (GrammarNode) ((DefaultMutableTreeNode) value).getUserObject();
            if (nInfo.orNode)
                setIcon(orNodeIcon);
            if (nInfo.filename != null && nInfo.filename.length() > 0)
                setIcon(fileIcon);
            if (nInfo.nodeName != null && nInfo.nodeName.indexOf("WordNet") != -1)
                setIcon(wnIcon);
            if (nInfo.repeat == GrammarNode.ZERO_TO_MANY)
                setIcon(starIcon);
            if (nInfo.repeat == GrammarNode.ONE_TO_MANY)
                setIcon(plusIcon);
            if (nInfo.repeat == GrammarNode.OPTIONAL) 
                setIcon(optional);
    
            return this;
        }
    }

    /** *************************************************************
     * Listens for a right-click on a menu item.
     */
    private class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
    
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
    
        private void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {
                if (e.isPopupTrigger()) 
                    popup.show(e.getComponent(), e.getX(), e.getY());
            
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                tree.addSelectionPath(selPath);
            }
        }
    }

    /** *************************************************************
     * Required by TreeSelectionListener interface.
     */
    public void valueChanged(TreeSelectionEvent e) {

        GrammarNode newnode;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        Object nodeObject = node.getUserObject();
        if (node.isLeaf()) 
            newnode = (GrammarNode) nodeObject;        
    }
        
    /** *************************************************************
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {

        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }

        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        frame = new JFrame("Grammar Tree");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        GrammarTree newContentPane = new GrammarTree();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        frame.setJMenuBar(newContentPane.createMenu());
        newContentPane.createPopupMenu();

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /** *************************************************************
     * Create a window for editing information about a grammar node.
     * Node that this method creates a heavy dependency on the method
     * actionPerformed() which must be cognizent of this methods
     * structure of creating panels and components.  The hierarchical 
     * panel structure is:
     * 
     * contentPanel
     *   inputPanel
     *     nodeName
     *     KIFtemplate
     *     nodeType
     *       andOrButtonGroup
     *         andNode
     *         orNode
     *     fileName
     *     repeat
     *       repeatButtonGroup
     *         dontRepeat
     *         oneToMany
     *         zeroToMany
     *   buttonPanel
     */
    private JPanel createNodeEditPanel(GrammarNode node) {

        JPanel contentPanel;
        JPanel buttonPanel = new JPanel();
        JPanel promptPanel = new JPanel();
        promptPanel.setLayout(new GridLayout(5,1));  
              
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(5,1));
 
        JPanel nodeName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nodeName.add(new JLabel("Node name   "));
        JPanel KIFtemplate = new JPanel(new FlowLayout(FlowLayout.LEFT));
        KIFtemplate.add(new JLabel("KIF template"));
        JPanel nodeType = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nodeType.add(new JLabel("Node type   "));
        JPanel fileName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileName.add(new JLabel("File name   "));
        JPanel repeat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        repeat.add(new JLabel("Repeat      "));

        JRadioButton andNode = new JRadioButton("and node");      // Create and/or node radio buttons
        andNode.setActionCommand("andNode");
        andNode.setSelected(true);
        JRadioButton orNode = new JRadioButton("or node");
        orNode.setActionCommand("orNode");
        ButtonGroup andOrGroup = new ButtonGroup();
        andOrGroup.add(andNode);
        andOrGroup.add(orNode);
        JPanel andOrButtonGroup = new JPanel();
        andOrButtonGroup.add(andNode);
        andOrButtonGroup.add(orNode);

        JRadioButton dontRepeat = new JRadioButton("don't repeat");      // Create repeating node radio buttons
        dontRepeat.setActionCommand("dontRepeat");
        dontRepeat.setSelected(true);
        JRadioButton oneToMany = new JRadioButton("one to many");
        oneToMany.setActionCommand("oneToMany");
        JRadioButton zeroToMany = new JRadioButton("zero to many");
        zeroToMany.setActionCommand("zeroToMany");
        JRadioButton optional = new JRadioButton("optional");
        zeroToMany.setActionCommand("optional");
        ButtonGroup repeatGroup = new ButtonGroup();
        repeatGroup.add(dontRepeat);
        repeatGroup.add(oneToMany);
        repeatGroup.add(zeroToMany);
        repeatGroup.add(optional);
        JPanel repeatButtonGroup = new JPanel();
        repeatButtonGroup.add(dontRepeat);
        repeatButtonGroup.add(oneToMany);
        repeatButtonGroup.add(zeroToMany);
        repeatButtonGroup.add(optional);
                                                                         
        if (node == null) {                                         // create input fields
            nodeName.add(new JTextField(10));
            KIFtemplate.add(new JTextField(10));
            nodeType.add(andOrButtonGroup);
            fileName.add(new JTextField(10));
            repeat.add(repeatButtonGroup);
        }
        else {
            int length = 10;      // length of the given text field in characters.
            if (node.nodeName == null || length > node.nodeName.length()) 
                length = 10;
            else
                length = node.nodeName.length() + 1;
            nodeName.add(new JTextField(node.nodeName,length));
            if (node.KIFtemplate == null || length > node.KIFtemplate.length()) 
                length = 10;
            else
                length = node.KIFtemplate.length() + 1;
            KIFtemplate.add(new JTextField(node.KIFtemplate,length));
            if (node.orNode) 
                orNode.setSelected(true);
            else
                andNode.setSelected(true);
            nodeType.add(andOrButtonGroup);
            if (node.filename == null || length > node.filename.length())
                length = 10;
            else
                length = node.filename.length() + 1;
            fileName.add(new JTextField(node.filename,length));
            if (node.repeat == GrammarNode.ZERO_TO_MANY) 
                zeroToMany.setSelected(true);
            else {
                if (node.repeat == GrammarNode.ONE_TO_MANY) 
                    oneToMany.setSelected(true);
                else {
                    if (node.repeat == GrammarNode.OPTIONAL) 
                        optional.setSelected(true);
                    else
                        dontRepeat.setSelected(true);
                }
            }
            repeat.add(repeatButtonGroup);          
        }

        inputPanel.add(nodeName);                                  // add input fields
        inputPanel.add(KIFtemplate);
        inputPanel.add(nodeType);
        inputPanel.add(fileName);
        inputPanel.add(repeat);

        JButton okButton = new JButton("OK");                   //  add buttons
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
        contentPanel.add(inputPanel);
        contentPanel.add(buttonPanel);
        return contentPanel;
    }

    /** *************************************************************
     * Remove all nodes except the root node.
     */
    public void clear() {

        top.removeAllChildren();
        treeModel.reload();
    }    

    /** *************************************************************
     * Remove the currently selected node.
     */
    public void removeCurrentNode() {

        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
                         (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                return;
            }
        } 

        // Either there was no selection, or the root was selected.
        toolkit.beep();
    }

    /** *************************************************************
     * Add child to the currently selected node.
     */
    public DefaultMutableTreeNode addObject(Object child) {

        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null)
            parentNode = top;
        else 
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        
        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
                                            Object child, 
                                            boolean shouldBeVisible) {

        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        if (parent == null) 
            parent = top;
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        //Make sure the user can see the new node.
        if (shouldBeVisible) {
            TreePath path = new TreePath(childNode.getPath());
            tree.scrollPathToVisible(path);
            treeModel.nodeChanged(childNode);
            //System.out.println("INFO in GrammarTree.addObject(): Making node " + ((GrammarNode) child).nodeName + " visible.");
        }
        return childNode;
    }

    public void addNode(DefaultMutableTreeNode parent,
                                            DefaultMutableTreeNode child, 
                                            boolean shouldBeVisible) {

        if (parent == null) 
            parent = top;
        treeModel.insertNodeInto(child, parent, parent.getChildCount());

        //Make sure the user can see the new node.
        if (shouldBeVisible) {
            TreePath path = new TreePath(child.getPath());
            tree.scrollPathToVisible(path);
            treeModel.nodeChanged(child);
            //System.out.println("INFO in GrammarTree.addNode(): Making node visible.");
        }
    }

    /** *************************************************************
     * Modify the currently selected node.
     */
    public void changeObject(Object node) {

        TreePath path = tree.getSelectionPath();
        DefaultMutableTreeNode selectedNode = null;

        if (path == null)
            return;
        
        selectedNode = (DefaultMutableTreeNode) (path.getLastPathComponent());
        selectedNode.setUserObject(node);
        treeModel.nodeChanged(selectedNode);
        tree.scrollPathToVisible(new TreePath(path));
        return;
    }

    /** *************************************************************
     * Respond to menu selection actions
     */
    private void getGrammarNode(GrammarNode node) {

        dialog = new JDialog(frame,"Node information");
        JPanel contentPanel = createNodeEditPanel(node);
        dialog.setContentPane(contentPanel);
        dialog.pack();
        // dialog.setSize(new Dimension(350,300));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /** *************************************************************
     * Respond to menu selection actions. Note that this method depends heavily
     * on the structure of panels in the edit window, which is created in
     * createNodeEditPanel();
     */
    public void actionPerformed(ActionEvent e) {

        System.out.println("INFO in GrammarTree.actionPerformed(): " + e.getSource().getClass().getName());
        if (e.getSource().getClass().getName().indexOf("JMenuItem") != -1) {
            JMenuItem source = (JMenuItem) e.getSource();
            try {
                if (source.getText().equalsIgnoreCase("load"))
                    load();
                if (source.getText().equalsIgnoreCase("save"))
                    save();
                if (source.getText().equalsIgnoreCase("parse"))
                    parse();
                if (source.getText().equalsIgnoreCase("Add")) {
                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    GrammarNode node = (GrammarNode) dmtn.getUserObject();
                    System.out.println("Add to " + node.nodeName);
                    newNode = true;            // controls whether to perform addObject() or changeObject() below
                    getGrammarNode(null);
                }
                if (source.getText().equalsIgnoreCase("Delete")) {
                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    GrammarNode node = (GrammarNode) dmtn.getUserObject();
                    System.out.println("Delete " + node.nodeName);
                    removeCurrentNode();
                }
                if (source.getText().equalsIgnoreCase("Edit")) {
                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    GrammarNode node = (GrammarNode) dmtn.getUserObject();
                    System.out.println("Edit " + node.nodeName);
                    newNode = false;           // controls whether to perform addObject() or changeObject() below
                    getGrammarNode(node);
                }
            }
            catch (IOException ioe) {
                System.out.println("Error in GrammarTree.actionPerformed: Action " + source.getText());
                System.out.println(ioe.getMessage());
            }
        }
        else {
            JButton source = (JButton) e.getSource();
            boolean orNode;
            System.out.println(source.getText());
            if (source.getText().equalsIgnoreCase("OK")) {       // The user pressed "OK" on the node edit dialog.
                JPanel inputPanel = (JPanel) dialog.getContentPane().getComponent(0);  // get the input panel

                JPanel inputItemPanel = (JPanel) inputPanel.getComponent(0);           // get the name panel
                String name = ((JTextField) inputItemPanel.getComponent(1)).getText();
                System.out.println("name: " + name);

                inputItemPanel = (JPanel) inputPanel.getComponent(1);                  // get the KIF template panel
                String template = ((JTextField) inputItemPanel.getComponent(1)).getText();
                System.out.println("KIF template: " + template);

                inputItemPanel = (JPanel) inputPanel.getComponent(2);                  // get the and/or panel
                if (((JRadioButton) ((JPanel) inputItemPanel.getComponent(1)).getComponent(1)).getSelectedObjects() == null) {
                    orNode = false;
                    System.out.println("INFO in GrammarTree.actionPerformed: and node selected.");
                }
                else {
                    System.out.println("INFO in GrammarTree.actionPerformed: or node selected.");
                    orNode = true;
                }

                inputItemPanel = (JPanel) inputPanel.getComponent(3);                  // get the filename panel
                String filename = ((JTextField) inputItemPanel.getComponent(1)).getText();
                System.out.println("filename: " + filename);

                int repeatInt = GrammarNode.NO_REPEAT;
                inputItemPanel = (JPanel) inputPanel.getComponent(4);                  // get the repeat panel
                if (((JRadioButton) ((JPanel) inputItemPanel.getComponent(1)).getComponent(0)).getSelectedObjects() != null)
                    repeatInt = GrammarNode.NO_REPEAT;
                if (((JRadioButton) ((JPanel) inputItemPanel.getComponent(1)).getComponent(1)).getSelectedObjects() != null)
                    repeatInt = GrammarNode.ONE_TO_MANY;
                if (((JRadioButton) ((JPanel) inputItemPanel.getComponent(1)).getComponent(2)).getSelectedObjects() != null)
                    repeatInt = GrammarNode.ZERO_TO_MANY;
                if (((JRadioButton) ((JPanel) inputItemPanel.getComponent(1)).getComponent(3)).getSelectedObjects() != null)
                    repeatInt = GrammarNode.OPTIONAL;
                
                GrammarNode node = new GrammarNode(name,template,orNode,filename,repeatInt);
                if (node.nodeName != null && newNode) 
                    addObject(node);
                if (node.nodeName != null && !newNode) 
                    changeObject(node);
            }
            dialog.setVisible(false);      // Whether the user clicks OK or Cancel, the window disappears.
            //if (source.getText().equalsIgnoreCase("Cancel")) {
            //    dialog.setVisible(false);
            //}
        }
    }

    /** *************************************************************
     * Create a copy of a node and all its children.
     */
    private DefaultMutableTreeNode deepNodeCopy(DefaultMutableTreeNode node) {

        GrammarNode nInfo = new GrammarNode((GrammarNode) node.getUserObject());
        DefaultMutableTreeNode result = new DefaultMutableTreeNode(nInfo);
        for (int i = 0; i < node.getChildCount(); i++) {
            result.add(deepNodeCopy((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return result;
    }

    /** *************************************************************
     * Load a node from an XML element into the JTree
     */
    private void loadNode(BasicXMLelement xml, DefaultMutableTreeNode parent) { 

        DefaultMutableTreeNode newNode = null;
        GrammarNode nInfo = null;
        int children = 0;
        String name = null;
        boolean visibleBoolean = false;
        if (xml.tagname.equalsIgnoreCase("node")) {
            name = (String) xml.attributes.get("name");
            String kif = (String) xml.attributes.get("kif");
            String filename = (String) xml.attributes.get("filename");
            children = (new Integer((String) xml.attributes.get("children"))).intValue();
            String orNode = (String) xml.attributes.get("orNode");
            boolean orNodeBoolean = false;
            if (orNode != null && orNode.length() > 0)
                orNodeBoolean = (new Boolean(orNode)).booleanValue();
            String visible = (String) xml.attributes.get("visible");
            if (visible != null && visible.length() > 0)
                visibleBoolean = (new Boolean(visible)).booleanValue();
            String repeat = (String) xml.attributes.get("repeat");
            int repeatInt = GrammarNode.NO_REPEAT;
            if (repeat != null) 
                repeatInt = Integer.valueOf(repeat).intValue();

            nInfo = new GrammarNode(name,kif,orNodeBoolean,filename,repeatInt,visibleBoolean);
            if (parent == null) {
                top = new DefaultMutableTreeNode(nInfo);
                //initialize(top);
                treeModel.setRoot(top);
                grammarItems.put(name,top);
                for (int i = 0; i < children; i++) {
                    BasicXMLelement element = (BasicXMLelement) xml.subelements.get(i);
                    loadNode(element,top);
                }
            }
            else {
                if (grammarItems.containsKey(name)) { 
                    DefaultMutableTreeNode node = deepNodeCopy((DefaultMutableTreeNode) grammarItems.get(name));
                    //System.out.println("INFO in GrammarTree.loadNode(): Linking to node: " + name);
                    addNode(parent,node,visibleBoolean);
                }
                else {
                    newNode = addObject(parent,nInfo,visibleBoolean);
                    //System.out.println("INFO in GrammarTree.loadNode(): Creating new node: " + name);
                    //System.out.println("INFO in GrammarTree.loadNode(): Visible: " + visible);
                    grammarItems.put(name,newNode);
                    for (int i = 0; i < children; i++) {
                        BasicXMLelement element = (BasicXMLelement) xml.subelements.get(i);
                        loadNode(element,newNode);
                    }
                    //System.out.print("INFO in GrammarTree.loadNode(): Node " + name + " has visibility: ");
                    //System.out.println(tree.isVisible(new TreePath(newNode.getPath())));
                }
            }
        }
        else
            System.out.println("Error in TreeDemo.load(): Bad tag: " + xml.tagname);
    }

    /** *************************************************************
     * Load the contents of an XML file into the grammar tree.
     */
    public void load() throws IOException {

        StringBuffer xmlString = new StringBuffer();
        BufferedReader br = null;
        grammarItems = new Hashtable();

        clear();
        try {
            String dir = System.getProperty("user.dir");
            dir = dir + File.separator + "KBs" + File.separator;
            br = new BufferedReader(new FileReader(dir + "grammar.xml"));
            do {
                String line = br.readLine();
                xmlString.append(line + "\n");
            } while (br.ready());
        }
        catch (java.io.IOException e) {
            System.out.println("Error in KBmanager.readConfiguration(): IO exception parsing file grammar.xml");
            System.out.println(e.getMessage());
        }
        finally {
            if (br != null) 
                br.close();
        }

        BasicXMLparser xml = new BasicXMLparser(xmlString.toString());
        loadNode((BasicXMLelement) xml.elements.get(0),null);
    }
    
    /** *************************************************************
     * Write the node and its children to an XML file. 
     */
    private void saveChild(Hashtable saveList, PrintWriter pw, DefaultMutableTreeNode node) throws IOException { 

        GrammarNode userNode = (GrammarNode) node.getUserObject();
        userNode.save(pw,tree.isVisible(new TreePath(node.getPath())),node.getChildCount());
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            GrammarNode nodeI = (GrammarNode) child.getUserObject();
            System.out.println("INFO in GrammarTree.saveChild(): Saving node: " + nodeI.nodeName);
            if (saveList.containsKey(nodeI.nodeName.intern()) &&
                ((Boolean) saveList.get(nodeI.nodeName.intern())).booleanValue()) {
                child.removeAllChildren();
                saveChild(saveList,pw,child);
            }
            else {
                saveList.put(nodeI.nodeName.intern(),new Boolean(true));
                saveChild(saveList,pw,child);
            }
        }
        pw.println("</node>");
    }

    /** *************************************************************
     * Write the tree to an XML file.
     */
    public void save() throws IOException {

        Hashtable saveList = new Hashtable(); // Associate a node name with a boolean indicated whether it has been saved.
        Iterator items = grammarItems.keySet().iterator();
        while (items.hasNext()) {
            String key = (String) items.next();
            saveList.put(key.intern(),new Boolean(false));
        }
        String dir = System.getProperty("user.dir");
        dir = dir + File.separator + "KBs" + File.separator;
        String filename = dir + "grammar.xml";
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            fw = new FileWriter(filename);
            pw = new PrintWriter(fw);
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            if (!root.isLeaf())
                saveChild(saveList,pw,root);
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + filename + ": " + e.getMessage());
        }
        finally {            
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    /** ************************************************************
     * Parse a sentence using the current grammar.  Calls the celt parser
     * to parse which in turn calls interpreter to convert the parse data
     * to a logic expression.
     */
    public void parse() {

        resetNodes(top);

        sentence = (String) JOptionPane.showInputDialog(
                    frame,
                    "Sentence:",
                    "Sentence Input",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "John kicks the cart.");
        if (sentence == null || sentence.length() < 1) 
            sentence = "The mason lays large bricks with a strong trowel.";
        CELTparser celtParser = new CELTparser();
        celtParser.grammarTree = treeModel;
        try {
            WordNet.initOnce();
        }
        catch (IOException ioe) {
            System.out.println("Error in GrammarTree.parse(): IO Error: " + ioe.getMessage());
        }
        celtParser.parse(sentence);
        System.out.println("INFO in GrammarTree.parse(): Completed parse.");
    }

    /** *************************************************************
     * Clear the parse data from the given node downward.  Typically,
     * this is called initially with the top node of the tree.
     */
    public void resetNodes(DefaultMutableTreeNode node) {

        GrammarNode nInfo = (GrammarNode) node.getUserObject();
        nInfo.reset();

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            resetNodes(child);
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        if (args.length > 0) 
            sentence = args[0];

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
