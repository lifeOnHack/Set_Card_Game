package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableTest {

    Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    Config config;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Player player;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {

        /*
         * Properties properties = new Properties();
         * properties.put("Rows", "2");
         * properties.put("Columns", "2");
         * properties.put("FeatureSize", "3");
         * properties.put("FeatureCount", "4");
         * properties.put("TableDelaySeconds", "0");
         * properties.put("PlayerKeys1", "81,87,69,82");
         * properties.put("PlayerKeys2", "85,73,79,80");
         * MockLogger logger = new MockLogger();
         * Config config = new Config(logger, properties);
         * slotToCard = new Integer[config.tableSize];
         * cardToSlot = new Integer[config.deckSize];
         * 
         * Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
         * table = new Table(env, slotToCard, cardToSlot);
         */

        config = new Config(logger, "");
        Env env = new Env(logger, config, ui, util);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        table = new Table(env, slotToCard, cardToSlot);
    }

    void assertSetUp() {
        for (int i = 0; i < config.players; i++) {
            Integer[] playersTokens = table.getPlyrTok(i);
            for (int j = 0; j < playersTokens.length; j++) {
                assertEquals(-1, playersTokens[j]);
            }
        }
    }

    private int fillTwoSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    private int[] fillThreeSlots() {
        slotToCard[10] = 5;
        slotToCard[3] = 7;
        slotToCard[6] = 2;
        cardToSlot[5] = 10;
        cardToSlot[7] = 3;
        cardToSlot[2] = 6;

        int[] slotsFilled = { 10, 3, 6 };
        return slotsFilled;
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
            cardToSlot[i] = i;
        }
    }

    @Test
    void countCards_NoSlotsAreFilled() {

        assertEquals(0, table.countCards());
    }

    @Test
    void countCards_SomeSlotsAreFilled() {

        int slotsFilled = fillTwoSlots();
        assertEquals(slotsFilled, table.countCards());
    }

    @Test
    void countCards_AllSlotsAreFilled() {

        fillAllSlots();
        assertEquals(slotToCard.length, table.countCards());
    }

    @Test
    void placeToken_placeWhereTheresNoCard() {
        assertEquals(0, table.placeToken(0, 0));
    }

    @Test
    void placeToken_placeThreeTokens() {
        int[] slotsFilled = fillThreeSlots();
        for (int slot : slotsFilled) {
            assertEquals(1, table.placeToken(0, slot));
        }
    }

    @Test
    void placeToken_placeFourTokens() {
        fillAllSlots();
        for (int i = 0; i < 3; i++) {
            assertEquals(1, table.placeToken(0, i));
        }
        assertEquals(0, table.placeToken(0, 4));
    }

    /*
     * @Test
     * void placeCard_SomeSlotsAreFilled() {
     * 
     * fillSomeSlots();
     * placeSomeCardsAndAssert();
     * }
     * 
     * @Test
     * void placeCard_AllSlotsAreFilled() {
     * fillAllSlots();
     * placeSomeCardsAndAssert();
     * }
     */

    /*
     * static class MockUserInterface implements UserInterface {
     * 
     * @Override
     * public void placeCard(int card, int slot) {}
     * 
     * @Override
     * public void removeCard(int slot) {}
     * 
     * @Override
     * public void setCountdown(long millies, boolean warn) {}
     * 
     * @Override
     * public void setElapsed(long millies) {}
     * 
     * @Override
     * public void setScore(int player, int score) {}
     * 
     * @Override
     * public void setFreeze(int player, long millies) {}
     * 
     * @Override
     * public void placeToken(int player, int slot) {}
     * 
     * @Override
     * public void removeTokens() {}
     * 
     * @Override
     * public void removeTokens(int slot) {}
     * 
     * @Override
     * public void removeToken(int player, int slot) {}
     * 
     * @Override
     * public void announceWinner(int[] players) {}
     * };
     * 
     * static class MockUtil implements Util {
     * 
     * @Override
     * public int[] cardToFeatures(int card) {
     * return new int[0];
     * }
     * 
     * @Override
     * public int[][] cardsToFeatures(int[] cards) {
     * return new int[0][];
     * }
     * 
     * @Override
     * public boolean testSet(int[] cards) {
     * return false;
     * }
     * 
     * @Override
     * public List<int[]> findSets(List<Integer> deck, int count) {
     * return null;
     * }
     * }
     * 
     * static class MockLogger extends Logger {
     * protected MockLogger() {
     * super("", null);
     * }
     * }
     */
}
