/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.fuseable.ScalarSupplier;
import io.reactivex.rxjava3.internal.jdk8.*;
import io.reactivex.rxjava3.internal.observers.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.operators.mixed.*;
import io.reactivex.rxjava3.internal.operators.observable.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.observables.*;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;

/**
 * The Observable class is the non-backpressured, optionally multi-valued base reactive class that
 * offers factory methods, intermediate operators and the ability to consume synchronous
 * and/or asynchronous reactive dataflows.
 * <p>
 * Many operators in the class accept {@code ObservableSource}(s), the base reactive interface
 * for such non-backpressured flows, which {@code Observable} itself implements as well.
 * <p>
 * The Observable's operators, by default, run with a buffer size of 128 elements (see {@link Flowable#bufferSize()}),
 * that can be overridden globally via the system parameter {@code rx3.buffer-size}. Most operators, however, have
 * overloads that allow setting their internal buffer size explicitly.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" height="317" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/legend.png" alt="">
 * <p>
 * The design of this class was derived from the
 * <a href="https://github.com/reactive-streams/reactive-streams-jvm">Reactive-Streams design and specification</a>
 * by removing any backpressure-related infrastructure and implementation detail, replacing the
 * {@code org.reactivestreams.Subscription} with {@link Disposable} as the primary means to dispose of
 * a flow.
 * <p>
 * The {@code Observable} follows the protocol
 * <pre><code>
 *      onSubscribe onNext* (onError | onComplete)?
 * </code></pre>
 * where
 * the stream can be disposed through the {@code Disposable} instance provided to consumers through
 * {@code Observer.onSubscribe}.
 * <p>
 * Unlike the {@code Observable} of version 1.x, {@link #subscribe(Observer)} does not allow external disposal
 * of a subscription and the {@code Observer} instance is expected to expose such capability.
 * <p>Example:
 * <pre><code>
 * Disposable d = Observable.just("Hello world!")
 *     .delay(1, TimeUnit.SECONDS)
 *     .subscribeWith(new DisposableObserver&lt;String&gt;() {
 *         &#64;Override public void onStart() {
 *             System.out.println("Start!");
 *         }
 *         &#64;Override public void onNext(String t) {
 *             System.out.println(t);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *         }
 *     });
 *
 * Thread.sleep(500);
 * // the sequence can now be disposed via dispose()
 * d.dispose();
 * </code></pre>
 *
 * @param <T>
 *            the type of the items emitted by the Observable
 * @see Flowable
 * @see io.reactivex.rxjava3.observers.DisposableObserver
 */
public abstract class Observable<T> implements ObservableSource<T> {

    /**
     * Mirrors the one ObservableSource in an Iterable of several ObservableSources that first either emits an item or sends
     * a termination notification.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/amb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code amb} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param sources
     *            an Iterable of ObservableSource sources competing to react first. A subscription to each source will
     *            occur in the same order as in the Iterable.
     * @return an Observable that emits the same sequence as whichever of the source ObservableSources first
     *         emitted an item or sent a termination notification
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> amb(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableAmb<>(null, sources));
    }

    /**
     * Mirrors the one ObservableSource in an array of several ObservableSources that first either emits an item or sends
     * a termination notification.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/amb.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param sources
     *            an array of ObservableSource sources competing to react first. A subscription to each source will
     *            occur in the same order as in the array.
     * @return an Observable that emits the same sequence as whichever of the source ObservableSources first
     *         emitted an item or sent a termination notification
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    public static <T> Observable<T> ambArray(@NonNull ObservableSource<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        int len = sources.length;
        if (len == 0) {
            return empty();
        }
        if (len == 1) {
            return (Observable<T>)wrap(sources[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableAmb<>(sources, null));
    }

    /**
     * Returns the default 'island' size or capacity-increment hint for unbounded buffers.
     * <p>Delegates to {@link Flowable#bufferSize} but is public for convenience.
     * <p>The value can be overridden via system parameter {@code rx3.buffer-size}
     * <em>before</em> the {@link Flowable} class is loaded.
     * @return the default 'island' size or capacity-increment hint
     */
    @CheckReturnValue
    public static int bufferSize() {
        return Flowable.bufferSize();
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> combineLatest(
            @NonNull Iterable<? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatest(sources, combiner, bufferSize());
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @param bufferSize
     *            the internal buffer size and prefetch amount applied to every source Observable
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Observable<R> combineLatest(
            @NonNull Iterable<? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(null, sources, combiner, s, false));
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> combineLatestArray(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestArray(sources, combiner, bufferSize());
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @param bufferSize
     *            the internal buffer size and prefetch amount applied to every source Observable
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Observable<R> combineLatestArray(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(sources, null, combiner, s, false));
    }

    /**
     * Combines two source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from either of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines three source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines four source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines five source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param source5
     *            the fifth source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines six source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param source5
     *            the fifth source ObservableSource
     * @param source6
     *            the sixth source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines seven source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param source5
     *            the fifth source ObservableSource
     * @param source6
     *            the sixth source ObservableSource
     * @param source7
     *            the seventh source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines eight source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <T8> the element type of the eighth source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param source5
     *            the fifth source ObservableSource
     * @param source6
     *            the sixth source ObservableSource
     * @param source7
     *            the seventh source ObservableSource
     * @param source8
     *            the eighth source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
            @NonNull Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7, source8 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines nine source ObservableSources by emitting an item that aggregates the latest values of each of the
     * source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatest.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the element type of the first source
     * @param <T2> the element type of the second source
     * @param <T3> the element type of the third source
     * @param <T4> the element type of the fourth source
     * @param <T5> the element type of the fifth source
     * @param <T6> the element type of the sixth source
     * @param <T7> the element type of the seventh source
     * @param <T8> the element type of the eighth source
     * @param <T9> the element type of the ninth source
     * @param <R> the combined output type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            the second source ObservableSource
     * @param source3
     *            the third source ObservableSource
     * @param source4
     *            the fourth source ObservableSource
     * @param source5
     *            the fifth source ObservableSource
     * @param source6
     *            the sixth source ObservableSource
     * @param source7
     *            the seventh source ObservableSource
     * @param source8
     *            the eighth source ObservableSource
     * @param source9
     *            the ninth source ObservableSource
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Observable<R> combineLatest(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
            @NonNull ObservableSource<? extends T9> source9,
            @NonNull Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(source9, "source9 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return combineLatestArray(new ObservableSource[] { source1, source2, source3, source4, source5, source6, source7, source8, source9 }, Functions.toFunction(combiner), bufferSize());
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.png" alt="">
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> combineLatestDelayError(
            @NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestDelayError(sources, combiner, bufferSize());
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source ObservableSources terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided array of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @param bufferSize
     *            the internal buffer size and prefetch amount applied to every source Observable
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Observable<R> combineLatestDelayError(@NonNull ObservableSource<? extends T>[] sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(combiner, "combiner is null");
        if (sources.length == 0) {
            return empty();
        }
        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(sources, null, combiner, s, true));
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source ObservableSources terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> combineLatestDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner) {
        return combineLatestDelayError(sources, combiner, bufferSize());
    }

    /**
     * Combines a collection of source ObservableSources by emitting an item that aggregates the latest values of each of
     * the source ObservableSources each time an item is received from any of the source ObservableSources, where this
     * aggregation is defined by a specified function and delays any error from the sources until
     * all source ObservableSources terminate.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     * <p>
     * If any of the sources never produces an item but only terminates (normally or with an error), the
     * resulting sequence terminates immediately (normally or with all the errors accumulated till that point).
     * If that input source is also synchronous, other sources after it will not be subscribed to.
     * <p>
     * If the provided iterable of ObservableSources is empty, the resulting sequence completes immediately without emitting
     * any items and without any calls to the combiner function.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/combineLatestDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code combineLatestDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the common base type of source values
     * @param <R>
     *            the result type
     * @param sources
     *            the collection of source ObservableSources
     * @param combiner
     *            the aggregation function used to combine the items emitted by the source ObservableSources
     * @param bufferSize
     *            the internal buffer size and prefetch amount applied to every source Observable
     * @return an Observable that emits items that are the result of combining the items emitted by the source
     *         ObservableSources by means of the given aggregation function
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> Observable<R> combineLatestDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> combiner, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableCombineLatest<>(null, sources, combiner, s, true));
    }

    /**
     * Concatenates elements of each ObservableSource provided via an Iterable sequence into a single sequence
     * of elements without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the common value type of the sources
     * @param sources the Iterable sequence of ObservableSources
     * @return the new Observable instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concat(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return fromIterable(sources).concatMapDelayError((Function)Functions.identity(), false, bufferSize());
    }

    /**
     * Returns an Observable that emits the items emitted by each of the ObservableSources emitted by the source
     * ObservableSource, one after the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @return an Observable that emits items all of the items emitted by the ObservableSources emitted by
     *         {@code ObservableSources}, one after the other, without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concat(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concat(sources, bufferSize());
    }

    /**
     * Returns an Observable that emits the items emitted by each of the ObservableSources emitted by the source
     * ObservableSource, one after the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @param prefetch
     *            the number of ObservableSources to prefetch from the sources sequence.
     * @return an Observable that emits items all of the items emitted by the ObservableSources emitted by
     *         {@code ObservableSources}, one after the other, without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concat(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(sources, Functions.identity(), prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Returns an Observable that emits the items emitted by two ObservableSources, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be concatenated
     * @param source2
     *            an ObservableSource to be concatenated
     * @return an Observable that emits items emitted by the two source ObservableSources, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concat(@NonNull ObservableSource<? extends T> source1, ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return concatArray(source1, source2);
    }

    /**
     * Returns an Observable that emits the items emitted by three ObservableSources, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be concatenated
     * @param source2
     *            an ObservableSource to be concatenated
     * @param source3
     *            an ObservableSource to be concatenated
     * @return an Observable that emits items emitted by the three source ObservableSources, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concat(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return concatArray(source1, source2, source3);
    }

    /**
     * Returns an Observable that emits the items emitted by four ObservableSources, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be concatenated
     * @param source2
     *            an ObservableSource to be concatenated
     * @param source3
     *            an ObservableSource to be concatenated
     * @param source4
     *            an ObservableSource to be concatenated
     * @return an Observable that emits items emitted by the four source ObservableSources, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concat(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return concatArray(source1, source2, source3, source4);
    }

    /**
     * Concatenates a variable number of ObservableSource sources.
     * <p>
     * Note: named this way because of overload conflict with concat(ObservableSource&lt;ObservableSource&gt;)
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new Observable instance
     * @throws NullPointerException if sources is null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> concatArray(@NonNull ObservableSource<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            return wrap((ObservableSource<T>)sources[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(fromArray(sources), Functions.identity(), bufferSize(), ErrorMode.BOUNDARY));
    }

    /**
     * Concatenates a variable number of ObservableSource sources and delays errors from any of them
     * till all terminate.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new Observable instance
     * @throws NullPointerException if sources is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> concatArrayDelayError(@NonNull ObservableSource<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        if (sources.length == 1) {
            @SuppressWarnings("unchecked")
            Observable<T> source = (Observable<T>)wrap(sources[0]);
            return source;
        }
        return concatDelayError(fromArray(sources));
    }

    /**
     * Concatenates an array of ObservableSources eagerly into a single stream of values.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEager.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of ObservableSources that need to be eagerly concatenated
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <T> Observable<T> concatArrayEager(@NonNull ObservableSource<? extends T>... sources) {
        return concatArrayEager(bufferSize(), bufferSize(), sources);
    }

    /**
     * Concatenates an array of ObservableSources eagerly into a single stream of values.
     * <p>
     * <img width="640" height="495" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEager.nn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them
     * in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of ObservableSources that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscriptions at a time, {@link Integer#MAX_VALUE}
     *                       is interpreted as indication to subscribe to all sources at once
     * @param prefetch the number of elements to prefetch from each ObservableSource source
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> concatArrayEager(int maxConcurrency, int prefetch, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).concatMapEagerDelayError((Function)Functions.identity(), false, maxConcurrency, prefetch);
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values
     * and delaying any errors until all sources terminate.
     * <p>
     * <img width="640" height="354" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEagerDelayError.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s
     * and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @return the new Observable instance with the specified concatenation behavior
     * @since 2.2.1 - experimental
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <T> Observable<T> concatArrayEagerDelayError(@NonNull ObservableSource<? extends T>... sources) {
        return concatArrayEagerDelayError(bufferSize(), bufferSize(), sources);
    }

    /**
     * Concatenates an array of {@link ObservableSource}s eagerly into a single stream of values
     * and delaying any errors until all sources terminate.
     * <p>
     * <img width="640" height="460" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatArrayEagerDelayError.nn.png" alt="">
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source {@code ObservableSource}s. The operator buffers the values emitted by these {@code ObservableSource}s
     * and then drains them in order, each one after the previous one completes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources an array of {@code ObservableSource}s that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscriptions at a time, {@link Integer#MAX_VALUE}
     *                       is interpreted as indication to subscribe to all sources at once
     * @param prefetch the number of elements to prefetch from each {@code ObservableSource} source
     * @return the new Observable instance with the specified concatenation behavior
     * @since 2.2.1 - experimental
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> concatArrayEagerDelayError(int maxConcurrency, int prefetch, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).concatMapEagerDelayError((Function)Functions.identity(), true, maxConcurrency, prefetch);
    }

    /**
     * Concatenates the Iterable sequence of ObservableSources into a single sequence by subscribing to each ObservableSource,
     * one after the other, one at a time and delays any errors till the all inner ObservableSources terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the Iterable sequence of ObservableSources
     * @return the new ObservableSource with the concatenating behavior
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concatDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return concatDelayError(fromIterable(sources));
    }

    /**
     * Concatenates the ObservableSource sequence of ObservableSources into a single sequence by subscribing to each inner ObservableSource,
     * one after the other, one at a time and delays any errors till the all inner and the outer ObservableSources terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the ObservableSource sequence of ObservableSources
     * @return the new ObservableSource with the concatenating behavior
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concatDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concatDelayError(sources, bufferSize(), true);
    }

    /**
     * Concatenates the ObservableSource sequence of ObservableSources into a single sequence by subscribing to each inner ObservableSource,
     * one after the other, one at a time and delays any errors till the all inner and the outer ObservableSources terminate.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatDelayError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources the ObservableSource sequence of ObservableSources
     * @param prefetch the number of elements to prefetch from the outer ObservableSource
     * @param tillTheEnd if true exceptions from the outer and all inner ObservableSources are delayed to the end
     *                   if false, exception from the outer ObservableSource is delayed till the current ObservableSource terminates
     * @return the new ObservableSource with the concatenating behavior
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> concatDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int prefetch, boolean tillTheEnd) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMap(sources, Functions.identity(), prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }

    /**
     * Concatenates an ObservableSource sequence of ObservableSources eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source ObservableSources as they are observed. The operator buffers the values emitted by these
     * ObservableSources and then drains them in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatEager.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of ObservableSources that need to be eagerly concatenated
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concatEager(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return concatEager(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates an ObservableSource sequence of ObservableSources eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * emitted source ObservableSources as they are observed. The operator buffers the values emitted by these
     * ObservableSources and then drains them in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatEager.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of ObservableSources that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner ObservableSources; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner ObservableSources can be active at the same time
     * @param prefetch the number of elements to prefetch from each inner ObservableSource source
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concatEager(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int prefetch) {
        return wrap(sources).concatMapEager((Function)Functions.identity(), maxConcurrency, prefetch);
    }

    /**
     * Concatenates a sequence of ObservableSources eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them
     * in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatEager.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of ObservableSources that need to be eagerly concatenated
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concatEager(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        return concatEager(sources, bufferSize(), bufferSize());
    }

    /**
     * Concatenates a sequence of ObservableSources eagerly into a single stream of values.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them
     * in order, each one after the previous one completes.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatEager.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param sources a sequence of ObservableSources that need to be eagerly concatenated
     * @param maxConcurrency the maximum number of concurrently running inner ObservableSources; {@link Integer#MAX_VALUE}
     *                       is interpreted as all inner ObservableSources can be active at the same time
     * @param prefetch the number of elements to prefetch from each inner ObservableSource source
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> concatEager(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int prefetch) {
        return fromIterable(sources).concatMapEagerDelayError((Function)Functions.identity(), false, maxConcurrency, prefetch);
    }

    /**
     * Provides an API (via a cold Observable) that bridges the reactive world with the callback-style world.
     * <p>
     * Example:
     * <pre><code>
     * Observable.&lt;Event&gt;create(emitter -&gt; {
     *     Callback listener = new Callback() {
     *         &#64;Override
     *         public void onEvent(Event e) {
     *             emitter.onNext(e);
     *             if (e.isLast()) {
     *                 emitter.onComplete();
     *             }
     *         }
     *
     *         &#64;Override
     *         public void onFailure(Exception e) {
     *             emitter.onError(e);
     *         }
     *     };
     *
     *     AutoCloseable c = api.someMethod(listener);
     *
     *     emitter.setCancellable(c::close);
     *
     * });
     * </code></pre>
     * <p>
     * Whenever an {@link Observer} subscribes to the returned {@code Observable}, the provided
     * {@link ObservableOnSubscribe} callback is invoked with a fresh instance of an {@link ObservableEmitter}
     * that will interact only with that specific {@code Observer}. If this {@code Observer}
     * disposes the flow (making {@link ObservableEmitter#isDisposed} return true),
     * other observers subscribed to the same returned {@code Observable} are not affected.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/create.png" alt="">
     * <p>
     * You should call the ObservableEmitter's onNext, onError and onComplete methods in a serialized fashion. The
     * rest of its methods are thread-safe.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type
     * @param source the emitter that is called when an Observer subscribes to the returned {@code Observable}
     * @return the new Observable instance
     * @see ObservableOnSubscribe
     * @see ObservableEmitter
     * @see Cancellable
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableCreate<>(source));
    }

    /**
     * Returns an Observable that calls an ObservableSource factory to create an ObservableSource for each new Observer
     * that subscribes. That is, for each subscriber, the actual ObservableSource that subscriber observes is
     * determined by the factory function.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/defer.png" alt="">
     * <p>
     * The defer Observer allows you to defer or delay emitting items from an ObservableSource until such time as an
     * Observer subscribes to the ObservableSource. This allows an {@link Observer} to easily obtain updates or a
     * refreshed version of the sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param supplier
     *            the ObservableSource factory function to invoke for each {@link Observer} that subscribes to the
     *            resulting ObservableSource
     * @param <T>
     *            the type of the items emitted by the ObservableSource
     * @return an Observable whose {@link Observer}s' subscriptions trigger an invocation of the given
     *         ObservableSource factory function
     * @see <a href="http://reactivex.io/documentation/operators/defer.html">ReactiveX operators documentation: Defer</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> defer(@NonNull Supplier<? extends ObservableSource<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableDefer<>(supplier));
    }

    /**
     * Returns an Observable that emits no items to the {@link Observer} and immediately invokes its
     * {@link Observer#onComplete onComplete} method.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/empty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code empty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the type of the items (ostensibly) emitted by the ObservableSource
     * @return an Observable that emits no items to the {@link Observer} but immediately invokes the
     *         {@link Observer}'s {@link Observer#onComplete() onComplete} method
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Empty</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Observable<T> empty() {
        return RxJavaPlugins.onAssembly((Observable<T>) ObservableEmpty.INSTANCE);
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     * <p>
     * <img width="640" height="220" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.supplier.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param errorSupplier
     *            a Supplier factory to return a Throwable for each individual Observer
     * @param <T>
     *            the type of the items (ostensibly) emitted by the ObservableSource
     * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> error(@NonNull Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableError<>(errorSupplier));
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     * <p>
     * <img width="640" height="220" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/error.item.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param exception
     *            the particular Throwable to pass to {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the ObservableSource
     * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> error(@NonNull Throwable exception) {
        Objects.requireNonNull(exception, "exception is null");
        return error(Functions.justSupplier(exception));
    }

    /**
     * Converts an Array into an ObservableSource that emits the items in the Array.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/from.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            the array of elements
     * @param <T>
     *            the type of items in the Array and the type of items to be emitted by the resulting ObservableSource
     * @return an Observable that emits each item in the source Array
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> fromArray(@NonNull T... items) {
        Objects.requireNonNull(items, "items is null");
        if (items.length == 0) {
            return empty();
        }
        if (items.length == 1) {
            return just(items[0]);
        }
        return RxJavaPlugins.onAssembly(new ObservableFromArray<>(items));
    }

    /**
     * Returns an Observable that, when an observer subscribes to it, invokes a function you specify and then
     * emits the value returned from that function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCallable.png" alt="">
     * <p>
     * This allows you to defer the execution of the function you specify until an observer subscribes to the
     * ObservableSource. That is to say, it makes the function "lazy."
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromCallable} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Callable} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link Observer#onError(Throwable)},
     *   except when the downstream has disposed this {@code Observable} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   </dd>
     * </dl>
     * @param supplier
     *         a function, the execution of which should be deferred; {@code fromCallable} will invoke this
     *         function only when an observer subscribes to the ObservableSource that {@code fromCallable} returns
     * @param <T>
     *         the type of the item emitted by the ObservableSource
     * @return an Observable whose {@link Observer}s' subscriptions trigger an invocation of the given function
     * @see #defer(Supplier)
     * @see #fromSupplier(Supplier)
     * @since 2.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromCallable(@NonNull Callable<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableFromCallable<>(supplier));
    }

    /**
     * Converts a {@link Future} into an ObservableSource.
     * <p>
     * <img width="640" height="284" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.noarg.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an ObservableSource that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * <em>Important note:</em> This ObservableSource is blocking; you cannot dispose it.
     * <p>
     * Unlike 1.x, disposing the Observable won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting ObservableSource
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromFuture(@NonNull Future<? extends T> future) {
        Objects.requireNonNull(future, "future is null");
        return RxJavaPlugins.onAssembly(new ObservableFromFuture<>(future, 0L, null));
    }

    /**
     * Converts a {@link Future} into an ObservableSource, with a timeout on the Future.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.timeout.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an ObservableSource that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * Unlike 1.x, disposing the Observable won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <p>
     * <em>Important note:</em> This ObservableSource is blocking; you cannot dispose it.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting ObservableSource
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit) {
        Objects.requireNonNull(future, "future is null");
        Objects.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new ObservableFromFuture<>(future, timeout, unit));
    }

    /**
     * Converts a {@link Future} into an ObservableSource, with a timeout on the Future.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.timeout.scheduler.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an ObservableSource that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * Unlike 1.x, disposing the Observable won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <p>
     * <em>Important note:</em> This ObservableSource is blocking; you cannot dispose it.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param scheduler
     *            the {@link Scheduler} to wait for the Future on. Use a Scheduler such as
     *            {@link Schedulers#io()} that can block and wait on the Future
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting ObservableSource
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> Observable<T> fromFuture(@NonNull Future<? extends T> future, long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        Observable<T> o = fromFuture(future, timeout, unit);
        return o.subscribeOn(scheduler);
    }

    /**
     * Converts a {@link Future}, operating on a specified {@link Scheduler}, into an ObservableSource.
     * <p>
     * <img width="640" height="294" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromFuture.scheduler.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an ObservableSource that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * Unlike 1.x, disposing the Observable won't cancel the future. If necessary, one can use composition to achieve the
     * cancellation effect: {@code futureObservableSource.doOnDispose(() -> future.cancel(true));}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param future
     *            the source {@link Future}
     * @param scheduler
     *            the {@link Scheduler} to wait for the Future on. Use a Scheduler such as
     *            {@link Schedulers#io()} that can block and wait on the Future
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting ObservableSource
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> Observable<T> fromFuture(@NonNull Future<? extends T> future, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        Observable<T> o = fromFuture(future);
        return o.subscribeOn(scheduler);
    }

    /**
     * Converts an {@link Iterable} sequence into an ObservableSource that emits the items in the sequence.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromIterable.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type of items to be emitted by the
     *            resulting ObservableSource
     * @return an Observable that emits each item in the source {@link Iterable} sequence
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     * @see #fromStream(Stream)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromIterable(@NonNull Iterable<? extends T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableFromIterable<>(source));
    }

    /**
     * Converts an arbitrary Reactive-Streams Publisher into an Observable.
     * <p>
     * <img width="640" height="344" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromPublisher.o.png" alt="">
     * <p>
     * The {@link Publisher} must follow the
     * <a href="https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams">Reactive-Streams specification</a>.
     * Violating the specification may result in undefined behavior.
     * <p>
     * If possible, use {@link #create(ObservableOnSubscribe)} to create a
     * source-like {@code Observable} instead.
     * <p>
     * Note that even though {@link Publisher} appears to be a functional interface, it
     * is not recommended to implement it through a lambda as the specification requires
     * state management that is not achievable with a stateless lambda.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The source {@code publisher} is consumed in an unbounded fashion without applying any
     *  backpressure to it.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromPublisher} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type of the flow
     * @param publisher the Publisher to convert
     * @return the new Observable instance
     * @throws NullPointerException if publisher is null
     * @see #create(ObservableOnSubscribe)
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromPublisher(@NonNull Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return RxJavaPlugins.onAssembly(new ObservableFromPublisher<>(publisher));
    }

    /**
     * Returns an Observable that, when an observer subscribes to it, invokes a supplier function you specify and then
     * emits the value returned from that function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Observable.fromSupplier.png" alt="">
     * <p>
     * This allows you to defer the execution of the function you specify until an observer subscribes to the
     * ObservableSource. That is to say, it makes the function "lazy."
     * <dl>
     *   <dt><b>Scheduler:</b></dt>
     *   <dd>{@code fromSupplier} does not operate by default on a particular {@link Scheduler}.</dd>
     *   <dt><b>Error handling:</b></dt>
     *   <dd> If the {@link Supplier} throws an exception, the respective {@link Throwable} is
     *   delivered to the downstream via {@link Observer#onError(Throwable)},
     *   except when the downstream has disposed this {@code Observable} source.
     *   In this latter case, the {@code Throwable} is delivered to the global error handler via
     *   {@link RxJavaPlugins#onError(Throwable)} as an {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}.
     *   </dd>
     * </dl>
     * @param supplier
     *         a function, the execution of which should be deferred; {@code fromSupplier} will invoke this
     *         function only when an observer subscribes to the ObservableSource that {@code fromSupplier} returns
     * @param <T>
     *         the type of the item emitted by the ObservableSource
     * @return an Observable whose {@link Observer}s' subscriptions trigger an invocation of the given function
     * @see #defer(Supplier)
     * @see #fromCallable(Callable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> fromSupplier(@NonNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return RxJavaPlugins.onAssembly(new ObservableFromSupplier<>(supplier));
    }

    /**
     * Returns a cold, synchronous and stateless generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the generated value type
     * @param generator the Consumer called whenever a particular downstream Observer has
     * requested a value. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signalling multiple {@code onNext}
     * in a call will make the operator signal {@code IllegalStateException}.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> generate(@NonNull Consumer<Emitter<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(Functions.nullSupplier(),
                ObservableInternalHelper.simpleGenerator(generator), Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-Observer state
     * @param <T> the generated value type
     * @param initialState the Supplier to generate the initial state for each Observer
     * @param generator the Consumer called with the current state whenever a particular downstream Observer has
     * requested a value. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signalling multiple {@code onNext}
     * in a call will make the operator signal {@code IllegalStateException}.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiConsumer<S, Emitter<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, ObservableInternalHelper.simpleBiGenerator(generator), Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-Observer state
     * @param <T> the generated value type
     * @param initialState the Supplier to generate the initial state for each Observer
     * @param generator the Consumer called with the current state whenever a particular downstream Observer has
     * requested a value. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event. Signalling multiple {@code onNext}
     * in a call will make the operator signal {@code IllegalStateException}.
     * @param disposeState the Consumer that is called with the current state when the generator
     * terminates the sequence or it gets disposed
     * @return the new Observable instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, S> Observable<T> generate(
            @NonNull Supplier<S> initialState,
            @NonNull BiConsumer<S, Emitter<T>> generator,
            @NonNull Consumer<? super S> disposeState) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, ObservableInternalHelper.simpleBiGenerator(generator), disposeState);
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-Observer state
     * @param <T> the generated value type
     * @param initialState the Supplier to generate the initial state for each Observer
     * @param generator the Function called with the current state whenever a particular downstream Observer has
     * requested a value. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event and should return a (new) state for
     * the next invocation. Signalling multiple {@code onNext}
     * in a call will make the operator signal {@code IllegalStateException}.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiFunction<S, Emitter<T>, S> generator) {
        return generate(initialState, generator, Functions.emptyConsumer());
    }

    /**
     * Returns a cold, synchronous and stateful generator of values.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/generate.2.png" alt="">
     * <p>
     * Note that the {@link Emitter#onNext}, {@link Emitter#onError} and
     * {@link Emitter#onComplete} methods provided to the function via the {@link Emitter} instance should be called synchronously,
     * never concurrently and only while the function body is executing. Calling them from multiple threads
     * or outside the function call is not supported and leads to an undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code generate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <S> the type of the per-Observer state
     * @param <T> the generated value type
     * @param initialState the Supplier to generate the initial state for each Observer
     * @param generator the Function called with the current state whenever a particular downstream Observer has
     * requested a value. The callback then should call {@code onNext}, {@code onError} or
     * {@code onComplete} to signal a value or a terminal event and should return a (new) state for
     * the next invocation. Signalling multiple {@code onNext}
     * in a call will make the operator signal {@code IllegalStateException}.
     * @param disposeState the Consumer that is called with the current state when the generator
     * terminates the sequence or it gets disposed
     * @return the new Observable instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, S> Observable<T> generate(@NonNull Supplier<S> initialState, @NonNull BiFunction<S, Emitter<T>, S> generator,
            @NonNull Consumer<? super S> disposeState) {
        Objects.requireNonNull(initialState, "initialState is null");
        Objects.requireNonNull(generator, "generator is null");
        Objects.requireNonNull(disposeState, "disposeState is null");
        return RxJavaPlugins.onAssembly(new ObservableGenerate<>(initialState, generator, disposeState));
    }

    /**
     * Returns an Observable that emits a {@code 0L} after the {@code initialDelay} and ever increasing numbers
     * after each {@code period} of time thereafter.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.p.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code interval} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param initialDelay
     *            the initial delay time to wait before emitting the first value of 0L
     * @param period
     *            the period of time between emissions of the subsequent numbers
     * @param unit
     *            the time unit for both {@code initialDelay} and {@code period}
     * @return an Observable that emits a 0L after the {@code initialDelay} and ever increasing numbers after
     *         each {@code period} of time thereafter
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     * @since 1.0.12
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> interval(long initialDelay, long period, @NonNull TimeUnit unit) {
        return interval(initialDelay, period, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits a {@code 0L} after the {@code initialDelay} and ever increasing numbers
     * after each {@code period} of time thereafter, on a specified {@link Scheduler}.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.ps.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param initialDelay
     *            the initial delay time to wait before emitting the first value of 0L
     * @param period
     *            the period of time between emissions of the subsequent numbers
     * @param unit
     *            the time unit for both {@code initialDelay} and {@code period}
     * @param scheduler
     *            the Scheduler on which the waiting happens and items are emitted
     * @return an Observable that emits a 0L after the {@code initialDelay} and ever increasing numbers after
     *         each {@code period} of time thereafter, while running on the given Scheduler
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     * @since 1.0.12
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Observable<Long> interval(long initialDelay, long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableInterval(Math.max(0L, initialDelay), Math.max(0L, period), unit, scheduler));
    }

    /**
     * Returns an Observable that emits a sequential number every specified interval of time.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code interval} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param period
     *            the period size in time units (see below)
     * @param unit
     *            time units to use for the interval size
     * @return an Observable that emits a sequential number each time interval
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> interval(long period, @NonNull TimeUnit unit) {
        return interval(period, period, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits a sequential number every specified interval of time, on a
     * specified Scheduler.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/interval.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param period
     *            the period size in time units (see below)
     * @param unit
     *            time units to use for the interval size
     * @param scheduler
     *            the Scheduler to use for scheduling the items
     * @return an Observable that emits a sequential number each time interval
     * @see <a href="http://reactivex.io/documentation/operators/interval.html">ReactiveX operators documentation: Interval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static Observable<Long> interval(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return interval(period, period, unit, scheduler);
    }

    /**
     * Signals a range of long values, the first after some initial delay and the rest periodically after.
     * <p>
     * The sequence completes immediately after the last value (start + count - 1) has been reached.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/intervalRange.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code intervalRange} by default operates on the {@link Schedulers#computation() computation} {@link Scheduler}.</dd>
     * </dl>
     * @param start that start value of the range
     * @param count the number of values to emit in total, if zero, the operator emits an onComplete after the initial delay.
     * @param initialDelay the initial delay before signalling the first value (the start)
     * @param period the period between subsequent values
     * @param unit the unit of measure of the initialDelay and period amounts
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> intervalRange(long start, long count, long initialDelay, long period, @NonNull TimeUnit unit) {
        return intervalRange(start, count, initialDelay, period, unit, Schedulers.computation());
    }

    /**
     * Signals a range of long values, the first after some initial delay and the rest periodically after.
     * <p>
     * The sequence completes immediately after the last value (start + count - 1) has been reached.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/intervalRange.s.png" alt="">     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>you provide the {@link Scheduler}.</dd>
     * </dl>
     * @param start that start value of the range
     * @param count the number of values to emit in total, if zero, the operator emits an onComplete after the initial delay.
     * @param initialDelay the initial delay before signalling the first value (the start)
     * @param period the period between subsequent values
     * @param unit the unit of measure of the initialDelay and period amounts
     * @param scheduler the target scheduler where the values and terminal signals will be emitted
     * @return the new Observable instance
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Observable<Long> intervalRange(long start, long count, long initialDelay, long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }

        if (count == 0L) {
            return Observable.<Long>empty().delay(initialDelay, unit, scheduler);
        }

        long end = start + (count - 1);
        if (start > 0 && end < 0) {
            throw new IllegalArgumentException("Overflow! start + count is bigger than Long.MAX_VALUE");
        }
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableIntervalRange(start, end, Math.max(0L, initialDelay), Math.max(0L, period), unit, scheduler));
    }

    /**
     * Returns an Observable that signals the given (constant reference) item and then completes.
     * <p>
     * <img width="640" height="290" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.item.png" alt="">
     * <p>
     * Note that the item is taken and re-emitted as is and not computed by any means by {@code just}. Use {@link #fromCallable(Callable)}
     * to generate a single item on demand (when {@code Observer}s subscribe to it).
     * <p>
     * See the multi-parameter overloads of {@code just} to emit more than one (constant reference) items one after the other.
     * Use {@link #fromArray(Object...)} to emit an arbitrary number of items that are known upfront.
     * <p>
     * To emit the items of an {@link Iterable} sequence (such as a {@link java.util.List}), use {@link #fromIterable(Iterable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return an Observable that emits {@code value} as a single item and then completes
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     * @see #just(Object, Object)
     * @see #fromCallable(Callable)
     * @see #fromArray(Object...)
     * @see #fromIterable(Iterable)
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return RxJavaPlugins.onAssembly(new ObservableJust<>(item));
    }

    /**
     * Converts two items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");

        return fromArray(item1, item2);
    }

    /**
     * Converts three items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");

        return fromArray(item1, item2, item3);
    }

    /**
     * Converts four items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.4.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");

        return fromArray(item1, item2, item3, item4);
    }

    /**
     * Converts five items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.5.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");

        return fromArray(item1, item2, item3, item4, item5);
    }

    /**
     * Converts six items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.6.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");

        return fromArray(item1, item2, item3, item4, item5, item6);
    }

    /**
     * Converts seven items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.7.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7);
    }

    /**
     * Converts eight items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.8.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    /**
     * Converts nine items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.9.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param item9
     *            ninth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8, @NonNull T item9) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");
        Objects.requireNonNull(item9, "item9 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8, item9);
    }

    /**
     * Converts ten items into an ObservableSource that emits those items.
     * <p>
     * <img width="640" height="186" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/just.10.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item1
     *            first item
     * @param item2
     *            second item
     * @param item3
     *            third item
     * @param item4
     *            fourth item
     * @param item5
     *            fifth item
     * @param item6
     *            sixth item
     * @param item7
     *            seventh item
     * @param item8
     *            eighth item
     * @param item9
     *            ninth item
     * @param item10
     *            tenth item
     * @param <T>
     *            the type of these items
     * @return an Observable that emits each item
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    @CheckReturnValue
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> Observable<T> just(@NonNull T item1, @NonNull T item2, @NonNull T item3, @NonNull T item4, @NonNull T item5, @NonNull T item6, @NonNull T item7, @NonNull T item8, @NonNull T item9, @NonNull T item10) {
        Objects.requireNonNull(item1, "item1 is null");
        Objects.requireNonNull(item2, "item2 is null");
        Objects.requireNonNull(item3, "item3 is null");
        Objects.requireNonNull(item4, "item4 is null");
        Objects.requireNonNull(item5, "item5 is null");
        Objects.requireNonNull(item6, "item6 is null");
        Objects.requireNonNull(item7, "item7 is null");
        Objects.requireNonNull(item8, "item8 is null");
        Objects.requireNonNull(item9, "item9 is null");
        Objects.requireNonNull(item10, "item10 is null");

        return fromArray(item1, item2, item3, item4, item5, item6, item7, item8, item9, item10);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, without any transformation, while limiting the
     * number of concurrent subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable, int, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items to prefetch from each inner ObservableSource
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @throws IllegalArgumentException
     *             if {@code maxConcurrent} is less than or equal to 0
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable, int, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, without any transformation, while limiting the
     * number of concurrent subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(int, int, ObservableSource...)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items to prefetch from each inner ObservableSource
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @throws IllegalArgumentException
     *             if {@code maxConcurrent} is less than or equal to 0
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeArrayDelayError(int, int, ObservableSource...)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> mergeArray(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity());
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, without any transformation, while limiting the
     * number of concurrent subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(Iterable, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @throws IllegalArgumentException
     *             if {@code maxConcurrent} is less than or equal to 0
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(Iterable, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), maxConcurrency);
    }

    /**
     * Flattens an ObservableSource that emits ObservableSources into a single ObservableSource that emits the items emitted by
     * those ObservableSources, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.oo.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @return an Observable that emits items that are the result of flattening the ObservableSources emitted by the
     *         {@code source} ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NonNull
    public static <T> Observable<T> merge(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), false, Integer.MAX_VALUE, bufferSize()));
    }

    /**
     * Flattens an ObservableSource that emits ObservableSources into a single ObservableSource that emits the items emitted by
     * those ObservableSources, without any transformation, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.oo.png" alt="">
     * <p>
     * You can combine the items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, int)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits items that are the result of flattening the ObservableSources emitted by the
     *         {@code source} ObservableSource
     * @throws IllegalArgumentException
     *             if {@code maxConcurrent} is less than or equal to 0
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @since 1.1.0
     * @see #mergeDelayError(ObservableSource, int)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), false, maxConcurrency, bufferSize()));
    }

    /**
     * Flattens two ObservableSources into a single ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return fromArray(source1, source2).flatMap((Function)Functions.identity(), false, 2);
    }

    /**
     * Flattens three ObservableSources into a single ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @param source3
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return fromArray(source1, source2, source3).flatMap((Function)Functions.identity(), false, 3);
    }

    /**
     * Flattens four ObservableSources into a single ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeDelayError(ObservableSource, ObservableSource, ObservableSource, ObservableSource)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @param source3
     *            an ObservableSource to be merged
     * @param source4
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeDelayError(ObservableSource, ObservableSource, ObservableSource, ObservableSource)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> merge(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return fromArray(source1, source2, source3, source4).flatMap((Function)Functions.identity(), false, 4);
    }

    /**
     * Flattens an Array of ObservableSources into one ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.io.png" alt="">
     * <p>
     * You can combine items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code merge} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArray} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If any of the source {@code ObservableSource}s signal a {@code Throwable} via {@code onError}, the resulting
     *  {@code Observable} terminates with that {@code Throwable} and all other source {@code ObservableSource}s are disposed.
     *  If more than one {@code ObservableSource} signals an error, the resulting {@code Observable} may terminate with the
     *  first one's error or, depending on the concurrency of the sources, may terminate with a
     *  {@code CompositeException} containing two or more of the various error signals.
     *  {@code Throwable}s that didn't make into the composite will be sent (individually) to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors. Similarly, {@code Throwable}s
     *  signaled by source(s) after the returned {@code Observable} has been disposed or terminated with a
     *  (composite) error will be sent to the same global error handler.
     *  Use {@link #mergeArrayDelayError(ObservableSource...)} to merge sources and terminate only when all source {@code ObservableSource}s
     *  have completed or failed with an error.
     *  </dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of ObservableSources
     * @return an Observable that emits all of the items emitted by the ObservableSources in the Array
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @see #mergeArrayDelayError(ObservableSource...)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> mergeArray(@NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), sources.length);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these ObservableSources.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items to prefetch from each inner ObservableSource
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, int maxConcurrency, int bufferSize) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an array of ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these ObservableSources.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the array of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @param bufferSize
     *            the number of items to prefetch from each inner ObservableSource
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> mergeArrayDelayError(int maxConcurrency, int bufferSize, @NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them, while limiting the number of concurrent subscriptions to these ObservableSources.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency);
    }

    /**
     * Flattens an ObservableSource that emits ObservableSources into one ObservableSource, in a way that allows an Observer to
     * receive all successfully emitted items from all of the source ObservableSources without being interrupted by
     * an error notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @return an Observable that emits all of the items emitted by the ObservableSources emitted by the
     *         {@code source} ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NonNull
    public static <T> Observable<T> mergeDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), true, Integer.MAX_VALUE, bufferSize()));
    }

    /**
     * Flattens an ObservableSource that emits ObservableSources into one ObservableSource, in a way that allows an Observer to
     * receive all successfully emitted items from all of the source ObservableSources without being interrupted by
     * an error notification from one of them, while limiting the
     * number of concurrent subscriptions to these ObservableSources.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            an ObservableSource that emits ObservableSources
     * @param maxConcurrency
     *            the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits all of the items emitted by the ObservableSources emitted by the
     *         {@code source} ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     * @since 2.0
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int maxConcurrency) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        return RxJavaPlugins.onAssembly(new ObservableFlatMap(sources, Functions.identity(), true, maxConcurrency, bufferSize()));
    }

    /**
     * Flattens two ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource)} except that if any of the merged ObservableSources
     * notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from
     * propagating that error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if both merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items that are emitted by the two source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        return fromArray(source1, source2).flatMap((Function)Functions.identity(), true, 2);
    }

    /**
     * Flattens three ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from all of the source ObservableSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource, ObservableSource)} except that if any of the merged
     * ObservableSources notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain
     * from propagating that error notification until all of the merged ObservableSources have finished emitting
     * items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @param source3
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items that are emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        return fromArray(source1, source2, source3).flatMap((Function)Functions.identity(), true, 3);
    }

    /**
     * Flattens four ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from all of the source ObservableSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource, ObservableSource, ObservableSource, ObservableSource)} except that if any of
     * the merged ObservableSources notify of an error via {@link Observer#onError onError}, {@code mergeDelayError}
     * will refrain from propagating that error notification until all of the merged ObservableSources have finished
     * emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param source1
     *            an ObservableSource to be merged
     * @param source2
     *            an ObservableSource to be merged
     * @param source3
     *            an ObservableSource to be merged
     * @param source4
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items that are emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> mergeDelayError(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull ObservableSource<? extends T> source3, @NonNull ObservableSource<? extends T> source4) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        return fromArray(source1, source2, source3, source4).flatMap((Function)Functions.identity(), true, 4);
    }

    /**
     * Flattens an Iterable of ObservableSources into one ObservableSource, in a way that allows an Observer to receive all
     * successfully emitted items from each of the source ObservableSources without being interrupted by an error
     * notification from one of them.
     * <p>
     * This behaves like {@link #merge(ObservableSource)} except that if any of the merged ObservableSources notify of an
     * error via {@link Observer#onError onError}, {@code mergeDelayError} will refrain from propagating that
     * error notification until all of the merged ObservableSources have finished emitting items.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeDelayError.png" alt="">
     * <p>
     * Even if multiple merged ObservableSources send {@code onError} notifications, {@code mergeDelayError} will only
     * invoke the {@code onError} method of its Observers once.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeArrayDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element base type
     * @param sources
     *            the Iterable of ObservableSources
     * @return an Observable that emits items that are the result of flattening the items emitted by the
     *         ObservableSources in the Iterable
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    @SafeVarargs
    public static <T> Observable<T> mergeArrayDelayError(@NonNull ObservableSource<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, sources.length);
    }

    /**
     * Returns an Observable that never sends any items or notifications to an {@link Observer}.
     * <p>
     * <img width="640" height="185" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/never.png" alt="">
     * <p>
     * This ObservableSource is useful primarily for testing purposes.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code never} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T>
     *            the type of items (not) emitted by the ObservableSource
     * @return an Observable that never emits any items or sends any notifications to an {@link Observer}
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Never</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Observable<T> never() {
        return RxJavaPlugins.onAssembly((Observable<T>) ObservableNever.INSTANCE);
    }

    /**
     * Returns an Observable that emits a sequence of Integers within a specified range.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/range.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code range} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param start
     *            the value of the first Integer in the sequence
     * @param count
     *            the number of sequential Integers to generate
     * @return an Observable that emits a range of sequential Integers
     * @throws IllegalArgumentException
     *             if {@code count} is less than zero, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Integer#MAX_VALUE}
     * @see <a href="http://reactivex.io/documentation/operators/range.html">ReactiveX operators documentation: Range</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Observable<Integer> range(int start, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return empty();
        }
        if (count == 1) {
            return just(start);
        }
        if ((long)start + (count - 1) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer overflow");
        }
        return RxJavaPlugins.onAssembly(new ObservableRange(start, count));
    }

    /**
     * Returns an Observable that emits a sequence of Longs within a specified range.
     * <p>
     * <img width="640" height="195" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/rangeLong.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code rangeLong} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param start
     *            the value of the first Long in the sequence
     * @param count
     *            the number of sequential Longs to generate
     * @return an Observable that emits a range of sequential Longs
     * @throws IllegalArgumentException
     *             if {@code count} is less than zero, or if {@code start} + {@code count} &minus; 1 exceeds
     *             {@link Long#MAX_VALUE}
     * @see <a href="http://reactivex.io/documentation/operators/range.html">ReactiveX operators documentation: Range</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static Observable<Long> rangeLong(long start, long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }

        if (count == 0) {
            return empty();
        }

        if (count == 1) {
            return just(start);
        }

        long end = start + (count - 1);
        if (start > 0 && end < 0) {
            throw new IllegalArgumentException("Overflow! start + count is bigger than Long.MAX_VALUE");
        }

        return RxJavaPlugins.onAssembly(new ObservableRangeLong(start, count));
    }

    /**
     * Returns a Single that emits a Boolean value that indicates whether two ObservableSource sequences are the
     * same by comparing the items emitted by each ObservableSource pairwise.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first ObservableSource to compare
     * @param source2
     *            the second ObservableSource to compare
     * @param <T>
     *            the type of items emitted by each ObservableSource
     * @return a Single that emits a Boolean value that indicates whether the two sequences are the same
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Single<Boolean> sequenceEqual(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate(), bufferSize());
    }

    /**
     * Returns a Single that emits a Boolean value that indicates whether two ObservableSource sequences are the
     * same by comparing the items emitted by each ObservableSource pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first ObservableSource to compare
     * @param source2
     *            the second ObservableSource to compare
     * @param isEqual
     *            a function used to compare items emitted by each ObservableSource
     * @param <T>
     *            the type of items emitted by each ObservableSource
     * @return a Single that emits a Boolean value that indicates whether the two ObservableSource two sequences
     *         are the same according to the specified function
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Single<Boolean> sequenceEqual(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull BiPredicate<? super T, ? super T> isEqual) {
        return sequenceEqual(source1, source2, isEqual, bufferSize());
    }

    /**
     * Returns a Single that emits a Boolean value that indicates whether two ObservableSource sequences are the
     * same by comparing the items emitted by each ObservableSource pairwise based on the results of a specified
     * equality function.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first ObservableSource to compare
     * @param source2
     *            the second ObservableSource to compare
     * @param isEqual
     *            a function used to compare items emitted by each ObservableSource
     * @param bufferSize
     *            the number of items to prefetch from the first and second source ObservableSource
     * @param <T>
     *            the type of items emitted by each ObservableSource
     * @return an Observable that emits a Boolean value that indicates whether the two ObservableSource two sequences
     *         are the same according to the specified function
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Single<Boolean> sequenceEqual(
            @NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            @NonNull BiPredicate<? super T, ? super T> isEqual, int bufferSize) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(isEqual, "isEqual is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableSequenceEqualSingle<>(source1, source2, isEqual, bufferSize));
    }

    /**
     * Returns a Single that emits a Boolean value that indicates whether two ObservableSource sequences are the
     * same by comparing the items emitted by each ObservableSource pairwise.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sequenceEqual.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sequenceEqual} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source1
     *            the first ObservableSource to compare
     * @param source2
     *            the second ObservableSource to compare
     * @param bufferSize
     *            the number of items to prefetch from the first and second source ObservableSource
     * @param <T>
     *            the type of items emitted by each ObservableSource
     * @return a Single that emits a Boolean value that indicates whether the two sequences are the same
     * @see <a href="http://reactivex.io/documentation/operators/sequenceequal.html">ReactiveX operators documentation: SequenceEqual</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Single<Boolean> sequenceEqual(@NonNull ObservableSource<? extends T> source1, @NonNull ObservableSource<? extends T> source2,
            int bufferSize) {
        return sequenceEqual(source1, source2, ObjectHelper.equalsPredicate(), bufferSize);
    }

    /**
     * Converts an ObservableSource that emits ObservableSources into an ObservableSource that emits the items emitted by the
     * most recently emitted of those ObservableSources.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an ObservableSource that emits ObservableSources. Each time it observes one of
     * these emitted ObservableSources, the ObservableSource returned by {@code switchOnNext} begins emitting the items
     * emitted by that ObservableSource. When a new ObservableSource is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted ObservableSource and begins emitting items from the new one.
     * <p>
     * The resulting ObservableSource completes if both the outer ObservableSource and the last inner ObservableSource, if any, complete.
     * If the outer ObservableSource signals an onError, the inner ObservableSource is disposed and the error delivered in-sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the source ObservableSource that emits ObservableSources
     * @param bufferSize
     *            the number of items to prefetch from the inner ObservableSources
     * @return an Observable that emits the items emitted by the ObservableSource most recently emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> switchOnNext(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap(sources, Functions.identity(), bufferSize, false));
    }

    /**
     * Converts an ObservableSource that emits ObservableSources into an ObservableSource that emits the items emitted by the
     * most recently emitted of those ObservableSources.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchDo.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an ObservableSource that emits ObservableSources. Each time it observes one of
     * these emitted ObservableSources, the ObservableSource returned by {@code switchOnNext} begins emitting the items
     * emitted by that ObservableSource. When a new ObservableSource is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted ObservableSource and begins emitting items from the new one.
     * <p>
     * The resulting ObservableSource completes if both the outer ObservableSource and the last inner ObservableSource, if any, complete.
     * If the outer ObservableSource signals an onError, the inner ObservableSource is disposed and the error delivered in-sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the source ObservableSource that emits ObservableSources
     * @return an Observable that emits the items emitted by the ObservableSource most recently emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> switchOnNext(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return switchOnNext(sources, bufferSize());
    }

    /**
     * Converts an ObservableSource that emits ObservableSources into an ObservableSource that emits the items emitted by the
     * most recently emitted of those ObservableSources and delays any exception until all ObservableSources terminate.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchOnNextDelayError.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an ObservableSource that emits ObservableSources. Each time it observes one of
     * these emitted ObservableSources, the ObservableSource returned by {@code switchOnNext} begins emitting the items
     * emitted by that ObservableSource. When a new ObservableSource is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted ObservableSource and begins emitting items from the new one.
     * <p>
     * The resulting ObservableSource completes if both the main ObservableSource and the last inner ObservableSource, if any, complete.
     * If the main ObservableSource signals an onError, the termination of the last inner ObservableSource will emit that error as is
     * or wrapped into a CompositeException along with the other possible errors the former inner ObservableSources signalled.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the source ObservableSource that emits ObservableSources
     * @return an Observable that emits the items emitted by the ObservableSource most recently emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> switchOnNextDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources) {
        return switchOnNextDelayError(sources, bufferSize());
    }

    /**
     * Converts an ObservableSource that emits ObservableSources into an ObservableSource that emits the items emitted by the
     * most recently emitted of those ObservableSources and delays any exception until all ObservableSources terminate.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchOnNextDelayError.png" alt="">
     * <p>
     * {@code switchOnNext} subscribes to an ObservableSource that emits ObservableSources. Each time it observes one of
     * these emitted ObservableSources, the ObservableSource returned by {@code switchOnNext} begins emitting the items
     * emitted by that ObservableSource. When a new ObservableSource is emitted, {@code switchOnNext} stops emitting items
     * from the earlier-emitted ObservableSource and begins emitting items from the new one.
     * <p>
     * The resulting ObservableSource completes if both the main ObservableSource and the last inner ObservableSource, if any, complete.
     * If the main ObservableSource signals an onError, the termination of the last inner ObservableSource will emit that error as is
     * or wrapped into a CompositeException along with the other possible errors the former inner ObservableSources signalled.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchOnNextDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the item type
     * @param sources
     *            the source ObservableSource that emits ObservableSources
     * @param prefetch
     *            the number of items to prefetch from the inner ObservableSources
     * @return an Observable that emits the items emitted by the ObservableSource most recently emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/switch.html">ReactiveX operators documentation: Switch</a>
     * @since 2.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> switchOnNextDelayError(@NonNull ObservableSource<? extends ObservableSource<? extends T>> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap(sources, Functions.identity(), prefetch, true));
    }

    /**
     * Returns an Observable that emits {@code 0L} after a specified delay, and then completes.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single {@code 0L}
     * @param unit
     *            time units to use for {@code delay}
     * @return an Observable that {@code 0L} after a specified delay, and then completes
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public static Observable<Long> timer(long delay, @NonNull TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits {@code 0L} after a specified delay, on a specified Scheduler, and then
     * completes.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timer.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param delay
     *            the initial delay before emitting a single 0L
     * @param unit
     *            time units to use for {@code delay}
     * @param scheduler
     *            the {@link Scheduler} to use for scheduling the item
     * @throws NullPointerException
     *             if {@code unit} is null, or
     *             if {@code scheduler} is null
     * @return an Observable that emits {@code 0L} after a specified delay, on a specified Scheduler, and then
     *         completes
     * @see <a href="http://reactivex.io/documentation/operators/timer.html">ReactiveX operators documentation: Timer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public static Observable<Long> timer(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableTimer(Math.max(delay, 0L), unit, scheduler));
    }

    /**
     * Create an Observable by wrapping an ObservableSource <em>which has to be implemented according
     * to the Reactive-Streams-based Observable specification by handling
     * disposal correctly; no safeguards are provided by the Observable itself</em>.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code unsafeCreate} by default doesn't operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type emitted
     * @param onSubscribe the ObservableSource instance to wrap
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> unsafeCreate(@NonNull ObservableSource<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        if (onSubscribe instanceof Observable) {
            throw new IllegalArgumentException("unsafeCreate(Observable) should be upgraded");
        }
        return RxJavaPlugins.onAssembly(new ObservableFromUnsafeSource<>(onSubscribe));
    }

    /**
     * Constructs an ObservableSource that creates a dependent resource object which is disposed of when the downstream
     * calls dispose().
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated ObservableSource
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the ObservableSource
     * @param sourceSupplier
     *            the factory function to create an ObservableSource
     * @param disposer
     *            the function that will dispose of the resource
     * @return the ObservableSource whose lifetime controls the lifetime of the dependent resource object
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, D> Observable<T> using(
            @NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> disposer) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    /**
     * Constructs an ObservableSource that creates a dependent resource object which is disposed of just before
     * termination if you have set {@code disposeEagerly} to {@code true} and a dispose() call does not occur
     * before termination. Otherwise resource disposal will occur on a dispose() call.  Eager disposal is
     * particularly appropriate for a synchronous ObservableSource that reuses resources. {@code disposeAction} will
     * only be called once per subscription.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/using.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code using} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the element type of the generated ObservableSource
     * @param <D> the type of the resource associated with the output sequence
     * @param resourceSupplier
     *            the factory function to create a resource object that depends on the ObservableSource
     * @param sourceSupplier
     *            the factory function to create an ObservableSource
     * @param disposer
     *            the function that will dispose of the resource
     * @param eager
     *            If {@code true} then resource disposal will happen either on a {@code dispose()} call before the upstream is disposed
     *            or just before the emission of a terminal event ({@code onComplete} or {@code onError}).
     *            If {@code false} the resource disposal will happen either on a {@code dispose()} call after the upstream is disposed
     *            or just after the emission of a terminal event ({@code onComplete} or {@code onError}).
     * @return the ObservableSource whose lifetime controls the lifetime of the dependent resource object
     * @see <a href="http://reactivex.io/documentation/operators/using.html">ReactiveX operators documentation: Using</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, D> Observable<T> using(
            @NonNull Supplier<? extends D> resourceSupplier,
            @NonNull Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
            @NonNull Consumer<? super D> disposer, boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(disposer, "disposer is null");
        return RxJavaPlugins.onAssembly(new ObservableUsing<T, D>(resourceSupplier, sourceSupplier, disposer, eager));
    }

    /**
     * Wraps an ObservableSource into an Observable if not already an Observable.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code wrap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the value type
     * @param source the source ObservableSource instance
     * @return the new Observable instance or the same as the source
     * @throws NullPointerException if source is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<T> wrap(@NonNull ObservableSource<T> source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Observable) {
            return RxJavaPlugins.onAssembly((Observable<T>)source);
        }
        return RxJavaPlugins.onAssembly(new ObservableFromUnsafeSource<>(source));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an Iterable of other ObservableSources.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each of the source ObservableSources;
     * the second item emitted by the new ObservableSource will be the result of the function applied to the second
     * item emitted by each of those ObservableSources; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the source ObservableSource that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(Arrays.asList(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)), (a) -&gt; a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common value type
     * @param <R> the zipped result type
     * @param sources
     *            an Iterable of source ObservableSources
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> zip(@NonNull Iterable<? extends ObservableSource<? extends T>> sources, @NonNull Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(null, sources, zipper, bufferSize(), false));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an Iterable of other ObservableSources.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each of the source ObservableSources;
     * the second item emitted by the new ObservableSource will be the result of the function applied to the second
     * item emitted by each of those ObservableSources; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the source ObservableSource that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(Arrays.asList(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)), (a) -&gt; a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zipIterable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     *
     * @param sources
     *            an Iterable of source ObservableSources
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @param delayError
     *            delay errors signalled by any of the source ObservableSource until all ObservableSources terminate
     * @param bufferSize
     *            the number of elements to prefetch from each source ObservableSource
     * @param <T> the common source value type
     * @param <R> the zipped result type
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T, R> Observable<R> zip(@NonNull Iterable<? extends ObservableSource<? extends T>> sources,
            @NonNull Function<? super Object[], ? extends R> zipper, boolean delayError,
            int bufferSize) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(null, sources, zipper, bufferSize, delayError));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the new ObservableSource will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results
     *            in an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the new ObservableSource will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results
     *            in an item that will be emitted by the resulting ObservableSource
     * @param delayError delay errors from any of the source ObservableSources till the other terminates
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize(), source1, source2);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the new ObservableSource will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results
     *            in an item that will be emitted by the resulting ObservableSource
     * @param delayError delay errors from any of the source ObservableSources till the other terminates
     * @param bufferSize the number of elements to prefetch from each source ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError, int bufferSize) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize, source1, source2);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * three items emitted, in sequence, by three other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, and the first item emitted by {@code o3}; the second item emitted by the new
     * ObservableSource will be the result of the function applied to the second item emitted by {@code o1}, the
     * second item emitted by {@code o2}, and the second item emitted by {@code o3}; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3,
            @NonNull Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * four items emitted, in sequence, by four other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, and the first item emitted by {@code 04};
     * the second item emitted by the new ObservableSource will be the result of the function applied to the second
     * item emitted by each of those ObservableSources; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2,
            @NonNull ObservableSource<? extends T3> source3, @NonNull ObservableSource<? extends T4> source4,
            @NonNull Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * five items emitted, in sequence, by five other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, the first item emitted by {@code o4}, and
     * the first item emitted by {@code o5}; the second item emitted by the new ObservableSource will be the result of
     * the function applied to the second item emitted by each of those ObservableSources; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param source5
     *            a fifth source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, T5, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5,
            @NonNull Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * six items emitted, in sequence, by six other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each source ObservableSource, the
     * second item emitted by the new ObservableSource will be the result of the function applied to the second item
     * emitted by each of those ObservableSources, and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param source5
     *            a fifth source ObservableSource
     * @param source6
     *            a sixth source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * seven items emitted, in sequence, by seven other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each source ObservableSource, the
     * second item emitted by the new ObservableSource will be the result of the function applied to the second item
     * emitted by each of those ObservableSources, and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param source5
     *            a fifth source ObservableSource
     * @param source6
     *            a sixth source ObservableSource
     * @param source7
     *            a seventh source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7,
            @NonNull Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * eight items emitted, in sequence, by eight other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each source ObservableSource, the
     * second item emitted by the new ObservableSource will be the result of the function applied to the second item
     * emitted by each of those ObservableSources, and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g, h) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <T8> the value type of the eighth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param source5
     *            a fifth source ObservableSource
     * @param source6
     *            a sixth source ObservableSource
     * @param source7
     *            a seventh source ObservableSource
     * @param source8
     *            an eighth source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8,
            @NonNull Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7, source8);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * nine items emitted, in sequence, by nine other ObservableSources.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each source ObservableSource, the
     * second item emitted by the new ObservableSource will be the result of the function applied to the second item
     * emitted by each of those ObservableSources, and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext}
     * as many times as the number of {@code onNext} invocations of the source ObservableSource that emits the fewest
     * items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2), ..., (a, b, c, d, e, f, g, h, i) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the value type of the first source
     * @param <T2> the value type of the second source
     * @param <T3> the value type of the third source
     * @param <T4> the value type of the fourth source
     * @param <T5> the value type of the fifth source
     * @param <T6> the value type of the sixth source
     * @param <T7> the value type of the seventh source
     * @param <T8> the value type of the eighth source
     * @param <T9> the value type of the ninth source
     * @param <R> the zipped result type
     * @param source1
     *            the first source ObservableSource
     * @param source2
     *            a second source ObservableSource
     * @param source3
     *            a third source ObservableSource
     * @param source4
     *            a fourth source ObservableSource
     * @param source5
     *            a fifth source ObservableSource
     * @param source6
     *            a sixth source ObservableSource
     * @param source7
     *            a seventh source ObservableSource
     * @param source8
     *            an eighth source ObservableSource
     * @param source9
     *            a ninth source ObservableSource
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Observable<R> zip(
            @NonNull ObservableSource<? extends T1> source1, @NonNull ObservableSource<? extends T2> source2, @NonNull ObservableSource<? extends T3> source3,
            @NonNull ObservableSource<? extends T4> source4, @NonNull ObservableSource<? extends T5> source5, @NonNull ObservableSource<? extends T6> source6,
            @NonNull ObservableSource<? extends T7> source7, @NonNull ObservableSource<? extends T8> source8, @NonNull ObservableSource<? extends T9> source9,
            @NonNull Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(source5, "source5 is null");
        Objects.requireNonNull(source6, "source6 is null");
        Objects.requireNonNull(source7, "source7 is null");
        Objects.requireNonNull(source8, "source8 is null");
        Objects.requireNonNull(source9, "source9 is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), source1, source2, source3, source4, source5, source6, source7, source8, source9);
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * items emitted, in sequence, by an array of other ObservableSources.
     * <p>
     * {@code zip} applies this function in strict sequence, so the first item emitted by the new ObservableSource
     * will be the result of the function applied to the first item emitted by each of the source ObservableSources;
     * the second item emitted by the new ObservableSource will be the result of the function applied to the second
     * item emitted by each of those ObservableSources; and so forth.
     * <p>
     * The resulting {@code ObservableSource<R>} returned from {@code zip} will invoke {@code onNext} as many times as
     * the number of {@code onNext} invocations of the source ObservableSource that emits the fewest items.
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>zip(new ObservableSource[]{range(1, 5).doOnComplete(action1), range(6, 5).doOnComplete(action2)}, (a) -&gt;
     * a)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <p>
     * Note on method signature: since Java doesn't allow creating a generic array with {@code new T[]}, the
     * implementation of this operator has to create an {@code Object[]} instead. Unfortunately, a
     * {@code Function<Integer[], R>} passed to the method would trigger a {@code ClassCastException}.
     *
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zipArray.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the common element type
     * @param <R> the result type
     * @param sources
     *            an array of source ObservableSources
     * @param zipper
     *            a function that, when applied to an item emitted by each of the source ObservableSources, results in
     *            an item that will be emitted by the resulting ObservableSource
     * @param delayError
     *            delay errors signalled by any of the source ObservableSource until all ObservableSources terminate
     * @param bufferSize
     *            the number of elements to prefetch from each source ObservableSource
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public static <T, R> Observable<R> zipArray(
            @NonNull Function<? super Object[], ? extends R> zipper,
            boolean delayError, int bufferSize,
            @NonNull ObservableSource<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(zipper, "zipper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableZip<>(sources, null, zipper, bufferSize, delayError));
    }

    // ***************************************************************************************************
    // Instance operators
    // ***************************************************************************************************

    /**
     * Returns a Single that emits a Boolean that indicates whether all of the items emitted by the source
     * ObservableSource satisfy a condition.
     * <p>
     * <img width="640" height="264" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/all.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code all} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates an item and returns a Boolean
     * @return a Single that emits {@code true} if all items emitted by the source ObservableSource satisfy the
     *         predicate; otherwise, {@code false}
     * @see <a href="http://reactivex.io/documentation/operators/all.html">ReactiveX operators documentation: All</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> all(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableAllSingle<>(this, predicate));
    }

    /**
     * Mirrors the ObservableSource (current or provided) that first either emits an item or sends a termination
     * notification.
     * <p>
     * <img width="640" height="385" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ambWith.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ambWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an ObservableSource competing to react first. A subscription to this provided source will occur after
     *            subscribing to the current source.
     * @return an Observable that emits the same sequence as whichever of the source ObservableSources first
     *         emitted an item or sent a termination notification
     * @see <a href="http://reactivex.io/documentation/operators/amb.html">ReactiveX operators documentation: Amb</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> ambWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return ambArray(this, other);
    }

    /**
     * Returns a Single that emits {@code true} if any item emitted by the source ObservableSource satisfies a
     * specified condition, otherwise {@code false}. <em>Note:</em> this always emits {@code false} if the
     * source ObservableSource is empty.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/any.2.png" alt="">
     * <p>
     * In Rx.Net this is the {@code any} Observer but we renamed it in RxJava to better match Java naming
     * idioms.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code any} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the condition to test items emitted by the source ObservableSource
     * @return a Single that emits a Boolean that indicates whether any item emitted by the source
     *         ObservableSource satisfies the {@code predicate}
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> any(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableAnySingle<>(this, predicate));
    }

    /**
     * Returns the first item emitted by this {@code Observable}, or throws
     * {@code NoSuchElementException} if it emits no items.
     * <p>
     * <img width="640" height="412" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingFirst.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingFirst} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the first item emitted by this {@code Observable}
     * @throws NoSuchElementException
     *             if this {@code Observable} emits no items
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingFirst() {
        BlockingFirstObserver<T> observer = new BlockingFirstObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the first item emitted by this {@code Observable}, or a default value if it emits no
     * items.
     * <p>
     * <img width="640" height="329" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingFirst.o.default.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingFirst} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if this {@code Observable} emits no items
     * @return the first item emitted by this {@code Observable}, or the default value if it emits no
     *         items
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingFirst(@NonNull T defaultItem) {
        BlockingFirstObserver<T> observer = new BlockingFirstObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        return v != null ? v : defaultItem;
    }

    /**
     * Consumes the upstream {@code Observable} in a blocking fashion and invokes the given
     * {@code Consumer} with each upstream item on the <em>current thread</em> until the
     * upstream terminates.
     * <p>
     * <img width="640" height="330" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingForEach.o.png" alt="">
     * <p>
     * <em>Note:</em> the method will only return if the upstream terminates or the current
     * thread is interrupted.
     * <p>
     * This method executes the {@code Consumer} on the current thread while
     * {@link #subscribe(Consumer)} executes the consumer on the original caller thread of the
     * sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingForEach} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@link Consumer} to invoke for each item emitted by the {@code Observable}
     * @throws RuntimeException
     *             if an error occurs
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX documentation: Subscribe</a>
     * @see #subscribe(Consumer)
     * @see #blockingForEach(Consumer, int)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void blockingForEach(@NonNull Consumer<? super T> onNext) {
        blockingForEach(onNext, bufferSize());
    }

    /**
     * Consumes the upstream {@code Observable} in a blocking fashion and invokes the given
     * {@code Consumer} with each upstream item on the <em>current thread</em> until the
     * upstream terminates.
     * <p>
     * <img width="640" height="330" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingForEach.o.png" alt="">
     * <p>
     * <em>Note:</em> the method will only return if the upstream terminates or the current
     * thread is interrupted.
     * <p>
     * This method executes the {@code Consumer} on the current thread while
     * {@link #subscribe(Consumer)} executes the consumer on the original caller thread of the
     * sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingForEach} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param onNext
     *            the {@link Consumer} to invoke for each item emitted by the {@code Observable}
     * @param capacityHint
     *            the number of items expected to be buffered (allows reducing buffer reallocations)
     * @throws RuntimeException
     *             if an error occurs; {@link Error}s and {@link RuntimeException}s are rethrown
     *             as they are, checked {@link Exception}s are wrapped into {@code RuntimeException}s
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX documentation: Subscribe</a>
     * @see #subscribe(Consumer)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void blockingForEach(@NonNull Consumer<? super T> onNext, int capacityHint) {
        Iterator<T> it = blockingIterable(capacityHint).iterator();
        while (it.hasNext()) {
            try {
                onNext.accept(it.next());
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                ((Disposable)it).dispose();
                throw ExceptionHelper.wrapOrThrow(e);
            }
        }
    }

    /**
     * Converts this {@code Observable} into an {@link Iterable}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingIterable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an {@link Iterable} version of this {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingIterable() {
        return blockingIterable(bufferSize());
    }

    /**
     * Converts this {@code Observable} into an {@link Iterable}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingIterable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint the expected number of items to be buffered
     * @return an {@link Iterable} version of this {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingIterable(int capacityHint) {
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return new BlockingObservableIterable<>(this, capacityHint);
    }

    /**
     * Returns the last item emitted by this {@code Observable}, or throws
     * {@code NoSuchElementException} if this {@code Observable} emits no items.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLast.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLast} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @return the last item emitted by this {@code Observable}
     * @throws NoSuchElementException
     *             if this {@code Observable} emits no items
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingLast() {
        BlockingLastObserver<T> observer = new BlockingLastObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the last item emitted by this {@code Observable}, or a default value if it emits no
     * items.
     * <p>
     * <img width="640" height="310" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLastDefault.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLast} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if this {@code Observable} emits no items
     * @return the last item emitted by the {@code Observable}, or the default value if it emits no
     *         items
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingLast(@NonNull T defaultItem) {
        BlockingLastObserver<T> observer = new BlockingLastObserver<>();
        subscribe(observer);
        T v = observer.blockingGet();
        return v != null ? v : defaultItem;
    }

    /**
     * Returns an {@link Iterable} that returns the latest item emitted by this {@code Observable},
     * waiting if necessary for one to become available.
     * <p>
     * <img width="640" height="350" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingLatest.o.png" alt="">
     * <p>
     * If this {@code Observable} produces items faster than {@code Iterator.next} takes them,
     * {@code onNext} events might be skipped, but {@code onError} or {@code onComplete} events are not.
     * <p>
     * Note also that an {@code onNext} directly followed by {@code onComplete} might hide the {@code onNext}
     * event.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingLatest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Iterable that always returns the latest item emitted by this {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingLatest() {
        return new BlockingObservableLatest<>(this);
    }

    /**
     * Returns an {@link Iterable} that always returns the item most recently emitted by this
     * {@code Observable}.
     * <p>
     * <img width="640" height="426" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingMostRecent.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingMostRecent} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param initialValue
     *            the initial value that the {@link Iterable} sequence will yield if this
     *            {@code Observable} has not yet emitted an item
     * @return an {@link Iterable} that on each iteration returns the item that this {@code Observable}
     *         has most recently emitted
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingMostRecent(@NonNull T initialValue) {
        return new BlockingObservableMostRecent<>(this, initialValue);
    }

    /**
     * Returns an {@link Iterable} that blocks until this {@code Observable} emits another item, then
     * returns that item.
     * <p>
     * <img width="640" height="427" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingNext.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an {@link Iterable} that blocks upon each iteration until this {@code Observable} emits
     *         a new item, whereupon the Iterable returns that item
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Iterable<T> blockingNext() {
        return new BlockingObservableNext<>(this);
    }

    /**
     * If this {@code Observable} completes after emitting a single item, return that item, otherwise
     * throw a {@code NoSuchElementException}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingSingle.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @return the single item emitted by this {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingSingle() {
        T v = singleElement().blockingGet();
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    /**
     * If this {@code Observable} completes after emitting a single item, return that item; if it emits
     * more than one item, throw an {@code IllegalArgumentException}; if it emits no items, return a default
     * value.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/blockingSingleDefault.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If the source signals an error, the operator wraps a checked {@link Exception}
     *  into {@link RuntimeException} and throws that. Otherwise, {@code RuntimeException}s and
     *  {@link Error}s are rethrown as they are.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to return if this {@code Observable} emits no items
     * @return the single item emitted by this {@code Observable}, or the default value if it emits no
     *         items
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final T blockingSingle(@NonNull T defaultItem) {
        return single(defaultItem).blockingGet();
    }

    /**
     * Returns a {@link Future} representing the only value emitted by this {@code Observable}.
     * <p>
     * <img width="640" height="312" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/toFuture.o.png" alt="">
     * <p>
     * If the {@link Observable} emits more than one item, {@link java.util.concurrent.Future} will receive an
     * {@link java.lang.IndexOutOfBoundsException}. If the {@link Observable} is empty, {@link java.util.concurrent.Future}
     * will receive an {@link java.util.NoSuchElementException}. The {@code Observable} source has to terminate in order
     * for the returned {@code Future} to terminate as well.
     * <p>
     * If the {@code Observable} may emit more than one item, use {@code Observable.toList().toFuture()}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFuture} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Future} that expects a single item to be emitted by this {@code Observable}
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Future<T> toFuture() {
        return subscribeWith(new FutureObserver<>());
    }

    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     * <p>
     * <img width="640" height="270" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.0.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @since 2.0
     * @see #blockingSubscribe(Consumer)
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe() {
        ObservableBlockingSubscribe.subscribe(this);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.1.png" alt="">
     * <p>
     * If the {@code Observable} emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * Using the overloads {@link #blockingSubscribe(Consumer, Consumer)}
     * or {@link #blockingSubscribe(Consumer, Consumer, Action)} instead is recommended.
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @since 2.0
     * @see #blockingSubscribe(Consumer, Consumer)
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext) {
        ObservableBlockingSubscribe.subscribe(this, onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="396" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.2.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @since 2.0
     * @see #blockingSubscribe(Consumer, Consumer, Action)
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        ObservableBlockingSubscribe.subscribe(this, onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the source and calls the given callbacks <strong>on the current thread</strong>.
     * <p>
     * <img width="640" height="394" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingSubscribe.o.png" alt="">
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally or with an error. Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @since 2.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError, @NonNull Action onComplete) {
        ObservableBlockingSubscribe.subscribe(this, onNext, onError, onComplete);
    }

    /**
     * Subscribes to the source and calls the {@link Observer} methods <strong>on the current thread</strong>.
     * <p>
     * Note that calling this method will block the caller thread until the upstream terminates
     * normally, with an error or the {@code Observer} disposes the {@link Disposable} it receives via
     * {@link Observer#onSubscribe(Disposable)}.
     * Therefore, calling this method from special threads such as the
     * Android Main Thread or the Swing Event Dispatch Thread is not recommended.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * The a dispose() call is composed through.
     * @param observer the {@code Observer} instance to forward events and calls to in the current thread
     * @since 2.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public final void blockingSubscribe(@NonNull Observer<? super T> observer) {
        ObservableBlockingSubscribe.subscribe(this, observer);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each containing {@code count} items. When the source
     * ObservableSource completes, the resulting ObservableSource emits the current buffer and propagates the notification
     * from the source ObservableSource. Note that if the source ObservableSource issues an onError notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items in each buffer before it should be emitted
     * @return an Observable that emits connected, non-overlapping buffers, each containing at most
     *         {@code count} items from the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(int count) {
        return buffer(count, count);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits buffers every {@code skip} items, each containing {@code count} items. When the source
     * ObservableSource completes, the resulting ObservableSource emits the current buffer and propagates the notification
     * from the source ObservableSource. Note that if the source ObservableSource issues an onError notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer4.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each buffer before it should be emitted
     * @param skip
     *            how many items emitted by the source ObservableSource should be skipped before starting a new
     *            buffer. Note that when {@code skip} and {@code count} are equal, this is the same operation as
     *            {@link #buffer(int)}.
     * @return an Observable that emits buffers for every {@code skip} item from the source ObservableSource and
     *         containing at most {@code count} items
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(int count, int skip) {
        return buffer(count, skip, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits buffers every {@code skip} items, each containing {@code count} items. When the source
     * ObservableSource completes, the resulting ObservableSource emits the current buffer and propagates the notification
     * from the source ObservableSource. Note that if the source ObservableSource issues an onError notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer4.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param count
     *            the maximum size of each buffer before it should be emitted
     * @param skip
     *            how many items emitted by the source ObservableSource should be skipped before starting a new
     *            buffer. Note that when {@code skip} and {@code count} are equal, this is the same operation as
     *            {@link #buffer(int)}.
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return an Observable that emits buffers for every {@code skip} item from the source ObservableSource and
     *         containing at most {@code count} items
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U extends Collection<? super T>> Observable<U> buffer(int count, int skip, @NonNull Supplier<U> bufferSupplier) {
        ObjectHelper.verifyPositive(count, "count");
        ObjectHelper.verifyPositive(skip, "skip");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBuffer<>(this, count, skip, bufferSupplier));
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each containing {@code count} items. When the source
     * ObservableSource completes, the resulting ObservableSource emits the current buffer and propagates the notification
     * from the source ObservableSource. Note that if the source ObservableSource issues an onError notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param count
     *            the maximum number of items in each buffer before it should be emitted
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return an Observable that emits connected, non-overlapping buffers, each containing at most
     *         {@code count} items from the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(int count, @NonNull Supplier<U> bufferSupplier) {
        return buffer(count, count, bufferSupplier);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new buffer periodically, as determined by the {@code timeskip} argument. It emits
     * each buffer after a fixed timespan, specified by the {@code timespan} argument. When the source
     * ObservableSource completes, the resulting ObservableSource emits the current buffer and propagates the notification
     * from the source ObservableSource. Note that if the source ObservableSource issues an onError notification
     * the event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @return an Observable that emits new buffers of items emitted by the source ObservableSource periodically after
     *         a fixed timespan has elapsed
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, long timeskip, @NonNull TimeUnit unit) {
        return buffer(timespan, timeskip, unit, Schedulers.computation(), ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new buffer periodically, as determined by the {@code timeskip} argument, and on the
     * specified {@code scheduler}. It emits each buffer after a fixed timespan, specified by the
     * {@code timespan} argument. When the source ObservableSource completes, the resulting ObservableSource emits the
     * current buffer and propagates the notification from the source ObservableSource. Note that if the source
     * ObservableSource issues an onError notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a buffer
     * @return an Observable that emits new buffers of items emitted by the source ObservableSource periodically after
     *         a fixed timespan has elapsed
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return buffer(timespan, timeskip, unit, scheduler, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new buffer periodically, as determined by the {@code timeskip} argument, and on the
     * specified {@code scheduler}. It emits each buffer after a fixed timespan, specified by the
     * {@code timespan} argument. When the source ObservableSource completes, the resulting ObservableSource emits the
     * current buffer and propagates the notification from the source ObservableSource. Note that if the source
     * ObservableSource issues an onError notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer7.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param timespan
     *            the period of time each buffer collects items before it is emitted
     * @param timeskip
     *            the period of time after which a new buffer will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a buffer
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return an Observable that emits new buffers of items emitted by the source ObservableSource periodically after
     *         a fixed timespan has elapsed
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferTimed<>(this, timespan, timeskip, unit, scheduler, bufferSupplier, Integer.MAX_VALUE, false));
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument. When the source ObservableSource completes, the resulting ObservableSource emits the
     * current buffer and propagates the notification from the source ObservableSource. Note that if the source
     * ObservableSource issues an onError notification the event is passed on immediately without first emitting the
     * buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer5.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @return an Observable that emits connected, non-overlapping buffers of items emitted by the source
     *         ObservableSource within a fixed duration
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit) {
        return buffer(timespan, unit, Schedulers.computation(), Integer.MAX_VALUE);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the source ObservableSource completes, the resulting ObservableSource emits the current buffer and
     * propagates the notification from the source ObservableSource. Note that if the source ObservableSource issues an
     * onError notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @return an Observable that emits connected, non-overlapping buffers of items emitted by the source
     *         ObservableSource, after a fixed duration or when the buffer reaches maximum capacity (whichever occurs
     *         first)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, int count) {
        return buffer(timespan, unit, Schedulers.computation(), count);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument as measured on the specified {@code scheduler}, or a maximum size specified by
     * the {@code count} argument (whichever is reached first). When the source ObservableSource completes, the resulting
     * ObservableSource emits the current buffer and propagates the notification from the source ObservableSource. Note
     * that if the source ObservableSource issues an onError notification the event is passed on immediately without
     * first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a buffer
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @return an Observable that emits connected, non-overlapping buffers of items emitted by the source
     *         ObservableSource after a fixed duration or when the buffer reaches maximum capacity (whichever occurs
     *         first)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, int count) {
        return buffer(timespan, unit, scheduler, count, ArrayListSupplier.asSupplier(), false);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument as measured on the specified {@code scheduler}, or a maximum size specified by
     * the {@code count} argument (whichever is reached first). When the source ObservableSource completes, the resulting
     * ObservableSource emits the current buffer and propagates the notification from the source ObservableSource. Note
     * that if the source ObservableSource issues an onError notification the event is passed on immediately without
     * first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer6.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a buffer
     * @param count
     *            the maximum size of each buffer before it is emitted
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @param restartTimerOnMaxSize if true the time window is restarted when the max capacity of the current buffer
     *            is reached
     * @return an Observable that emits connected, non-overlapping buffers of items emitted by the source
     *         ObservableSource after a fixed duration or when the buffer reaches maximum capacity (whichever occurs
     *         first)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Observable<U> buffer(
            long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, int count,
            @NonNull Supplier<U> bufferSupplier,
            boolean restartTimerOnMaxSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        ObjectHelper.verifyPositive(count, "count");
        return RxJavaPlugins.onAssembly(new ObservableBufferTimed<>(this, timespan, timespan, unit, scheduler, bufferSupplier, count, restartTimerOnMaxSize));
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping buffers, each of a fixed duration specified by the
     * {@code timespan} argument and on the specified {@code scheduler}. When the source ObservableSource completes,
     * the resulting ObservableSource emits the current buffer and propagates the notification from the source
     * ObservableSource. Note that if the source ObservableSource issues an onError notification the event is passed on
     * immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer5.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each buffer collects items before it is emitted and replaced with a new
     *            buffer
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a buffer
     * @return an Observable that emits connected, non-overlapping buffers of items emitted by the source
     *         ObservableSource within a fixed duration
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<@NonNull List<T>> buffer(long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return buffer(timespan, unit, scheduler, Integer.MAX_VALUE, ArrayListSupplier.asSupplier(), false);
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits buffers that it creates when the specified {@code openingIndicator} ObservableSource emits an
     * item, and closes when the ObservableSource returned from {@code closingIndicator} emits an item. If any of the
     * source ObservableSource, {@code openingIndicator} or {@code closingIndicator} issues an onError notification the
     * event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="470" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TOpening> the element type of the buffer-opening ObservableSource
     * @param <TClosing> the element type of the individual buffer-closing ObservableSources
     * @param openingIndicator
     *            the ObservableSource that, when it emits an item, causes a new buffer to be created
     * @param closingIndicator
     *            the {@link Function} that is used to produce an ObservableSource for every buffer created. When this
     *            ObservableSource emits an item, the associated buffer is emitted.
     * @return an Observable that emits buffers, containing items from the source ObservableSource, that are created
     *         and closed when the specified ObservableSources emit items
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <TOpening, TClosing> Observable<@NonNull List<T>> buffer(
            @NonNull ObservableSource<? extends TOpening> openingIndicator,
            @NonNull Function<? super TOpening, ? extends ObservableSource<? extends TClosing>> closingIndicator) {
        return buffer(openingIndicator, closingIndicator, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an Observable that emits buffers of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits buffers that it creates when the specified {@code openingIndicator} ObservableSource emits an
     * item, and closes when the ObservableSource returned from {@code closingIndicator} emits an item. If any of the
     * source ObservableSource, {@code openingIndicator} or {@code closingIndicator} issues an onError notification the
     * event is passed on immediately without first emitting the buffer it is in the process of assembling.
     * <p>
     * <img width="640" height="470" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param <TOpening> the element type of the buffer-opening ObservableSource
     * @param <TClosing> the element type of the individual buffer-closing ObservableSources
     * @param openingIndicator
     *            the ObservableSource that, when it emits an item, causes a new buffer to be created
     * @param closingIndicator
     *            the {@link Function} that is used to produce an ObservableSource for every buffer created. When this
     *            ObservableSource emits an item, the associated buffer is emitted.
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return an Observable that emits buffers, containing items from the source ObservableSource, that are created
     *         and closed when the specified ObservableSources emit items
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <TOpening, TClosing, @NonNull U extends Collection<? super T>> Observable<U> buffer(
            @NonNull ObservableSource<? extends TOpening> openingIndicator,
            @NonNull Function<? super TOpening, ? extends ObservableSource<? extends TClosing>> closingIndicator,
            @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(openingIndicator, "openingIndicator is null");
        Objects.requireNonNull(closingIndicator, "closingIndicator is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferBoundary<T, U, TOpening, TClosing>(this, openingIndicator, closingIndicator, bufferSupplier));
    }

    /**
     * Returns an Observable that emits non-overlapping buffered items from the source ObservableSource each time the
     * specified boundary ObservableSource emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.png" alt="">
     * <p>
     * Completion of either the source or the boundary ObservableSource causes the returned ObservableSource to emit the
     * latest buffer and complete. If either the source ObservableSource or the boundary ObservableSource issues an
     * onError notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundary
     *            the boundary ObservableSource
     * @return an Observable that emits buffered items from the source ObservableSource when the boundary ObservableSource
     *         emits an item
     * @see #buffer(ObservableSource, int)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <B> Observable<@NonNull List<T>> buffer(@NonNull ObservableSource<B> boundary) {
        return buffer(boundary, ArrayListSupplier.asSupplier());
    }

    /**
     * Returns an Observable that emits non-overlapping buffered items from the source ObservableSource each time the
     * specified boundary ObservableSource emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.png" alt="">
     * <p>
     * Completion of either the source or the boundary ObservableSource causes the returned ObservableSource to emit the
     * latest buffer and complete. If either the source ObservableSource or the boundary ObservableSource issues an
     * onError notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundary
     *            the boundary ObservableSource
     * @param initialCapacity
     *            the initial capacity of each buffer chunk
     * @return an Observable that emits buffered items from the source ObservableSource when the boundary ObservableSource
     *         emits an item
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     * @see #buffer(ObservableSource)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <B> Observable<@NonNull List<T>> buffer(@NonNull ObservableSource<B> boundary, int initialCapacity) {
        ObjectHelper.verifyPositive(initialCapacity, "initialCapacity");
        return buffer(boundary, Functions.createArrayList(initialCapacity));
    }

    /**
     * Returns an Observable that emits non-overlapping buffered items from the source ObservableSource each time the
     * specified boundary ObservableSource emits an item.
     * <p>
     * <img width="640" height="395" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/buffer8.png" alt="">
     * <p>
     * Completion of either the source or the boundary ObservableSource causes the returned ObservableSource to emit the
     * latest buffer and complete. If either the source ObservableSource or the boundary ObservableSource issues an
     * onError notification the event is passed on immediately without first emitting the buffer it is in the process of
     * assembling.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code buffer} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the collection subclass type to buffer into
     * @param <B>
     *            the boundary value type (ignored)
     * @param boundary
     *            the boundary ObservableSource
     * @param bufferSupplier
     *            a factory function that returns an instance of the collection subclass to be used and returned
     *            as the buffer
     * @return an Observable that emits buffered items from the source ObservableSource when the boundary ObservableSource
     *         emits an item
     * @see #buffer(ObservableSource, int)
     * @see <a href="http://reactivex.io/documentation/operators/buffer.html">ReactiveX operators documentation: Buffer</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <B, @NonNull U extends Collection<? super T>> Observable<U> buffer(@NonNull ObservableSource<B> boundary, @NonNull Supplier<U> bufferSupplier) {
        Objects.requireNonNull(boundary, "boundary is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableBufferExactBoundary<>(this, boundary, bufferSupplier));
    }

    /**
     * Returns an Observable that subscribes to this ObservableSource lazily, caches all of its events
     * and replays them, in the same order as received, to all the downstream subscribers.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cache.png" alt="">
     * <p>
     * This is useful when you want an ObservableSource to cache responses and you can't control the
     * subscribe/dispose behavior of all the {@link Observer}s.
     * <p>
     * The operator subscribes only when the first downstream subscriber subscribes and maintains
     * a single subscription towards this ObservableSource. In contrast, the operator family of {@link #replay()}
     * that return a {@link ConnectableObservable} require an explicit call to {@link ConnectableObservable#connect()}.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}
     * Observer so be careful not to use this Observer on ObservableSources that emit an infinite or very large number
     * of items that will use up memory.
     * A possible workaround is to apply `takeUntil` with a predicate or
     * another source before (and perhaps after) the application of cache().
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .subscribe(...);
     * </code></pre>
     * Since the operator doesn't allow clearing the cached values either, the possible workaround is
     * to forget all references to it via {@link #onTerminateDetach()} applied along with the previous
     * workaround:
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .subscribe(...);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cache} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that, when first subscribed to, caches all of its items and notifications for the
     *         benefit of subsequent subscribers
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> cache() {
        return cacheWithInitialCapacity(16);
    }

    /**
     * Returns an Observable that subscribes to this ObservableSource lazily, caches all of its events
     * and replays them, in the same order as received, to all the downstream subscribers.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cacheWithInitialCapacity.o.png" alt="">
     * <p>
     * This is useful when you want an ObservableSource to cache responses and you can't control the
     * subscribe/dispose behavior of all the {@link Observer}s.
     * <p>
     * The operator subscribes only when the first downstream subscriber subscribes and maintains
     * a single subscription towards this ObservableSource. In contrast, the operator family of {@link #replay()}
     * that return a {@link ConnectableObservable} require an explicit call to {@link ConnectableObservable#connect()}.
     * <p>
     * <em>Note:</em> You sacrifice the ability to dispose the origin when you use the {@code cache}
     * Observer so be careful not to use this Observer on ObservableSources that emit an infinite or very large number
     * of items that will use up memory.
     * A possible workaround is to apply `takeUntil` with a predicate or
     * another source before (and perhaps after) the application of cache().
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .subscribe(...);
     * </code></pre>
     * Since the operator doesn't allow clearing the cached values either, the possible workaround is
     * to forget all references to it via {@link #onTerminateDetach()} applied along with the previous
     * workaround:
     * <pre><code>
     * AtomicBoolean shouldStop = new AtomicBoolean();
     *
     * source.takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .cache()
     *       .takeUntil(v -&gt; shouldStop.get())
     *       .onTerminateDetach()
     *       .subscribe(...);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cacheWithInitialCapacity} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>
     * <em>Note:</em> The capacity hint is not an upper bound on cache size. For that, consider
     * {@link #replay(int)} in combination with {@link ConnectableObservable#autoConnect()} or similar.
     *
     * @param initialCapacity hint for number of items to cache (for optimizing underlying data structure)
     * @return an Observable that, when first subscribed to, caches all of its items and notifications for the
     *         benefit of subsequent subscribers
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> cacheWithInitialCapacity(int initialCapacity) {
        ObjectHelper.verifyPositive(initialCapacity, "initialCapacity");
        return RxJavaPlugins.onAssembly(new ObservableCache<>(this, initialCapacity));
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource, converted to the specified
     * type.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/cast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code cast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output value type cast to
     * @param clazz
     *            the target class type that {@code cast} will cast the items emitted by the source ObservableSource
     *            into before emitting them from the resulting ObservableSource
     * @return an Observable that emits each item from the source ObservableSource after converting it to the
     *         specified type
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<U> cast(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(Functions.castFunction(clazz));
    }

    /**
     * Collects items emitted by the finite source ObservableSource into a single mutable data structure and returns
     * a Single that emits this structure.
     * <p>
     * <img width="640" height="330" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collect.2.png" alt="">
     * <p>
     * This is a simplified version of {@code reduce} that does not need to return the state on each pass.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the accumulator and output type
     * @param initialValueSupplier
     *           the mutable data structure that will collect the items
     * @param collector
     *           a function that accepts the {@code state} and an emitted item, and modifies {@code state}
     *           accordingly
     * @return a Single that emits the result of collecting the values emitted by the source ObservableSource
     *         into a single mutable data structure
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Single<U> collect(@NonNull Supplier<? extends U> initialValueSupplier, @NonNull BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialValueSupplier, "initialValueSupplier is null");
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ObservableCollectSingle<>(this, initialValueSupplier, collector));
    }

    /**
     * Collects items emitted by the finite source ObservableSource into a single mutable data structure and returns
     * a Single that emits this structure.
     * <p>
     * <img width="640" height="330" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collectInto.o.png" alt="">
     * <p>
     * This is a simplified version of {@code reduce} that does not need to return the state on each pass.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collectInto} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the accumulator and output type
     * @param initialValue
     *           the mutable data structure that will collect the items
     * @param collector
     *           a function that accepts the {@code state} and an emitted item, and modifies {@code state}
     *           accordingly
     * @return a Single that emits the result of collecting the values emitted by the source ObservableSource
     *         into a single mutable data structure
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Single<U> collectInto(@NonNull U initialValue, @NonNull BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialValue, "initialValue is null");
        return collect(Functions.justSupplier(initialValue), collector);
    }

    /**
     * Transform an ObservableSource by applying a particular Transformer function to it.
     * <p>
     * This method operates on the ObservableSource itself whereas {@link #lift} operates on the ObservableSource's
     * Observers.
     * <p>
     * If the operator you are creating is designed to act on the individual items emitted by a source
     * ObservableSource, use {@link #lift}. If your operator is designed to transform the source ObservableSource as a whole
     * (for instance, by applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the output ObservableSource
     * @param composer implements the function that transforms the source ObservableSource
     * @return the source ObservableSource, transformed by the transformer function
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> compose(@NonNull ObservableTransformer<? super T, ? extends R> composer) {
        return wrap(((ObservableTransformer<T, R>) Objects.requireNonNull(composer, "composer is null")).apply(this));
    }

    /**
     * Returns a new Observable that emits items resulting from applying a function that you supply to each item
     * emitted by the source ObservableSource, where that function returns an ObservableSource, and then emitting the items
     * that result from concatenating those resulting ObservableSources.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMap(Function, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner ObservableSource sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and concatenating the ObservableSources obtained from this transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #concatMap(Function, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }

    /**
     * Returns a new Observable that emits items resulting from applying a function that you supply to each item
     * emitted by the source ObservableSource, where that function returns an ObservableSource, and then emitting the items
     * that result from concatenating those resulting ObservableSources.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMap(Function, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner ObservableSource sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param prefetch
     *            the number of elements to prefetch from the current Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and concatenating the ObservableSources obtained from this transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #concatMap(Function, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap<>(this, mapper, prefetch, ErrorMode.IMMEDIATE));
    }

    /**
     * Returns a new Observable that emits items resulting from applying a function that you supply to each item
     * emitted by the source ObservableSource, where that function returns an ObservableSource, and then emitting the items
     * that result from concatenating those resulting ObservableSources.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <p>
     * The difference between {@link #concatMap(Function, int)} and this operator is that this operator guarantees the {@code mapper}
     * function is executed on the specified scheduler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMap} executes the given {@code mapper} function on the provided {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the type of the inner ObservableSource sources and thus the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param prefetch
     *            the number of elements to prefetch from the current Observable
     * @param scheduler
     *            the scheduler where the {@code mapper} function will be executed
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and concatenating the ObservableSources obtained from this transformation
     * @since 3.0.0
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> concatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int prefetch, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapScheduler<>(this, mapper, prefetch, ErrorMode.IMMEDIATE, scheduler));
    }

    /**
     * Maps each of the items into an ObservableSource, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner ObservableSources
     * till all of them terminate.
     * <p>
     * <img width="640" height="347" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMapDelayError(Function, boolean, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of this ObservableSource into the inner ObservableSources.
     * @return the new ObservableSource instance with the concatenation behavior
     * @see #concatMapDelayError(Function, boolean, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMapDelayError(mapper, true, bufferSize());
    }

    /**
     * Maps each of the items into an ObservableSource, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner ObservableSources
     * till all of them terminate.
     * <p>
     * <img width="640" height="347" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <p>
     * Note that there is no guarantee where the given {@code mapper} function will be executed; it could be on the subscribing thread,
     * on the upstream thread signaling the new item to be mapped or on the thread where the inner source terminates. To ensure
     * the {@code mapper} function is confined to a known thread, use the {@link #concatMapDelayError(Function, boolean, int, Scheduler)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of this ObservableSource into the inner ObservableSources.
     * @param tillTheEnd
     *            if true, all errors from the outer and inner ObservableSource sources are delayed until the end,
     *            if false, an error from the main source is signalled when the current ObservableSource source terminates
     * @param prefetch
     *            the number of elements to prefetch from the current Observable
     * @return the new ObservableSource instance with the concatenation behavior
     * @see #concatMapDelayError(Function, boolean, int, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableConcatMap<>(this, mapper, prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }

    /**
     * Maps each of the items into an ObservableSource, subscribes to them one after the other,
     * one at a time and emits their values in order
     * while delaying any error from either this or any of the inner ObservableSources
     * till all of them terminate.
     * <p>
     * <img width="640" height="347" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param mapper the function that maps the items of this ObservableSource into the inner ObservableSources.
     * @param tillTheEnd
     *            if true, all errors from the outer and inner ObservableSource sources are delayed until the end,
     *            if false, an error from the main source is signalled when the current ObservableSource source terminates
     * @param prefetch
     *            the number of elements to prefetch from the current Observable
     * @param scheduler
     *            the scheduler where the {@code mapper} function will be executed
     * @return the new ObservableSource instance with the concatenation behavior
     * @see #concatMapDelayError(Function, boolean, int)
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> concatMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int prefetch, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapScheduler<>(this, mapper, prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, scheduler));
    }

    /**
     * Maps a sequence of values into ObservableSources and concatenates these ObservableSources eagerly into a single
     * ObservableSource.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEager.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of ObservableSources that will be
     *               eagerly concatenated
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapEager(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return concatMapEager(mapper, Integer.MAX_VALUE, bufferSize());
    }

    /**
     * Maps a sequence of values into ObservableSources and concatenates these ObservableSources eagerly into a single
     * ObservableSource.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEager.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of ObservableSources that will be
     *               eagerly concatenated
     * @param maxConcurrency the maximum number of concurrent subscribed ObservableSources
     * @param prefetch hints about the number of expected values from each inner ObservableSource, must be positive
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapEager(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            int maxConcurrency, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapEager<>(this, mapper, ErrorMode.IMMEDIATE, maxConcurrency, prefetch));
    }

    /**
     * Maps a sequence of values into ObservableSources and concatenates these ObservableSources eagerly into a single
     * ObservableSource.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEagerDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of ObservableSources that will be
     *               eagerly concatenated
     * @param tillTheEnd
     *            if true, all errors from the outer and inner ObservableSource sources are delayed until the end,
     *            if false, an error from the main source is signalled when the current ObservableSource source terminates
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapEagerDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd) {
        return concatMapEagerDelayError(mapper, tillTheEnd, Integer.MAX_VALUE, bufferSize());
    }

    /**
     * Maps a sequence of values into ObservableSources and concatenates these ObservableSources eagerly into a single
     * ObservableSource.
     * <p>
     * Eager concatenation means that once a subscriber subscribes, this operator subscribes to all of the
     * source ObservableSources. The operator buffers the values emitted by these ObservableSources and then drains them in
     * order, each one after the previous one completes.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapEagerDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the value type
     * @param mapper the function that maps a sequence of values into a sequence of ObservableSources that will be
     *               eagerly concatenated
     * @param tillTheEnd
     *               if true, exceptions from the current Observable and all the inner ObservableSources are delayed until
     *               all of them terminate, if false, exception from the current Observable is delayed until the
     *               currently running ObservableSource terminates
     * @param maxConcurrency the maximum number of concurrent subscribed ObservableSources
     * @param prefetch
     *               the number of elements to prefetch from each source ObservableSource
     * @return the new ObservableSource instance with the specified concatenation behavior
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapEagerDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean tillTheEnd, int maxConcurrency, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapEager<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, maxConcurrency, prefetch));
    }

    /**
     * Maps each element of the upstream Observable into CompletableSources, subscribes to them one at a time in
     * order and waits until the upstream and all CompletableSources complete.
     * <p>
     * <img width="640" height="505" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.6 - experimental
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns a CompletableSource
     * @return a Completable that signals {@code onComplete} when the upstream and all CompletableSources complete
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return concatMapCompletable(mapper, 2);
    }

    /**
     * Maps each element of the upstream Observable into CompletableSources, subscribes to them one at a time in
     * order and waits until the upstream and all CompletableSources complete.
     * <p>
     * <img width="640" height="505" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.6 - experimental
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns a CompletableSource
     *
     * @param capacityHint
     *            the number of upstream items expected to be buffered until the current CompletableSource,  mapped from
     *            the current item, completes.
     * @return a Completable that signals {@code onComplete} when the upstream and all CompletableSources complete
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper, int capacityHint) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapCompletable<>(this, mapper, ErrorMode.IMMEDIATE, capacityHint));
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, delaying all errors till both this {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @return a new Completable instance
     * @see #concatMapCompletable(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return concatMapCompletableDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, optionally delaying all errors till both this {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code CompletableSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code CompletableSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return a new Completable instance
     * @see #concatMapCompletable(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean tillTheEnd) {
        return concatMapCompletableDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link CompletableSource}s and subscribes to them one after the
     * other terminates, optionally delaying all errors till both this {@code Observable} and all
     * inner {@code CompletableSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with the upstream item and should return
     *               a {@code CompletableSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code CompletableSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code CompletableSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code CompletableSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code CompletableSource}s.
     * @return a new Completable instance
     * @see #concatMapCompletable(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable concatMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean tillTheEnd, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapCompletable<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, prefetch));
    }

    /**
     * Returns an Observable that concatenate each item emitted by the source ObservableSource with the values in an
     * Iterable corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="275" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapIterable.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source ObservableSource
     * @return an Observable that emits the results of concatenating the items emitted by the source ObservableSource with
     *         the values in the Iterables corresponding to those items, as generated by {@code collectionSelector}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<U> concatMapIterable(@NonNull Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlattenIterable<>(this, mapper));
    }

    /**
     * Returns an Observable that concatenate each item emitted by the source ObservableSource with the values in an
     * Iterable corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="275" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapIterable.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source ObservableSource
     * @param prefetch
     *            the number of elements to prefetch from the current Observable
     * @return an Observable that emits the results of concatenating the items emitted by the source ObservableSource with
     *         the values in the Iterables corresponding to those items, as generated by {@code collectionSelector}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<U> concatMapIterable(@NonNull Function<? super T, ? extends Iterable<? extends U>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return concatMap(ObservableInternalHelper.flatMapIntoIterable(mapper), prefetch);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other succeeds or completes, emits their success value if available or terminates immediately if
     * either this {@code Observable} or the current inner {@code MaybeSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @return a new Observable instance
     * @see #concatMapMaybeDelayError(Function)
     * @see #concatMapMaybe(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return concatMapMaybe(mapper, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other succeeds or completes, emits their success value if available or terminates immediately if
     * either this {@code Observable} or the current inner {@code MaybeSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code MaybeSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code MaybeSource}s.
     * @return a new Observable instance
     * @see #concatMapMaybe(Function)
     * @see #concatMapMaybeDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapMaybe<>(this, mapper, ErrorMode.IMMEDIATE, prefetch));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and delaying all errors
     * till both this {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @return a new Observable instance
     * @see #concatMapMaybe(Function)
     * @see #concatMapMaybeDelayError(Function, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return concatMapMaybeDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and optionally delaying all errors
     * till both this {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code MaybeSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code MaybeSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return a new Observable instance
     * @see #concatMapMaybe(Function, int)
     * @see #concatMapMaybeDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean tillTheEnd) {
        return concatMapMaybeDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and subscribes to them one after the
     * other terminates, emits their success value if available and optionally delaying all errors
     * till both this {@code Observable} and all inner {@code MaybeSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code MaybeSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code MaybeSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code MaybeSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code MaybeSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code MaybeSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code MaybeSource}s.
     * @return a new Observable instance
     * @see #concatMapMaybe(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean tillTheEnd, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapMaybe<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, prefetch));
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds, emits their success values or terminates immediately if
     * either this {@code Observable} or the current inner {@code SingleSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @return a new Observable instance
     * @see #concatMapSingleDelayError(Function)
     * @see #concatMapSingle(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return concatMapSingle(mapper, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds, emits their success values or terminates immediately if
     * either this {@code Observable} or the current inner {@code SingleSource} fail.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code SingleSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code SingleSource}s.
     * @return a new Observable instance
     * @see #concatMapSingle(Function)
     * @see #concatMapSingleDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapSingle<>(this, mapper, ErrorMode.IMMEDIATE, prefetch));
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and delays all errors
     * till both this {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @return a new Observable instance
     * @see #concatMapSingle(Function)
     * @see #concatMapSingleDelayError(Function, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return concatMapSingleDelayError(mapper, true, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and optionally delays all errors
     * till both this {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code SingleSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code SingleSource} terminates and only then is
     *                   it emitted to the downstream.
     * @return a new Observable instance
     * @see #concatMapSingle(Function, int)
     * @see #concatMapSingleDelayError(Function, boolean, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean tillTheEnd) {
        return concatMapSingleDelayError(mapper, tillTheEnd, 2);
    }

    /**
     * Maps the upstream items into {@link SingleSource}s and subscribes to them one after the
     * other succeeds or fails, emits their success values and optionally delays  errors
     * till both this {@code Observable} and all inner {@code SingleSource}s terminate.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the result type of the inner {@code SingleSource}s
     * @param mapper the function called with the upstream item and should return
     *               a {@code SingleSource} to become the next source to
     *               be subscribed to
     * @param tillTheEnd If {@code true}, errors from this {@code Observable} or any of the
     *                   inner {@code SingleSource}s are delayed until all
     *                   of them terminate. If {@code false}, an error from this
     *                   {@code Observable} is delayed until the current inner
     *                   {@code SingleSource} terminates and only then is
     *                   it emitted to the downstream.
     * @param prefetch The number of upstream items to prefetch so that fresh items are
     *                 ready to be mapped when a previous {@code SingleSource} terminates.
     *                 The operator replenishes after half of the prefetch amount has been consumed
     *                 and turned into {@code SingleSource}s.
     * @return a new Observable instance
     * @see #concatMapSingle(Function, int)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> concatMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean tillTheEnd, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ObservableConcatMapSingle<>(this, mapper, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY, prefetch));
    }

    /**
     * Returns an Observable that emits the items emitted from the current ObservableSource, then the next, one after
     * the other, without interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an ObservableSource to be concatenated after the current
     * @return an Observable that emits items emitted by the two source ObservableSources, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns an {@code Observable} that emits the items from this {@code Observable} followed by the success item or error event
     * of the other {@link SingleSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the SingleSource whose signal should be emitted after this {@code Observable} completes normally.
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithSingle<>(this, other));
    }

    /**
     * Returns an {@code Observable} that emits the items from this {@code Observable} followed by the success item or terminal events
     * of the other {@link MaybeSource}.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the MaybeSource whose signal should be emitted after this Observable completes normally.
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithMaybe<>(this, other));
    }

    /**
     * Returns an {@code Observable} that emits items from this {@code Observable} and when it completes normally, the
     * other {@link CompletableSource} is subscribed to and the returned {@code Observable} emits its terminal events.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concat.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code CompletableSource} to subscribe to once the current {@code Observable} completes normally
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> concatWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableConcatWithCompletable<>(this, other));
    }

    /**
     * Returns a Single that emits a Boolean that indicates whether the source ObservableSource emitted a
     * specified item.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/contains.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code contains} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param element
     *            the item to search for in the emissions from the source ObservableSource
     * @return a Single that emits {@code true} if the specified item is emitted by the source ObservableSource,
     *         or {@code false} if the source ObservableSource completes without emitting that item
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> contains(@NonNull Object element) {
        Objects.requireNonNull(element, "element is null");
        return any(Functions.equalsWith(element));
    }

    /**
     * Returns a Single that counts the total number of items emitted by the source ObservableSource and emits
     * this count as a 64-bit Long.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/count.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code count} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits a single item: the number of items emitted by the source ObservableSource as a
     *         64-bit Long item
     * @see <a href="http://reactivex.io/documentation/operators/count.html">ReactiveX operators documentation: Count</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Long> count() {
        return RxJavaPlugins.onAssembly(new ObservableCountSingle<>(this));
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, except that it drops items emitted by the
     * source ObservableSource that are followed by another item within a computed debounce duration.
     * <p>
     * <img width="640" height="425" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.f.png" alt="">
     * <p>
     * The delivery of the item happens on the thread of the first {@code onNext} or {@code onComplete}
     * signal of the generated {@code ObservableSource} sequence,
     * which if takes too long, a newer item may arrive from the upstream, causing the
     * generated sequence to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code debounce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the debounce value type (ignored)
     * @param debounceSelector
     *            function to retrieve a sequence that indicates the throttle duration for each item
     * @return an Observable that omits items emitted by the source ObservableSource that are followed by another item
     *         within a computed debounce duration
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> debounce(@NonNull Function<? super T, ? extends ObservableSource<U>> debounceSelector) {
        Objects.requireNonNull(debounceSelector, "debounceSelector is null");
        return RxJavaPlugins.onAssembly(new ObservableDebounce<>(this, debounceSelector));
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, except that it drops items emitted by the
     * source ObservableSource that are followed by newer items before a timeout value expires. The timer resets on
     * each emission.
     * <p>
     * <em>Note:</em> If items keep being emitted by the source ObservableSource faster than the timeout then no items
     * will be emitted by the resulting ObservableSource.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.png" alt="">
     * <p>
     * Delivery of the item after the grace period happens on the {@code computation} {@code Scheduler}'s
     * {@code Worker} which if takes too long, a newer item may arrive from the upstream, causing the
     * {@code Worker}'s task to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code debounce} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the source
     *            ObservableSource in which that ObservableSource emits no items in order for the item to be emitted by the
     *            resulting ObservableSource
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @return an Observable that filters out items from the source ObservableSource that are too quickly followed by
     *         newer items
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #throttleWithTimeout(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> debounce(long timeout, @NonNull TimeUnit unit) {
        return debounce(timeout, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, except that it drops items emitted by the
     * source ObservableSource that are followed by newer items before a timeout value expires on a specified
     * Scheduler. The timer resets on each emission.
     * <p>
     * <em>Note:</em> If items keep being emitted by the source ObservableSource faster than the timeout then no items
     * will be emitted by the resulting ObservableSource.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/debounce.s.png" alt="">
     * <p>
     * Delivery of the item after the grace period happens on the given {@code Scheduler}'s
     * {@code Worker} which if takes too long, a newer item may arrive from the upstream, causing the
     * {@code Worker}'s task to get disposed, which may also interrupt any downstream blocking operation
     * (yielding an {@code InterruptedException}). It is recommended processing items
     * that may take long time to be moved to another thread via {@link #observeOn} applied after
     * {@code debounce} itself.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            the time each item has to be "the most recent" of those emitted by the source ObservableSource to
     *            ensure that it's not dropped
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @param scheduler
     *            the {@link Scheduler} to use internally to manage the timers that handle the timeout for each
     *            item
     * @return an Observable that filters out items from the source ObservableSource that are too quickly followed by
     *         newer items
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #throttleWithTimeout(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> debounce(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableDebounceTimed<>(this, timeout, unit, scheduler));
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource or a specified default item
     * if the source ObservableSource is empty.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/defaultIfEmpty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code defaultIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the item to emit if the source ObservableSource emits no items
     * @return an Observable that emits either the specified default item if the source ObservableSource emits no
     *         items, or the items emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/defaultifempty.html">ReactiveX operators documentation: DefaultIfEmpty</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> defaultIfEmpty(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return switchIfEmpty(just(defaultItem));
    }

    /**
     * Returns an Observable that delays the emissions of the source ObservableSource via another ObservableSource on a
     * per-item basis.
     * <p>
     * <img width="640" height="450" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.o.png" alt="">
     * <p>
     * <em>Note:</em> the resulting ObservableSource will immediately propagate any {@code onError} notification
     * from the source ObservableSource.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the item delay value type (ignored)
     * @param itemDelay
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource, which is
     *            then used to delay the emission of that item by the resulting ObservableSource until the ObservableSource
     *            returned from {@code itemDelay} emits an item
     * @return an Observable that delays the emissions of the source ObservableSource via another ObservableSource on a
     *         per-item basis
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> delay(@NonNull Function<? super T, ? extends ObservableSource<U>> itemDelay) {
        Objects.requireNonNull(itemDelay, "itemDelay is null");
        return flatMap(ObservableInternalHelper.itemDelay(itemDelay));
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource shifted forward in time by a
     * specified delay. Error notifications from the source ObservableSource are not delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return the source ObservableSource shifted in time by the specified delay
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delay(long delay, @NonNull TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation(), false);
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource shifted forward in time by a
     * specified delay. If {@code delayError} is true, error notifications will also be delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param delayError
     *            if true, the upstream exception is signalled with the given delay, after all preceding normal elements,
     *            if false, the upstream exception is signalled immediately
     * @return the source ObservableSource shifted in time by the specified delay
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delay(long delay, @NonNull TimeUnit unit, boolean delayError) {
        return delay(delay, unit, Schedulers.computation(), delayError);
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource shifted forward in time by a
     * specified delay. Error notifications from the source ObservableSource are not delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@link Scheduler} to use for delaying
     * @return the source ObservableSource shifted in time by the specified delay
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delay(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delay(delay, unit, scheduler, false);
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource shifted forward in time by a
     * specified delay. If {@code delayError} is true, error notifications will also be delayed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param delay
     *            the delay to shift the source by
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the {@link Scheduler} to use for delaying
     * @param delayError
     *            if true, the upstream exception is signalled with the given delay, after all preceding normal elements,
     *            if false, the upstream exception is signalled immediately
     * @return the source ObservableSource shifted in time by the specified delay
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delay(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return RxJavaPlugins.onAssembly(new ObservableDelay<>(this, delay, unit, scheduler, delayError));
    }

    /**
     * Returns an Observable that delays the subscription to and emissions from the source ObservableSource via another
     * ObservableSource on a per-item basis.
     * <p>
     * <img width="640" height="450" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delay.oo.png" alt="">
     * <p>
     * <em>Note:</em> the resulting ObservableSource will immediately propagate any {@code onError} notification
     * from the source ObservableSource.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the subscription delay value type (ignored)
     * @param <V>
     *            the item delay value type (ignored)
     * @param subscriptionDelay
     *            a function that returns an ObservableSource that triggers the subscription to the source ObservableSource
     *            once it emits any item
     * @param itemDelay
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource, which is
     *            then used to delay the emission of that item by the resulting ObservableSource until the ObservableSource
     *            returned from {@code itemDelay} emits an item
     * @return an Observable that delays the subscription and emissions of the source ObservableSource via another
     *         ObservableSource on a per-item basis
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<T> delay(@NonNull ObservableSource<U> subscriptionDelay,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemDelay) {
        return delaySubscription(subscriptionDelay).delay(itemDelay);
    }

    /**
     * Returns an Observable that delays the subscription to this Observable
     * until the other Observable emits an element or completes normally.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the value type of the other Observable, irrelevant
     * @param other the other Observable that should trigger the subscription
     *        to this Observable.
     * @return an Observable that delays the subscription to this Observable
     *         until the other Observable emits an element or completes normally.
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> delaySubscription(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableDelaySubscriptionOther<>(this, other));
    }

    /**
     * Returns an Observable that delays the subscription to the source ObservableSource by a given amount of time.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code delaySubscription} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param delay
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @return an Observable that delays the subscription to the source ObservableSource by the given amount
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> delaySubscription(long delay, @NonNull TimeUnit unit) {
        return delaySubscription(delay, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that delays the subscription to the source ObservableSource by a given amount of time,
     * both waiting and subscribing on a given Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/delaySubscription.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param delay
     *            the time to delay the subscription
     * @param unit
     *            the time unit of {@code delay}
     * @param scheduler
     *            the Scheduler on which the waiting and subscription will happen
     * @return an Observable that delays the subscription to the source ObservableSource by a given
     *         amount, waiting and subscribing on the given Scheduler
     * @see <a href="http://reactivex.io/documentation/operators/delay.html">ReactiveX operators documentation: Delay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> delaySubscription(long delay, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }

    /**
     * Returns an Observable that reverses the effect of {@link #materialize materialize} by transforming the
     * {@link Notification} objects extracted from the source items via a selector function
     * into their respective {@code Observer} signal types.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/dematerialize.png" alt="">
     * <p>
     * The intended use of the {@code selector} function is to perform a
     * type-safe identity mapping (see example) on a source that is already of type
     * {@code Notification<T>}. The Java language doesn't allow
     * limiting instance methods to a certain generic argument shape, therefore,
     * a function is used to ensure the conversion remains type safe.
     * <p>
     * When the upstream signals an {@link Notification#createOnError(Throwable) onError} or
     * {@link Notification#createOnComplete() onComplete} item, the
     * returned Observable disposes of the flow and terminates with that type of terminal event:
     * <pre><code>
     * Observable.just(createOnNext(1), createOnComplete(), createOnNext(2))
     * .doOnDispose(() -&gt; System.out.println("Disposed!"));
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1);
     * </code></pre>
     * If the upstream signals {@code onError} or {@code onComplete} directly, the flow is terminated
     * with the same event.
     * <pre><code>
     * Observable.just(createOnNext(1), createOnNext(2))
     * .dematerialize(notification -&gt; notification)
     * .test()
     * .assertResult(1, 2);
     * </code></pre>
     * If this behavior is not desired, the completion can be suppressed by applying {@link #concatWith(ObservableSource)}
     * with a {@link #never()} source.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code dematerialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.2.4 - experimental
     *
     * @param <R> the output value type
     * @param selector function that returns the upstream item and should return a Notification to signal
     * the corresponding {@code Observer} event to the downstream.
     * @return an Observable that emits the items and notifications embedded in the {@link Notification} objects
     *         selected from the items emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/materialize-dematerialize.html">ReactiveX operators documentation: Dematerialize</a>
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> dematerialize(@NonNull Function<? super T, Notification<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return RxJavaPlugins.onAssembly(new ObservableDematerialize<>(this, selector));
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct
     * based on {@link Object#equals(Object)} comparison.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.png" alt="">
     * <p>
     * It is recommended the elements' class {@code T} in the flow overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()} to provide meaningful comparison between items as the default Java
     * implementation only considers reference equivalence.
     * <p>
     * By default, {@code distinct()} uses an internal {@link java.util.HashSet} per Observer to remember
     * previously seen items and uses {@link java.util.Set#add(Object)} returning {@code false} as the
     * indicator for duplicates.
     * <p>
     * Note that this internal {@code HashSet} may grow unbounded as items won't be removed from it by
     * the operator. Therefore, using very long or infinite upstream (with very distinct elements) may lead
     * to {@code OutOfMemoryError}.
     * <p>
     * Customizing the retention policy can happen only by providing a custom {@link java.util.Collection} implementation
     * to the {@link #distinct(Function, Supplier)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits only those items emitted by the source ObservableSource that are distinct from
     *         each other
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinct(Function)
     * @see #distinct(Function, Supplier)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinct() {
        return distinct(Functions.identity(), Functions.createHashSet());
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct according
     * to a key selector function and based on {@link Object#equals(Object)} comparison of the objects
     * returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.key.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()} to provide meaningful comparison between the key objects as the default
     * Java implementation only considers reference equivalence.
     * <p>
     * By default, {@code distinct()} uses an internal {@link java.util.HashSet} per Observer to remember
     * previously seen keys and uses {@link java.util.Set#add(Object)} returning {@code false} as the
     * indicator for duplicates.
     * <p>
     * Note that this internal {@code HashSet} may grow unbounded as keys won't be removed from it by
     * the operator. Therefore, using very long or infinite upstream (with very distinct keys) may lead
     * to {@code OutOfMemoryError}.
     * <p>
     * Customizing the retention policy can happen only by providing a custom {@link java.util.Collection} implementation
     * to the {@link #distinct(Function, Supplier)} overload.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @return an Observable that emits those items emitted by the source ObservableSource that have distinct keys
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinct(Function, Supplier)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Observable<T> distinct(@NonNull Function<? super T, K> keySelector) {
        return distinct(keySelector, Functions.createHashSet());
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct according
     * to a key selector function and based on {@link Object#equals(Object)} comparison of the objects
     * returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinct.key.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()}
     * and {@link Object#hashCode()}  to provide meaningful comparison between the key objects as
     * the default Java implementation only considers reference equivalence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinct} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @param collectionSupplier
     *            function called for each individual Observer to return a Collection subtype for holding the extracted
     *            keys and whose add() method's return indicates uniqueness.
     * @return an Observable that emits those items emitted by the source ObservableSource that have distinct keys
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Observable<T> distinct(@NonNull Function<? super T, K> keySelector, @NonNull Supplier<? extends Collection<? super K>> collectionSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinct<>(this, keySelector, collectionSupplier));
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct from their
     * immediate predecessors based on {@link Object#equals(Object)} comparison.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.png" alt="">
     * <p>
     * It is recommended the elements' class {@code T} in the flow overrides the default {@code Object.equals()} to provide
     * meaningful comparison between items as the default Java implementation only considers reference equivalence.
     * Alternatively, use the {@link #distinctUntilChanged(BiPredicate)} overload and provide a comparison function
     * in case the class {@code T} can't be overridden with custom {@code equals()} or the comparison itself
     * should happen on different terms or properties of the class {@code T}.
     * <p>
     * Note that the operator always retains the latest item from upstream regardless of the comparison result
     * and uses it in the next comparison with the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@code CharSequence}s or {@code List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits those items from the source ObservableSource that are distinct from their
     *         immediate predecessors
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @see #distinctUntilChanged(BiPredicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinctUntilChanged() {
        return distinctUntilChanged(Functions.identity());
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct from their
     * immediate predecessors, according to a key selector function and based on {@link Object#equals(Object)} comparison
     * of those objects returned by the key selector function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.key.png" alt="">
     * <p>
     * It is recommended the keys' class {@code K} overrides the default {@code Object.equals()} to provide
     * meaningful comparison between the key objects as the default Java implementation only considers reference equivalence.
     * Alternatively, use the {@link #distinctUntilChanged(BiPredicate)} overload and provide a comparison function
     * in case the class {@code K} can't be overridden with custom {@code equals()} or the comparison itself
     * should happen on different terms or properties of the item class {@code T} (for which the keys can be
     * derived via a similar selector).
     * <p>
     * Note that the operator always retains the latest key from upstream regardless of the comparison result
     * and uses it in the next comparison with the next key derived from the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@code CharSequence}s or {@code List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type
     * @param keySelector
     *            a function that projects an emitted item to a key value that is used to decide whether an item
     *            is distinct from another one or not
     * @return an Observable that emits those items from the source ObservableSource whose keys are distinct from
     *         those of their immediate predecessors
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Observable<T> distinctUntilChanged(@NonNull Function<? super T, K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinctUntilChanged<>(this, keySelector, ObjectHelper.equalsPredicate()));
    }

    /**
     * Returns an Observable that emits all items emitted by the source ObservableSource that are distinct from their
     * immediate predecessors when compared with each other via the provided comparator function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/distinctUntilChanged.png" alt="">
     * <p>
     * Note that the operator always retains the latest item from upstream regardless of the comparison result
     * and uses it in the next comparison with the next upstream item.
     * <p>
     * Note that if element type {@code T} in the flow is mutable, the comparison of the previous and current
     * item may yield unexpected results if the items are mutated externally. Common cases are mutable
     * {@code CharSequence}s or {@code List}s where the objects will actually have the same
     * references when they are modified and {@code distinctUntilChanged} will evaluate subsequent items as same.
     * To avoid such situation, it is recommended that mutable data is converted to an immutable one,
     * for example using {@code map(CharSequence::toString)} or {@code map(list -> Collections.unmodifiableList(new ArrayList<>(list)))}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code distinctUntilChanged} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparer the function that receives the previous item and the current item and is
     *                   expected to return true if the two are equal, thus skipping the current value.
     * @return an Observable that emits those items from the source ObservableSource that are distinct from their
     *         immediate predecessors
     * @see <a href="http://reactivex.io/documentation/operators/distinct.html">ReactiveX operators documentation: Distinct</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> distinctUntilChanged(@NonNull BiPredicate<? super T, ? super T> comparer) {
        Objects.requireNonNull(comparer, "comparer is null");
        return RxJavaPlugins.onAssembly(new ObservableDistinctUntilChanged<>(this, Functions.identity(), comparer));
    }

    /**
     * Calls the specified consumer with the current item after this item has been emitted to the downstream.
     * <p>Note that the {@code onAfterNext} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doAfterNext.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterNext} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Operator-fusion:</b></dt>
     *  <dd>This operator supports boundary-limited synchronous or asynchronous queue-fusion.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onAfterNext the Consumer that will be called after emitting an item from upstream to the downstream
     * @return the new Observable instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doAfterNext(@NonNull Consumer<? super T> onAfterNext) {
        Objects.requireNonNull(onAfterNext, "onAfterNext is null");
        return RxJavaPlugins.onAssembly(new ObservableDoAfterNext<>(this, onAfterNext));
    }

    /**
     * Registers an {@link Action} to be called when this ObservableSource invokes either
     * {@link Observer#onComplete onComplete} or {@link Observer#onError onError}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doAfterTerminate.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doAfterTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onFinally
     *            an {@link Action} to be invoked when the source ObservableSource finishes
     * @return an Observable that emits the same items as the source ObservableSource, then invokes the
     *         {@link Action}
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doOnTerminate(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doAfterTerminate(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.EMPTY_ACTION, onFinally);
    }

    /**
     * Calls the specified action after this Observable signals onError or onCompleted or gets disposed by
     * the downstream.
     * <p>In case of a race between a terminal event and a dispose call, the provided {@code onFinally} action
     * is executed once per subscription.
     * <p>Note that the {@code onFinally} action is shared between subscriptions and as such
     * should be thread-safe.
     * <p>
     * <img width="640" height="281" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doFinally.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doFinally} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Operator-fusion:</b></dt>
     *  <dd>This operator supports boundary-limited synchronous or asynchronous queue-fusion.</dd>
     * </dl>
     * <p>History: 2.0.1 - experimental
     * @param onFinally the action called when this Observable terminates or gets disposed
     * @return the new Observable instance
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doFinally(@NonNull Action onFinally) {
        Objects.requireNonNull(onFinally, "onFinally is null");
        return RxJavaPlugins.onAssembly(new ObservableDoFinally<>(this, onFinally));
    }

    /**
     * Calls the dispose {@code Action} if the downstream disposes the sequence.
     * <p>
     * The action is shared between subscriptions and thus may be called concurrently from multiple
     * threads; the action must be thread safe.
     * <p>
     * If the action throws a runtime exception, that exception is rethrown by the {@code dispose()} call,
     * sometimes as a {@code CompositeException} if there were multiple exceptions along the way.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnDispose.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnDispose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onDispose
     *            the action that gets called when the source {@code ObservableSource}'s Disposable is disposed
     * @return the source {@code ObservableSource} modified so as to call this Action when appropriate
     * @throws NullPointerException if onDispose is null
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnDispose(@NonNull Action onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), onDispose);
    }

    /**
     * Modifies the source ObservableSource so that it invokes an action when it calls {@code onComplete}.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnComplete.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnComplete} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onComplete
     *            the action to invoke when the source ObservableSource calls {@code onComplete}
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnComplete(@NonNull Action onComplete) {
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the appropriate onXXX consumer (shared between all subscribers) whenever a signal with the same type
     * passes through, before forwarding them to downstream.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    private Observable<T> doOnEach(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError, @NonNull Action onComplete, @NonNull Action onAfterTerminate) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return RxJavaPlugins.onAssembly(new ObservableDoOnEach<>(this, onNext, onError, onComplete, onAfterTerminate));
    }

    /**
     * Modifies the source ObservableSource so that it invokes an action for each item it emits.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNotification
     *            the action to invoke for each item emitted by the source ObservableSource
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnEach(@NonNull Consumer<? super Notification<T>> onNotification) {
        Objects.requireNonNull(onNotification, "onNotification is null");
        return doOnEach(
                Functions.notificationOnNext(onNotification),
                Functions.notificationOnError(onNotification),
                Functions.notificationOnComplete(onNotification),
                Functions.EMPTY_ACTION
            );
    }

    /**
     * Modifies the source ObservableSource so that it notifies an Observer for each item and terminal event it emits.
     * <p>
     * In case the {@code onError} of the supplied observer throws, the downstream will receive a composite
     * exception containing the original exception and the exception thrown by {@code onError}. If either the
     * {@code onNext} or the {@code onComplete} method of the supplied observer throws, the downstream will be
     * terminated and will receive this thrown exception.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnEach.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param observer
     *            the observer to be notified about onNext, onError and onComplete events on its
     *            respective methods before the actual downstream Observer gets notified.
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnEach(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        return doOnEach(
                ObservableInternalHelper.observerOnNext(observer),
                ObservableInternalHelper.observerOnError(observer),
                ObservableInternalHelper.observerOnComplete(observer),
                Functions.EMPTY_ACTION);
    }

    /**
     * Modifies the source ObservableSource so that it invokes an action if it calls {@code onError}.
     * <p>
     * In case the {@code onError} action throws, the downstream will receive a composite exception containing
     * the original exception and the exception thrown by {@code onError}.
     * <p>
     * <img width="640" height="355" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onError
     *            the action to invoke if the source ObservableSource calls {@code onError}
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnError(@NonNull Consumer<? super Throwable> onError) {
        return doOnEach(Functions.emptyConsumer(), onError, Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Calls the appropriate onXXX method (shared between all Observer) for the lifecycle events of
     * the sequence (subscription, disposal).
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnLifecycle.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnLifecycle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *              a Consumer called with the Disposable sent via Observer.onSubscribe()
     * @param onDispose
     *              called when the downstream disposes the Disposable via dispose()
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnLifecycle(@NonNull Consumer<? super Disposable> onSubscribe, @NonNull Action onDispose) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onDispose, "onDispose is null");
        return RxJavaPlugins.onAssembly(new ObservableDoOnLifecycle<>(this, onSubscribe, onDispose));
    }

    /**
     * Modifies the source ObservableSource so that it invokes an action when it calls {@code onNext}.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnNext.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            the action to invoke when the source ObservableSource calls {@code onNext}
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnNext(@NonNull Consumer<? super T> onNext) {
        return doOnEach(onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION, Functions.EMPTY_ACTION);
    }

    /**
     * Modifies the source {@code ObservableSource} so that it invokes the given action when it is subscribed from
     * its subscribers. Each subscription will result in an invocation of the given action except when the
     * source {@code ObservableSource} is reference counted, in which case the source {@code ObservableSource} will invoke
     * the given action for the first subscription.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnSubscribe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onSubscribe
     *            the Consumer that gets called when an Observer subscribes to the current {@code Observable}
     * @return the source {@code ObservableSource} modified so as to call this Consumer when appropriate
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnSubscribe(@NonNull Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.EMPTY_ACTION);
    }

    /**
     * Modifies the source ObservableSource so that it invokes an action when it calls {@code onComplete} or
     * {@code onError}.
     * <p>
     * <img width="640" height="327" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doOnTerminate.o.png" alt="">
     * <p>
     * This differs from {@code doAfterTerminate} in that this happens <em>before</em> the {@code onComplete} or
     * {@code onError} notification.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code doOnTerminate} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onTerminate
     *            the action to invoke when the source ObservableSource calls {@code onComplete} or {@code onError}
     * @return the source ObservableSource with the side-effecting behavior applied
     * @see <a href="http://reactivex.io/documentation/operators/do.html">ReactiveX operators documentation: Do</a>
     * @see #doAfterTerminate(Action)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> doOnTerminate(@NonNull Action onTerminate) {
        Objects.requireNonNull(onTerminate, "onTerminate is null");
        return doOnEach(Functions.emptyConsumer(),
                Functions.actionConsumer(onTerminate), onTerminate,
                Functions.EMPTY_ACTION);
    }

    /**
     * Returns a Maybe that emits the single item at a specified index in a sequence of emissions from
     * this Observable or completes if this Observable signals fewer elements than index.
     * <p>
     * <img width="640" height="363" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAt.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAt} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @return a Maybe that emits a single item: the item at the specified position in the sequence of
     *         those emitted by the source ObservableSource
     * @throws IndexOutOfBoundsException
     *             if {@code index} is less than 0
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> elementAt(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        return RxJavaPlugins.onAssembly(new ObservableElementAtMaybe<>(this, index));
    }

    /**
     * Returns a Single that emits the item found at a specified index in a sequence of emissions from
     * this Observable, or a default item if that index is out of range.
     * <p>
     * <img width="640" height="353" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAtDefault.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAt} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @param defaultItem
     *            the default item
     * @return a Single that emits the item at the specified position in the sequence emitted by the source
     *         ObservableSource, or the default item if that index is outside the bounds of the source sequence
     * @throws IndexOutOfBoundsException
     *             if {@code index} is less than 0
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> elementAt(long index, @NonNull T defaultItem) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableElementAtSingle<>(this, index, defaultItem));
    }

    /**
     * Returns a Single that emits the item found at a specified index in a sequence of emissions from this Observable
     * or signals a {@link NoSuchElementException} if this Observable signals fewer elements than index.
     * <p>
     * <img width="640" height="362" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/elementAtOrError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code elementAtOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param index
     *            the zero-based index of the item to retrieve
     * @return a Single that emits the item at the specified position in the sequence emitted by the source
     *         ObservableSource, or the default item if that index is outside the bounds of the source sequence
     * @throws IndexOutOfBoundsException
     *             if {@code index} is less than 0
     * @see <a href="http://reactivex.io/documentation/operators/elementat.html">ReactiveX operators documentation: ElementAt</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> elementAtOrError(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        return RxJavaPlugins.onAssembly(new ObservableElementAtSingle<>(this, index, null));
    }

    /**
     * Filters items emitted by an ObservableSource by only emitting those that satisfy a specified predicate.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates each item emitted by the source ObservableSource, returning {@code true}
     *            if it passes the filter
     * @return an Observable that emits only those items emitted by the source ObservableSource that the filter
     *         evaluates as {@code true}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> filter(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableFilter<>(this, predicate));
    }

    /**
     * Returns a Maybe that emits only the very first item emitted by the source ObservableSource, or
     * completes if the source ObservableSource is empty.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstElement.m.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Maybe instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> firstElement() {
        return elementAt(0L);
    }

    /**
     * Returns a Single that emits only the very first item emitted by the source ObservableSource, or a default item
     * if the source ObservableSource completes without emitting any items.
     * <p>
     * <img width="640" height="286" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/first.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code first} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the default item to emit if the source ObservableSource doesn't emit anything
     * @return the new Single instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> first(@NonNull T defaultItem) {
        return elementAt(0L, defaultItem);
    }

    /**
     * Returns a Single that emits only the very first item emitted by this Observable or
     * signals a {@link NoSuchElementException} if this Observable is empty.
     * <p>
     * <img width="640" height="434" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Single instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> firstOrError() {
        return elementAtOrError(0L);
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source ObservableSource, where that function returns an ObservableSource, and then merging those resulting
     * ObservableSources and emitting the results of this merger.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner ObservableSources and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and merging the results of the ObservableSources obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return flatMap(mapper, false);
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source ObservableSource, where that function returns an ObservableSource, and then merging those resulting
     * ObservableSources and emitting the results of this merger.
     * <p>
     * <img width="640" height="356" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner ObservableSources and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and merging the results of the ObservableSources obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, boolean delayErrors) {
        return flatMap(mapper, delayErrors, Integer.MAX_VALUE);
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source ObservableSource, where that function returns an ObservableSource, and then merging those resulting
     * ObservableSources and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="441" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner ObservableSources and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and merging the results of the ObservableSources obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, delayErrors, maxConcurrency, bufferSize());
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source ObservableSource, where that function returns an ObservableSource, and then merging those resulting
     * ObservableSources and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="441" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner ObservableSources and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @param bufferSize
     *            the number of elements to prefetch from each inner ObservableSource
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and merging the results of the ObservableSources obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableFlatMap<>(this, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    /**
     * Returns an Observable that applies a function to each item emitted or notification raised by the source
     * ObservableSource and then flattens the ObservableSources returned from these functions and emits the resulting items.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onNextMapper
     *            a function that returns an ObservableSource to merge for each item emitted by the source ObservableSource
     * @param onErrorMapper
     *            a function that returns an ObservableSource to merge for an onError notification from the source
     *            ObservableSource
     * @param onCompleteSupplier
     *            a function that returns an ObservableSource to merge for an onComplete notification from the source
     *            ObservableSource
     * @return an Observable that emits the results of merging the ObservableSources returned from applying the
     *         specified functions to the emissions and notifications of the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(
            @NonNull Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            @NonNull Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            @NonNull Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(new ObservableMapNotification<>(this, onNextMapper, onErrorMapper, onCompleteSupplier));
    }

    /**
     * Returns an Observable that applies a function to each item emitted or notification raised by the source
     * ObservableSource and then flattens the ObservableSources returned from these functions and emits the resulting items,
     * while limiting the maximum number of concurrent subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.nce.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the result type
     * @param onNextMapper
     *            a function that returns an ObservableSource to merge for each item emitted by the source ObservableSource
     * @param onErrorMapper
     *            a function that returns an ObservableSource to merge for an onError notification from the source
     *            ObservableSource
     * @param onCompleteSupplier
     *            a function that returns an ObservableSource to merge for an onComplete notification from the source
     *            ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits the results of merging the ObservableSources returned from applying the
     *         specified functions to the emissions and notifications of the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(
            @NonNull Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            @NonNull Function<Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            @NonNull Supplier<? extends ObservableSource<? extends R>> onCompleteSupplier,
            int maxConcurrency) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(new ObservableMapNotification<>(this, onNextMapper, onErrorMapper, onCompleteSupplier), maxConcurrency);
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source ObservableSource, where that function returns an ObservableSource, and then merging those resulting
     * ObservableSources and emitting the results of this merger, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="441" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaxConcurrency.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the value type of the inner ObservableSources and the output type
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source ObservableSource and merging the results of the ObservableSources obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int maxConcurrency) {
        return flatMap(mapper, false, maxConcurrency, bufferSize());
    }

    /**
     * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
     * source ObservableSource and a specified collection ObservableSource.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource
     * @param resultSelector
     *            a function that combines one item emitted by each of the source and collection ObservableSources and
     *            returns an item to be emitted by the resulting ObservableSource
     * @return an Observable that emits the results of applying a function to a pair of values emitted by the
     *         source ObservableSource and the collection ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> resultSelector) {
        return flatMap(mapper, resultSelector, false, bufferSize(), bufferSize());
    }

    /**
     * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
     * source ObservableSource and a specified collection ObservableSource.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection ObservableSources and
     *            returns an item to be emitted by the resulting ObservableSource
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @return an Observable that emits the results of applying a function to a pair of values emitted by the
     *         source ObservableSource and the collection ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors) {
        return flatMap(mapper, combiner, delayErrors, bufferSize(), bufferSize());
    }

    /**
     * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
     * source ObservableSource and a specified collection ObservableSource, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection ObservableSources and
     *            returns an item to be emitted by the resulting ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @return an Observable that emits the results of applying a function to a pair of values emitted by the
     *         source ObservableSource and the collection ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, combiner, delayErrors, maxConcurrency, bufferSize());
    }

    /**
     * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
     * source ObservableSource and a specified collection ObservableSource, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection ObservableSources and
     *            returns an item to be emitted by the resulting ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @param delayErrors
     *            if true, exceptions from the current Observable and all inner ObservableSources are delayed until all of them terminate
     *            if false, the first one signalling an exception will terminate the whole sequence immediately
     * @param bufferSize
     *            the number of elements to prefetch from the inner ObservableSources.
     * @return an Observable that emits the results of applying a function to a pair of values emitted by the
     *         source ObservableSource and the collection ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return flatMap(ObservableInternalHelper.flatMapWithCombiner(mapper, combiner), delayErrors, maxConcurrency, bufferSize);
    }

    /**
     * Returns an Observable that emits the results of a specified function to the pair of values emitted by the
     * source ObservableSource and a specified collection ObservableSource, while limiting the maximum number of concurrent
     * subscriptions to these ObservableSources.
     * <p>
     * <img width="640" height="390" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mergeMap.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the collection ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param mapper
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource
     * @param combiner
     *            a function that combines one item emitted by each of the source and collection ObservableSources and
     *            returns an item to be emitted by the resulting ObservableSource
     * @param maxConcurrency
     *         the maximum number of ObservableSources that may be subscribed to concurrently
     * @return an Observable that emits the results of applying a function to a pair of values emitted by the
     *         source ObservableSource and the collection ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends R> combiner, int maxConcurrency) {
        return flatMap(mapper, combiner, false, maxConcurrency, bufferSize());
    }

    /**
     * Maps each element of the upstream Observable into CompletableSources, subscribes to them and
     * waits until the upstream and all CompletableSources complete.
     * <p>
     * <img width="640" height="424" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapCompletable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param mapper the function that received each source value and transforms them into CompletableSources.
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        return flatMapCompletable(mapper, false);
    }

    /**
     * Maps each element of the upstream Observable into CompletableSources, subscribes to them and
     * waits until the upstream and all CompletableSources complete, optionally delaying all errors.
     * <p>
     * <img width="640" height="361" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapCompletableDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param mapper the function that received each source value and transforms them into CompletableSources.
     * @param delayErrors if true errors from the upstream and inner CompletableSources are delayed until each of them
     * terminates.
     * @return the new Completable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable flatMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapCompletableCompletable<>(this, mapper, delayErrors));
    }

    /**
     * Returns an Observable that merges each item emitted by the source ObservableSource with the values in an
     * Iterable corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of item emitted by the resulting Iterable
     * @param mapper
     *            a function that returns an Iterable sequence of values for when given an item emitted by the
     *            source ObservableSource
     * @return an Observable that emits the results of merging the items emitted by the source ObservableSource with
     *         the values in the Iterables corresponding to those items, as generated by {@code collectionSelector}
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<U> flatMapIterable(@NonNull Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlattenIterable<>(this, mapper));
    }

    /**
     * Returns an Observable that emits the results of applying a function to the pair of values from the source
     * ObservableSource and an Iterable corresponding to that item that is generated by a selector.
     * <p>
     * <img width="640" height="410" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapIterable.o.r.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the collection element type
     * @param <V>
     *            the type of item emitted by the resulting Iterable
     * @param mapper
     *            a function that returns an Iterable sequence of values for each item emitted by the source
     *            ObservableSource
     * @param resultSelector
     *            a function that returns an item based on the item emitted by the source ObservableSource and the
     *            Iterable returned for that item by the {@code collectionSelector}
     * @return an Observable that emits the items returned by {@code resultSelector} for each item in the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<V> flatMapIterable(@NonNull Function<? super T, ? extends Iterable<? extends U>> mapper,
            @NonNull BiFunction<? super T, ? super U, ? extends V> resultSelector) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return flatMap(ObservableInternalHelper.flatMapIntoIterable(mapper), resultSelector, false, bufferSize(), bufferSize());
    }

    /**
     * Maps each element of the upstream Observable into MaybeSources, subscribes to all of them
     * and merges their onSuccess values, in no particular order, into a single Observable sequence.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into MaybeSources.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        return flatMapMaybe(mapper, false);
    }

    /**
     * Maps each element of the upstream Observable into MaybeSources, subscribes to them
     * and merges their onSuccess values, in no particular order, into a single Observable sequence,
     * optionally delaying all errors.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapMaybe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into MaybeSources.
     * @param delayErrors if true errors from the upstream and inner MaybeSources are delayed until each of them
     * terminates.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapMaybe<>(this, mapper, delayErrors));
    }

    /**
     * Maps each element of the upstream Observable into SingleSources, subscribes to all of them
     * and merges their onSuccess values, in no particular order, into a single Observable sequence.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into SingleSources.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        return flatMapSingle(mapper, false);
    }

    /**
     * Maps each element of the upstream Observable into SingleSources, subscribes to them
     * and merges their onSuccess values, in no particular order, into a single Observable sequence,
     * optionally delaying all errors.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapSingle.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the result value type
     * @param mapper the function that received each source value and transforms them into SingleSources.
     * @param delayErrors if true errors from the upstream and inner SingleSources are delayed until each of them
     * terminates.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> flatMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper, boolean delayErrors) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapSingle<>(this, mapper, delayErrors));
    }

    /**
     * Subscribes to the {@link ObservableSource} and receives notifications for each element.
     * <p>
     * <img width="640" height="264" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEach.o.png" alt="">
     * <p>
     * Alias to {@link #subscribe(Consumer)}
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            {@link Consumer} to execute for each item.
     * @return
     *            a Disposable that allows disposing of an asynchronous sequence
     * @throws NullPointerException
     *             if {@code onNext} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEach(@NonNull Consumer<? super T> onNext) {
        return subscribe(onNext);
    }

    /**
     * Subscribes to the {@link ObservableSource} and receives notifications for each element until the
     * onNext Predicate returns false.
     * <p>
     * <img width="640" height="272" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachWhile.o.png" alt="">
     * <p>
     * If the Observable emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            {@link Predicate} to execute for each item.
     * @return
     *            a Disposable that allows disposing of an asynchronous sequence
     * @throws NullPointerException
     *             if {@code onNext} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext) {
        return forEachWhile(onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the {@link ObservableSource} and receives notifications for each element and error events until the
     * onNext Predicate returns false.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            {@link Predicate} to execute for each item.
     * @param onError
     *            {@link Consumer} to execute when an error is emitted.
     * @return
     *            a Disposable that allows disposing of an asynchronous sequence
     * @throws NullPointerException
     *             if {@code onNext} is null, or
     *             if {@code onError} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        return forEachWhile(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to the {@link ObservableSource} and receives notifications for each element and the terminal events until the
     * onNext Predicate returns false.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *            {@link Predicate} to execute for each item.
     * @param onError
     *            {@link Consumer} to execute when an error is emitted.
     * @param onComplete
     *            {@link Action} to execute when completion is signalled.
     * @return
     *            a Disposable that allows disposing of an asynchronous sequence
     * @throws NullPointerException
     *             if {@code onNext} is null, or
     *             if {@code onError} is null, or
     *             if {@code onComplete} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable forEachWhile(@NonNull Predicate<? super T> onNext, @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        ForEachWhileObserver<T> o = new ForEachWhileObserver<>(onNext, onError, onComplete);
        subscribe(o);
        return o;
    }

    /**
     * Groups the items emitted by an {@code ObservableSource} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s. The emitted {@code GroupedObservableSource} allows only a single
     * {@link Observer} during its lifetime and if this {@code Observer} calls dispose() before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservableSource} emission.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
     * <p>
     * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservableSource}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return an {@code ObservableSource} that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the source ObservableSource that share that
     *         key value
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Observable<GroupedObservable<K, T>> groupBy(@NonNull Function<? super T, ? extends K> keySelector) {
        return groupBy(keySelector, (Function)Functions.identity(), false, bufferSize());
    }

    /**
     * Groups the items emitted by an {@code ObservableSource} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s. The emitted {@code GroupedObservableSource} allows only a single
     * {@link Observer} during its lifetime and if this {@code Observer} calls dispose() before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservableSource} emission.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
     * <p>
     * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservableSource}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @param delayError
     *            if true, the exception from the current Observable is delayed in each group until that specific group emitted
     *            the normal values; if false, the exception bypasses values in the groups and is reported immediately.
     * @return an {@code ObservableSource} that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the source ObservableSource that share that
     *         key value
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Observable<GroupedObservable<K, T>> groupBy(@NonNull Function<? super T, ? extends K> keySelector, boolean delayError) {
        return groupBy(keySelector, (Function)Functions.identity(), delayError, bufferSize());
    }

    /**
     * Groups the items emitted by an {@code ObservableSource} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s. The emitted {@code GroupedObservableSource} allows only a single
     * {@link Observer} during its lifetime and if this {@code Observer} calls dispose() before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservableSource} emission.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
     * <p>
     * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservableSource}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @return an {@code ObservableSource} that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the source ObservableSource that share that
     *         key value
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            Function<? super T, ? extends V> valueSelector) {
        return groupBy(keySelector, valueSelector, false, bufferSize());
    }

    /**
     * Groups the items emitted by an {@code ObservableSource} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s. The emitted {@code GroupedObservableSource} allows only a single
     * {@link Observer} during its lifetime and if this {@code Observer} calls dispose() before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservableSource} emission.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
     * <p>
     * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservableSource}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @param delayError
     *            if true, the exception from the current Observable is delayed in each group until that specific group emitted
     *            the normal values; if false, the exception bypasses values in the groups and is reported immediately.
     * @return an {@code ObservableSource} that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the source ObservableSource that share that
     *         key value
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector, boolean delayError) {
        return groupBy(keySelector, valueSelector, delayError, bufferSize());
    }

    /**
     * Groups the items emitted by an {@code ObservableSource} according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s. The emitted {@code GroupedObservableSource} allows only a single
     * {@link Observer} during its lifetime and if this {@code Observer} calls dispose() before the
     * source terminates, the next emission by the source having the same key will trigger a new
     * {@code GroupedObservableSource} emission.
     * <p>
     * <img width="640" height="360" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupBy.png" alt="">
     * <p>
     * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
     * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedObservableSource}s that do not concern you. Instead, you can signal to them that they may
     * discard their buffers by applying an operator like {@link #ignoreElements} to them.
     * <p>
     * Note also that ignoring groups or subscribing later (i.e., on another thread) will result in
     * so-called group abandonment where a group will only contain one element and the group will be
     * re-created over and over as new upstream items trigger a new group. The behavior is
     * a trade-off between no-dataloss, upstream cancellation and excessive group creation.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupBy} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param keySelector
     *            a function that extracts the key for each item
     * @param valueSelector
     *            a function that extracts the return element for each item
     * @param delayError
     *            if true, the exception from the current Observable is delayed in each group until that specific group emitted
     *            the normal values; if false, the exception bypasses values in the groups and is reported immediately.
     * @param bufferSize
     *            the hint for how many {@link GroupedObservable}s and element in each {@link GroupedObservable} should be buffered
     * @param <K>
     *            the key type
     * @param <V>
     *            the element type
     * @return an {@code ObservableSource} that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the source ObservableSource that share that
     *         key value
     * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX operators documentation: GroupBy</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Observable<GroupedObservable<K, V>> groupBy(@NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            boolean delayError, int bufferSize) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");

        return RxJavaPlugins.onAssembly(new ObservableGroupBy<>(this, keySelector, valueSelector, bufferSize, delayError));
    }

    /**
     * Returns an Observable that correlates two ObservableSources when they overlap in time and groups the results.
     * <p>
     * There are no guarantees in what order the items get combined when multiple
     * items from one or both source ObservableSources overlap.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/groupJoin.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code groupJoin} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TRight> the value type of the right ObservableSource source
     * @param <TLeftEnd> the element type of the left duration ObservableSources
     * @param <TRightEnd> the element type of the right duration ObservableSources
     * @param <R> the result type
     * @param other
     *            the other ObservableSource to correlate items from the source ObservableSource with
     * @param leftEnd
     *            a function that returns an ObservableSource whose emissions indicate the duration of the values of
     *            the source ObservableSource
     * @param rightEnd
     *            a function that returns an ObservableSource whose emissions indicate the duration of the values of
     *            the {@code right} ObservableSource
     * @param resultSelector
     *            a function that takes an item emitted by each ObservableSource and returns the value to be emitted
     *            by the resulting ObservableSource
     * @return an Observable that emits items based on combining those items emitted by the source ObservableSources
     *         whose durations overlap
     * @see <a href="http://reactivex.io/documentation/operators/join.html">ReactiveX operators documentation: Join</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <TRight, TLeftEnd, TRightEnd, R> Observable<R> groupJoin(
            @NonNull ObservableSource<? extends TRight> other,
            @NonNull Function<? super T, ? extends ObservableSource<TLeftEnd>> leftEnd,
            @NonNull Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
            @NonNull BiFunction<? super T, ? super Observable<TRight>, ? extends R> resultSelector
    ) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(leftEnd, "leftEnd is null");
        Objects.requireNonNull(rightEnd, "rightEnd is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return RxJavaPlugins.onAssembly(new ObservableGroupJoin<>(
                this, other, leftEnd, rightEnd, resultSelector));
    }

    /**
     * Hides the identity of this Observable and its Disposable.
     * <p>Allows hiding extra features such as {@link io.reactivex.rxjava3.subjects.Subject}'s
     * {@link Observer} methods or preventing certain identity-based
     * optimizations (fusion).
     * <p>
     * <img width="640" height="283" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/hide.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code hide} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new Observable instance
     *
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> hide() {
        return RxJavaPlugins.onAssembly(new ObservableHide<>(this));
    }

    /**
     * Ignores all items emitted by the source ObservableSource and only calls {@code onComplete} or {@code onError}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ignoreElements.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ignoreElements} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Completable instance
     * @see <a href="http://reactivex.io/documentation/operators/ignoreelements.html">ReactiveX operators documentation: IgnoreElements</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable ignoreElements() {
        return RxJavaPlugins.onAssembly(new ObservableIgnoreElementsCompletable<>(this));
    }

    /**
     * Returns a Single that emits {@code true} if the source ObservableSource is empty, otherwise {@code false}.
     * <p>
     * In Rx.Net this is negated as the {@code any} Observer but we renamed this in RxJava to better match Java
     * naming idioms.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/isEmpty.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code isEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits a Boolean
     * @see <a href="http://reactivex.io/documentation/operators/contains.html">ReactiveX operators documentation: Contains</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<Boolean> isEmpty() {
        return all(Functions.alwaysFalse());
    }

    /**
     * Correlates the items emitted by two ObservableSources based on overlapping durations.
     * <p>
     * There are no guarantees in what order the items get combined when multiple
     * items from one or both source ObservableSources overlap.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/join_.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code join} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <TRight> the value type of the right ObservableSource source
     * @param <TLeftEnd> the element type of the left duration ObservableSources
     * @param <TRightEnd> the element type of the right duration ObservableSources
     * @param <R> the result type
     * @param other
     *            the second ObservableSource to join items from
     * @param leftEnd
     *            a function to select a duration for each item emitted by the source ObservableSource, used to
     *            determine overlap
     * @param rightEnd
     *            a function to select a duration for each item emitted by the {@code right} ObservableSource, used to
     *            determine overlap
     * @param resultSelector
     *            a function that computes an item to be emitted by the resulting ObservableSource for any two
     *            overlapping items emitted by the two ObservableSources
     * @return an Observable that emits items correlating to items emitted by the source ObservableSources that have
     *         overlapping durations
     * @see <a href="http://reactivex.io/documentation/operators/join.html">ReactiveX operators documentation: Join</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <TRight, TLeftEnd, TRightEnd, R> Observable<R> join(
            @NonNull ObservableSource<? extends TRight> other,
            @NonNull Function<? super T, ? extends ObservableSource<TLeftEnd>> leftEnd,
            @NonNull Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
            @NonNull BiFunction<? super T, ? super TRight, ? extends R> resultSelector
    ) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(leftEnd, "leftEnd is null");
        Objects.requireNonNull(rightEnd, "rightEnd is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return RxJavaPlugins.onAssembly(new ObservableJoin<T, TRight, TLeftEnd, TRightEnd, R>(
                this, other, leftEnd, rightEnd, resultSelector));
    }

    /**
     * Returns a Maybe that emits the last item emitted by this Observable or
     * completes if this Observable is empty.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastElement.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Maybe that emits the last item from the source ObservableSource or notifies observers of an
     *         error
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> lastElement() {
        return RxJavaPlugins.onAssembly(new ObservableLastMaybe<>(this));
    }

    /**
     * Returns a Single that emits only the last item emitted by this Observable, or a default item
     * if this Observable completes without emitting any items.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/last.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code last} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            the default item to emit if the source ObservableSource is empty
     * @return a Single that emits only the last item emitted by the source ObservableSource, or a default item
     *         if the source ObservableSource is empty
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> last(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableLastSingle<>(this, defaultItem));
    }

    /**
     * Returns a Single that emits only the last item emitted by this Observable or
     * signals a {@link NoSuchElementException} if this Observable is empty.
     * <p>
     * <img width="640" height="236" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits only the last item emitted by the source ObservableSource.
     *         If the source ObservableSource completes without emitting any items a {@link NoSuchElementException} will be thrown.
     * @see <a href="http://reactivex.io/documentation/operators/last.html">ReactiveX operators documentation: Last</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> lastOrError() {
        return RxJavaPlugins.onAssembly(new ObservableLastSingle<>(this, null));
    }

    /**
     * <strong>This method requires advanced knowledge about building operators, please consider
     * other standard composition methods first;</strong>
     * Returns an {@code Observable} which, when subscribed to, invokes the {@link ObservableOperator#apply(Observer) apply(Observer)} method
     * of the provided {@link ObservableOperator} for each individual downstream {@link Observer} and allows the
     * insertion of a custom operator by accessing the downstream's {@link Observer} during this subscription phase
     * and providing a new {@code Observer}, containing the custom operator's intended business logic, that will be
     * used in the subscription process going further upstream.
     * <p>
     * Generally, such a new {@code Observer} will wrap the downstream's {@code Observer} and forwards the
     * {@code onNext}, {@code onError} and {@code onComplete} events from the upstream directly or according to the
     * emission pattern the custom operator's business logic requires. In addition, such operator can intercept the
     * flow control calls of {@code dispose} and {@code isDisposed} that would have traveled upstream and perform
     * additional actions depending on the same business logic requirements.
     * <p>
     * Example:
     * <pre><code>
     * // Step 1: Create the consumer type that will be returned by the ObservableOperator.apply():
     *
     * public final class CustomObserver&lt;T&gt; implements Observer&lt;T&gt;, Disposable {
     *
     *     // The downstream's Observer that will receive the onXXX events
     *     final Observer&lt;? super String&gt; downstream;
     *
     *     // The connection to the upstream source that will call this class' onXXX methods
     *     Disposable upstream;
     *
     *     // The constructor takes the downstream subscriber and usually any other parameters
     *     public CustomObserver(Observer&lt;? super String&gt; downstream) {
     *         this.downstream = downstream;
     *     }
     *
     *     // In the subscription phase, the upstream sends a Disposable to this class
     *     // and subsequently this class has to send a Disposable to the downstream.
     *     // Note that relaying the upstream's Disposable directly is not allowed in RxJava
     *     &#64;Override
     *     public void onSubscribe(Disposable d) {
     *         if (upstream != null) {
     *             d.dispose();
     *         } else {
     *             upstream = d;
     *             downstream.onSubscribe(this);
     *         }
     *     }
     *
     *     // The upstream calls this with the next item and the implementation's
     *     // responsibility is to emit an item to the downstream based on the intended
     *     // business logic, or if it can't do so for the particular item,
     *     // request more from the upstream
     *     &#64;Override
     *     public void onNext(T item) {
     *         String str = item.toString();
     *         if (str.length() &lt; 2) {
     *             downstream.onNext(str);
     *         }
     *         // Observable doesn't support backpressure, therefore, there is no
     *         // need or opportunity to call upstream.request(1) if an item
     *         // is not produced to the downstream
     *     }
     *
     *     // Some operators may handle the upstream's error while others
     *     // could just forward it to the downstream.
     *     &#64;Override
     *     public void onError(Throwable throwable) {
     *         downstream.onError(throwable);
     *     }
     *
     *     // When the upstream completes, usually the downstream should complete as well.
     *     &#64;Override
     *     public void onComplete() {
     *         downstream.onComplete();
     *     }
     *
     *     // Some operators may use their own resources which should be cleaned up if
     *     // the downstream disposes the flow before it completed. Operators without
     *     // resources can simply forward the dispose to the upstream.
     *     // In some cases, a disposed flag may be set by this method so that other parts
     *     // of this class may detect the dispose and stop sending events
     *     // to the downstream.
     *     &#64;Override
     *     public void dispose() {
     *         upstream.dispose();
     *     }
     *
     *     // Some operators may simply forward the call to the upstream while others
     *     // can return the disposed flag set in dispose().
     *     &#64;Override
     *     public boolean isDisposed() {
     *         return upstream.isDisposed();
     *     }
     * }
     *
     * // Step 2: Create a class that implements the ObservableOperator interface and
     * //         returns the custom consumer type from above in its apply() method.
     * //         Such class may define additional parameters to be submitted to
     * //         the custom consumer type.
     *
     * final class CustomOperator&lt;T&gt; implements ObservableOperator&lt;String, T&gt; {
     *     &#64;Override
     *     public Observer&lt;T&gt; apply(Observer&lt;? super String&gt; downstream) {
     *         return new CustomObserver&lt;T&gt;(downstream);
     *     }
     * }
     *
     * // Step 3: Apply the custom operator via lift() in a flow by creating an instance of it
     * //         or reusing an existing one.
     *
     * Observable.range(5, 10)
     * .lift(new CustomOperator&lt;Integer&gt;())
     * .test()
     * .assertResult("5", "6", "7", "8", "9");
     * </code></pre>
     * <p>
     * Creating custom operators can be complicated and it is recommended one consults the
     * <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a> page about
     * the tools, requirements, rules, considerations and pitfalls of implementing them.
     * <p>
     * Note that implementing custom operators via this {@code lift()} method adds slightly more overhead by requiring
     * an additional allocation and indirection per assembled flows. Instead, extending the abstract {@code Observable}
     * class and creating an {@link ObservableTransformer} with it is recommended.
     * <p>
     * Note also that it is not possible to stop the subscription phase in {@code lift()} as the {@code apply()} method
     * requires a non-null {@code Observer} instance to be returned, which is then unconditionally subscribed to
     * the upstream {@code Observable}. For example, if the operator decided there is no reason to subscribe to the
     * upstream source because of some optimization possibility or a failure to prepare the operator, it still has to
     * return an {@code Observer} that should immediately dispose the upstream's {@code Disposable} in its
     * {@code onSubscribe} method. Again, using an {@code ObservableTransformer} and extending the {@code Observable} is
     * a better option as {@link #subscribeActual} can decide to not subscribe to its upstream after all.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lift} does not operate by default on a particular {@link Scheduler}, however, the
     *  {@link ObservableOperator} may use a {@code Scheduler} to support its own asynchronous behavior.</dd>
     * </dl>
     *
     * @param <R> the output value type
     * @param lifter the {@link ObservableOperator} that receives the downstream's {@code Observer} and should return
     *               an {@code Observer} with custom behavior to be used as the consumer for the current
     *               {@code Observable}.
     * @return the new Observable instance
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0">RxJava wiki: Writing operators</a>
     * @see #compose(ObservableTransformer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> lift(@NonNull ObservableOperator<? extends R, ? super T> lifter) {
        Objects.requireNonNull(lifter, "lifter is null");
        return RxJavaPlugins.onAssembly(new ObservableLift<>(this, lifter));
    }

    /**
     * Returns an Observable that applies a specified function to each item emitted by the source ObservableSource and
     * emits the results of these function applications.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the output type
     * @param mapper
     *            a function to apply to each item emitted by the ObservableSource
     * @return an Observable that emits the items from the source ObservableSource, transformed by the specified
     *         function
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMap<>(this, mapper));
    }

    /**
     * Returns an Observable that represents all of the emissions <em>and</em> notifications from the source
     * ObservableSource into emissions marked with their original types within {@link Notification} objects.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/materialize.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code materialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits items that are the result of materializing the items and notifications
     *         of the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/materialize-dematerialize.html">ReactiveX operators documentation: Materialize</a>
     * @see #dematerialize(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Notification<T>> materialize() {
        return RxJavaPlugins.onAssembly(new ObservableMaterialize<>(this));
    }

    /**
     * Flattens this and another ObservableSource into a single ObservableSource, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple ObservableSources so that they appear as a single ObservableSource, by
     * using the {@code mergeWith} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an ObservableSource to be merged
     * @return an Observable that emits all of the items emitted by the source ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }

    /**
     * Merges the sequence of items of this Observable with the success value of the other SingleSource.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * The success value of the other {@code SingleSource} can get interleaved at any point of this
     * {@code Observable} sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code SingleSource} whose success value to merge with
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull SingleSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithSingle<>(this, other));
    }

    /**
     * Merges the sequence of items of this Observable with the success value of the other MaybeSource
     * or waits both to complete normally if the MaybeSource is empty.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <p>
     * The success value of the other {@code MaybeSource} can get interleaved at any point of this
     * {@code Observable} sequence.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code MaybeSource} which provides a success value to merge with or completes
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull MaybeSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithMaybe<>(this, other));
    }

    /**
     * Relays the items of this Observable and completes only when the other CompletableSource completes
     * as well.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.10 - experimental
     * @param other the {@code CompletableSource} to await for completion
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> mergeWith(@NonNull CompletableSource other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableMergeWithCompletable<>(this, other));
    }

    /**
     * Modifies an ObservableSource to perform its emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer with {@link Flowable#bufferSize()} "island size".
     *
     * <p>Note that onError notifications will cut ahead of onNext notifications on the emission thread if Scheduler is truly
     * asynchronous. If strict event ordering is required, consider using the {@link #observeOn(Scheduler, boolean)} overload.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given Scheduler's Worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary.
     *
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @return the source ObservableSource modified so that its {@link Observer}s are notified on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler, boolean)
     * @see #observeOn(Scheduler, boolean, int)
     * @see #delay(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler) {
        return observeOn(scheduler, false, bufferSize());
    }

    /**
     * Modifies an ObservableSource to perform its emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer with {@link Flowable#bufferSize()} "island size" and optionally delays onError notifications.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given Scheduler's Worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary.
     *
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @param delayError
     *            indicates if the onError notification may not cut ahead of onNext notification on the other side of the
     *            scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received
     *            from upstream
     * @return the source ObservableSource modified so that its {@link Observer}s are notified on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler)
     * @see #observeOn(Scheduler, boolean, int)
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError) {
        return observeOn(scheduler, delayError, bufferSize());
    }

    /**
     * Modifies an ObservableSource to perform its emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer of configurable "island size" and optionally delays onError notifications.
     * <p>
     * <img width="640" height="308" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/observeOn.png" alt="">
     * <p>
     * This operator keeps emitting as many signals as it can on the given Scheduler's Worker thread,
     * which may result in a longer than expected occupation of this thread. In other terms,
     * it does not allow per-signal fairness in case the worker runs on a shared underlying thread.
     * If such fairness and signal/work interleaving is preferred, use the delay operator with zero time instead.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>"Island size" indicates how large chunks the unbounded buffer allocates to store the excess elements waiting to be consumed
     * on the other side of the asynchronous boundary. Values below 16 are not recommended in performance sensitive scenarios.
     *
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @param delayError
     *            indicates if the onError notification may not cut ahead of onNext notification on the other side of the
     *            scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received
     *            from upstream
     * @param bufferSize the size of the buffer.
     * @return the source ObservableSource modified so that its {@link Observer}s are notified on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     * @see #observeOn(Scheduler)
     * @see #observeOn(Scheduler, boolean)
     * @see #delay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn<>(this, scheduler, delayError, bufferSize));
    }

    /**
     * Filters the items emitted by an ObservableSource, only emitting those of the specified type.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ofClass.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code ofType} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the output type
     * @param clazz
     *            the class type to filter the items emitted by the source ObservableSource
     * @return an Observable that emits items from the source ObservableSource of type {@code clazz}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<U> ofType(@NonNull Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return filter(Functions.isInstanceOf(clazz)).cast(clazz);
    }

    /**
     * Instructs an ObservableSource to pass control to another ObservableSource rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="">
     * <p>
     * By default, when an ObservableSource encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the ObservableSource invokes its Observer's {@code onError} method, and then quits
     * without invoking any more of its Observer's methods. The {@code onErrorResumeNext} method changes this
     * behavior. If you pass a function that returns an ObservableSource ({@code resumeFunction}) to
     * {@code onErrorResumeNext}, if the original ObservableSource encounters an error, instead of invoking its
     * Observer's {@code onError} method, it will instead relinquish control to the ObservableSource returned from
     * {@code resumeFunction}, which will invoke the Observer's {@link Observer#onNext onNext} method if it is
     * able to do so. In such a case, because no ObservableSource necessarily invokes {@code onError}, the Observer
     * may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeNext} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param resumeFunction
     *            a function that returns an ObservableSource that will take over if the source ObservableSource encounters
     *            an error
     * @return the original ObservableSource, with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorResumeNext(@NonNull Function<? super Throwable, ? extends ObservableSource<? extends T>> resumeFunction) {
        Objects.requireNonNull(resumeFunction, "resumeFunction is null");
        return RxJavaPlugins.onAssembly(new ObservableOnErrorNext<>(this, resumeFunction));
    }

    /**
     * Instructs an ObservableSource to pass control to another ObservableSource rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorResumeNext.png" alt="">
     * <p>
     * By default, when an ObservableSource encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the ObservableSource invokes its Observer's {@code onError} method, and then quits
     * without invoking any more of its Observer's methods. The {@code onErrorResumeWith} method changes this
     * behavior. If you pass another ObservableSource ({@code next}) to an ObservableSource's
     * {@code onErrorResumeWith} method, if the original ObservableSource encounters an error, instead of invoking its
     * Observer's {@code onError} method, it will instead relinquish control to {@code next} which
     * will invoke the Observer's {@link Observer#onNext onNext} method if it is able to do so. In such a case,
     * because no ObservableSource necessarily invokes {@code onError}, the Observer may never know that an error
     * happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorResumeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param next
     *            the next ObservableSource source that will take over if the source ObservableSource encounters
     *            an error
     * @return the original ObservableSource, with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorResumeWith(@NonNull ObservableSource<? extends T> next) {
        Objects.requireNonNull(next, "next is null");
        return onErrorResumeNext(Functions.justFunction(next));
    }

    /**
     * Instructs an ObservableSource to emit an item (returned by a specified function) rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturn.o.png" alt="">
     * <p>
     * By default, when an ObservableSource encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the ObservableSource invokes its Observer's {@code onError} method, and then quits
     * without invoking any more of its Observer's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to an ObservableSource's {@code onErrorReturn}
     * method, if the original ObservableSource encounters an error, instead of invoking its Observer's
     * {@code onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param valueSupplier
     *            a function that returns a single value that will be emitted along with a regular onComplete in case
     *            the current Observable signals an onError event
     * @return the original ObservableSource with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorReturn(@NonNull Function<? super Throwable, ? extends T> valueSupplier) {
        Objects.requireNonNull(valueSupplier, "valueSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableOnErrorReturn<>(this, valueSupplier));
    }

    /**
     * Instructs an ObservableSource to emit an item (returned by a specified function) rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onErrorReturnItem.o.png" alt="">
     * <p>
     * By default, when an ObservableSource encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the ObservableSource invokes its Observer's {@code onError} method, and then quits
     * without invoking any more of its Observer's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to an ObservableSource's {@code onErrorReturn}
     * method, if the original ObservableSource encounters an error, instead of invoking its Observer's
     * {@code onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onErrorReturnItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the value that is emitted along with a regular onComplete in case the current
     *            Observable signals an exception
     * @return the original ObservableSource with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onErrorReturnItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return onErrorReturn(Functions.justFunction(item));
    }

    /**
     * Nulls out references to the upstream producer and downstream Observer if
     * the sequence is terminated or downstream calls dispose().
     * <p>
     * <img width="640" height="246" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/onTerminateDetach.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code onTerminateDetach} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return an Observable which nulls out references to the upstream producer and downstream Observer if
     * the sequence is terminated or downstream calls dispose()
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> onTerminateDetach() {
        return RxJavaPlugins.onAssembly(new ObservableDetach<>(this));
    }

    /**
     * Returns a {@link ConnectableObservable}, which is a variety of ObservableSource that waits until its
     * {@link ConnectableObservable#connect connect} method is called before it begins emitting items to those
     * {@link Observer}s that have subscribed to it.
     * <p>
     * <img width="640" height="510" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishConnect.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code publish} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link ConnectableObservable} that upon connection causes the source ObservableSource to emit items
     *         to its {@link Observer}s
     * @see <a href="http://reactivex.io/documentation/operators/publish.html">ReactiveX operators documentation: Publish</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> publish() {
        return RxJavaPlugins.onAssembly(new ObservablePublish<>(this));
    }

    /**
     * Returns an Observable that emits the results of invoking a specified selector on items emitted by a
     * {@link ConnectableObservable} that shares a single subscription to the underlying sequence.
     * <p>
     * <img width="640" height="647" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishFunction.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code publish} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a function that can use the multicasted source sequence as many times as needed, without
     *            causing multiple subscriptions to the source sequence. Observers to the given source will
     *            receive all notifications of the source from the time of the subscription forward.
     * @return an Observable that emits the results of invoking the selector on the items emitted by a {@link ConnectableObservable} that shares a single subscription to the underlying sequence
     * @see <a href="http://reactivex.io/documentation/operators/publish.html">ReactiveX operators documentation: Publish</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> publish(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return RxJavaPlugins.onAssembly(new ObservablePublishSelector<>(this, selector));
    }

    /**
     * Returns a Maybe that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource, then feeds the result of that function along with the second item emitted by the source
     * ObservableSource into the same function, and so on until all items have been emitted by the finite source ObservableSource,
     * and emits the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduce.2.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, whose
     *            result will be used in the next accumulator call
     * @return a Maybe that emits a single item that is the result of accumulating the items emitted by
     *         the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> reduce(@NonNull BiFunction<T, T, T> reducer) {
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceMaybe<>(this, reducer));
    }

    /**
     * Returns a Single that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource and a specified seed value, then feeds the result of that function along with the second item
     * emitted by an ObservableSource into the same function, and so on until all items have been emitted by the
     * finite source ObservableSource, emitting the final result from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduceSeed.o.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that the {@code seed} is shared among all subscribers to the resulting ObservableSource
     * and may cause problems if it is mutable. To make sure each subscriber gets its own value, defer
     * the application of this operator via {@link #defer(Supplier)}:
     * <pre><code>
     * ObservableSource&lt;T&gt; source = ...
     * Single.defer(() -&gt; source.reduce(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     *
     * // alternatively, by using compose to stay fluent
     *
     * source.compose(o -&gt;
     *     Observable.defer(() -&gt; o.reduce(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)).toObservable())
     * ).firstOrError();
     *
     * // or, by using reduceWith instead of reduce
     *
     * source.reduceWith(() -&gt; new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     * </code></pre>
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduce} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the accumulator and output value type
     * @param seed
     *            the initial (seed) accumulator value
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, the
     *            result of which will be used in the next accumulator call
     * @return a Single that emits a single item that is the result of accumulating the output from the
     *         items emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     * @see #reduceWith(Supplier, BiFunction)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Single<R> reduce(R seed, @NonNull BiFunction<R, ? super T, R> reducer) {
        Objects.requireNonNull(seed, "seed is null");
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceSeedSingle<>(this, seed, reducer));
    }

    /**
     * Returns a Single that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource and a seed value derived from calling a specified seedSupplier, then feeds the result
     * of that function along with the second item emitted by an ObservableSource into the same function,
     * and so on until all items have been emitted by the finite source ObservableSource, emitting the final result
     * from the final call to your function as its sole item.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/reduceWith.o.png" alt="">
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "aggregate," "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulator object to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code reduceWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the accumulator and output value type
     * @param seedSupplier
     *            the Supplier that provides the initial (seed) accumulator value for each individual Observer
     * @param reducer
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, the
     *            result of which will be used in the next accumulator call
     * @return a Single that emits a single item that is the result of accumulating the output from the
     *         items emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/reduce.html">ReactiveX operators documentation: Reduce</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Single<R> reduceWith(@NonNull Supplier<R> seedSupplier, @NonNull BiFunction<R, ? super T, R> reducer) {
        Objects.requireNonNull(seedSupplier, "seedSupplier is null");
        Objects.requireNonNull(reducer, "reducer is null");
        return RxJavaPlugins.onAssembly(new ObservableReduceWithSingle<>(this, seedSupplier, reducer));
    }

    /**
     * Returns an Observable that repeats the sequence of items emitted by the source ObservableSource indefinitely.
     * <p>
     * <img width="640" height="287" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatInf.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits the items emitted by the source ObservableSource repeatedly and in sequence
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeat() {
        return repeat(Long.MAX_VALUE);
    }

    /**
     * Returns an Observable that repeats the sequence of items emitted by the source ObservableSource at most
     * {@code count} times.
     * <p>
     * <img width="640" height="336" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatCount.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times the source ObservableSource items are repeated, a count of 0 will yield an empty
     *            sequence
     * @return an Observable that repeats the sequence of items emitted by the source ObservableSource at most
     *         {@code count} times
     * @throws IllegalArgumentException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeat(long times) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        if (times == 0) {
            return empty();
        }
        return RxJavaPlugins.onAssembly(new ObservableRepeat<>(this, times));
    }

    /**
     * Returns an Observable that repeats the sequence of items emitted by the source ObservableSource until
     * the provided stop function returns true.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatUntil.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stop
     *                a boolean supplier that is called when the current Observable completes;
     *                if it returns true, the returned Observable completes; if it returns false,
     *                the upstream Observable is resubscribed.
     * @return the new Observable instance
     * @throws NullPointerException
     *             if {@code stop} is null
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeatUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return RxJavaPlugins.onAssembly(new ObservableRepeatUntil<>(this, stop));
    }

    /**
     * Returns an Observable that emits the same values as the source ObservableSource with the exception of an
     * {@code onComplete}. An {@code onComplete} notification from the source will result in the emission of
     * a {@code void} item to the ObservableSource provided as an argument to the {@code notificationHandler}
     * function. If that ObservableSource calls {@code onComplete} or {@code onError} then {@code repeatWhen} will
     * call {@code onComplete} or {@code onError} on the child subscription. Otherwise, this ObservableSource will
     * resubscribe to the source ObservableSource.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/repeatWhen.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code repeatWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives an ObservableSource of notifications with which a user can complete or error, aborting the repeat.
     * @return the source ObservableSource modified with repeat logic
     * @see <a href="http://reactivex.io/documentation/operators/repeat.html">ReactiveX operators documentation: Repeat</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> repeatWhen(@NonNull Function<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        return RxJavaPlugins.onAssembly(new ObservableRepeatWhen<>(this, handler));
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the underlying ObservableSource
     * that will replay all of its items and notifications to any future {@link Observer}. A Connectable
     * ObservableSource resembles an ordinary ObservableSource, except that it does not begin emitting items when it is
     * subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link ConnectableObservable} that upon connection causes the source ObservableSource to emit its
     *         items to its {@link Observer}s
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay() {
        return ObservableReplay.createFrom(this);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on the items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource.
     * <p>
     * <img width="640" height="449" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.f.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @return an Observable that emits items that are the results of invoking the selector on a
     *         {@link ConnectableObservable} that shares a single subscription to the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this), selector);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying {@code bufferSize} notifications.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="391" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable ObservableSource can replay
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource
     *         replaying no more than {@code bufferSize} items
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, int, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, bufferSize, false), selector);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying {@code bufferSize} notifications.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="391" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            the selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable ObservableSource can replay
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given bufferSize, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource
     *         replaying no more than {@code bufferSize} items
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, bufferSize, eagerTruncate), selector);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnt.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable ObservableSource can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource, and
     *         replays no more than {@code bufferSize} items that were emitted within the window defined by
     *         {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit) {
        return replay(selector, bufferSize, time, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="328" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable ObservableSource can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that is the time source for the window
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource, and
     *         replays no more than {@code bufferSize} items that were emitted within the window defined by
     *         {@code time}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(
                ObservableInternalHelper.replaySupplier(this, bufferSize, time, unit, scheduler, false), selector);
    }
    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying no more than {@code bufferSize} items that were emitted within a specified time window.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * <p>
     * <img width="640" height="328" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fnts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable ObservableSource can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that is the time source for the window
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource, and
     *         replays no more than {@code bufferSize} items that were emitted within the window defined by
     *         {@code time}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(
                ObservableInternalHelper.replaySupplier(this, bufferSize, time, unit, scheduler, eagerTruncate), selector);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="393" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ft.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     *         replaying all items that were emitted within the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit) {
        return replay(selector, time, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is the time source for the window
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     *         replaying all items that were emitted within the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(Function, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, time, unit, scheduler, false), selector);
    }

    /**
     * Returns an Observable that emits items that are the results of invoking a specified selector on items
     * emitted by a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     * replaying all items that were emitted within a specified time window.
     * <p>
     * <img width="640" height="366" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.fts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param selector
     *            a selector function, which can use the multicasted sequence as many times as needed, without
     *            causing multiple subscriptions to the ObservableSource
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is the time source for the window
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return an Observable that emits items that are the results of invoking the selector on items emitted by
     *         a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource,
     *         replaying all items that were emitted within the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final <R> Observable<R> replay(@NonNull Function<? super Observable<T>, ? extends ObservableSource<R>> selector, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.multicastSelector(ObservableInternalHelper.replaySupplier(this, time, unit, scheduler, eagerTruncate), selector);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource that
     * replays at most {@code bufferSize} items emitted by that ObservableSource. A Connectable ObservableSource resembles
     * an ordinary ObservableSource, except that it does not begin emitting items when it is subscribed to, but only
     * when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.n.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no beyond-bufferSize items are referenced,
     * use the {@link #replay(int, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays at most {@code bufferSize} items emitted by that ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.create(this, bufferSize, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource that
     * replays at most {@code bufferSize} items emitted by that ObservableSource. A Connectable ObservableSource resembles
     * an ordinary ObservableSource, except that it does not begin emitting items when it is subscribed to, but only
     * when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.n.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no beyond-bufferSize items are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays at most {@code bufferSize} items emitted by that ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, boolean eagerTruncate) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return ObservableReplay.create(this, bufferSize, eagerTruncate);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * replays at most {@code bufferSize} items that were emitted during a specified time window. A Connectable
     * ObservableSource resembles an ordinary ObservableSource, except that it does not begin emitting items when it is
     * subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nt.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items are referenced,
     * use the {@link #replay(int, long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays at most {@code bufferSize} items that were emitted during the window defined by
     *         {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit) {
        return replay(bufferSize, time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * that replays a maximum of {@code bufferSize} items that are emitted within a specified time window. A
     * Connectable ObservableSource resembles an ordinary ObservableSource, except that it does not begin emitting items
     * when it is subscribed to, but only when its {@code connect} method is called.
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items are referenced,
     * use the {@link #replay(int, long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is used as a time source for the window
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays at most {@code bufferSize} items that were emitted during the window defined by
     *         {@code time}
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(int, long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, bufferSize, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * that replays a maximum of {@code bufferSize} items that are emitted within a specified time window. A
     * Connectable ObservableSource resembles an ordinary ObservableSource, except that it does not begin emitting items
     * when it is subscribed to, but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.nts.png" alt="">
     * <p>
     * Note that due to concurrency requirements, {@code replay(bufferSize)} may hold strong references to more than
     * {@code bufferSize} source emissions.
     * To ensure no out-of-date or beyond-bufferSize items
     * are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param bufferSize
     *            the buffer size that limits the number of items that can be replayed
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler that is used as a time source for the window
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays at most {@code bufferSize} items that were emitted during the window defined by
     *         {@code time}
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @throws IllegalArgumentException
     *             if {@code bufferSize} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(int bufferSize, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, bufferSize, eagerTruncate);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * replays all items emitted by that ObservableSource within a specified time window. A Connectable ObservableSource
     * resembles an ordinary ObservableSource, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code replay} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays the items that were emitted during the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit) {
        return replay(time, unit, Schedulers.computation());
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * replays all items emitted by that ObservableSource within a specified time window. A Connectable ObservableSource
     * resembles an ordinary ObservableSource, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ts.png" alt="">
     * <p>
     * Note that the internal buffer may retain strong references to the oldest item. To ensure no out-of-date items
     * are referenced, use the {@link #replay(long, TimeUnit, Scheduler, boolean)} overload with {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that is the time source for the window
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays the items that were emitted during the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     * @see #replay(long, TimeUnit, Scheduler, boolean)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, false);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     * replays all items emitted by that ObservableSource within a specified time window. A Connectable ObservableSource
     * resembles an ordinary ObservableSource, except that it does not begin emitting items when it is subscribed to,
     * but only when its {@code connect} method is called.
     * <p>
     * <img width="640" height="445" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/replay.o.ts.png" alt="">
     * <p>
     * Note that the internal buffer may retain strong references to the oldest item. To ensure no out-of-date items
     * are referenced, set {@code eagerTruncate = true}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that is the time source for the window
     * @param eagerTruncate
     *            if true, whenever the internal buffer is truncated to the given bufferSize/age, the
     *            oldest item will be guaranteed dereferenced, thus avoiding unexpected retention
     * @return a {@link ConnectableObservable} that shares a single subscription to the source ObservableSource and
     *         replays the items that were emitted during the window defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/replay.html">ReactiveX operators documentation: Replay</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final ConnectableObservable<T> replay(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean eagerTruncate) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return ObservableReplay.create(this, time, unit, scheduler, eagerTruncate);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, resubscribing to it if it calls {@code onError}
     * (infinite retry count).
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.png" alt="">
     * <p>
     * If the source ObservableSource calls {@link Observer#onError}, this method will resubscribe to the source
     * ObservableSource rather than propagating the {@code onError} call.
     * <p>
     * Any and all items emitted by the source ObservableSource will be emitted by the resulting ObservableSource, even
     * those emitted during failed subscriptions. For example, if an ObservableSource fails at first but emits
     * {@code [1, 2]} then succeeds the second time and emits {@code [1, 2, 3, 4, 5]} then the complete sequence
     * of emissions and notifications would be {@code [1, 2, 1, 2, 3, 4, 5, onComplete]}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the source ObservableSource modified with retry logic
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry() {
        return retry(Long.MAX_VALUE, Functions.alwaysTrue());
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, resubscribing to it if it calls {@code onError}
     * and the predicate returns true for that specific exception and retry count.
     * <p>
     * <img width="640" height="235" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.ne.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            the predicate that determines if a resubscription may happen in case of a specific exception
     *            and retry count
     * @return the source ObservableSource modified with retry logic
     * @see #retry()
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(@NonNull BiPredicate<? super Integer, ? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new ObservableRetryBiPredicate<>(this, predicate));
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, resubscribing to it if it calls {@code onError}
     * up to a specified number of retries.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.n.png" alt="">
     * <p>
     * If the source ObservableSource calls {@link Observer#onError}, this method will resubscribe to the source
     * ObservableSource for a maximum of {@code count} resubscriptions rather than propagating the
     * {@code onError} call.
     * <p>
     * Any and all items emitted by the source ObservableSource will be emitted by the resulting ObservableSource, even
     * those emitted during failed subscriptions. For example, if an ObservableSource fails at first but emits
     * {@code [1, 2]} then succeeds the second time and emits {@code [1, 2, 3, 4, 5]} then the complete sequence
     * of emissions and notifications would be {@code [1, 2, 1, 2, 3, 4, 5, onComplete]}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param times
     *            the number of times to resubscribe if the current Observable fails
     * @return the source ObservableSource modified with retry logic
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(long times) {
        return retry(times, Functions.alwaysTrue());
    }

    /**
     * Retries at most times or until the predicate returns false, whichever happens first.
     * <p>
     * <img width="640" height="269" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.nfe.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param times the number of times to resubscribe if the current Observable fails
     * @param predicate the predicate called with the failure Throwable and should return true to trigger a retry.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(long times, @NonNull Predicate<? super Throwable> predicate) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        Objects.requireNonNull(predicate, "predicate is null");

        return RxJavaPlugins.onAssembly(new ObservableRetryPredicate<>(this, times, predicate));
    }

    /**
     * Retries the current Observable if the predicate returns true.
     * <p>
     * <img width="640" height="248" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retry.o.e.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retry} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate the predicate that receives the failure Throwable and should return true to trigger a retry.
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retry(@NonNull Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }

    /**
     * Retries until the given stop function returns true.
     * <p>
     * <img width="640" height="261" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retryUntil.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param stop the function that should return true to stop retrying
     * @return the new Observable instance
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retryUntil(@NonNull BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, Functions.predicateReverseFor(stop));
    }

    /**
     * Returns an Observable that emits the same values as the source ObservableSource with the exception of an
     * {@code onError}. An {@code onError} notification from the source will result in the emission of a
     * {@link Throwable} item to the ObservableSource provided as an argument to the {@code notificationHandler}
     * function. If that ObservableSource calls {@code onComplete} or {@code onError} then {@code retry} will call
     * {@code onComplete} or {@code onError} on the child subscription. Otherwise, this ObservableSource will
     * resubscribe to the source ObservableSource.
     * <p>
     * <img width="640" height="430" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/retryWhen.f.png" alt="">
     * <p>
     * Example:
     *
     * This retries 3 times, each time incrementing the number of seconds it waits.
     *
     * <pre><code>
     *  Observable.create((ObservableEmitter&lt;? super String&gt; s) -&gt; {
     *      System.out.println("subscribing");
     *      s.onError(new RuntimeException("always fails"));
     *  }).retryWhen(attempts -&gt; {
     *      return attempts.zipWith(Observable.range(1, 3), (n, i) -&gt; i).flatMap(i -&gt; {
     *          System.out.println("delay retry by " + i + " second(s)");
     *          return Observable.timer(i, TimeUnit.SECONDS);
     *      });
     *  }).blockingForEach(System.out::println);
     * </code></pre>
     *
     * Output is:
     *
     * <pre> {@code
     * subscribing
     * delay retry by 1 second(s)
     * subscribing
     * delay retry by 2 second(s)
     * subscribing
     * delay retry by 3 second(s)
     * subscribing
     * } </pre>
     * <p>
     * Note that the inner {@code ObservableSource} returned by the handler function should signal
     * either {@code onNext}, {@code onError} or {@code onComplete} in response to the received
     * {@code Throwable} to indicate the operator should retry or terminate. If the upstream to
     * the operator is asynchronous, signalling onNext followed by onComplete immediately may
     * result in the sequence to be completed immediately. Similarly, if this inner
     * {@code ObservableSource} signals {@code onError} or {@code onComplete} while the upstream is
     * active, the sequence is terminated with the same signal immediately.
     * <p>
     * The following example demonstrates how to retry an asynchronous source with a delay:
     * <pre><code>
     * Observable.timer(1, TimeUnit.SECONDS)
     *     .doOnSubscribe(s -&gt; System.out.println("subscribing"))
     *     .map(v -&gt; { throw new RuntimeException(); })
     *     .retryWhen(errors -&gt; {
     *         AtomicInteger counter = new AtomicInteger();
     *         return errors
     *                   .takeWhile(e -&gt; counter.getAndIncrement() != 3)
     *                   .flatMap(e -&gt; {
     *                       System.out.println("delay retry by " + counter.get() + " second(s)");
     *                       return Observable.timer(counter.get(), TimeUnit.SECONDS);
     *                   });
     *     })
     *     .blockingSubscribe(System.out::println, System.out::println);
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code retryWhen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param handler
     *            receives an ObservableSource of notifications with which a user can complete or error, aborting the
     *            retry
     * @return the source ObservableSource modified with retry logic
     * @see <a href="http://reactivex.io/documentation/operators/retry.html">ReactiveX operators documentation: Retry</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> retryWhen(
            @NonNull Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        return RxJavaPlugins.onAssembly(new ObservableRetryWhen<>(this, handler));
    }

    /**
     * Subscribes to the current Observable and wraps the given Observer into a SafeObserver
     * (if not already a SafeObserver) that
     * deals with exceptions thrown by a misbehaving Observer (that doesn't follow the
     * Reactive-Streams specification).
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code safeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param observer the incoming Observer instance
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final void safeSubscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        if (observer instanceof SafeObserver) {
            subscribe(observer);
        } else {
            subscribe(new SafeObserver<>(observer));
        }
    }

    /**
     * Returns an Observable that emits the most recently emitted item (if any) emitted by the source ObservableSource
     * within periodic time intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sample} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return an Observable that emits the results of sampling the items emitted by the source ObservableSource at
     *         the specified time interval
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit) {
        return sample(period, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits the most recently emitted item (if any) emitted by the source ObservableSource
     * within periodic time intervals and optionally emit the very last upstream item when the upstream completes.
     * <p>
     * <img width="640" height="276" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sample} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @return an Observable that emits the results of sampling the items emitted by the source ObservableSource at
     *         the specified time interval
     * @param emitLast
     *            if true and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if false, an unsampled last item is ignored.
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit)
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, boolean emitLast) {
        return sample(period, unit, Schedulers.computation(), emitLast);
    }

    /**
     * Returns an Observable that emits the most recently emitted item (if any) emitted by the source ObservableSource
     * within periodic time intervals, where the intervals are defined on a particular Scheduler.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param scheduler
     *            the {@link Scheduler} to use when sampling
     * @return an Observable that emits the results of sampling the items emitted by the source ObservableSource at
     *         the specified time interval
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleTimed<>(this, period, unit, scheduler, false));
    }

    /**
     * Returns an Observable that emits the most recently emitted item (if any) emitted by the source ObservableSource
     * within periodic time intervals, where the intervals are defined on a particular Scheduler
     *  and optionally emit the very last upstream item when the upstream completes.
     * <p>
     * <img width="640" height="276" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.s.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which {@code period} is defined
     * @param scheduler
     *            the {@link Scheduler} to use when sampling
     * @param emitLast
     *            if true and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if false, an unsampled last item is ignored.
     * @return an Observable that emits the results of sampling the items emitted by the source ObservableSource at
     *         the specified time interval
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #throttleLast(long, TimeUnit, Scheduler)
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> sample(long period, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean emitLast) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleTimed<>(this, period, unit, scheduler, emitLast));
    }

    /**
     * Returns an Observable that, when the specified {@code sampler} ObservableSource emits an item or completes,
     * emits the most recently emitted item (if any) emitted by the source ObservableSource since the previous
     * emission from the {@code sampler} ObservableSource.
     * <p>
     * <img width="640" height="289" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.o.nolast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code sample} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the sampler ObservableSource
     * @param sampler
     *            the ObservableSource to use for sampling the source ObservableSource
     * @return an Observable that emits the results of sampling the items emitted by this ObservableSource whenever
     *         the {@code sampler} ObservableSource emits an item or completes
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> sample(@NonNull ObservableSource<U> sampler) {
        Objects.requireNonNull(sampler, "sampler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleWithObservable<>(this, sampler, false));
    }

    /**
     * Returns an Observable that, when the specified {@code sampler} ObservableSource emits an item or completes,
     * emits the most recently emitted item (if any) emitted by the source ObservableSource since the previous
     * emission from the {@code sampler} ObservableSource
     * and optionally emit the very last upstream item when the upstream or other ObservableSource complete.
     * <p>
     * <img width="640" height="289" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sample.o.emitlast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code sample} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * <p>History: 2.0.5 - experimental
     * @param <U> the element type of the sampler ObservableSource
     * @param sampler
     *            the ObservableSource to use for sampling the source ObservableSource
     * @param emitLast
     *            if true and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if false, an unsampled last item is ignored.
     * @return an Observable that emits the results of sampling the items emitted by this ObservableSource whenever
     *         the {@code sampler} ObservableSource emits an item or completes
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @since 2.1
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> sample(@NonNull ObservableSource<U> sampler, boolean emitLast) {
        Objects.requireNonNull(sampler, "sampler is null");
        return RxJavaPlugins.onAssembly(new ObservableSampleWithObservable<>(this, sampler, emitLast));
    }

    /**
     * Returns an Observable that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource, then feeds the result of that function along with the second item emitted by the source
     * ObservableSource into the same function, and so on until all items have been emitted by the source ObservableSource,
     * emitting the result of each of these iterations.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scan.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scan} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return an Observable that emits the results of each call to the accumulator function
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> scan(@NonNull BiFunction<T, T, T> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator is null");
        return RxJavaPlugins.onAssembly(new ObservableScan<>(this, accumulator));
    }

    /**
     * Returns an Observable that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource and a seed value, then feeds the result of that function along with the second item emitted by
     * the source ObservableSource into the same function, and so on until all items have been emitted by the source
     * ObservableSource, emitting the result of each of these iterations.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that the ObservableSource that results from this method will emit {@code initialValue} as its first
     * emitted item.
     * <p>
     * Note that the {@code initialValue} is shared among all subscribers to the resulting ObservableSource
     * and may cause problems if it is mutable. To make sure each subscriber gets its own value, defer
     * the application of this operator via {@link #defer(Supplier)}:
     * <pre><code>
     * ObservableSource&lt;T&gt; source = ...
     * Observable.defer(() -&gt; source.scan(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)));
     *
     * // alternatively, by using compose to stay fluent
     *
     * source.compose(o -&gt;
     *     Observable.defer(() -&gt; o.scan(new ArrayList&lt;&gt;(), (list, item) -&gt; list.add(item)))
     * );
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scan} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the initial, accumulator and result type
     * @param initialValue
     *            the initial (seed) accumulator item
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return an Observable that emits {@code initialValue} followed by the results of each call to the
     *         accumulator function
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> scan(@NonNull R initialValue, @NonNull BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(initialValue, "initialValue is null");
        return scanWith(Functions.justSupplier(initialValue), accumulator);
    }

    /**
     * Returns an Observable that applies a specified accumulator function to the first item emitted by a source
     * ObservableSource and a seed value, then feeds the result of that function along with the second item emitted by
     * the source ObservableSource into the same function, and so on until all items have been emitted by the source
     * ObservableSource, emitting the result of each of these iterations.
     * <p>
     * <img width="640" height="320" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/scanSeed.png" alt="">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that the ObservableSource that results from this method will emit the value returned
     * by the {@code seedSupplier} as its first item.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code scanWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the initial, accumulator and result type
     * @param seedSupplier
     *            a Supplier that returns the initial (seed) accumulator item for each individual Observer
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source ObservableSource, whose
     *            result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the
     *            next accumulator call
     * @return an Observable that emits {@code initialValue} followed by the results of each call to the
     *         accumulator function
     * @see <a href="http://reactivex.io/documentation/operators/scan.html">ReactiveX operators documentation: Scan</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> scanWith(@NonNull Supplier<R> seedSupplier, @NonNull BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(seedSupplier, "seedSupplier is null");
        Objects.requireNonNull(accumulator, "accumulator is null");
        return RxJavaPlugins.onAssembly(new ObservableScanSeed<>(this, seedSupplier, accumulator));
    }

    /**
     * Forces an ObservableSource's emissions and notifications to be serialized and for it to obey
     * <a href="http://reactivex.io/documentation/contract.html">the ObservableSource contract</a> in other ways.
     * <p>
     * It is possible for an ObservableSource to invoke its Observers' methods asynchronously, perhaps from
     * different threads. This could make such an ObservableSource poorly-behaved, in that it might try to invoke
     * {@code onComplete} or {@code onError} before one of its {@code onNext} invocations, or it might call
     * {@code onNext} from two different threads concurrently. You can force such an ObservableSource to be
     * well-behaved and sequential by applying the {@code serialize} method to it.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/synchronize.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code serialize} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an {@link ObservableSource} that is guaranteed to be well-behaved and to make only serialized calls to
     *         its observers
     * @see <a href="http://reactivex.io/documentation/operators/serialize.html">ReactiveX operators documentation: Serialize</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> serialize() {
        return RxJavaPlugins.onAssembly(new ObservableSerialized<>(this));
    }

    /**
     * Returns a new {@link ObservableSource} that multicasts (and shares a single subscription to) the original {@link ObservableSource}. As long as
     * there is at least one {@link Observer} this {@link ObservableSource} will be subscribed and emitting data.
     * When all subscribers have disposed it will dispose the source {@link ObservableSource}.
     * <p>
     * This is an alias for {@link #publish()}.{@link ConnectableObservable#refCount() refCount()}.
     * <p>
     * <img width="640" height="510" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/publishRefCount.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code share} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an {@code ObservableSource} that upon connection causes the source {@code ObservableSource} to emit items
     *         to its {@link Observer}s
     * @see <a href="http://reactivex.io/documentation/operators/refcount.html">ReactiveX operators documentation: RefCount</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> share() {
        return publish().refCount();
    }

    /**
     * Returns a Maybe that completes if this Observable is empty or emits the single item emitted by this Observable,
     * or signals an {@code IllegalArgumentException} if this Observable emits more than one item.
     * <p>
     * <img width="640" height="217" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/singleElement.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleElement} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Maybe} that emits the single item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Maybe<T> singleElement() {
        return RxJavaPlugins.onAssembly(new ObservableSingleMaybe<>(this));
    }

    /**
     * Returns a Single that emits the single item emitted by this Observable, if this Observable
     * emits only a single item, or a default item if the source ObservableSource emits no items. If the source
     * ObservableSource emits more than one item, an {@code IllegalArgumentException} is signalled instead.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/single.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code single} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param defaultItem
     *            a default value to emit if the source ObservableSource emits no item
     * @return the new Single instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> single(@NonNull T defaultItem) {
        Objects.requireNonNull(defaultItem, "defaultItem is null");
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<>(this, defaultItem));
    }

    /**
     * Returns a Single that emits the single item emitted by this Observable if this Observable
     * emits only a single item, otherwise
     * if this Observable completes without emitting any items or emits more than one item a
     * {@link NoSuchElementException} or {@code IllegalArgumentException} will be signalled respectively.
     * <p>
     * <img width="640" height="228" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrError.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleOrError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Single instance
     * @see <a href="http://reactivex.io/documentation/operators/first.html">ReactiveX operators documentation: First</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<T> singleOrError() {
        return RxJavaPlugins.onAssembly(new ObservableSingleSingle<>(this, null));
    }

    /**
     * Returns an Observable that skips the first {@code count} items emitted by the source ObservableSource and emits
     * the remainder.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code skip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the number of items to skip
     * @return an Observable that is identical to the source ObservableSource except that it does not emit the first
     *         {@code count} items that the source ObservableSource emits
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skip(long count) {
        if (count <= 0) {
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new ObservableSkip<>(this, count));
    }

    /**
     * Returns an Observable that skips values emitted by the source ObservableSource before a specified time window
     * elapses.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skip} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window to skip
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that skips values emitted by the source ObservableSource before the time window defined
     *         by {@code time} elapses and the emits the remainder
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> skip(long time, @NonNull TimeUnit unit) {
        return skipUntil(timer(time, unit));
    }

    /**
     * Returns an Observable that skips values emitted by the source ObservableSource before a specified time window
     * on a specified {@link Scheduler} elapses.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skip.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use for the timed skipping</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window to skip
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@link Scheduler} on which the timed wait happens
     * @return an Observable that skips values emitted by the source ObservableSource before the time window defined
     *         by {@code time} and {@code scheduler} elapses, and then emits the remainder
     * @see <a href="http://reactivex.io/documentation/operators/skip.html">ReactiveX operators documentation: Skip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skip(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return skipUntil(timer(time, unit, scheduler));
    }

    /**
     * Returns an Observable that drops a specified number of items from the end of the sequence emitted by the
     * source ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.png" alt="">
     * <p>
     * This Observer accumulates a queue long enough to store the first {@code count} items. As more items are
     * received, items are taken from the front of the queue and emitted by the returned ObservableSource. This causes
     * such items to be delayed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code skipLast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            number of items to drop from the end of the source sequence
     * @return an Observable that emits the items emitted by the source ObservableSource except for the dropped ones
     *         at the end
     * @throws IndexOutOfBoundsException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skipLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new ObservableSkipLast<>(this, count));
    }

    /**
     * Returns an Observable that drops items emitted by the source ObservableSource during a specified time window
     * before the source completes.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.t.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that drops those items emitted by the source ObservableSource in a time window before the
     *         source completes defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit) {
        return skipLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an Observable that drops items emitted by the source ObservableSource during a specified time window
     * before the source completes.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.t.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @return an Observable that drops those items emitted by the source ObservableSource in a time window before the
     *         source completes defined by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, boolean delayError) {
        return skipLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }

    /**
     * Returns an Observable that drops items emitted by the source ObservableSource during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @return an Observable that drops those items emitted by the source ObservableSource in a time window before the
     *         source completes defined by {@code time} and {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return skipLast(time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an Observable that drops items emitted by the source ObservableSource during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use to track the current time</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @return an Observable that drops those items emitted by the source ObservableSource in a time window before the
     *         source completes defined by {@code time} and {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        return skipLast(time, unit, scheduler, delayError, bufferSize());
    }

    /**
     * Returns an Observable that drops items emitted by the source ObservableSource during a specified time window
     * (defined on a specified scheduler) before the source completes.
     * <p>
     * <img width="640" height="340" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipLast.ts.png" alt="">
     * <p>
     * Note: this action will cache the latest items arriving in the specified time window.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the scheduler used as the time source
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be skipped
     * @return an Observable that drops those items emitted by the source ObservableSource in a time window before the
     *         source completes defined by {@code time} and {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/skiplast.html">ReactiveX operators documentation: SkipLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> skipLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        // the internal buffer holds pairs of (timestamp, value) so double the default buffer size
        int s = bufferSize << 1;
        return RxJavaPlugins.onAssembly(new ObservableSkipLastTimed<>(this, time, unit, scheduler, s, delayError));
    }

    /**
     * Returns an Observable that skips items emitted by the source ObservableSource until a second ObservableSource emits
     * an item.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the other ObservableSource
     * @param other
     *            the second ObservableSource that has to emit an item before the source ObservableSource's elements begin
     *            to be mirrored by the resulting ObservableSource
     * @return an Observable that skips items from the source ObservableSource until the second ObservableSource emits an
     *         item, then emits the remaining items
     * @see <a href="http://reactivex.io/documentation/operators/skipuntil.html">ReactiveX operators documentation: SkipUntil</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> skipUntil(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableSkipUntil<>(this, other));
    }

    /**
     * Returns an Observable that skips all items emitted by the source ObservableSource as long as a specified
     * condition holds true, but emits all further source items as soon as the condition becomes false.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/skipWhile.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code skipWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function to test each item emitted from the source ObservableSource
     * @return an Observable that begins emitting items emitted by the source ObservableSource when the specified
     *         predicate becomes false
     * @see <a href="http://reactivex.io/documentation/operators/skipwhile.html">ReactiveX operators documentation: SkipWhile</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> skipWhile(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableSkipWhile<>(this, predicate));
    }

    /**
     * Returns an Observable that emits the events emitted by source ObservableSource, in a
     * sorted order. Each item emitted by the ObservableSource must implement {@link Comparable} with respect to all
     * other items in the sequence.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/sorted.png" alt="">
     * <p>
     * If any item emitted by this Observable does not implement {@link Comparable} with respect to
     * all other items emitted by this Observable, no items will be emitted and the
     * sequence is terminated with a {@link ClassCastException}.
     *
     * <p>Note that calling {@code sorted} with long, non-terminating or infinite sources
     * might cause {@link OutOfMemoryError}
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return an Observable that emits the items emitted by the source ObservableSource in sorted order
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> sorted() {
        return toList().toObservable().map(Functions.listSorter(Functions.naturalComparator())).flatMapIterable(Functions.identity());
    }

    /**
     * Returns an Observable that emits the events emitted by source ObservableSource, in a
     * sorted order based on a specified comparison function.
     *
     * <p>Note that calling {@code sorted} with long, non-terminating or infinite sources
     * might cause {@link OutOfMemoryError}
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code sorted} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param sortFunction
     *            a function that compares two items emitted by the source ObservableSource and returns an Integer
     *            that indicates their sort order
     * @return an Observable that emits the items emitted by the source ObservableSource in sorted order
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> sorted(@NonNull Comparator<? super T> sortFunction) {
        Objects.requireNonNull(sortFunction, "sortFunction is null");
        return toList().toObservable().map(Functions.listSorter(sortFunction)).flatMapIterable(Functions.identity());
    }

    /**
     * Returns an Observable that emits the items in a specified {@link Iterable} before it begins to emit items
     * emitted by the source ObservableSource.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithIterable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            an Iterable that contains the items you want the modified ObservableSource to emit first
     * @return an Observable that emits the items in the specified {@link Iterable} and then emits the items
     *         emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @since 3.0.0
     * @see #startWithItem(Object)
     * @see #startWithArray(Object...)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWithIterable(@NonNull Iterable<? extends T> items) {
        return concatArray(fromIterable(items), this);
    }

    /**
     * Returns an Observable that emits the items in a specified {@link ObservableSource} before it begins to emit
     * items emitted by the source ObservableSource.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            an ObservableSource that contains the items you want the modified ObservableSource to emit first
     * @return an Observable that emits the items in the specified {@link ObservableSource} and then emits the items
     *         emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWith(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concatArray(other, this);
    }

    /**
     * Returns an Observable that emits a specified item before it begins to emit items emitted by the source
     * ObservableSource.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWith.item.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithItem} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param item
     *            the item to emit first
     * @return an Observable that emits the specified item before it begins to emit items emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @see #startWithArray(Object...)
     * @see #startWithIterable(Iterable)
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> startWithItem(@NonNull T item) {
        Objects.requireNonNull(item, "item is null");
        return concatArray(just(item), this);
    }

    /**
     * Returns an Observable that emits the specified items before it begins to emit items emitted by the source
     * ObservableSource.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startWithArray.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startWithArray} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param items
     *            the array of values to emit first
     * @return an Observable that emits the specified items before it begins to emit items emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/startwith.html">ReactiveX operators documentation: StartWith</a>
     * @see #startWithItem(Object)
     * @see #startWithIterable(Iterable)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @SafeVarargs
    @NonNull
    public final Observable<T> startWithArray(@NonNull T... items) {
        Observable<T> fromArray = fromArray(items);
        if (fromArray == empty()) {
            return RxJavaPlugins.onAssembly(this);
        }
        return concatArray(fromArray, this);
    }

    /**
     * Subscribes to an ObservableSource and ignores {@code onNext} and {@code onComplete} emissions.
     * <p>
     * If the Observable emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the ObservableSource has finished sending them
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to an ObservableSource and provides a callback to handle the items it emits.
     * <p>
     * If the Observable emits an error, it is wrapped into an
     * {@link io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the RxJavaPlugins.onError handler.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the ObservableSource
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the ObservableSource has finished sending them
     * @throws NullPointerException
     *             if {@code onNext} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext) {
        return subscribe(onNext, Functions.ON_ERROR_MISSING, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to an ObservableSource and provides callbacks to handle the items it emits and any error
     * notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the ObservableSource
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             ObservableSource
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the ObservableSource has finished sending them
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws NullPointerException
     *             if {@code onNext} is null, or
     *             if {@code onError} is null
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to an ObservableSource and provides callbacks to handle the items it emits and any error or
     * completion notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param onNext
     *             the {@code Consumer<T>} you have designed to accept emissions from the ObservableSource
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             ObservableSource
     * @param onComplete
     *             the {@code Action} you have designed to accept a completion notification from the
     *             ObservableSource
     * @return a {@link Disposable} reference with which the caller can stop receiving items before
     *         the ObservableSource has finished sending them
     * @throws NullPointerException
     *             if {@code onNext} is null, or
     *             if {@code onError} is null, or
     *             if {@code onComplete} is null
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Disposable subscribe(@NonNull Consumer<? super T> onNext, @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        LambdaObserver<T> ls = new LambdaObserver<>(onNext, onError, onComplete, Functions.emptyConsumer());

        subscribe(ls);

        return ls;
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);

            Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null Observer. Please change the handler provided to RxJavaPlugins.setOnObservableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

            subscribeActual(observer);
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }

    /**
     * Operator implementations (both source and intermediate) should implement this method that
     * performs the necessary business logic and handles the incoming {@link Observer}s.
     * <p>There is no need to call any of the plugin hooks on the current {@code Observable} instance or
     * the {@code Observer}; all hooks and basic safeguards have been
     * applied by {@link #subscribe(Observer)} before this method gets called.
     * @param observer the incoming Observer, never null
     */
    protected abstract void subscribeActual(@NonNull Observer<? super T> observer);

    /**
     * Subscribes a given Observer (subclass) to this Observable and returns the given
     * Observer as is.
     * <p>Usage example:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10);
     * CompositeDisposable composite = new CompositeDisposable();
     *
     * DisposableObserver&lt;Integer&gt; ds = new DisposableObserver&lt;&gt;() {
     *     // ...
     * };
     *
     * composite.add(source.subscribeWith(ds));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <E> the type of the Observer to use and return
     * @param observer the Observer (subclass) to use and return, not null
     * @return the input {@code observer}
     * @throws NullPointerException if {@code observer} is null
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull E extends Observer<? super T>> E subscribeWith(E observer) {
        subscribe(observer);
        return observer;
    }

    /**
     * Asynchronously subscribes Observers to this ObservableSource on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/subscribeOn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the source ObservableSource modified so that its subscriptions happen on the
     *         specified {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> subscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSubscribeOn<>(this, scheduler));
    }

    /**
     * Returns an Observable that emits the items emitted by the source ObservableSource or the items of an alternate
     * ObservableSource if the source ObservableSource is empty.
     * <p>
     * <img width="640" height="255" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchifempty.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchIfEmpty} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *              the alternate ObservableSource to subscribe to if the source does not emit any items
     * @return  an ObservableSource that emits the items emitted by the source ObservableSource or the items of an
     *          alternate ObservableSource if the source ObservableSource is empty.
     * @since 1.1.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> switchIfEmpty(@NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchIfEmpty<>(this, other));
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns an ObservableSource, and then emitting the items emitted by the most recently emitted
     * of these ObservableSources.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner ObservableSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the inner ObservableSource is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner ObservableSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @return an Observable that emits the items emitted by the ObservableSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapDelayError(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return switchMap(mapper, bufferSize());
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns an ObservableSource, and then emitting the items emitted by the most recently emitted
     * of these ObservableSources.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner ObservableSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the inner ObservableSource is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner ObservableSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param bufferSize
     *            the number of elements to prefetch from the current active inner ObservableSource
     * @return an Observable that emits the items emitted by the ObservableSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapDelayError(Function, int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap<>(this, mapper, bufferSize, false));
    }

    /**
     * Maps the upstream values into {@link CompletableSource}s, subscribes to the newer one while
     * disposing the subscription to the previous {@code CompletableSource}, thus keeping at most one
     * active {@code CompletableSource} running.
     * <p>
     * <img width="640" height="521" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapCompletable.f.png" alt="">
     * <p>
     * Since a {@code CompletableSource} doesn't produce any items, the resulting reactive type of
     * this operator is a {@link Completable} that can only indicate successful completion or
     * a failure in any of the inner {@code CompletableSource}s or the failure of the current
     * {@link Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapCompletable} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>If either this {@code Observable} or the active {@code CompletableSource} signals an {@code onError},
     *  the resulting {@code Completable} is terminated immediately with that {@code Throwable}.
     *  Use the {@link #switchMapCompletableDelayError(Function)} to delay such inner failures until
     *  every inner {@code CompletableSource}s and the main {@code Observable} terminates in some fashion.
     *  If they fail concurrently, the operator may combine the {@code Throwable}s into a
     *  {@link io.reactivex.rxjava3.exceptions.CompositeException CompositeException}
     *  and signal it to the downstream instead. If any inactivated (switched out) {@code CompletableSource}
     *  signals an {@code onError} late, the {@code Throwable}s will be signalled to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors.
     *  </dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with each upstream item and should return a
     *               {@link CompletableSource} to be subscribed to and awaited for
     *               (non blockingly) for its terminal event
     * @return the new Completable instance
     * @see #switchMapCompletableDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable switchMapCompletable(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapCompletable<>(this, mapper, false));
    }

    /**
     * Maps the upstream values into {@link CompletableSource}s, subscribes to the newer one while
     * disposing the subscription to the previous {@code CompletableSource}, thus keeping at most one
     * active {@code CompletableSource} running and delaying any main or inner errors until all
     * of them terminate.
     * <p>
     * <img width="640" height="453" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapCompletableDelayError.f.png" alt="">
     * <p>
     * Since a {@code CompletableSource} doesn't produce any items, the resulting reactive type of
     * this operator is a {@link Completable} that can only indicate successful completion or
     * a failure in any of the inner {@code CompletableSource}s or the failure of the current
     * {@link Observable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapCompletableDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>Errors of this {@code Observable} and all the {@code CompletableSource}s, who had the chance
     *  to run to their completion, are delayed until
     *  all of them terminate in some fashion. At this point, if there was only one failure, the respective
     *  {@code Throwable} is emitted to the downstream. It there were more than one failures, the
     *  operator combines all {@code Throwable}s into a {@link io.reactivex.rxjava3.exceptions.CompositeException CompositeException}
     *  and signals that to the downstream.
     *  If any inactivated (switched out) {@code CompletableSource}
     *  signals an {@code onError} late, the {@code Throwable}s will be signalled to the global error handler via
     *  {@link RxJavaPlugins#onError(Throwable)} method as {@code UndeliverableException} errors.
     *  </dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param mapper the function called with each upstream item and should return a
     *               {@link CompletableSource} to be subscribed to and awaited for
     *               (non blockingly) for its terminal event
     * @return the new Completable instance
     * @see #switchMapCompletable(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Completable switchMapCompletableDelayError(@NonNull Function<? super T, ? extends CompletableSource> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapCompletable<>(this, mapper, true));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and switches (subscribes) to the newer ones
     * while disposing the older ones (and ignoring their signals) and emits the latest success value of the current one if
     * available while failing immediately if this {@code Observable} or any of the
     * active inner {@code MaybeSource}s fail.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapMaybe} does not operate by default on a particular {@link Scheduler}.</dd>
     *  <dt><b>Error handling:</b></dt>
     *  <dd>This operator terminates with an {@code onError} if this {@code Observable} or any of
     *  the inner {@code MaybeSource}s fail while they are active. When this happens concurrently, their
     *  individual {@code Throwable} errors may get combined and emitted as a single
     *  {@link io.reactivex.rxjava3.exceptions.CompositeException CompositeException}. Otherwise, a late
     *  (i.e., inactive or switched out) {@code onError} from this {@code Observable} or from any of
     *  the inner {@code MaybeSource}s will be forwarded to the global error handler via
     *  {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)} as
     *  {@link io.reactivex.rxjava3.exceptions.UndeliverableException UndeliverableException}</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the output value type
     * @param mapper the function called with the current upstream event and should
     *               return a {@code MaybeSource} to replace the current active inner source
     *               and get subscribed to.
     * @return the new Observable instance
     * @see #switchMapMaybeDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapMaybe(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapMaybe<>(this, mapper, false));
    }

    /**
     * Maps the upstream items into {@link MaybeSource}s and switches (subscribes) to the newer ones
     * while disposing the older ones  (and ignoring their signals) and emits the latest success value of the current one if
     * available, delaying errors from this {@code Observable} or the inner {@code MaybeSource}s until all terminate.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapMaybeDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.11 - experimental
     * @param <R> the output value type
     * @param mapper the function called with the current upstream event and should
     *               return a {@code MaybeSource} to replace the current active inner source
     *               and get subscribed to.
     * @return the new Observable instance
     * @see #switchMapMaybe(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapMaybeDelayError(@NonNull Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapMaybe<>(this, mapper, true));
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns a SingleSource, and then emitting the item emitted by the most recently emitted
     * of these SingleSources.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner SingleSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the inner SingleSource is disposed and the error delivered in-sequence.
     * <p>
     * <img width="640" height="531" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapSingle.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapSingle} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the element type of the inner SingleSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns a
     *            SingleSource
     * @return an Observable that emits the item emitted by the SingleSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapSingleDelayError(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapSingle(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapSingle<>(this, mapper, false));
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns a SingleSource, and then emitting the item emitted by the most recently emitted
     * of these SingleSources and delays any error until all SingleSources terminate.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner SingleSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the termination of the last inner SingleSource will emit that error as is
     * or wrapped into a CompositeException along with the other possible errors the former inner SingleSources signalled.
     * <p>
     * <img width="640" height="467" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMapSingleDelayError.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapSingleDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.0.8 - experimental
     * @param <R> the element type of the inner SingleSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns a
     *            SingleSource
     * @return an Observable that emits the item emitted by the SingleSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMapSingle(Function)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapSingleDelayError(@NonNull Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchMapSingle<>(this, mapper, true));
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns an ObservableSource, and then emitting the items emitted by the most recently emitted
     * of these ObservableSources and delays any error until all ObservableSources terminate.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner ObservableSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the termination of the last inner ObservableSource will emit that error as is
     * or wrapped into a CompositeException along with the other possible errors the former inner ObservableSources signalled.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner ObservableSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @return an Observable that emits the items emitted by the ObservableSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMap(Function)
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return switchMapDelayError(mapper, bufferSize());
    }

    /**
     * Returns a new ObservableSource by applying a function that you supply to each item emitted by the source
     * ObservableSource that returns an ObservableSource, and then emitting the items emitted by the most recently emitted
     * of these ObservableSources and delays any error until all ObservableSources terminate.
     * <p>
     * The resulting ObservableSource completes if both the upstream ObservableSource and the last inner ObservableSource, if any, complete.
     * If the upstream ObservableSource signals an onError, the termination of the last inner ObservableSource will emit that error as is
     * or wrapped into a CompositeException along with the other possible errors the former inner ObservableSources signalled.
     * <p>
     * <img width="640" height="350" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchMap.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code switchMapDelayError} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the inner ObservableSources and the output
     * @param mapper
     *            a function that, when applied to an item emitted by the source ObservableSource, returns an
     *            ObservableSource
     * @param bufferSize
     *            the number of elements to prefetch from the current active inner ObservableSource
     * @return an Observable that emits the items emitted by the ObservableSource returned from applying {@code func} to the most recently emitted item emitted by the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     * @see #switchMap(Function, int)
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> switchMapDelayError(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableSwitchMap<>(this, mapper, bufferSize, true));
    }

    /**
     * Returns an Observable that emits only the first {@code count} items emitted by the source ObservableSource. If the source emits fewer than
     * {@code count} items then all of its items are emitted.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.png" alt="">
     * <p>
     * This method returns an ObservableSource that will invoke a subscribing {@link Observer}'s
     * {@link Observer#onNext onNext} function a maximum of {@code count} times before invoking
     * {@link Observer#onComplete onComplete}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code take} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @return an Observable that emits only the first {@code count} items emitted by the source ObservableSource, or
     *         all of the items from the source ObservableSource if that ObservableSource emits fewer than {@code count} items
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> take(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= 0 required but it was " + count);
        }
        return RxJavaPlugins.onAssembly(new ObservableTake<>(this, count));
    }

    /**
     * Returns an Observable that emits those items emitted by source ObservableSource before a specified time runs
     * out.
     * <p>
     * If time runs out before the {@code Observable} completes normally, the {@code onComplete} event will be
     * signaled on the default {@code computation} {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code take} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that emits those items emitted by the source ObservableSource before the time runs out
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> take(long time, @NonNull TimeUnit unit) {
        return takeUntil(timer(time, unit));
    }

    /**
     * Returns an Observable that emits those items emitted by source ObservableSource before a specified time (on a
     * specified Scheduler) runs out.
     * <p>
     * If time runs out before the {@code Observable} completes normally, the {@code onComplete} event will be
     * signaled on the provided {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/take.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler used for time source
     * @return an Observable that emits those items emitted by the source ObservableSource before the time runs out,
     *         according to the specified Scheduler
     * @see <a href="http://reactivex.io/documentation/operators/take.html">ReactiveX operators documentation: Take</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> take(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeUntil(timer(time, unit, scheduler));
    }

    /**
     * Returns an Observable that emits at most the last {@code count} items emitted by the source ObservableSource. If the source emits fewer than
     * {@code count} items then all of its items are emitted.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.n.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code takeLast} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit from the end of the sequence of items emitted by the source
     *            ObservableSource
     * @return an Observable that emits at most the last {@code count} items emitted by the source ObservableSource
     * @throws IndexOutOfBoundsException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count >= 0 required but it was " + count);
        }
        if (count == 0) {
            return RxJavaPlugins.onAssembly(new ObservableIgnoreElements<>(this));
        }
        if (count == 1) {
            return RxJavaPlugins.onAssembly(new ObservableTakeLastOne<>(this));
        }
        return RxJavaPlugins.onAssembly(new ObservableTakeLast<>(this, count));
    }

    /**
     * Returns an Observable that emits at most a specified number of items from the source ObservableSource that were
     * emitted in a specified window of time before the ObservableSource completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tn.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeLast} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that emits at most {@code count} items from the source ObservableSource that were emitted
     *         in a specified window of time before the ObservableSource completed
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit) {
        return takeLast(count, time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an Observable that emits at most a specified number of items from the source ObservableSource that were
     * emitted in a specified window of time before the ObservableSource completed, where the timing information is
     * provided by a given Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tns.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@link Scheduler} that provides the timestamps for the observed items
     * @return an Observable that emits at most {@code count} items from the source ObservableSource that were emitted
     *         in a specified window of time before the ObservableSource completed, where the timing information is
     *         provided by the given {@code scheduler}
     * @throws IndexOutOfBoundsException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeLast(count, time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an Observable that emits at most a specified number of items from the source ObservableSource that were
     * emitted in a specified window of time before the ObservableSource completed, where the timing information is
     * provided by a given Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.tns.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use for tracking the current time</dd>
     * </dl>
     *
     * @param count
     *            the maximum number of items to emit
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the {@link Scheduler} that provides the timestamps for the observed items
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be last
     * @return an Observable that emits at most {@code count} items from the source ObservableSource that were emitted
     *         in a specified window of time before the ObservableSource completed, where the timing information is
     *         provided by the given {@code scheduler}
     * @throws IndexOutOfBoundsException
     *             if {@code count} is less than zero
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long count, long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (count < 0) {
            throw new IndexOutOfBoundsException("count >= 0 required but it was " + count);
        }
        return RxJavaPlugins.onAssembly(new ObservableTakeLastTimed<>(this, count, time, unit, scheduler, bufferSize, delayError));
    }

    /**
     * Returns an Observable that emits the items from the source ObservableSource that were emitted in a specified
     * window of time before the ObservableSource completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code takeLast} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @return an Observable that emits the items from the source ObservableSource that were emitted in the window of
     *         time before the ObservableSource completed specified by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit) {
        return takeLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    /**
     * Returns an Observable that emits the items from the source ObservableSource that were emitted in a specified
     * window of time before the ObservableSource completed.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.t.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code takeLast} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @return an Observable that emits the items from the source ObservableSource that were emitted in the window of
     *         time before the ObservableSource completed specified by {@code time}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.TRAMPOLINE)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, boolean delayError) {
        return takeLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }

    /**
     * Returns an Observable that emits the items from the source ObservableSource that were emitted in a specified
     * window of time before the ObservableSource completed, where the timing information is provided by a specified
     * Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that provides the timestamps for the Observed items
     * @return an Observable that emits the items from the source ObservableSource that were emitted in the window of
     *         time before the ObservableSource completed specified by {@code time}, where the timing information is
     *         provided by {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return takeLast(time, unit, scheduler, false, bufferSize());
    }

    /**
     * Returns an Observable that emits the items from the source ObservableSource that were emitted in a specified
     * window of time before the ObservableSource completed, where the timing information is provided by a specified
     * Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that provides the timestamps for the Observed items
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @return an Observable that emits the items from the source ObservableSource that were emitted in the window of
     *         time before the ObservableSource completed specified by {@code time}, where the timing information is
     *         provided by {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError) {
        return takeLast(time, unit, scheduler, delayError, bufferSize());
    }

    /**
     * Returns an Observable that emits the items from the source ObservableSource that were emitted in a specified
     * window of time before the ObservableSource completed, where the timing information is provided by a specified
     * Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeLast.ts.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param time
     *            the length of the time window
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler
     *            the Scheduler that provides the timestamps for the Observed items
     * @param delayError
     *            if true, an exception signalled by the current Observable is delayed until the regular elements are consumed
     *            by the downstream; if false, an exception is immediately signalled and all regular elements dropped
     * @param bufferSize
     *            the hint about how many elements to expect to be last
     * @return an Observable that emits the items from the source ObservableSource that were emitted in the window of
     *         time before the ObservableSource completed specified by {@code time}, where the timing information is
     *         provided by {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/takelast.html">ReactiveX operators documentation: TakeLast</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> takeLast(long time, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        return takeLast(Long.MAX_VALUE, time, unit, scheduler, delayError, bufferSize);
    }

    /**
     * Returns an Observable that emits the items emitted by the source Observable until a second ObservableSource
     * emits an item.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param other
     *            the ObservableSource whose first emitted item will cause {@code takeUntil} to stop emitting items
     *            from the source Observable
     * @param <U>
     *            the type of items emitted by {@code other}
     * @return an Observable that emits the items emitted by the source Observable until such time as {@code other} emits its first item
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U> Observable<T> takeUntil(@NonNull ObservableSource<U> other) {
        Objects.requireNonNull(other, "other is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeUntil<>(this, other));
    }

    /**
     * Returns an Observable that emits items emitted by the source Observable, checks the specified predicate
     * for each item, and then completes when the condition is satisfied.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeUntil.p.png" alt="">
     * <p>
     * The difference between this operator and {@link #takeWhile(Predicate)} is that here, the condition is
     * evaluated <em>after</em> the item is emitted.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeUntil} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param stopPredicate
     *            a function that evaluates an item emitted by the source Observable and returns a Boolean
     * @return an Observable that first emits items emitted by the source Observable, checks the specified
     *         condition after each item, and then completes when the condition is satisfied.
     * @see <a href="http://reactivex.io/documentation/operators/takeuntil.html">ReactiveX operators documentation: TakeUntil</a>
     * @see Observable#takeWhile(Predicate)
     * @since 1.1.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeUntil(@NonNull Predicate<? super T> stopPredicate) {
        Objects.requireNonNull(stopPredicate, "stopPredicate is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeUntilPredicate<>(this, stopPredicate));
    }

    /**
     * Returns an Observable that emits items emitted by the source ObservableSource so long as each item satisfied a
     * specified condition, and then completes as soon as this condition is not satisfied.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/takeWhile.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code takeWhile} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates an item emitted by the source ObservableSource and returns a Boolean
     * @return an Observable that emits the items from the source ObservableSource so long as each item satisfies the
     *         condition defined by {@code predicate}, then completes
     * @see <a href="http://reactivex.io/documentation/operators/takewhile.html">ReactiveX operators documentation: TakeWhile</a>
     * @see Observable#takeUntil(Predicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<T> takeWhile(@NonNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return RxJavaPlugins.onAssembly(new ObservableTakeWhile<>(this, predicate));
    }

    /**
     * Returns an Observable that emits only the first item emitted by the source ObservableSource during sequential
     * time windows of a specified duration.
     * <p>
     * This differs from {@link #throttleLast} in that this only tracks passage of time whereas
     * {@link #throttleLast} ticks at scheduled intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleFirst} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param windowDuration
     *            time to wait before emitting another item after emitting the last item
     * @param unit
     *            the unit of time of {@code windowDuration}
     * @return an Observable that performs the throttle operation
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleFirst(long windowDuration, @NonNull TimeUnit unit) {
        return throttleFirst(windowDuration, unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits only the first item emitted by the source ObservableSource during sequential
     * time windows of a specified duration, where the windows are managed by a specified Scheduler.
     * <p>
     * This differs from {@link #throttleLast} in that this only tracks passage of time whereas
     * {@link #throttleLast} ticks at scheduled intervals.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param skipDuration
     *            time to wait before emitting another item after emitting the last item
     * @param unit
     *            the unit of time of {@code skipDuration}
     * @param scheduler
     *            the {@link Scheduler} to use internally to manage the timers that handle timeout for each
     *            event
     * @return an Observable that performs the throttle operation
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleFirst(long skipDuration, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableThrottleFirstTimed<>(this, skipDuration, unit, scheduler));
    }

    /**
     * Returns an Observable that emits only the last item emitted by the source ObservableSource during sequential
     * time windows of a specified duration.
     * <p>
     * This differs from {@link #throttleFirst} in that this ticks along at a scheduled interval whereas
     * {@link #throttleFirst} does not tick, it just tracks passage of time.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLast} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param intervalDuration
     *            duration of windows within which the last item emitted by the source ObservableSource will be
     *            emitted
     * @param unit
     *            the unit of time of {@code intervalDuration}
     * @return an Observable that performs the throttle operation
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #sample(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLast(long intervalDuration, @NonNull TimeUnit unit) {
        return sample(intervalDuration, unit);
    }

    /**
     * Returns an Observable that emits only the last item emitted by the source ObservableSource during sequential
     * time windows of a specified duration, where the duration is governed by a specified Scheduler.
     * <p>
     * This differs from {@link #throttleFirst} in that this ticks along at a scheduled interval whereas
     * {@link #throttleFirst} does not tick, it just tracks passage of time.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLast.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param intervalDuration
     *            duration of windows within which the last item emitted by the source ObservableSource will be
     *            emitted
     * @param unit
     *            the unit of time of {@code intervalDuration}
     * @param scheduler
     *            the {@link Scheduler} to use internally to manage the timers that handle timeout for each
     *            event
     * @return an Observable that performs the throttle operation
     * @see <a href="http://reactivex.io/documentation/operators/sample.html">ReactiveX operators documentation: Sample</a>
     * @see #sample(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLast(long intervalDuration, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return sample(intervalDuration, unit, scheduler);
    }

    /**
     * Throttles items from the upstream {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.png" alt="">
     * <p>
     * Unlike the option with {@link #throttleLatest(long, TimeUnit, boolean)}, the very last item being held back
     * (if any) is not emitted when the upstream completes.
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLatest} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @return the new Observable instance
     * @see #throttleLatest(long, TimeUnit, boolean)
     * @see #throttleLatest(long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit) {
        return throttleLatest(timeout, unit, Schedulers.computation(), false);
    }

    /**
     * Throttles items from the upstream {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.e.png" alt="">
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleLatest} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param emitLast If {@code true}, the very last item from the upstream will be emitted
     *                 immediately when the upstream completes, regardless if there is
     *                 a timeout window active or not. If {@code false}, the very last
     *                 upstream item is ignored and the flow terminates.
     * @return the new Observable instance
     * @see #throttleLatest(long, TimeUnit, Scheduler, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, boolean emitLast) {
        return throttleLatest(timeout, unit, Schedulers.computation(), emitLast);
    }

    /**
     * Throttles items from the upstream {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.s.png" alt="">
     * <p>
     * Unlike the option with {@link #throttleLatest(long, TimeUnit, Scheduler, boolean)}, the very last item being held back
     * (if any) is not emitted when the upstream completes.
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param scheduler the {@link Scheduler} where the timed wait and latest item
     *                  emission will be performed
     * @return the new Observable instance
     * @see #throttleLatest(long, TimeUnit, Scheduler, boolean)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return throttleLatest(timeout, unit, scheduler, false);
    }

    /**
     * Throttles items from the upstream {@code Observable} by first emitting the next
     * item from upstream, then periodically emitting the latest item (if any) when
     * the specified timeout elapses between them.
     * <p>
     * <img width="640" height="325" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleLatest.se.png" alt="">
     * <p>
     * If no items were emitted from the upstream during this timeout phase, the next
     * upstream item is emitted immediately and the timeout window starts from then.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait after an item emission towards the downstream
     *                before trying to emit the latest item from upstream again
     * @param unit    the time unit
     * @param scheduler the {@link Scheduler} where the timed wait and latest item
     *                  emission will be performed
     * @param emitLast If {@code true}, the very last item from the upstream will be emitted
     *                 immediately when the upstream completes, regardless if there is
     *                 a timeout window active or not. If {@code false}, the very last
     *                 upstream item is ignored and the flow terminates.
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleLatest(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, boolean emitLast) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableThrottleLatest<>(this, timeout, unit, scheduler, emitLast));
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, except that it drops items emitted by the
     * source ObservableSource that are followed by newer items before a timeout value expires. The timer resets on
     * each emission (alias to {@link #debounce(long, TimeUnit, Scheduler)}).
     * <p>
     * <em>Note:</em> If items keep being emitted by the source ObservableSource faster than the timeout then no items
     * will be emitted by the resulting ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code throttleWithTimeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the source
     *            ObservableSource in which that ObservableSource emits no items in order for the item to be emitted by the
     *            resulting ObservableSource
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @return an Observable that filters out items from the source ObservableSource that are too quickly followed by
     *         newer items
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #debounce(long, TimeUnit)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> throttleWithTimeout(long timeout, @NonNull TimeUnit unit) {
        return debounce(timeout, unit);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, except that it drops items emitted by the
     * source ObservableSource that are followed by newer items before a timeout value expires on a specified
     * Scheduler. The timer resets on each emission (Alias to {@link #debounce(long, TimeUnit, Scheduler)}).
     * <p>
     * <em>Note:</em> If items keep being emitted by the source ObservableSource faster than the timeout then no items
     * will be emitted by the resulting ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleWithTimeout.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            the length of the window of time that must pass after the emission of an item from the source
     *            ObservableSource in which that ObservableSource emits no items in order for the item to be emitted by the
     *            resulting ObservableSource
     * @param unit
     *            the unit of time for the specified {@code timeout}
     * @param scheduler
     *            the {@link Scheduler} to use internally to manage the timers that handle the timeout for each
     *            item
     * @return an Observable that filters out items from the source ObservableSource that are too quickly followed by
     *         newer items
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">ReactiveX operators documentation: Debounce</a>
     * @see #debounce(long, TimeUnit, Scheduler)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> throttleWithTimeout(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return debounce(timeout, unit, scheduler);
    }

    /**
     * Returns an Observable that emits records of the time interval between consecutive items emitted by the
     * source ObservableSource.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits time interval information items
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timeInterval() {
        return timeInterval(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits records of the time interval between consecutive items emitted by the
     * source ObservableSource, where this interval is computed on a specified Scheduler.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@link Scheduler}.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} used to compute time intervals
     * @return an Observable that emits time interval information items
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull Scheduler scheduler) {
        return timeInterval(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Returns an Observable that emits records of the time interval between consecutive items emitted by the
     * source ObservableSource.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timeInterval} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @return an Observable that emits time interval information items
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull TimeUnit unit) {
        return timeInterval(unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits records of the time interval between consecutive items emitted by the
     * source ObservableSource, where this interval is computed on a specified Scheduler.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeInterval.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @param scheduler
     *            the {@link Scheduler} used to compute time intervals
     * @return an Observable that emits time interval information items
     * @see <a href="http://reactivex.io/documentation/operators/timeinterval.html">ReactiveX operators documentation: TimeInterval</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timeInterval(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeInterval<>(this, unit, scheduler));
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, but notifies observers of a
     * {@code TimeoutException} if an item emitted by the source ObservableSource doesn't arrive within a window of
     * time after the emission of the previous item, where that period of time is measured by an ObservableSource that
     * is a function of the previous item.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout3.png" alt="">
     * <p>
     * Note: The arrival of the first source item is never timed out.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <V>
     *            the timeout value type (ignored)
     * @param itemTimeoutIndicator
     *            a function that returns an ObservableSource for each item emitted by the source
     *            ObservableSource and that determines the timeout window for the subsequent item
     * @return an Observable that mirrors the source ObservableSource, but notifies observers of a
     *         {@code TimeoutException} if an item emitted by the source ObservableSource takes longer to arrive than
     *         the time window defined by the selector for the previously emitted item
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <V> Observable<T> timeout(@NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator) {
        return timeout0(null, itemTimeoutIndicator, null);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, but that switches to a fallback ObservableSource if
     * an item emitted by the source ObservableSource doesn't arrive within a window of time after the emission of the
     * previous item, where that period of time is measured by an ObservableSource that is a function of the previous
     * item.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout4.png" alt="">
     * <p>
     * Note: The arrival of the first source item is never timed out.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <V>
     *            the timeout value type (ignored)
     * @param itemTimeoutIndicator
     *            a function that returns an ObservableSource, for each item emitted by the source ObservableSource, that
     *            determines the timeout window for the subsequent item
     * @param other
     *            the fallback ObservableSource to switch to if the source ObservableSource times out
     * @return an Observable that mirrors the source ObservableSource, but switches to mirroring a fallback ObservableSource
     *         if an item emitted by the source ObservableSource takes longer to arrive than the time window defined
     *         by the selector for the previously emitted item
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <V> Observable<T> timeout(@NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(null, itemTimeoutIndicator, other);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting ObservableSource terminates and notifies observers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between emitted items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the source ObservableSource modified to notify observers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit timeUnit) {
        return timeout0(timeout, timeUnit, null, Schedulers.computation());
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the source ObservableSource is disposed and resulting ObservableSource begins instead
     * to mirror a fallback ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param other
     *            the fallback ObservableSource to use in case of a timeout
     * @return the source ObservableSource modified to switch to the fallback ObservableSource in case of a timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit timeUnit, @NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, timeUnit, other, Schedulers.computation());
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource but applies a timeout policy for each emitted
     * item using a specified Scheduler. If the next item isn't emitted within the specified timeout duration
     * starting from its predecessor, the source ObservableSource is disposed and resulting ObservableSource
     * begins instead to mirror a fallback ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.2s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the {@link Scheduler} to run the timeout timers on
     * @param other
     *            the ObservableSource to use as the fallback in case of a timeout
     * @return the source ObservableSource modified so that it will switch to the fallback ObservableSource in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit timeUnit, @NonNull Scheduler scheduler, @NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, timeUnit, other, scheduler);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource but applies a timeout policy for each emitted
     * item, where this policy is governed on a specified Scheduler. If the next item isn't emitted within the
     * specified timeout duration starting from its predecessor, the resulting ObservableSource terminates and
     * notifies observers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout.1s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the Scheduler to run the timeout timers on
     * @return the source ObservableSource modified to notify observers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> timeout(long timeout, @NonNull TimeUnit timeUnit, @NonNull Scheduler scheduler) {
        return timeout0(timeout, timeUnit, null, scheduler);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, but notifies observers of a
     * {@code TimeoutException} if either the first item emitted by the source ObservableSource or any subsequent item
     * doesn't arrive within time windows defined by other ObservableSources.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout5.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the first timeout value type (ignored)
     * @param <V>
     *            the subsequent timeout value type (ignored)
     * @param firstTimeoutIndicator
     *            a function that returns an ObservableSource that determines the timeout window for the first source
     *            item
     * @param itemTimeoutIndicator
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource and that
     *            determines the timeout window in which the subsequent source item must arrive in order to
     *            continue the sequence
     * @return an Observable that mirrors the source ObservableSource, but notifies observers of a
     *         {@code TimeoutException} if either the first item or any subsequent item doesn't arrive within
     *         the time windows specified by the timeout selectors
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<T> timeout(@NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator) {
        Objects.requireNonNull(firstTimeoutIndicator, "firstTimeoutIndicator is null");
        return timeout0(firstTimeoutIndicator, itemTimeoutIndicator, null);
    }

    /**
     * Returns an Observable that mirrors the source ObservableSource, but switches to a fallback ObservableSource if either
     * the first item emitted by the source ObservableSource or any subsequent item doesn't arrive within time windows
     * defined by other ObservableSources.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timeout6.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code timeout} operates by default on the {@code immediate} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the first timeout value type (ignored)
     * @param <V>
     *            the subsequent timeout value type (ignored)
     * @param firstTimeoutIndicator
     *            a function that returns an ObservableSource which determines the timeout window for the first source
     *            item
     * @param itemTimeoutIndicator
     *            a function that returns an ObservableSource for each item emitted by the source ObservableSource and that
     *            determines the timeout window in which the subsequent source item must arrive in order to
     *            continue the sequence
     * @param other
     *            the fallback ObservableSource to switch to if the source ObservableSource times out
     * @return an Observable that mirrors the source ObservableSource, but switches to the {@code other} ObservableSource if
     *         either the first item emitted by the source ObservableSource or any subsequent item doesn't arrive
     *         within time windows defined by the timeout selectors
     * @throws NullPointerException
     *             if {@code itemTimeoutIndicator} is null, or
     *             if {@code other} is null
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<T> timeout(
            @NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @NonNull ObservableSource<? extends T> other) {
        Objects.requireNonNull(firstTimeoutIndicator, "firstTimeoutIndicator is null");
        Objects.requireNonNull(other, "other is null");
        return timeout0(firstTimeoutIndicator, itemTimeoutIndicator, other);
    }

    @NonNull
    private Observable<T> timeout0(long timeout, @NonNull TimeUnit timeUnit,
            @Nullable ObservableSource<? extends T> other,
            @NonNull Scheduler scheduler) {
        Objects.requireNonNull(timeUnit, "timeUnit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeoutTimed<>(this, timeout, timeUnit, scheduler, other));
    }

    @NonNull
    private <U, V> Observable<T> timeout0(
            @NonNull ObservableSource<U> firstTimeoutIndicator,
            @NonNull Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            @Nullable ObservableSource<? extends T> other) {
        Objects.requireNonNull(itemTimeoutIndicator, "itemTimeoutIndicator is null");
        return RxJavaPlugins.onAssembly(new ObservableTimeout<>(this, firstTimeoutIndicator, itemTimeoutIndicator, other));
    }

    /**
     * Returns an Observable that emits each item emitted by the source ObservableSource, wrapped in a
     * {@link Timed} object.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @return an Observable that emits timestamped items from the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timestamp() {
        return timestamp(TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits each item emitted by the source ObservableSource, wrapped in a
     * {@link Timed} object whose timestamps are provided by a specified Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@link Scheduler}.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} to use as a time source
     * @return an Observable that emits timestamped items from the source ObservableSource with timestamps provided by
     *         the {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull Scheduler scheduler) {
        return timestamp(TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Returns an Observable that emits each item emitted by the source ObservableSource, wrapped in a
     * {@link Timed} object.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code timestamp} does not operate on any particular scheduler but uses the current time
     *  from the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @return an Observable that emits timestamped items from the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull TimeUnit unit) {
        return timestamp(unit, Schedulers.computation());
    }

    /**
     * Returns an Observable that emits each item emitted by the source ObservableSource, wrapped in a
     * {@link Timed} object whose timestamps are provided by a specified Scheduler.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/timestamp.s.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate on any particular scheduler but uses the current time
     *  from the specified {@link Scheduler}.</dd>
     * </dl>
     *
     * @param unit the time unit for the current time
     * @param scheduler
     *            the {@link Scheduler} to use as a time source
     * @return an Observable that emits timestamped items from the source ObservableSource with timestamps provided by
     *         the {@code scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/timestamp.html">ReactiveX operators documentation: Timestamp</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE) // Supplied scheduler is only used for creating timestamps.
    @NonNull
    public final Observable<Timed<T>> timestamp(@NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return map(Functions.timestampWith(unit, scheduler));
    }

    /**
     * Calls the specified converter function during assembly time and returns its resulting value.
     * <p>
     * This allows fluent conversion to any other type.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code to} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.7 - experimental
     * @param <R> the resulting object type
     * @param converter the function that receives the current Observable instance and returns a value
     * @return the converted value
     * @throws NullPointerException if converter is null
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> R to(@NonNull ObservableConverter<T, ? extends R> converter) {
        return Objects.requireNonNull(converter, "converter is null").apply(this);
    }

    /**
     * Returns a Single that emits a single item, a list composed of all the items emitted by the
     * finite source ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.2.png" alt="">
     * <p>
     * Normally, an ObservableSource that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior, instructing the
     * ObservableSource to compose a list of all of these items and then to invoke the Observer's {@code onNext}
     * function once, passing it the entire list, by calling the ObservableSource's {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a Single that emits a single item: a List containing all of the items emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toList() {
        return toList(16);
    }

    /**
     * Returns a Single that emits a single item, a list composed of all the items emitted by the
     * finite source ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.2.png" alt="">
     * <p>
     * Normally, an ObservableSource that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior, instructing the
     * ObservableSource to compose a list of all of these items and then to invoke the Observer's {@code onNext}
     * function once, passing it the entire list, by calling the ObservableSource's {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint
     *         the number of elements expected from the current Observable
     * @return a Single that emits a single item: a List containing all of the items emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toList(int capacityHint) {
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return RxJavaPlugins.onAssembly(new ObservableToListSingle<>(this, capacityHint));
    }

    /**
     * Returns a Single that emits a single item, a list composed of all the items emitted by the
     * finite source ObservableSource.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toList.o.c.png" alt="">
     * <p>
     * Normally, an ObservableSource that returns multiple items will do so by invoking its {@link Observer}'s
     * {@link Observer#onNext onNext} method for each such item. You can change this behavior, instructing the
     * ObservableSource to compose a list of all of these items and then to invoke the Observer's {@code onNext}
     * function once, passing it the entire list, by calling the ObservableSource's {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated collection to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the subclass of a collection of Ts
     * @param collectionSupplier
     *               the Supplier returning the collection (for each individual Observer) to be filled in
     * @return a Single that emits a single item: a List containing all of the items emitted by the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull U extends Collection<? super T>> Single<U> toList(@NonNull Supplier<U> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return RxJavaPlugins.onAssembly(new ObservableToListSingle<>(this, collectionSupplier));
    }

    /**
     * Returns a Single that emits a single HashMap containing all items emitted by the
     * finite source ObservableSource, mapped by the keys returned by a specified
     * {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.png" alt="">
     * <p>
     * If more than one source item maps to the same key, the HashMap will contain the latest of those items.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the HashMap
     * @return a Single that emits a single item: a HashMap containing the mapped items from the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Single<@NonNull Map<K, T>> toMap(@NonNull Function<? super T, ? extends K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return collect(HashMapSupplier.asSupplier(), Functions.toMapKeySelector(keySelector));
    }

    /**
     * Returns a Single that emits a single HashMap containing values corresponding to items emitted by the
     * finite source ObservableSource, mapped by the keys returned by a specified {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.png" alt="">
     * <p>
     * If more than one source item maps to the same key, the HashMap will contain a single entry that
     * corresponds to the latest of those items.
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the HashMap
     * @param valueSelector
     *            the function that extracts the value from a source item to be used in the HashMap
     * @return a Single that emits a single item: a HashMap containing the mapped items from the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Single<Map<K, V>> toMap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        return collect(HashMapSupplier.asSupplier(), Functions.toMapKeyValueSelector(keySelector, valueSelector));
    }

    /**
     * Returns a Single that emits a single Map, returned by a specified {@code mapFactory} function, that
     * contains keys and values extracted from the items emitted by the finite source ObservableSource.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMap.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @param keySelector
     *            the function that extracts the key from a source item to be used in the Map
     * @param valueSelector
     *            the function that extracts the value from the source items to be used as value in the Map
     * @param mapSupplier
     *            the function that returns a Map instance to be used
     * @return a Single that emits a single item: a Map that contains the mapped items emitted by the
     *         source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Single<Map<K, V>> toMap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<? extends Map<K, V>> mapSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        Objects.requireNonNull(mapSupplier, "mapSupplier is null");
        return collect(mapSupplier, Functions.toMapKeyValueSelector(keySelector, valueSelector));
    }

    /**
     * Returns a Single that emits a single HashMap that contains an ArrayList of items emitted by the
     * finite source ObservableSource keyed by a specified {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param keySelector
     *            the function that extracts the key from the source items to be used as key in the HashMap
     * @return a Single that emits a single item: a HashMap that contains an ArrayList of items mapped from
     *         the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K> Single<@NonNull Map<K, Collection<T>>> toMultimap(@NonNull Function<? super T, ? extends K> keySelector) {
        Function<? super T, ? extends T> valueSelector = Functions.identity();
        Supplier<Map<K, Collection<T>>> mapSupplier = HashMapSupplier.asSupplier();
        Function<K, List<T>> collectionFactory = ArrayListSupplier.asFunction();
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }

    /**
     * Returns a Single that emits a single HashMap that contains an ArrayList of values extracted by a
     * specified {@code valueSelector} function from items emitted by the finite source ObservableSource,
     * keyed by a specified {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @param keySelector
     *            the function that extracts a key from the source items to be used as key in the HashMap
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as value in the HashMap
     * @return a Single that emits a single item: a HashMap that contains an ArrayList of items mapped from
     *         the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Single<@NonNull Map<K, Collection<V>>> toMultimap(@NonNull Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
        Supplier<Map<K, Collection<V>>> mapSupplier = HashMapSupplier.asSupplier();
        Function<K, List<V>> collectionFactory = ArrayListSupplier.asFunction();
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }

    /**
     * Returns a Single that emits a single Map, returned by a specified {@code mapFactory} function, that
     * contains a custom collection of values, extracted by a specified {@code valueSelector} function from
     * items emitted by the source ObservableSource, and keyed by the {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @param keySelector
     *            the function that extracts a key from the source items to be used as the key in the Map
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as the value in the Map
     * @param mapSupplier
     *            the function that returns a Map instance to be used
     * @param collectionFactory
     *            the function that returns a Collection instance for a particular key to be used in the Map
     * @return a Single that emits a single item: a Map that contains the collection of mapped items from
     *         the source ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Single<@NonNull Map<K, Collection<V>>> toMultimap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<? extends Map<K, Collection<V>>> mapSupplier,
            @NonNull Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        Objects.requireNonNull(mapSupplier, "mapSupplier is null");
        Objects.requireNonNull(collectionFactory, "collectionFactory is null");
        return collect(mapSupplier, Functions.toMultimapKeyValueSelector(keySelector, valueSelector, collectionFactory));
    }

    /**
     * Returns a Single that emits a single Map, returned by a specified {@code mapFactory} function, that
     * contains an ArrayList of values, extracted by a specified {@code valueSelector} function from items
     * emitted by the finite source ObservableSource and keyed by the {@code keySelector} function.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toMultiMap.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated map to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toMultimap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @param keySelector
     *            the function that extracts a key from the source items to be used as the key in the Map
     * @param valueSelector
     *            the function that extracts a value from the source items to be used as the value in the Map
     * @param mapSupplier
     *            the function that returns a Map instance to be used
     * @return a Single that emits a single item: a Map that contains a list items mapped from the source
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <K, V> Single<@NonNull Map<K, Collection<V>>> toMultimap(
            @NonNull Function<? super T, ? extends K> keySelector,
            @NonNull Function<? super T, ? extends V> valueSelector,
            @NonNull Supplier<Map<K, Collection<V>>> mapSupplier
    ) {
        return toMultimap(keySelector, valueSelector, mapSupplier, ArrayListSupplier.asFunction());
    }

    /**
     * Converts the current Observable into a Flowable by applying the specified backpressure strategy.
     * <p>
     * Marble diagrams for the various backpressure strategies are as follows:
     * <ul>
     * <li>{@link BackpressureStrategy#BUFFER}
     * <p>
     * <img width="640" height="274" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.buffer.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#DROP}
     * <p>
     * <img width="640" height="389" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.drop.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#LATEST}
     * <p>
     * <img width="640" height="296" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.latest.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#ERROR}
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.error.png" alt="">
     * </li>
     * <li>{@link BackpressureStrategy#MISSING}
     * <p>
     * <img width="640" height="411" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toFlowable.o.missing.png" alt="">
     * </li>
     * </ul>
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator applies the chosen backpressure strategy of {@link BackpressureStrategy} enum.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toFlowable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param strategy the backpressure strategy to apply
     * @return the new Flowable instance
     */
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Flowable<T> toFlowable(@NonNull BackpressureStrategy strategy) {
        Flowable<T> f = new FlowableFromObservable<>(this);

        switch (strategy) {
            case DROP:
                return f.onBackpressureDrop();
            case LATEST:
                return f.onBackpressureLatest();
            case MISSING:
                return f;
            case ERROR:
                return RxJavaPlugins.onAssembly(new FlowableOnBackpressureError<>(f));
            default:
                return f.onBackpressureBuffer();
        }
    }

    /**
     * Returns a Single that emits a list that contains the items emitted by the finite source ObservableSource, in a
     * sorted order. Each item emitted by the ObservableSource must implement {@link Comparable} with respect to all
     * other items in the sequence.
     *
     * <p>If any item emitted by this Observable does not implement {@link Comparable} with respect to
     *             all other items emitted by this Observable, no items will be emitted and the
     *             sequence is terminated with a {@link ClassCastException}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return a Single that emits a list that contains the items emitted by the source ObservableSource in
     *         sorted order
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList() {
        return toSortedList(Functions.naturalOrder());
    }

    /**
     * Returns a Single that emits a list that contains the items emitted by the finite source ObservableSource, in a
     * sorted order based on a specified comparison function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.f.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator
     *            a function that compares two items emitted by the source ObservableSource and returns an Integer
     *            that indicates their sort order
     * @return a Single that emits a list that contains the items emitted by the source ObservableSource in
     *         sorted order
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(@NonNull Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList().map(Functions.listSorter(comparator));
    }

    /**
     * Returns a Single that emits a list that contains the items emitted by the finite source ObservableSource, in a
     * sorted order based on a specified comparison function.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.f.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param comparator
     *            a function that compares two items emitted by the source ObservableSource and returns an Integer
     *            that indicates their sort order
     * @param capacityHint
     *             the initial capacity of the ArrayList used to accumulate items before sorting
     * @return a Single that emits a list that contains the items emitted by the source ObservableSource in
     *         sorted order
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(@NonNull Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList(capacityHint).map(Functions.listSorter(comparator));
    }

    /**
     * Returns a Single that emits a list that contains the items emitted by the finite source ObservableSource, in a
     * sorted order. Each item emitted by the ObservableSource must implement {@link Comparable} with respect to all
     * other items in the sequence.
     *
     * <p>If any item emitted by this Observable does not implement {@link Comparable} with respect to
     *             all other items emitted by this Observable, no items will be emitted and the
     *             sequence is terminated with a {@link ClassCastException}.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.2.png" alt="">
     * <p>
     * Note that this operator requires the upstream to signal {@code onComplete} for the accumulated list to
     * be emitted. Sources that are infinite and never complete will never emit anything through this
     * operator and an infinite source may lead to a fatal {@code OutOfMemoryError}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toSortedList} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint
     *             the initial capacity of the ArrayList used to accumulate items before sorting
     * @return a Single that emits a list that contains the items emitted by the source ObservableSource in
     *         sorted order
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Single<@NonNull List<T>> toSortedList(int capacityHint) {
        return toSortedList(Functions.naturalOrder(), capacityHint);
    }

    /**
     * Modifies the source ObservableSource so that subscribers will dispose it on a specified
     * {@link Scheduler}.
     * <p>
     * <img width="640" height="452" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/unsubscribeOn.o.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param scheduler
     *            the {@link Scheduler} to perform the call to dispose() of the upstream Disposable
     * @return the source ObservableSource modified so that its dispose() calls happen on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<T> unsubscribeOn(@NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableUnsubscribeOn<>(this, scheduler));
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each containing {@code count} items. When the source
     * ObservableSource completes or encounters an error, the resulting ObservableSource emits the current window and
     * propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="400" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window3.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @return an Observable that emits connected, non-overlapping windows, each containing at most
     *         {@code count} items from the source ObservableSource
     * @throws IllegalArgumentException if either count is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count) {
        return window(count, count, bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits windows every {@code skip} items, each containing no more than {@code count} items. When
     * the source ObservableSource completes or encounters an error, the resulting ObservableSource emits the current window
     * and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window4.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param skip
     *            how many items need to be skipped before starting a new window. Note that if {@code skip} and
     *            {@code count} are equal this is the same operation as {@link #window(long)}.
     * @return an Observable that emits windows every {@code skip} items containing at most {@code count} items
     *         from the source ObservableSource
     * @throws IllegalArgumentException if either count or skip is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count, long skip) {
        return window(count, skip, bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits windows every {@code skip} items, each containing no more than {@code count} items. When
     * the source ObservableSource completes or encounters an error, the resulting ObservableSource emits the current window
     * and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="365" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window4.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param skip
     *            how many items need to be skipped before starting a new window. Note that if {@code skip} and
     *            {@code count} are equal this is the same operation as {@link #window(long)}.
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return an Observable that emits windows every {@code skip} items containing at most {@code count} items
     *         from the source ObservableSource
     * @throws IllegalArgumentException if either count or skip is non-positive
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Observable<Observable<T>> window(long count, long skip, int bufferSize) {
        ObjectHelper.verifyPositive(count, "count");
        ObjectHelper.verifyPositive(skip, "skip");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindow<>(this, count, skip, bufferSize));
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the source
     * ObservableSource completes or ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @return an Observable that emits new windows periodically as a fixed timespan elapses
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit) {
        return window(timespan, timeskip, unit, Schedulers.computation(), bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the source
     * ObservableSource completes or ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @return an Observable that emits new windows periodically as a fixed timespan elapses
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return window(timespan, timeskip, unit, scheduler, bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource starts a new window periodically, as determined by the {@code timeskip} argument. It emits
     * each window after a fixed timespan, specified by the {@code timespan} argument. When the source
     * ObservableSource completes or ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window7.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted
     * @param timeskip
     *            the period of time after which a new window will be created
     * @param unit
     *            the unit of time that applies to the {@code timespan} and {@code timeskip} arguments
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return an Observable that emits new windows periodically as a fixed timespan elapses
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, long timeskip, @NonNull TimeUnit unit, @NonNull Scheduler scheduler, int bufferSize) {
        ObjectHelper.verifyPositive(timespan, "timespan");
        ObjectHelper.verifyPositive(timeskip, "timeskip");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        return RxJavaPlugins.onAssembly(new ObservableWindowTimed<>(this, timespan, timeskip, unit, scheduler, Long.MAX_VALUE, bufferSize, false));
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument. When the source ObservableSource completes or encounters an error, the resulting
     * ObservableSource emits the current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window5.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @return an Observable that emits connected, non-overlapping windows representing items emitted by the
     *         source ObservableSource during fixed, consecutive durations
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit) {
        return window(timespan, unit, Schedulers.computation(), Long.MAX_VALUE, false);
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument or a maximum size as specified by the {@code count} argument (whichever is
     * reached first). When the source ObservableSource completes or encounters an error, the resulting ObservableSource
     * emits the current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @return an Observable that emits connected, non-overlapping windows of items from the source ObservableSource
     *         that were emitted during a fixed duration of time or when the window has reached maximum capacity
     *         (whichever occurs first)
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            long count) {
        return window(timespan, unit, Schedulers.computation(), count, false);
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument or a maximum size as specified by the {@code count} argument (whichever is
     * reached first). When the source ObservableSource completes or encounters an error, the resulting ObservableSource
     * emits the current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time that applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param restart
     *            if true, when a window reaches the capacity limit, the timer is restarted as well
     * @return an Observable that emits connected, non-overlapping windows of items from the source ObservableSource
     *         that were emitted during a fixed duration of time or when the window has reached maximum capacity
     *         (whichever occurs first)
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            long count, boolean restart) {
        return window(timespan, unit, Schedulers.computation(), count, restart);
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration as specified by the
     * {@code timespan} argument. When the source ObservableSource completes or encounters an error, the resulting
     * ObservableSource emits the current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="375" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window5.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @return an Observable that emits connected, non-overlapping windows containing items emitted by the
     *         source ObservableSource within a fixed duration
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler) {
        return window(timespan, unit, scheduler, Long.MAX_VALUE, false);
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the source ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @return an Observable that emits connected, non-overlapping windows of items from the source ObservableSource
     *         that were emitted during a fixed duration of time or when the window has reached maximum capacity
     *         (whichever occurs first)
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, long count) {
        return window(timespan, unit, scheduler, count, false);
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the source ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @param restart
     *            if true, when a window reaches the capacity limit, the timer is restarted as well
     * @return an Observable that emits connected, non-overlapping windows of items from the source ObservableSource
     *         that were emitted during a fixed duration of time or when the window has reached maximum capacity
     *         (whichever occurs first)
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(long timespan, @NonNull TimeUnit unit,
            @NonNull Scheduler scheduler, long count, boolean restart) {
        return window(timespan, unit, scheduler, count, restart, bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits connected, non-overlapping windows, each of a fixed duration specified by the
     * {@code timespan} argument or a maximum size specified by the {@code count} argument (whichever is reached
     * first). When the source ObservableSource completes or encounters an error, the resulting ObservableSource emits the
     * current window and propagates the notification from the source ObservableSource.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window6.s.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify which {@link Scheduler} this operator will use.</dd>
     * </dl>
     *
     * @param timespan
     *            the period of time each window collects items before it should be emitted and replaced with a
     *            new window
     * @param unit
     *            the unit of time which applies to the {@code timespan} argument
     * @param count
     *            the maximum size of each window before it should be emitted
     * @param scheduler
     *            the {@link Scheduler} to use when determining the end and start of a window
     * @param restart
     *            if true, when a window reaches the capacity limit, the timer is restarted as well
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return an Observable that emits connected, non-overlapping windows of items from the source ObservableSource
     *         that were emitted during a fixed duration of time or when the window has reached maximum capacity
     *         (whichever occurs first)
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @NonNull
    public final Observable<Observable<T>> window(
            long timespan, @NonNull TimeUnit unit, @NonNull Scheduler scheduler,
            long count, boolean restart, int bufferSize) {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        ObjectHelper.verifyPositive(count, "count");
        return RxJavaPlugins.onAssembly(new ObservableWindowTimed<>(this, timespan, timespan, unit, scheduler, count, bufferSize, restart));
    }

    /**
     * Returns an Observable that emits non-overlapping windows of items it collects from the source ObservableSource
     * where the boundary of each window is determined by the items emitted from a specified boundary-governing
     * ObservableSource.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window8.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the window element type (ignored)
     * @param boundary
     *            an ObservableSource whose emitted items close and open windows
     * @return an Observable that emits non-overlapping windows of items it collects from the source ObservableSource
     *         where the boundary of each window is determined by the items emitted from the {@code boundary}
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <B> Observable<Observable<T>> window(@NonNull ObservableSource<B> boundary) {
        return window(boundary, bufferSize());
    }

    /**
     * Returns an Observable that emits non-overlapping windows of items it collects from the source ObservableSource
     * where the boundary of each window is determined by the items emitted from a specified boundary-governing
     * ObservableSource.
     * <p>
     * <img width="640" height="475" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window8.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <B>
     *            the window element type (ignored)
     * @param boundary
     *            an ObservableSource whose emitted items close and open windows
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return an Observable that emits non-overlapping windows of items it collects from the source ObservableSource
     *         where the boundary of each window is determined by the items emitted from the {@code boundary}
     *         ObservableSource
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <B> Observable<Observable<T>> window(@NonNull ObservableSource<B> boundary, int bufferSize) {
        Objects.requireNonNull(boundary, "boundary is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindowBoundary<>(this, boundary, bufferSize));
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits windows that contain those items emitted by the source ObservableSource between the time when
     * the {@code openingIndicator} ObservableSource emits an item and when the ObservableSource returned by
     * {@code closingIndicator} emits an item.
     * <p>
     * <img width="640" height="550" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window2.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the window-opening ObservableSource
     * @param <V> the element type of the window-closing ObservableSources
     * @param openingIndicator
     *            an ObservableSource that, when it emits an item, causes another window to be created
     * @param closingIndicator
     *            a {@link Function} that produces an ObservableSource for every window created. When this ObservableSource
     *            emits an item, the associated window is closed and emitted
     * @return an Observable that emits windows of items emitted by the source ObservableSource that are governed by
     *         the specified window-governing ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<Observable<T>> window(
            @NonNull ObservableSource<U> openingIndicator,
            @NonNull Function<? super U, ? extends ObservableSource<V>> closingIndicator) {
        return window(openingIndicator, closingIndicator, bufferSize());
    }

    /**
     * Returns an Observable that emits windows of items it collects from the source ObservableSource. The resulting
     * ObservableSource emits windows that contain those items emitted by the source ObservableSource between the time when
     * the {@code openingIndicator} ObservableSource emits an item and when the ObservableSource returned by
     * {@code closingIndicator} emits an item.
     * <p>
     * <img width="640" height="550" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/window2.png" alt="">
     * <p>
     * Note that ignoring windows or subscribing later (i.e., on another thread) will result in
     * so-called window abandonment where a window may not contain any elements. In this case, subsequent
     * elements will be dropped until the condition for the next window boundary is satisfied. The behavior is
     * a trade-off for ensuring upstream cancellation can happen under some race conditions.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This version of {@code window} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the window-opening ObservableSource
     * @param <V> the element type of the window-closing ObservableSources
     * @param openingIndicator
     *            an ObservableSource that, when it emits an item, causes another window to be created
     * @param closingIndicator
     *            a {@link Function} that produces an ObservableSource for every window created. When this ObservableSource
     *            emits an item, the associated window is closed and emitted
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @return an Observable that emits windows of items emitted by the source ObservableSource that are governed by
     *         the specified window-governing ObservableSources
     * @see <a href="http://reactivex.io/documentation/operators/window.html">ReactiveX operators documentation: Window</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, V> Observable<Observable<T>> window(
            @NonNull ObservableSource<U> openingIndicator,
            @NonNull Function<? super U, ? extends ObservableSource<V>> closingIndicator, int bufferSize) {
        Objects.requireNonNull(openingIndicator, "openingIndicator is null");
        Objects.requireNonNull(closingIndicator, "closingIndicator is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableWindowBoundarySelector<>(this, openingIndicator, closingIndicator, bufferSize));
    }

    /**
     * Merges the specified ObservableSource into this ObservableSource sequence by using the {@code resultSelector}
     * function only when the source ObservableSource (this instance) emits an item.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator, by default, doesn't run any particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U> the element type of the other ObservableSource
     * @param <R> the result type of the combination
     * @param other
     *            the other ObservableSource
     * @param combiner
     *            the function to call when this ObservableSource emits an item and the other ObservableSource has already
     *            emitted an item, to generate the item to be emitted by the resulting ObservableSource
     * @return an Observable that merges the specified ObservableSource into this ObservableSource by using the
     *         {@code resultSelector} function only when the source ObservableSource sequence (this instance) emits an
     *         item
     * @since 2.0
     * @see <a href="http://reactivex.io/documentation/operators/combinelatest.html">ReactiveX operators documentation: CombineLatest</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> withLatestFrom(@NonNull ObservableSource<? extends U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> combiner) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(combiner, "combiner is null");

        return RxJavaPlugins.onAssembly(new ObservableWithLatestFrom<T, U, R>(this, combiner, other));
    }

    /**
     * Combines the value emission from this ObservableSource with the latest emissions from the
     * other ObservableSources via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when this ObservableSource emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <R> the result value type
     * @param source1 the first other ObservableSource
     * @param source2 the second other ObservableSource
     * @param combiner the function called with an array of values from each participating ObservableSource
     * @return the new ObservableSource instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <T1, T2, R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull Function3<? super T, ? super T1, ? super T2, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2 }, f);
    }

    /**
     * Combines the value emission from this ObservableSource with the latest emissions from the
     * other ObservableSources via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when this ObservableSource emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <T3> the third other source's value type
     * @param <R> the result value type
     * @param source1 the first other ObservableSource
     * @param source2 the second other ObservableSource
     * @param source3 the third other ObservableSource
     * @param combiner the function called with an array of values from each participating ObservableSource
     * @return the new ObservableSource instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <T1, T2, T3, R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull ObservableSource<T3> source3,
            @NonNull Function4<? super T, ? super T1, ? super T2, ? super T3, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2, source3 }, f);
    }

    /**
     * Combines the value emission from this ObservableSource with the latest emissions from the
     * other ObservableSources via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when this ObservableSource emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first other source's value type
     * @param <T2> the second other source's value type
     * @param <T3> the third other source's value type
     * @param <T4> the fourth other source's value type
     * @param <R> the result value type
     * @param source1 the first other ObservableSource
     * @param source2 the second other ObservableSource
     * @param source3 the third other ObservableSource
     * @param source4 the fourth other ObservableSource
     * @param combiner the function called with an array of values from each participating ObservableSource
     * @return the new ObservableSource instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <T1, T2, T3, T4, R> Observable<R> withLatestFrom(
            @NonNull ObservableSource<T1> source1, @NonNull ObservableSource<T2> source2,
            @NonNull ObservableSource<T3> source3, @NonNull ObservableSource<T4> source4,
            @NonNull Function5<? super T, ? super T1, ? super T2, ? super T3, ? super T4, R> combiner) {
        Objects.requireNonNull(source1, "source1 is null");
        Objects.requireNonNull(source2, "source2 is null");
        Objects.requireNonNull(source3, "source3 is null");
        Objects.requireNonNull(source4, "source4 is null");
        Objects.requireNonNull(combiner, "combiner is null");
        Function<Object[], R> f = Functions.toFunction(combiner);
        return withLatestFrom(new ObservableSource[] { source1, source2, source3, source4 }, f);
    }

    /**
     * Combines the value emission from this ObservableSource with the latest emissions from the
     * other ObservableSources via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when this ObservableSource emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param others the array of other sources
     * @param combiner the function called with an array of values from each participating ObservableSource
     * @return the new ObservableSource instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> withLatestFrom(@NonNull ObservableSource<?>[] others, @NonNull Function<? super Object[], R> combiner) {
        Objects.requireNonNull(others, "others is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new ObservableWithLatestFromMany<>(this, others, combiner));
    }

    /**
     * Combines the value emission from this ObservableSource with the latest emissions from the
     * other ObservableSources via a function to produce the output item.
     *
     * <p>Note that this operator doesn't emit anything until all other sources have produced at
     * least one value. The resulting emission only happens when this ObservableSource emits (and
     * not when any of the other sources emit, unlike combineLatest).
     * If a source doesn't produce any value and just completes, the sequence is completed immediately.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/withLatestFrom.png" alt="">
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This operator does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param others the iterable of other sources
     * @param combiner the function called with an array of values from each participating ObservableSource
     * @return the new ObservableSource instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <R> Observable<R> withLatestFrom(@NonNull Iterable<? extends ObservableSource<?>> others, @NonNull Function<? super Object[], R> combiner) {
        Objects.requireNonNull(others, "others is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return RxJavaPlugins.onAssembly(new ObservableWithLatestFromMany<>(this, others, combiner));
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source ObservableSource and a specified Iterable sequence.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.i.png" alt="">
     * <p>
     * Note that the {@code other} Iterable is evaluated as items are observed from the source ObservableSource; it is
     * not pre-consumed. This allows you to zip infinite streams on either side.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items in the {@code other} Iterable
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param other
     *            the Iterable sequence
     * @param zipper
     *            a function that combines the pairs of items from the ObservableSource and the Iterable to generate
     *            the items to be emitted by the resulting ObservableSource
     * @return an Observable that pairs up values from the source ObservableSource and the {@code other} Iterable
     *         sequence and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> zipWith(@NonNull Iterable<U> other, @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return RxJavaPlugins.onAssembly(new ObservableZipIterable<>(this, other, zipper));
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source ObservableSource and another specified ObservableSource.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param other
     *            the other ObservableSource
     * @param zipper
     *            a function that combines the pairs of items from the two ObservableSources to generate the items to
     *            be emitted by the resulting ObservableSource
     * @return an Observable that pairs up values from the source ObservableSource and the {@code other} ObservableSource
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        return zip(this, other, zipper);
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source ObservableSource and another specified ObservableSource.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param other
     *            the other ObservableSource
     * @param zipper
     *            a function that combines the pairs of items from the two ObservableSources to generate the items to
     *            be emitted by the resulting ObservableSource
     * @param delayError
     *            if true, errors from the current Observable or the other ObservableSource is delayed until both terminate
     * @return an Observable that pairs up values from the source ObservableSource and the {@code other} ObservableSource
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError) {
        return zip(this, other, zipper, delayError);
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source ObservableSource and another specified ObservableSource.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
     * <p>
     * The operator subscribes to its sources in order they are specified and completes eagerly if
     * one of the sources is shorter than the rest while disposing the other sources. Therefore, it
     * is possible those other sources will never be able to run to completion (and thus not calling
     * {@code doOnComplete()}). This can also happen if the sources are exactly the same length; if
     * source A completes and B has been consumed and is about to complete, the operator detects A won't
     * be sending further values and it will dispose B immediately. For example:
     * <pre><code>range(1, 5).doOnComplete(action1).zipWith(range(6, 5).doOnComplete(action2), (a, b) -&gt; a + b)</code></pre>
     * {@code action1} will be called but {@code action2} won't.
     * <br>To work around this termination property,
     * use {@link #doOnDispose(Action)} as well or use {@code using()} to do cleanup in case of completion
     * or a dispose() call.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <U>
     *            the type of items emitted by the {@code other} ObservableSource
     * @param <R>
     *            the type of items emitted by the resulting ObservableSource
     * @param other
     *            the other ObservableSource
     * @param zipper
     *            a function that combines the pairs of items from the two ObservableSources to generate the items to
     *            be emitted by the resulting ObservableSource
     * @param bufferSize
     *            the capacity hint for the buffer in the inner windows
     * @param delayError
     *            if true, errors from the current Observable or the other ObservableSource is delayed until both terminate
     * @return an Observable that pairs up values from the source ObservableSource and the {@code other} ObservableSource
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <U, R> Observable<R> zipWith(@NonNull ObservableSource<? extends U> other,
            @NonNull BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError, int bufferSize) {
        return zip(this, other, zipper, delayError, bufferSize);
    }

    // -------------------------------------------------------------------------
    // Fluent test support, super handy and reduces test preparation boilerplate
    // -------------------------------------------------------------------------
    /**
     * Creates a TestObserver and subscribes
     * it to this Observable.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new TestObserver instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final TestObserver<T> test() { // NoPMD
        TestObserver<T> to = new TestObserver<>();
        subscribe(to);
        return to;
    }

    /**
     * Creates a TestObserver, optionally disposes it and then subscribes
     * it to this Observable.
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code test} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param dispose dispose the TestObserver before it is subscribed to this Observable?
     * @return the new TestObserver instance
     * @since 2.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final TestObserver<T> test(boolean dispose) { // NoPMD
        TestObserver<T> to = new TestObserver<>();
        if (dispose) {
            to.dispose();
        }
        subscribe(to);
        return to;
    }

    // -------------------------------------------------------------------------
    // JDK 8 Support
    // -------------------------------------------------------------------------

    /**
     * Converts the existing value of the provided optional into a {@link #just(Object)}
     * or an empty optional into an {@link #empty()} {@code Observable} instance.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromOptional.o.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated optional reference and does not
     * by any means create this original optional. If the optional is to be created per
     * consumer upon subscription, use {@link #defer(Supplier)} around {@code fromOptional}:
     * <pre><code>
     * Observable.defer(() -&gt; Observable.fromOptional(createOptional()));
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the optional value
     * @param optional the optional value to convert into an {@code Observable}
     * @return the new Observable instance
     * @since 3.0.0
     * @see #just(Object)
     * @see #empty()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<@NonNull T> fromOptional(@NonNull Optional<T> optional) {
        Objects.requireNonNull(optional, "optional is null");
        return optional.map(Observable::just).orElseGet(Observable::empty);
    }

    /**
     * Signals the completion value or error of the given (hot) {@link CompletionStage}-based asynchronous calculation.
     * <p>
     * <img width="640" height="262" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromCompletionStage.o.png" alt="">
     * <p>
     * Note that the operator takes an already instantiated, running or terminated {@code CompletionStage}.
     * If the optional is to be created per consumer upon subscription, use {@link #defer(Supplier)}
     * around {@code fromCompletionStage}:
     * <pre><code>
     * Observable.defer(() -&gt; Observable.fromCompletionStage(createCompletionStage()));
     * </code></pre>
     * <p>
     * If the {@code CompletionStage} completes with {@code null}, a {@link NullPointerException} is signaled.
     * <p>
     * Canceling the flow can't cancel the execution of the {@code CompletionStage} because {@code CompletionStage}
     * itself doesn't support cancellation. Instead, the operator detaches from the {@code CompletionStage}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromCompletionStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the CompletionStage
     * @param stage the CompletionStage to convert to Observable and signal its terminal value or error
     * @return the new Observable instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<@NonNull T> fromCompletionStage(@NonNull CompletionStage<T> stage) {
        Objects.requireNonNull(stage, "stage is null");
        return RxJavaPlugins.onAssembly(new ObservableFromCompletionStage<>(stage));
    }

    /**
     * Converts a {@link Stream} into a finite {@code Observable} and emits its items in the sequence.
     * <p>
     * <img width="640" height="407" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/fromStream.o.png" alt="">
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. Exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #fromIterable(Iterable)}:
     * <pre><code>
     * Stream&lt;T&gt; stream = ...
     * Observable.fromIterable(stream::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * IntStream intStream = IntStream.rangeClosed(1, 10);
     * Observable.fromStream(intStream.boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code fromStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the element type of the source {@code Stream}
     * @param stream the {@code Stream} of values to emit
     * @return the new Observable instance
     * @since 3.0.0
     * @see #fromIterable(Iterable)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public static <T> Observable<@NonNull T> fromStream(@NonNull Stream<T> stream) {
        Objects.requireNonNull(stream, "stream is null");
        return RxJavaPlugins.onAssembly(new ObservableFromStream<>(stream));
    }

    /**
     * Maps each upstream value into an {@link Optional} and emits the contained item if not empty.
     * <p>
     * <img width="640" height="306" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/mapOptional.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code mapOptional} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-null output type
     * @param mapper the function that receives the upstream item and should return a <em>non-empty</em> {@code Optional}
     * to emit as the output or an <em>empty</em> {@code Optional} to skip to the next upstream value
     * @return the new Observable instance
     * @since 3.0.0
     * @see #map(Function)
     * @see #filter(Predicate)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> mapOptional(@NonNull Function<? super T, @NonNull Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMapOptional<>(this, mapper));
    }

    /**
     * Collects the finite upstream's values into a container via a Stream {@link Collector} callback set and emits
     * it as the success result.
     * <p>
     * <img width="640" height="358" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/collector.o.png" alt="">
     *
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code collect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param <R> the non-null result type
     * @param <A> the intermediate container type used for the accumulation
     * @param collector the interface defining the container supplier, accumulator and finisher functions;
     * see {@link Collectors} for some standard implementations
     * @return the new Single instance
     * @since 3.0.0
     * @see Collectors
     * @see #collect(Supplier, BiConsumer)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R, A> Single<R> collect(@NonNull Collector<T, A, R> collector) {
        Objects.requireNonNull(collector, "collector is null");
        return RxJavaPlugins.onAssembly(new ObservableCollectWithCollectorSingle<>(this, collector));
    }

    /**
     * Signals the first upstream item (or the default item if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <p>
     * {@code CompletionStage}s don't have a notion of emptiness and allow {@code null}s, therefore, one can either use
     * a {@code defaultItem} of {@code null} or turn the flow into a sequence of {@link Optional}s and default to {@link Optional#empty()}:
     * <pre><code>
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).firstStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #firstOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> firstStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableFirstStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the only expected upstream item (or the default item if the upstream is empty)
     * or signals {@link IllegalArgumentException} if the upstream has more than one item
     * via a {@link CompletionStage}.
     * <p>
     * <img width="640" height="227" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <p>
     * {@code CompletionStage}s don't have a notion of emptiness and allow {@code null}s, therefore, one can either use
     * a {@code defaultItem} of {@code null} or turn the flow into a sequence of {@link Optional}s and default to {@link Optional#empty()}:
     * <pre><code>
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).singleStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #singleOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> singleStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableSingleStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the last upstream item (or the default item if the upstream is empty) via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="313" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <p>
     * {@code CompletionStage}s don't have a notion of emptiness and allow {@code null}s, therefore, one can either use
     * a {@code defaultItem} of {@code null} or turn the flow into a sequence of {@link Optional}s and default to {@link Optional#empty()}:
     * <pre><code>
     * CompletionStage&lt;Optional&lt;T&gt;&gt; stage = source.map(Optional::of).lastStage(Optional.empty());
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @param defaultItem the item to signal if the upstream is empty
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #lastOrErrorStage()
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> lastStage(@Nullable T defaultItem) {
        return subscribeWith(new ObservableLastStageObserver<>(true, defaultItem));
    }

    /**
     * Signals the first upstream item or a {@link NoSuchElementException} if the upstream is empty via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="341" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/firstOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code firstOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #firstStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> firstOrErrorStage() {
        return subscribeWith(new ObservableFirstStageObserver<>(false, null));
    }

    /**
     * Signals the only expected upstream item, a {@link NoSuchElementException} if the upstream is empty
     * or signals {@link IllegalArgumentException} if the upstream has more than one item
     * via a {@link CompletionStage}.
     * <p>
     * <img width="640" height="227" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/singleOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code singleOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #singleStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> singleOrErrorStage() {
        return subscribeWith(new ObservableSingleStageObserver<>(false, null));
    }

    /**
     * Signals the last upstream item or a {@link NoSuchElementException} if the upstream is empty via
     * a {@link CompletionStage}.
     * <p>
     * <img width="640" height="343" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/lastOrErrorStage.o.png" alt="">
     * <p>
     * The upstream can be canceled by converting the resulting {@code CompletionStage} into
     * {@link CompletableFuture} via {@link CompletionStage#toCompletableFuture()} and
     * calling {@link CompletableFuture#cancel(boolean)} on it.
     * The upstream will be also cancelled if the resulting {@code CompletionStage} is converted to and
     * completed manually by {@link CompletableFuture#complete(Object)} or {@link CompletableFuture#completeExceptionally(Throwable)}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code lastOrErrorStage} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new CompletionStage instance
     * @since 3.0.0
     * @see #lastStage(Object)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final CompletionStage<T> lastOrErrorStage() {
        return subscribeWith(new ObservableLastStageObserver<>(false, null));
    }

    /**
     * Creates a sequential {@link Stream} to consume or process this {@code Observable} in a blocking manner via
     * the Java {@code Stream} API.
     * <p>
     * <img width="640" height="399" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingStream.o.png" alt="">
     * <p>
     * Cancellation of the upstream is done via {@link Stream#close()}, therefore, it is strongly recommended the
     * consumption is performed within a try-with-resources construct:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10)
     *        .subscribeOn(Schedulers.computation());
     *
     * try (Stream&lt;Integer&gt; stream = source.blockingStream()) {
     *     stream.limit(3).forEach(System.out::println);
     * }
     * </code></pre>
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return the new Stream instance
     * @since 3.0.0
     * @see #blockingStream(int)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Stream<T> blockingStream() {
        return blockingStream(bufferSize());
    }

    /**
     * Creates a sequential {@link Stream} to consume or process this {@code Observable} in a blocking manner via
     * the Java {@code Stream} API.
     * <p>
     * <img width="640" height="399" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/blockingStream.oi.png" alt="">
     * <p>
     * Cancellation of the upstream is done via {@link Stream#close()}, therefore, it is strongly recommended the
     * consumption is performed within a try-with-resources construct:
     * <pre><code>
     * Observable&lt;Integer&gt; source = Observable.range(1, 10)
     *        .subscribeOn(Schedulers.computation());
     *
     * try (Stream&lt;Integer&gt; stream = source.blockingStream(4)) {
     *     stream.limit(3).forEach(System.out::println);
     * }
     * </code></pre>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code blockingStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param capacityHint the expected number of items to be buffered
     * @return the new Stream instance
     * @since 3.0.0
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final Stream<T> blockingStream(int capacityHint) {
        Iterator<T> iterator = blockingIterable(capacityHint).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .onClose(((Disposable) iterator)::dispose);
    }

    /**
     * Maps each upstream item into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/concatMapStream.o.png" alt="">
     * <p>
     * Due to the blocking and sequential nature of Java {@link Stream}s, the streams are mapped and consumed in a sequential fashion
     * without interleaving (unlike a more general {@link #flatMap(Function)}). Therefore, {@code flatMapStream} and
     * {@code concatMapStream} are identical operators and are provided as aliases.
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. Exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #concatMapIterable(Function)}:
     * <pre><code>
     * source.concatMapIterable(v -&gt; createStream(v)::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.concatMapStream(v -&gt; IntStream.rangeClosed(v + 1, v + 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code concatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @return the new Observable instance
     * @since 3.0.0
     * @see #concatMap(Function)
     * @see #concatMapIterable(Function)
     * @see #flatMapStream(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> concatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        return flatMapStream(mapper);
    }

    /**
     * Maps each upstream item into a {@link Stream} and emits the {@code Stream}'s items to the downstream in a sequential fashion.
     * <p>
     * <img width="640" height="299" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/flatMapStream.o.png" alt="">
     * <p>
     * Due to the blocking and sequential nature of Java {@link Stream}s, the streams are mapped and consumed in a sequential fashion
     * without interleaving (unlike a more general {@link #flatMap(Function)}). Therefore, {@code flatMapStream} and
     * {@code concatMapStream} are identical operators and are provided as aliases.
     * <p>
     * The operator closes the {@code Stream} upon cancellation and when it terminates. Exceptions raised when
     * closing a {@code Stream} are routed to the global error handler ({@link RxJavaPlugins#onError(Throwable)}.
     * If a {@code Stream} should not be closed, turn it into an {@link Iterable} and use {@link #flatMapIterable(Function)}:
     * <pre><code>
     * source.flatMapIterable(v -&gt; createStream(v)::iterator);
     * </code></pre>
     * <p>
     * Note that {@code Stream}s can be consumed only once; any subsequent attempt to consume a {@code Stream}
     * will result in an {@link IllegalStateException}.
     * <p>
     * Primitive streams are not supported and items have to be boxed manually (e.g., via {@link IntStream#boxed()}):
     * <pre><code>
     * source.flatMapStream(v -&gt; IntStream.rangeClosed(v + 1, v + 10).boxed());
     * </code></pre>
     * <p>
     * {@code Stream} does not support concurrent usage so creating and/or consuming the same instance multiple times
     * from multiple threads can lead to undefined behavior.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code flatMapStream} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the element type of the {@code Stream}s and the result
     * @param mapper the function that receives an upstream item and should return a {@code Stream} whose elements
     * will be emitted to the downstream
     * @return the new Observable instance
     * @since 3.0.0
     * @see #flatMap(Function)
     * @see #flatMapIterable(Function)
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @NonNull
    public final <@NonNull R> Observable<R> flatMapStream(@NonNull Function<? super T, @NonNull ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableFlatMapStream<>(this, mapper));
    }
}
