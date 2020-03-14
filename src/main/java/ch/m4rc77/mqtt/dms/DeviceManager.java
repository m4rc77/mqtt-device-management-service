package ch.m4rc77.mqtt.dms;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DeviceManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceManager.class.getName());

    static final String START_CMD = ".cmd";
    static final String KILL_CMD = ".killCmd";
    static final String EXEC_CMD = ".exec";

    private boolean running = false;
    private Process process;

    private String currentProcessKillCommand = "";
    private String currentProcessStartCommand = "";

    private Properties config;

    DeviceManager(Properties prop) {
        log.info("Create DeviceManager ...");
        config = prop;
    }

    boolean executeCommand(String topic, String propertyKey) {
        try {

            String startCommand = config.getProperty(propertyKey + START_CMD, "");
            String killCommand = config.getProperty(propertyKey + KILL_CMD, "");
            String execCommand = config.getProperty(propertyKey + EXEC_CMD, "");

            if (execCommand.isEmpty() && startCommand.equals(currentProcessStartCommand) && getState() == State.RUNNING) {
                // requested process is already running ... just do nothing
                log.debug("Command " + startCommand + " already running.");
                return true;
            }

            if (!startCommand.isEmpty() && !currentProcessKillCommand.isEmpty()) {
                killProc(currentProcessKillCommand);
            }

            if (!startCommand.isEmpty()) {
                log.debug("Going to execute " + startCommand + " for topic " + topic);

                if (startCommand.startsWith("http") || startCommand.startsWith("https")) {
                    // --> webpage ... start browser
                    String url = startCommand;

                    // add cache buster ...
                    url += url.contains("?") ? "&" : "?";
                    url += "cache_buster=" + System.currentTimeMillis();

                    String startBrowserCommand = config.getProperty("browser.start", "") + " " + url;
                    startProcess(startBrowserCommand);
                    killCommand = config.getProperty("browser.kill");
                } else {
                    startProcess(startCommand);
                }
                currentProcessKillCommand = killCommand;
                currentProcessStartCommand = startCommand;

            } else if (!execCommand.isEmpty()) {
                execProcess(execCommand);
            } else {
                log.warn("Unable to find config property " + propertyKey + ".cmd or " + propertyKey + ".exec");
            }
            return true;
        } catch (Exception e) {
            log.error("executeCommand() for topic " + topic + " failed! Error " + e, e);
            return false;
        }
    }

    private State getState() {
        if (running) {
//            if (process != null && process.isAlive()) { // Java 8
            if (process != null && isAlive(process)) {
                return State.RUNNING;
            } else {
                return State.DEFECTIVE;
            }
        } else {
//            if (process != null && process.isAlive()) {  // Java 8
            if (process != null && isAlive(process)) {
                return State.DEFECTIVE;
            } else {
                return State.INACTIVE;
            }
        }
    }

    /**
     * Java 7 replacement for Process.isAlive() of Java 8
     * @param process the process to check
     * @return true if the process is alive
     */
    private boolean isAlive(Process process) {
        // @see https://docs.oracle.com/javase/7/docs/api/java/lang/Process.html#exitValue()
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            //IllegalThreadStateException - if the subprocess represented by this Process object has not yet terminated.
            return true;
        }
    }

    private boolean startProcess(String command) {
        boolean ok = false;
        if (!running && process == null) {
            try {
                String[] cmdArray = new String[3];
                cmdArray[0] = "bash";
                cmdArray[1] = "-c";
                cmdArray[2] = command;

                log.debug("Going to start command: " + cmdArray[0] + " " + cmdArray[1] + " " + cmdArray[2]);

                ProcessBuilder pb = new ProcessBuilder();
                pb.command(cmdArray);
                pb.inheritIO();
                pb.environment().put("DISPLAY", System.getenv("DISPLAY"));
                process = pb.start();
                ok = true;
                running = true;
                log.info("Started Process PID=" + getPidOfProcess(process) + ", cmd=" + command);
            } catch (IOException ioe) {
                log.error("Unable to start process with '" + command + "'!", ioe);
                process = null;
            }
        } else {
            log.warn("Unable to start process for '" + command + "' as there is already one process instance running");
        }
        return ok;
    }

    private boolean execProcess(String command) {
        boolean ok = false;

        try {
            String[] cmdArray = new String[3];
            cmdArray[0] = "bash";
            cmdArray[1] = "-c";
            cmdArray[2] = command;

            log.debug("Going to exec command: " + cmdArray[0] + " " + cmdArray[1] + " " + cmdArray[2]);

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmdArray);
            pb.inheritIO();
            pb.start();
            ok = true;
        } catch (IOException ioe) {
            log.error("Unable to exec command '" + command + "'!", ioe);
        }

        return ok;
    }

    private void killProc(String killCmd) {
        if (process != null) {
            log.debug("Stop process with: " + killCmd);
            try {
                String[] cmdArray = new String[3];
                cmdArray[0] = "bash";
                cmdArray[1] = "-c";
                cmdArray[2] = killCmd;

                ProcessBuilder pb = new ProcessBuilder();
                pb.command(cmdArray);
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    log.info("Exec of " + killCmd + " returned " + exitCode);
                } else {
                    log.warn("Exec of " + killCmd + " failed with code " + exitCode);
                }
            } catch (Exception e) {
                log.error("Failed to stop process with " + killCmd, e);
//                process.destroyForcibly(); // Java 8
                process.destroy();
            }
        }
        running = false;
        process = null;
    }

    void shutdown() {
        log.info("Shutdown DeviceManager");
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private static long getPidOfProcess(Process p) {
        long pid = -1;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }
}

enum State {
    RUNNING,
    DEFECTIVE,
    INACTIVE
}
