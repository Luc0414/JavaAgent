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
