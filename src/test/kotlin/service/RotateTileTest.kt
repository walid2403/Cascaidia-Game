package service

import entity.GameState
import entity.Habitates
import entity.PlayerType
import entity.Tile
import entity.WildlifeToken
import kotlin.test.*

/**
 * Klasse um Methode [PlayerActionService.rotateTile] zu testen
 */
class RotateTileTest {

        private lateinit var rootService: RootService
        private lateinit var playerActionService: PlayerActionService
        private lateinit var gameService: GameService

        /**
         * setUp
         */
        @BeforeTest
        fun setUp() {
            rootService = RootService()
            playerActionService = rootService.playerActionService
            gameService = rootService.gameService

        }

        /**
         * damit Spielerliste für [GameService.startNewGame] direkt erstellt wird
         */
        private fun setUpPlayers():List<Pair<String, PlayerType>> {
            return listOf(Pair("Mert",PlayerType.EASY_BOT),Pair("Lotfi", PlayerType.HUMAN))
        }
        /**
         * damit Valueliste für [GameService.startNewGame] direkt erstellt wird
         */
        private fun setUpCards():List<Boolean> {
            return listOf(true,true,true,true,true)
        }

        /**
         * versichern dass Methode nur in madechoice funktioniert
         */
        @Test
        fun `fails with false gameState`(){
            gameService.startNewGame(setUpPlayers(),setUpCards())
            val game=rootService.currentGame!!

            val gameStates= listOf<GameState>(
                GameState.PLAYED_TILE,
                GameState.START_OF_TURN,
                GameState.END_OF_TURN,
                GameState.HAS_EXTERMINATED
            )
            for(gameState in gameStates){
                game.gameState=gameState
                assertFailsWith<IllegalStateException> {
                    playerActionService.rotateTile(right = true)
                }
            }

        }

        /**
         * teste ob die rotation nach rechts korrekt
         */
        @Test
        fun `rotation to the right correctly`(){
            gameService.startNewGame(setUpPlayers(),setUpCards())
            val game=rootService.currentGame!!


            //0;PPPFFF;EB;no
            val habs= mutableListOf<Habitates>(
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.FORESTS,
                Habitates.FORESTS,
                Habitates.FORESTS
            )
            val possibles= listOf<WildlifeToken>(
                WildlifeToken.ELK,
                WildlifeToken.BEAR
            )

            val tile= Tile(0,habs,possibles)

            game.gameState= GameState.MADE_CHOICE
            game.choices.clear()
            game.choices.add(Pair(tile, WildlifeToken.FOX))

            game.selectedChoice=Pair(0,0)

            playerActionService.rotateTile(right = true)

            val expectedRightRotation=mutableListOf<Habitates>(
                Habitates.FORESTS,
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.FORESTS,
                Habitates.FORESTS,
            )

            assertEquals(expectedRightRotation,game.choices[0].first.habs)


        }

        /**
         * teste ob die rotation nach links korrekt
         */
        @Test
        fun `rotation to the left correctly`(){
            gameService.startNewGame(setUpPlayers(),setUpCards())
            val game=rootService.currentGame!!


            //0;PPPFFF;EB;no
            val habs= mutableListOf<Habitates>(
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.FORESTS,
                Habitates.FORESTS,
                Habitates.FORESTS
            )
            val possibles= listOf<WildlifeToken>(
                WildlifeToken.ELK,
                WildlifeToken.BEAR
            )

            val tile= Tile(0,habs,possibles)

            game.gameState= GameState.MADE_CHOICE
            game.choices.clear()
            game.choices.add(Pair(tile, WildlifeToken.FOX))

            game.selectedChoice=Pair(0,0)

            playerActionService.rotateTile(right = false)

            val expectedLeftRotation=mutableListOf<Habitates>(
                Habitates.PRAIRIES,
                Habitates.PRAIRIES,
                Habitates.FORESTS,
                Habitates.FORESTS,
                Habitates.FORESTS,
                Habitates.PRAIRIES
            )

            assertEquals(expectedLeftRotation,game.choices[0].first.habs)
        }
}