package app;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class MainTest {
    @Test void addsNumbers() { assertEquals(42, Main.add(20, 22)); }
}
