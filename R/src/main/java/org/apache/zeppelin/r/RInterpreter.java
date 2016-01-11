/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.zeppelin.r;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class RInterpreter extends Interpreter {
    private final Logger logger = LoggerFactory.getLogger(RInterpreter.class);
    private RConnection connection;

    static {
        Interpreter.register("r", RInterpreter.class.getName());
    }

    public RInterpreter(Properties property) {
        super(property);
    }

    @Override public void open() {
        try {
            connection = new RConnection();
            logger.info("Connected to an Rserve instance");
            if (loadRMarkdown()) {
                String msg = "Rmarkdown loaded successfully";
                logger.info(msg);
            } else {
                String msg = "Rmarkdown loading was unsuccessful";
                logger.info(msg);
            }

            connection.voidEval("getFunctionNames <- function() {\n" +
                    "    loaded <- (.packages())\n" +
                    "    loaded <- paste(\"package:\", loaded, sep =\"\")\n" +
                    "    return(sort(unlist(lapply(loaded, lsf.str))))\n" +
                    "}");
            if (loadSparkR()) {
                String msg = "sparkR loaded successfully";
                logger.info(msg);
            } else {
                String msg = "sparkR loading was unsuccessful";
                logger.info(msg);
            }
        } catch (RserveException e) {
            String msg = "No Rserve instance available!";
            logger.error(msg, e);
        }
    }

    @Override public void close() {
        try {
            connection.shutdown();
            logger.info("Shutting down Rserve");
        } catch (RserveException e) {
            e.getMessage();
        }
    }

    @Override public InterpreterResult interpret(String lines, InterpreterContext contextInterpreter) {
        //avoid null pointer exception
        if (connection == null || !connection.isConnected()) {
            return new InterpreterResult(InterpreterResult.Code.ERROR, "No connection to Rserve");
        }

        // WORKAROUND: Rserve fails the first time (StartRserve may be involved...).
       /* if (firstStart) {
            firstStart = false;
            interpret("print(\"First start\")", contextInterpreter);
        }*/

        logger.info("Run R command '" + lines + "'");

        BufferedWriter writer = null;
        try {
            File in = File.createTempFile("forRmarkdown-" + contextInterpreter.getParagraphId(), ".Rmd");
            String inPath = in.getAbsolutePath().substring(0, in.getAbsolutePath().length() - 4);
            File out = new File(inPath + ".html");

            writer = new BufferedWriter(new FileWriter(in));
            writer.write("\n```{r comment=NA, echo=FALSE}\n" + lines + "\n```");
            writer.close();

            String rcmd = "render('" + in.getAbsolutePath() + "')" + "\n";

            connection.voidEval(rcmd);

            String html = new String(Files.readAllBytes(out.toPath()));

            // Only keep the bare results.
            String htmlOut = html.substring(html.indexOf("<body>") + 7, html.indexOf("</body>") - 1)
                    .replaceAll("<code>", "").replaceAll("</code>", "").replaceAll("\n\n", "").replaceAll("\n", "<br>")
                    .replaceAll("<pre>", "<p class='text'>").replaceAll("</pre>", "</p>").replaceAll("<br>", "")
                    .replaceAll("main-container", "");

            return new InterpreterResult(InterpreterResult.Code.SUCCESS, "%html\n" + htmlOut);
        } catch (RserveException e) {
            logger.error("Exception while connecting to Rserve", e);
            return new InterpreterResult(InterpreterResult.Code.ERROR, e.getMessage());
        } catch (java.io.IOException e) {
            logger.error("Exception while connecting to Rserve", e);
            return new InterpreterResult(InterpreterResult.Code.ERROR, e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                String msg = "Exception while closing BufferedWriterr";
                logger.error(msg, e);
            }
        }
    }

    @Override public void cancel(InterpreterContext context) {
    }

    @Override public FormType getFormType() {
        return FormType.NONE;
    }

    @Override public int getProgress(InterpreterContext context) {
        return 0;
    }

    @Override public Scheduler getScheduler() {
        return SchedulerFactory.singleton().createOrGetFIFOScheduler(RInterpreter.class.getName() + this.hashCode());
    }

    @Override public List<String> completion(String buf, int cursor) {
        List<String> list = new ArrayList<>();
        String[] funcList;
        String[] varList;
        try {
            varList = connection.eval("ls()").asStrings();
            funcList = connection.eval("getFunctionNames()").asStrings();
            String before = buf.substring(0, cursor - 1);
            List<String> listFunc = new ArrayList<>(Arrays.asList(funcList));
            List<String> listVar = new ArrayList<>(Arrays.asList(varList));
            listVar.remove("getFunctionNames");
            if (before.endsWith("\n") || before.endsWith(" ")) {
                list.addAll(listVar);
            } else {
                String[] tokenize = before.replaceAll("\n", " ").split(" ");
                String lastWord = tokenize[tokenize.length - 1];
                for (String s : listVar) {
                    if (s.startsWith(lastWord))
                        list.add(s);
                }
                for (String s : listFunc) {
                    if (s.startsWith(lastWord))
                        list.add(s);
                }
            }

        } catch (RserveException e) {
            String msg = e.getMessage();
            logger.warn(msg, e);
        } catch (REXPMismatchException e) {
            String msg = e.getMessage();
            logger.warn(msg, e);
        }

        return list;
    }

    /**
     * loads sparkR library into the environment and creates sparkContext and sqlContext.
     * SPARK_HOME should be set
     */
    private boolean loadSparkR() {
        try {
            connection.voidEval(".libPaths(c(file.path(Sys.getenv(\"SPARK_HOME\"),\"R\",\"lib\"), .libPaths()))");
            connection.voidEval("library('SparkR')");
            connection.voidEval("sc <- sparkR.init(master = \"local\")");
            connection.voidEval("sqlContext <- sparkRSQL.init(sc)");
            return true;
        } catch (RserveException e) {
            String msg = "Exception while closing BufferedWriterr";
            logger.warn(msg, e);
        }
        return false;
    }

    private boolean loadRMarkdown() {
        String[] loadedLibraries;
        try {
            loadedLibraries = connection.eval("library('rmarkdown')").asStrings();
            if(("rmarkdown").equals(loadedLibraries[0])) {
                return true;
            }
        } catch (RserveException e) {
            String msg = "Error while loading rmarkdown";
            logger.error(msg, e);
        } catch (REXPMismatchException e) {
            String msg = "Regular expression mismatch error";
            logger.error(msg, e);
        }
        return false;
    }

}