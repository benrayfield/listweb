package listweb.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import humanaicore.common.Text;
import humanaicore.err.Todo;
import listweb.Options;
import listweb.Root;

/** auto saves changes when closed, so make sure to keep backups often. */
public class TwoPrilistsPanel extends JPanel{
	
	public final PrilistStack up, down;
	
	protected final JTextField textSearch;
	
	protected final JComboBox textContext;
	
	//protected final JCheckBox chkAlsoSearchDefs;
	
	protected boolean regexErr;
	
	/*"Will mindmap break (considering I designed the events) if editing or viewing the prilist of its own stack?"
	"TODO PrilistStack must use 2 prilists as its stack, or maybe each name can have a prilist and prilistGhost. Also, how to store which was selected in a prilist to restore it?"
	"Should a prilist's stack display its top item? If its always prilistPrilist, dont need to, but if its potentially different each time, like rightclick menu on different objects, then would store many such stacks."
	"Need the ability to view the same prilist in 2 different prilistStacks at 2 different scrollPositions"
	"scrollPositions should be either integer or string, for how far up/down the stack to view, from screen center considering that prilistStackUi can be up or down. This will as long as the scroll positions are stored separately per prilistStack. But I dont want to store extremely many scroll positions in each stack since any name can be a stack."
	*/
	
	public TwoPrilistsPanel(final String topStackName, String bottomStackName, String selectPtr){
		setLayout(new GridLayout(2, 1));
		up = new PrilistStack(topStackName, selectPtr, true);
		down = new PrilistStack(bottomStackName, selectPtr, false);
		JPanel upPanel = new JPanel(new BorderLayout());
		textSearch = new JTextField();
		textSearch.getDocument().addDocumentListener(new DocumentListener(){
			public void insertUpdate(DocumentEvent e){ onChange(); }
			public void removeUpdate(DocumentEvent e){ onChange(); }
			public void changedUpdate(DocumentEvent e){}
		});
		textContext = new JComboBox(new String[]{"find all","regex","command"});
		ActionListener a = new ActionListener(){
			public void actionPerformed(ActionEvent e){ onChange(); }
		};
		textContext.addActionListener(a);
		//chkAlsoSearchDefs = new JCheckBox();
		//chkAlsoSearchDefs.setToolTipText("also search defs (if \"find all\" or \"regex\")");
		//chkAlsoSearchDefs.addActionListener(a);
		JPanel p = new JPanel(new BorderLayout());
		p.add(textSearch, BorderLayout.CENTER);
		JPanel midRight = new JPanel(new BorderLayout());
		//midRight.add(textContext, BorderLayout.CENTER);
		//midRight.add(chkAlsoSearchDefs, BorderLayout.EAST);
		//p.add(midRight, BorderLayout.EAST);
		p.add(textContext, BorderLayout.EAST);
		upPanel.add(p, BorderLayout.SOUTH);
		upPanel.add(up, BorderLayout.CENTER);
		add(upPanel);
		add(down);
	}
	
	protected void onChange(){
		String s = textSearch.getText();
		final String out = command(
			textSearch.getText(), (String)textContext.getSelectedItem(), Options.searchInDefs());
		final int c = textSearch.getCaretPosition();
		SwingUtilities.invokeLater(()->{
			textContext.setBackground(regexErr ? Color.gray : Color.white);
			if(!s.equals(out)){
				textSearch.setText(out);
				textSearch.setCaretPosition(Math.min(c, out.length()));
			}
		});
	}
	
	/** returns the text to replace the command as you type it.
	FIXME TODO this should be done in a "command" property of some mindmapItem,
	such as the up stack or another ptr in constructor, and let events cause the command
	whenever the text changes, which may replace that someptr.command therefore update display in ui.
	FIXME TODO Similar for each AddTextRemove?
	*/
	protected String command(String s, String context, boolean alsoSearchDefs){
		if(alsoSearchDefs) return "TODO for future expansion";
		regexErr = false;
		boolean isFindAll = context.equals("find all");
		boolean isRegex = context.equals("regex");
		boolean isCommand = context.equals("command");
		if(isCommand) return "TODO for future expansion";
		if(s.endsWith("  ")) return "";
		if(s.trim().length() > 1){
			final String tokens[] = Text.splitByWhitespaceNoEmptyTokens(s);
			
			Predicate<String> hardQuery = null;
			if(isFindAll){
				hardQuery = (String name)->{
					name = name.toLowerCase();
					for(String token : tokens){
						if(!name.contains(token.toLowerCase())) return false;
					}
					return true;
				};
			}else if(isRegex){
				try{
					final Pattern regex = Pattern.compile(s);
					hardQuery = (String name)->{
						return regex.matcher(name).matches();
					};
				}catch(PatternSyntaxException e){
					regexErr = true;
				}
			}	
			else throw new Todo("Unknown search/command context: "+context);
			
			Comparator<String> sort = new Comparator<String>(){
				public int compare(String x, String y){
					if(x.length() < y.length()) return -1; //prefer shorter
					if(x.length() > y.length()) return 1;
					return x.compareTo(y); //if same len, compare strings as usual
				}
			};
			if(hardQuery != null){
				String searchResults[] = Root.namesCache //instead of Root.listweb.keySet()
					.stream()
					.filter(hardQuery)
					.sorted(sort)
					.toArray((int siz)->{return new String[siz];});
				Root.setPrilist("searchResults", new ArrayList(Arrays.asList(searchResults)));
				boolean popGhosts = !Root.prilist(up.stackName).contains("searchResults");
				up.acycSelectInStack("searchResults", popGhosts);
				//up.select("searchResults", false);
				//System.out.println("Search results: "+Arrays.asList(searchResults));
			}
		}
		return s;
	}

}