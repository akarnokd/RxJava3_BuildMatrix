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

package io.reactivex.rxjava3.validators;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Check if the parameter name in Objects.requireNonNull
 * and ObjectHelper.verifyPositive calls match the parameter
 *  name in the message.
 */
public class ParamValidationNaming {

    @Test
    public void checkCompletable() throws Exception {
        processFile(Completable.class);
    }

    @Test
    public void checkSingle() throws Exception {
        processFile(Single.class);
    }

    @Test
    public void checkMaybe() throws Exception {
        processFile(Maybe.class);
    }

    @Test
    public void checkObservable() throws Exception {
        processFile(Observable.class);
    }

    @Test
    public void checkFlowable() throws Exception {
        processFile(Flowable.class);
    }

    @Test
    public void checkParallelFlowable() throws Exception {
        processFile(ParallelFlowable.class);
    }

    @Test
    public void checkConnectableObservable() throws Exception {
        processFile(ConnectableObservable.class);
    }

    @Test
    public void checkConnectableFlowable() throws Exception {
        processFile(ConnectableFlowable.class);
    }

    @Test
    public void checkSubject() throws Exception {
        processFile(Subject.class);
    }

    @Test
    public void checkFlowableProcessor() throws Exception {
        processFile(FlowableProcessor.class);
    }

    @Test
    public void checkDisposable() throws Exception {
        processFile(Disposable.class);
    }

    @Test
    public void checkScheduler() throws Exception {
        processFile(Scheduler.class);
    }

    @Test
    public void checkSchedulers() throws Exception {
        processFile(Schedulers.class);
    }

    @Test
    public void checkAsyncSubject() throws Exception {
        processFile(AsyncSubject.class);
    }

    @Test
    public void checkBehaviorSubject() throws Exception {
        processFile(BehaviorSubject.class);
    }

    @Test
    public void checkPublishSubject() throws Exception {
        processFile(PublishSubject.class);
    }

    @Test
    public void checkReplaySubject() throws Exception {
        processFile(ReplaySubject.class);
    }

    @Test
    public void checkUnicastSubject() throws Exception {
        processFile(UnicastSubject.class);
    }

    @Test
    public void checkSingleSubject() throws Exception {
        processFile(SingleSubject.class);
    }

    @Test
    public void checkMaybeSubject() throws Exception {
        processFile(MaybeSubject.class);
    }

    @Test
    public void checkCompletableSubject() throws Exception {
        processFile(CompletableSubject.class);
    }

    @Test
    public void checkAsyncProcessor() throws Exception {
        processFile(AsyncProcessor.class);
    }

    @Test
    public void checkBehaviorProcessor() throws Exception {
        processFile(BehaviorProcessor.class);
    }

    @Test
    public void checkPublishProcessor() throws Exception {
        processFile(PublishProcessor.class);
    }

    @Test
    public void checkReplayProcessor() throws Exception {
        processFile(ReplayProcessor.class);
    }

    @Test
    public void checkUnicastProcessor() throws Exception {
        processFile(UnicastProcessor.class);
    }

    @Test
    public void checkMulticastProcessor() throws Exception {
        processFile(MulticastProcessor.class);
    }

    static void processFile(Class<?> clazz) throws Exception {
        String baseClassName = clazz.getSimpleName();
        File f = TestHelper.findSource(baseClassName, clazz.getPackage().getName());
        if (f == null) {
            return;
        }
        String fullClassName = clazz.getName();

        int errorCount = 0;
        StringBuilder errors = new StringBuilder();

        List<String> lines = Files.readAllLines(f.toPath());

        for (int j = 0; j < lines.size(); j++) {
            String line = lines.get(j).trim();

            for (ValidatorStrings validatorStr : VALIDATOR_STRINGS) {
                if (line.startsWith(validatorStr.code)) {

                    int comma = line.indexOf(',');

                    String paramName = line.substring(validatorStr.code.length(), comma);

                    int quote = line.indexOf('"', comma);

                    String message = line.substring(quote + 1, quote + 2 + paramName.length());

                    if (!line.contains("The RxJavaPlugins")
                            && !(message.startsWith(paramName)
                            && (message.endsWith(" ") || message.endsWith("\"")))) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : Wrong validator message parameter name\r\n    ")
                        .append(line)
                        .append("\r\n")
                        .append("    ").append(paramName).append(" != ").append(message)
                        .append("\r\n at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }

                    // FIXME enable for other types in separate PR!
                    if (!baseClassName.equals("Completable")) {
                        continue;
                    }
                    // find JavaDoc of throws
                    boolean found = false;
                    for (int k = j - 1; k >= 0; k--) {
                        String linek = lines.get(k).trim();
                        if (linek.startsWith("/**")) {
                            break;
                        }
                        if (linek.startsWith("}")) {
                            found = true; // no JavaDoc
                            break;
                        }
                        if (linek.startsWith(validatorStr.javadoc)) {
                            // see if a @code paramName is present
                            String paramStr = "{@code " + paramName + "}";
                            for (int m = k; m < lines.size(); m++) {
                                String linem = lines.get(m).trim();
                                if (linem.startsWith("* @see")
                                        || linem.startsWith("* @since")
                                        || linem.startsWith("*/")) {
                                    break;
                                }
                                if (linem.contains(paramStr)) {
                                    found = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (!found) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : missing '")
                        .append(validatorStr.javadoc)
                        .append("' for argument validation: ")
                        .append(paramName)
                        .append("\r\n    ")
                        .append(line)
                        .append("\r\n at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }
                }
            }
        }

        if (errorCount != 0) {
            errors.insert(0, errorCount + " problems\r\n");
            errors.setLength(errors.length() - 2);
            throw new AssertionError(errors.toString());
        }
    }

    static final class ValidatorStrings {
        final String code;
        final String javadoc;
        ValidatorStrings(String code, String javadoc) {
            this.code = code;
            this.javadoc = javadoc;
        }
    }

    static final List<ValidatorStrings> VALIDATOR_STRINGS = Arrays.asList(
            new ValidatorStrings("Objects.requireNonNull(", "* @throws NullPointerException"),
            new ValidatorStrings("ObjectHelper.verifyPositive(", "* @throws IllegalArgumentException")
    );

}
