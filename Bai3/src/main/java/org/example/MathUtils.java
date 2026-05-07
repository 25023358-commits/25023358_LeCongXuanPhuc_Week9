package org.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MathUtils {
    public static final Logger logger = LoggerFactory.getLogger(MathUtils.class);
    public int add(int a, int b) {
       logger.info("Adding {} and {}", a, b);
       return a + b;
    }
    public int divide(int a, int b) {
        logger.info("Dividing {} by {}", a, b);
        if (b == 0) {
            logger.error("Division by zero!");
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return a / b;
    }
}