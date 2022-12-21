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
    private int[] curset;// save current set that need to remove by the cards number

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
        boolean s = true;

        while (!shouldFinish()) {
            shuffleNReset();
            placeCardsOnTable();
            updateTimerDisplay(true);
            if (s) {
                startPT();
                s = false;
            }
            table.hints();
            timerLoop();
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
        while (!terminate && (System.currentTimeMillis() < reshuffleTime || env.config.turnTimeoutMillis <= 0)) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();// iff needed
            placeCardsOnTable();// iff needed
        }
        for (Player p : players) {
            p.myState.setState(STATES.STOP);
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        for (Player p : players) {
            p.terminate();
            tPlayers[p.id].interrupt();
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
            table.removeAtPoint(curset[0], curset[1], curset[2], players, plysCheckReq);
            for (int i = 0; i < curset.length; i++) {
                env.ui.removeTokens(table.getCTS()[curset[i]]);
                table.removeByCard(curset[i]);
            }
            if (env.config.turnTimeoutMillis >= 0) {
                updateTimerDisplay(true);// means someone make point
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
            synchronized (cardArr) {
                if (cardArr[i] == null & !deck.isEmpty()) {
                    int cc = deck.remove(0);// remove top card
                    table.placeCard(cc, i);
                }
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
            if (reshuffleTime - System.currentTimeMillis() > env.config.turnTimeoutWarningMillis)
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
        long t = System.currentTimeMillis();
        if (env.config.turnTimeoutMillis > 0) {// countdown for env.config.turnTimeoutMillis >0
            if (reset) {
                reshuffleTime = t + env.config.turnTimeoutMillis;
            }
            long tLeft = reshuffleTime - t;
            env.ui.setCountdown(tLeft > 0 ? tLeft : 0, reshuffleTime - t <= env.config.turnTimeoutWarningMillis);
            // setElapsed
        } else if (env.config.turnTimeoutMillis == 0) {// start from zero up =0
            if (env.util.findSets(deckToCheck(), 1).size() == 0) {
                for (Player p : players) {
                    p.myState.setState(STATES.STOP);
                }
                removeAllCardsFromTable();
                if (shouldFinish()) {
                    terminate = true;
                    return;
                }
                shuffleNReset(); // wakeAll();
                placeCardsOnTable();
                reset = true;
            }
            if (reset) {
                reshuffleTime = System.currentTimeMillis();
            }
            env.ui.setCountdown(t - reshuffleTime, false);
        } else {// <0
            if (env.util.findSets(deckToCheck(), 1).size() == 0) {
                reshuffleTime = System.currentTimeMillis() - MIN_IN_MS;
                System.out.println("there is no set");
            } else {
                reshuffleTime = Long.MAX_VALUE;
            }
        }
    }

    public LinkedList<Integer> deckToCheck() {
        LinkedList<Integer> res = new LinkedList<Integer>();
        for (Integer intg : table.getSTC()) {
            if (intg != null) {
                res.add(intg);
            }
        }
        return res;
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        Integer[] cardArr = table.getSTC();
        env.ui.removeTokens();
        for (int i = 0; i < cardArr.length; i++) {
            if (cardArr[i] != null) {
                deck.add(cardArr[i]);
                table.removeCard(i);
            }
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    public void announceWinners() {
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
        for(int i = 0; i < players.length; i++){
            if (players[i].getScore() == bestScore) {
                winners[--numOfWinners] = i;
            }
        }
        env.ui.announceWinner(winners);
    }


    public void addCheckReq(int p) {
        synchronized (plysCheckReq) {
            plysCheckReq.addLast(p);
            myThread.interrupt();
        }
        players[p].myState.setState(STATES.WAIT_FOR_RES);// freez player till results
        // System.out.println("player" + p + " request check");
    }

    public LinkedList<Integer> getPlysCheckReq(){
        return plysCheckReq;
    }

    private void checkPlyrsSets() {
        int curPly = -1;
        synchronized (this.plysCheckReq) {
            if (!this.plysCheckReq.isEmpty()) {
                curPly = plysCheckReq.removeFirst();
            }
        }
        updateTimerDisplay(false);
        if (curPly != -1) {
            // Integer[] set = table.getPlyrTok(curPly);
            // synchronized (set) {
            try {
                // Integer[] stc = table.getSTC();
                // synchronized (stc) {
                // this.curset = new int[] { stc[set[0]], stc[set[1]], stc[set[2]] };
                // }

                if (null == (this.curset = table.getPSet(players[curPly])))
                    return;

            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                players[curPly].reset();
                System.out.println(e);
                return;
            }
            // }
            updateTimerDisplay(false);
            if (env.util.testSet(curset)) {
                synchronized (players[curPly].myState) {
                    players[curPly].myState.assignPoint();
                    players[curPly].myState.wakeup();
                }
            } else {
                synchronized (players[curPly].myState) {
                    players[curPly].myState.assignPenalty();
                    players[curPly].myState.wakeup();
                }
                this.curset = null;
            }
            updateTimerDisplay(false);
        }
    }

    private void shuffleNReset() {
        Collections.shuffle(deck);
        for (Player p : players) {
            p.reset();
        } // dealer freez here untill everybody finish penalty\score
        table.reset();
        synchronized (plysCheckReq) {
            plysCheckReq.clear();
        }
    }
}
