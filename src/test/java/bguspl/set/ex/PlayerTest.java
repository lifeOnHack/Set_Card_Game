package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.getScore() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, ""), ui, util);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void keyPressed_insertOneElem() {
        assertTrue(player.inputQ.isEmpty());
        int elem = 2;
        player.keyPressed(elem);
        assertFalse(player.inputQ.isEmpty());
        assertEquals(1, player.inputQ.size());
        assertTrue(player.inputQ.contains(elem));
    }

    @Test
    void keyPressed_insertThreeElem() {
        assertTrue(player.inputQ.isEmpty());
        int elem1 = 2;
        int elem2 = 7;
        int elem3 = 8;
        player.keyPressed(elem1);
        player.keyPressed(elem2);
        player.keyPressed(elem3);
        assertFalse(player.inputQ.isEmpty());
        assertEquals(3, player.inputQ.size());
        assertTrue(player.inputQ.contains(elem1));
        assertTrue(player.inputQ.contains(elem2));
        assertTrue(player.inputQ.contains(elem3));
    }

    @Test
    void keyPressed_insertFourElem() {
        assertTrue(player.inputQ.isEmpty());
        int elem1 = 2;
        int elem2 = 7;
        int elem3 = 8;
        int elem4 = 4;
        player.keyPressed(elem1);
        player.keyPressed(elem2);
        player.keyPressed(elem3);
        player.keyPressed(elem4);
        assertFalse(player.inputQ.isEmpty());
        assertEquals(3, player.inputQ.size());
        assertTrue(player.inputQ.contains(elem1));
        assertTrue(player.inputQ.contains(elem2));
        assertTrue(player.inputQ.contains(elem3));
        assertFalse(player.inputQ.contains(elem4));
    }

    @Test
    void keyPressed_insertSameElemMultTimes() {
        assertTrue(player.inputQ.isEmpty());
        int elem = 3;
        player.keyPressed(elem);
        player.keyPressed(elem);
        assertFalse(player.inputQ.isEmpty());
        assertEquals(2, player.inputQ.size());
        assertTrue(player.inputQ.contains(elem));
    }

    /*
     * @Test
     * void point() {
     * 
     * // force table.countCards to return 3
     * when(table.countCards()).thenReturn(3); // this part is just for
     * demonstration
     * 
     * // calculate the expected score for later
     * int expectedScore = player.getScore() + 1;
     * 
     * // call the method we are testing
     * player.point();
     * 
     * // check that the score was increased correctly
     * assertEquals(expectedScore, player.getScore());
     * 
     * // check that ui.setScore was called with the player's id and the correct
     * score
     * verify(ui).setScore(eq(player.id), eq(expectedScore));
     * }
     */
}