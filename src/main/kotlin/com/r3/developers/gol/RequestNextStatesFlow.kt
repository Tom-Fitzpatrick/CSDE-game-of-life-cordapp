package com.r3.developers.gol

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import kotlin.random.Random

@CordaSerializable
class NextStatesStartArgs(val initialValues: Array<BooleanArray>)

@CordaSerializable
class RequestMessage(val sender: MemberX500Name, val message: String)

@CordaSerializable
class Message(val sender: MemberX500Name, val message: String)


// MyFirstFlow is an initiating flow, it's corresponding responder flow is called MyFirstFlowResponder (defined below)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatingFlow(protocol = "get-next-states")
// MyFirstFlow should inherit from RPCStartableFlow, which tells Corda it can be started via an RPC call
class RequestNextStatesFlow: RPCStartableFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // Corda has a set of injectable services which are injected into the flow at runtime.
    // Flows declare them with @CordaInjectable, then the flows have access to their services.

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging provides a service for establishing flow sessions between Virtual Nodes and
    // sending and receiving payloads between them
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup



    // When a flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services
    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("RequestNextStatesFlow.call() called")

        log.info("requestBody: ${requestBody.getRequestBody()}")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, NextStatesStartArgs::class.java)

        val gamestate = NextStatesStartArgs(
            arrayOf(
                booleanArrayOf(true, false, false, true),
                booleanArrayOf(true, false, true, false),
                booleanArrayOf(true, true, false, true),
                booleanArrayOf(false, false, true, false)
            ))

        val ourIdentity = memberLookup.myInfo().name

        val members = memberLookup.lookup().filter { memberInfo ->  memberInfo.name.commonName != "Main"}

        log.info("all members visable from RequestNextStatesFlow:")
        log.info("$members")

        var response = Message(ourIdentity, "Null")

        for (member in members) {
            val message = RequestMessage(ourIdentity, "Request ${member.name.commonName} for next state from ${ourIdentity.commonName}.")
            val session = flowMessaging.initiateFlow(member.name)

            session.send(message)

            // Receive a response from the Responder flow
            response = session.receive(Message::class.java)
        }


        // The return value of a RPCStartableFlow must always be a String, this string will be passed
        // back as the REST RPC response when the status of the flow is queried on Corda, or as the return
        // value from the flow when testing using the Simulator
        return response.message
    }
}

// MyFirstFlowResponder is a responder flow, it's corresponding initiating flow is called MyFirstFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "get-next-states")
// Responder flows must inherit from ResponderFlow
class GetPixelNextState: ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    private fun getCoordinates(name: String): Pair<Int, Int>{
        var (x, y) = name.substring(1).split('Y')
        return Pair<Int, Int>(x.toInt(), y.toInt())
    }

    private fun getAjacentCoordinates(x: Int, y: Int): List<Pair<Int, Int>>{
        val offsets = listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1),
            Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
        val transform: (Pair<Int, Int>) -> Pair<Int, Int> = {Pair(it.first + x, it.second + y)}
        val ajacentNodes: List<Pair<Int, Int>> = offsets.map(transform)
        return ajacentNodes
    }

    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {

        // Useful logging to follow what's happening in the console or logs
        log.info("GOL: GetPixelNextState.call() called")

        // Receive the payload and deserialize it into a Message class
        val receivedMessage = session.receive(RequestMessage::class.java)

        log.info("GOL: Message received: ${receivedMessage.message} ")

        val ourIdentity = memberLookup.myInfo().name
        val (x, y) = getCoordinates(ourIdentity.commonName!!)
        val ajacentCoordinates = getAjacentCoordinates(x, y)
        val members = memberLookup.lookup().filter { memberInfo ->  memberInfo.name.commonName != "Main"}

        log.info("GOL: ajacentCoordinates $ajacentCoordinates")

        val memberCoordinates = members.map{
            getCoordinates(it.name.commonName!!)
        }

        log.info("GOL: memberCoordinates - $memberCoordinates")

        var numAlive = 0
        for (ajacentCoordinate in ajacentCoordinates) {
            if (ajacentCoordinate in memberCoordinates) {
                if (Random.nextBoolean()) {
                    numAlive += 1
                }
            }
        }

        var nextState = "Dead"
        if ((numAlive <= 2) or (numAlive >= 4)) {
            nextState = "Dead"
        } else {
            nextState = "Alive"
        }

        val response = Message(ourIdentity, "I (${ourIdentity.commonName}) am $nextState")
        // Log the response to be sent.
        log.info("MFF: response.message: ${response.message}")

        // Send the response via the send method on the flow session
        session.send(response)
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.csdetemplate.MyFirstFlow",
    "requestData": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */