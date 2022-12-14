package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Collections;
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
    private final Thread[] tPlayers;
    private final LinkedList<Integer> plysCheckReq;
    private int[] curset;
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
    private final long SLEEP_TIME = 150;

    Thread myThread;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        plysCheckReq = new LinkedList<Integer>();
        this.tPlayers = new Thread[players.length];
    }

    private void initPlyrsThread() {
        for (int i = 0; i < players.length; i++) {
            tPlayers[i] = new Thread(players[i]);
        }
    }

    private void startPT() {
        for (int i = 0; i < tPlayers.length; i++) {
            tPlayers[i].start();
        }
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        myThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        initPlyrsThread();
        startPT();
        while (!shouldFinish()) {
            shuffleNReset();
            updateTimerDisplay(true);
            placeCardsOnTable();
            wakeAll();// add notify players, by stats
            timerLoop();
            // updateTimerDisplay(false);
            // add wait to players, by stats
            stopAll();
            removeAllCardsFromTable();
        }
        terminate();
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
            removeCardsFromTable();// iff needed
            placeCardsOnTable();// iff needed
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
    private void removeCardsFromTable() {
        if (curset != null) {
            for (int i = 0; i < curset.length; i++) {
                table.removeByCard(curset[i]);
                // deck.add(curset[i]);
            }
        }
        curset = null;
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        Integer[] cardArr = table.getSTC();
        for (int i = 0; i < cardArr.length; i++) {
            if (cardArr[i] == null) {
                int cc = deck.remove(0);// remove top card
                table.placeCard(cc, i);
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            // System.out.println(env.config.players * SLEEP_TIME);
            Thread.sleep(env.config.players * SLEEP_TIME);
        } catch (InterruptedException e) {

        } finally {
            updateTimerDisplay(false);
            checkPlyrsSets();
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // System.out.println("update time, reset=" + reset);
        long t = System.currentTimeMillis();
        if (reset) {
            reshuffleTime = t + MIN_IN_MS;
        }
        env.ui.setCountdown(reshuffleTime - t, reshuffleTime - t <= env.config.turnTimeoutWarningMillis);
        // setElapsed
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        Integer[] cardArr = table.getSTC();
        for (int i = 0; i < cardArr.length; i++) {
            deck.add(cardArr[i]);
            table.removeCard(i);
        }
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
            myThread.interrupt();
        }
    }

    private void checkPlyrsSets() {
        boolean con = true;
        int curPly = -1;
        Integer[] stc = table.getSTC();
        while (con) {
            synchronized (this.plysCheckReq) {
                if (!this.plysCheckReq.isEmpty()) {
                    curPly = plysCheckReq.removeFirst();
                } else {
                    con = false;
                    // continue;
                }
            }
            if (con) {
                updateTimerDisplay(false);
                Integer[] set = table.getPlyrTok(curPly);
                synchronized (set) {
                    this.curset = new int[] { stc[set[0]], stc[set[1]], stc[set[2]] };
                    if (env.util.testSet(curset)) {
                        table.reset();
                        for (Player p : players) {
                            p.reset();
                        }
                        // players[curPly].point();// may be changed
                        interruptPlayer(curPly, STATES.DO_POINT);
                        synchronized (this.plysCheckReq) {
                            plysCheckReq.clear();
                            con = false;
                        }
                    } else {
                        // players[curPly].penalty(); // may be changed
                        interruptPlayer(curPly, STATES.DO_PENALTY);
                        this.curset = null;
                    }
                }
            }

        }
    }

    private void shuffleNReset() {
        Collections.shuffle(deck);
        for (Player p : players) {
            p.reset();
        }
        table.reset();
    }

    private void wakeAll() {
        for (Player player : players) {
            player.myState.wakeup();
        }
    }

    private void stopAll() {
        for (Player player : players) {
            player.myState.setState(STATES.STOP);
        }
    }

    private void interruptPlayer(int pId, STATES s) {
        players[pId].myState.setState(s);
        System.out.println("waking up" + " player" + pId);
        players[pId].myState.wakeup();
    }
}
