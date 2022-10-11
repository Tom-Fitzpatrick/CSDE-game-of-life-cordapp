package com.r3.developers.gol

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class RequestNextStatesFlowTest {


    // Names picked to match the corda network in config/dev-net.json
    private val pixelIdentities = listOf(
        MemberX500Name.parse("CN=X0Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y4, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y5, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y5, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y5, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y5, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X4Y5, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X5Y5, OU=pixel, O=R3, L=London, C=GB")
    )

    private val gameIdentity = MemberX500Name.parse("CN=Game, OU=Admin, O=R3, L=London, C=GB")
    private val stateIdentity = MemberX500Name.parse("CN=State, OU=Admin, O=R3, L=London, C=GB")

    @Test
    fun `test that State Flow returns correct message`() {

        val simulator = Simulator()

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val stateVN = simulator.createVirtualNode(stateHoldingId, RequestNextStatesFlow::class.java, RequestIndividualStateFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var initialGamestate = GameState(
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
            RequestIndividualStateFlow::class.java,
            initialGamestate
        )

        val flowResponse = stateVN.callFlow(requestData)

        val expectedResponse = """["......",".XXX..",".X....",".XX...","......","......"]"""

        println("response received:")
        println(flowResponse)
        println("expected response")
        println(expectedResponse)

        assert(flowResponse == expectedResponse)
    }

    @Test
    fun `test that Game Flow returns correct message`() {

        val simulator = Simulator()

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        val stateVN = simulator.createVirtualNode(stateHoldingId, RequestNextStatesFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var initialGamestate = GameState(
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
            initialGamestate
        )

        val flowResponse = gameVN.callFlow(requestData)
        val expectedResponse = """[".XXX..",".X.X..","X....X","XX...X","X....X",".XXX.."]"""

        println("flow response:")
        println(flowResponse)
        println("expected response:")
        println(expectedResponse)


        assert(flowResponse == expectedResponse)
    }

    @Test
    fun `test that infinite pattern continues correctly`() {

        val simulator = Simulator()

        val gameHoldingId = HoldingIdentity.Companion.create(gameIdentity)
        val stateHoldingId = HoldingIdentity.Companion.create(stateIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        val gameVN = simulator.createVirtualNode(gameHoldingId, RunGameOfLifeFlow::class.java)
        val stateVN = simulator.createVirtualNode(stateHoldingId, RequestNextStatesFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        var initialGamestate = GameState(
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
            initialGamestate
        )

        val flowResponse = gameVN.callFlow(requestData)
        val expectedResponse = """["......","..X...","..X...","..X...","......","......"]"""

        println("flow response:")
        println(flowResponse)
        println("expected response:")
        println(expectedResponse)


        assert(flowResponse == expectedResponse)
    }
}