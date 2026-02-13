package com.al.hl7fhirtransformer.dto;

public class StatusCount {
    private String _id; // Corresponds to 'status' in group by
    private long count;

    public StatusCount() {
    }

    public StatusCount(String _id, long count) {
        this._id = _id;
        this.count = count;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
