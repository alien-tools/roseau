package com.github.maracas.roseau.changes;

public class RandTests {
    public class S {
        public int f ;
    }

    public class Ty extends S {

        private int f = 20;

        // Method to verify linkage error
        public void verifyLinkageError() {
            System.out.println(f); // Accessing the new field in subclass T
            System.out.println(super.f); // Accessing the old field in superclass S
        }
    }
}
