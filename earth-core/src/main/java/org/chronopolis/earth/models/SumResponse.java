package org.chronopolis.earth.models;

/**
 * A response which also includes a total_size field
 *
 * Created by shake on 8/4/16.
 */
public class SumResponse<E> extends Response<E> {

    private Long totalSize;

    public Long getTotalSize() {
        return totalSize;
    }

    public SumResponse setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
        return this;
    }
}
