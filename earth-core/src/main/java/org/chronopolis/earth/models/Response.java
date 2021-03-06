package org.chronopolis.earth.models;

import java.util.List;

/**
 * A response object from the DPN REST API
 *
 * Created by shake on 3/2/15.
 */
public class Response<E> {
    private int count;
    private String next;
    private String previous;
    private List<E> results;

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
