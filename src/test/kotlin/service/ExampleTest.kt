package service

import tools.aqua.bgw.util.Stack
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * A simple test class to demonstrate a basic unit test.
 */
class ExampleTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService: RootService

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun testIfSetUpWorked() {
        val testStack = Stack<Int>()

        for (i in 0..10) {
            testStack.push(i)
        }

        println(testStack.peek())
        println(testStack.peekAll())
        testStack.pushAll(testStack.popAll())
        println(testStack.peekAll())
    }
}