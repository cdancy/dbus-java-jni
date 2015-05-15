package org.freedesktop.dbus.resolver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.freedesktop.dbus.DBusMemberName;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * TODO: not caching all things, just interfaces and signals.
 * 
 * @author Mario Costa
 *
 */
public class DBusIntrospectionParser {

	/**
	 * Cache to store previous introspection data. TODO: consider using
	 * something like WeakHashMap<K,V> ?
	 */
	Map<String, DBusIntrospectMember> cache = new Hashtable<String, DBusIntrospectMember>();

	public DBusIntrospectMember parseIntrospectionData(String introspection) {
		
		DBusIntrospectParser parser = new DBusIntrospectParser();
		
		try {
			//TODO: one node can have more than one interface !!, it means more than one interface on one object!! 
			//this is implemented for one only ... fix it
		} catch(BreakSaxParsingException ex) {
			
		}
		
		return parser.getResult();
	}
	
	void cache(DBusIntrospectMember member) {
		cache.put(member.getName(), member);
	}

	boolean isCached(String name) {
		return cache.containsKey(name);
	}

	private static class BreakSaxParsingException extends SAXException {

		public BreakSaxParsingException() {
			super();
		}
		
		public BreakSaxParsingException(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Parses the xml, into a tree of metadata objects, used to deserialize and build proxy classes.
	 * 
	 * @author Mario Costa
	 *
	 */
	public class DBusIntrospectParser extends DefaultHandler {
		Stack<DBusIntrospectMember> stack = new Stack<DBusIntrospectMember>();
		
		DBusIntrospectMember root;
		
		DBusIntrospectMember getResult() {
			return root;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);

			// <interface name="somename">
			if (qName.equals("interface")) {
				String name = attributes.getValue("name");
				
				if(isCached(name)) {
					root = cache.get(name);
					throw new DBusIntrospectionParser.BreakSaxParsingException();
				}

				DBusIntrospectInterface member = new DBusIntrospectInterface(name); 
				stack.push(member);
				cache(member);
				
				// <method name="somename">
			} else if (qName.equals("method")) {
				String name = attributes.getValue("name");
				DBusIntrospectMethod method = new DBusIntrospectMethod(name);
				stack.push(method);

			} else if (qName.equals("signal")) {
				String name = attributes.getValue("name");
				DBusIntrospectSignal signal = new DBusIntrospectSignal(name);
				stack.push(signal);
				cache(signal);

				// <property name="VersionMajor" type="u" access="read">
			} else if (qName.equals("property")) {
				String name = attributes.getValue("name");
				String type = attributes.getValue("type");
				String access = attributes.getValue("access");

				DBusIntrospectProperty prop = new DBusIntrospectProperty(name,
						type, access);
				DBusIntrospectInterface interf = (DBusIntrospectInterface) stack
						.peek();
				interf.addMemberProperty(prop);
			} else if (qName.equals("arg")) {
				// <arg type="u" name="result" direction="out">
				String name = attributes.getValue("name");
				String type = attributes.getValue("type");
				String direction = attributes.getValue("direction");

				DBusIntrospectArg arg = new DBusIntrospectArg(name, type,
						direction);
				DBusIntrospectMember parent = stack.peek();

				if (parent.isMethod()) {
					DBusIntrospectMethod method = (DBusIntrospectMethod) parent;
					method.addArgument(arg);
				} else if (parent.isSignal()) {
					DBusIntrospectSignal signal = (DBusIntrospectSignal) parent;
					signal.addArgument(arg);
				} else {
					throw new RuntimeException("Unexpected parent member:"
							+ parent.getClass());
				}
			}

		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			super.endElement(uri, localName, qName);

			try {
				if (qName.equals("interface")) {
					DBusIntrospectInterface member = (DBusIntrospectInterface) stack
							.pop();
					
					//Top level interface, no more interfaces.
					if (!stack.isEmpty()) {
						DBusIntrospectInterface interf = (DBusIntrospectInterface) stack
								.peek();
						interf.addMemberInterface(member);
					} else {
						root = member;
					}

				} else if (qName.equals("method")) {
					DBusIntrospectMethod member = (DBusIntrospectMethod) stack
							.pop();
					DBusIntrospectInterface interf = (DBusIntrospectInterface) stack
							.peek();

					interf.addMemberMethod(member);
				} else if (qName.equals("signal")) {
					DBusIntrospectSignal member = (DBusIntrospectSignal) stack
							.pop();
					DBusIntrospectInterface interf = (DBusIntrospectInterface) stack
							.peek();

					interf.addMemberSignal(member);
				}
			} catch (java.util.EmptyStackException ex) {
				ex.printStackTrace();
			}

		}

	}

	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException {

		File f = new File(
				"/home/ubuntu/workspace/dbus-java-packagekit-client/src/main/resources/org.freedesktop.PackageKit.xml");
		javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory
				.newInstance();
		SAXParser saxParser = spf.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();

		ContentHandler handler = new DBusIntrospectionParser.DBusIntrospectParser();
		xmlReader.setContentHandler(handler);
		
		//Use this: saxParser.parse(is, dh, systemId);

		// xmlReader.setContentHandler(new DefaultHandler() {
		// @Override
		// public void startElement(String uri, String localName,
		// String qName, Attributes attributes) throws SAXException {
		// super.startElement(uri, localName, qName, attributes);
		//
		// System.out.println("uri:" + uri);
		// System.out.println("localName:" + localName);
		// System.out.println("qName:" + qName);
		//
		// }
		// });

		xmlReader
				.parse("file:///home/ubuntu/workspace/dbus-java-packagekit-client/src/main/resources/org.freedesktop.PackageKit.xml");
	}

}
