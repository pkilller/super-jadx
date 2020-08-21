package jadx.api;

public interface JavaNode {

	void setName(String name);

	String getName();

	String getFullName();

	String getRawFullName();

//	String getAlias();
//
	String getAliasFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDecompiledLine();
}
