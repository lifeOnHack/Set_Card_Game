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

    StateLock() {
        state = STATES.STOP;
    }

    public synchronized STATES getState() {
        return state;
    }

    public synchronized void setState(STATES newS) {
        state = newS;
    }

    public void makeAction(Player p) {
        switch (state) {
            case STOP:
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                break;
            case WAIT_FOR_RES:
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        if (state == STATES.DO_PENALTY) {
                            p.penalty();
                        } else if (state == STATES.DO_POINT) {
                            p.point();
                        }
                    }
                }
                break;
            case FREE_TO_GO:
                break;
            case DO_PENALTY:
                p.penalty();
                break;
            case DO_POINT:
                p.point();
                break;
        }

    }

    public synchronized void wakeup() {
        notifyAll();
    }
}
