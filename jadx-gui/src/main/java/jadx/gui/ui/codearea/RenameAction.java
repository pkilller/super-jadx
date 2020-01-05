package jadx.gui.ui.codearea;

import jadx.api.*;
import jadx.core.deobf.Deobfuscator;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.info.VarInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.*;
import jadx.core.utils.files.InputFile;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.RenameDialog;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.NLS;
import org.fife.ui.rsyntaxtextarea.Token;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RenameAction extends AbstractAction implements PopupMenuListener {
	private static final long serialVersionUID = -4692546549977976384L;

	private final transient ContentPanel contentPanel;
	private final transient CodeArea codeArea;
	private final transient JClass jCls;

	private transient JavaNode node;
	private transient JNode jnode;

	static Deobfuscator deobfuscator = null;

	public RenameAction(ContentPanel contentPanel, CodeArea codeArea, JClass jCls) {
		super(NLS.str("popup.rename"));
		this.contentPanel = contentPanel;
		this.codeArea = codeArea;
		this.jCls = jCls;
	}

	private void initDeobfuscat() {
		RootNode root = JadxDecompiler.instance.getRoot();
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return;
		}

		InputFile firstInputFile = dexNodes.get(0).getDexFile().getInputFile();
		Path inputFilePath = firstInputFile.getFile().getAbsoluteFile().toPath();

		String inputName = inputFilePath.getFileName().toString();
		String baseName = inputName.substring(0, inputName.lastIndexOf('.'));
		Path deobfMapPath = inputFilePath.getParent().resolve(baseName + ".jobf");

		JadxArgs args = root.getArgs();

		deobfuscator = new Deobfuscator(args, dexNodes, deobfMapPath);

		Map<ClassNode, JavaClass> classes = JadxDecompiler.instance.getClassesMap();
		for (JavaClass cls : classes.values()) {
			ClassNode clsNode = cls.getClassNode();
			ClassInfo clsInfo = clsNode.getClassInfo();
			if (clsInfo.getAliasFromPreset()) {
				deobfuscator.setClsAlias(clsNode, clsInfo.getAliasShortName());
			}
		}

		Map<FieldNode, JavaField> fields = JadxDecompiler.instance.getFieldsMap();
		for (JavaField fld : fields.values()) {
			FieldNode fldNode = fld.getFieldNode();
			FieldInfo fldInfo = fldNode.getFieldInfo();
			if (fldInfo.getAliasFromPreset()) {
				deobfuscator.setFieldAlias(fldNode, fldInfo.getAlias());
			}
		}

		Map<MethodNode, JavaMethod> methods = JadxDecompiler.instance.getMethodsMap();
		for (JavaMethod mth : methods.values()) {
			MethodNode mthNode = mth.getMethodNode();
			MethodInfo mthInfo = mthNode.getMethodInfo();
			if (mthInfo.getAliasFromPreset()) {
				deobfuscator.setMthAlias(mthNode, mthInfo.getAlias());
			}
		}

		Map<VarNode, JavaVar> vars = JadxDecompiler.instance.getVarsMap();
		for (JavaVar var : vars.values()) {
			VarNode varNode = var.getVarNode();
			VarInfo varInfo = varNode.getVarInfo();
			if (varInfo.getAliasFromPreset()) {
				deobfuscator.setVarAlias(varNode, varInfo.getAlias());
			}
		}

		deobfuscator.updateVarsDeobs();
	}

	public void setDeobfuscatName(JavaNode node, String alias) {
		if (deobfuscator == null) {
			initDeobfuscat();
		}

		if (node instanceof JavaClass) {
			deobfuscator.setClsAlias(((JavaClass) node).getClassNode(), alias);
		} else if (node instanceof JavaMethod) {
			deobfuscator.setMthAlias(((JavaMethod) node).getMethodNode(), alias);
		} else if (node instanceof JavaField) {
			deobfuscator.setFieldAlias(((JavaField) node).getFieldNode(), alias);
		} else if (node instanceof JavaVar) {
			deobfuscator.setVarAlias(((JavaVar) node).getVarNode(), alias);
		}
		deobfuscator.savePresets(true);
	}

	private void recompileJavaCode(ClassNode classNode) {
		classNode.unload();
		classNode.setCode(null);
		classNode.setState(ProcessState.NOT_LOADED);
		classNode.load();
	}

	private void rebuildUsage(ClassNode classNode) {
		JavaClass cls = JadxDecompiler.instance.getJavaClassByNode(classNode);
		CacheObject cache = contentPanel.getTabbedPane().getMainWindow().getCacheObject();
		cache.getIndexJob().updateClsCache(cls);
	}

	private MethodNode findTopMethod(MethodNode method) {
		MethodNode topMth = recursiveFindTopVirtualMethod(method.getName(), method.getReturnType(),
				method.getArguments(false), method.getParentClass());
		if (topMth != null) {
			return topMth;
		}

		topMth = recursiveFindTopInterfaceMethod(method.getName(), method.getReturnType(),
				method.getArguments(false), method.getParentClass());
		if (topMth != null) {
			return topMth;
		}
		return null;
	}

	private MethodNode recursiveFindTopVirtualMethod(String name, ArgType retType, List<RegisterArg> argsNonThis,
			ClassNode beginClassNode) {
		ArgType superType = beginClassNode.getSuperClass();
		if (superType == null) {
			return null;
		}
		ClassNode superClass = beginClassNode.dex().resolveClass(superType);
		if (superClass == null) {
			return null;
		}
		MethodNode curMth = superClass.getMethodByPrototype(name,
				retType,
				argsNonThis);
		MethodNode topMth = recursiveFindTopVirtualMethod(name, retType, argsNonThis, superClass);

		if (null != topMth) {
			return topMth;
		} else if (null != curMth) {
			return curMth;
		} else {
			return null;
		}
	}

	private MethodNode recursiveFindTopInterfaceMethod(String name, ArgType retType, List<RegisterArg> argsNonThis,
			ClassNode beginClassNode) {
		List<ArgType> interfaces = beginClassNode.getInterfaces();
		for (ArgType in : interfaces) {
			ClassNode inNode = beginClassNode.dex().resolveClass(in);
			if (inNode == null) {
				return null;
			}
			MethodNode curMth = inNode.getMethodByPrototype(name,
					retType,
					argsNonThis);

			MethodNode topMth = recursiveFindTopInterfaceMethod(name, retType, argsNonThis, inNode);

			if (null != topMth) {
				return topMth;
			} else if (null != curMth) {
				return curMth;
			}
		}
		return null;
	}

	private List<MethodNode> findVirtualsMethods(MethodNode method) {
		List<MethodNode> outOverrideMethods = new ArrayList<>();
		recursiveFindImpmenentMethods(method.getName(), method.getReturnType(),
				method.getArguments(false), method.getParentClass(), outOverrideMethods);
		if (outOverrideMethods.size() > 0) {
			return outOverrideMethods;
		}

		recursiveFindOverrideMethods(method.getName(), method.getReturnType(),
				method.getArguments(false), method.getParentClass(), outOverrideMethods);
		if (outOverrideMethods.size() > 0) {
			return outOverrideMethods;
		}
		return null;
	}

	private void recursiveFindImpmenentMethods(String name, ArgType retType, List<RegisterArg> argsNonThis,
			ClassNode beginClassNode, List<MethodNode> outOverrideMethods) {
		List<ClassNode> impClasses = beginClassNode.getImplements();

		for (ClassNode impClass : impClasses) {
			MethodNode subMethod = impClass.getMethodByPrototype(name,
					retType,
					argsNonThis);
			if (subMethod != null) {
				// has override
				outOverrideMethods.add(subMethod);
			}
			recursiveFindImpmenentMethods(name, retType, argsNonThis, impClass, outOverrideMethods);
		}
	}

	private void recursiveFindOverrideMethods(String name, ArgType retType, List<RegisterArg> argsNonThis,
			ClassNode beginClassNode, List<MethodNode> outOverrideMethods) {
		List<ClassNode> subClasses = beginClassNode.getSubClasses();

		for (ClassNode subClass : subClasses) {
			MethodNode subMethod = subClass.getMethodByPrototype(name,
					retType,
					argsNonThis);
			if (subMethod != null) {
				// has override
				outOverrideMethods.add(subMethod);
			}
			recursiveFindOverrideMethods(name, retType, argsNonThis, subClass, outOverrideMethods);
		}
	}

	// 获取所有包含该覆盖方法
	public List<JavaMethod> getOverrideMethods(JavaMethod method) {
		List<JavaMethod> overrideJMethods = new ArrayList<>();

		MethodNode mth = method.getMethodNode();
		if (!mth.isVirtual() || mth.isConstructor()) {
			return null;
		}
		// find the same method prototype from top super
		MethodNode topMethodNode = findTopMethod(method.getMethodNode());
		if (topMethodNode == null) {
			topMethodNode = method.getMethodNode();
		}
		overrideJMethods.add(JadxDecompiler.instance.getJavaMethodByNode(topMethodNode));

		// find all override methods
		List<MethodNode> virtualMethods = findVirtualsMethods(topMethodNode);
		if (virtualMethods != null) {
			for (MethodNode virMth : virtualMethods) {
				overrideJMethods.add(JadxDecompiler.instance.getJavaMethodByNode(virMth));
			}
		}
		return overrideJMethods;
	}

	private void refreashCodeAren(JavaNode jnode) {
		MainWindow mainWin = contentPanel.getTabbedPane().getMainWindow();
		JNode jOuterClass = mainWin.getCacheObject().getNodeCache().makeFrom(jnode.getTopParentClass());
		ContentPanel panel = mainWin.getTabbedPane().getOpenTabs().get(jOuterClass);
		if (panel instanceof ClassCodeContentPanel) {
			CodeArea area = (CodeArea) ((ClassCodeContentPanel) panel).getCodeArea();
			ScrollPosition pos = area.getScrollPosition();
			area.reload();
			area.setScrollPosition(pos);
		}
	}

	public void reGenerateClassesCode() {
		MainWindow mainWin = contentPanel.getTabbedPane().getMainWindow();
		CacheObject cache = mainWin.getCacheObject();

		// Is caching complete?
		if (!cache.getIndexJob().isComplete()) {
			return;
		}

		// usage classes.
		List<JavaNode> usageNodes = new ArrayList<>();
		List<CodeNode> usages = cache.getUsageInfo().getUsageList(jnode);
		if (usages == null) {
			return;
		}
		for (CodeNode usage : usages) {
			if (!usageNodes.contains(usage.getJavaNode())) {
				usageNodes.add(usage.getJavaNode());
			}
		}

		// will be regenerate override methods and usage nodes for the method.
		if (jnode instanceof JMethod) {
			List<JavaMethod> overrideMethods = getOverrideMethods((JavaMethod) jnode.getJavaNode());
			if (overrideMethods != null) {
				for (JavaMethod mth : overrideMethods) {
					JNode jmth = mainWin.getCacheObject().getNodeCache().makeFrom(mth);

					// usage classes.
					List<CodeNode> tmpUsages = cache.getUsageInfo().getUsageList(jmth);
					if (tmpUsages == null) {
						return;
					}

					for (CodeNode usage : tmpUsages) {
						if (!usageNodes.contains(usage.getJavaNode())) {
							usageNodes.add(usage.getJavaNode());
						}
					}
				}
			}
		}

		// update clesses
		List<JavaClass> needUpdateClasses = new ArrayList<>();
		for (JavaNode usageNode : usageNodes) {
			JavaClass topClass = usageNode.getTopParentClass();
			if (!needUpdateClasses.contains(topClass)) {
				needUpdateClasses.add(topClass);
			}
		}

		for (JavaClass jcls : needUpdateClasses) {
			recompileJavaCode(jcls.getClassNode());
			rebuildUsage(jcls.getClassNode());

			// refresh code aren
			refreashCodeAren(jcls);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			return;
		}
		MainWindow mainWindow = contentPanel.getTabbedPane().getMainWindow();
		jnode = mainWindow.getCacheObject().getNodeCache().makeFrom(node);

		RenameDialog renameDialog = new RenameDialog(this, node.getFullName(), node);
		renameDialog.setVisible(true);
	}

	void setCaretFollowsToken(Token token) {
		try {
			// Caret follows cursor.
			int line = codeArea.getLineOfOffset(token.getOffset());
			codeArea.setCaretAtLine(line);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		node = null;
		Point pos = codeArea.getMousePosition();
		System.out.println("pos:" + pos);
		if (pos != null) {
			Token token = codeArea.viewToToken(pos);
			System.out.println("token:" + token);
			if (token != null) {
				node = codeArea.getJavaNodeAtOffset(jCls, token.getOffset());
				System.out.println("node:" + node);
				setCaretFollowsToken(token);
			}
		}
		setEnabled(node != null);
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// do nothing
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// do nothing
	}
}
