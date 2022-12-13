package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.Util;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    private final LinkedList<Integer> plysCheckReq;
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    private final long MIN_IN_MS = 60000;
    private final long FIVE_SEC = 5000;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        plysCheckReq = new LinkedList<Integer>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        while (!shouldFinish()) {
            updateTimerDisplay(true);
            placeCardsOnTable();
            // add notify players
            timerLoop();
            updateTimerDisplay(false);
            // add wait
            removeAllCardsFromTable();
        }
        announceWinners();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable(false);// may not need
            placeCardsOnTable();// may not need
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        for (Player p : players) {
            p.terminate();
        }
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks if any cards should be removed from the table and returns them to the
     * deck.
     */
    private void removeCardsFromTable(Boolean all) {
        // TODO implement
        Integer[] cardArr = table.getSTC();
        for (int i = 0; i < cardArr.length; i++) {
            if (all || cardArr[i] == null) {
                env.ui.removeCard(i);
                cardArr[i] = null;
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        Integer[] cardArr = table.getSTC();
        for (int i = 0; i < cardArr.length; i++) {
            if (cardArr[i] == null) {
                int cc = deck.remove(0);
                table.placeCard(cc, i);
                // env.ui.placeCard(cc, i);
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(env.config.turnTimeoutMillis);
        } catch (InterruptedException e) {
            
        } finally {
            checkPlyrsSets();
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        long t = System.currentTimeMillis();
        if (reset) {
            reshuffleTime = t + MIN_IN_MS;
        }
        env.ui.setCountdown(reshuffleTime - t, reshuffleTime - t <= FIVE_SEC);
        // setElapsed
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        removeCardsFromTable(true);
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int bestScore = -1;
        int numOfWinners = 0;
        for (Player player : players) {
            if (player.getScore() > bestScore) {
                bestScore = player.getScore();
                numOfWinners = 1;
            } else if (player.getScore() == bestScore) {
                numOfWinners++;
            }
        }
        int[] winners = new int[numOfWinners];
        for (Player player : players) {
            if (player.getScore() == bestScore) {
                winners[--numOfWinners] = player.id;
            }
        }
        env.ui.announceWinner(winners);
    }

    public void addCheckReq(int p) {
        synchronized (plysCheckReq) {
            plysCheckReq.addLast(p);
            Thread.currentThread().interrupt();
        }
    }

    private void checkPlyrsSets() {
        boolean con = true;
        int curPly = -1;
        while (con) {
            synchronized (this.plysCheckReq) {
                if (!this.plysCheckReq.isEmpty()) {
                    curPly = plysCheckReq.removeFirst();
                } else {
                    con = false;
                }
                if (con) {
                    Integer[] set = table.getPlyrTok(curPly);
                    synchronized (set) {
                        int[] sset = new int[] { set[0], set[1], set[2] };
                        env.util.testSet(sset);
                    }
                }
            }

        }
    }
}
