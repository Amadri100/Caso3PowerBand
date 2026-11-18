package powerband.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainTest {

    @Test
    void mainClassIsAvailable() {
        assertDoesNotThrow(() -> Class.forName("powerband.main.Main"));
    }
}
