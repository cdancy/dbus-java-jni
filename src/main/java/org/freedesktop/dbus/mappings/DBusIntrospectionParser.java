package org.freedesktop.dbus.mappings;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class for parsing the introspection data, into a metada structure.
 * 
 * <ul>
 * <li>node</li>
 * Represents an object.
 * <li>interface</li>
 * Interface provided by the object.
 * <li>signal</li>
 * Signal emitted by object.
 * <li>method</li>
 * Method provided by the object.
 * <li>property</li>
 * Properties provided by the object.
 * </ul>
 * 
 * The structure, can be repeated, as one node, can have, children nodes
 * (objects) ...
 * 
 * TODO: in future implement, recursive node introspection, check is
 * requirements makes sense.
 * 
 * @author Mario Costa
 * 
 *         For introspection format, see:
 * @see http 
 *      ://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format
 *
 *      For the xml format Schema, see:
 * @see http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd
 *
 */
public class DBusIntrospectionParser {
    
    private static final Logger logger=LoggerFactory.getLogger(DBusIntrospectionParser.class);
    
    /**
     * Cache to store previous introspection data. something like
     * WeakHashMap<K,V> ?
     * 
     * The cache, is synchronized, so more than one thread updating the cache
     * should not result in any issue.
     */
    Map<String, DBusIntrospectMember> cache = new Hashtable<String, DBusIntrospectMember>();

    SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    public List<DBusIntrospectMember> parseIntrospectionData(Reader introspectReader) {

	DBusIntrospectParser contentHandler = new DBusIntrospectParser();

	try {
	    SAXParser parser = parserFactory.newSAXParser();
	    XMLReader xmlReader = parser.getXMLReader();
	    xmlReader.setContentHandler(contentHandler);
	    xmlReader.parse(new InputSource(introspectReader));
	} catch (ParserConfigurationException | SAXException | IOException e) {
	    logger.info("Something went wrong parsing: {}", e.getCause());
	}

	return contentHandler.getResult();
    }

    public List<DBusIntrospectMember> parseIntrospectionData(String introspection) {
	return parseIntrospectionData(new StringReader(introspection));
    }

    private void cache(DBusIntrospectMember member) {
	cache.put(member.getName(), member);
    }

    private boolean isCached(String name) {
	return cache.containsKey(name);
    }

    /**
     * Parses the xml, into a tree of metadata objects, used to deserialize and
     * build proxy classes.
     * 
     * @author Mario Costa
     *
     */
    public class DBusIntrospectParser extends DefaultHandler {
	Stack<DBusIntrospectMember> stack = new Stack<DBusIntrospectMember>();

	/**
	 * The members of the root node, from introspection data.
	 */
	List<DBusIntrospectMember> root = new ArrayList<DBusIntrospectMember>();

	List<DBusIntrospectMember> getResult() {
	    return root;
	}

	private boolean skipInterface = false;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	    super.startElement(uri, localName, qName, attributes);

	    // Skip do not process parsing.
	    if (skipInterface)
		return;

	    // <interface name="somename">
	    if (qName.equals("interface")) {
		String name = attributes.getValue("name");

		/**
		 * If is cached, skip parsing this interface.
		 */
		if (isCached(name)) {
		    root.add(cache.get(name));
		    skipInterface = true;
		    return;
		}

		DBusIntrospectInterface interf = new DBusIntrospectInterface(name);
		stack.push(interf);
		cache(interf);

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

		DBusIntrospectProperty prop = new DBusIntrospectProperty(name, type, access);
		DBusIntrospectInterface interf = (DBusIntrospectInterface) stack.peek();
		interf.addMemberProperty(prop);
	    } else if (qName.equals("arg")) {
		// <arg type="u" name="result" direction="out">
		String name = attributes.getValue("name");
		String type = attributes.getValue("type");
		String direction = attributes.getValue("direction");

		DBusIntrospectArg arg = new DBusIntrospectArg(name, type, direction);
		DBusIntrospectMember parent = stack.peek();

		if (parent.isMethod()) {
		    DBusIntrospectMethod method = (DBusIntrospectMethod) parent;
		    method.addArgument(arg);
		} else if (parent.isSignal()) {
		    DBusIntrospectSignal signal = (DBusIntrospectSignal) parent;
		    signal.addArgument(arg);
		} else {
		    throw new RuntimeException("Unexpected parent member:" + parent.getClass());
		}
	    }

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	    super.endElement(uri, localName, qName);

	    if (skipInterface) {
		if (qName.equals("interface")) {
		    skipInterface = false;
		}

	    } else {
		if (qName.equals("interface")) {
		    /* There are no interface members */
		    DBusIntrospectInterface interf = (DBusIntrospectInterface) stack.pop();
		    root.add(interf);

		} else if (qName.equals("method")) {
		    DBusIntrospectMethod member = (DBusIntrospectMethod) stack.pop();
		    DBusIntrospectInterface interf = (DBusIntrospectInterface) stack.peek();

		    interf.addMemberMethod(member);
		} else if (qName.equals("signal")) {
		    DBusIntrospectSignal member = (DBusIntrospectSignal) stack.pop();
		    DBusIntrospectInterface interf = (DBusIntrospectInterface) stack.peek();

		    interf.addMemberSignal(member);
		}
	    }
	}

    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

	File f = new File("/home/ubuntu/workspace/dbus-java-packagekit-client/src/main/resources/org.freedesktop.PackageKit.xml");

	DBusIntrospectionParser introspectParser = new DBusIntrospectionParser();

	List<DBusIntrospectMember> members = introspectParser.parseIntrospectionData(new FileReader(f));

	for (DBusIntrospectMember member : members) {
	    System.out.println("member name: " + member.getName());
	}

	List<DBusIntrospectMember> members1 = introspectParser.parseIntrospectionData(new FileReader(f));

	for (DBusIntrospectMember member : members1) {
	    System.out.println("member name: " + member.getName());
	}
	
	System.out.println("equals: " + members.equals(members1));

    }

}
