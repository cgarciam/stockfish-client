package home.stockfish;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.MoveBackup;
import com.github.bhlangonijr.chesslib.Square;
import com.github.louism33.chesscore.Chessboard;
import com.github.louism33.utils.MoveParserFromAN;

import lombok.extern.slf4j.Slf4j;

/** Main class to run the Stockfish chess engine. */
@Slf4j
public final class MoveUtils {

    private MoveUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts a move in LAN (Long Algebraic Notation) format to SAN (Standard
     * Algebraic Notation) format.
     *
     * @param board      The current board state.
     * @param chessboard The chess board object for additional context.
     * @param lan        The move in LAN format (e.g., "e5f6").
     * @return The move in SAN format (e.g., "exf6").
     */
    // @SuppressWarnings({"PMD.CommentSize"})
    public static String lanToSan(final Board board, final Chessboard chessboard, final String lan) {
        validate(board, chessboard, lan);

        // Create a temporary board to avoid modifying the original
        final Board tempBoard = new Board();
        tempBoard.loadFromFen(board.getFen());

        if (log.isTraceEnabled()) {
            log.trace("Board:\n{}", tempBoard);
        }

        String san;
        final String check;
        if (tempBoard.doMove(lan)) {
            check = inCheck(tempBoard, lan);
            final MoveBackup last = tempBoard.getBackup().getLast();
            if (log.isTraceEnabled()) {
                log.trace("Move applied: {}", last);
            }
            if (Square.NONE.equals(last.getEnPassant()) || Square.NONE.equals(last.getEnPassantTarget())) {
                san = MoveParserFromAN.lanToSan(chessboard, lan);
            } else {
                san = lan.charAt(0) + "x" + lan.substring(2);
                log.info("En passant move found: {}", san);
            }
        } else {
            throw new IllegalArgumentException("Movimiento LAN no válido: " + lan);
        }
        if (!check.isEmpty()) {
            // If check or check-mate, append the symbol
            san += check;
        }
        if (log.isDebugEnabled()) {
            log.debug("LAN: {} -> SAN: {}", lan, san);
        }

        return san;
    }

    private static String inCheck(final Board board, final String lan) {
      String check = "";
      if (board.isKingAttacked()) {
          if (log.isTraceEnabled()) {
              log.trace("King attacked: {}", lan);
          }
          if (board.isMated()) {
              check = "#";
          } else {
              check = "+";
          }
      }
      return check;
    }

    private static void validate(final Board board, final Chessboard chessboard, final String lan) {
        if (board == null) {
            throw new IllegalArgumentException("El parámetro 'board' no puede ser nulo");
        }
        if (chessboard == null) {
            throw new IllegalArgumentException("El parámetro 'chessboard' no puede ser nulo");
        }
        if (lan == null || lan.isEmpty()) {
            throw new IllegalArgumentException("El parámetro 'lan' no puede ser nulo ni vacío");
        }
    }

}