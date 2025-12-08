public class Example {
    public int BadField = 0;
    private int neverUsed = 0;

    private Example() {}

    public void BadMethodName() {
        int z = 2; //should not flag, we are looking at private fields only
        int x = 0;
        for (int i = 0; i < 100; i++) { //magic number should be flagged here
            x += i;
        }
        System.out.println(x);
    }

    public String getNullValue() {
        int y = 5;
        return null; //method returns null and should be flagged
    }


}
