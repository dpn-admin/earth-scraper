package org.chronopolis.earth.models;

import java.util.List;

/**
 * Created by shake on 3/2/15.
 */
public class Response<E> {
    int count;
    String next;
    String previous;
    List<E> results;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public String getNext() {
        return next;
    }

    public void setNext(final String next) {
        this.next = next;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(final String previous) {
        this.previous = previous;
    }

    public List<E> getResults() {
        return results;
    }

    public void setResults(final List<E> results) {
        this.results = results;
    }
}
