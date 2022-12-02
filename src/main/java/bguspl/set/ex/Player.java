package bguspl.set.ex;

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
    /**
     * the places of the tockens on the board
     */
    private int[] tockenPlaces;

    private final int MAX_SLOTS = 11; // MN
    private final int NOT_PLACED = -1;// MN

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
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        usedTockens = 0;
        tockenPlaces = new int[] { NOT_PLACED, NOT_PLACED, NOT_PLACED };
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
        }
        if (!human)
            try {
                aiThread.join();
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
                this.keyPressed(rnd.nextInt(MAX_SLOTS));

                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.setDaemon(true);// will deteminate when the app close (potentialy SOL)
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
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        System.out.println("check val: " + slot);// probeb num from 0 to 11
        boolean res = setTockIfNeed(slot);
        if (res)
            usedTockens++;
        else
            usedTockens--;
        // add/remove tocken to card

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        usedTockens = 0; // reset
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        try {
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException ignr) {
        }
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        try {
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException ignr) {
        }
    }

    public int getScore() {
        return score;
    }

    boolean setTockIfNeed(int slot) {//redesign
        if(slot == tockenPlaces[0]){
            tockenPlaces[0] = NOT_PLACED;
            return false;
        } 
        if(slot == tockenPlaces[1]){
            tockenPlaces[1] = NOT_PLACED;
            return false;
        } 
        if(slot == tockenPlaces[2]){
            tockenPlaces[2] = NOT_PLACED;
            return false;
        } 
        if (usedTockens < 3) {
            
        }
        return true;
    }
}
