package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppClassAdapter extends ClassAdapter implements
		Opcodes, ClassAdapterFactory {
	public WebAppClassAdapter() {
		super(null);
	}

	public WebAppClassAdapter(ClassVisitor cv) {
		super(cv);
	}

	public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
		return new WebAppClassAdapter(visitor);
	}
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (name.equals("isFiltersDefined")) {
      mv = new IsFilterDefinedMethodAdapter(mv);
    }
    
    return mv;
  }
  
  private static class IsFilterDefinedMethodAdapter extends MethodAdapter implements Opcodes {
    public IsFilterDefinedMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitInsn(int opcode) {
      if (opcode == IRETURN) {
        visitInsn(POP);
        mv.visitInsn(ICONST_1);
      }
      super.visitInsn(opcode);
    }
  }
}
