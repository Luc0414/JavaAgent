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
