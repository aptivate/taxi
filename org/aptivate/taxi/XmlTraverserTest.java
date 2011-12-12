package org.aptivate.taxi;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.aptivate.taxi.XmlTraverser.ElementNode;
import org.aptivate.taxi.XmlTraverser.Node;

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

	public void testTraverse() throws Exception
	{
		ElementNode a = (ElementNode) XmlTraverser.parse("<a><b /><c>sometext</c></a>", "a");
		assertEquals("a", a.name());
		List<Node> children = a.children();
		assertEquals("b", children.get(0).name());
		assertEquals("c", children.get(1).name());
		assertEquals("sometext", children.get(1).text());
	}
}
