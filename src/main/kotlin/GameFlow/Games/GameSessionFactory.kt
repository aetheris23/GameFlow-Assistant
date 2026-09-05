package GameFlow.Games

import GameFlow.Input.MouseController
import GameFlow.Models.GameType
import GameFlow.Games.BlueArchive.BlueArchiveProfile
import GameFlow.Games.UmaMusume.UmaMusumeProfile
import GameFlow.Vision.TemplateMatcher

/**
 * Builds the {@link GameFlow.Core.GameSession} for the selected game: picks the
 * matching {@link GameProfile} and wires templates + vision + input together.
 */
class GameSessionFactory private constructor() {

    companion object {

        fun create(game: GameType, matcher: TemplateMatcher,
                   mouse: MouseController, clickCooldownMs: Int): ProfileGameSession {
            val templates = TemplateLibrary()
            val profile = if (game == GameType.UMA_MUSUME) UmaMusumeProfile()
                            else if (game == GameType.BLUE_ARCHIVE) BlueArchiveProfile()
                            else throw IllegalArgumentException("Unsupported game: " + game)
            return ProfileGameSession(profile, templates, matcher, mouse, clickCooldownMs)
        }
    }
}