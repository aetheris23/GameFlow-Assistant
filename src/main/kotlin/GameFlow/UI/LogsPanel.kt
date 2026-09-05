package GameFlow.UI

import java.awt.BorderLayout

import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

import GameFlow.Services.LoggingService

/**
 * Log view. Shows the ring buffer from the logging service; DEBUG lines are only
 * appended while debug logging is on (default off) to save resources. The engine
 * pushes updates through the collector below on its own thread; appending happens
 * on the EDT via a Swing Timer.
 */
class LogsPanel : JPanel() {

    private val area = JTextArea(20, 70)

    init {
        setLayout(BorderLayout())
        area.setEditable(false)
        area.setLineWrap(true)
        val scroll = JScrollPane(area)
        scroll.setBorder(BorderFactory.createTitledBorder("Logs"))
        add(scroll, BorderLayout.CENTER)
    }

    fun appendLine(log: LoggingService) {
        val lines = log.recent()
        val sb = StringBuilder()
        for (e in lines) sb.append(e.toString()).append('\n')
        area.setText(sb.toString())
        area.setCaretPosition(area.getDocument().getLength())
    }
}