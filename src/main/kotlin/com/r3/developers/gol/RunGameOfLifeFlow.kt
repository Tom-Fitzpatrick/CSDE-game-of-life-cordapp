package com.r3.developers.gol

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger

@CordaSerializable
class GameInit(val rounds: Int, val initialGamestate: Array<CharArray>)

@InitiatingFlow(protocol = "game-of-life")
class RunGameOfLifeFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("GOL RunGameOfLifeFlow.call() called")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GameInit::class.java)
        log.info("${flowArgs.initialGamestate}")

        val initialGamestate = flowArgs.initialGamestate
        val gameRounds = flowArgs.rounds

        val ourIdentity = memberLookup.myInfo().name
        var message = Message(ourIdentity, jsonMarshallingService.format(initialGamestate))

        var allGamestates = mutableListOf<Array<CharArray>>()
        allGamestates.add(initialGamestate)

        for (i in 1..gameRounds) {
            val session = flowMessaging.initiateFlow(MemberX500Name.parse("CN=State, OU=Admin, O=R3, L=London, C=GB"))
            session.send(message)
            message = session.receive(Message::class.java)

            val newGamestate = jsonMarshallingService.parse(message.message, Array<CharArray>::class.java)
            allGamestates.add(newGamestate)
        }

        for ((i, gamestate) in allGamestates.withIndex()) {
            log.info("\n\nGamestate $i:")
            for (row in gamestate) {
                log.info("|  ${row.joinToString(" ")}")
            }
        }

        return jsonMarshallingService.format(allGamestates)
    }
}
