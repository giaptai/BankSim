package business.service.transaction.observer;

import java.time.LocalDateTime;

import resources.TransactionStatus;
import resources.annotations.Builder;

@Builder
public class TransactionEvent {

    private String currThreadName;
    private String type;
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private double predictedBalance;
    private double actualBalance;
    private LocalDateTime startTime;
    private TransactionStatus status;
    private String message;

    public String getCurrThreadName() {
        return currThreadName;
    }

    public String getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public double getPredictedBalance() {
        return predictedBalance;
    }

    public double getActualBalance() {
        return actualBalance;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    private TransactionEvent(TransactionEventBuilder builder) {
        this.currThreadName = builder.currThreadName;
        this.type = builder.type;
        this.fromAccountId = builder.fromAccountId;
        this.toAccountId = builder.toAccountId;
        this.amount = builder.amount;
        this.predictedBalance = builder.predictedBalance;
        this.actualBalance = builder.actualBalance;
        this.startTime = builder.startTime;
        this.status = builder.status;
        this.message = builder.message;
    }

    public static TransactionEventBuilder builder() {
        return new TransactionEventBuilder();
    }

    public static class TransactionEventBuilder {
        private String currThreadName;
        private String type;
        private String fromAccountId;
        private String toAccountId;
        private double amount;
        private double predictedBalance;
        private double actualBalance;
        private LocalDateTime startTime;
        private TransactionStatus status;
        private String message;

        public TransactionEventBuilder currThreadName(String currThreadName) {
            this.currThreadName = currThreadName;
            return this;
        }

        public TransactionEventBuilder type(String type) {
            this.type = type;
            return this;
        }

        public TransactionEventBuilder fromAccountId(String fromAccountId) {
            this.fromAccountId = fromAccountId;
            return this;
        }

        public TransactionEventBuilder toAccountId(String toAccountId) {
            this.toAccountId = toAccountId;
            return this;
        }

        public TransactionEventBuilder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public TransactionEventBuilder predictedBalance(double predictedBalance) {
            this.predictedBalance = predictedBalance;
            return this;
        }

        public TransactionEventBuilder actualBalance(double actualBalance) {
            this.actualBalance = actualBalance;
            return this;
        }

        public TransactionEventBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public TransactionEventBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public TransactionEventBuilder message(String message) {
            this.message = message;
            return this;
        }

        public TransactionEvent build() {
            return new TransactionEvent(this);
        }
    }
}
