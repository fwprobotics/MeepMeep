package com.noahbres.meepmeep.example

import com.acmerobotics.roadrunner.Pose2d
import com.noahbres.meepmeep.MeepMeep
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder

fun main() {
    System.setProperty("sun.java2d.opengl", "true")

    val meepMeep = MeepMeep(1200, 900, 60)

    val bot = DefaultBotBuilder(meepMeep)
        .build()

    val action = bot.drive.actionBuilder(Pose2d(0.0, 0.0, 0.0))
        .lineToX(50.0)
        .build()

    bot.runAction(action)

    meepMeep.setBackground(MeepMeep.Background.FIELD_CENTERSTAGE_JUICE_LIGHT_CRI)
        .setDarkMode(false)
        .setBackgroundAlpha(0.95f)
        .addEntity(bot)
        .start()
}