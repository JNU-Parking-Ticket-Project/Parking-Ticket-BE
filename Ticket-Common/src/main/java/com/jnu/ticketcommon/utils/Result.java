package com.jnu.ticketcommon.utils;


import com.jnu.ticketcommon.exception.InternalSeverErrorException;
import com.jnu.ticketcommon.exception.OtherServerInternalSeverErrorException;
import com.jnu.ticketcommon.exception.TicketCodeException;

import java.util.Objects;
import java.util.function.Function;

public abstract class Result<T, E> {

    public static <T, E> Result<T, E> success(T value) {
        Objects.requireNonNull(value, "Value cannot be null");
        return new Success<>(value);
    }

    public static <T, E> Result<T, E> failure(E error) {
        Objects.requireNonNull(error, "Error cannot be null");
        return new Failure<>(error);
    }

    public abstract <U> Result<U, E> map(Function<T, U> mapper);

    public abstract <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);

    public abstract T getOrElse(T defaultValue);

    public abstract T getOrThrow();

    public abstract boolean isSuccess();

    public abstract <R> R fold(Function<T, R> onSuccess, Function<E, R> onFailure);

    private static class Success<T, E> extends Result<T, E> {
        private final T value;

        private Success(T value) {
            this.value = value;
        }

        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.success(mapper.apply(value));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<E, R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    private static class Failure<T,E> extends Result<T, E> {
        private final E error;

        private Failure(E error) {
            this.error = error;
        }

        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.failure(error);
        }

        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return Result.failure(error);
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T getOrThrow() {
            throw error instanceof RuntimeException ? (RuntimeException)error : new InternalSeverErrorException();
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<E, R> onFailure) {
            return onFailure.apply(error);
        }
    }
}
