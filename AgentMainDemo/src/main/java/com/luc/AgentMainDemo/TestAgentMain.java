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
