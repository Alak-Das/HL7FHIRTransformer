package com.al.hl7fhirtransformer.dto;

import java.util.List;
import java.util.Map;

public class TransactionSummaryResponse {
    private long totalCount;
    private int totalPages;
    private int currentPage;
    private Map<String, Long> statusCounts;
    private List<TransactionDTO> transactions;

    public TransactionSummaryResponse() {
    }

    public TransactionSummaryResponse(long totalCount, int totalPages, int currentPage, Map<String, Long> statusCounts,
            List<TransactionDTO> transactions) {
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.statusCounts = statusCounts;
        this.transactions = transactions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }

    public static class Builder {
        private long totalCount;
        private int totalPages;
        private int currentPage;
        private Map<String, Long> statusCounts;
        private List<TransactionDTO> transactions;

        public Builder totalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder currentPage(int currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public Builder statusCounts(Map<String, Long> statusCounts) {
            this.statusCounts = statusCounts;
            return this;
        }

        public Builder transactions(List<TransactionDTO> transactions) {
            this.transactions = transactions;
            return this;
        }

        public TransactionSummaryResponse build() {
            return new TransactionSummaryResponse(totalCount, totalPages, currentPage, statusCounts, transactions);
        }
    }
}
