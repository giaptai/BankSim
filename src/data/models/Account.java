package data.models;

import java.text.DecimalFormat;

import resources.annotations.Builder;

@Builder
public class Account {
    private int accountId;
    private String ownerName;
    private double balance;

    public Account() {
    }

    private Account(Builder builder){
        this.accountId = builder.accountId;
        this.ownerName = builder.ownerName;
        this.balance = builder.balance;
    }

    public Account(int accountId, String ownerName, double balance) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public Account(String ownerName, double balance) {
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "Account [" + "Account Id = " + accountId + ", Owner Name = " + ownerName + ", balance = "
                + df.format(balance)
                + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int accountId;
        private String ownerName;
        private double balance;

        public Builder() {
        }

        public Builder accountId(int accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder balance(double balance) {
            this.balance = balance;
            return this;
        }

        public Account build(){
            return new Account(this);
        }
    }
}
