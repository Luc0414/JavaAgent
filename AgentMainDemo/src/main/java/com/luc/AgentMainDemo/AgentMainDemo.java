package com.luc.AgentMainDemo;

import com.sun.tools.attach.*;

import java.io.IOException;
import java.util.List;

public class AgentMainDemo{
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        // agentmain文件所在位置
        String path = "C:\\Users\\Luc\\Desktop\\JavaAgent\\agentmain\\target\\agentmain-1.0-SNAPSHOT-jar-with-dependencies.jar";
        // 返回Java虚拟机列表,VirtualMachineDescriptor描述Java虚拟机的容器类
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for(VirtualMachineDescriptor v:list){
            // contains() 方法用于判断字符串中是否包含指定的字符或字符串
            if(v.displayName().contains("Hello")){
                System.out.println(v.displayName());
                // 将 jvm 虚拟机的 pid 号传入 attach 来进行远程连接
                VirtualMachine vm = VirtualMachine.attach(v.id());
                // 将 agent.jar 发送给虚拟机
                vm.loadAgent(path);
                vm.detach();
            }
        }
        testMethod();
    }
    public static void testMethod(){
        System.out.println("TestMethod() is caller");
    }
}