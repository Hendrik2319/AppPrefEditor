package net.schwarzbaer.java.tools.appprefeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.gui.TextAreaDialog;

public class AppPrefEditor {

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
//		int i = 0;
//		showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.class));
//		//showPrefs(i++,Preferences.systemNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.class));
//		//showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.PolarTextOverlay.class));
//		//showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.Dummy.class));
//		showPrefs(i++,Preferences.userRoot());
//		try {
//			showPrefs(i++,Preferences.systemRoot());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		new PreferencesView();
	}

	@SuppressWarnings("unused")
	private static void showPrefs(int index, Preferences prefs) {
		showPrefs(System.out, index, prefs);
	}
	private static void showPrefs(PrintStream out, int index, Preferences prefs) {
		out.printf("Preferences[%d]%n",index);
		showPrefs(out, prefs);
		out.println("[END]");
		out.println();
	}
	private static void showPrefs(PrintStream out, Preferences prefs) {
		if (prefs==null)
			out.printf("<null>%n");
		else {
			String[] keys = null;
			out.printf("Name: \"%s\"%n",prefs.name());
			out.printf("AbsolutePath: \"%s\"%n",prefs.absolutePath());
			out.printf("IsUserNode: %s%n",prefs.isUserNode());
			
			try { out.printf("ChildrenNames: %s%n",Arrays.toString(prefs.childrenNames())); }
			catch (BackingStoreException e) { out.printf("ChildrenNames: <BackingStoreException>%n"); e.printStackTrace(); }
			
			try { out.printf("Keys: %s%n",Arrays.toString(keys = prefs.keys())); }
			catch (BackingStoreException e) { out.printf("Keys: <BackingStoreException>%n"); e.printStackTrace(); }
			
			if (keys!=null)
				for (String key:keys) {
					String value = prefs.get(key,"<unknown value>");
					out.printf("    Value[\"%s\"]: \"%s\"%n", key, value);
				}
		}
	}
	
	private static String prefToString(Preferences prefs) {
		ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(byteArrayOS);
		showPrefs(out, prefs);
		out.flush();
		return byteArrayOS.toString();
	}

	static class PreferencesView {

		private final StandardMainWindow mainwindow;
		private final JTree prefTree;
		private final JTextArea prefOutput;
		private final JList<ValueListModel.Entry> valueList;
		private final FileChooser xmlFileChooser;
		private ValueListModel valueListModel;
		private PreferencesTreeNode selectedTreeNode;
		
		PreferencesView() {
			GridBagConstraints c;
			
			xmlFileChooser = new FileChooser("Preferences Backup", "xml");
			
			prefTree = new JTree(PreferencesTreeNode.createRootNode());
			JScrollPane treeScrollPane = new JScrollPane(prefTree);
			treeScrollPane.setPreferredSize(new Dimension(300,500));
			
			JButton exportBtn, deleteBtn;
			JPanel treeButtonPanel = new JPanel(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill=GridBagConstraints.BOTH;
			c.weightx=0;
			treeButtonPanel.add(            createButton("Refresh"        , true , e->refreshTree()),c);
			treeButtonPanel.add(exportBtn = createButton("Export Selected", false, e->exportSelectedSubTree()),c);
			treeButtonPanel.add(            createButton("Import"         , true , e->importSubTree()),c);
			treeButtonPanel.add(deleteBtn = createButton("Delete Selected", false, e->deleteSelectedSubTree()),c);
			c.weightx=1;
			treeButtonPanel.add(new JLabel(),c);
			
			prefOutput = new JTextArea();
			JScrollPane textScrollPane = new JScrollPane(prefOutput);
			textScrollPane.setBorder(BorderFactory.createTitledBorder("Selected Preferences"));
			textScrollPane.setPreferredSize(new Dimension(300,300));
			
			valueList = new JList<>();
			JScrollPane valueListScrollPane = new JScrollPane(valueList);
			valueListScrollPane.setPreferredSize(new Dimension(300,200));
			
			JButton removeValueBtn,editValueBtn,renameValueBtn;
			JPanel valueListButtonPanel = new JPanel(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill=GridBagConstraints.BOTH;
			c.weightx=0;
			valueListButtonPanel.add(removeValueBtn = createButton("Remove Value",false, e->removeValue(valueList.getSelectedValue())),c);
			valueListButtonPanel.add(  editValueBtn = createButton(  "Edit Value",false, e->  editValue(valueList.getSelectedValue())),c);
			valueListButtonPanel.add(renameValueBtn = createButton("Rename Value",false, e->renameValue(valueList.getSelectedValue())),c);
			c.weightx=1;
			valueListButtonPanel.add(new JLabel(),c);
			
			valueList.addListSelectionListener(e -> {
				ValueListModel.Entry selectedValue = valueList.getSelectedValue();
				removeValueBtn.setEnabled(selectedValue!=null);
				editValueBtn  .setEnabled(selectedValue!=null);
				renameValueBtn.setEnabled(selectedValue!=null);
			});
			
			prefTree.addTreeSelectionListener(e -> {
				selectedTreeNode = null;
				TreePath path = prefTree.getSelectionPath();
				if (path!=null) {
					Object obj = path.getLastPathComponent();
					if (obj instanceof PreferencesTreeNode)
						selectedTreeNode = (PreferencesTreeNode) obj;
				}
				showValues();
				setValueListModel();
				removeValueBtn.setEnabled(false);
				editValueBtn  .setEnabled(false);
				renameValueBtn.setEnabled(false);
				exportBtn.setEnabled(selectedTreeNode!=null);
				deleteBtn.setEnabled(selectedTreeNode!=null && !selectedTreeNode.isRoot());
			});
			
			JPanel treePanel = new JPanel(new BorderLayout(3,3));
			treePanel.setBorder(BorderFactory.createTitledBorder("Preferences Tree"));
			treePanel.add(treeScrollPane,BorderLayout.CENTER);
			treePanel.add(treeButtonPanel,BorderLayout.SOUTH);
			
			JPanel valuePanel = new JPanel(new BorderLayout(3,3));
			valuePanel.setBorder(BorderFactory.createTitledBorder("Preferences Values"));
			valuePanel.add(valueListScrollPane,BorderLayout.CENTER);
			valuePanel.add(valueListButtonPanel,BorderLayout.SOUTH);
			
			JSplitPane rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
			rightPanel.setTopComponent(textScrollPane);
			rightPanel.setBottomComponent(valuePanel);
			rightPanel.setResizeWeight(0.5);
			
			JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.setLeftComponent(treePanel);
			contentPane.setRightComponent(rightPanel);
			contentPane.setResizeWeight(0.5);
			
			mainwindow = new StandardMainWindow("Application Preferences Editor");
			mainwindow.startGUI(contentPane);
			
			expandTree();
		}

		private void refreshTree() {
			prefTree.setModel(new DefaultTreeModel(PreferencesTreeNode.createRootNode()));
			expandTree();
			// TODO Auto-generated method stub
		}

		private void expandTree() {
			for (int i=0; i<prefTree.getRowCount(); i++)
				prefTree.expandRow(i);
		}

		private void exportSelectedSubTree() {
			if (selectedTreeNode==null) return;
			if (xmlFileChooser.showSaveDialog(mainwindow)!=FileChooser.APPROVE_OPTION) return;
			File file = xmlFileChooser.getSelectedFile();
			try (FileOutputStream fileOut = new FileOutputStream(file)) {
				selectedTreeNode.preferences.exportSubtree(fileOut);
			}
			catch (FileNotFoundException ex) { ex.printStackTrace(); }
			catch (IOException ex) { ex.printStackTrace(); }
			catch (BackingStoreException ex) { ex.printStackTrace(); }
		}

		private void importSubTree() {
			if (xmlFileChooser.showOpenDialog(mainwindow)!=FileChooser.APPROVE_OPTION) return;
			File file = xmlFileChooser.getSelectedFile();
			try (FileInputStream fileIn = new FileInputStream(file)) {
				selectedTreeNode = null;
				Preferences.importPreferences(fileIn);
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			catch (InvalidPreferencesFormatException e) { e.printStackTrace(); }
			updateTree();
		}

		private void deleteSelectedSubTree() {
			if (selectedTreeNode==null) return;
			if (selectedTreeNode.isRoot()) return;
			try { selectedTreeNode.preferences.removeNode(); }
			catch (BackingStoreException e) { e.printStackTrace(); }
			updateTree();
		}

		private void updateTree() {
			prefTree.setModel(new DefaultTreeModel(PreferencesTreeNode.createRootNode()));
			expandTree();
			//showValues();
			//setValueListModel();
		}

		private void renameValue(ValueListModel.Entry entry) {
			String newName = JOptionPane.showInputDialog(mainwindow, "Change name of \""+entry.key+"\":", entry.key);
			if (newName==null) return;
			valueListModel.changeName(entry,newName);
		}

		private void editValue(ValueListModel.Entry entry) {
			String newValue = TextAreaDialog.editText(mainwindow, "Set value of \""+entry.key+"\"", 600, 500, true, entry.value);
			//String newValue = JOptionPane.showInputDialog(mainwindow, "Set value of \""+entry.key+"\":", entry.value);
			if (newValue==null) return;
			valueListModel.changeValue(entry,newValue);
		}

		private void removeValue(ValueListModel.Entry entry) {
			int result = JOptionPane.showConfirmDialog(mainwindow, "Do you want to remove \""+entry.key+"\"?", "Remove Entry", JOptionPane.YES_NO_CANCEL_OPTION);
			if (result!=JOptionPane.YES_OPTION) return;
			valueListModel.remove(entry);
		}

		private JButton createButton(String title, boolean enabled, ActionListener al) {
			JButton comp = new JButton(title);
			if (al!=null) comp.addActionListener(al);
			comp.setEnabled(enabled);
			return comp;
		}
		
		private void setValueListModel() {
			if (selectedTreeNode!=null && selectedTreeNode.preferences!=null)
				try {
					valueListModel = new ValueListModel(selectedTreeNode.preferences,selectedTreeNode.preferences.keys());
					valueList.setModel(valueListModel);
					return;
				}
				catch (BackingStoreException e) {}
			valueListModel = null;
			valueList.setModel(new DefaultListModel<>());
		}

		private void showValues() {
			prefOutput.setText("");
			if (selectedTreeNode==null) return;
			if (selectedTreeNode.preferences==null)
				prefOutput.setText("defined as child in parent node\r\nbut has no preferences node");
			else
				prefOutput.setText(prefToString(selectedTreeNode.preferences));
		}
		
		private static class ValueListModel implements ListModel<ValueListModel.Entry> {
			
			private static class Entry {
				private String key,value;
				public Entry(String key, String value) {
					this.key = key;
					this.value = value;
				}
				@Override public String toString() { return String.format("Value[\"%s\"]: \"%s\"", key, value); }
				
			}
			
			private final Vector<ListDataListener> listDataListeners;
			private final Vector<Entry> entries;
			private final Preferences preferences;
			
			ValueListModel(Preferences preferences, String[] keys) {
				this.preferences = preferences;
				entries = new Vector<>();
				for (String key:keys) {
					String value = preferences.get(key,null);
					entries.add( new Entry(key,value) );
				}
				listDataListeners = new Vector<>(); 
			}

			public void changeName(Entry entry, String newName) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				preferences.remove(entry.key);
				entry.key = newName;
				preferences.put(entry.key,entry.value);
				notifyListeners(i, ListDataEvent.CONTENTS_CHANGED, (l,e)->l.contentsChanged(e));
			}

			public void changeValue(Entry entry, String newValue) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				entry.value = newValue;
				preferences.put(entry.key,entry.value);
				notifyListeners(i, ListDataEvent.CONTENTS_CHANGED, (l,e)->l.contentsChanged(e));
			}

			public void remove(Entry entry) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				entries.remove(entry);
				preferences.remove(entry.key);
				notifyListeners(i, ListDataEvent.INTERVAL_REMOVED, (l,e)->l.intervalRemoved(e));
			}

			private void notifyListeners(int index, int eventType, BiConsumer<ListDataListener, ListDataEvent> listenerAction) {
				ListDataEvent e = new ListDataEvent(this, eventType, index,index);
				listDataListeners.forEach(l->listenerAction.accept(l,e));
			}

			@Override public int getSize() { return entries.size(); }
			@Override public Entry getElementAt(int index) { if (index<0 || entries.size()<=index) return null; return entries.get(index); }
			
			@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
			@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }
		}

		private static class PreferencesTreeNode implements TreeNode {

			private final PreferencesTreeNode parent;
			private final String name;
			private final Preferences preferences;
			private final Vector<PreferencesTreeNode> children;
			
			static PreferencesTreeNode createRootNode() {
				return new PreferencesTreeNode(null,"userRoot",Preferences.userRoot());
			}
			
			PreferencesTreeNode(PreferencesTreeNode parent, String name, Preferences preferences) {
				this.parent = parent;
				this.name = name;
				this.preferences = preferences;
				children = new Vector<>();
				if (this.preferences!=null)
					try {
						String[] childrenNames = this.preferences.childrenNames();
						for (String childName:childrenNames) {
							Preferences pref = null;
							if (this.preferences.nodeExists(childName)) pref = this.preferences.node(childName);
							children.add(new PreferencesTreeNode(this,childName,pref));
						}
					} catch (BackingStoreException e) {
						e.printStackTrace();
					}
			}
			
			public boolean isRoot() {
				return preferences.absolutePath().equals("/");
			}
			
			@Override public String toString() { return name; }

			@Override public TreeNode getChildAt(int childIndex) {
				if (childIndex<0 || children.size()<=childIndex) return null;
				return children.get(childIndex);
			}
			
			@Override public Enumeration<PreferencesTreeNode>  children() { return children.elements(); }
			@Override public int getChildCount() { return children.size(); }
			@Override public TreeNode getParent() { return parent; }
			@Override public int getIndex(TreeNode node) { return children.indexOf(node); }
			@Override public boolean getAllowsChildren() { return true; }
			@Override public boolean isLeaf() { return children.isEmpty(); }
		}
	
	}
}
