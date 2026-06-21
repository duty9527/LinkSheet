package app.linksheet.feature.engine.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.RewriteQueriesToDropUnusedColumns
import app.linksheet.api.database.BaseDao
import app.linksheet.feature.engine.database.entity.ExpressionRule
import app.linksheet.feature.engine.database.entity.Scenario
import app.linksheet.feature.engine.database.entity.ScenarioExpression
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioExpressionDao : BaseDao<ScenarioExpression> {
    @Query("SELECT * FROM scenario_expression")
    override fun getAll(): Flow<List<ScenarioExpression>>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * 
        FROM scenario s
            JOIN scenario_expression se ON s.id = se.scenarioId
            JOIN expression_rule e ON se.expressionId = e.id
        ORDER BY position ASC
    """)
    fun getAllScenarioExpressions(): Flow<Map<Scenario, List<ExpressionRule>>>

//    @RewriteQueriesToDropUnusedColumns
//    @Query(
//        """
//        SELECT *
//        FROM scenario s
//            JOIN scenario_expression se ON s.id = se.scenarioId
//            JOIN expression_rule e ON se.expressionId = e.id
//        WHERE s.id = :id
//    fun getScenarioExpressions(id: Long): Flow<ScenarioInfo?>

    @Query("""
        SELECT e.* 
        FROM expression_rule e
            JOIN scenario_expression se ON e.id = se.expressionId
        WHERE se.scenarioId = :scenarioId
    """)
    fun getRulesForScenario(scenarioId: Long): Flow<List<ExpressionRule>>
}

data class ScenarioInfo(
    val scenario: Scenario,
    val rules: List<ExpressionRule>
)
