package business.service;

import java.lang.FunctionalInterface;

@FunctionalInterface
public interface ITransactionValidator {
    boolean isValid(double balance, double amount);
}
