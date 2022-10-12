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

@CordaSerializable
class GameState(val values: Array<CharArray>)

@CordaSerializable
class Message(val sender: MemberX500Name, val message: String)

fun getCoordinates(name: String): Pair<Int, Int>{
    var (x, y) = name.substring(1).split('Y')
    return Pair(x.toInt(), y.toInt())
}

@InitiatedBy(protocol = "game-of-life")
class RequestNextGameStateFlow: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("GOL: RequestNextGamestateFlow.call() called")
        val receivedMessage = session.receive(Message::class.java)
        val currentGamestate = jsonMarshallingService.parse(receivedMessage.message, Array<CharArray>::class.java)

        val nextGamestate = currentGamestate.map{CharArray(it.size)}
        val ourIdentity = memberLookup.myInfo().name

        val members = memberLookup.lookup().filter { memberInfo ->  memberInfo.name.organisationUnit != "Admin"}

        for (member in members) {
            var memberCoordinates = getCoordinates(member.name.commonName!!)

            val getNextPixelSubflow = GetNextPixelSubflow(session, member.name, currentGamestate)
            val response = flowEngine.subFlow(getNextPixelSubflow)

            log.info("setting $memberCoordinates to ${response}")
            nextGamestate[memberCoordinates.first][memberCoordinates.second] = response.toCharArray()[0]
        }

        log.info("New gamestate: ")
        for (row in nextGamestate) {
            log.info(row.joinToString(","))
        }

        val newGameState = jsonMarshallingService.format(nextGamestate)

        session.send(Message(ourIdentity, newGameState))
    }
}

@InitiatingFlow("get-next-pixel")
class GetNextPixelSubflow(private val existingSession: FlowSession, private val x500Name: MemberX500Name, private val gamestate: Array<CharArray>) : SubFlow<String> {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(): String {
        val newSession: FlowSession = flowMessaging.initiateFlow(x500Name)

        val newMessage = Message(x500Name, jsonMarshallingService.format(gamestate))
        newSession.send(newMessage)

        val message = newSession.receive(Message::class.java)

        return message.message
    }
}

@InitiatedBy(protocol = "get-next-pixel")
class GetPixelNextState: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    private fun getAjacentCoordinates(x: Int, y: Int): List<Pair<Int, Int>>{
        val offsets = listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1),
            Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
        val transform: (Pair<Int, Int>) -> Pair<Int, Int> = {Pair(it.first + x, it.second + y)}
        val ajacentNodes: List<Pair<Int, Int>> = offsets.map(transform)
        return ajacentNodes
    }

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("GOL: GetPixelNextState.call() called")

        val receivedMessage = session.receive(Message::class.java)

        val gamestate = jsonMarshallingService.parse(receivedMessage.message, Array<CharArray>::class.java)

        val ourIdentity = memberLookup.myInfo().name
        val (x, y) = getCoordinates(ourIdentity.commonName!!)
        val ajacentCoordinates = getAjacentCoordinates(x, y)
        val members = memberLookup.lookup().filter { memberInfo ->  memberInfo.name.organisationUnit != "Admin"}

        val memberCoordinates = members.map{
            getCoordinates(it.name.commonName!!)
        }

//        var ajacentValues = ""
        var numAlive = 0
        for (ajacentCoordinate in ajacentCoordinates) {
            if (ajacentCoordinate in memberCoordinates) {
//                ajacentValues += ", ${gamestate.get(ajacentCoordinate.first).get(ajacentCoordinate.second)}"
                if (gamestate.get(ajacentCoordinate.first).get(ajacentCoordinate.second) == 'X') {
                    numAlive += 1
                }
            }
        }

        val currentState = gamestate.get(x).get(y)
        var nextState = '.'

        if (currentState == 'X' && (numAlive < 2 || numAlive > 3)) {
            nextState = '.'
        } else if (currentState == '.' && numAlive == 3) {
            nextState = 'X'
        } else {
            nextState = currentState
        }

        val response = Message(ourIdentity, "$nextState")

//        log.info("${ourIdentity.commonName} current value: $currentState")
//        log.info("values around ${ourIdentity.commonName}: $ajacentValues")
//        log.info("numAlive: $numAlive")
//        log.info("${ourIdentity.commonName} next value: ${response.message}")

        session.send(response)
    }
}

//@InitiatingFlow(protocol = "get-next-state")
//class RequestIndividualStateFlow: RPCStartableFlow {
//
//    private companion object {
//        val log = contextLogger()
//    }
//
//    @CordaInject
//    lateinit var jsonMarshallingService: JsonMarshallingService
//
//    @CordaInject
//    lateinit var flowMessaging: FlowMessaging
//
//    @CordaInject
//    lateinit var flowEngine: FlowEngine
//
//    @CordaInject
//    lateinit var memberLookup: MemberLookup
//
//    @Suspendable
//    override fun call(requestBody: RPCRequestData): String {
//
//        log.info("RequestNextStatesFlow.call() called")
//        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GameState::class.java)
//
//        val gamestate = flowArgs.values
//
//        val ourIdentity = memberLookup.myInfo().name
//
//        val members = memberLookup.lookup().filter { memberInfo ->  memberInfo.name.organisationUnit != "Admin"}
//
//        for (member in members) {
//            log.info("member name: ${member.name.commonName}")
//            var memberCoordinates = getCoordinates(member.name.commonName!!)
//            val session = flowMessaging.initiateFlow(member.name)
//
//            val getNextPixelSubflow = GetNextPixel(session, member.name, gamestate)
//            val response = flowEngine.subFlow(getNextPixelSubflow)
//
//            log.info("setting $memberCoordinates to ${response}")
//            gamestate[memberCoordinates.first][memberCoordinates.second] = response.toCharArray()[0]
//        }
//
//        log.info("resultant gamestate: ")
//        for (row in gamestate) {
//            log.info(row.joinToString(","))
//        }
//
//        val newGameState = jsonMarshallingService.format(gamestate)
//        return newGameState
//    }
//}