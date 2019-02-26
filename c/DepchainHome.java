package softtest.depchain.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.attribute.SetOfIntegerSyntax;
import javax.sound.midi.SysexMessage;

import org.apache.tools.ant.filters.FixCrLfFilter.AddAsisRemove;
import org.apache.tools.ant.taskdefs.PathConvert.MapEntry;
import org.jaxen.JaxenException;

import org.omg.CORBA.PRIVATE_MEMBER;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.jndi.cosnaming.IiopUrl.Address;
import com.sun.org.apache.bcel.internal.generic.IFGE;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.sun.org.apache.bcel.internal.generic.VariableLengthInstruction;
import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;
import com.sun.swing.internal.plaf.basic.resources.basic;
import com.sun.tracing.dtrace.ArgsAttributes;
import com.sun.tracing.dtrace.FunctionName;
import com.sun.xml.internal.bind.v2.model.core.ID;
import com.sun.xml.internal.bind.v2.runtime.Name;
import com.sun.xml.internal.ws.message.StringHeader;

import edu.emory.mathcs.backport.java.util.Arrays;
import softtest.CharacteristicExtract.c.Graph_Info;
import softtest.CharacteristicExtract.c.StatementFeature;
import softtest.CharacteristicExtract.c.test;
import softtest.DefUseAnalysis.c.DUAnalysisVisitor;
import softtest.ast.c.ASTAdditiveExpression;
import softtest.ast.c.ASTArgumentExpressionList;
import softtest.ast.c.ASTAssignmentExpression;
import softtest.ast.c.ASTCompoundStatement;
import softtest.ast.c.ASTConstant;
import softtest.ast.c.ASTDeclaration;
import softtest.ast.c.ASTDeclarator;
import softtest.ast.c.ASTDirectDeclarator;
import softtest.ast.c.ASTExpression;
import softtest.ast.c.ASTExpressionStatement;
import softtest.ast.c.ASTFunctionDeclaration;
import softtest.ast.c.ASTFunctionDefinition;
import softtest.ast.c.ASTInitDeclarator;
import softtest.ast.c.ASTInitializer;
import softtest.ast.c.ASTNestedFunctionDefinition;
import softtest.ast.c.ASTParameterDeclaration;
import softtest.ast.c.ASTParameterList;
import softtest.ast.c.ASTPostfixExpression;
import softtest.ast.c.ASTPrimaryExpression;
import softtest.ast.c.ASTSelectionStatement;
import softtest.ast.c.ASTStatement;
import softtest.ast.c.ASTTranslationUnit;
import softtest.ast.c.ASTUnaryExpression;
import softtest.ast.c.CCharStream;
import softtest.ast.c.CParser;
import softtest.ast.c.CParserVisitor;
import softtest.ast.c.Node;
import softtest.ast.c.SimpleNode;
import softtest.callgraph.c.CEdge;
import softtest.callgraph.c.CGraph;
import softtest.callgraph.c.CVexNode;
import softtest.cfg.c.ControlFlowData;
import softtest.cfg.c.ControlFlowVisitor;
import softtest.cfg.c.Edge;
import softtest.cfg.c.Graph;
import softtest.cfg.c.GraphVisitor;
import softtest.cfg.c.VexNode;

import softtest.depchain.c.DepChainUtil.listSCVPVisitor;
import softtest.domain.c.analysis.ControlFlowDomainVisitor;
import softtest.domain.c.analysis.SymbolDomainSet;
import softtest.domain.c.analysis.ValueSet;
import softtest.fsmanalysis.c.AnalysisElement;
import softtest.interpro.c.InterCallGraph;
import softtest.interpro.c.InterContext;
import softtest.interpro.c.InterMethodVisitor;
import softtest.interpro.c.Method;
import softtest.interpro.c.MethodNode;
import softtest.pretreatment.PlatformType;
import softtest.pretreatment.Pretreatment;
import softtest.rules.c.StateMachineUtils;
import softtest.rules.gcc.fault.OOB_CheckStateMachine;
import softtest.scvp.c.Position;
import softtest.scvp.c.SCVP;
import softtest.scvp.c.SCVPString;
import softtest.scvp.c.SCVPVisitor;
import softtest.symboltable.c.AbstractScope;
import softtest.symboltable.c.NameOccurrence;
import softtest.symboltable.c.OccurrenceAndExpressionTypeFinder;
import softtest.symboltable.c.ScopeAndDeclarationFinder;
import softtest.symboltable.c.NameOccurrence.OccurrenceType;
import softtest.test.c.hangtiancheng.STBO;
import sun.java2d.pipe.SpanShapeRenderer.Simple;
import sun.net.www.content.text.plain;

public class DepchainHome implements Serializable {
	/**
	 * ���л�ID
	 */
	private static final long serialVersionUID = 5802149016969017989L;
	private List<AnalysisElement> elements = new ArrayList<AnalysisElement>();;
	private String analysisDir = "";
	private List<String> files = new ArrayList<String>(); // ���ڴ洢�ռ���������.c�ļ�
	private InterCallGraph interCGraph = InterCallGraph.getInstance(); // �õ�������Щ�������ļ���������ϵ
	private String[] args;
	private Pretreatment pre = new Pretreatment();

	public DepchainHome(String[] args) {

		// add by lsc 2018/9/20
		// �˴�Ϊ����·���µ��ļ���args[0]��ʾ����·���µ�����.c�ļ���args[1]��ʾ����ָ����.c�ļ�
		this.analysisDir = args[0];
		this.setArgs(args);
		init();
	}

	private HashMap<String, ASTTranslationUnit> astmap = new HashMap<String, ASTTranslationUnit>();
	private HashMap<String, Graph> cfgmap = new HashMap<String, Graph>();
	private HashMap<Graph, String> cfgmap2 = new HashMap<Graph, String>();
	private HashMap<String, CGraph> cgMap = new HashMap<String, CGraph>();

	// add by lsc 2018/10/26 ���ڵõ���Դ������������غ���������ȫ���ı�����NameOccurence
	private static Set<NameOccurrence> set2 = new HashSet<>();

	// �ռ�����·���µ�����.CԴ�ļ�
	private void collect(File srcFileDir) {
		if (srcFileDir.isFile() && srcFileDir.getName().matches(InterContext.SRCFILE_POSTFIX)) {
			files.add(srcFileDir.getPath());
		} else if (srcFileDir.isDirectory()) {
			File[] fs = srcFileDir.listFiles();
			for (int i = 0; i < fs.length; i++) {
				collect(fs[i]);
			}
		}
	}

	// ����Ԥ����ĳ�ʼ��
	private void init() {
		pre.setPlatform(PlatformType.GCC);

		File srcFileDir = new File(analysisDir);
		collect(srcFileDir);
	}

	public List<List<String>> list_out = new ArrayList<>();
	public List<List<String>> list_out_thisfile = new ArrayList<>();
	public List<String> list_function = new ArrayList<>();
	public List<String> list_operator = new ArrayList<>();
	// add by lsc ���ڼ�¼���к������䱻���ú������ϵ�ӳ��
	public Map<String, List<String>> map_function = new HashMap<>();
	// add by lsc 2018/11/27�� ���ڼ�¼���к���������ú������ϵ�ӳ��
	public Map<String, List<String>> map_function_contains = new HashMap<>();
	// add by lsc 2018/11/27 ���ڼ�¼��Դ���������к������䱻���ú������ϵ�ӳ��
	public Map<String, List<String>> map_function_internal = new HashMap<>();

	// add by lsc 2018/11/27�������ڼ�¼��Դ����������·�����Ӧ��Ҫ�����ĺ������ϵ�ӳ��
	public Map<List<String>, List<List<String>>> Map_path_HighlightFunction = new HashMap<>();

	// add by lsc 2018/11/28,���ڼ�¼��Ӧÿ·����Ҫ�����ĺ�������
	public List<Set<String>> HighlightFunction = new ArrayList<>();

	// add by lsc 2018/11/29�����ڼ�¼�������ص��ⲿ����Դ
	public List<List<String>> list_out_variable = new ArrayList<>();

	// add by lsc 2018/11/29 , ���ڼ�¼��Ӧ�к�����е��ⲿ����Դ��ӳ��
	public Map<VexNode, List<String>> map_line_list_out_variable = new HashMap<>();

	// add by lsc 2019/1/2 ���ڼ�¼������������������·����ӳ��
	public Map<String, String> map_function_path = new HashMap<>();
	// add by lsc 2019/1/2 ���ڼ�¼�������������Ӧ�����﷨���ڵ��ӳ��
	public Map<String, SimpleNode> map_function_simpleNode = new HashMap<>();

	// add by lsc 2019/1/2
	public List<String> list_outsource = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		DepchainHome test = new DepchainHome(args);
		// motified by lsc 2018/11/29�����۲�����Σ��Ƚ��ⲿ����Դ��ȡ
		// add by lsc 2018/11/12��¼�ⲿ����Դ
		System.out.println(test.analyse1());
		// add by lsc 2019/1/2Ϊ�˻�ȡ���׼����Ϣ

		if (args.length == 5) {

			System.out.println(test.analyse2());// ���������

			// �������ص��ⲿ����Դ��ȡ
			System.out
					.println("���vexNode�ڵ���ⲿ����Դ��ӳ��map_line_list_out_variable�� " + test.getmap_line_list_out_variable());

			test.list_out_variable = test.getList_Out_Valiable(test.getmap_line_list_out_variable());
			System.out.println("����������ص��ⲿ����Դ�� " + test.list_out_variable);

			test.analyse3();
			System.out.println("pathSet3: " + test.pathSet3);

			System.out.println("��Դ·�������������Ƕ����ӳ��:");
			Set<String> set = null;

			for (String string : test.pathSet3) {
				set = new HashSet<>();
				String[] strings = string.split("<-");

				for (String string2 : strings) {
					set.add(string2);
				}
			}

			for (String string : set) {
				if (test.map_function_contains.containsKey(string)) {
					test.map_function_internal.put(string, test.map_function_contains.get(string));
				}
			}

			System.out.println("���map_function_internal: ");
			for (Map.Entry<String, List<String>> entry : test.map_function_internal.entrySet()) {
				System.out.println(entry.getKey() + ":" + entry.getValue());
			}

			// ����HightlightFunction
			for (String string : test.pathSet3) {
				Set<String> set2 = new HashSet<>();
				String[] strings = string.split("<-");
				for (String string2 : strings) {
					// add by lsc 2018/11/28
					// ���ڽ��·���п��ܰ���map2�в����ڵĺ��������(��Ϊ·�����ü��㣬��map2��������룬��
					// ���Ƿ���ֵ����f()ֻ�����ö�������ֵ���������ᷢ��)
					if (test.map2.containsKey(string2)) {
						set2.add(string2);
						if (test.map_function_contains.containsKey(string2)) {
							for (String string3 : test.map_function_contains.get(string2)) {
								if (test.map2.containsKey(string3)) {
									set2.add(string3);
								}
							}
						}
					}

				}

				test.HighlightFunction.add(set2);
			}

			System.out.println("���HighlightFunction:");
			for (Set<String> set2 : test.HighlightFunction) {
				for (String string : set2) {
					System.out.print(string + " ");
				}
				System.out.println();
			}

			System.out.println("�������������");
			for (Set<String> set1 : test.HighlightFunction) {

				System.out.println(set1);
			}
		}
		if (args.length == 4) {
			System.out.println(test.analyse3());
		} else {
			if (args.length <= 2) {

			}
		}

	}

	/*
	 * add by lsc 2019/1/2 ��ȡ��Ӧ���
	 */
	public void prepare() throws Exception {
		SearchFunction searchFunction = new SearchFunction();
		searchFunction.main(args);
		list_function = searchFunction.getList();
		// System.out.println(list_function.toString());
		map_function = searchFunction.getMapOfFunction();
		map_function_contains = searchFunction.getMapOfFunctionContains();
		// add by lsc 2019/1/2
		map_function_path = searchFunction.getMap_Function_Path();
		map_function_simpleNode = searchFunction.getMap_Function_SimpleNode();

	}

	public HashSet<String> pathSet3 = new HashSet<String>();

	public void getpathSet3(String function, String path) {
		if (map_function.get(function) == null) {
			pathSet3.add(path);
		} else {
			for (String string : map_function.get(function)) {

				getpathSet3(string, path + "<-" + string);
			}
		}

	}

	/**
	 * ������.CԴ�ļ����ν��д�����Ԥ���롢����������ȫ�ֺ������ù�ϵͼ
	 * 
	 * @throws IOException
	 */
	private void process() throws IOException {
		// ��һ����������.CԴ�ļ�����Ԥ����
		PreAnalysis();

		// �ڶ���������ȫ�ֺ������ù�ϵͼ
		List<AnalysisElement> orders = interCGraph.getAnalysisTopoOrder();
		if (orders.size() != elements.size()) {
			for (AnalysisElement element : elements) {
				boolean exist = false;
				for (AnalysisElement order : orders) {
					if (order == element) {
						exist = true;
					}
				}
				if (!exist) {
					orders.add(element);
				}
			}
		}
	}

	private void PreAnalysis() {
		for (String srcFile : files) {
			AnalysisElement element = new AnalysisElement(srcFile);
			elements.add(element);
			// Ԥ����֮���.IԴ�ļ�
			String afterPreprocessFile = null;
			List<String> include = new ArrayList<String>();

			List<String> macro = new ArrayList<String>();
			afterPreprocessFile = pre.pretreat(srcFile, include, macro);// ���ø���������Ԥ����ָ�������м��ļ�

			try {
				String temp = element.getFileName();
				// ���������﷨��
				System.out.println("���ɳ����﷨��...");
				System.out.println(afterPreprocessFile);
				CParser parser = CParser.getParser(new CCharStream(new FileInputStream(afterPreprocessFile)));
				ASTTranslationUnit root = parser.TranslationUnit();
				astmap.put(srcFile, root);// ���﷨�����ڴ��ͨ���ļ�������

				// �������ű�
				System.out.println("���ɷ��ű�...");
				ScopeAndDeclarationFinder sc = new ScopeAndDeclarationFinder();
				root.jjtAccept(sc, null);
				OccurrenceAndExpressionTypeFinder o = new OccurrenceAndExpressionTypeFinder();
				root.jjtAccept(o, null);

				// ����ȫ�ֺ������ù�ϵ
				System.out.println("����ȫ�ֺ������ù�ϵ...");
				root.jjtAccept(new InterMethodVisitor(), element);

				// �ļ��ں������ù�ϵͼ
				System.out.println("�����ļ��ں������ù�ϵ...");
				CGraph g = new CGraph();
				((AbstractScope) (root.getScope())).resolveCallRelation(g);
				List<CVexNode> list = g.getTopologicalOrderList(element);
				Collections.reverse(list);
				cgMap.put(srcFile, g);

				// ���ɿ�����ͼ
				System.out.println("���ɿ�����ͼ...");
				ControlFlowVisitor cfv = new ControlFlowVisitor(element.getFileName());
				ControlFlowData flow = new ControlFlowData();
				for (CVexNode cvnode : list) {
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
						cfv.visit((ASTFunctionDefinition) node, flow);
						cfgmap.put(node.getImage(), ((ASTFunctionDefinition) node).getGraph());
						cfgmap2.put(((ASTFunctionDefinition) node).getGraph(), node.getImage());

					}
				}

				// ���ɶ���ʹ����
				System.out.println("���ɶ���ʹ����...");

				/**
				 * add by lsc 2018/9/14�˴��ȽϹؼ��ĵ�����ASTTranslationUnit.java�е�
				 * public Object jjtAccept(CParserVisitor visitor, Object data)
				 * ���� DUAnalysisVisitor.java�е�visit������
				 * ֮��initDefUse(),��֮��AbstractScope.java�е�
				 * checkOccurrenceType()����"��������"������NameOccurrence��
				 */
				root.jjtAccept(new DUAnalysisVisitor(), null);

				// ����SCVP
				System.out.println("����scvp...");

				for (CVexNode cvnode : list) {
					SimpleNode node = cvnode.getMethodNameDeclaration().getNode();
					if (node instanceof ASTFunctionDefinition) {
						// System.out.println(cvnode.toString());

						node.jjtAccept(new SCVPVisitor(), null);

					}
				}
				System.out.println("OK.");

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public List<List<String>> analyse1() throws Exception {
		test_outputlib tg = new test_outputlib();
		String[] args1 = new String[1];
		// �˴����Դ������������ȡ��·����Ϣ,2018/11/30 add by lsc
		// args1[0] = args[1];
		tg.main(args);
		list_out = tg.getList();
		list_outsource = tg.getlist_outsource();
		return list_out;
	}

	public List<String> getList_outSource() throws Exception {
		test_outputlib tg = new test_outputlib();

		tg.main(args);

		list_outsource = tg.getlist_outsource();
		return list_outsource;
	}

	// ȫ�ֱ���map2�д洢��ÿ�������������Դ����к�
	private Map<String, Set<Integer>> map2 = new HashMap<>();

	// List<List<VexNode>> paths = new ArrayList<>();
	List<String> paths = new ArrayList<>();

	public Map<String, Set<Integer>> analyse2() throws Exception {
		findAstOfVariable findAstOfVariable = new findAstOfVariable();
		findAstOfVariable.main(args);
		list_operator = findAstOfVariable.getList_Operator();

		System.out.println(list_operator.toString());
		prepare();
		process();

		// ��ȡҪ�������NameOccurrence
		// NameOccurrence occ = locate(args[1], args[2], args[3],
		// Integer.valueOf(args[4]));
		//
		// VexNode vexNode = getVexNodeOflocate(args[2], args[3],
		// Integer.valueOf(args[4]));
		// System.out.println(vexNode + " " + vexNode.getTreenode());
		// SimpleNode simpleNode = getSimpleNodeOflocate(args[2], 1, args[3],
		// Integer.valueOf(args[4]));
		// System.out.println(simpleNode);
		//
		// Graph graph11 = cfgmap.get(args[2]);
		// for (Map.Entry<String, NameOccurrence> map :
		// graph11.getOcctable().entrySet()) {
		// String key = map.getKey();
		// NameOccurrence value = map.getValue();
		// System.out.println(key + " " + value);
		// }

		// add by lsc 2019/1/11
		String funcName = args[2];
		String varName = args[3];
		int line = Integer.parseInt(args[4]);
		VexNode vexNode = getVexNodeOflocate(funcName, varName, line);
		System.out.println("��ʼ�Ŀ��������vexNode: " + vexNode);
		

		// ��ÿ���ڵ��ǰ���ڵ�
		getVexNodeOfPreMap(vexNode);
		// ��for-whileѭ��
		// paths.clear();
		// paths = getPath_test(vexNode,paths);
		//

		set_path_node.clear();
		paths.clear();
		paths = getPath100(vexNode, paths);
		System.err.println("paths : ");
		Set<String> set = new HashSet<>();
		for (String str : paths) {
			set.add(str);
			System.out.println(str);
		}
		System.err.println("set_path_node: " + set_path_node.toString());
		System.err.println("paths��Ԫ�ظ���Ϊ: " + paths.size());
		System.err.println("set��Ԫ�ظ���Ϊ: " + set.size());

		// generate2(occ);
		//
		// System.out.println(vis.toString());
		// System.out.println("�ź���׷�������" + VariableOfSort.toString());
		//
		// System.out.println("map2:");
		return map2;
	}

	// public HashSet<String> pathSet = new HashSet<String>();
	// Ӧ�ѿ���Ҫ�󣬽�����·����ϵ����String���ʹ洢��LinkedList��ʽ�洢
	public HashSet<String> pathSet = new HashSet<>();
	public LinkedList<String> linkedList = new LinkedList<>();

	/**
	 * 
	 * @param occ
	 */
	private void generate2(NameOccurrence occ) {
		DepGraphNode g = new DepGraphNode(occ);

		// ���·������
		pathSet.clear();

		// ����µ�·������
		paths.clear();

		// ���Set����������׼���洢������Ϣ(NameOccurrence),��set����������������
		vis.clear();
		VariableOfSort.clear();

		// ��dfs������ģ��ݹ飬�����ֵ��������Ϊ�����ṩg,��ȫ�ֱ������������������ĵ��ù�ϵ�������map2��¼
		// dfs(occ, g, 0, 1);

	}

	/**
	 * add by lsc 2018/12/1
	 * 
	 * @param occ
	 * @return
	 * @throws IOException
	 */
	public Map<VexNode, List<String>> getmap_line_list_out_variable() throws IOException {
		map_line_list_out_variable.clear();
		process();
		// ��ȡҪ�������NameOccurrence
		NameOccurrence occ = locate(args[1], args[2], args[3], Integer.valueOf(args[4]));

		DepGraphNode g = new DepGraphNode(occ);

		// ���Set����������׼���洢������Ϣ(NameOccurrence),��set����������������
		vis.clear();

		// ��dfs������ģ��ݹ飬�����ֵ��������Ϊ�����ṩg,��ȫ�ֱ������������������ĵ��ù�ϵ�������map2��¼
		dfs(occ, g, 0, 1);

		return map_line_list_out_variable;
	}


	private Map<String, Set<String>> vexMap = new HashMap<>();
	private Map<String, Boolean> condMap = new HashMap<>();
	private Map<String, Boolean> condLineMap = new HashMap<>();

	/*
	 * ��ȡ������ĵ���·����ϵ��Ϣ noted by lsc 2018/10/26
	 */
	private void dfsGetAllPath(DepGraphNode root, String path) {
		if (root.child == null || root.child.size() == 0) {
			pathSet.add(path);
		}
		String curFuncName = cfgmap2.get(root.occ.getLocation().getCurrentVexNode().getGraph());
		for (DepGraphNode child : root.child) {
			String childFuncname = cfgmap2.get(child.occ.getLocation().getCurrentVexNode().getGraph());

			if (!curFuncName.equals(childFuncname))

				dfsGetAllPath(child, path + "<-" + childFuncname);
			else
				dfsGetAllPath(child, path);
		}
	}

	private HashSet<NameOccurrence> vis = new HashSet<>();

	// add by lsc 2018/11/28����Ϊ��׷���ź����ķ������
	private List<NameOccurrence> VariableOfSort = new ArrayList<>();

	/*
	 * add by lsc 2019/1/12 ���ݵ�ǰ�������ڵ�õ���ǰ������ͼ�Ͻڵ�������ڵ��mapӳ��
	 */
	public Map<String, VexNode> getMapOfVexNode(VexNode vexNode) {
		Map<String, VexNode> map = new HashMap<>();
		Graph graph = vexNode.getGraph();
		for (VexNode nodes : graph.getAllnodes()) {
			if (!map.containsKey(nodes.toString())) {
				map.put(nodes.toString(), nodes);
			}
		}
		return map;
	}

	/*
	 * add by lsc 2019/1/12 �ж������ڵ��Ƿ�������
	 */
	public boolean IsConnect(VexNode head, VexNode tail) {
		Hashtable<String, Edge> hashtable = head.getInedges();
		Set<String> set = hashtable.keySet();
		for (String string : set) {
			Edge edge = hashtable.get(string);
			VexNode node = edge.getTailNode();
			if (node.equals(tail)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * add by lsc 2019/13/11:46 Ŀ�ģ�Ϊ�˲����� ���ܣ����ݸ���������һ��������ͼ�ڵ��ȡ��ǰ�����������нڵ���ǰ���ڵ��ӳ��
	 */
	public Map<VexNode, List<VexNode>> getVexNodeOfPreMap(VexNode vexNode) {
		Graph graph = vexNode.getGraph();
		Map<VexNode, List<VexNode>> map = new HashMap<>();
		for (VexNode node : graph.getAllnodes()) {
			Hashtable<String, Edge> hashtable = node.getInedges();
			Set<String> set = hashtable.keySet();
			List<VexNode> list = new ArrayList<>();
			for (String string : set) {
				Edge edge = hashtable.get(string);
				VexNode vexNode2 = edge.getTailNode();
				list.add(vexNode2);
			}
			if (!map.containsKey(node)) {
				map.put(node, list);
			}
			String str = getVexNodeType(node);
			System.out.println("�ڵ����ǰ���ڵ��ӳ�䣺" + node + "   :    " + list + "   " +"�ڵ��Ӧ���﷨���ڵ�:"+node.getTreenode()+"  "+ "�ڵ�����ͣ� "+str);
		}
		return map;
	}

	// add by lsc 2019/1/15�����洢�Ѿ������ʹ��Ľڵ�
	Set<VexNode> set_path_node = new HashSet<>();

	
	
	
	/*add by lsc 2019/1/16
	 * �������� ��Ŀ��ڵ��������ǰ·��
	 *���ܣ�����Ŀ��ڵ��ȡ�Ӻ�����ڵ�������пɴ�·��
	 */
	public List<String> getPath100(VexNode vexNode , List<String> path) {
		if (path.isEmpty()) {
		findpath("", vexNode);
		}
		return path;
	}
	
	/*
	 * add by lsc 2019/1/16 ��20:32
	 */
	public void findpath(String oldpath , VexNode newVexNode ) {
		if(!oldpath.contains(newVexNode.toString())) {
			oldpath = oldpath +"<-" + newVexNode.toString();
			set_path_node.add(newVexNode);
		}
		Hashtable<String, Edge> hashtable = newVexNode.getInedges();
		if(hashtable.isEmpty()) {
			paths.add(oldpath);
		}
		else {
			Set<String> set = hashtable.keySet();
			for(String string : set) {
				Edge edge = hashtable.get(string);
				VexNode vexNode = edge.getTailNode();
				if(!set_path_node.contains(vexNode) || !oldpath.contains(vexNode.toString())) {
					findpath(oldpath, vexNode);
				}
				else if(set_path_node.contains(vexNode) && oldpath.contains(vexNode.toString())) {
					if(vexNode.toString().contains("for_head") || vexNode.toString().contains("while_head")) {
						oldpath = oldpath + "<-" + vexNode.toString();
						Hashtable<String, Edge> table = vexNode.getInedges();
						Set<String>set_table = table.keySet();
						for(String string2 : set_table) {
							Edge edge2 = table.get(string2);
							VexNode vexNode2 = edge2.getTailNode();
							if(vexNode2.getSnumber() < vexNode.getSnumber()) {
								findpath(oldpath, vexNode2);
							}
						}
					}
				}
			}
		}
	}
	

	
	
	/*
	 * add by lsc 2019/1/16
	 * ��һ��set�����е����ݽ��������������һ�����鼯��
	 */
	public String[] set_to_array_sort(Set<String> set) {
		String[] array = new String[set.size()];
		int count = 0;
		for(String string : set) {
			array[count++] = string;
		}
		Arrays.sort(array);
		return array;
	}
	
	
	/*
	 * add by lsc 2019/1/16
	 * ����һ��·���ַ����а����ض��ڵ����ּ���
	 */
	public int get_vexnode_count(String str , VexNode vexNode) {
		int count = 0;
		String[] strings = str.split("<-");
		for(String string : strings) {
			if(string.equals(vexNode.toString())) {
				count ++;
			}
		}
		return count;
	}
	


	/**
	 * 
	 * @param NameOccurrence
	 *            occ
	 * @param DepGraphNode
	 *            g
	 * @param int
	 *            cond
	 * @param int
	 *            depth
	 */
	private void dfs(NameOccurrence occ, DepGraphNode g, int cond, int depth) {


	}

	

	/**
	 * add by lsc 2018/12/4 , ȡ�õȺ��ұߵı������б�
	 */
	public List getRightVar(SimpleNode node) {
		Node assignmentNode = node.getFirstParentOfType(ASTAssignmentExpression.class);
		if (assignmentNode != null && assignmentNode instanceof ASTAssignmentExpression
				&& assignmentNode.jjtGetNumChildren() == 3) {

			ASTAssignmentExpression rightExpr = (ASTAssignmentExpression) assignmentNode.jjtGetChild(2);

			// ȡ���Ҳ����ʽ�����еı������б�
			List rightVars = rightExpr.findChildrenOfType(ASTPrimaryExpression.class);
			return rightVars;

		}

		// add by lsc 2018/12/11����ֱ�Ӷ����ʱ������int m = a + 1;
		if (node instanceof ASTDirectDeclarator) {
			ASTInitDeclarator astInitDeclarator = (ASTInitDeclarator) node
					.getFirstParentOfType(ASTInitDeclarator.class);
			ASTInitializer astInitializer = (ASTInitializer) astInitDeclarator
					.getFirstChildOfType(ASTInitializer.class);
			List rightVars = astInitializer.findChildrenOfType(ASTPrimaryExpression.class);
			return rightVars;
		}

		return null;
	}
	
	
	/**
	 * ȡ�ñ���ʽ��ߵı���
	 * add by lsc 2019/2/14
	 */
	public SimpleNode getLeftVar(SimpleNode node) {
		return node;
		
	}

	


	/**
	 * add by lsc 2018/9/19 ��ȡҪ�������NameOccurrence
	 * 
	 * @param fileName
	 * @param funcName
	 * @param symbolName
	 * @param line
	 * @return
	 */
	private NameOccurrence locate(String fileName, String funcName, String symbolName, int line) {
		Graph cfg = cfgmap.get(funcName);
		String occStr = symbolName + ":" + line;
		if (cfg == null)
			return null;
		return cfg.getOcctable().get(occStr);
	}

	/**
	 * occ; child; private List<VexNode> path; isCond;
	 * 
	 * @author lsc
	 *
	 */
	@SuppressWarnings("unused")
	private class DepGraphNode {
		private NameOccurrence occ;
		private List<DepGraphNode> child;
		private List<VexNode> path;
		private boolean isCond;

		public DepGraphNode(NameOccurrence occ) {
			this.occ = occ;
			child = new ArrayList<DepGraphNode>();
			path = new ArrayList<VexNode>();
			isCond = false;

		}

		public DepGraphNode addChild(NameOccurrence o) {
			DepGraphNode childNode = new DepGraphNode(o);
			child.add(childNode);

			try {

				VexNode from = o.getLocation().getCurrentVexNode();
				VexNode to = occ.getLocation().getCurrentVexNode();
				childNode.path = Graph.findAPath(from, to);

				return childNode;
			} catch (Exception e) {
				// e.printStackTrace();
				return null;
			}
		}

	}

	/**
	 * �Ӻ�������ڵ��ȡ�������ڵ㣬���Դ�ASTPostfixExpression.getImage()��ȡ���ú����ĺ����� add by lsc
	 * 2019/11/20
	 * 
	 * @param def
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<ASTPostfixExpression> getCalleeNodesFromFunction(ASTFunctionDefinition def) {
		String xpathString = ".//PostfixExpression[@Operators='(']";
		softtest.cfg.c.Graph cfg = def.getGraph();
		VexNode entryNode = cfg.getEntryNode();
		SimpleNode entry_astnode = entryNode.getTreenode();
		List<ASTPostfixExpression> xList = null;
		try {
			xList = entry_astnode.findChildNodesWithXPath(xpathString);
		} catch (JaxenException e) {
			e.printStackTrace();
		}
		return xList;
	}

	public List<List<String>> getList_Out_Valiable(Map<VexNode, List<String>> map_line_list_out_variable) {
		//
		for (Map.Entry<VexNode, List<String>> entry : map_line_list_out_variable.entrySet()) {
			list_out_variable.add(entry.getValue());
		}
		return list_out_variable;
	}

	public HashSet<String> analyse3() throws Exception {

		getpathSet3(args[2], args[2]);

		System.out.println("������������õ����·������Դ�����");

		return pathSet3;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public Map<String, Boolean> getCondLineMap() {
		return condLineMap;
	}

	public void setCondLineMap(Map<String, Boolean> condLineMap) {
		this.condLineMap = condLineMap;
	}

	// add by lsc 2018/12/24
	public HashMap<String, Graph> getCfgmap() {
		return cfgmap;
	}

	/**
	 * add by lsc 2019/1/10 �ɺ����������������кŻ�ȡ��λ��Ŀ�����ͼ�ڵ�
	 */
	public VexNode getVexNodeOflocate(String funcName, String varName, int line) {
		SimpleNode simpleNode = map_function_simpleNode.get(funcName);
		Graph graph = cfgmap.get(funcName);
		for (VexNode vexNode : graph.getAllnodes()) {
			SimpleNode temp = vexNode.getTreenode();
			if (temp.getBeginLine() == line) {
				return vexNode;
			}
		}
		return null;
	}

	/**
	 * add by lsc 2019/1/10 �ɺ��������������������������������ڼ���������)���кŻ�ȡ��λ��ĳ����﷨�����
	 */
	public SimpleNode getSimpleNodeOflocate(String funcName, int index, String varName, int line) {
		SimpleNode node = null;
		VexNode vexNode = getVexNodeOflocate(funcName, varName, line);
		SimpleNode simpleNode = vexNode.getTreenode();
		List<SimpleNode> list = null;
		if (varName.contains(".") || varName.contains("->") || varName.contains("[")) {
			list = simpleNode.findXpath(".//PostfixExpression");
		} else {
			list = simpleNode.findXpath(".//PrimaryExpression");
		}
		return list.get(index - 1); 
	}
	
	/**
	 * ������������ͷ���
	 * add by lsc 2019/2/13 
	 */
	public String getVexNodeType(VexNode vexNode) {
		SimpleNode node = vexNode.getTreenode();
		String type = "";
		if(vexNode.getInedges().isEmpty()) {
			type = "0:������ͼ����ڽ��";
			return type;
		}
		else if(vexNode.getOutedges().isEmpty()) {
			type = "4:������ͼ�ĳ��ڽ��";
			return type;
		}
		else if(node instanceof ASTExpressionStatement) {
			ASTAssignmentExpression assignmentExpression = (ASTAssignmentExpression) node.getFirstChildOfType(ASTAssignmentExpression.class);
			if(assignmentExpression.jjtGetNumChildren() == 3) {
				type = "3:�޶�������";
				return type;
			}
			else if(assignmentExpression.jjtGetNumChildren() == 1) {
				List list = assignmentExpression.findXpath(".//PrimaryExpression[@Method='true']");
				if(!list.isEmpty()){
					SimpleNode simpleNode = (SimpleNode) list.get(0);
					if(list_outsource.contains(simpleNode.getImage())) {
						type = "5:�⺯��";
					}
					else {
						type = "1:ֻ�к���f()����f(c)";
					}
				}
				else {
					type = "6:�����Լ�����";
				}
				return type;
			}
		}
		else if(node instanceof ASTDeclaration) {
//			if(node.containsChildOfType(ASTInitializer.class)) {
				type = "2:�ж�������";
//			}
				return type;
		}
		type = "-1:�����ݽڵ�";
		return type;
	}
	
	

	
	/**
	 * �жϱ����ĳ�������
	 * add by lsc 2019/2/15 
	 * ����3��:�������ڿ������ڵ�vexNode�� ������varName , ���� index
	 */
	public String getVarType(VexNode vexNode , String varName , int index) {
		String type = "";
		String string = getVexNodeType(vexNode);
		if(string.contains("0")) {
			type = "����";
		}
		else if(string.contains("1")) {
			type = "ʹ�õ�";
		}
		else if(string.contains("2")) {
			SimpleNode simpleNode = vexNode.getTreenode();
			ASTInitDeclarator astInitDeclarator = (ASTInitDeclarator) simpleNode.getFirstChildOfType(ASTInitDeclarator.class);
			if(astInitDeclarator.jjtGetNumChildren() == 1) {
				
			}
			else if(astInitDeclarator.jjtGetNumChildren() == 2) {
				
			}
		}
		else if(string.contains("3")) {
			
		}
		else if(string.contains("4")) {
			
		}
		else if(string.contains("5")) {
			
		}
		else if(string.contains("6")) {
			//�����Լ����������ʹ�õ�
			type = "ʹ�õ�";	
		}
		else if(string.contains("-1")) {
			
		}
		return type;
	}
	
    public enum Type{
    	/** ���� */
    	DEF,
    	/** ʹ�� */
    	USE,
    	DEF_AFTER_USE,//i++;i+=5;
    	//add by cmershen,2016.3.14,�������
    	ENTRANCE
    }
    
    /**
     * add by lsc 2019/2/15
     * �жϱ����Ƿ��ڸ�ֵ����ʽ�����(�������)
     * ����3��������ʽ���ڿ��������vexNode , ������varName , ������������index
     */
    public boolean isVarNameAtLeft(VexNode vexNode , String varName , int index)  {
    	String string = getVexNodeType(vexNode);
    	if(index > 1) {
    		return false;
    	}
    	//�ж����������
    	if(string.contains("2")) {
			SimpleNode simpleNode = vexNode.getTreenode();
			ASTInitDeclarator astInitDeclarator = (ASTInitDeclarator) simpleNode.getFirstChildOfType(ASTInitDeclarator.class);
			//�޳�ʼ�����
			if(astInitDeclarator.jjtGetNumChildren() == 1) {
				return false;
			}
			//�г�ʼ�����
			else if(astInitDeclarator.jjtGetNumChildren() == 2) {
				ASTDirectDeclarator astDirectDeclarator = (ASTDirectDeclarator) astInitDeclarator.getFirstChildOfType(ASTDirectDeclarator.class);
				String str = astDirectDeclarator.getImage();
				if(str.equals(varName)) {
					return true;
				}
			}
		}
    	//�޶���������� 
		else if(string.contains("3")) {
			
		}
		return false;
    }

    
    /**����post�﷨���ڵ��ȡ��Ӧ�ı���������
     * add by lsc 2019/2/17
     */
    public String getVarNameFromPostNode(SimpleNode node) {
    	String varName = "";
    	if(node instanceof ASTPostfixExpression) {
    		if(node.jjtGetNumChildren() == 1) {
    			SimpleNode node2 = (SimpleNode) node.getFirstChildOfType(ASTPrimaryExpression.class);
    			varName = node2.getImage();
    		}
    		else {
    			
    		}
    	}
    	
//    	return varName;  
    	
    }
    
    
    
}