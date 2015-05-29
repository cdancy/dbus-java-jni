/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.Arrays;

import static org.freedesktop.dbus.Gettext._;

final class RemoteInvocationHandler implements InvocationHandler
{
   private static final Logger logger= LoggerFactory.getLogger(RemoteInvocationHandler.class);
   public static final int CALL_TYPE_SYNC = 0;
   public static final int CALL_TYPE_ASYNC = 1;
   public static final int CALL_TYPE_CALLBACK = 2;
   public static Object convertRV(String sig, Object[] rp, Method m, AbstractConnection conn) throws DBusException
   {
      Class<? extends Object> c = m.getReturnType();

      if (null == rp) { 
         if(null == c || Void.TYPE.equals(c)) return null;
         else throw new DBusExecutionException(_("Wrong return type (got void, expected a value)"));
      } else {
         try {
            if (logger.isTraceEnabled()) {
                logger.trace("Converting return parameters from {} to type {}", Arrays.deepToString(rp), m.getGenericReturnType());
            }
            rp = Marshalling.deSerializeParameters(rp,new Type[] { m.getGenericReturnType() }, conn);
         }
         catch (Exception e) {
            logger.debug("exception", e);
            throw new DBusExecutionException(MessageFormat.format(_("Wrong return type (failed to de-serialize correct types: {0} )"), new Object[] { e.getMessage() }),e);
         }
      }

      switch (rp.length) {
         case 0:
            if (null == c || Void.TYPE.equals(c))
               return null;
            else throw new DBusExecutionException(_("Wrong return type (got void, expected a value)"));
         case 1:
            return rp[0];
         default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c))
               throw new DBusExecutionException(_("Wrong return type (not expecting Tuple)"));
            
            Constructor<? extends Object> cons = c.getConstructors()[0];
            try {
               return cons.newInstance(rp);
            } catch (Exception e) {
               logger.debug("Flushing outbound queue and quitting");
               throw new DBusException(e.getMessage(),e);
            }
      }
   }
   @SuppressWarnings("unchecked")
   public static Object executeRemoteMethod(RemoteObject ro, Method m, AbstractConnection conn, int syncmethod, CallbackHandler callback, Object... args) throws DBusExecutionException
   {
      Type[] ts = m.getGenericParameterTypes();
      String sig = null;
      if (ts.length > 0) try {
         sig = Marshalling.getDBusType(ts);
         args = Marshalling.convertParameters(args, ts, conn);
      } catch (DBusException DBe) {
         throw new DBusExecutionException(_("Failed to construct D-Bus type: ")+DBe.getMessage(),DBe);
      }
      MethodCall call;
      byte flags = 0;
      if (!ro.autostart) flags |= Message.Flags.NO_AUTO_START;
      if (syncmethod == CALL_TYPE_ASYNC) flags |= Message.Flags.ASYNC;
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) flags |= Message.Flags.NO_REPLY_EXPECTED;
      try {
         String name;
         
         //TODO: this bellongs to mappins logic
         if (m.isAnnotationPresent(DBusMemberName.class))
            name = m.getAnnotation(DBusMemberName.class).value();
         else
            name = m.getName();
         
         if (null == ro.ifaces)  //TODO: not sure when this happens !?
            call = new MethodCall(ro.busname, ro.objectpath, null, name,flags, sig, args);
         else {
             Class<?> ifc = m.getDeclaringClass();
             //TODO: not sure if this class is the best to have this ...
             String ifacename = DBusConnection.getDBus2JavaMappingStrategy().javaClass2DBusName(ifc);
             call = new MethodCall(ro.busname, ro.objectpath, ifacename, name, flags, sig, args);
         }
      } catch (DBusException DBe) {
         logger.debug("Dbus exception: ", DBe);
         throw new DBusExecutionException(_("Failed to construct outgoing method call: ")+DBe.getMessage(),DBe);
      }
      if (null == conn.outgoing) throw new NotConnected(_("Not Connected"));

      switch (syncmethod) {
         case CALL_TYPE_ASYNC: 
            conn.queueOutgoing(call);
            return new DBusAsyncReply(call, m, conn);
         case CALL_TYPE_CALLBACK:
             synchronized (conn.pendingCallbacks) {
                logger.debug("Queueing Callback {} for {}",callback,call);
                conn.pendingCallbacks.put(call, callback);
                conn.pendingCallbackReplys.put(call, new DBusAsyncReply(call, m, conn));
             }
             conn.queueOutgoing(call);
             return null;
         case CALL_TYPE_SYNC:
             conn.queueOutgoing(call);
             break;
      }

      // get reply
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) return null;

      Message reply = call.getReply();
      if (null == reply) throw new DBus.Error.NoReply(_("No reply within specified time"));
               
      if (reply instanceof Error)
         ((Error) reply).throwException();

      try {
         return convertRV(reply.getSig(), reply.getParameters(), m, conn);
      } catch (DBusException e) {
         logger.debug("Dbus exception:", e);
         throw new DBusExecutionException(e.getMessage(),e);
      }
   }

   AbstractConnection conn;
   RemoteObject remote;
   public RemoteInvocationHandler(AbstractConnection conn, RemoteObject remote)
   {
      super();
      this.remote = remote;
      this.conn = conn;
   }
   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (method.getName().equals("isRemote")) return true;
      else if(method.getName().equals("getObjectPath")) return remote.objectpath; 
      else if (method.getName().equals("clone")) return null;
      else if (method.getName().equals("equals")) {
         try { 
            if (1 == args.length)   
               return new Boolean(remote.equals(((RemoteInvocationHandler) Proxy.getInvocationHandler(args[0])).remote));
         } catch (IllegalArgumentException IAe) {
            return Boolean.FALSE;
         }
      }
      else if (method.getName().equals("finalize")) return null;
      else if (method.getName().equals("getClass")) return DBusInterface.class;
      else if (method.getName().equals("hashCode")) return remote.hashCode();
      else if (method.getName().equals("notify")) {
         remote.notify();
         return null;
      } else if (method.getName().equals("notifyAll")) {
         remote.notifyAll();
         return null;
      } else if (method.getName().equals("wait"))  {
         if (0 == args.length) remote.wait();
         else if (1 == args.length 
               && args[0] instanceof Long) remote.wait((Long) args[0]);
         else if (2 == args.length 
               && args[0] instanceof Long
               && args[1] instanceof Integer) 
            remote.wait((Long) args[0], (Integer) args[1]);
         if (args.length <= 2)
            return null;
      }
      else if (method.getName().equals("toString"))
         return remote.toString();

      return executeRemoteMethod(remote, method, conn, CALL_TYPE_SYNC, null, args);
   }
}

