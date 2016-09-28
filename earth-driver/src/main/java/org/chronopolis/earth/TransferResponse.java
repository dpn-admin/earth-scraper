package org.chronopolis.earth;

import java.util.List;

/**
 * @deprecated old. no longer used. slated for removal in 2.0.0-RELEASE
 *
 * Created by shake on 11/13/14.
 */
@Deprecated
public class TransferResponse {
    int count;
    String next;
    String previous;
    List<Transfer> results;

    public long getCount() {
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

    public List<Transfer> getResults() {
        return results;
    }

    public void setResults(final List<Transfer> results) {
        this.results = results;
    }
}
