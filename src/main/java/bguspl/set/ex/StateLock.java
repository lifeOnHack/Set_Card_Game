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
        System.out.println("new state: " + state);
    }

    public void makeAction(Player p) {
        Runnable resFunc = null;
        switch (state) {
            case STOP:
                synchronized (this) {
                    try {
                        System.out.println("player" + p.id + " wait on stop");
                        wait();
                    } catch (InterruptedException e) {
                    }
                    setState(STATES.FREE_TO_GO);
                }
                break;
            case WAIT_FOR_RES:
                synchronized (this) {
                    try {
                        System.out.println("player" + p.id + " wait on results");
                        wait();
                    } catch (InterruptedException e) {

                    }
                    if (state == STATES.DO_PENALTY) {
                        // p.penalty();
                        resFunc = new Runnable() {
                            @Override
                            public void run() {
                                p.penalty();
                            }
                        };
                        System.out.println("player" + p.id + " get penalty");
                    } else if (state == STATES.DO_POINT) {
                        // p.point();
                        resFunc = new Runnable() {
                            @Override
                            public void run() {
                                p.point();
                            }
                        };
                        System.out.println("player" + p.id + " get point");
                    }
                    setState(STATES.FREE_TO_GO);
                }
                break;
            case FREE_TO_GO:
                break;
            /*
             * case DO_PENALTY:
             * p.penalty();
             * break;
             * case DO_POINT:
             * p.point();
             * break;
             */
            default:
                break;
        }
        if (resFunc != null) {
            resFunc.run();
        }
    }

    public synchronized void wakeup(Player p) {
        p.notifyInputQ();
        notifyAll();
    }
}
