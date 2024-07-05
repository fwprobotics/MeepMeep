package com.noahbres.meepmeep.core.ui

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.system.exitProcess

class WindowFrame(title: String, windowWidth: Int, windowHeight: Int) : JFrame() {

    constructor(title: String, windowSize: Int) : this(title, windowSize, windowSize)

    var internalWidth = windowWidth
    var internalHeight = windowHeight

    val canvas = MainCanvas(internalWidth, internalHeight)
    val canvasPanel = JPanel()

    init {
        setTitle(title)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(we: WindowEvent?) {
                super.windowClosing(we)

                dispose()
                exitProcess(0)
            }
        })

        setSize(internalWidth, internalHeight)
        setLocationRelativeTo(null)

        isResizable = false

        layout = BoxLayout(contentPane, BoxLayout.X_AXIS)

        canvasPanel.layout = BoxLayout(canvasPanel, BoxLayout.Y_AXIS)
        canvasPanel.add(canvas)

        contentPane.add(canvasPanel)
        pack()

        canvas.start()
    }
}
