package listweb.start;
import static humanaicore.common.CommonFuncs.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;
import humanaicore.common.Files;
import humanaicore.common.ScreenUtil;
import humanaicore.common.Text;
import humanaicore.common.Time;
import humanaicore.err.Err;
import humanaicore.err.Todo;
import listweb.Debug;
import listweb.Maintenance;
import listweb.Root;
import listweb.Util;
import listweb.ui.DefPanel;
import listweb.ui.NameFieldNoneditable;
import listweb.ui.TwoPrilistsPanel;

public class StartSingleUserWindow{
	
	public static void main(String[] args) throws Exception{
		lg(StartSingleUserWindow.class.getName());
		if(Root.jsonLockFile.exists()) throw new Err(
			"A copy of the prog is already running, which I know because file exists"
			+" (If this is not true, such as could not delete it while closing last time,"
			+" delete it and try again): "+Root.jsonLockFile);
		Root.jsonLockFile.createNewFile();
		Root.jsonLockFile.deleteOnExit(); //unlock jsonDir when this prog closes
		String up = "defaultUpStack", down = "defaultDownStack", selectPtr = "defaultSelectPtr";
		if(!Debug.skipRootCharsUpdateOnBootForSpeed){
			Root.updateRootChars();
			Root.saveChanges();
		}else{
			Root.updateNameCacheFromContentsOfFirstChars();
		}
		System.out.println("root prilist: "+Root.prilist(Root.rootName));
		JFrame window = new JFrame(Util.progName);
		
		JMenuBar menu = new JMenuBar();
		JMenu actions = new JMenu("Actions");
		actions.add(new JMenuItem(
			new AbstractAction("Include new files added externally, and add all files to their first letter/symbol like theFile goes in t"){
				public void actionPerformed(ActionEvent e){
					Root.updateRootChars();
				}
			}
		));
		actions.add(new JMenuItem(
			new AbstractAction("Maintenance make sure if x is in y's list, y is in x's list (automatic but slow)"){
				public void actionPerformed(ActionEvent e){
					Maintenance.loadAndSaveAllVarFilesToMakeExistenceInPrilistSymmetric();
				}
			}
		));
		actions.add(new JMenuItem(
			new AbstractAction("Empty defaultUpStack and defaultDownStack (Do this if the 2 middle lists get too big to see outer lists)"){
				public void actionPerformed(ActionEvent e){
					Root.clearStack(up, Root.rootName);
					Root.clearStack(down, Root.rootName);
				}
			}
		));
		menu.add(actions);
		JMenu options = new JMenu("Options");
		options.add(new JCheckBox("Search in defs (the text lowest in the window for each name) - TODO code this"));
		options.add(new JCheckBox("Allow mindmap to run code typed into defs, useful for programmers (SECURITY WARNING)"));
		menu.add(options);
		JMenu help = new JMenu("Help");
		help.add(new JLabel("This is a web. Anything can connect to anything."));
		help.add(new JLabel("If x is in y's list, y is in x's list,"));
		help.add(new JLabel("but they can be different orders (try by priority or time)."));
		help.addSeparator();
		help.add(new JLabel("Left click goes into a name"));
		help.add(new JLabel("Right click backs out of a name"));
		help.add(new JLabel("Left and right click together selects a name"));
		help.add(new JLabel("Drag any name in or drag to reorder (useful if ordered highest priority closer to middle of window)"));
		help.addSeparator();
		help.add(new JLabel("The + button adds a name, and - removes it."));
		help.addSeparator();
		help.add(new JLabel("You can search by parts of words like \"ord sea\" finds names containing both \"search\" and \"words\""));
		help.addSeparator();
		help.add(new JLabel("No save button. All changes are automaticly saved every "+Root.varSaveInterval()/60+" minutes"));
		help.add(new JLabel("and copied to version history every "+Root.vervarSaveInterval()/60+" minutes, when changes exist."));
		help.add(new JLabel("To get those versions, like if you delete or change by accident,"));
		help.add(new JLabel("they're the .jsonperline files in "+Root.jsonVarDir));
		help.add(new JLabel("The .json files are smaller and easier to read and are the newest of each name."));
		help.addSeparator();
		help.add(new JLabel(Util.progName+" is opensource "+Util.licenseName));
		help.add(new JLabel("To get the source code, unzip this jar file which you doubleclicked."));
		menu.add(help);
		window.setJMenuBar(menu);
		
		new Thread(){
			public void run(){
				while(true){
					Root.saveChanges();
					double now = Time.time();
					double blockSize = Root.varSaveInterval();
					double blockStart = now - now%blockSize;
					double next = blockStart+blockSize;
					Time.sleepNoThrow(next-now);
				}
			}
		}.start();
		//System.err.println("Error: must stop using selectPtr for which thing in upStack and downStack is selected. They write it but dont read. They each have their own selectedName prop parallel to their prilist/stack.");
		if(Root.prilist(up).isEmpty()) Root.addToEndOfPrilistIfNotExist(up, Root.rootName);
		if(Root.prilist(down).isEmpty()) Root.addToEndOfPrilistIfNotExist(down, Root.rootName);
		if(Root.get(up).get("selectedName") == null) Root.set(up, "selectedName", Root.rootName);
		if(Root.get(down).get("selectedName") == null) Root.set(down, "selectedName", Root.rootName);
		TwoPrilistsPanel p = new TwoPrilistsPanel(up, down, selectPtr);
		//JTabbedPane tabs = new JTabbedPane();
		//Dimension tSiz = new Dimension(200,170);
		//tabs.setMinimumSize(tSiz);
		//tabs.setPreferredSize(tSiz);
		DefPanel def = new DefPanel(selectPtr);
		/*tabs.add("def", def);
		JTabbedPane tabs2 = new JTabbedPane();
		tabs2.add("paint", new JLabel("<html>TODO simple paint with mouse here, to draw things about mindmapItems instead of write them</html>"));
		tabs2.add("options", new JLabel("TODO options"));
		*/
		//window.setLayout(new BorderLayout());
		JSplitPane vsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		/*
		JSplitPane vsplit2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		vsplit2.setResizeWeight(.5);
		vsplit2.setContinuousLayout(true);
		vsplit2.add(tabs);
		vsplit2.add(tabs2);
		vsplit.add(p);
		JPanel lowPanel = new JPanel();
		lowPanel.setLayout(new BorderLayout());
		lowPanel.add(new NameFieldNoneditable(selectPtr), BorderLayout.NORTH);
		lowPanel.add(vsplit2, BorderLayout.CENTER);
		vsplit.add(lowPanel);
		vsplit.setResizeWeight(2./3);
		vsplit.setContinuousLayout(true);
		window.add(vsplit);
		*/
		
		JPanel lowPanel = new JPanel();
		lowPanel.setLayout(new BorderLayout());
		lowPanel.add(new NameFieldNoneditable(selectPtr), BorderLayout.NORTH);
		lowPanel.add(def, BorderLayout.CENTER);
		vsplit.add(p);
		vsplit.add(lowPanel);
		vsplit.setResizeWeight(.75); //FIXME why does this have no effect sometimes?
		vsplit.setContinuousLayout(true);
		window.add(vsplit);
		
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		//window.setSize(screen.width/4, screen.height-150);
		//window.setSize(screen.width/4, screen.height-50);
		window.setSize(300,700);
		ScreenUtil.moveToScreenCenter(window);
		window.setLocation((screen.width-window.getWidth())/2, 10);
		//window.setLocation(30,70);
		//window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.addWindowListener(new WindowListener(){
			public void windowOpened(WindowEvent e){}
			public void windowClosing(WindowEvent e){
				try{
					lg("Window is closing...");
					Root.onClosingProg();
				}finally{
					System.exit(0);
				}
			}
			public void windowClosed(WindowEvent e){}
			public void windowIconified(WindowEvent e){}
			public void windowDeiconified(WindowEvent e){}
			public void windowActivated(WindowEvent e){}
			public void windowDeactivated(WindowEvent e){}
		});
		byte iconBytes[] = Files.readFileRel("/data/humanaicore/icon.jpg");
		window.setIconImage(ImageIO.read(new ByteArrayInputStream(iconBytes)));
		window.setVisible(true);
		//Time.sleepNoThrow(1.5);
		//vsplit.setResizeWeight(.5);
	}

}
