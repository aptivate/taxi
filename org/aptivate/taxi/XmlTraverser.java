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
		
		void append(Node child)
		{
			children.add(child);
		}
		
		public String name()
		{
			return name;
		}
		
		public List<Node> children()
		{
			return new ArrayList<Node>(children);
		}
		
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

		public String text()
		{
			if (children.size() != 1)
			{
				throw new AssertionFailedError("Expected to find only " +
					"one node within " + toString());
			}
			
			Node child = children.get(0);

			if (!(child instanceof XmlTraverser.TextNode))
			{
				throw new AssertionFailedError("Expected to find only " +
					"text within " + toString());
			}
			
			return child.name();
		}

		public abstract String toString();
	}
	
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
	}
	
	/**
	 * A {@link Node} that represents an XML element.
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
	public static Node parse(InputStream input, String expectedRootNode)
	throws XMLStreamException
	{
		XMLStreamReader parser = 
			XMLInputFactory.newInstance().createXMLStreamReader(input);
		return parse(parser, expectedRootNode);
	}

	public static Node parse(XMLStreamReader parser, String expectedRootNode)
	throws XMLStreamException
	{
		Stack<Node> stack = new Stack<Node>();
		Node current = null, root = null;
		
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
}