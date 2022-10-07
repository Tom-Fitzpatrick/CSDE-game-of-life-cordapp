package com.r3.developers.gol

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import org.junit.jupiter.api.Test

class RequestNextStatesFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private val pixelIdentities = listOf(
        MemberX500Name.parse("CN=X0Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y0, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y1, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y2, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X0Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X1Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X2Y3, OU=pixel, O=R3, L=London, C=GB"),
        MemberX500Name.parse("CN=X3Y3, OU=pixel, O=R3, L=London, C=GB"),
    )

    private val leaderIdentity = MemberX500Name.parse("CN=Main, OU=leader, O=R3, L=London, C=GB")

    @Test
    fun `test that MyFirstFLow returns correct message`() {

        // Instantiate an instance of the Simulator
        val simulator = Simulator()

        // Create Alice's and Bob HoldingIDs
        val leaderHoldingId = HoldingIdentity.Companion.create(leaderIdentity)
        val pixelHoldingIds = pixelIdentities.map{HoldingIdentity.Companion.create(it)}

        // Create Alice and Bob's virtual nodes, including the Class's of the flows which will be registered on each node.
        // We don't assign Bob's virtual node to a val because we don't need it for this particular test.
        val leaderVN = simulator.createVirtualNode(leaderHoldingId, RequestNextStatesFlow::class.java)

        for (pixelID in pixelHoldingIds) {
            simulator.createVirtualNode(pixelID, GetPixelNextState::class.java)
        }

        // Create an instance of the MyFirstFlowStartArgs which contains the request arguments for starting the flow
        val nextStatesStartArgs = NextStatesStartArgs(Array(3) { BooleanArray(3) })

        // Create a requestData object
        val requestData = RequestData.create(
            "request no 1",        // A unique reference for the instance of the flow request
            RequestNextStatesFlow::class.java,        // The name of the flow class which is to be started
            nextStatesStartArgs            // The object which contains the start arguments of the flow
        )

        // Call the Flow on Alice's virtual node and capture the response from the flow
        val flowResponse = leaderVN.callFlow(requestData)

        // Check that the flow has returned the expected string
        assert(flowResponse == "Hello Alice, best wishes from Bob")
    }
}