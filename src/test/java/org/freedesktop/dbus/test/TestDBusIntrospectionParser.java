package org.freedesktop.dbus.test;

import static org.junit.Assert.*;

import java.util.List;

import org.freedesktop.dbus.mappings.DBusIntrospectMember;
import org.freedesktop.dbus.mappings.DBusIntrospectionParser;
import org.junit.Test;

public class TestDBusIntrospectionParser {

    @Test
    public void test() {
	String introspect1 = 
		"<node>" + 
		"<interface name=\"interf1\"><method name=\"method1\"></method></interface>" + 
		"<interface name=\"interf2\"></interface>" + 
		"</node>";

	DBusIntrospectionParser parser = new DBusIntrospectionParser();
	
	List<DBusIntrospectMember> results = parser.parseIntrospectionData(introspect1);
	
	assertEquals("First interface name", results.get(0).getName(), "interf1");
	assertEquals("Second interface name", results.get(1).getName(), "interf2");
	
	List<DBusIntrospectMember> results1 = parser.parseIntrospectionData(introspect1);
	
	assertTrue("Parsed objects are cached, parsing the same input resuts in the same objects", results.get(0) == results1.get(0));
	
    }

}
