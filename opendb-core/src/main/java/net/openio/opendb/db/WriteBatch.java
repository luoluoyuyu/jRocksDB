package net.openio.opendb.db;

import net.openio.opendb.log.Log;
import net.openio.opendb.log.WalLog;
import net.openio.opendb.model.SequenceNumber;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WriteBatch {


  private volatile long maxTime;

  private final AtomicBoolean storage = new AtomicBoolean();

  private final AtomicBoolean addState = new AtomicBoolean();

  private final Node head = new Node();

  private volatile Node tail = head;

  LinkedList<Log> logs = new LinkedList<>();

  volatile long writeSequenceNumber;

  public WriteBatch(long maxTime) {
    this.maxTime = maxTime;
  }

  public abstract List<String> getFile();

  public SequenceNumber getMaxSequenceNumber() {
    return new SequenceNumber(maxTime);
  }

  public abstract List<Log> getWal(SequenceNumber sequenceNumber);

  abstract boolean isSyn();


  public void addLog(WalLog walLog) {
    Node node = new Node();

    while (addState.compareAndSet(false, true)) {

    }
    SequenceNumber sequenceNumber = new SequenceNumber(++maxTime);
    walLog.getKey().setSequenceNumber(sequenceNumber);
    walLog.setSequenceNumber(sequenceNumber);
    tail.next = node;
    node.pre = tail;
    tail = node;
    node.sequenceNumber = sequenceNumber;
    synchronized (this) {
      logs.add(walLog);
    }
    addState.set(false);
    while (node.next == null) {
      if (!storage.compareAndSet(false, true)) {
        continue;
      }
      List<Log> logs = new LinkedList<>();
      if (isSyn()) {
        synchronized (this) {
          if (this.logs.size() < 1 << 19 || tail == node) {
            logs = this.logs;
            this.logs = new LinkedList<>();
          } else {
            storage.set(false);
            break;
          }
        }
      }
      step(logs);
      writeSequenceNumber = sequenceNumber.getTimes();
      node.pre.next = null;
      head.next = node.next;

      storage.set(false);
      return;
    }
    waitForNextNode(node);
  }

  abstract void step(List<Log> list);


  abstract void waitForNextNode(Node snapshot);

  static class Node {
    volatile Node next;

    volatile Node pre;

    SequenceNumber sequenceNumber;
  }

  abstract void close();
}
