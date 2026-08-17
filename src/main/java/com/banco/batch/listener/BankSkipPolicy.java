package com.banco.batch.listener;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;

public class BankSkipPolicy implements SkipPolicy {

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) throws SkipLimitExceededException {
        if (skipCount >= 100_000) {
            throw new SkipLimitExceededException(100_000, throwable);
        }
        return throwable instanceof FlatFileParseException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof NumberFormatException
                || throwable instanceof NullPointerException;
    }
}
