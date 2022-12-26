package bguspl.set.ex;

enum STATES {
    STOP,
    FREE_TO_GO,
    WAIT_FOR_RES,
    DO_PENALTY,
    DO_POINT,
    END
}

public class StateLock {
    private STATES state;
    private Player p;
    private Runnable resFunc = null;
    private Object runLock = new Object();

    StateLock(Player player) {
        state = STATES.FREE_TO_GO;
        p = player;
    }

    public synchronized STATES getState() {
        return state;
    }

    public synchronized void setState(STATES newS) {
        state = newS;
        notifyAll();
    }

    public void nextMove() throws InterruptedException {
        boolean runReady;
        synchronized (runLock) {
            runReady = resFunc != null;
        }
        if (!runReady)
            synchronized (this) {
                if (state == STATES.STOP | state == STATES.WAIT_FOR_RES) {
                    wait();
                }
            }
        makeAction();
    }

    public void makeAction() {

        synchronized (runLock) {
            if (resFunc != null) {// cuz of the stync dealer need to wait for ending of penallty
                resFunc.run();
                resFunc = null;
            }
        }
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
