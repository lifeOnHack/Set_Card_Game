package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    /*
     * Dealer of the game
     */
    private final Dealer dlr;
    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * the number of tockens the player put
     * Max 3
     */
    private int usedTockens;
    /*
     * input queue of keys
     * max size 3
     */
    Queue<Integer> inputQ;

    private final int MAX_SLOTS = 11; // MN
    private final int Q_MAX_INP = 3;// MN
    private final int SEC = 1000;// MN

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.dlr = dealer;
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        usedTockens = 0;
        inputQ = new LinkedList<>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human)
            createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            synchronized (inputQ) {
                while (inputQ.size() == 0 /* && myState.getState() == STATES.FREE_TO_GO */) {
                    try {
                        inputQ.wait();
                    } catch (InterruptedException ignored) {
                        terminate();
                    }
                }
                while (inputQ.size() > 0) {
                    // add/remove tocken from card
                    usedTockens += table.setTokIfNeed(this.id, inputQ.remove());
                    inputQ.notifyAll();
                    if (usedTockens == Q_MAX_INP) {
                        dlr.addCheckReq(id);
                    }
                }
            }
        }
        if (!human)
            try {
                aiThread.join();
                aiThread.interrupt();
            } catch (InterruptedException ignored) {
            }
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            Random rnd = new Random();
            while (!terminate) {
                synchronized (inputQ) {
                    System.out.println("plater" + id + " " /* + myState.getState() */ + " -- " + inputQ.size());
                    if (inputQ.size() == MAX_SLOTS /* && myState.getState() == STATES.FREE_TO_GO */)
                        try {
                            inputQ.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                }
                this.keyPressed(rnd.nextInt(MAX_SLOTS + 1));
                try {
                    Thread.sleep(SEC / 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        // aiThread.setDaemon(true);
        // will deteminate when the app close (potentialySOL)
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        if (!human) {
            aiThread.interrupt();
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        synchronized (inputQ) {
            if (inputQ.size() < Q_MAX_INP) {
                inputQ.add(slot);
                inputQ.notifyAll();
            }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        env.ui.setScore(id, ++score);
        System.out.println("fuunnn player " + id + " got point");
        try {
            // Thread.sleep(env.config.pointFreezeMillis);

            Long endFrz = System.currentTimeMillis() + env.config.pointFreezeMillis;
            while (System.currentTimeMillis() < endFrz) {
                env.ui.setFreeze(id, endFrz - System.currentTimeMillis());
                playerThread.sleep(SEC / 2);
            }
        } catch (InterruptedException ignr) {
        }
        reset(); // reset
        env.ui.setFreeze(id, 0);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        System.out.println("damn player " + id + " penalised");
        try {
            // Thread.sleep(env.config.penaltyFreezeMillis);
            Long endFrz = System.currentTimeMillis() + env.config.penaltyFreezeMillis;
            while (System.currentTimeMillis() < endFrz) {
                env.ui.setFreeze(id, endFrz - System.currentTimeMillis());
                playerThread.sleep(SEC / 2);
            }
        } catch (InterruptedException ignr) {
        }
        if (human) {
            synchronized (inputQ) {
                this.inputQ.clear();
                inputQ.notifyAll();
            }
        } else
            reset();
        env.ui.setFreeze(id, 0);
    }

    public int getScore() {
        return score;
    }

    public void reset() {
        synchronized (inputQ) {
            this.inputQ.clear();
            inputQ.notifyAll();
        }
        usedTockens = 0;
        table.resetPlayer(id);
    }

    public void notifyInputQ() {
        synchronized (inputQ) {
            inputQ.notifyAll();
        }
    }

    public void tokenGotRemoved() {
        usedTockens--;
    }
}
