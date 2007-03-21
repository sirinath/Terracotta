/*
@COPYRIGHT@
*/
package demo.sharededitor.models;

import demo.sharededitor.events.IListListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class ObjectManager
	implements IListListener
{
	private List objList;
	private transient List grabList;
	private transient List listeners;
	private transient BaseObject lastGrabbed;

	public ObjectManager()
	{
		objList = Collections.synchronizedList(new ArrayList());
		init_transients();
		notifyListeners(null);
	}

	public void init_transients()
	{
		listeners = Collections.synchronizedList(new ArrayList());
		grabList  = Collections.synchronizedList(new ArrayList());
	}

	public void addListener(IListListener listListener)
	{
	   synchronized(listeners) { 
   		if (!listeners.contains(listListener))
   		{
   			listeners.add(listListener);
   			listListener.changed(this, null);
   		}
	   }
	}

	public void removeListener(IListListener listListener)
	{
	   synchronized(objList) { 
   		if (listeners.contains(listListener))
   			listeners.remove(listListener);
		}
	}

	private void notifyListeners(Object obj)
	{
	   synchronized(objList) { 
   		Iterator i = listeners.iterator();
   		while (i.hasNext())
   		{
   			IListListener listListener = (IListListener)i.next();
   			listListener.changed(this, obj);
   		}
	   }
	}

	public synchronized void add(BaseObject obj)
	{
	   synchronized(objList) { 
      	if (objList.contains(obj))
      		return;

      	obj.addListener(this);
      	objList.add(obj);
      	obj.notifyListeners(obj);

      	notifyListeners(obj);
	   }
	}

	public synchronized void remove(BaseObject obj)
	{
	   synchronized(objList) { 
   		if (!objList.contains(obj))
   			return;

   		objList.remove(obj);
   		obj.notifyListeners(obj);
   		obj.removeListener(this);

   		notifyListeners(obj);
	   }
	}

	public BaseObject[] reversedList()
	{
	   synchronized(objList) { 
   		List list = new ArrayList(objList);
   		Collections.reverse(list);
   		return (BaseObject[])list.toArray(new BaseObject[0]);
	   }
	}

	public BaseObject[] list()
	{
	   synchronized(objList) { 
		   return (BaseObject[])objList.toArray(new BaseObject[0]);
	   }
	}

	public boolean canGrabAt(int x, int y)
	{
		BaseObject[] list = reversedList();
		for(int i=0; i<list.length; i++)
		{
			BaseObject obj = list[i];
			if (obj.isAt(x, y))
				return true;
		}
		return false;
	}

	public BaseObject create(int x, int y, String name)
	{
		BaseObject obj = BaseObject.createObject(name);
		obj.move(x, y);
		add(obj);
		ungrabAll();
		grab(obj, x, y);
		return obj;
	}

	public BaseObject grabAt(int x, int y, boolean ungrabAll)
	{
		BaseObject[] list = reversedList();
		for(int i=0; i<list.length; i++)
		{
			BaseObject obj = list[i];
			if (obj.isAt(x, y))
			{
				if (ungrabAll)
					ungrabAll();
				grab(obj, x, y);
				return obj;
			}
		}
		if (ungrabAll)
			ungrabAll();
		return null;
	}
	
	public BaseObject lastGrabbed()
	{
		return lastGrabbed;
	}

	private void ungrabAll()
	{
	   synchronized(grabList) { 
   		lastGrabbed = null;
   		grabList.clear();
   		notifyListeners(null);
	   }
	}

	private void grab(BaseObject obj, int x, int y)
	{
	   synchronized(grabList) { 
   		obj.selectAction(true);
		
   		if (grabList.contains(obj))
   			return;

   		grabList.add(obj);
   		obj.setGrabbedAnchorAt(x, y);

   		notifyListeners(obj);
   		lastGrabbed = obj;
	   }
	}

	private void ungrab(BaseObject obj)
	{
	   synchronized(grabList) { 
   		obj.selectAction(false);
		
   		if (!grabList.contains(obj))
   			return;

   		grabList.remove(obj);
   		notifyListeners(obj);
   		lastGrabbed = null;
	   }
	}

	private void grab(BaseObject obj)
	{
	   synchronized(grabList) { 
   		if (grabList.contains(obj))
   			return;

   		obj.selectAction(true);
   		grabList.add(obj);
   		notifyListeners(obj);
   		lastGrabbed = obj;
	   }
	}

	public void deleteSelection()
	{
	   synchronized(grabList) { 
   		Iterator i = grabList.iterator();
   		while (i.hasNext())
   			remove((BaseObject)i.next());
		}
	}

	public void selectAll()
	{
		BaseObject[] list = list();
		for(int i=0; i<list.length; i++)
			grab(list[i]);
	}
	
	public void selectAllWithin(BaseObject boundary)
	{
		BaseObject[] list = list();
		for(int i=0; i<list.length; i++)
		{
		   java.awt.Shape s1 = boundary.getShape();
		   java.awt.Shape s2 = list[i].getShape();
		   if (s1.contains(s2.getBounds2D()))
		      grab(list[i]);
		}
	}

	public void clearSelection()
	{
		BaseObject[] list = list();
		for(int i=0; i<list.length; i++)
			ungrab(list[i]);
	}

	public void invertSelection()
	{
		BaseObject[] list = list();
		for(int i=0; i<list.length; i++)
		{
			BaseObject obj = list[i];
			if (isGrabbed(obj))
				ungrab(obj);
			else
				grab(obj);
		}
	}

	public void toggleSelection()
	{
	   synchronized(objList) { 
	      synchronized(grabList) {
      		if (objList.size() == grabList.size())
      			clearSelection();
      		else
      			selectAll();
			}
		}
	}

	public boolean isGrabbed(BaseObject obj)
	{
	   synchronized(grabList) { 
   		return grabList.contains(obj);
	   }
	}

	public void changed(Object source, Object obj)
	{
		notifyListeners(obj);
	}
}
