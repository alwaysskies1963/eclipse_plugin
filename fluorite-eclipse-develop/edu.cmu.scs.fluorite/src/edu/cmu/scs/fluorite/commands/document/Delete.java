package edu.cmu.scs.fluorite.commands.document;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class Delete extends DocChange {
	
	public Delete() {
	}

	public Delete(int offset, int length, int startLine, int endLine,
			String text, IDocument document) {
		mOffset = offset;
		mLength = length;
		mStartLine = startLine;
		mEndLine = endLine;

		mText = text;

		if (document != null) {
			// We want the AST for the source code AFTER the deletion is
			// completed.
			String documentContent = document.get();
			documentContent = documentContent.substring(0, mOffset)
					+ documentContent.substring(mOffset + mLength);
			calcNumericalValues(documentContent);
		}
	}

	private int mOffset;
	private int mLength;
	private int mStartLine;
	private int mEndLine;

	private String mText;

	public boolean execute(IEditorPart target) {
		return false;
	}

	public void dump() {
	}

	public Map<String, String> getAttributesMap() {
		Map<String, String> attrMap = new HashMap<String, String>();
		attrMap.put("offset", Integer.toString(mOffset));
		attrMap.put("length", Integer.toString(mLength));
		attrMap.put("startLine", Integer.toString(mStartLine));
		attrMap.put("endLine", Integer.toString(mEndLine));

		if (getNumericalValues() != null) {
			for (Map.Entry<String, Integer> pair : getNumericalValues().entrySet()) {
				attrMap.put(pair.getKey(), Integer.toString(pair.getValue()));
			}
		}

		return attrMap;
	}

	public Map<String, String> getDataMap() {
		Map<String, String> dataMap = new HashMap<String, String>();
		if (getText() != null) {
			dataMap.put("text", getText());
		}

		return dataMap;
	}

	@Override
	public void createFrom(Element commandElement) {
		super.createFrom(commandElement);
		
		Attr attr = null;
		NodeList nodeList = null;
		
		if ((attr = commandElement.getAttributeNode("offset")) != null) {
			mOffset = Integer.parseInt(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode("length")) != null) {
			mLength = Integer.parseInt(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode("startLine")) != null) {
			mStartLine = Integer.parseInt(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode("endLine")) != null) {
			mEndLine = Integer.parseInt(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode("endLine")) != null) {
			mEndLine = Integer.parseInt(attr.getValue());
		}
		
		if ((nodeList = commandElement.getElementsByTagName("text")).getLength() > 0) {
			Node textNode = nodeList.item(0);
			mText = checkTextValidity(textNode.getTextContent(), mLength);
		}
	}

	public String getCommandType() {
		return "Delete";
	}

	public String getName() {
		return "Delete text (offset: " + Integer.toString(mOffset)
				+ ", length: " + Integer.toString(mLength) + ", startLine: "
				+ Integer.toString(mStartLine) + ", endLine: "
				+ Integer.toString(mEndLine) + ")";
	}

	public String getDescription() {
		return null;
	}

	public String getCategory() {
		return EventRecorder.DocumentChangeCategory;
	}

	public String getCategoryID() {
		return EventRecorder.DocumentChangeCategoryID;
	}

	public int getOffset() {
		return mOffset;
	}

	public int getLength() {
		return mLength;
	}

	public void setStartLine(int startLine) {
		mStartLine = startLine;
	}

	public int getStartLine() {
		return mStartLine;
	}

	public void setEndLine(int endLine) {
		mEndLine = endLine;
	}

	public int getEndLine() {
		return mEndLine;
	}

	public String getText() {
		return mText;
	}

	public boolean combine(ICommand anotherCommand) {
		if (!(anotherCommand instanceof Delete)) {
			return false;
		}

		Delete nextCommand = (Delete) anotherCommand;

		if (nextCommand.getOffset() != getOffset()
				&& nextCommand.getOffset() + nextCommand.getLength() != getOffset()) {
			return false;
		}

		if (nextCommand.getText() == null && getText() != null
				|| nextCommand.getText() != null && getText() == null) {
			return false;
		}

		if (nextCommand.getOffset() == getOffset()) {
			mEndLine += nextCommand.getEndLine() - nextCommand.getStartLine();
			if (getText() != null) {
				mText = getText() + nextCommand.getText();
			}
		} else if (nextCommand.getOffset() + nextCommand.getLength() == getOffset()) {
			mStartLine -= nextCommand.getEndLine() - nextCommand.getStartLine();
			mOffset = nextCommand.getOffset();
			if (getText() != null) {
				mText = nextCommand.getText() + getText();
			}
		}

		mLength += nextCommand.getLength();

		replaceNumericalValues(nextCommand);

		return true;
	}

	@Override
	public void apply(IDocument doc) {
		try {
			doc.replace(getOffset(), getLength(), "");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String apply(String original) {
		try {
			return original.substring(0, getOffset())
					+ original.substring(getOffset() + getLength());
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		
		return original;
	}

	@Override
	public void apply(StringBuilder builder) {
		try {
			builder.delete(getOffset(), getOffset() + getLength());
		} catch (StringIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Range apply(Range range) {
		if (!range.contains(getDeletionRange())) {
			throw new IllegalArgumentException();
		}
		
		return new Range(range.getOffset(), range.getLength() - getLength());
	}

	@Override
	public void applyInverse(IDocument doc) {
		try {
			doc.replace(getOffset(), 0, getText());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String applyInverse(String original) {
		try {
			return original.substring(0, getOffset()) + getText()
					+ original.substring(getOffset());
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		
		return original;
	}

	@Override
	public void applyInverse(StringBuilder builder) {
		try {
			builder.insert(getOffset(), getText());
		} catch (StringIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Range applyInverse(Range range) {
		if (!range.contains(getOffset())) {
			throw new IllegalArgumentException();
		}
		
		return new Range(range.getOffset(), range.getLength() + getLength());
	}

	@Override
	public double getY1() {
		if (getNumericalValues() != null && getNumericalValues().containsKey("docLength")) {
			return 100.0 * getOffset() / (getNumericalValues().get("docLength") + getLength()); 
		}
		
		return 0;
	}

	@Override
	public double getY2() {
		if (getNumericalValues() != null && getNumericalValues().containsKey("docLength")) {
			return 100.0 * (getOffset() + getLength()) / (getNumericalValues().get("docLength") + getLength()); 
		}
		
		return 100;
	}

	@Override
	public Range getDeletionRange() {
		if (mDeletionRange == null) {
			mDeletionRange = new Range(getOffset(), getLength());
		}
		
		return mDeletionRange;
	}

	@Override
	public String getDeletedText() {
		return getText();
	}

	@Override
	public Range getInsertionRange() {
		if (mInsertionRange == null) {
			mInsertionRange = new Range(getOffset(), 0);
		}
		
		return mInsertionRange;
	}

	@Override
	public String getInsertedText() {
		return "";
	}

	@Override
	public String toString() {
		return String.format("[Delete:%d]\n%s", getCommandIndex(), getText());
	}
	
}
