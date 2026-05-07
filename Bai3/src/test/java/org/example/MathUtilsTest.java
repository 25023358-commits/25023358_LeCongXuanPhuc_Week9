package org.example;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class MathUtilsTest {
    void testAdd(){
        MathUtils math = new MathUtils();
        assertEquals(8, new MathUtils().add(5, 3));
    }
    void testDivide(){
        MathUtils math = new MathUtils();
        assertEquals(2, math.divide(10, 5));
    }
    void testDivideByZero() {

        MathUtils math = new MathUtils();

        assertThrows(IllegalArgumentException.class,
                () -> math.divide(10, 0));
    }
}