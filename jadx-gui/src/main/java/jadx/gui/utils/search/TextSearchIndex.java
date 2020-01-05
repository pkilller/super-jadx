package jadx.gui.utils.search;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.CodeWriter;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.SearchDialog;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.UiUtils;

import static jadx.gui.ui.SearchDialog.SearchOptions.CLASS;
import static jadx.gui.ui.SearchDialog.SearchOptions.CODE;
import static jadx.gui.ui.SearchDialog.SearchOptions.FIELD;
import static jadx.gui.ui.SearchDialog.SearchOptions.IGNORE_CASE;
import static jadx.gui.ui.SearchDialog.SearchOptions.METHOD;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private final JNodeCache nodeCache;

	private SearchIndex<JNode> clsNamesIndex;
	private SearchIndex<JNode> mthNamesIndex;
	private SearchIndex<JNode> fldNamesIndex;
	private SearchIndex<CodeNode> codeIndex;
	private Map<JavaNode, Integer> indexOfClsName;
	private Map<JavaNode, Integer> indexOfMthName;
	private Map<JavaNode, Integer> indexOfFldName;
	private Map<String, Integer> indexOfClsNode;

	private List<JavaClass> skippedClasses = new ArrayList<>();

	public TextSearchIndex(JNodeCache nodeCache) {
		this.nodeCache = nodeCache;
		this.clsNamesIndex = new SimpleIndex<>();
		this.mthNamesIndex = new SimpleIndex<>();
		this.fldNamesIndex = new SimpleIndex<>();
		this.codeIndex = new CodeIndex<>();

		this.indexOfClsName = new HashMap<>();
		this.indexOfMthName = new HashMap<>();
		this.indexOfFldName = new HashMap<>();
		this.indexOfClsNode = new HashMap<>();
	}

	public void indexNames(JavaClass cls) {
		// index class names
		if (!indexOfClsName.containsKey(cls)) {
			int index = clsNamesIndex.size();
			clsNamesIndex.put(cls.getFullName(), nodeCache.makeFrom(cls));
			indexOfClsName.put(cls, index);
		} else {
			int index = indexOfClsName.get(cls);
			clsNamesIndex.replace(index, cls.getFullName(), nodeCache.makeFrom(cls));
		}

		// index mth names
		for (JavaMethod mth : cls.getMethods()) {
			if (!indexOfMthName.containsKey(mth)) {
				int index = mthNamesIndex.size();
				mthNamesIndex.put(mth.getFullName(), nodeCache.makeFrom(mth));
				indexOfMthName.put(mth, index);
			} else {
				int index = indexOfMthName.get(mth);
				mthNamesIndex.replace(index, mth.getFullName(), nodeCache.makeFrom(mth));
			}
		}

		// index fld names
		for (JavaField fld : cls.getFields()) {
			if (!indexOfFldName.containsKey(fld)) {
				int index = fldNamesIndex.size();
				fldNamesIndex.put(fld.getFullName(), nodeCache.makeFrom(fld));
				indexOfFldName.put(fld, index);
			} else {
				int index = indexOfFldName.get(fld);
				fldNamesIndex.replace(index, fld.getFullName(), nodeCache.makeFrom(fld));
			}
		}

		// index inner class
		for (JavaClass innerCls : cls.getInnerClasses()) {
			indexNames(innerCls);
		}
	}
	static String template = " ";
	public void indexCode(JavaClass cls, CodeLinesInfo linesInfo, List<StringRef> lines, boolean isUpdate) {
		// fill or clean
		String key = cls.getRawFullName();
		int comments = cls.getMethods().size() + cls.getFields().size() + 1; // methods + fields + clsName
		int lineCount = comments * 2 + lines.size();
		int clsIndex = -1;
		StringRef strref = StringRef.fromStr(template);
		if (!indexOfClsNode.containsKey(key)) {
			// reserved for some comments, for name of classes, fields and methods.
			clsIndex = codeIndex.getNextIndex();
			// fill
			for (int i = 0; i < lineCount; i ++) {
				codeIndex.put(strref, null);
			}
			indexOfClsNode.put(key, clsIndex);
		} else {
			// clean
			clsIndex = indexOfClsNode.get(key);
			for (int i = 0; i < lineCount; i ++) {
				codeIndex.replace(clsIndex + i, strref, null);
			}
		}

		try {
			boolean strRefSupported = codeIndex.isStringRefSupported();
			int count = lines.size();
			for (int i = 0; i < count; i++) {
				StringRef line = lines.get(i);
				int lineLength = line.length();
				if (lineLength == 0 || (lineLength == 1 && line.charAt(0) == '}')) {
					continue;
				}
				int lineNum = i + 1;
				JavaNode node = linesInfo.getJavaNodeByLine(lineNum);
				CodeNode codeNode = new CodeNode(nodeCache.makeFrom(node == null ? cls : node), lineNum, line);
				if (strRefSupported) {
					codeIndex.replace(clsIndex + i, line, codeNode);
				} else {
					codeIndex.replace(clsIndex + i, line.toString(), codeNode);
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to index class: {}", cls, e);
		}
	}

	public void indexCode(JavaClass cls, CodeLinesInfo linesInfo, List<StringRef> lines) {
		indexCode(cls, linesInfo, lines, false);
	}

	public Flowable<JNode> buildSearch(String text, Set<SearchDialog.SearchOptions> options) {
		boolean ignoreCase = options.contains(IGNORE_CASE);
		LOG.debug("Building search, ignoreCase: {}", ignoreCase);

		Flowable<JNode> result = Flowable.empty();
		if (options.contains(CLASS)) {
			result = Flowable.concat(result, clsNamesIndex.search(text, ignoreCase));
		}
		if (options.contains(METHOD)) {
			result = Flowable.concat(result, mthNamesIndex.search(text, ignoreCase));
		}
		if (options.contains(FIELD)) {
			result = Flowable.concat(result, fldNamesIndex.search(text, ignoreCase));
		}
		if (options.contains(CODE)) {
			if (codeIndex.size() > 0) {
				result = Flowable.concat(result, codeIndex.search(text, ignoreCase));
			}
			if (!skippedClasses.isEmpty()) {
				result = Flowable.concat(result, searchInSkippedClasses(text, ignoreCase));
			}
		}
		return result;
	}

	public Flowable<CodeNode> searchInSkippedClasses(final String searchStr, final boolean caseInsensitive) {
		return Flowable.create(emitter -> {
			LOG.debug("Skipped code search started: {} ...", searchStr);
			for (JavaClass javaClass : skippedClasses) {
				String code = javaClass.getCode();
				int pos = 0;
				while (pos != -1) {
					pos = searchNext(emitter, searchStr, javaClass, code, pos, caseInsensitive);
					if (emitter.isCancelled()) {
						LOG.debug("Skipped Code search canceled: {}", searchStr);
						return;
					}
				}
				if (!UiUtils.isFreeMemoryAvailable()) {
					LOG.warn("Skipped code search stopped due to memory limit: {}", UiUtils.memoryInfo());
					emitter.onComplete();
					return;
				}
			}
			LOG.debug("Skipped code search complete: {}, memory usage: {}", searchStr, UiUtils.memoryInfo());
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	private int searchNext(FlowableEmitter<CodeNode> emitter, String text, JavaNode javaClass, String code,
			int startPos, boolean ignoreCase) {
		int pos;
		if (ignoreCase) {
			pos = StringUtils.indexOfIgnoreCase(code, text, startPos);
		} else {
			pos = code.indexOf(text, startPos);
		}
		if (pos == -1) {
			return -1;
		}
		int lineStart = 1 + code.lastIndexOf(CodeWriter.NL, pos);
		int lineEnd = code.indexOf(CodeWriter.NL, pos + text.length());
		StringRef line = StringRef.subString(code, lineStart, lineEnd == -1 ? code.length() : lineEnd);
		emitter.onNext(new CodeNode(nodeCache.makeFrom(javaClass), -pos, line.trim()));
		return lineEnd;
	}

	public void classCodeIndexSkipped(JavaClass cls) {
		this.skippedClasses.add(cls);
	}

	public int getSkippedCount() {
		return skippedClasses.size();
	}
}
