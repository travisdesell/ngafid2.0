package org.ngafid;

import org.junit.Test;
import org.junit.platform.commons.annotation.Testable;

import static org.junit.Assert.assertEquals;

public class HelloWorldTest {
    @Test
    public void testHelloWorld() {
        String helloWorld = "Hello World!";
        assertEquals("Hello World!", helloWorld);
    }
}
