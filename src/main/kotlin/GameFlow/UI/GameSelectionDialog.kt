package GameFlow.UI

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.BorderLayout

import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

import GameFlow.Models.GameType

/**
 * Startup "Game Selection" dialog (spec #1/#19). The user picks a game; the App
 * then loads that game's profile, settings, and templates.
 */
class GameSelectionDialog : JDialog {

    private val lblTitle = JLabel("Select a game to assist:")
    private val btnUma = JButton("Uma Musume")
    private val btnBlue = JButton("Blue Archive")

    private var chosen: GameType = GameType.NONE

    constructor(owner: JFrame) : super(owner, "GameFlow Assistant - Game Selection", true) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        setSize(Dimension(360, 200))
        setLocationRelativeTo(owner)

        lblTitle.setHorizontalAlignment(SwingConstants.CENTER)

        val root = JPanel()
        root.setLayout(FlowLayout(FlowLayout.CENTER))
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16))
        root.add(lblTitle)

        val buttons = JPanel(FlowLayout(FlowLayout.CENTER, 24, 8))
        buttons.add(btnUma)
        buttons.add(btnBlue)
        root.add(buttons)
        add(root, BorderLayout.CENTER)

        btnUma.addActionListener { choose(GameType.UMA_MUSUME) }
        btnBlue.addActionListener { choose(GameType.BLUE_ARCHIVE) }
    }

    private fun choose(game: GameType) {
        this.chosen = game
        dispose()
    }

    fun chosenGame(): GameType = chosen
}