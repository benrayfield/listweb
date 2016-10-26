package listweb.ui;
import static humanaicore.common.CommonFuncs.*;

import humanaicore.common.Time;
import humanaicore.err.*;
import javafx.embed.swing.SwingFXUtils;
import listweb.Debug;
import listweb.Root;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** as Consumer<String> listens for events of specific mindmap name(s?).
Funcs are named acyc* when they change data in (TODO Acyc instead of Map) Util.mindmap
and let events do the rest such as to select something in a JList.
*/
public class PrilistStack extends JPanel implements Consumer<String> /**TODO KeyListener*/{
	
	public final String stackName, selectPtr;
	
	protected final JList stack, prilist;
	protected final AddTextRemove text;
	protected final JScrollPane scroll;
	
	/** Scrolling writes a var in my data system with listeners, and one of the listeners
	scrolls whenever a certain var changes but only if its value differs from the on screen
	scroll position. This allows you to come back to a list and find it scrolled the same
	as when you left it, and it allows other parts of the system (such as keyboard controls
	in a textfield or AI prediction) to choose to scroll it by writing that same var.
	<br><br>
	I almost solved it. Works when no 2 events happen at near time. But sometimes when I
	scroll with mouse wheel or change the list contents 2 times fast, 2 events are created
	that each believe the scroll should be a different position, and since scrolling must
	be delayed by SwingUtilities.invokeLater, each sees the other's changing of the scroll
	as something to react to by scrolling again, and it loops forever until I click.
	*/
	//protected volatile int scrollEventsScheduled;
	//protected final Set<Double> scrollPositionsScheduled;
	protected volatile long scrollEventSequence;
	
	public final boolean up;
	
	//protected String stackNameSelectedName;
	
	protected int updateScrollInLayoutThisManyMoreTimes = 2;

	protected boolean ignoreStackListEvents = false;
	
	protected boolean ignorePrilistScrollEvent = false;
	
	/** stackName.prilist is the stack and stackGhost, divided between stackName.selectedName.
	When click to select a name, writes that to selectPtr.selectedName
	which a JTextArea normally listens to and loads selectPtr.selectedName.def.
	selectPtr.selectedName can be built on openendedly in the mapacyc system.
	Only writes to selectPtr.selectedName. Never reads it. Uses stackName.selectedName for that.
	*/
	public PrilistStack(String stackName, String selectPtr, boolean up){
		this.stackName = stackName;
		this.selectPtr = selectPtr;
		this.up = up;
		stack = newJList();
		stack.setCellRenderer(new StackRender(stackName, up, Color.white, Color.black, Color.blue, Color.gray));
		prilist = newJList();
		prilist.setTransferHandler(new PrilistTransferHandler(this));
		
		//button1 goes into list
		//button2 or any 2 bu ttons together selects list
		//button3 backs out of list
		prilist.addMouseListener(new MouseListener(){
			protected final boolean mouseButtonDown[] = new boolean[4];
			/** at least 2 mouse buttons were down since last time none were down */
			protected boolean multiButtonClick;
			public int howManyMouseButtonsDown(){
				int sum = 0;
				if(mouseButtonDown[MouseEvent.BUTTON1]) sum++;
				if(mouseButtonDown[MouseEvent.BUTTON2]) sum++;
				if(mouseButtonDown[MouseEvent.BUTTON3]) sum++;
				return sum;
			}
			Point lastMouseDownAt = new Point(-1000000,-1000000);
			public void mousePressed(MouseEvent e){
				mouseButtonDown[e.getButton()] = true;
				lastMouseDownAt = e.getPoint();
				if(howManyMouseButtonsDown() >= 2) multiButtonClick = true;
			}
			public void mouseReleased(MouseEvent e){
				lg("prilist MouseListener e="+e);
				mouseButtonDown[e.getButton()] = false;
				boolean multiButtonClick_ = multiButtonClick; //set it before use it below and before events
				if(howManyMouseButtonsDown() == 0) multiButtonClick = false;
				Point p = e.getPoint();
				boolean isProbablyDrag = 5 <= lastMouseDownAt.distance(lastMouseDownAt);
				lastMouseDownAt = new Point(-1000000,-1000000);
				if(isProbablyDrag){
					lg("MouseListener ignoring drag");
				}else{
					lg("MouseListener click");
					//is probably a click, but possible its a drag from
					//another JList to the same place mouse last dragged from here 
					int jlistIndex = prilist.locationToIndex(e.getPoint());
					if(jlistIndex != -1){
						String nameClicked = (String) prilist.getModel().getElementAt(jlistIndex);
						if(!multiButtonClick_ && e.getButton() == MouseEvent.BUTTON1){ //push (and select)
							boolean popGhosts = true; //usually
							List backedStack = Root.prilist(stackName);
							int indexInStackOfNameClickedInPrilist = backedStack.indexOf(nameClicked);
							int indexInStackOfSelected = backedStack.indexOf(Root.get(stackName).get("selectedName"));
							if(indexInStackOfNameClickedInPrilist != -1 && indexInStackOfSelected != -1
									&& indexInStackOfNameClickedInPrilist <= indexInStackOfSelected){
								popGhosts = false; //Because otherwise would remove previous selection in stack
							}
							acycSelectInStack(nameClicked, popGhosts);
						}else if(multiButtonClick_ || e.getButton() == MouseEvent.BUTTON2){ //only select it in prilist (load its def etc)
							acycSelectInPrilist(nameClicked);
						}else if(!multiButtonClick_ && e.getButton() == MouseEvent.BUTTON3){ //pop (and select whats under it in stack)
							acycPop();
						}
					}
				}
			}
			public void mouseExited(MouseEvent e){}
			public void mouseEntered(MouseEvent e){}
			public void mouseClicked(MouseEvent e){}
		});
		stack.setDragEnabled(true);
		prilist.setDragEnabled(true);
		prilist.setDropMode(DropMode.INSERT);
		text = new AddTextRemove(
			(String addMe)->{
				addMe = addMe.trim();
				if(!addMe.equals("")){
					//lg("TODO find prilist and add at top");
					String prilistName = (String) Root.get(stackName).get("selectedName");
					if(prilistName != null){
						List backedPrilist = Root.prilist(prilistName);
						int i = backedPrilist.indexOf(addMe);
						if(i == -1){
							lg("Add and select "+addMe);
							Root.putAtTopOfPrilist(prilistName, addMe);
							i = 0;
						}
						if(up) i = backedPrilist.size()-1-i;
						final int I = i;
						acycSelectInPrilist(addMe);
						lg("FIXME why does this have no effect? SwingUtilities.invokeLater(()->prilist.ensureIndexIsVisible(I));");
					}
				}
			},
			"",
			(String removeMe)->{
				removeMe = removeMe.trim();
				if(!removeMe.equals("")){
					//lg("TODO find prilist and remove and select the one under it");
					String prilistName = (String) Root.get(stackName).get("selectedName");
					if(prilistName != null){
						List backedPrilist = Root.prilist(prilistName);
						int i = backedPrilist.indexOf(removeMe);
						if(i != -1){
							backedPrilist.remove(i);
							if(Root.view(stackName, prilistName).get("selectedName") != null){
								//only select the next lower in prilist if removeMe was selected
								if(!backedPrilist.isEmpty()){
									int newSelectedIndex = Math.min(i, backedPrilist.size()-1);
									String newSelectName = (String) backedPrilist.get(newSelectedIndex);
									lg("After removing "+removeMe+", selecting "+newSelectName);
									acycSelectInPrilist(newSelectName);
								}
							}
						}
					}
				}
			}
		);
		
		stack.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(ignoreStackListEvents || e.getValueIsAdjusting()) return;
				lg("stack ListSelectionListener e="+e);
				JList jlist = (JList) e.getSource();
				int i = jlist.getSelectedIndex();
				
				WholeListAtOnceModel listModel = (WholeListAtOnceModel)jlist.getModel();
				final String selectedName = i==-1 ? null : (String) listModel.getImmutableContent().get(i);
				
				//FIXME this breaks other ui events, so do it later
				/*
				//If anything is selected in prilist, unselect it so what was just clicked in stack
				//can be the main selection (def editable etc).
				Root.view(stackName, selectedName).remove("selectedName");
				Root.set(selectPtr, "selectedName", selectedName); //main selection
				*/
				//
				//FIXME whenClickInStackSelectItselfInItsOwnPrilistAsWorkaroundForOnlyBeingAbleToMainSelectInPrilist
				//Put its own name at the top of its own prilist and select it, instead of selecting from stack.
				Root.putAtTopOfPrilist(selectedName, selectedName);
				Root.view(stackName, selectedName).put("selectedName", selectedName); //next line Root.set does my event
				
				//remember and trigger events that its selected in stack
				Root.set(stackName, "selectedName", selectedName);
				
				//FIXME? why does selectedName automatically become main selection (its def editable, etc)?
				//Or does that only happen in events started from ui (not from acyc side)?
				Root.set(selectPtr, "selectedName", selectedName); //main selection
			}
		});
		
		prilist.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(ignoreStackListEvents || e.getValueIsAdjusting()) return;
				lg("prilist ListSelectionListener e="+e);
				//synchronized(getTreeLock()){
					JList jlist = (JList) e.getSource();
					int i = jlist.getSelectedIndex();
					WholeListAtOnceModel listModel = (WholeListAtOnceModel)jlist.getModel();
					//DefaultListModel listModel = (DefaultListModel) jlist.getModel();
					final String selectedNameInPrilist = i==-1
						? null : (String) listModel.getImmutableContent().get(i);
					String prilistName = (String) Root.get(stackName).get("selectedName");
					if(prilistName != null){
						//Redesigning to fix: "Wait, stackName.selectedName is for whats selected in stack, but whats in prilist can differ, and it cant be stored in name of that prilist since it can be in multiple JLists and used separately, including to drag between them, so where to store that selection? Call it stackName.selectedNameInPrilist? Since I'm going to store a map of name to scroll position, should it be combined with that? Could make it a map of name to map, and that inner map would contain scrollPos and selectedName and later possibly other fields. Yes. I choose this."
						text.write(selectedNameInPrilist==null ? "" : selectedNameInPrilist); 
						Root.view(stackName, prilistName).put("selectedName", selectedNameInPrilist);
						//Root.fireListeners(stackName);
						Root.onChange(stackName);
					}
				//}
			}
		});
		
		/*List<String> stackContent = Util.prilist(stackName);
		//TODO merge duplicate code below with accept(String)
		if(stackContent.isEmpty()) throw new Err("Stack prilist empty: "+stackName);
		String selectedName = (String) Util.get(stackName).get("selectedName");
		if(selectedName == null){
			selectedName = stackContent.get(0);
			Util.get(stackName).put("selectedName", selectedName);
		}
		int selectedIndex = stackContent.indexOf(selectedName);
		setJListContent(stack, stackContent.subList(0, selectedIndex+1), up);
		setJListContent(stackGhost, stackContent.subList(selectedIndex+1, stackContent.size()), up);
		setJListContent(prilist, Util.prilist(selectedName), up);
		*/
		setLayout(new BorderLayout());
		int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
		int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
		if(up){
			JPanel low = new JPanel(new BorderLayout());
			low.add(text, BorderLayout.NORTH);
			low.add(stack, BorderLayout.CENTER);
			add(low, BorderLayout.SOUTH);
			JPanel squashPrilist = new JPanel(new BorderLayout());
			JLabel fillEmptySpaceAbovePrilist = new JLabel("");
			fillEmptySpaceAbovePrilist.addMouseListener(new MouseListener(){
				public void mouseReleased(MouseEvent e){
					if(e.getButton() == MouseEvent.BUTTON3){
						acycPop();
					}
				}
				public void mousePressed(MouseEvent e){}
				public void mouseExited(MouseEvent e){}
				public void mouseEntered(MouseEvent e){}
				public void mouseClicked(MouseEvent e){}
			});
			squashPrilist.add(fillEmptySpaceAbovePrilist, BorderLayout.CENTER);
			squashPrilist.add(prilist, BorderLayout.SOUTH);
			scroll = new JScrollPane(squashPrilist, v, h);
			//scroll.getVerticalScrollBar().setUnitIncrement(scroll.getVerticalScrollBar().getUnitIncrement()*4);
		}else{
			JPanel high = new JPanel(new BorderLayout());
			high.add(text, BorderLayout.SOUTH);
			high.add(stack, BorderLayout.CENTER);
			add(high, BorderLayout.NORTH);
			scroll = new JScrollPane(prilist, v, h);
		}
		scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener(){
			public void adjustmentValueChanged(AdjustmentEvent e){
				//lg("scroll source "+e.getSource());
				if(!e.getValueIsAdjusting()){
					if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("top of observed");
					if(ignorePrilistScrollEvent){
						lg("ignorePrilistScrollEvent");
					}else{
						double fraction = getScrollYFraction(scroll, up);
						String prilistName = (String) Root.get(stackName).get("selectedName");
						lg("\r\nObserved "+stackName+"'s view of "+prilistName+" scroll "+fraction+" time="+Time.timeStr());
						if(prilistName != null){
							Root.view(stackName,prilistName).put("prilistScrollYFraction", fraction);
							Root.onChange(stackName);
						}
					}
				}
			}
		});
		add(scroll, BorderLayout.CENTER);
		Root.startListening(this, stackName); //instant event
		listenOnlyToNameOfStackAndPossiblySelectedPrilist();
		
		/*new Thread(){
			public void run(){
				while(true){
					SwingUtilities.invokeLater(()->{
						double now = Time.time();
						double fraction = .5+.5*Math.sin(now);
						lg("Set scroll to "+fraction);
						setScrollYFraction(scroll, up, fraction);
						Time.sleepNoThrow(1.3);
					});
				}
			};
		}.start();*/
	}
	
	public static double getScrollYFraction(JScrollPane scroll, boolean up){
		JScrollBar vs = scroll.getVerticalScrollBar();
		double min = vs.getMinimum();
		double max = vs.getMaximum();
		if(min < max){
			double val = vs.getValue();
			if(up){
				double pixelsYDisplayed = scroll.getViewport().getExtentSize().getHeight();
				val += pixelsYDisplayed;
			}
			double fraction = (val-min)/(max-min);
			fraction = Math.max(0, Math.min(fraction,1));
			if(up) fraction = 1-fraction;
			return fraction;
		}
		return Double.NaN;
		//throw new Err("Cant read scrollYFraction because min="+min+" max="+max);
	}
	
	public void setScrollYFraction(JScrollPane scroll, boolean up, double fraction){
		if(fraction < 0 || 1 < fraction) throw new Err("Not a fraction: "+fraction+" up="+up);
		if(up) fraction = 1-fraction;
		JScrollBar vs = scroll.getVerticalScrollBar();
		int min = vs.getMinimum();
		int max = vs.getMaximum();
		//lg("setScrollYFraction min="+min+" max="+max);
		if(min < max){
			double newVal = min+fraction*(max-min);
			if(up){
				double pixelsYDisplayed = scroll.getViewport().getExtentSize().getHeight();
				newVal -= pixelsYDisplayed;
			}
			if(Debug.logSetScroll) lg(" --- setScrollYFraction min="+min+" max="+max+" newVal="+newVal);
			if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("before setScroll");
			vs.setValue(Math.max(min, Math.min((int)Math.round(newVal), max)));
			if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("after setScroll");
			//scroll.repaint();
		}else{
			if(Debug.logSetScroll) lg(" --- setScrollYFraction FAIL min="+min+" max="+max);
		}
		//throw new Err("Cant set scrollYFraction because min="+min+" max="+max);
	}
	
	protected void listenOnlyToNameOfStackAndPossiblySelectedPrilist(){
		Root.stopListening(this);
		Root.startListeningWithoutInstantEvent(this, stackName);
		String prilistName = (String) Root.get(stackName).get("selectedName");
		if(prilistName != null){
			Root.startListeningWithoutInstantEvent(this, prilistName);
		}
	}
	
	protected JList newJList(){
		//JList j = new JList(new DefaultListModel());
		JList j = new JList(new WholeListAtOnceModel());
		j.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//j.addListSelectionListener(this);
		return j;
	}
	
	/** Adds to stack if not exist */
	public void acycSelectInStack(String name, boolean popGhosts){
		Root.set(selectPtr, "selectedName", name);
		if(name == null){
			if(popGhosts) throw new Err("Name to select in stack is null, so cant pop ghosts leaving nothing.");
			Root.set(stackName, "selectedName", null);
		}else{
			List backedStack = Root.prilist(stackName);
			boolean nameIsInStack = backedStack.contains(name);
			String oldSelectedInStackOrNull = (String) Root.get(stackName).get("selectedName");
			boolean anythingIsSelectedInStack = oldSelectedInStackOrNull!=null && backedStack.contains(oldSelectedInStackOrNull);
			if(nameIsInStack){
				int nameIndexInStack = backedStack.indexOf(name);
				if(popGhosts){
					backedStack.subList(nameIndexInStack+1, backedStack.size()).clear();
					Root.onChange(stackName);
				}
				Root.set(stackName, "selectedName", name);
			}else{ //!nameIsInStack
				if(anythingIsSelectedInStack){
					if(!popGhosts) throw new Err("Name="+name+" is not in stack="+stackName+" so must popGhosts but you said dont.");
					int stackSelectIndex = backedStack.indexOf(oldSelectedInStackOrNull);
					backedStack.subList(stackSelectIndex+1, backedStack.size()).clear();
					backedStack.add(name); 
					//Util.fireListeners(stackName);
					Root.set(stackName, "selectedName", name); //calls fireListeners(stackName)
					Root.addToEndOfPrilistIfNotExist(name, stackName);
				}else{
					throw new Err("Nothing selected in stack="+stackName+", so cant add "+name);
				}
			}
		}
		//listenOnlyToNameOfStackAndPossiblySelectedPrilist();
	}
	
	/** does not push or pop */
	public void acycSelectInPrilist(String name){
		String prilistName = (String) Root.get(stackName).get("selectedName");
		if(prilistName == null) throw new Err("No select in stack so no prilist should be there");
		if(!Root.prilist(prilistName).contains(name)) throw new Err("prilist "+prilistName+" not contains "+name+" so cant select it");
		Root.view(stackName, prilistName).put("selectedName", name);
		Root.onChange(stackName);
		Root.set(selectPtr, "selectedName", name);
		
		//FIXME java ui shouldnt be here since its an acyc* func,
		//but I dont want it to update every time get an event about this stack
		//since that would replace what person is typing/editing in it.
		//New field may need to be added in the maps.
		text.write(name);
	}
	
	/** Moves selection (if it exists) down 1 in stack, leaving ghosts */
	public void acycPop(){
		String s = (String) Root.get(stackName).get("selectedName");
		if(s != null){
			List backedStack = Root.prilist(stackName);
			int i = backedStack.indexOf(s);
			if(0 < i){ //selection exists and isnt lowest
				acycSelectInStack((String)backedStack.get(i-1), false);
			}
		}
	}
	
	/** Updates in mapacyc system and lets events do the rest.
	Ghosts are stackName.prilist contents above stackName.selectedName,
	or all are ghosts if stackName.selectedName is not in the stack,
	but if all are ghosts then this should not be called with popGhosts==true
	because it would be called from the stack to select one of the ghosts.
	*
	public void select(String name, boolean popGhosts){
		if(!Util.equals(stackNameSelectedName, name)){
			//System.out.println("select "+name);
			if(popGhosts){
				String oldSelectedOrNull = (String) Util.get(stackName).get("selectedName");
				if(oldSelectedOrNull == null){
					throw new Err("popGhosts while all are ghosts, select "+name);
				}else{
					List backedStack = Util.prilist(stackName);
					int i = backedStack.indexOf(oldSelectedOrNull);
					if(i == -1) throw new Err("stackName.prilist not contain (old)stackName.selectedName,"
						+" where stackName="+stackName+" and (old)stackName.selectedName="+oldSelectedOrNull);
					int keepHowMany = Math.max(i+1, backedStack.indexOf(name)+1);
					backedStack.subList(keepHowMany, backedStack.size()).clear();
				}
			}
			if(name != null && !Util.prilist(stackName).contains(name)){
				Util.addToEndOfPrilistIfNotExist(stackName, name);
			}
			Util.set(stackName, "selectedName", name);
			//TODO Can this be done simpler by stop listening to only specific name(s)?
			Util.stopListening(this);
			Util.startListening(this, stackName);
			//dont listen to selectPtr, only write to it: Util.startListening(this, selectPtr);
			Util.startListening(this, name);
			Util.set(selectPtr, "selectedName", name);
			boolean oldIgnoreListEvents = ignoreListEvents;
			try{
				if(name == null){
					stack.setSelectedIndices(new int[0]);
				}else{
					int i = jlistContent(stack,false).indexOf(name);
					if(i != -1){
						stack.setSelectedIndex(i);
						stackNameSelectedName = name;
					}
				}
				stack.invalidate();
			}finally{
				ignoreListEvents = oldIgnoreListEvents;
			}
		}
	}*/
	
	public void doLayout(){
		super.doLayout();
		if(Debug.logSwingLock) lg("TREELOCK doLayout");
		synchronized(getTreeLock()){
			if(updateScrollInLayoutThisManyMoreTimes > 0){
				updateScrollInLayoutThisManyMoreTimes--;
				JScrollBar bar = scroll.getVerticalScrollBar();
				if(up) bar.setValue(bar.getMaximum());
				double scrollUnitsPerItem = (double)bar.getMaximum()/prilist.getModel().getSize();
				//FIXME how many scrollUnits per turn of mouse wheel differs across OS, like far more of them on mac
				bar.setUnitIncrement((int)Math.ceil(.7*scrollUnitsPerItem));
			}
		}
		if(Debug.logSwingLock) lg("TREEUNLOCK doLayout");
	}

	/** Puts stack concat stackGhost in the stack JList.
	stackGhost should be gray since its not active but can be clicked to become active.
	The prilist JList displays the prilist of the top of stack.
	scrollPrilist restores the scroll position which was lost when clicked another name.
	*
	public void setState(String stack[], String stackGhost[], Rectangle scrollPrilist){
		throw new Todo();
	}*/

	
	/** acyc event. writes to screen *
	public void accept(String eventAboutThisName){
		//if(!up) System.out.println("down stackGhost: "+jlistContent(stackGhost, up));
		System.out.println("got event about name "+eventAboutThisName);
		String stackNameSelectedName = (String) Util.get(stackName).get("selectedName");
		List correctStack = Util.prilist(stackName);
		int index = stackNameSelectedName==null ? -1 : correctStack.indexOf(stackNameSelectedName); //2 ways to be -1
		if(index == -1) stackNameSelectedName = null;
		//stackNameSelectedName = (String) stack.getSelectedValue();
		//final boolean changeStackNameSelectedName = Util.equals(s, stackNameSelectedName);
		//selectedPtrSelectedName = s;
		//stackNameSelectedName = s;
		List correctPrilist = stackNameSelectedName==null
			? new ArrayList()
			: Util.prilist(stackNameSelectedName);
		final boolean changeStack = !jlistContent(stack,up).equals(correctStack);
		//final boolean changePrilist = !uiPrilistContent().equals(correctPrilist);
		//final List correctActiveStack = new ArrayList(correctStack.subList(0,index+1));
		//final List correctGhostStack = new ArrayList(correctStack.subList(index+1,correctStack.size()));
		final boolean changePrilist = !jlistContent(prilist,up).equals(correctPrilist);
		//if(changeStackNameSelectedName){
			//stack.invalidate();
		//}
		//if(changeStackNameSelectedName || changeStack || changePrilist) SwingUtilities.invokeLater(()->{
		if(changeStack || changePrilist) SwingUtilities.invokeLater(()->{
			if(changeStack) setJListContent(stack, correctStack, up);
			if(changePrilist) setJListContent(prilist, correctPrilist, up);
			
			//FIXME setJListContent keeps selection where it was. 
			
			//if(changeStackNameSelectedName){
			//	stack.repaint();
			//}
		});
	}*/
	protected volatile int acceptSwingStack;
	protected volatile int droppingFromDragSwingStack;
	public synchronized void accept(final String eventAboutThisName){
		if(acceptSwingStack < 0) throw new Err("acceptSwingStack="+acceptSwingStack);
		boolean wasDroppingFromDragSwingStack = droppingFromDragSwingStack > 0;
		if(wasDroppingFromDragSwingStack || acceptSwingStack == 0){
			try{
				acceptSwingStack++;
				if(wasDroppingFromDragSwingStack) droppingFromDragSwingStack++;
				if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("top of accept");
				if(Debug.logAcycEvents) lg("acyc event about name "+eventAboutThisName);
				SwingUtilities.invokeLater(()->{
					if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("top of accept scheduled");
					if(Debug.logSwingInvoke) lg("SwingUtilities.invokeLater OUTER event about name "+eventAboutThisName);
					String prilistNameOrNull = (String) Root.get(stackName).get("selectedName");
					List backedStack = Root.prilist(stackName);
					setJListContentAndSelect(stack, backedStack, up, prilistNameOrNull);
					if(prilistNameOrNull == null){
						//SwingUtilities.invokeLater(()->{
							if(Debug.logSwingInvoke) lg("SwingUtilities.invokeLater INNER NOSELECT about name "+eventAboutThisName);
							setJListContentAndSelect(prilist, Collections.EMPTY_LIST, up, null);
						//});
					}else{
						final List backedPrilist = Root.prilist(prilistNameOrNull);
						final String selectedInPrilist = (String) Root.view(stackName, prilistNameOrNull).get("selectedName");
						//SwingUtilities.invokeLater(()->{
							if(Debug.logSwingInvoke) lg("SwingUtilities.invokeLater INNER event about name "+eventAboutThisName);
							Double prilistScrollYFraction =
								(Double) Root.view(stackName, prilistNameOrNull).get("prilistScrollYFraction");
							if(prilistScrollYFraction == null){
								lg("No prilistScrollYFraction so use 0");
								prilistScrollYFraction = 0.;
							}
							//FIXME old JList contents (from prev prilist) are there when scrolling for next prilist
							final double fraction = prilistScrollYFraction;
							//SwingUtilities.invokeLater(()->{
								//final boolean oldIgnore = ignorePrilistScrollEvent;
								//ignorePrilistScrollEvent = true;
								setJListContentAndSelect(prilist, backedPrilist, up, selectedInPrilist);
								//lg("SCHEDULING Scrolling "+stackName+"'s view of "+prilistNameOrNull+" to "+fraction+" time="+Time.timeStr());
								acceptSwingStack++;
								if(wasDroppingFromDragSwingStack) droppingFromDragSwingStack++;
								SwingUtilities.invokeLater(()->{
									//ignorePrilistScrollEvent = oldIgnore;
									try{
										lg("DOING Scrolling "+stackName+"'s view of "+prilistNameOrNull+" to "+fraction+" time="+Time.timeStr());
										scrollPrilist(fraction);
									}finally{
										acceptSwingStack--;
										if(wasDroppingFromDragSwingStack) droppingFromDragSwingStack--;
									}
								});
							//});
						//});
					}
				});
			}finally{
				acceptSwingStack--;
				if(wasDroppingFromDragSwingStack) droppingFromDragSwingStack--;
				SwingUtilities.invokeLater(()->{ //Was lack of this causing the ConcurrentModificationException?
					listenOnlyToNameOfStackAndPossiblySelectedPrilist(); //TODO optimize: only if selection in stack changed
				});
			}
		}else{
			if(Debug.logDroppingOfPossiblyInfinitelyLoopingAcycEvents)
				lg("\r\nIgnoring acycEvent for "+eventAboutThisName+" because not dropping, and acceptSwingStack="
					+acceptSwingStack+" which would be possible to infinite loop if 2 swing events near in time.\r\n");
		}
	}
	
	/** copy *
	protected void setJListContent(JList j, List content, boolean reverse){
		synchronized(j.getTreeLock()){
			try{
				ignoreListEvents = true;
				Object selected = j.getSelectedValue();
				if(reverse){
					content = new ArrayList(content);
					Collections.reverse(content);
				}
				DefaultListModel model = (DefaultListModel) j.getModel();
				model.clear();
				for(Object o : content) model.addElement(o);
				int newSelectIndex = content.indexOf(selected);
				if(newSelectIndex != -1) j.setSelectedIndex(newSelectIndex);
				j.invalidate();
			}finally{
				ignoreListEvents = false;
			}
		}
	}*/
	
	/** Scroll must be done after this in SwingUtilities.invokeLater */
	protected void setJListContentAndSelect(JList j, List content, boolean reverse, Object select){
		if(Debug.logSwingLock) lg("TREELOCK setJListContentAndSelect");
		synchronized(j.getTreeLock()){
			
			if(Debug.logPrilistScrollFractionFromMap){
				String jl;
				if(j == prilist) jl = "prilist";
				else if(j == stack) jl = "stack";
				else jl = ""+j;
				logPrilistScrollFractionFromMap("setJListContentAndSelect "+jl);
			}
			
			try{
				ignoreStackListEvents = true;
				ignorePrilistScrollEvent = true;
				if(reverse){
					content = new ArrayList(content);
					Collections.reverse(content);
				}
				//DefaultListModel model = (DefaultListModel) j.getModel();
				WholeListAtOnceModel model = (WholeListAtOnceModel)j.getModel();
				
				List oldContent = jlistContent(j,false);
				if(oldContent.equals(content)){
					if(Debug.logJListEvents) lg("Not setting jlist content to what it already is: "+summarizeList(content));
				}else{
					if(Debug.logJListEvents) lg("Setting jlist content to: "+summarizeList(content));
					model.setContent(content);
					/*lg("model.clear");
					model.clear();
					for(Object o : content){
						lg("model.addElement: "+o);
						model.addElement(o);
					}
					*/
					//j.invalidate();
				}
				
				int newSelectIndex = content.indexOf(select);
				int oldSelectIndex = j.getSelectedIndex();
				if(oldSelectIndex == newSelectIndex){
					if(Debug.logJListEvents) lg("Not setting jlist selectedIndex to what it already is: "+oldSelectIndex);
				}else{
					if(Debug.logJListEvents) lg("Setting jlist selectedIndex from "+oldSelectIndex+" to "+newSelectIndex);
					if(newSelectIndex == -1) j.clearSelection();
					else j.setSelectedIndex(newSelectIndex);
				}
			}finally{
				ignoreStackListEvents = false;
				ignorePrilistScrollEvent = false;
				if(Debug.logSwingLock) lg("TREEUNLOCK setJListContentAndSelect");
			}
		}
	}
	
	protected void logPrilistScrollFractionFromMap(String comment){
		String prilistName = (String)Root.get(stackName).get("selectedName");
		if(prilistName == null){
			lg("SCRMAP no prilist selected ("+comment+")");
		}else{
			Double d = (Double) Root.view(stackName,prilistName).get("prilistScrollYFraction");
			lg("SCRMAP prilist="+prilistName+" fraction="+d+" jlistSize="+prilist.getModel().getSize()
				+" obsScroll="+getScrollYFraction(scroll,up)+" ("+comment+")");
		}
	}
	
	protected void scrollPrilist(double prilistScrollYFraction){
		if(Debug.logPrilistScrollFractionFromMap) logPrilistScrollFractionFromMap("top of scrollPrilist "+prilistScrollYFraction);
		double observeScrollYFraction = getScrollYFraction(scroll, up);
		double abs = Math.abs(prilistScrollYFraction-observeScrollYFraction);
		//if(1e-9 < abs){
		if(1e-6 < abs){
			//FIXME will events copy scroll positions across computers
			//without infiniteLoop from being 1 pixel different or things like that?
			//boolean oldIgnore = ignorePrilistScrollEvent;
			//try{
				//ignorePrilistScrollEvent = true;
				
				//FIXME? It appears scrolling creates async event so the value of
				//ignorePrilistScrollEvent here doesnt affect it.
				setScrollYFraction(scroll, up, prilistScrollYFraction);
				
			//}finally{
				//ignorePrilistScrollEvent = oldIgnore;
			//}
		}else{
			if(abs == 0) lg("Not scrolling to "+prilistScrollYFraction+", already there");
			else lg("Not scrolling from "+observeScrollYFraction+" to "+prilistScrollYFraction+" because change is too small");
		}
	}
	
	static String summarizeList(List x){
		if(x.size() < 10) return x.toString();
		String pre = x.subList(0, 3).toString();
		String suf = x.subList(x.size()-3, x.size()).toString();
		return pre.substring(0,pre.length()-1)+" ...listSize"+x.size()+"... "+suf.substring(1);
	}
	
	/** returns immutable list (of possibly mutable content, but normally its strings) */
	public static List jlistContent(JList jlist, boolean reverse){
		WholeListAtOnceModel model = (WholeListAtOnceModel)jlist.getModel();
		return model.getImmutableContent();
		/*DefaultListModel model = (DefaultListModel) jlist.getModel();
		List ret = new ArrayList();
		for(int i=0; i<model.getSize(); i++) ret.add(model.get(i));
		if(reverse) Collections.reverse(ret);
		return ret;
		*/
	}

	/*
	//ListSelectionListener
	public void valueChanged(ListSelectionEvent e){
		if(ignoreListEvents || e.getValueIsAdjusting()) return;
		synchronized(getTreeLock()){
			JList jlist = (JList) e.getSource();
			int i = jlist.getSelectedIndex();
			//if(i != -1){
				DefaultListModel listModel = (DefaultListModel) jlist.getModel();
				final String selectedName = i==-1 ? null : (String) listModel.get(i);
				final boolean popGhosts = e.getSource()!=stack; //keep ghosts if clicking within the stack
				//final boolean popGhosts = false; //FIXME
				SwingUtilities.invokeLater(()->{
					select(selectedName, popGhosts);
				});
			//}
		}
	}*/
	
	/*
	public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e){
    	//TODO lastTouchedWhen = TimeUtil.time();
    	//log("typed "+e);
    	int k = e.getKeyCode();
    	if(e.getComponent() == textfield){
        	if(k == KeyEvent.VK_ENTER) add();
        	else if(k == KeyEvent.VK_UP){
        		int newIndex = jlist.getSelectedIndex()-1;
        		if(newIndex >= 0){
        			jlist.setSelectedValue(listModel.get(newIndex),true);
        		}
        	}else if(k == KeyEvent.VK_DOWN){
        		int newIndex = jlist.getSelectedIndex()+1;
        		if(newIndex < listModel.size()){
        			jlist.setSelectedValue(listModel.get(newIndex),true);
        		}
        	}
    	}else if(e.getComponent() == jlist){
    		if(k == KeyEvent.VK_DELETE || k == KeyEvent.VK_BACK_SPACE || k == KeyEvent.VK_MINUS) del();
    		else if(k == KeyEvent.VK_ENTER || k == KeyEvent.VK_PLUS) add();
    	}
    }

    public void keyReleased(KeyEvent e){}
    */
	
	
	protected static class PrilistTransferHandler extends TransferHandler{
		
		public final PrilistStack p;
		
		public PrilistTransferHandler(PrilistStack p){
			this.p = p;
		}
		
		public boolean canImport(TransferHandler.TransferSupport info){
			return info.isDataFlavorSupported(DataFlavor.stringFlavor);
				//TODO? | info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}
		
		public int getSourceActions(JComponent c){ return TransferHandler.COPY_OR_MOVE; }
		
		protected Transferable createTransferable(JComponent c){
			if(c instanceof JList){
				List selected = ((JList)c).getSelectedValuesList();
				if(selected.size() > 0) return new StringSelection(selected.get(0).toString());
				//else throw new Err("Cant create Transferable from JList because nothing selected in "+c);
				else return new StringSelection("Cant create Transferable from JList because nothing selected in "+c);
			}
			return new StringSelection(c.toString());
		}
		
		public boolean importData(TransferHandler.TransferSupport info){
			if(!info.isDrop()) return false;
			DropLocation dl = info.getDropLocation();
			Component target = info.getComponent();
			if(!(target instanceof JList)) return false;
			JList targetJList = (JList) target;
			//DefaultListModel listModel = (DefaultListModel) targetJList.getModel();
			if(dl instanceof JList.DropLocation){
				JList.DropLocation jdl = (JList.DropLocation)dl;
				if(jdl.isInsert()){
					int insertIndex = ((JList.DropLocation)dl).getIndex();
					//if(info.getComponent() is this vs the other JList)
					Transferable t = info.getTransferable();
					try{
						String nameDropped = (String) t.getTransferData(DataFlavor.stringFlavor);;
						lg("Drop "+nameDropped);
						//copy immutable list to ArrayList
						List<String> uiListContent = new ArrayList(jlistContent(targetJList,false));
						int indexAlreadyInList = uiListContent.indexOf(nameDropped);
						while(indexAlreadyInList != -1){ //prilist must not have duplicates, but just in case
							if(indexAlreadyInList < insertIndex) insertIndex--;
							uiListContent.remove(indexAlreadyInList);
							indexAlreadyInList = uiListContent.indexOf(nameDropped);
						}
						uiListContent.add(insertIndex, nameDropped);
						if(p.up) Collections.reverse(uiListContent);
						//String nameOfParentOfPrilist = returnNameOfParentOfPrilist.get();
						String nameOfParentOfPrilist = (String)Root.get(p.stackName).get("selectedName");
						//if(!Util.prilist(nameOfParentOfPrilist).equals(uiListContent)){
							lg("Setting prilist of "+nameOfParentOfPrilist+" to a size "+uiListContent.size()+" list.");
							try{
								p.droppingFromDragSwingStack++;
								//SwingUtilities.invokeLater(()->{
									Root.setPrilist(nameOfParentOfPrilist, uiListContent);
									p.acycSelectInPrilist(nameDropped);
									Root.addToEndOfPrilistIfNotExist(nameDropped, nameOfParentOfPrilist);
								//});
								//SwingUtilities.invokeLater(()->p.acycSelectInPrilist(nameDropped));
							}finally{
								p.droppingFromDragSwingStack--;
							}
						//}
					}catch(Exception e){
						lgToUser("DnD transferable="+t+"\r\n"+e.getMessage());
						e.printStackTrace(); //TODO? use JSelfModify logging
					}
				}
				
			}
			return false;
		}
		
	};

}