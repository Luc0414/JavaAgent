package com.agent.premain;

import java.lang.instrument.Instrumentation;

public class DemoTest {
    public static void premain(String agentArgs, Instrumentation inst) throws Exception{
        System.out.println(agentArgs);
        for(int i=0;i<5;i++){
            System.out.println("premain method is invoked!");
        }
    }
}