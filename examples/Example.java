public class Example {
    public int BadField = 0;

    private Example() {}

    public void BadMethodName() {
        int z = 2;
        int x = 0;
        for (int i = 0; i < 100; i++) {
            x += i;
        }
        System.out.println(x);
    }

    public String getNullValue() {
        int y = 5;
        return null;
    }


}
