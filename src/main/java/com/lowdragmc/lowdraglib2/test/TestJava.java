package com.lowdragmc.lowdraglib2.test;

import java.util.Arrays;
import java.util.List;

public class TestJava {
    public class A<T> {
        public List<T> list;
        public T t;
    }

    public class B extends A<Boolean> {

    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(B.class.getFields()));
    }
}
