package home.stockfish;

import static com.github.louism33.utils.MoveParserFromAN.buildMoveFromLAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.louism33.chesscore.Chessboard;
import com.github.louism33.utils.MoveParserFromAN;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class MoveUtilsTest {
    private final Board myBoard = new Board();

    @Test
    void lanToSan1() {
        final Chessboard board = new Chessboard();
        final String lan1 = "e4";
        final String san1 = MoveParserFromAN.lanToSan(board, lan1);
        assertEquals(lan1, san1);
    }

    @Test
    void lanToSan2() {
        final Chessboard board = new Chessboard();
        final String lan2 = "g1f3";
        final String san2 = MoveParserFromAN.lanToSan(board, lan2);
        log.debug("LAN: " + lan2 + " -> SAN: " + san2);
        assertEquals("Nf3", san2);
    }

    @Test
    void lanToSanKnightCapture() {
        // For the following test case, add to the board the initial position given by
        // moves: 1.e4 e5 2.Nf3 Nc6 and test for next move: 3. Nxe5
        final Chessboard board3 = setup3();
        final String lan3 = "f3e5";
        final String san3 = MoveParserFromAN.lanToSan(board3, lan3);
        log.debug("LAN: " + lan3 + " -> SAN: " + san3);
        assertEquals("Nxe5", san3);
    }

    // Test case for castling:
    @Test
    void lanToSanCastling() {
        final Chessboard board4 = setup4();
        final String lan4 = "O-O"; // <-- TO DO: add support for e1g1
        final String san4 = MoveParserFromAN.lanToSan(board4, lan4);
        log.debug("LAN: " + lan4 + " -> SAN: " + san4);
        assertEquals("O-O", san4);
    }

    // Test case for promoting a pawn:
    @Test
    void lanToSanPromotion() {
        final Chessboard board5 = setup5();
        final String lan5 = "b7a8q";
        final String san5 = MoveParserFromAN.lanToSan(board5, lan5);
        log.debug("LAN: " + lan5 + " -> SAN: " + san5);
        assertEquals("bxa8=Q", san5);
    }

    // Test case for en passant capture:
    // TO DO To delete tests in class com.github.louism33.utils.MoveParserFromANTest?
    @Test
    void lanToSanEnPassant() {
        final Chessboard chessboard = setup();
        final String lan = "e5f6";
        final String san = MoveUtils.lanToSan(myBoard, chessboard, lan);

        assertEquals("exf6", san);
    }

    private Chessboard setup() {
        final Chessboard chessboard = new Chessboard();
        // Set up the board for:
        // @formatter:off
        /*
1. e4 d5
2. e5 f5
3. exf6 e.p.
         */
        // @formatter:on
        final String ms = "e2e4 d7d5 e4e5 f7f5";
        log.debug("moves: {}", ms);
        final String[] moves = ms.split(" ");
        final int length = moves.length;
        for (int i = 0; i < length; i++) {
            final int move = buildMoveFromLAN(chessboard, moves[i]);
            chessboard.makeMoveAndFlipTurn(move);
            myBoard.doMove(moves[i]);
        }
        // En passant move:
        final Move epMove = new Move("e5f6", Side.WHITE);
        assertTrue(myBoard.legalMoves().contains(epMove));
        return chessboard;
    }

    private Chessboard setup5() {
        final Chessboard board = new Chessboard();
        // Set up the board for:
        // @formatter:off
/*
1. a4 b5
2. a5 b4
3. a6 Bb7
4. axb7 b3
5. bxa8=Q
*/
        // @formatter:on
        final String ms = "a2a4 b7b5 a4a5 b5b4 a5a6 c8b7 a6b7 b4b3";
        final String[] moves = ms.split(" ");
        final int length = moves.length;
        for (int i = 0; i < length; i++) {
            final int move = buildMoveFromLAN(board, moves[i]);
            board.makeMoveAndFlipTurn(move);
        }
        return board;
    }

    private Chessboard setup4() {
        final Chessboard board = new Chessboard();
        // Set up the board for 1.e4 e5 2.Nf3 Nc6 3.Bc4 Nf6
        final String ms = "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6";
        final String[] moves = ms.split(" ");
        final int length = moves.length;
        for (int i = 0; i < length; i++) {
            final int move = buildMoveFromLAN(board, moves[i]);
            board.makeMoveAndFlipTurn(move);
        }
        return board;
    }

    private Chessboard setup3() {
        final Chessboard board = new Chessboard();
        // Set up the board for 1.e4 e5 2.Nf3 Nc6 3.Nxe5
        final String ms = "e2e4 e7e5 g1f3 b8c6";
        final String[] moves = ms.split(" ");
        final int length = moves.length;
        for (int i = 0; i < length; i++) {
            final int move = buildMoveFromLAN(board, moves[i]);
            board.makeMoveAndFlipTurn(move);
        }
        return board;
    }

    @Test
    void validate_throwsIfBoardIsNull() {
        Chessboard chessboard = new Chessboard();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            MoveUtils.lanToSan(null, chessboard, "e2e4")
        );
        assertTrue(ex.getMessage().contains("board"));
    }

    @Test
    void validate_throwsIfChessboardIsNull() {
        Board board = new Board();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            MoveUtils.lanToSan(board, null, "e2e4")
        );
        assertTrue(ex.getMessage().contains("chessboard"));
    }

    @Test
    void validate_throwsIfLanIsNull() {
        Board board = new Board();
        Chessboard chessboard = new Chessboard();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            MoveUtils.lanToSan(board, chessboard, null)
        );
        assertTrue(ex.getMessage().contains("lan"));
    }

    @Test
    void validate_throwsIfLanIsEmpty() {
        Board board = new Board();
        Chessboard chessboard = new Chessboard();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            MoveUtils.lanToSan(board, chessboard, "")
        );
        assertTrue(ex.getMessage().contains("lan"));
    }

}