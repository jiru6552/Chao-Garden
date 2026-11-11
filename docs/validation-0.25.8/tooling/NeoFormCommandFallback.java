import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Build-environment adapter for restricted ProcessHandle metadata. */
public final class NeoFormCommandFallback {
  public static void premain(String ignored, Instrumentation instrumentation) {
    instrumentation.addTransformer(new ClassFileTransformer() {
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> prior,
          ProtectionDomain domain,byte[] original) {
        if (!name.equals("net/neoforged/neoform/runtime/engine/NeoFormEngine")
            && !name.equals("net/neoforged/neoform/runtime/actions/ExternalJavaToolAction")) return null;
        ClassReader reader=new ClassReader(original);
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        int[] patched={0};
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
          @Override public MethodVisitor visitMethod(int access,String method,String descriptor,
              String signature,String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,method,descriptor,signature,exceptions)) {
              boolean command;
              @Override public void visitMethodInsn(int opcode,String owner,String call,String desc,boolean itf) {
                if(command && owner.equals("java/util/Optional") && call.equals("orElseThrow")
                    && desc.equals("()Ljava/lang/Object;")) {
                  super.visitLdcInsn("java.home");
                  super.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/System","getProperty","(Ljava/lang/String;)Ljava/lang/String;",false);
                  super.visitLdcInsn("/bin/java");
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","concat","(Ljava/lang/String;)Ljava/lang/String;",false);
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/util/Optional","orElse","(Ljava/lang/Object;)Ljava/lang/Object;",false);
                  command=false;patched[0]++;
                } else {
                  super.visitMethodInsn(opcode,owner,call,desc,itf);
                  command=owner.equals("java/lang/ProcessHandle$Info") && call.equals("command")
                      && desc.equals("()Ljava/util/Optional;");
                }
              }
            };
          }
        },0);
        if(patched[0]!=1)throw new IllegalStateException("Unexpected NeoForm command lookup count: "+name+" = "+patched[0]);
        System.err.println("Applied sandbox executable fallback to "+name+" (build environment only).");
        return writer.toByteArray();
      }
    },false);
  }
}
