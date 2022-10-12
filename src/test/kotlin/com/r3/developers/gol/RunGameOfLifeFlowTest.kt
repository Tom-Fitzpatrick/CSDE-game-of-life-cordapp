package com.r3.developers.gol

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.marshalling.*
import net.corda.simulator.Simulator
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.parse
import net.corda.v5.base.types.MemberX500Name
import org.apache.groovy.json.internal.JsonFastParser
import org.junit.jupiter.api.Test

class RunGameOfLifeFlowTest {

    private fun generateX500Names(dimension: Int): List<MemberX500Name> {
        var result = mutableListOf<MemberX500Name>()
        for (x in 0 until dimension) {
            for (y in 0 until dimension) {
                result.add(MemberX500Name.parse("CN=X${x}Y${y}, OU=pixel, O=R3, L=London, C=GB"))
            }
        }
        return result
    }

    private fun parseFinalState(result: String): String {
        val finalState = result.removeSurrounding("[", "]").split("[", "],").last()
        return finalState.substring(0, finalState.lastIndex)
    }

    private val gameIdentity = MemberX500Name.parse("CN=Game, OU=Admin, O=R3, L=London, C=GB")
    private val stateIdentity = MemberX500Name.parse("CN=State, OU=Admin, O=R3, L=London, C=GB")

    @Test
    fun `test that Game Flow completes on correct state`() {
        val simulator = Simulator()

        val pixelIdentities = generateX500Names(6)

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        simulator.createVirtualNode(stateHoldingId, RequestNextGameStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var gameInit = GameInit(
            20,
            arrayOf(
                charArrayOf('.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', 'X', 'X', '.', '.'),
                charArrayOf('.', 'X', 'X', '.', '.', '.'),
                charArrayOf('.', '.', 'X', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.'),
            ))

        val requestData = RequestData.create(
            "request no 1",
            RunGameOfLifeFlow::class.java,
            gameInit
        )

        val flowResponse = gameVN.callFlow(requestData)
        val finalState = parseFinalState(flowResponse)
        val expectedResponse = """".XXX..",".X.X..","X....X","XX...X","X....X",".XXX..""""

        println("flow final state:")
        println(finalState)

        println("expected response:")
        println(expectedResponse)

        assert(finalState == expectedResponse)
    }

    @Test
    fun `test that glider glides`() {
        val simulator = Simulator()

        val pixelIdentities = generateX500Names(8)

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        simulator.createVirtualNode(stateHoldingId, RequestNextGameStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var gameInit = GameInit(
            20,
            arrayOf(
                charArrayOf('X', '.', 'X', '.', '.', '.', '.', '.'),
                charArrayOf('.', 'X', 'X', '.', '.', '.', '.', '.'),
                charArrayOf('.', 'X', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
            ))

        val requestData = RequestData.create(
            "request no 1",
            RunGameOfLifeFlow::class.java,
            gameInit
        )

        val flowResponse = gameVN.callFlow(requestData)
        val finalState = parseFinalState(flowResponse)
        val expectedResponse = """"........","........","........","........","........",".....X.X","......XX","......X.""""

        println("flow final state:")
        println(finalState)

        println("expected response:")
        println(expectedResponse)

        assert(finalState == expectedResponse)
    }

    @Test
    fun `test that infinite pattern iterates correctly`() {

        val simulator = Simulator()

        val pixelIdentities = generateX500Names(6)

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        simulator.createVirtualNode(stateHoldingId, RequestNextGameStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var gameInit = GameInit(
            20,
            arrayOf(
                charArrayOf('.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', 'X', '.', '.', '.'),
                charArrayOf('.', '.', 'X', '.', '.', '.'),
                charArrayOf('.', '.', 'X', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.')
        ))

        val requestData = RequestData.create(
            "request no 1",
            RunGameOfLifeFlow::class.java,
            gameInit
        )

        val flowResponse = gameVN.callFlow(requestData)
        val finalState = parseFinalState(flowResponse)
        val expectedResponse = """"......","..X...","..X...","..X...","......","......""""

        println("flow final state:")
        println(finalState)

        println("expected response:")
        println(expectedResponse)

        assert(finalState == expectedResponse)
    }

    @Test
    fun `test 3x3 glider`() {

        val simulator = Simulator()

        val pixelIdentities = generateX500Names(3)

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        simulator.createVirtualNode(stateHoldingId, RequestNextGameStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var gameInit = GameInit(
            3,
            arrayOf(
                charArrayOf('.', '.', 'X'),
                charArrayOf('X', '.', 'X'),
                charArrayOf('.', 'X', '.'),
            ))

        val requestData = RequestData.create(
            "request no 1",
            RunGameOfLifeFlow::class.java,
            gameInit
        )

        val flowResponse = gameVN.callFlow(requestData)
        val finalState = parseFinalState(flowResponse)
        val expectedResponse = """"...","...","...""""

        println("flow final state:")
        println(finalState)

        println("expected response:")
        println(expectedResponse)

        assert(finalState == expectedResponse)
    }

    @Test
    fun `test that large number of nodes can be run`() {
        val simulator = Simulator()

        val pixelIdentities = generateX500Names(16)

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        simulator.createVirtualNode(stateHoldingId, RequestNextGameStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var gameInit = GameInit(
            20,
            arrayOf(
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', 'X', 'X', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', 'X', 'X', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', 'X', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
                charArrayOf('.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'),
            ))

        val requestData = RequestData.create(
            "request no 1",
            RunGameOfLifeFlow::class.java,
            gameInit
        )

        val flowResponse = gameVN.callFlow(requestData)
        val finalState = parseFinalState(flowResponse)
        val expectedResponse = """"................","................","................","................","................",".....XXX........","......XX........","XX...XX.........","X....X.X........","XX....XXXX......",".X...X..XX......",".....X.XX.......",".....XXX........","................","................","................""""

        println("flow final state:")
        println(finalState)

        println("expected response:")
        println(expectedResponse)

        assert(finalState == expectedResponse)
    }
}