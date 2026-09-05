package GameFlow.UI

import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout

import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

import GameFlow.Models.AutomationStatus
import GameFlow.Models.DashboardModel

/**
 * Dashboard view (tab). Plain labels repopulated by a Swing timer reading the
 * {@link DashboardModel}; no engine logic lives here, keeping the view/animated
 * concerns separate from automation (MVVM-style).
 */
class DashboardPanel(private val vm: MainViewModel) : JPanel() {

    private val lblGame = JLabel("", SwingConstants.LEFT)
    private val lblGameStatus = JLabel("Waiting for game selection")
    private val lblAutomation = JLabel("Idle")
    private val lblState = JLabel("-")
    private val lblTask = JLabel("-")
    private val lblProgress = JLabel("0 / 0")
    private val lblError = JLabel(" ")

    private val btnStart = JButton("START")
    private val btnPause = JButton("PAUSE")
    private val btnResume = JButton("RESUME")
    private val btnStop = JButton("STOP")

    init {
        setLayout(BorderLayout())

        val info = JPanel(GridLayout(0, 1, 6, 6))
        info.setBorder(BorderFactory.createTitledBorder("Dashboard"))
        info.add(row("Game", lblGame))
        info.add(row("Game Status", lblGameStatus))
        info.add(row("Automation", lblAutomation))
        info.add(row("Current State", lblState))
        info.add(row("Current Task", lblTask))
        info.add(rowLabel("Progress", lblProgress))
        info.add(rowLabel("Error Status", lblError))
        add(info, BorderLayout.CENTER)

        val buttons = JPanel(FlowLayout())
        btnStart.addActionListener { vm.start() }
        btnPause.addActionListener { vm.pause() }
        btnResume.addActionListener { vm.resume() }
        btnStop.addActionListener { vm.stop() }
        buttons.add(btnStart)
        buttons.add(btnPause)
        buttons.add(btnResume)
        buttons.add(btnStop)
        add(buttons, BorderLayout.SOUTH)
    }

    private fun row(name: String, value: JLabel): JPanel {
        val n = JLabel(name + ":")
        val p = JPanel(FlowLayout(FlowLayout.LEFT))
        p.add(n)
        p.add(value)
        return p
    }

    private fun rowLabel(name: String, value: JLabel): JPanel {
        val n = JLabel(name + ":")
        val p = JPanel(FlowLayout(FlowLayout.LEFT))
        p.add(n)
        p.add(value)
        return p
    }

    /** Called by the owning frame's Timer (EDT) to repaint from the model. */
    fun refresh() {
        val m = vm.model()
        lblGame.setText(m.game().displayName())
        lblGameStatus.setText(m.gameStatus())
        lblAutomation.setText(describe(m.automation()))
        lblState.setText(m.currentState())
        lblTask.setText(m.currentTask())
        lblProgress.setText("" + m.completedTasks() + " / " + m.totalTasks())
        lblError.setText(if (m.errorStatus().isEmpty()) " " else m.errorStatus())
        btnStart.setVisible(!vm.running() && !vm.paused())
        btnStop.setVisible(vm.running() || vm.paused())
        btnPause.setVisible(vm.running())
        btnResume.setVisible(vm.paused())
    }

    private fun describe(s: AutomationStatus): String = if (s == null) "Idle" else s.name
}