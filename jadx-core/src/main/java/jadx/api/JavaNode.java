package jadx.api;

public interface JavaNode {

	String getName();

	String getFullName();

	String getRawFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDecompiledLine();
}
