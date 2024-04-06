import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class SkipListNode {
    int tag;
    SkipListNode[] next;

    public SkipListNode(int tag, int level) {
        this.tag = tag;
        this.next = new SkipListNode[level + 1];
    }
}

class ConcurrentSkipList {
    private static final int MAX_LEVEL = 32; // Maximum level of the skip list
    private SkipListNode head;
    private AtomicInteger level;
    private AtomicBoolean shouldRestart;

    public ConcurrentSkipList() {
        this.head = new SkipListNode(Integer.MIN_VALUE, MAX_LEVEL);
        this.level = new AtomicInteger(0);
        this.shouldRestart = new AtomicBoolean(false);
    }

    public boolean addPresent(int tag) {
        int topLevel = randomLevel();
        SkipListNode[] preds = new SkipListNode[MAX_LEVEL + 1];
        SkipListNode[] succs = new SkipListNode[MAX_LEVEL + 1];

        while (true) {
            boolean found = find(tag, preds, succs);
            if (found) {
                return false; // Present with the same tag already exists
            } else {
                int highestLocked = -1;
                try {
                    for (int i = 0; i <= topLevel; i++) {
                        preds[i].next[i] = new SkipListNode(tag, i);
                    }
                    return true; // Present successfully added
                } finally {
                    for (int i = 0; i <= highestLocked; i++) {
                        preds[i] = null;
                    }
                }
            }
        }
    }

    public boolean removePresent(int tag) {
        SkipListNode[] preds = new SkipListNode[MAX_LEVEL + 1];
        SkipListNode[] succs = new SkipListNode[MAX_LEVEL + 1];

        while (true) {
            boolean found = find(tag, preds, succs);
            if (!found) {
                return false; // Present not found
            } else {
                int highestLocked = -1;
                try {
                    for (int level = 0; level < succs.length; level++) {
                        SkipListNode succ = succs[level];
                        if (succ != null && succ.tag == tag) {
                            highestLocked = level;
                            preds[level].next[level] = succ.next[level];
                        }
                    }
                    return true; // Present successfully removed
                } finally {
                    for (int i = 0; i <= highestLocked; i++) {
                        preds[i] = null;
                    }
                }
            }
        }
    }

    public boolean isPresent(int tag) {
        SkipListNode[] preds = new SkipListNode[MAX_LEVEL + 1];
        SkipListNode[] succs = new SkipListNode[MAX_LEVEL + 1];
        return find(tag, preds, succs);
    }

    private boolean find(int tag, SkipListNode[] preds, SkipListNode[] succs) {
        while (true) {
            boolean found = search(tag, preds, succs);
            if (found) {
                return true;
            } else {
                if (shouldRestart.get()) {
                    shouldRestart.set(false);
                    continue;
                }
                return false;
            }
        }
    }

    private boolean search(int tag, SkipListNode[] preds, SkipListNode[] succs) {
        int highestLocked = -1;
        SkipListNode pred = head;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            SkipListNode curr = pred.next[level];
            while (curr != null && curr.tag < tag) {
                pred = curr;
                curr = pred.next[level];
            }
            preds[level] = pred;
            succs[level] = curr;
        }
        return succs[0] != null && succs[0].tag == tag;
    }

    private int randomLevel() {
        int level = 0;
        while (ThreadLocalRandom.current().nextDouble() < 0.5 && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }
}

class Servant implements Runnable {
    private ConcurrentSkipList list;
    private AtomicInteger presentsCounter;
    private AtomicInteger cardsCounter;

    public Servant(ConcurrentSkipList list, AtomicInteger presentsCounter, AtomicInteger cardsCounter) {
        this.list = list;
        this.presentsCounter = presentsCounter;
        this.cardsCounter = cardsCounter;
    }

    @Override
    public void run() {
        int numPresents = 500000;
        for (int i = 0; i < numPresents; i++) {
            int action = ThreadLocalRandom.current().nextInt(3);
            int tag = ThreadLocalRandom.current().nextInt(numPresents); // Assuming tags are unique and within the range of numPresents
            switch (action) {
                case 0:
                    if (presentsCounter.getAndIncrement() < numPresents) {
                        list.addPresent(tag);
                    }
                    break;
                case 1:
                    if (cardsCounter.getAndIncrement() < numPresents) {
                        list.removePresent(tag);
                    }
                    break;
                case 2:
                    list.isPresent(tag);
                    break;
            }
        }
    }
}

public class Assignment3 {
    public static void main(String[] args) throws InterruptedException {
        ConcurrentSkipList list = new ConcurrentSkipList();
        AtomicInteger presentsCounter = new AtomicInteger(0);
        AtomicInteger cardsCounter = new AtomicInteger(0);
        int numServants = 4;
        Thread[] servantThreads = new Thread[numServants];
        for (int i = 0; i < numServants; i++) {
            servantThreads[i] = new Thread(new Servant(list, presentsCounter, cardsCounter));
            servantThreads[i].start();
        }
        for (int i = 0; i < numServants; i++) {
            servantThreads[i].join();
        }
        System.out.println("All presents processed.");
    }
}
