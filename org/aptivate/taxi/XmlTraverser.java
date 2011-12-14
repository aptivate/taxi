/**
 * 
 */
package org.aptivate.taxi;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;

import org.aptivate.taxi.XmlTraverser.ElementNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * <h1>The Trivial API for XML Interpretation.</h1>
 * A very simple API for navigating an XML tree and extracting information.
 * Much simpler than both DOM and SAX.
 * @author Chris Wilson <chris+github@aptivate.org>
 * @see http://rita.logscluster.org/browser/rita/src/org/wfp/rita/util/XmlTraverser.java
 * @see http://rita.logscluster.org/browser/rita/src/org/wfp/rita/web/controller/ExecutableJarServlet.java 
 */
public class XmlTraverser
{
	public static abstract class Node
	{
		private String name;
		private List<Node> children = new ArrayList<Node>();
		
		public Node(String name)
		{
			this.name = name;
		}

		/**
		 * Append a new child node to this node, after the existing
		 * children.
		 */
		void append(Node child)
		{
			children.add(child);
		}

		/**
		 * @return the "local name" (simple name) of this Node, for example
		 * <code>a</code> or <code>tr</code>.
		 */
		public String name()
		{
			return name;
		}
		
		/**
		 * @return the list of all children of the current Node.
		 */
		public List<Node> children()
		{
			return new ArrayList<Node>(children);
		}
		
		/**
		 * @return the first child of the current Node with the specified
		 * name, for example the first <code>a</code> or <code>tr</code>
		 * child node. Returns null if the node has no children of the
		 * specified type.
		 */
		public Node firstChild(String name)
		{
			for (Node child : children)
			{
				if (child.name.equals(name))
				{
					return child;
				}
			}
			
			return null;
		}
		
		/**
		 * @return the first child of the current Node with the specified
		 * name, for example the first <code>a</code> or <code>tr</code>
		 * child node. Unlike {@link #firstChild(String)}, this method
		 * throws an exception if the node has no children of the specified
		 * type, instead of returning null.
		 */
		public Node forceChild(String name)
		{
			Node child = firstChild(name);
			
			if (child == null)
			{
				throw new AssertionFailedError("Expected to find <" + 
					name + "> within " + toString());
			}
			
			return child;
		}

		/**
		 * @return the Nth child of the current Node, enforcing that it
		 * has the specified name, or throwing an exception if it does not.
		 */
		public Node nthChild(int index, String name)
		{
			if (index >= children.size())
			{
				throw new AssertionFailedError("Expected child " +
						index + " <" + name + "> not found in " +
						toString());
			}
			
			Node child = children.get(index);
			
			if (!child.name.equals(name))
			{
				throw new AssertionFailedError("Expected <" + 
						name + "> but found <" + child.name + "> " +
						"as child " + index + " of " + toString());
			}
			
			return child;
		}

		/**
		 * @return the Nth child of the current Node with the specified
		 * name, or throws an exception if it does not exist.
		 */
		public Node nthChildOfType(int index, String name)
		{
			int remaining = index;
			
			for (Node child : children)
			{
				if (child.name().equals(name))
				{
					if (remaining == 0)
					{
						return child;
					}
					else
					{
						remaining--;
					}
				}
			}

			throw new AssertionFailedError("Expected <" + name + "> " +
					"number " + index + " not found in " + toString());
		}

		/**
		 * @return the text data contained within the only text child
		 * of this node.
		 */
		public String text()
		{
			if (children.size() != 1)
			{
				throw new AssertionFailedError("Expected to find just " +
					"one TextNode within " + toString());
			}
			
			Node child = children.get(0);

			if (!(child instanceof XmlTraverser.TextNode))
			{
				throw new AssertionFailedError("Expected to find just " +
					"one TextNode within " + toString());
			}
			
			return child.name();
		}

		public abstract String toString();
	}
	
	/**
	 * A {@link Node} that represents text content (between Elements)
	 * in an XML document, like DOM {@link Text}. 
	 */
	public static class TextNode extends Node
	{
		public TextNode(String text)
		{
			super(text);
		}

		public String toString()
		{
			return '"' + name() + '"';
		}
		
		void append(Node child)
		{
			throw new IllegalArgumentException("Cannot add children " +
					"to a text node");
		}

		/**
		 * @return the text data contained by this TextNode.
		 */
		public String text()
		{
			return name();
		}
	}
	
	/**
	 * A {@link Node} that represents an XML element, like a DOM
	 * {@link Element}.
	 */
	public static class ElementNode extends Node
	{
		private Map<String, String> attrs = new HashMap<String, String>();
		
		public ElementNode(String name)
		{
			super(name);
		}
		
		public void attr(String name, String value)
		{
			attrs.put(name, value);
		}
		
		/**
		 * Returns the value of the named attribute of the element. 
		 * @param name
		 * @return
		 */
		public String attr(String name)
		{
			return attrs.get(name);
		}

		/**
		 * Output something that looks vaguely like an XML element.
		 */
		public String toString()
		{
			StringBuilder str = new StringBuilder();
			str.append("<").append(name());
			
			for (String attrName : attrs.keySet())
			{
				str.append(" ").append(attrName).append("='").append(attrs.get(attrName)).append("'");
			}
			
			str.append(">");
			
			for (Node child : children())
			{
				str.append(child.toString());
			}
			
			str.append("</").append(name()).append(">\n");
			return str.toString();
		}
	}

	/**
	 * The main entry point. Parses a string, checks that its root node is
	 * of the expected type, and returns it as a {@link Node}.
	 * @param input The stream to parse.
	 * @param expectedRootNode The expected root node type.
	 * @return the root {@link Node}.
	 * @throws XMLStreamException if the parse fails.
	 */
	public static Node parse(String input, String expectedRootNode)
	throws XMLStreamException
	{
		XMLStreamReader parser = 
			XMLInputFactory.newInstance().createXMLStreamReader(
				new StringReader(input));
		return parse(parser, expectedRootNode);
	}

	/**
	 * The main entry point. Parses a stream, checks that its root node is
	 * of the expected type, and returns it as a {@link Node}.
	 * @param input The stream to parse.
	 * @param expectedRootNode The expected root node type.
	 * @return the root {@link Node}.
	 * @throws XMLStreamException if the parse fails.
	 */
	public static ElementNode parse(InputStream input, String expectedRootNode)
	throws XMLStreamException
	{
		XMLStreamReader parser = 
			XMLInputFactory.newInstance().createXMLStreamReader(input);
		return parse(parser, expectedRootNode);
	}

	public static ElementNode parse(XMLStreamReader parser, String expectedRootNode)
	throws XMLStreamException
	{
		Stack<ElementNode> stack = new Stack<ElementNode>();
		ElementNode current = null, root = null;
		
		for (int event = parser.next(); 
			event != XMLStreamConstants.END_DOCUMENT;
			event = parser.next())
		{
			if (event == XMLStreamConstants.START_ELEMENT)
			{
				ElementNode newNode = new ElementNode(parser.getLocalName());

				for (int i = 0; i < parser.getAttributeCount(); i++)
				{
					newNode.attr(parser.getAttributeLocalName(i),
						parser.getAttributeValue(i));
				}
				
				if (current == null)
				{
					current = newNode;
					root = newNode;
					
					if (! root.name().equals(expectedRootNode))
					{
						throw new ComparisonFailure("Wrong root node", 
							expectedRootNode, root.name());
					}
				}
				else
				{
					current.append(newNode);
				}
				
				stack.push(newNode);
				current = newNode;
			}
			else if (event == XMLStreamConstants.END_ELEMENT)
			{
				if (! current.name().equals(parser.getLocalName()))
				{
					throw new ComparisonFailure("End element", 
						current.name(), parser.getLocalName());
				}
				
				Node popped = stack.pop();

				if (! popped.equals(current))
				{
					throw new ComparisonFailure("Top of stack", 
						current.name(), popped.name());
				}
				
				if (stack.isEmpty())
				{
					current = null;
				}
				else
				{
					current = stack.peek();
				}
			}
			else if (event == XMLStreamConstants.CHARACTERS)
			{
				String text = parser.getText();
				
				if (text.matches("\\s+"))
				{
					// ignore whitespace
				}
				else
				{
					current.append(new TextNode(text));
				}
			}
		}
		
		if (! stack.isEmpty())
		{
			throw new AssertionFailedError("Expected empty stack at " +
				"end of document but still had: " + stack);
		}
		
		return root;
	}
	
	private static ElementNode toNode(Element domNode)
	{
		ElementNode ourNode = new ElementNode(domNode.getNodeName());
		NodeList children = domNode.getChildNodes();
		
		for (int i = 0; i < children.getLength(); i++)
		{
			org.w3c.dom.Node domChild = children.item(i);
			switch (domChild.getNodeType())
			{
			case org.w3c.dom.Node.ATTRIBUTE_NODE:
				Attr attr = (Attr) domChild;
				ourNode.attr(attr.getNodeName(), attr.getNodeValue());
				break;
			case org.w3c.dom.Node.ELEMENT_NODE:
				ourNode.append(toNode((Element) domChild));
				break;
			case org.w3c.dom.Node.TEXT_NODE:
				ourNode.append(new TextNode(domChild.getNodeValue()));
				break;
			case org.w3c.dom.Node.COMMENT_NODE:
				// ignore comments completely
				break;
			default:
				throw new IllegalArgumentException("Don't know how to " +
					"parse " + domChild);	
			}
		}
		
		return ourNode;
	}
	
	/**
	 * The main entry point for parsing a DOM {@link Document}.
	 * @return the root {@link ElementNode element} of the document.
	 */
	public static ElementNode parse(Document doc, String expectedRootNode)
	{
		if (doc.getChildNodes().getLength() != 1)
		{
			throw new AssertionFailedError("document had wrong number " +
				"of root nodes");
		}
		
		return parse(doc.getFirstChild(), expectedRootNode);
	}

	/**
	 * The main entry point for parsing a DOM {@link DocumentFragment}.
	 * @return the root {@link ElementNode element} of the document.
	 */
	public static ElementNode parse(DocumentFragment doc, String expectedRootNode)
	{
		if (doc.getChildNodes().getLength() != 1)
		{
			throw new AssertionFailedError("fragment had wrong number " +
				"of child nodes: " + doc.getChildNodes().getLength() + 
				": " + doc);
		}
		
		return parse(doc.getFirstChild(), expectedRootNode);
	}

	private static ElementNode parse(org.w3c.dom.Node doc, String expectedRootNode)
	{
		ElementNode root = toNode((Element) doc);

		if (root == null)
		{
			throw new AssertionFailedError("Missing root node");
		}

		if (root.name() == null)
		{
			throw new ComparisonFailure("Wrong root node", 
					expectedRootNode, "null");
		}
		
		if (! root.name().equals(expectedRootNode))
		{
			throw new ComparisonFailure("Wrong root node", 
				expectedRootNode, root.name());
		}
		
		return root;
	}

}