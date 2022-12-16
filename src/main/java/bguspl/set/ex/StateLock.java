package bguspl.set.ex;

enum STATES {
    STOP,
    FREE_TO_GO,
    WAIT_FOR_RES,
    DO_PENALTY,
    DO_POINT
}

public class StateLock {
    private STATES state;
    private Player p;
    private Runnable resFunc = null;
    private Object runLock = new Object();

    StateLock(Player player) {
        state = STATES.FREE_TO_GO; // STATES.STOP;
        p = player;
    }

    public synchronized STATES getState() {
        return state;
    }

    public synchronized void setState(STATES newS) {
        state = newS;
        notifyAll();
    }

    public synchronized void nextMove() throws InterruptedException {
        System.out.println(state);
        boolean runReady;
        synchronized (runLock) {
            runReady = resFunc != null;
        }
        if (!runReady)
            if (state == STATES.STOP | state == STATES.WAIT_FOR_RES) {
                wait();
            }
        makeAction();
    }

    public synchronized void makeAction() {

        synchronized (runLock) {
            if (resFunc != null) {
                resFunc.run();
                System.out.println("made act: player" + p.id);
                resFunc = null;
            }
        }
        /*
         * synchronized (this) {
         * state = STATES.FREE_TO_GO;
         * }
         */
    }

    public void assignPoint() {
        synchronized (runLock) {
            resFunc = new Runnable() {
                @Override
                public void run() {
                    p.point();
                }
            };
        }
    }

    public void assignPenalty() {
        synchronized (runLock) {
            resFunc = new Runnable() {
                @Override
                public void run() {
                    p.penalty();
                }
            };
        }
    }

    public void delRun() {
        synchronized (runLock) {
            resFunc = null;
        }
    }

    public synchronized void wakeup() {
        p.notifyInputQ();
        notifyAll();
    }
}
