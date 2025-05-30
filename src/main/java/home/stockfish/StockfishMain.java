package home.stockfish;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.github.bhlangonijr.chesslib.Board;
import com.github.louism33.chesscore.Chessboard;

import lombok.extern.slf4j.Slf4j;

/** Main class to run the Stockfish chess engine. */
@Slf4j
public final class StockfishMain {
    /** Stockfish client instance. */
    private static final Stockfish CLIENT = new Stockfish();
    /** Stockfish responds with this when there are no more valid moves. */
    private static final String NONE = "(none)";
    /** Path to the configuration file. */
    private static final String CONFIG_FILE = "src/main/resources/config.properties";
    /** For the rule of draw by triple repetition of position. */
    private static final int DRAW_BY_3 = 3;
    /** Global variable to store the best moves. */
    private static final List<String> BEST_MOVES = new ArrayList<>();
    /** The number of times a position has been reached. */
    private static final Map<String, Integer> POSITION_COUNTS = new ConcurrentHashMap<>();
    /** Indicates turn */
    private static boolean isBlackTurn;

    private StockfishMain() {
        // Private constructor to prevent instantiation
    }

    private static String moves(final List<String> bestMoves) {
        final List<String> moveList = new ArrayList<>(bestMoves);
        final StringBuilder moves = new StringBuilder();
        for (final String move : moveList) {
            moves.append(move).append(' ');
        }
        return moves.toString().trim();
    }

    private static String movesWithNumbers(final List<String> bestMoves) {
        final StringBuilder moves = new StringBuilder();
        int moveNumber = 1;

        for (int i = 0; i < bestMoves.size(); i++) {
            if (i == 0 && isBlackTurn) {
                // Start with Black's turn
                moves.append("1... ");
            } else if (i % 2 == 0) {
                // Add move number every two moves
                moves.append(moveNumber).append(". ");
                moveNumber++;
            }
            moves.append(bestMoves.get(i)).append(' ');
        }

        return moves.toString().trim();
    }

    /**
     * Main method to start the Stockfish engine and play a game against itself from
     * a given FEN.
     *
     * @param args Command line arguments (not used).
     * @throws IOException If an I/O error occurs while reading the configuration
     *                     file.
     */
    public static void main(final String... args) throws IOException {
        final Properties properties = new Properties();

        try (InputStream config = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            // Load the properties from the file.
            properties.load(config);
            final StockfishConfig stockfishConfig = new StockfishConfig(CLIENT);
            final String initialFen = stockfishConfig.setup(properties);
            final String fen = initialFen;
            int moveCount = 2;

            final int thinkingTime = stockfishConfig.getThinkingTime();
            isBlackTurn = stockfishConfig.isBlackTurn();
            String move;
            do {
                move = getMove(fen, moveCount, thinkingTime);
                if (NONE.equals(move)) {
                    log.warn("No valid moves available. Game over.");
                    generateGameReport(BEST_MOVES, initialFen.replace(" moves ", ""));
                    break;
                } else {
                    BEST_MOVES.add(move);
                    moveCount++;
                }

            } while (!NONE.equals(move));

        } finally {
            log.info("Stop the engine...");
            CLIENT.stopEngine();
            log.info("Engine stopped.");
        }
    }

    private static String getMove(final String fen, final int moveCount, final int thinkingTime) {
        // Update the position and check for repetition
        final String currentFen = fen + moves(BEST_MOVES);
        final String currentFenR = getFen();
        if (log.isDebugEnabled()) {
            log.debug("Moves: {}", movesWithNumbers(BEST_MOVES));
        }
        if (log.isTraceEnabled()) {
            log.trace("FEN actual with moves: {}", currentFen);
            log.trace("FEN actual: {}", currentFenR);
        }
        // Remove numbers related to move number from the FEN string
        final String sanitizedFen = currentFenR.replaceAll(" \\d+ \\d+$", "");
        log.debug("Sanitized FEN: {}", sanitizedFen);

        // Update the position counts with the sanitized FEN
        POSITION_COUNTS.put(sanitizedFen, POSITION_COUNTS.getOrDefault(sanitizedFen, 0) + 1);
        if (POSITION_COUNTS.get(sanitizedFen) == DRAW_BY_3) {
            log.info("The same position has been repeated 3 times. Stopping the game (Draw).");
            CLIENT.stopEngine();
            System.exit(0); // NOPMD DoNotTerminateVM
        }

        // Send the updated position to the engine
        CLIENT.sendCommand("position fen " + currentFen);
        CLIENT.sendCommand("go movetime " + thinkingTime);
        // Print the current board position
        print(CLIENT);

        // Read the output for the best move
        final String bestMoveResponse = CLIENT.readOutput("bestmove", 75_000);
        final String move = extractBestMove(bestMoveResponse);
        if (log.isInfoEnabled()) {
            log.info("Move {} {}", moveCount / 2, move);
        }
        final String algebraicMove = convertToAlgebraicNotation(move, getFen());
        if (log.isInfoEnabled()) {
            log.info("Move {} {}", moveCount / 2, algebraicMove);
        }
        return move;
    }

    /**
     * Converts a UCI move to algebraic notation.
     *
     * @param uciMove The move in UCI (lan) format (e.g., "e2e4").
     * @param fen     The current FEN string representing the board state.
     * @return The move in algebraic notation.
     */
    private static String convertToAlgebraicNotation(final String uciMove, final String fen) {
        if (uciMove == null || uciMove.isEmpty() || NONE.equals(uciMove)) {
            return ""; // NOPMD OnlyOneReturn
        }
        final Chessboard chessboard = new Chessboard(fen);
        final Board board = new Board();
        board.loadFromFen(fen);
        return MoveUtils.lanToSan(board, chessboard, uciMove);
    }

    /**
     * Generates a game report in PGN format.
     *
     * @param bestMoves  The list of best moves in UCI format.
     * @param initialFen The initial FEN string for the game.
     * @return The game report in PGN format.
     */
    private static String generateGameReport(final List<String> bestMoves, final String initialFen) {
        final StringBuilder report = new StringBuilder();
        report.append("[FEN \"").append(initialFen).append("\"]\n").append(movesWithNumbers(bestMoves));

        log.info("Game report generated:\n{}", report);
        return report.toString().trim();
    }

    /**
     * Prints the current board position in a human-readable format.
     *
     * @param client Stockfish client instance.
     */
    /* default */ static void print(final Stockfish client) {
        client.sendCommand("d");
        String boardOutput = client.readOutput("Fen:", 1_000)
        // @formatter:off
            .replace('K', '♔')
            .replace('Q', '♕')
            .replace('R', '♖')
            .replace('B', '♗')
            .replace('N', '♘')
            .replace('P', '♙')
            .replace('k', '♚')
            .replace('q', '♛')
            .replace('r', '♜')
            .replace('b', '♝')
            .replace('n', '♞')
            .replace('p', '♟');
        // @formatter:on

        // Dynamically find the start of the board output
        final String[] lines = boardOutput.split("\n");
        final StringBuilder boardBuilder = new StringBuilder();
        boolean boardStarted = false;

        for (final String line : lines) {
            if (line.contains("+---+---+---+---+---+---+---+---+")) { // Detect board start
                boardStarted = true;
            }
            if (boardStarted) {
                boardBuilder.append(line).append('\n');
            }
        }

        boardOutput = boardBuilder.toString().trim();

        // Remove the last line of the output
        final int lastLineIndex = boardOutput.lastIndexOf('\n');
        if (lastLineIndex != -1) {
            boardOutput = boardOutput.substring(0, lastLineIndex);
        }

        log.info("\n{}", boardOutput);
    }

    /**
     * Extracts the best move from the engine's response.
     *
     * @param response Response from the engine.
     * @return The best move in UCI format.
     */
    private static String extractBestMove(final String response) {
        for (final String line : response.split("\n")) {
            if (line.startsWith("bestmove")) {
                return line.split(" ")[1]; // NOPMD OnlyOneReturn
            }
        }
        return "";
    }

    /**
     * Retrieves the FEN string from the current board position.
     *
     * @param CLIENT The Stockfish client instance.
     * @return The FEN string representing the current board position.
     */
    /* default */ static String getFen() {
        String fen = "";
        // Send the 'd' command to get the board state
        CLIENT.sendCommand("d");
        final String output = CLIENT.readOutput("Fen:", 1_000);

        // Extract the FEN string from the output
        for (final String line : output.split("\n")) {
            if (line.startsWith("Fen:")) {
                // Extract FEN after "Fen:"
                fen = line.substring(5).trim();
                break;
            }
        }
        // Return an empty string if FEN cannot be retrieved
        if (log.isDebugEnabled()) {
            log.debug("FEN: {}", fen);
        }
        return fen;
    }

}