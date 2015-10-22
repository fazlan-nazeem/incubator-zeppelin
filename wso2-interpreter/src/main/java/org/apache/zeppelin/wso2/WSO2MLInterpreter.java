/*
* Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.zeppelin.wso2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


        import org.apache.zeppelin.interpreter.Interpreter;
        import org.apache.zeppelin.interpreter.InterpreterContext;
        import org.apache.zeppelin.interpreter.InterpreterResult;
        import org.apache.zeppelin.scheduler.Scheduler;
        import org.apache.zeppelin.scheduler.SchedulerFactory;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.List;
        import java.util.Properties;
        import java.util.Scanner;

        import java.net.URL;

        import java.io.IOException;
        import java.io.File;
        import java.io.InputStreamReader;

/**
 * Shell interpreter for Zeppelin.
 *
 * @author Leemoonsoo
 * @author anthonycorbacho
 *
 */
public class WSO2MLInterpreter extends Interpreter {
    Logger logger = LoggerFactory.getLogger(WSO2MLInterpreter.class);
    int commandTimeOut = 600000;

    static {
        Interpreter.register("wso2ml", WSO2MLInterpreter.class.getName());
    }

    public WSO2MLInterpreter(Properties property) {
        super(property);
    }

    @Override
    public void open() {}

    @Override
    public void close() {}


    @Override
    public InterpreterResult interpret(String cmd, InterpreterContext contextInterpreter) {
        //    logger.debug("Run shell command '" + cmd + "'");
        //    long start = System.currentTimeMillis();
        //    CommandLine cmdLine = CommandLine.parse("bash");
        //    cmdLine.addArgument("-c", false);
        //    cmdLine.addArgument(cmd, false);
        //    DefaultExecutor executor = new DefaultExecutor();
        //    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //    executor.setStreamHandler(new PumpStreamHandler(outputStream));
        //
        //    executor.setWatchdog(new ExecuteWatchdog(commandTimeOut));
        //    try {
        //      int exitValue = executor.execute(cmdLine);
        //      return new InterpreterResult(InterpreterResult.Code.SUCCESS, outputStream.toString()
        //              + "-->test");
        //    } catch (ExecuteException e) {
        //      logger.error("Can not run " + cmd, e);
        //      return new InterpreterResult(Code.ERROR, e.getMessage());
        //    } catch (IOException e) {
        //      logger.error("Can not run " + cmd, e);
        //      return new InterpreterResult(Code.ERROR, e.getMessage());
        //    }

    /*
    ClassLoader classLoader = getClass().getClassLoader();
    //File file = new File(classLoader.getResource("mlwidget.html").getFile());
    URL dataFile = classLoader.getResource("mlwidget.html");
    InputStreamReader dataReader = new InputStreamReader(dataFile.openStream());
    StringBuilder template = new StringBuilder("");

    try (Scanner scanner = new Scanner(file)) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        template.append(line).append("\n");
      }

      scanner.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return new InterpreterResult(InterpreterResult.Code.SUCCESS,
            "%html " + template.toString());

    */

        return new InterpreterResult(InterpreterResult.Code.SUCCESS,
                "%html <h3>Test</h3>");
    }

    @Override
    public void cancel(InterpreterContext context) {}

    @Override
    public FormType getFormType() {
        return FormType.SIMPLE;
    }

    @Override
    public int getProgress(InterpreterContext context) {
        return 0;
    }

    @Override
    public Scheduler getScheduler() {
        return SchedulerFactory.singleton().createOrGetFIFOScheduler(
                WSO2MLInterpreter.class.getName() + this.hashCode());
    }

    @Override
    public List<String> completion(String buf, int cursor) {
        return null;
    }

}