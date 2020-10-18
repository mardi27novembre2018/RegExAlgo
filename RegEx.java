import java.util.Scanner;

import javax.lang.model.util.ElementScanner6;

import java.util.Map;

//import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.List;
import java.lang.Exception;

public class RegEx {
  //MACROS
  static final int CONCAT = 0xC04CA7;
  static final int ETOILE = 0xE7011E;
  static final int ALTERN = 0xA17E54;
  static final int PROTECTION = 0xBADDAD;

  static final int PARENTHESEOUVRANT = 0x16641664;
  static final int PARENTHESEFERMANT = 0x51515151;
  static final int DOT = 0xD07;
  
  //REGEX
  private static String regEx;
  
  //CONSTRUCTOR
  public RegEx(){}

  //MAIN
  public static void main(String arg[]) {
    System.out.println("Welcome to Bogota, Mr. Thomas Anderson.");
    if (arg.length!=0) {
      regEx = arg[0];
    } else {
      Scanner scanner = new Scanner(System.in);
      System.out.print("  >> Please enter a regEx: ");
      regEx = scanner.next();
    }
    System.out.println("  >> Parsing regEx \""+regEx+"\".");
    System.out.println("  >> ...");
    
    if (regEx.length()<1) {
      System.err.println("  >> ERROR: empty regEx.");
    } else {
      System.out.print("  >> ASCII codes: ["+(int)regEx.charAt(0));
      for (int i=1;i<regEx.length();i++) System.out.print(","+(int)regEx.charAt(i));
      System.out.println("].");
      try {
        RegExTree ret = parse();
        Automata a = new Automata(ret);
        System.out.println(a.toString());
        System.out.println(a.toStringTab());
        System.out.println("  >> Tree result: "+ret.toString()+".");
      } catch (Exception e) {
        System.err.println("  >> ERROR: syntax error for regEx \""+regEx+"  cause = "+e.toString()+"\".");
        e.printStackTrace();
      }
    }
    

    System.out.println("  >> ...");
    System.out.println("  >> Parsing completed.");
    System.out.println("Goodbye Mr. Anderson.");
  }

  //FROM REGEX TO SYNTAX TREE
  private static RegExTree parse() throws Exception {
    //BEGIN DEBUG: set conditionnal to true for debug example
    if (false) throw new Exception();
    RegExTree example = exampleAhoUllman();
    if (false) return example;
    //END DEBUG

    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    for (int i=0;i<regEx.length();i++) result.add(new RegExTree(charToRoot(regEx.charAt(i)),new ArrayList<RegExTree>()));
    
    return parse(result);
  }
  private static int charToRoot(char c) {
    if (c=='.') return DOT;
    if (c=='*') return ETOILE;
    if (c=='|') return ALTERN;
    if (c=='(') return PARENTHESEOUVRANT;
    if (c==')') return PARENTHESEFERMANT;
    return (int)c;
  }
  private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
    while (containParenthese(result)) result=processParenthese(result);
    while (containEtoile(result)) result=processEtoile(result);
    while (containConcat(result)) result=processConcat(result);
    while (containAltern(result)) result=processAltern(result);

    if (result.size()>1) throw new Exception();

    return removeProtection(result.get(0));
  }
  private static boolean containParenthese(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==PARENTHESEFERMANT || t.root==PARENTHESEOUVRANT) return true;
    return false;
  }
  private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    for (RegExTree t: trees) {
      if (!found && t.root==PARENTHESEFERMANT) {
        boolean done = false;
        ArrayList<RegExTree> content = new ArrayList<RegExTree>();
        while (!done && !result.isEmpty())
          if (result.get(result.size()-1).root==PARENTHESEOUVRANT) { done = true; result.remove(result.size()-1); }
          else content.add(0,result.remove(result.size()-1));
        if (!done) throw new Exception();
        found = true;
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(parse(content));
        result.add(new RegExTree(PROTECTION, subTrees));
      } else {
        result.add(t);
      }
    }
    if (!found) throw new Exception();
    return result;
  }
  private static boolean containEtoile(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==ETOILE && t.subTrees.isEmpty()) return true;
    return false;
  }
  private static ArrayList<RegExTree> processEtoile(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    for (RegExTree t: trees) {
      if (!found && t.root==ETOILE && t.subTrees.isEmpty()) {
        if (result.isEmpty()) throw new Exception();
        found = true;
        RegExTree last = result.remove(result.size()-1);
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(last);
        result.add(new RegExTree(ETOILE, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static boolean containConcat(ArrayList<RegExTree> trees) {
    boolean firstFound = false;
    for (RegExTree t: trees) {
      if (!firstFound && t.root!=ALTERN) { firstFound = true; continue; }
      if (firstFound) if (t.root!=ALTERN) return true; else firstFound = false;
    }
    return false;
  }
  private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    boolean firstFound = false;
    for (RegExTree t: trees) {
      if (!found && !firstFound && t.root!=ALTERN) {
        firstFound = true;
        result.add(t);
        continue;
      }
      if (!found && firstFound && t.root==ALTERN) {
        firstFound = false;
        result.add(t);
        continue;
      }
      if (!found && firstFound && t.root!=ALTERN) {
        found = true;
        RegExTree last = result.remove(result.size()-1);
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(last);
        subTrees.add(t);
        result.add(new RegExTree(CONCAT, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static boolean containAltern(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==ALTERN && t.subTrees.isEmpty()) return true;
    return false;
  }
  private static ArrayList<RegExTree> processAltern(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    RegExTree gauche = null;
    boolean done = false;
    for (RegExTree t: trees) {
      if (!found && t.root==ALTERN && t.subTrees.isEmpty()) {
        if (result.isEmpty()) throw new Exception();
        found = true;
        gauche = result.remove(result.size()-1);
        continue;
      }
      if (found && !done) {
        if (gauche==null) throw new Exception();
        done=true;
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(gauche);
        subTrees.add(t);
        result.add(new RegExTree(ALTERN, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static RegExTree removeProtection(RegExTree tree) throws Exception {
    if (tree.root==PROTECTION && tree.subTrees.size()!=1) throw new Exception();
    if (tree.subTrees.isEmpty()) return tree;
    if (tree.root==PROTECTION) return removeProtection(tree.subTrees.get(0));

    ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
    for (RegExTree t: tree.subTrees) subTrees.add(removeProtection(t));
    return new RegExTree(tree.root, subTrees);
  }
  
  //EXAMPLE
  // --> RegEx from Aho-Ullman book Chap.10 Example 10.25
  private static RegExTree exampleAhoUllman() {
    RegExTree a = new RegExTree((int)'a', new ArrayList<RegExTree>());
    RegExTree b = new RegExTree((int)'b', new ArrayList<RegExTree>());
    RegExTree c = new RegExTree((int)'c', new ArrayList<RegExTree>());
    ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
    subTrees.add(c);
    RegExTree cEtoile = new RegExTree(ETOILE, subTrees);
    subTrees = new ArrayList<RegExTree>();
    subTrees.add(b);
    subTrees.add(cEtoile);
    RegExTree dotBCEtoile = new RegExTree(CONCAT, subTrees);
    subTrees = new ArrayList<RegExTree>();
    subTrees.add(a);
    subTrees.add(dotBCEtoile);
    return new RegExTree(ALTERN, subTrees);
  }
}

//UTILITARY CLASS
class RegExTree {
  protected int root;
  protected ArrayList<RegExTree> subTrees;
  public RegExTree(int root, ArrayList<RegExTree> subTrees) {
    this.root = root;
    this.subTrees = subTrees;
  }
  //FROM TREE TO PARENTHESIS
  public String toString() {
    if (subTrees.isEmpty()) return rootToString();
    String result = rootToString()+"("+subTrees.get(0).toString();
    System.out.println("size == "+subTrees.size());
    for (int i=1;i<subTrees.size();i++) result+=","+subTrees.get(i).toString();
    for(int i=1; i<subTrees.size();i++) result+="";
    return result+")";
  }
  private String rootToString() {
    if (root==RegEx.CONCAT) return ".";
    if (root==RegEx.ETOILE) return "*";
    if (root==RegEx.ALTERN) return "|";
    if (root==RegEx.DOT) return ".";
    return Character.toString((char)root);
  }

  
  public ArrayList<RegExTree> getSubTrees(){
      return this.subTrees;
  }

  public int getRoot()
  {
      return this.root;
  }
}


//Classe representant les noeuds d'un automate non deterministe
class AutomataNodeND {
	
	public int id;
	public RegExTree tree;
	public boolean acceptance; //determine si le noeud est celui d'acceptation de l'automate
	public Map<Integer,ArrayList<AutomataNodeND>> transitions; //dictionnaire des transtions liée aux etats d'arrivees
	
	public AutomataNodeND(int id, RegExTree tree) {
		this.tree = tree;
		this.id = id;
		this.acceptance = false;
		this.transitions = new HashMap<Integer,ArrayList<AutomataNodeND>>();
	}
	
	public RegExTree getTree() {
		return this.tree;
	}
	
	public void setAcceptance() {
		this.acceptance = true;
	}
	
	public boolean isAcceptance() {
		return this.acceptance;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void addTransition(int trans, AutomataNodeND arr) {
		if(transitions.containsKey(trans)) {
			transitions.get(trans).add(arr);
		}
		else {
			ArrayList<AutomataNodeND> ann = new ArrayList<AutomataNodeND>();
			ann.add(arr);
			transitions.put(trans,ann);
		}
	}
	
	public ArrayList<AutomataNodeND> getTransition(int trans){
		return this.transitions.get(trans);
	}
	
	public Map<Integer,ArrayList<AutomataNodeND>> getTransitions(){
		return this.transitions;
	}
}

//Classe representant le noeud d'un automate deterministe
class AutomataNodeD{
	public ArrayList<AutomataNodeD> ancetres; //repertorie les noeuds ancetres a celui ci
	public ArrayList<AutomataNodeND> courant; //repertorie les etats du noeud courant
	public Map<Integer,AutomataNodeD> liens;
	public ArrayList<String> chemins;
	public ArrayList<RegExTree> tree;
	public boolean acceptance; //determine si le noeud est celui d'acceptation de l'automate
	public boolean recursif; //determine si le noeud est recursif
	public boolean redirect; // determine si un noeud redirigie vers un autre
	public AutomataNodeD redirection; 
	
	public AutomataNodeD() {
		this.tree = new ArrayList<RegExTree>();
		this.acceptance = false;
		this.redirect = false;
		this.recursif = false;
		this.ancetres = new ArrayList<AutomataNodeD>();
		this.courant = new ArrayList<AutomataNodeND>();
		this.liens = new HashMap<Integer,AutomataNodeD>();
	}
	
	public void setCh(ArrayList<String> ch) {
		this.chemins = ch;
	}
	
	public ArrayList<String> getCh() {
		return this.chemins;
	}
	
	public void addChemin(RegExTree tree) {
		this.tree.add(tree);
	}
	
	public ArrayList<RegExTree> getTree(){
		return this.tree;
	}
	
	public void setAncetre(ArrayList<AutomataNodeD> anc) {
		this.ancetres = anc;
	}
	
	public void setCourant(ArrayList<AutomataNodeND> courant) {
		this.courant = courant;
	}
	
	public AutomataNodeD getRedirection() {
		return this.redirection;
	}
	
	public void setRedirection(AutomataNodeD redir) {
		this.redirect = true;
		this.redirection = redir;
	}
	
	public boolean isRedirection() {
		return this.redirect;
	}
	
	public ArrayList<AutomataNodeD> getAncetres(){
		return this.ancetres;
	}
	
	public void cloneAncetres(ArrayList<AutomataNodeD> ancetres) {
		this.ancetres = (ArrayList<AutomataNodeD>)ancetres.clone();
	}
	
	public void setAcceptance() {
		this.acceptance = true;
	}
	
	public boolean isAcceptance() {
		return this.acceptance;
	}
	
	public void setRecursif() {
		this.recursif = true;
	}
	
	public boolean isRecursif() {
		return this.recursif;
	}
	
	public void addCourant(AutomataNodeND link) {
		this.courant.add(link);
	}
	public void addLink(int trans, AutomataNodeD link) {
		this.liens.put(trans, link);
	}
	public void delLink(int trans) {
		this.liens.remove(trans);
	}
	
	public ArrayList<AutomataNodeND> getCourant(){
		return this.courant;
	}
	public Map<Integer,AutomataNodeD> getLinks(){
		return this.liens;
	}
	public AutomataNodeD getLink(int trans){
		return this.liens.get(trans);
	}
	public boolean isAncestre(int i) {
		return this.ancetres.contains(i);
	}
}


class Automata 
{
	private static final int ID_EPSILON_TRANSITION = -1;
	
	private AutomataNodeND start_node;
    private AutomataNodeND final_node;
    private Map<ArrayList<String>,AutomataNodeD> path_node;
    private int id_node;
    private AutomataNodeD racine_det; // premier noeud du tableau deterministe
    private ArrayList<Integer> transitions_c; //liste des transitions de l'automate

    public Automata(RegExTree mytree){
    	id_node = 0;
    	path_node = new HashMap<ArrayList<String>,AutomataNodeD>();
        start_node = new AutomataNodeND(id_node,mytree);
        id_node++;
        final_node = new AutomataNodeND(id_node,new RegExTree(-2,new ArrayList<RegExTree>()));
        id_node++;
        final_node.setAcceptance();
        
        this.transitions_c = new ArrayList<Integer>();
        racine_det = new AutomataNodeD();
        racine_det.addChemin(mytree);
        toAutomata(mytree,start_node,final_node,new RegExTree(-2,new ArrayList<RegExTree>()));
        detTabStart();
        toMap(racine_det,new ArrayList<AutomataNodeD>());
        System.out.println("First\n");
        for(ArrayList<String> cle : this.path_node.keySet()) {
        	System.out.println("cle="+cle+"\n");
        }
        red(racine_det,new ArrayList<AutomataNodeD>());
        /*opt(racine_det,new ArrayList<AutomataNodeD>());
        System.out.println("Second\n");
        for(ArrayList<String> cle : this.path_node.keySet()) {
        	System.out.println("cle="+cle+"\n");
        }
        red(racine_det,new ArrayList<AutomataNodeD>());
        System.out.println("Third\n");
        for(ArrayList<String> cle : this.path_node.keySet()) {
        	System.out.println("cle="+cle+"\n");
        }*/
    }
    
    public String ArraytoString(ArrayList<AutomataNodeND> an) {
    	String chaine = "";
    	for(AutomataNodeND a : an) {
    		chaine += " _ " +a.getId(); 
    	}
    	return chaine;
    }
    
    public String toStringRec(AutomataNodeND aut, ArrayList<Integer> deja) {
    	String chaine = "";
    	if(!deja.contains(aut.getId())) {
    		chaine += aut.getId();
    		deja.add(aut.getId());
    		for(int a : aut.getTransitions().keySet()) {
    			chaine += "  n"+ (char)a+" -> "+ ArraytoString(aut.getTransition(a));
    		}
    		chaine += "    \n";
    		for(int a : aut.getTransitions().keySet()) {
    			for(AutomataNodeND au : aut.getTransition(a)) {
        			chaine += toStringRec(au, deja);
        		}
    		}
    	}
    	return chaine;
    }
    
    public String toStringRecTab(AutomataNodeD n, String chemin, ArrayList<AutomataNodeD> deja) {
    	String chaine = "";
    	if(deja.contains(n)) {
    		return chaine;
    	}
    	deja.add(n);
    	
    	if((!n.isRecursif()) && (!n.isAcceptance())){
    		chaine += chemin + " : ";
    	} else if(n.isRecursif() && n.isAcceptance()) {
    		chaine += chemin + "-RF" + " : ";
    	} else if(n.isRecursif()) {
    		chaine += chemin + "-R" + " : ";
    	} else {
    		chaine += chemin + "-F" + " : ";
    	}
    	for(AutomataNodeND l : n.getCourant()) {
    		chaine += " _ "+l.getId();
    	}
    	chaine += "\n";
    	
    	for(int noeudi : n.getLinks().keySet()) {
    		chaine += toStringRecTab(n.getLink(noeudi),chemin+"-"+(char)noeudi,deja );
    	}
    	
    	return chaine;
    }
    
    public String toString() {
    	String chaine = "Chaque ligne correspond a la liste des liens d'un noeud.\n n-1 correspond a la transition epsilon.\n";
    	ArrayList<Integer> deja = new ArrayList<Integer>();//cette arraylist servira a identifier les noeuds deja rencontree

    	return chaine+toStringRec(start_node, deja);
    }
    
    public String toStringTab() {
    	String chaine = "Chaque ligne correspond a un noeud suivie par les etats de ce noeud.\n\n";
    	ArrayList<AutomataNodeD> deja = new ArrayList<AutomataNodeD>();//cette arraylist servira a identifier les noeuds deja rencontree
    	return chaine+toStringRecTab(this.racine_det,"0",deja);
    }


    private void toAutomata(RegExTree tree, AutomataNodeND start_node, AutomataNodeND final_node, RegExTree ts){	
    	//Cas où il s'agit d'une operation
    	RegExTree new_tree = new RegExTree( -2 , new ArrayList<RegExTree>());
    	
    	if (tree.getRoot()==RegEx.CONCAT || tree.getRoot()==RegEx.DOT) {
    		new_tree.subTrees.add(tree.getSubTrees().get(1));
    		new_tree.subTrees.add(ts);
    		
    		AutomataNodeND node1 = new AutomataNodeND(id_node, new_tree);
    		id_node++;
    		
    		AutomataNodeND node2 = new AutomataNodeND(id_node, new_tree);
    		id_node++;

    		
    		node1.addTransition(ID_EPSILON_TRANSITION, node2);
    		toAutomata(tree.getSubTrees().get(0),start_node,node1,new_tree);
    		toAutomata(tree.getSubTrees().get(1),node2,final_node,ts);
    	}
        if (tree.getRoot()==RegEx.ETOILE) {
        	RegExTree new_tree1 = new RegExTree( RegEx.ETOILE , new ArrayList<RegExTree>());
        	new_tree.subTrees.add(tree.getSubTrees().get(0));
    		new_tree.subTrees.add(ts);
        	new_tree1.subTrees.add(ts);
    		
        	AutomataNodeND node1 = new AutomataNodeND(id_node, new_tree);
    		id_node++;
    		AutomataNodeND node2 = new AutomataNodeND(id_node, new_tree1);
    		id_node++;
    		
    		
    		node2.addTransition(ID_EPSILON_TRANSITION, node1);
    		start_node.addTransition(ID_EPSILON_TRANSITION, node1);
    		node2.addTransition(ID_EPSILON_TRANSITION, final_node);
    		start_node.addTransition(ID_EPSILON_TRANSITION, final_node);
    		toAutomata(tree.getSubTrees().get(0),node1,node2,ts);
        }
        if (tree.getRoot()==RegEx.ALTERN) {
        	
        	RegExTree new_tree1 = new RegExTree( -2 , new ArrayList<RegExTree>());
        	
        	new_tree.subTrees.add(tree.getSubTrees().get(0));
    		new_tree.subTrees.add(ts);
    		
    		new_tree1.subTrees.add(tree.getSubTrees().get(1));
    		new_tree1.subTrees.add(ts);
        	
        	AutomataNodeND node1 = new AutomataNodeND(id_node, new_tree);
    		id_node++;
    		
    		AutomataNodeND node2 = new AutomataNodeND(id_node, ts);
    		id_node++;
    		
    		AutomataNodeND node3 = new AutomataNodeND(id_node, new_tree1);
    		id_node++;
    		
    		AutomataNodeND node4 = new AutomataNodeND(id_node, ts);
    		id_node++;
    		
    		start_node.addTransition(ID_EPSILON_TRANSITION, node1);
    		start_node.addTransition(ID_EPSILON_TRANSITION, node3);
    		
    		node2.addTransition(ID_EPSILON_TRANSITION, final_node);
    		node4.addTransition(ID_EPSILON_TRANSITION, final_node);
    		toAutomata(tree.getSubTrees().get(0),node1,node2,ts);
    		toAutomata(tree.getSubTrees().get(1),node3,node4,ts);
        }
        
    	//Cas où il s'agit d'une feuille
    	if(tree.getSubTrees().isEmpty()) {
    		final_node.tree.getSubTrees().add(new RegExTree(tree.getRoot(),new ArrayList<RegExTree>()));
    		start_node.addTransition(tree.getRoot(),final_node);
    		this.transitions_c.add(tree.getRoot());
    	}
    	
    }
    
    public void detTabStart() {
        ArrayList<AutomataNodeND> anc = new ArrayList<AutomataNodeND>();
        ArrayList<AutomataNodeD> ancnd = new ArrayList<AutomataNodeD>();
		ancnd.add(racine_det);
    	this.racine_det.addCourant(start_node);
    	int i;
    	anc.add(start_node);
    	
    	for(i = 0; i<anc.size();i++) {
    		ArrayList<AutomataNodeND> a1 = anc.get(i).getTransition(ID_EPSILON_TRANSITION);
    		if(a1==null) 
    			continue;
    		
    		for(AutomataNodeND a : a1) {
    			if(!anc.contains(a)) {
	    			anc.add(a);
	    			this.racine_det.addCourant(a);
	    			if(a.isAcceptance()) {
	    				racine_det.setAcceptance();
	    			}
	    		}
        	}
    	}
    	
    	for(int trans : this.transitions_c) {
    		AutomataNodeD noeud = new AutomataNodeD();
    		noeud.setAncetre(ancnd);
    		this.racine_det.addLink(trans, noeud);
    		detTab((ArrayList<AutomataNodeND>) anc.clone(), noeud, trans);
    		if(noeud.getCourant().isEmpty()) {
    			this.racine_det.delLink(trans);
    		}
    		else if(noeud.isRedirection()) {
    			this.racine_det.delLink(trans);
    			if(noeud.getRedirection() == racine_det) {
    				noeud.setRecursif();
    			}
    			else {
    				this.racine_det.addLink(trans, noeud.getRedirection());
    			}
    		}
    	}
    }
    
    //determination de l'automate deterministe
    public void detTab(ArrayList<AutomataNodeND> ancetres_direct, AutomataNodeD noeud, int current_link) {
    	ArrayList<AutomataNodeND> etats_courant = new ArrayList<AutomataNodeND>();
    	int i;
    	
    	for(AutomataNodeND ad : ancetres_direct) {
    		ArrayList<AutomataNodeND> at = ad.getTransition(current_link);
    		if(at==null)
    			continue;
    		for(AutomataNodeND a : at) {
    			if(!etats_courant.contains(a)) {
	    			etats_courant.add(a);
	    			noeud.addChemin(a.getTree());
	    			if(a.isAcceptance()) {
	    				noeud.setAcceptance();
	    			}
	    		}
        	}
    	}
    	
    	for(i = 0; i<etats_courant.size();i++) {
    		ArrayList<AutomataNodeND> at = etats_courant.get(i).getTransition(ID_EPSILON_TRANSITION);
    		if(at==null)
    			continue;
    		for(AutomataNodeND a : at) {
    			if(!etats_courant.contains(a)) {
	    			etats_courant.add(a);
	    			if(a.isAcceptance()) {
	    				noeud.setAcceptance();
	    			}
	    		}
        	}
    	}
    	
    	noeud.setCourant(etats_courant);
    	int k = 0;
    	if(etats_courant.size()==0)
    		return;
    	
    	//On verifie qu'un ancetre ne soit pas le duplicata de celui ci
    	for(AutomataNodeD aut : noeud.getAncetres()) {
    		for(AutomataNodeND cour : etats_courant) {
    			if(aut.getCourant().contains(cour))
    				k++;
    		}
    		if(k==etats_courant.size()) {
    			noeud.setRedirection(aut);
    			return;
    		}
    		k=0;
    	}
    	
    	for(int trans : this.transitions_c) {
    		AutomataNodeD noeud1 = new AutomataNodeD();
    		noeud.addLink(trans, noeud1);
    		ArrayList<AutomataNodeD> l = (ArrayList<AutomataNodeD>) noeud.getAncetres().clone();
    		l.add(noeud);
    		noeud1.setAncetre(l);
    		detTab((ArrayList<AutomataNodeND>) etats_courant.clone(), noeud1, trans);
    		
    		if(noeud1.getCourant().isEmpty()) {
    			noeud.delLink(trans);
    		}
    		if(noeud1.isRedirection()) {
    			noeud.delLink(trans);
    			if(noeud1.getRedirection() == noeud) {
    				noeud.setRecursif();
    			}
    			else {
    				noeud.addLink(trans, noeud1.getRedirection());
    			}
    		}
    	}
    }

    //Mise en place des redirections
    public void red(AutomataNodeD node, ArrayList<AutomataNodeD> deja) {
    			deja.add(node);
    	for(int key : node.getLinks().keySet()) {
    		if(node.getLink(key).isRedirection()) {
    			node.addLink(key, node.getLink(key).getRedirection());

    		}
    		if(!deja.contains(node.getLink(key))) {
    			red(node.getLink(key),deja);
    		}
    	}
    }
    
    //Optimisation de l'automate deterministe
    public void opt(AutomataNodeD node, ArrayList<AutomataNodeD> deja) {
    	int k = 0;
    	for(ArrayList<String> l : this.path_node.keySet()) {
    		if(node==this.path_node.get(l)) {
    			continue;
    		}
    		for(String c : node.getCh()) {
    			if(l.contains(c))
    				k++;
    		}
    		if(k==node.getCh().size()) {
    			node.setRedirection(this.path_node.get(l));
    			break;
    		}
    		k=0;
    	}
    	
    	for(int key : node.getLinks().keySet()) {
    		if(!deja.contains(node.getLink(key))) {
    			deja.add(node.getLink(key));
    			opt(node.getLink(key),deja);
    		}
    	}
    }
    //Conversion des noeuds de l'automate deterministe en chaine de caractere
    public void toMap(AutomataNodeD node, ArrayList<AutomataNodeD> deja) {
		deja.add(node);
    	ArrayList<String> cles = new ArrayList<String>();
    	
    	for(RegExTree t : node.getTree()){
    		cles.addAll(tabStr(t,""));
    	}
    	while(cles.remove(""));
    	node.setCh(cles);
    	
    	if(!this.path_node.containsKey(cles)) {
    		this.path_node.put(cles, node);
    	}
    	else {
    		node.setRedirection(this.path_node.get(cles));
    	}
    	
    	for(int key : node.getLinks().keySet()) {
    		if(!deja.contains(node.getLink(key))) {
    			toMap(node.getLink(key),deja);
    		}
    	}
    }
    
    //Fonction recursif convertissant un arbre en un tableau de string
    public ArrayList<String> tabStr(RegExTree tree, String chemin){
    	int c = tree.getRoot();
    	ArrayList<String> l = new ArrayList<String>();
    	
    	if(tree.getSubTrees().size()==0) {
    			if(c==-2) {
    				l.add(chemin);
    			}else {
    				l.add(chemin+"."+c);
    			}
    			return l;
   		}
   		for(RegExTree t : tree.getSubTrees()) {
    		if(c==-2) {
    			l.addAll(tabStr(t,chemin));
    		} else {
    			l.addAll(tabStr(t,chemin+"."+c));
    		}
    	}
    	
    
    	return l;
    }
}
