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
