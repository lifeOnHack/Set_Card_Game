package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import bguspl.set.Env;

//import java.lang.Math;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 * @inv usedTocken <= 3
 * @inv usedTocken >= 0
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
    public StateLock myState;
    private final int MAX_SLOTS = 11; // MN
    private final int Q_MAX_INP = 3;// MN

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
        myState = new StateLock();
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
            myState.makeAction(this);
            // while (myState.getState() == STATES.FREE_TO_GO)
            synchronized (inputQ) {
                while (inputQ.size() == 0 && myState.getState() == STATES.FREE_TO_GO) {
                    try {
                        inputQ.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                while (inputQ.size() > 0) {
                    // add/remove tocken from card
                    usedTockens += table.setTokIfNeed(this.id, inputQ.remove());
                    inputQ.notifyAll();
                    if (usedTockens == Q_MAX_INP) {
                        dlr.addCheckReq(id);
                        myState.setState(STATES.WAIT_FOR_RES);
                        // pause the player to wait for results
                    }
                }
            }
            // myState.makeAction(this);
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
                // TODO implement player key press simulator
                // pause by state
                myState.makeAction(this);
                synchronized (inputQ) {
                    if (inputQ.size() == MAX_SLOTS && myState.getState() == STATES.FREE_TO_GO)
                        try {
                            inputQ.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                }
                this.keyPressed(rnd.nextInt(MAX_SLOTS + 1));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                /*
                 * try {
                 * synchronized (this) {
                 * wait();
                 * }
                 * } catch (InterruptedException ignored) {
                 * }
                 */
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.setDaemon(true);// will determinate when the app close (potentialy SOL)
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
        // check state
        if (myState.getState() != STATES.FREE_TO_GO) {
            // System.out.println("cant perform press");
            return;
        }
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
        // myState.setState(STATES.FREE_TO_GO);// change state
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        // synchronized(score)
        env.ui.setScore(id, ++score);
        try {
            // Thread.sleep(env.config.pointFreezeMillis);
            System.out.println("funn player " + id + " got point");
            playerThread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException ignr) {
        }
        reset(); // reset
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        // myState.setState(STATES.FREE_TO_GO);// change state
        try {
            // Thread.sleep(env.config.penaltyFreezeMillis);
            System.out.println("damn player " + id + " penalised");
            playerThread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException ignr) {
        }
        synchronized (inputQ) {
            this.inputQ.clear();
            inputQ.notifyAll();
        }

    }

    public int getScore() {
        // synchronized(score)
        return score;
    }

    /*
     * start from zero object values
     * clear Q
     */
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
}