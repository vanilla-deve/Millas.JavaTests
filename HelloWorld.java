// Java Tests and Basics

// Imports
import java.util.Scanner;

// Class (main definition.)
public class HelloWorld{
    // Main function to run
    public static void main(String[] args){
        // So, prints a line, the usual.
        System.out.println("Hello, World");
        // This defines the scanner, or input.
        Scanner nameIn = new Scanner(System.in);
        System.out.println("What's your name?");
        // This advances the scanner past the current line and returns the input.
        String nameObj = nameIn.nextLine();
        // Actual printing system which uses the variable after it's returned.
        System.out.println("So... Your name is " + nameObj + ", right?");
        System.out.println("Then, " + nameObj + " I guess nice to meet you!");
        System.out.println("Bye!");
    }
}