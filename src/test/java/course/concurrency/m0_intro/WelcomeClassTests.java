package course.concurrency.m0_intro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WelcomeClassTests {

    WelcomeClass welcomeClass = new WelcomeClass();

    @Test
    public void test() {
        String message = welcomeClass.getMessage();
        assertEquals(message, "hello");
    }
}
