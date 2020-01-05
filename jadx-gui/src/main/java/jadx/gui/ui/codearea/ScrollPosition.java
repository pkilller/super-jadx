package jadx.gui.ui.codearea;

import java.awt.*;

public class ScrollPosition {
	Point mPoint;
	int mLine;
	public ScrollPosition(Point point, int line) {
		mPoint = point;
		mLine = line;
	}

	public Point getPoint() {
		return mPoint;
	}

	public int getLine() {
		return mLine;
	}
}
