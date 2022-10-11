package com.r3.developers.gol

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger

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

        log.info("RunGameOfLifeFlow.call() called")

        log.info("requestBody: ${requestBody.getRequestBody()}")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GameState::class.java)
        log.info("${flowArgs.initialValues}")

        val initialGamestate = flowArgs.initialValues

        log.info("trying to get all members")
        val members = memberLookup.lookup()
        for (member in members) {
            log.info(member.name.commonName)
        }

        log.info("got gamestate, getting ourIdentity next")

        val ourIdentity = memberLookup.myInfo().name

        log.info("ourIdentity ${ourIdentity.commonName}")

        var message = Message(ourIdentity, jsonMarshallingService.format(initialGamestate))

        var allGamestates = mutableListOf<Array<CharArray>>()

        for (i in 1..4) {
            val session = flowMessaging.initiateFlow(MemberX500Name.parse("CN=State, OU=Admin, O=R3, L=London, C=GB"))

            session.send(message)

            message = session.receive(Message::class.java)

            val newGamestate = jsonMarshallingService.parse(message.message, Array<CharArray>::class.java)
            allGamestates.add(newGamestate)
        }

        for ((i, gamestate) in allGamestates.withIndex()) {
            log.info("\nGamestate $i:")
            for (row in gamestate) {
                log.info(row.joinToString(" "))
            }
        }

        return message.message
    }
}
