package com.noahbres.meepmeep.roadrunner.entity

import com.acmerobotics.roadrunner.Action
import com.acmerobotics.roadrunner.Pose2d
import com.noahbres.meepmeep.MeepMeep
import com.noahbres.meepmeep.core.colorscheme.ColorScheme
import com.noahbres.meepmeep.core.entity.BotEntity
import com.noahbres.meepmeep.core.entity.EntityEventListener
import com.noahbres.meepmeep.core.exhaustive
import com.noahbres.meepmeep.core.util.FieldUtil
import com.noahbres.meepmeep.roadrunner.Constraints
import com.noahbres.meepmeep.roadrunner.DriveShim
import com.noahbres.meepmeep.roadrunner.DriveTrainType
import com.noahbres.meepmeep.roadrunner.ui.TrajectoryProgressSliderMaster
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.min

// TODO(ryanbrott): seems like the bot should own the path entities and selectively update/render the ones
// that need it and also update the pose (perhaps there should be another Entity interface?)
class RoadRunnerBotEntity(
    meepMeep: MeepMeep,
    private var constraints: Constraints,

    width: Double, height: Double,
    pose: Pose2d,

    val colorScheme: ColorScheme,
    opacity: Double,

    private var driveTrainType: DriveTrainType = DriveTrainType.MECANUM,

    var listenToSwitchThemeRequest: Boolean = false
) : BotEntity(meepMeep, width, height, pose, colorScheme, opacity), EntityEventListener {
    override val tag = "RR_BOT_ENTITY"

    override var zIndex: Int = 0

    var drive = DriveShim(driveTrainType, constraints, pose)

    var currentActionTimeline: ActionTimeline? = null

    private var actionEntity: ActionEntity? = null

    var looping = true
    private var running = false

    private var trajectorySequenceElapsedTime = 0.0
        set(value) {
            actionEntity?.trajectoryProgress = value
            field = value
        }

    var trajectoryPaused = false

    private var sliderMaster: TrajectoryProgressSliderMaster? = null
    private var sliderMasterIndex: Int? = null

    override fun update(deltaTime: Long) {
        if (!running) return

        if (!trajectoryPaused) trajectorySequenceElapsedTime += deltaTime / 1e9

        val (dt, timeline) = currentActionTimeline ?: return

        when {
            trajectorySequenceElapsedTime <= dt -> {
                var segPose: Pose2d? = null
                for ((beginTime, seg) in timeline) {
                    // Fill in the gap with the starting pose of the next segment.
                    if (beginTime > trajectorySequenceElapsedTime) {
                        segPose = seg.get(0.0)
                        break
                    }

                    val segmentOffsetTime = trajectorySequenceElapsedTime - beginTime
                    if (segmentOffsetTime < seg.duration) {
                        segPose = seg.get(segmentOffsetTime)
                        break
                    }
                }

                pose = segPose ?: Pose2d(0.0, 0.0, 0.0)

                drive.poseEstimate = pose

                actionEntity!!.markerEntityList.forEach { if (trajectorySequenceElapsedTime >= it.time) it.passed() }

                sliderMaster?.reportProgress(sliderMasterIndex ?: -1, trajectorySequenceElapsedTime)

                Unit
            }

            looping -> {
                actionEntity!!.markerEntityList.forEach {
                    it.reset()
                }
                trajectorySequenceElapsedTime = 0.0

                sliderMaster?.reportDone(sliderMasterIndex ?: -1)
            }

            else -> {
                trajectorySequenceElapsedTime = 0.0
                running = false
//                currentTrajectorySequence = null

                sliderMaster?.reportDone(sliderMasterIndex ?: -1)
            }
        }.exhaustive
    }

    fun export(name: String) {
        println("EXPORTING")
        val (dt, timeline) = currentActionTimeline ?: return
        val pathData: ArrayList<Map<String, Double>> = ArrayList();

                var segPose: Pose2d? = null
                for ((beginTime, seg) in timeline) {

                    for (i in 0..Math.ceil(seg.duration).toInt()) {
                        if (i < seg.duration) {
                            segPose = seg.get(i.toDouble())
                        }

                        if (segPose != null) {
                            pathData.add(
                                mapOf(
                                    "x" to 500-((segPose.position.y+FieldUtil.FIELD_HEIGHT/2)*500/144),
                                    "y" to (500*FieldUtil.FIELD_WIDTH/FieldUtil.FIELD_HEIGHT)-(segPose.position.x+FieldUtil.FIELD_WIDTH/2)*(500*FieldUtil.FIELD_WIDTH/FieldUtil.FIELD_HEIGHT)/FieldUtil.FIELD_WIDTH,
                                    "t" to (beginTime + i).coerceAtMost(beginTime + seg.duration)*1000
                                )
                            )
                        }
                    }
        }
        val pathDataJson = Json.encodeToString( pathData);
        val pathName = "crowdscout_routes/$name.json"
        File("crowdscout_routes").mkdir()
        File(pathName).writeText(pathDataJson)
        println("EXPORTED TO: $pathName")

    }

    fun start() {
        running = true
        trajectorySequenceElapsedTime = 0.0
    }

    fun resume() {
        running = true
    }

    fun pause() {
        trajectoryPaused = true
    }

    fun unpause() {
        trajectoryPaused = false
    }

    fun setTrajectoryProgressSeconds(seconds: Double) {
        val currentActionTimeline = currentActionTimeline
        trajectorySequenceElapsedTime = min(seconds, (currentActionTimeline ?: return).duration)
    }

    fun runAction(action: Action) {
        val t = actionTimeline(action)
        currentActionTimeline = t
        actionEntity = ActionEntity(meepMeep, t, colorScheme)
    }

    fun setConstraints(constraints: Constraints) {
        this.constraints = constraints

        drive = DriveShim(driveTrainType, constraints, pose)
    }

    fun setDriveTrainType(driveTrainType: DriveTrainType) {
        this.driveTrainType = driveTrainType

        drive = DriveShim(driveTrainType, constraints, pose)
    }

    override fun switchScheme(scheme: ColorScheme) {
        if (listenToSwitchThemeRequest)
            super.switchScheme(scheme)
    }

    fun setTrajectoryProgressSliderMaster(master: TrajectoryProgressSliderMaster, index: Int) {
        sliderMaster = master
        sliderMasterIndex = index
    }

    override fun onAddToEntityList() {
        if (actionEntity != null)
            meepMeep.requestToAddEntity(actionEntity!!)
    }

    override fun onRemoveFromEntityList() {
        if (actionEntity != null)
            meepMeep.requestToRemoveEntity(actionEntity!!)
    }
}