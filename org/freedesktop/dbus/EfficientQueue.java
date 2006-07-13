package org.freedesktop.dbus;

/**
 * Provides a DBusMessage queue which doesn't allocate objects
 * on insertion/removal. */
class EfficientQueue
{
   private DBusMessage[] mv;
   private int start;
   private int end;
   private int init_size;
   public EfficientQueue(int initial_size)
   {
      init_size = initial_size;
      shrink();
   }
   private void grow()
   {
      // create new vectors twice as long
      DBusMessage[] oldmv = mv;
      mv = new DBusMessage[oldmv.length*2];

      // copy start->length to the start of the new vector
      System.arraycopy(oldmv,start,mv,0,oldmv.length-start);
      // copy 0->end to the next part of the new vector
      if (end != (oldmv.length-1)) {
         System.arraycopy(oldmv,0,mv,oldmv.length-start,end+1);
      }
      // reposition pointers
      start = 0;
      end = oldmv.length;
   }
   // create a new vector with just the valid keys in and return it
   public long[] getKeys()
   {
      int size;
      if (start < end) size = end-start-1;
      else size = mv.length-(start-end);
      long[] lv = new long[size];
      System.arraycopy(mv,start,lv,0,mv.length-start);
      if (end != (mv.length-1)) 
         System.arraycopy(mv,0,lv,mv.length-start,end+1);
      return lv;
   }
   private void shrink()
   {
      if (null != mv && mv.length == init_size) return;
      // reset to original size
      mv = new DBusMessage[init_size];
      start = 0;
      end = 0;
   }
   public void add(DBusMessage m)
   {
      // put this at the end
      mv[end] = m;
      // move the end
      if (end == (mv.length-1)) end = 0; else end++;
      // if we are out of space, grow.
      if (end == start) grow();
   }
   public DBusMessage remove()
   {
      if (start == end) return null;
      // find the item
      int pos = start;
      // get the value
      DBusMessage m = mv[pos];
      // set it as unused
      mv[pos] = null;
      if (start == (mv.length-1)) start = 0; else start++;
      return m;
   }
   public boolean isEmpty()
   {
      // check if find succeeds
      return start == end;
   }   
}