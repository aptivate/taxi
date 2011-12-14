package org.aptivate.taxi;

import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.html.dom.HTMLDocumentImpl;
import org.aptivate.taxi.XmlTraverser.ElementNode;
import org.aptivate.taxi.XmlTraverser.Node;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;

public class XmlTraverserTest extends TestCase
{
	public void testParse() throws Exception
	{
		InputStream is = getClass().getResourceAsStream("sample.xml");
		Node project = XmlTraverser.parse(is, "project");
		
		Node plugins = project.forceChild("build")
			.forceChild("pluginManagement")
			.forceChild("plugins");
		
		Node assemblyPluginNode = null;
		
		for (Node plugin : plugins.children())
		{
			if (plugin.forceChild("artifactId").text().equals("maven-assembly-plugin"))
			{
				assemblyPluginNode = plugin;
			}
		}
		
		assertNotNull(assemblyPluginNode);
		
		Node mainClass = assemblyPluginNode.forceChild("configuration")
			.forceChild("archive")
			.forceChild("manifest")
			.forceChild("mainClass");
		assertEquals("org.wfp.rita.util.JettyLoader", mainClass.text());
	}
	
	public void testAttribute() throws Exception
	{
		ElementNode a = (ElementNode) XmlTraverser.parse("<a href='foo' />", "a");
		assertEquals("foo", a.attr("href"));
	}
	
	private static final String BASIC_XML = "<a><b /><c><!--comment-->" +
		"sometext</c><!--comment--></a>";
	
	private void assertBasicHtml(Node root)
	{
		assertEquals("a", root.name());
		List<Node> children = root.children();
		assertEquals("b", children.get(0).name());
		assertEquals("c", children.get(1).name());
		assertEquals("sometext", children.get(1).text());
	}

	public void testTraverse() throws Exception
	{
		assertBasicHtml(XmlTraverser.parse(BASIC_XML, "a"));
	}

	public void testTraverseNekoDom() throws Exception
	{
		DOMParser parser = new DOMParser();
		InputSource iso = new InputSource(new StringReader(BASIC_XML));
		parser.parse(iso);
		// Neko will "fix" the XML into a valid HTML document
		Node html = XmlTraverser.parse(parser.getDocument(), "HTML");
		Node body = html.forceChild("BODY");
		Node a = body.forceChild("A");
		Node b = a.nthChild(0, "B");
		Node c = b.nthChild(0, "C");
		assertEquals("sometext", c.text());
	}

	/**
	 * This test fails because the parser does something weird, ends up
	 * putting the B after the A, instead of inside it.
	 */
	public void testTraverseNekoFragment() throws Exception
	{
		DOMFragmentParser parser = new DOMFragmentParser();
		HTMLDocument document = new HTMLDocumentImpl();
		InputSource iso = new InputSource(new StringReader(BASIC_XML));
		DocumentFragment fragment = document.createDocumentFragment();
		parser.parse(iso, fragment);
		assertBasicHtml(XmlTraverser.parse(fragment, "a"));
	}

	public void testTraverseW3CDom() throws Exception
	{
		DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		InputSource iso = new InputSource(new StringReader(BASIC_XML));
		Document doc = dBuilder.parse(iso);
		assertBasicHtml(XmlTraverser.parse(doc, "a"));
	}

	public void testGetNthChild() throws Exception
	{
		Node a = XmlTraverser.parse("<a>" +
				"<b id='1' />" +
				"<c id='2' />" +
				"<b id='3' />" +
				"<c id='4' />" +
				"</a>", "a");
		assertEquals("1", ((ElementNode) a.nthChild(0, "b")).attr("id"));
		assertEquals("2", ((ElementNode) a.nthChild(1, "c")).attr("id"));
		assertEquals("3", ((ElementNode) a.nthChild(2, "b")).attr("id"));
		assertEquals("4", ((ElementNode) a.nthChild(3, "c")).attr("id"));
	}

	public void testGetNthChildOfType() throws Exception
	{
		Node a = XmlTraverser.parse("<a>" +
				"<b id='1' />" +
				"<c id='2' />" +
				"<b id='3' />" +
				"<c id='4' />" +
				"</a>", "a");
		assertEquals("1", ((ElementNode) a.nthChildOfType(0, "b")).attr("id"));
		assertEquals("2", ((ElementNode) a.nthChildOfType(0, "c")).attr("id"));
		assertEquals("3", ((ElementNode) a.nthChildOfType(1, "b")).attr("id"));
		assertEquals("4", ((ElementNode) a.nthChildOfType(1, "c")).attr("id"));
	}
}
