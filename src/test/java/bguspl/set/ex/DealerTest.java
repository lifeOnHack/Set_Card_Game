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

import java.util.LinkedList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {
    
    private Dealer dealer;
    private Env env;
    private Config config;
    @Mock
    private Player player0;
    @Mock
    private Player player1;
    @Mock
    private Player player2;
    @Mock
    private Table table;
    @Mock
    private Logger logger;
    @Mock
    private UserInterface ui;
    @Mock
    private Util util;
    

    @BeforeEach
    void setUp(){
        config = new Config(logger, "");
        env = new Env(logger, config, ui, util);
        Player[] players = {player0, player1, player2};
        dealer = new Dealer(env, table, players);
    }

    @Test
    void announceWinners_oneWinner(){
        when(player0.getScore()).thenReturn(3);
        when(player1.getScore()).thenReturn(5);
        when(player2.getScore()).thenReturn(6);
        
        int[] winners = {2};
        dealer.announceWinners();
        verify(env.ui).announceWinner(winners);
    }

    @Test
    void announceWinners_threeWinners(){
        when(player0.getScore()).thenReturn(6);
        when(player1.getScore()).thenReturn(6);
        when(player2.getScore()).thenReturn(6);
        
        int[] winners = {2, 1, 0};
        dealer.announceWinners();
        verify(env.ui).announceWinner(winners);
    }

    @Test
    void deckToCheck_emptyTable(){
        Integer[] emptyArray = new Integer[12];
        when(table.getSTC()).thenReturn(emptyArray);
        LinkedList<Integer> res = dealer.deckToCheck();
        assertTrue(res.isEmpty());
    }

    @Test
    void deckToCheck_twoCardsOnTable(){
        Integer[] tableCards = new Integer[12];
        tableCards[2] = 5;
        tableCards[7] = 9;
        when(table.getSTC()).thenReturn(tableCards);
        LinkedList<Integer> res = dealer.deckToCheck();
        assertFalse(res.isEmpty());
        assertTrue(res.contains(tableCards[2]));
        assertTrue(res.contains(tableCards[7]));
    }

    @Test
    void deckToCheck_fullTable(){
        Integer[] tableCards = new Integer[12];
        for (int i = 0; i<tableCards.length; i++) {
            tableCards[i] = i;
        }
        when(table.getSTC()).thenReturn(tableCards);
        LinkedList<Integer> res = dealer.deckToCheck();
        assertFalse(res.isEmpty());
        for (int i = 0; i<tableCards.length; i++) {
            assertTrue(res.contains(tableCards[i]));
        }
    }
}
