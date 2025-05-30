package home.stockfish;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * TD
 */
@Slf4j
public /*final*/ class StockfishConfig {
    /** Stockfish client instance. */
    private final Stockfish client;
    /** Default time in milliseconds for the engine to "think". */
    private static final int THINKING_TIME = 1_000;
    /** True if starting with Black's turn. */
    @Getter
    private boolean isBlackTurn;
    /** Think time parameter. */
    @Getter
    private int thinkingTime;
    /** Default Initial FEN string for the test. */
    private static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** Started with client */
    public StockfishConfig(final Stockfish client) {
        this.client = client;
    }

    /**
     * Sets up the Stockfish engine and initializes the game state.
     *
     * @param properties The properties loaded from the configuration file.
     * @return The initial FEN string for the game.
     */
    public String setup(final Properties properties) {
        final String stockfishPath = properties.getProperty("stockfish.path");
        thinkingTime = setupTime(properties);
        validateStockfishStart(stockfishPath);
        client.sendCommand("uci");
        if (log.isTraceEnabled()) {
            log.trace("Salida inicial del motor: {}", client.readOutput("uciok", 5000));
        }

        String fen = getInitialFen(properties);
        client.sendCommand("position fen " + fen);
        printAllPossibleMoves();
        if (log.isInfoEnabled()) {
            log.info("Starting game from position FEN: {}", fen);
            log.info("Initial FEN: {}", StockfishMain.getFen());
        }
        fen = fen + " moves ";
        isBlackTurn = "b".equals(fen.split(" ")[1]);
        return fen;
    }

    private static int setupTime(final Properties properties) {
        int thinkingTime = THINKING_TIME; // Default value

        // Read thinking time from the configuration file
        final String thinkingTimeProp = properties.getProperty("thinking.time");
        if (thinkingTimeProp != null) {
            try {
                thinkingTime = Integer.parseInt(thinkingTimeProp);
            } catch (final NumberFormatException e) {
                log.warn("Invalid thinking time in configuration. Using default: {}", THINKING_TIME);
            }
        }
        return thinkingTime;
    }

    private void validateStockfishStart(final String stockfishPath) {
        // Check if the path is null or empty
        if (isBlank(stockfishPath)) {
            log.error("Path to the Stockfish executable is not configured.");
            System.exit(1); // NOPMD DoNotTerminateVM
        }

        // Start the engine with the path to the Stockfish executable
        if (!client.startEngine(stockfishPath)) {
            log.error("Can't start the engine, exit");
            System.exit(1); // NOPMD DoNotTerminateVM
        }
    }

    private static String getInitialFen(final Properties properties) {
        // Read the FEN from the configuration file
        final String fenProp = properties.getProperty("fen");
        final String fen;
        if (fenProp == null) {
            log.info("FEN not found in configuration. Using default: {}", START_FEN);
            fen = START_FEN;
        } else {
            fen = fenProp;
        }
        return fen;
    }

    /** Prints all possible movements from the current position. */
    private void printAllPossibleMoves() {
        StockfishMain.print(client);

        // Get all possible moves using the 'go perft' command
        client.sendCommand("go perft 1");
        final String possibleMoves = client.readOutput("Nodes searched", 20_000);
        if (log.isDebugEnabled()) {
            log.debug("All possible moves:\n------------------------{}\n----------------------", possibleMoves);
        }
    }

}