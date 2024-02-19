public class Schwefel {
	
	public static void main(String[] args) {
        int d = args.length;

        float s = 418.9829f * d;

        for(int i = 0; i < d; ++i) {
            float x = Float.parseFloat(args[i]);
            s -= x * Math.sin(Math.sqrt(Math.abs(x)));
        }
        System.out.println("schwefel " + String.valueOf(s));

    }

}