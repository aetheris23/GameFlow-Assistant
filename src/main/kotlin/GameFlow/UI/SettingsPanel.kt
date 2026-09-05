package GameFlow.UI

import java.awt.GridLayout

import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.event.ChangeListener
import javax.swing.SpinnerNumberModel

import GameFlow.Database.Models.AppSettings

/**
 * Settings view. Reads/writes {@link AppSettings} for the selected game. Changes
 * are applied live by the App (engine reads the same settings each cycle).
 */
class SettingsPanel : JPanel() {

    private val pollActive = JSpinner(SpinnerNumberModel(300, 100, 2000, 50))
    private val pollLoading = JSpinner(SpinnerNumberModel(1000, 200, 5000, 100))
    private val pollIdle = JSpinner(SpinnerNumberModel(1500, 300, 8000, 100))
    private val cooldown = JSpinner(SpinnerNumberModel(400, 0, 3000, 50))
    private val retries = JSpinner(SpinnerNumberModel(5, 1, 30, 1))
    private val timeout = JSpinner(SpinnerNumberModel(15000, 1000, 120000, 500))
    private val debug = JCheckBox("Debug logging (uses little extra CPU)")

    private var current: AppSettings = AppSettings.defaults()

    init { build() }

    private fun build() {
        setLayout(GridLayout(0, 2, 8, 8))
        setBorder(BorderFactory.createTitledBorder("Automation settings (" + current.feature + ")"))
        add(JLabel("Poll (active, ms)"))
        add(pollActive)
        add(JLabel("Poll (loading, ms)"))
        add(pollLoading)
        add(JLabel("Poll (idle, ms)"))
        add(pollIdle)
        add(JLabel("Action cooldown (ms)"))
        add(cooldown)
        add(JLabel("Max retries per action"))
        add(retries)
        add(JLabel("Action timeout (ms)"))
        add(timeout)
        add(JLabel(""))
        add(debug)
    }

    fun load(s: AppSettings) {
        this.current = s
        pollActive.setValue(s.pollActiveMs)
        pollLoading.setValue(s.pollLoadingMs)
        pollIdle.setValue(s.pollIdleMs)
        cooldown.setValue(s.actionCooldownMs)
        retries.setValue(s.maxRetries)
        timeout.setValue(s.actionTimeoutMs)
        debug.setSelected(s.debugLogging)
    }

    fun apply(onChanged: ChangeListener) {
        for (sp in listOf(pollActive, pollLoading, pollIdle, cooldown, retries, timeout)) {
            sp.addChangeListener(onChanged)
        }
        debug.addChangeListener(onChanged)
    }

    fun snapshot(): AppSettings {
        return AppSettings(
            intOf(pollLoading),
            intOf(pollActive),
            intOf(pollIdle),
            intOf(cooldown),
            intOf(retries),
            intOf(timeout),
            current.feature,
            current.ocrEnabled,
            debug.isSelected())
    }

    private fun intOf(sp: JSpinner): Int = sp.getValue() as Int
}