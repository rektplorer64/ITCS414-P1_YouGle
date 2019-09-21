import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.DecimalFormat
import java.text.NumberFormat



internal class RankingMathHelperTest {

    private val vector: Map<Int, Double> = mapOf(1 to 0.0, 2 to 0.0, 3 to 0.584, 4 to 0.0, 5 to 0.584, 6 to 0.584);
    private var formatter: NumberFormat = DecimalFormat("#0.000")

    @Test
    fun `Norm Calculation Test`(){
        val norm = RankedQuery.RankingMathHelper.calculateNorm(vector)
        Assertions.assertEquals("1.012", formatter.format(norm))
    }
}