/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kanela.agent.util.classloader;


import java.util.List;
import java.util.ArrayList;
import kanela.agent.util.log.Logger;
import net.jodah.expiringmap.ExpiringMap;

// This implementation is adapted from:
// https://github.com/glowroot/glowroot/blob/v0.9.20/agent/core/src/main/java/org/glowroot/agent/weaving/PreInitializeWeavingClasses.java

// "There are some things that agents are allowed to do that simply should not be permitted"
//
// -- http://mail.openjdk.java.net/pipermail/hotspot-dev/2012-March/005464.html
//
// in particular (at least prior to parallel class loading in JDK 7) initializing other classes
// inside of a ClassFileTransformer.transform() method occasionally leads to deadlocks
//
// this is still a problem in JDK 7+, since parallel class loading must be opted in to by custom
// class loaders, see ClassLoader.registerAsParallelCapable()
//
// to avoid initializing other classes inside of the transform() method, all classes referenced from
// WeavingClassFileTransformer are pre-initialized (and all classes referenced from those classes,
// etc)
public class PreInitializeClasses {

    // this is probably not needed, since preInitializeLinkedHashMapKeySetAndKeySetIterator() is
    // only called a single time, but just to be safe ...
    public static volatile Object toPreventDeadCodeElimination;

    private PreInitializeClasses() {}

    public static void preInitializeClasses() {
        ClassLoader loader = PreInitializeClasses.class.getClassLoader();
        for (String type : usedTypes()) {
            initialize(type, loader, true);
        }
        preExpiryMapKeySetAndKeySetIterator();
    }

    private static void initialize(String type, ClassLoader loader,
            boolean warnOnNotExists) {
        try {
            Class.forName(type, true, loader);
        } catch (ClassNotFoundException e) {
            if (warnOnNotExists) {
                Logger.warn(() -> "class not found: " + type);
            }
            // log exception at error level
            Logger.error(() -> e.getMessage(), e);
        }
    }

    public static List<String> usedTypes() {
      List<String> types = new ArrayList<String>();
      types.addAll(getBytebuddyUsedTypes());
      return types;
    }

    private static String prependByteBuddyPrefix(String s) {
      // return "net.bytebuddy" + s;
      return "kanela.agent.libs.net.bytebuddy" + s;
    }

    // List obtained from running:
    // grep -rh net.bytebuddy ./agent/src/ | sed s"/import net.bytebuddy.//" | sort -u
    // and adapted manually
    private static List<String> getBytebuddyUsedTypes() {
      List<String> types = new ArrayList<String>();
      types.add(prependByteBuddyPrefix("agent.builder.AgentBuilder"));
      types.add(prependByteBuddyPrefix("agent.builder.AgentBuilder.Listener"));
      types.add(prependByteBuddyPrefix("agent.builder.AgentBuilder.RedefinitionStrategy.ResubmissionScheduler"));
      types.add(prependByteBuddyPrefix("agent.builder.ResettableClassFileTransformer"));
      types.add(prependByteBuddyPrefix("agent.ByteBuddyAgent"));
      types.add(prependByteBuddyPrefix("asm.Advice"));
      types.add(prependByteBuddyPrefix("asm.AsmVisitorWrapper"));
      types.add(prependByteBuddyPrefix("ByteBuddy"));
      types.add(prependByteBuddyPrefix("ClassFileVersion"));
      types.add(prependByteBuddyPrefix("description.ByteCodeElement"));
      types.add(prependByteBuddyPrefix("description.field.FieldDescription"));
      types.add(prependByteBuddyPrefix("description.field.FieldList"));
      types.add(prependByteBuddyPrefix("description.method.MethodDescription"));
      types.add(prependByteBuddyPrefix("description.method.MethodList"));
      types.add(prependByteBuddyPrefix("description.NamedElement"));
      types.add(prependByteBuddyPrefix("description.type.TypeDescription"));
      types.add(prependByteBuddyPrefix("dynamic.ClassFileLocator"));
      types.add(prependByteBuddyPrefix("dynamic.ClassFileLocator.ForClassLoader"));
      types.add(prependByteBuddyPrefix("dynamic.DynamicType"));
      types.add(prependByteBuddyPrefix("dynamic.loading.ClassInjector"));
      types.add(prependByteBuddyPrefix("dynamic.scaffold.MethodGraph"));
      types.add(prependByteBuddyPrefix("dynamic.scaffold.TypeValidation"));
      types.add(prependByteBuddyPrefix("implementation.bytecode.StackManipulation"));
      types.add(prependByteBuddyPrefix("implementation.Implementation"));
      types.add(prependByteBuddyPrefix("implementation.MethodDelegation"));
      types.add(prependByteBuddyPrefix("jar.asm.ClassReader"));
      types.add(prependByteBuddyPrefix("jar.asm.ClassVisitor"));
      types.add(prependByteBuddyPrefix("jar.asm.ClassWriter"));
      types.add(prependByteBuddyPrefix("jar.asm.commons.AdviceAdapter"));
      types.add(prependByteBuddyPrefix("jar.asm.commons.ClassRemapper"));
      types.add(prependByteBuddyPrefix("jar.asm.commons.Method"));
      types.add(prependByteBuddyPrefix("jar.asm.commons.MethodRemapper"));
      types.add(prependByteBuddyPrefix("jar.asm.commons.SimpleRemapper"));
      types.add(prependByteBuddyPrefix("jar.asm.Label"));
      types.add(prependByteBuddyPrefix("jar.asm.MethodVisitor"));
      types.add(prependByteBuddyPrefix("jar.asm.Opcodes"));
      types.add(prependByteBuddyPrefix("jar.asm.tree.ClassNode"));
      types.add(prependByteBuddyPrefix("jar.asm.tree.MethodNode"));
      types.add(prependByteBuddyPrefix("jar.asm.Type"));
      types.add(prependByteBuddyPrefix("matcher.ElementMatcher"));
      types.add(prependByteBuddyPrefix("matcher.ElementMatcher.Junction"));
      types.add(prependByteBuddyPrefix("matcher.ElementMatchers"));
      types.add(prependByteBuddyPrefix("pool.TypePool"));
      types.add(prependByteBuddyPrefix("utility.JavaModule"));
      types.add(prependByteBuddyPrefix("utility.OpenedClassReader"));
      return types;
    }

    private static void preExpiryMapKeySetAndKeySetIterator() {
      toPreventDeadCodeElimination = ExpiringMap.builder().build().keySet().iterator();
  }

}
