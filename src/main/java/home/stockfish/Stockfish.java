package home.stockfish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import lombok.extern.slf4j.Slf4j;

/** Class to interact with the Stockfish chess engine. Allows starting the engine, sending commands, and reading output. */
@Slf4j
//@SuppressWarnings("PMD.CommentRequired")
public class Stockfish implements AutoCloseable { // NOPMD AtLeastOneConstructor
    private static final String UCI_OK = "uciok";
    private Process process;
    private BufferedReader processReader;
    private BufferedWriter processWriter;

    /**
     * Starts the Stockfish engine.
     *
     * @param path Path to the Stockfish executable.
     * @return true if the engine started successfully, false otherwise.
     */
    public boolean startEngine(final String path) {
        boolean status = false;
        try {
            final boolean isUciOk;
            final ProcessBuilder builder = new ProcessBuilder(path);
            builder.redirectErrorStream(true);
            process = builder.start();

            processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            // Verify if the engine responds correctly
            sendCommand("uci");
            final String response = readOutput(UCI_OK, 5000);
            if (response.contains(UCI_OK)) {
                log.debug("Stockfish engine started successfully.");
                isUciOk = true;
            } else {
                log.error("Error starting Stockfish engine: did not receive 'uciok'.");
                isUciOk = false;
            }
            status = isUciOk;
        } catch (final IOException e) {
            log.error("Error starting Stockfish engine", e);
        }
        return status;
    }

    /**
     * Sends a command to the Stockfish engine.
     *
     * @param command The command to send.
     */
    public void sendCommand(final String command) {
        try {
            processWriter.write(command + "\n");
            processWriter.flush();
        } catch (final IOException e) {
            log.error("Error sending command to Stockfish engine", e);
        }
    }

    /**
     * Reads the output from the Stockfish engine until a specific expected string
     * is found or a timeout occurs.
     *
     * @param expected      The expected string to look for in the output.
     * @param timeoutMillis The timeout in milliseconds.
     * @return The output read from the engine.
     */
    public String readOutput(final String expected, final int timeoutMillis) {
        final StringBuilder output = new StringBuilder();
        final long startTime = System.currentTimeMillis();
        try {
            String line;
            while ((line = processReader.readLine()) != null) { // NOPMD AssignmentInOperand NOSONAR java:S135
                output.append(line).append('\n');
                if (line.contains(expected)) {
                    break;
                }
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    log.warn("Timeout reached while reading engine output.");
                    break;
                }
            }
        } catch (final IOException e) {
            log.error("Error al leer la salida del motor.", e);
        }
        return output.toString();
    }

    public void stopEngine() {
        try {
            if (processWriter != null) {
                processWriter.close();
            }
            if (processReader != null) {
                processReader.close();
            }
            if (process != null) {
                process.destroy();
            }
            log.info("Stockfish engine stopped successfully.");
        } catch (final IOException e) {
            log.error("Error while stopping Stockfish engine", e);
        }
    }

    @Override
    public void close() {
        stopEngine();
    }

}