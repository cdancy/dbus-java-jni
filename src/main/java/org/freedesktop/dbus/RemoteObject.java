/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class RemoteObject
{
   String busname;
   String objectpath;
   Class<? extends DBusInterface>[] ifaces;
   boolean autostart;
   public RemoteObject(String busname, String objectpath, Class<? extends DBusInterface>[] ifaces, boolean autostart)
   {
      this.busname = busname;
      this.objectpath = objectpath;
      this.ifaces = ifaces;
      this.autostart = autostart;
   }
  
   @Override
   public boolean equals(Object o)
   {
      if (o == null || !(o instanceof RemoteObject)) return false;
      final RemoteObject that = (RemoteObject) o;
      return Objects.equals(this.objectpath, that.objectpath)
          && Objects.equals(this.busname, that.busname);
//          && Objects.equals(this.iface, that.iface); //TODO:not sure it requires this check
   }
   @Override
   public int hashCode()
   {
      return (null == busname ? 0 : busname.hashCode()) + objectpath.hashCode(); 
//              + (null == iface ? 0 : iface.hashCode()); TODO: if its the same object path
   }
   public boolean autoStarting() { return autostart; }
   public String getBusName() { return busname; }
   public String getObjectPath() { return objectpath; }
   
   public Class<? extends DBusInterface>[]  getInterface() { 
       Class<?>[] result = new Class<?>[ifaces.length];
       System.arraycopy(ifaces, 0, result, 0, ifaces.length);
       return ifaces; 
   }
   
   @Override
   public String toString()
   {
      return busname+":"+objectpath+":"+Arrays.toString(ifaces);
   }
   
   
    public Method findMethod(String m, Class[] types) {
        Method result = null;
        boolean found = false;
        
        for (int i = 0; i < ifaces.length && !found; i++) {
            Class<?> iface = ifaces[i];
            
            try {
                result = iface.getMethod(m, types);
                found = true;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                continue;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        
        return result;
    }
}
