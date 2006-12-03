/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.dso.DSOField;
import com.tc.admin.dso.DSOMapEntryField;
import com.tc.admin.dso.DSOObject;
import com.tc.admin.dso.DSOObjectVisitor;
import com.tc.admin.dso.DSORoot;
import com.tc.admin.dso.RootsHelper;

public class ListRoots implements ConnectionListener, DSOObjectVisitor {
	private ServerConnectionManager m_scm;
	private final String host;
	private final int port;

	public ListRoots(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void run() {
		synchronized (this) {
			m_scm = new ServerConnectionManager(host, port, true, this);
		}
	}

	public synchronized void list() throws Exception {
		DSORoot[] roots = RootsHelper.getHelper().getRoots(
				m_scm.getConnectionContext());				
		for (int i = 0; i < roots.length; i++) {			
			System.out.println(roots[i].getName());
			System.out.println(roots[i].getClassName());
			int fieldCount = roots[i].getFieldCount();
			for (int j = 0; j < fieldCount; j++) {
				System.out.println(roots[i].getField(j));
				DSOObject field = roots[i].getField(j);
				field.accept(this);
				//DSOField field = (DSOField) roots[i].getField(j);
				
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// String host = args.length > 0 ? args[0] : "localhost";
		// int port = args.length > 1 ? Integer.parseInt(args[1]) : 9520;
		// new ListRoots(host, port);
		new ListRoots("localhost", 9520).run();		
	}

	public synchronized void handleConnection() {		
		try {
			list();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		m_scm.getConnectionContext().reset();
	}

	public synchronized void handleException(Throwable t) {
		t.printStackTrace();
	}

	public void visitDSOField(DSOField field) {		
		if (field.getType().indexOf("Vector") != -1) {
			int fCount = field.getFieldCount();
			System.out.println("count" + fCount);
			for (int k = 0; k < fCount; k++) {
				DSOField ff = (DSOField) field.getField(k);	
				System.out.println(ff);
				for (int m = 0; m < ff.getFieldCount(); m++) {
					System.out.println("\t" + ff.getField(m));
				}
			}
		}else {
			int fCount = field.getFieldCount();
			System.out.println("count" + fCount);
			for (int k = 0; k < fCount; k++) {
				DSOField ff = (DSOField) field.getField(k);	
				System.out.println(ff);
				for (int m = 0; m < ff.getFieldCount(); m++) {
					//System.out.println("\t" + ff.getField(m));
					DSOField ss = (DSOField) ff.getField(m);
					System.out.println("\t" + ss);
					for (int n = 0; n < ss.getFieldCount(); n++) {
						System.out.println("\t\t" + ss.getField(n));
					}					
				}
			}
		}			
	}

	public void visitDSORoot(DSORoot root) {
		System.err.println("I'm a root!: " + root);
	}

	public void visitDSOMapEntryField(DSOMapEntryField mapEntryField) {		
		System.out.println(mapEntryField.getKey());		
		DSOField ss = (DSOField) mapEntryField.getValue();
		System.out.println("\t" + ss);
		for (int n = 0; n < ss.getFieldCount(); n++) {
			System.out.println("\t\t" + ss.getField(n));
		}		
	}
}
