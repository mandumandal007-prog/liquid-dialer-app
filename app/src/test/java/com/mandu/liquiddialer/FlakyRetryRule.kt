package com.mandu.liquiddialer

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * CI ke shared runners kabhi kabhi heavy load me timing-flaky ho jaate hain (compose idle/scroll timing).
 * Ye rule ek failed UI test ko clean state (@Before dobara) ke saath dobara chalata hai.
 * Deterministic regression ab bhi FAIL hi hoga - sirf slow karke (har attempt chalta hai).
 */
class FlakyRetryRule(private val attempts: Int = 3) : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                var last: Throwable? = null
                repeat(attempts) {
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        last = t
                    }
                }
                throw last ?: IllegalStateException("retry without attempt")
            }
        }
}
