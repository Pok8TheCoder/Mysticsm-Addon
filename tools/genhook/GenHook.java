import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V21;

public final class GenHook {
    public static void main(String[] args) throws Exception {
        String internal = "net/minecraft/client/gui/screens/MysticismSkillPickHook";
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        writer.visit(V21, ACC_PUBLIC | ACC_SUPER, internal, null, "java/lang/Object",
                new String[]{"net/minecraft/client/gui/screens/MenuScreens$ScreenConstructor"});
        writer.visitField(ACC_PUBLIC | ACC_STATIC, "FACTORY", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor create = writer.visitMethod(ACC_PUBLIC, "create",
                "(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/screens/Screen;",
                null, null);
        create.visitCode();
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        create.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        create.visitLabel(start);
        create.visitFieldInsn(GETSTATIC, internal, "FACTORY", "Ljava/lang/invoke/MethodHandle;");
        create.visitVarInsn(ALOAD, 1);
        create.visitVarInsn(ALOAD, 2);
        create.visitVarInsn(ALOAD, 3);
        create.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke",
                "(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;)Ljava/lang/Object;",
                false);
        create.visitTypeInsn(CHECKCAST, "net/minecraft/client/gui/screens/Screen");
        create.visitLabel(end);
        create.visitInsn(ARETURN);
        create.visitLabel(handler);
        create.visitVarInsn(ASTORE, 4);
        create.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/RuntimeException");
        create.visitInsn(org.objectweb.asm.Opcodes.DUP);
        create.visitVarInsn(ALOAD, 4);
        create.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
        create.visitInsn(ATHROW);
        create.visitMaxs(0, 0);
        create.visitEnd();
        writer.visitEnd();

        Path out = Path.of(args[0]);
        Files.createDirectories(out.getParent());
        Files.write(out, writer.toByteArray());
    }
}
