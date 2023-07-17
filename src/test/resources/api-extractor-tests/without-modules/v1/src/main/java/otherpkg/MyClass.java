package otherpackage;

public class MyClass {

    private int MAX_VALUE = 100;
    public String name; //

    public MyClass(String name) {
        this.name = name;
    }

    public void printName() {
        System.out.println("Name: " + name);
    }

    public class InnerClass {
        public int value; //

        public InnerClass(int value) {
            this.value = value;
        }

        private int getValue() {
            return value;
        }

        public class NestedInnerClass {
            public String message; //

            public NestedInnerClass(String message) {
                this.message = message;
            }

            public void printMessage() {
                System.out.println("Nested Inner Message: " + message);
            }
        }
    }

    public void performOperations() {
        class LocalClass {
            private String message;

            public LocalClass(String message) {
                this.message = message;
            }

            public void printMessage() {
                System.out.println("Message: " + message);
            }
        }

        LocalClass local = new LocalClass("Hello");
        local.printMessage();
    }
}
