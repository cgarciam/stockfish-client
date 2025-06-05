package home.stockfish;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Test;

class StockfishMainTest {

    @Test
    void main() throws IOException {// NOSONAR S2699
        System.out.println("Starting " + new Date());
        StockfishMain.main(new String[] {});
    }

    @Test
    void fromMovesToFen() {
        // Next is my game of 4° round of the 2025 Juegos Interbancarios, played in cdmx on 2025-05-31.
        final String moves = "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4 Bb4+ 5. c3 Be7 6. Qb3 d5 7. exd5 Bc5 8. dxc6 Qe7+ 9. Kd2 dxc3+ 10. Nxc3 Qd6+ 11. Ke2 bxc6 12. Re1 Bf5 13. Kf1+ Ne7 14. Bxf7+ Kf8 15. Bg5 Qd3+ 16. Kg1 h6 17. Red1 Qc2 18. Bxe7+ Kxe7 19. Re1+ Kf8 20. Re2 Qxb3 21. Bxb3 g6 22. Ne5 Kg7 23. Nxc6 Rhf8 24. Rd1 Bg4 25. Rdd2 Bxe2 26. Rxe2 Rae8 27. Rxe8 Rxe8 28. g3 Rf8 29. Ne4 Bb6 30. Kf1 g5 31. Ke2 Re8 32. Bd5 g4 33. Kd3 Rf8 34. Nd4 Kg6 35. Ne6 Re8 36. Nf4+ Kf5 37. Bc6 Rd8+ 38. Nd5 a6 39. b4 a5 40. bxa5 Bxa5 41. Ke3 Rb8 42. Bd7+ Ke5 43. Ndf6 Rb2 44. Nxg4+ Kd5 45. Ngf6+";
        // Last move was a mistake: 45... Ke5 46. f4# 1-0.
        // Remove move numbers and dots:
        final String cleanedMoves = moves.replaceAll("\\d+\\.\\s*", "").replaceAll("\\.", "");
        final String fen = StockfishMain.fromMovesToFen(cleanedMoves);
        assertEquals("8/2pB4/5N1p/b2k4/4N3/4K1P1/Pr3P1P/8 b - - 2 89", fen);
    }

}