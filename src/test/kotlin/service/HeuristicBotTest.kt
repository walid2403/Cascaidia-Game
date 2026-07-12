package service

import entity.*
import service.bot.HeuristicBot
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.*

class HeuristicBotTest {

    private lateinit var rootService: RootService
    private lateinit var bot: HeuristicBot

    // @BeforeEach sorgt dafür, dass dieser Block VOR JEDEM einzelnen Test neu ausgeführt wird.
    @BeforeEach
    fun setup() {
        rootService = RootService()
        val currentGame = CascadiaGame(List(5) { true }, true)

        // 1. Tile Stack vorbereiten
        val tileStack = mutableListOf<Tile>()

        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            tileStack.add(Tile(i, habitats, emptyList()))
        }
        currentGame.tileStack.pushAll(tileStack)
        currentGame.natureTokens = 10

        // 2. DEN MARKT MANIPULIEREN (Wir legen absichtlich 4 BÄREN hin!)
        val choices = mutableListOf<Pair<Tile, WildlifeToken>>()
        for (i in 0 until 4) {
            val pair = Pair(
                Tile(10 + i, habitats, emptyList()),
                WildlifeToken.BEAR // Genau das, was wir für den Überpopulations-Test brauchen!
            )
            choices.add(pair)
        }
        currentGame.choices.addAll(choices)
        currentGame.selectedChoice = Pair(-1, -1)

        // Der Bot fängt gerade seinen Zug an
        currentGame.gameState = GameState.START_OF_TURN

        // 3. Genau einen Spieler (den Bot) hinzufügen
        val botPlayer = Player("BotSpieler", PlayerType.HARD_BOT)
        botPlayer.natureTokens = 3
        currentGame.playerQueue.add(botPlayer)

        // 4. Token Stack füllen (damit beim Wischen neue Tiere gezogen werden können).
        // Wir machen eine bunte Mischung, damit nie 4 gleiche gezogen werden!
        val mixedTokens = listOf(WildlifeToken.SALMON, WildlifeToken.HAWK, WildlifeToken.ELK, WildlifeToken.FOX,
            WildlifeToken.BEAR)
        val wildlifeTokens = List(30) { index -> mixedTokens[index % 5] }
        currentGame.wildlifeTokens.pushAll(wildlifeTokens)

        // Spiel in den Service laden
        rootService.currentGame = currentGame

        // Bot initialisieren
        bot = HeuristicBot(rootService)

    }

    /**
     * Testet, ob der Bot bei 4 gleichen Tieren automatisch das Wischen (Exterminate) einleitet.
     */
    @Test
    fun testOverpopulationWithFourIdenticalAnimals() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame
        checkNotNull(game)

        // Wir stellen sicher, dass die Ausgangslage stimmt (4 Bären)
        assertEquals(GameState.START_OF_TURN, game.gameState)
        assertEquals(4, game.choices.count { it.second == WildlifeToken.BEAR })

        // --- 2. ACT ---
        // wir rufen den Bot auf
        bot.makeTurn()

        // --- 3. ASSERT ---
        // wenn der Bot richtig funktioniert, ruft er rootService.gameService.exterminate(false) auf.
        // Die exterminate-Methode des GameServices sollte daraufhin den Zustand des Spiels
        assertEquals(GameState.START_OF_TURN, game.gameState)

        // zählen wir die Bären im Markt. Es dürfen jetzt keine 4 Bären mehr da sein!
        val bearCount = game.choices.count { it.second == WildlifeToken.BEAR }
        assert(bearCount < 4) { "Die 4 Bären wurden nicht abgeräumt!" }
    }

    /**
     * Testet, ob der Bot bei 4 gleichen Tieren automatisch das Wischen (Exterminate) einleitet.
     */
    @Test
    fun testOverpopulationWithThreeIdenticalAnimals() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        game.gameState = GameState.START_OF_TURN

        // Wir brauchen wieder ein Dummy-Tile
        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        val dummyTile = Tile(99, habitats, emptyList())

        repeat(4) {game.choices.removeAt(0)}

        // Wir manipulieren den Markt: 3 Bären und 1 Lachs!
        repeat(3){game.choices.add(Pair(dummyTile, WildlifeToken.BEAR))}
        game.choices.add(Pair(dummyTile, WildlifeToken.SALMON))

        // --- 2. ACT ---
        // der Bot ist dran. Da sein Board leer ist, bringen Bär und Lachs 0 Punkte (< 15).
        // Er sollte sich also fürs Wischen entscheiden!
        bot.makeTurn()

        // --- 3. ASSERT ---
        // da es ein freiwilliges Wischen war (playerTrigger = true),
        // MUSS der GameState laut eurem Framework jetzt auf HAS_EXTERMINATED stehen!
        assertEquals(GameState.HAS_EXTERMINATED, game.gameState)

        // Zur Sicherheit prüfen wir noch, ob die 3 Bären wirklich weg sind
        val bearCount = game.choices.count { it.second == WildlifeToken.BEAR }
        assert(bearCount < 3) { "Die 3 Bären wurden nicht abgeräumt!" }
    }

    /**Der Bot behält 3 gleiche Tiere und wählt ein Markt-Paar*/
    @Test
    fun testKeepThreeIdenticalAnimalsIfScoreIsHighAndChooseMarket(){

        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        game.gameState = GameState.START_OF_TURN
        val botPlayer = game.playerQueue.peek()!!

        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        // Plättchen 1 (Ziel für den Bot)
        val startTile = Tile(1, habitats, listOf(WildlifeToken.BEAR))
        botPlayer.board[Triple(0, 0, 0)] = startTile

        // Plättchen 2 (Mit einem Bären darauf, der als Nachbar dient!)
        val neighborTile = Tile(2, habitats, listOf(WildlifeToken.BEAR))
        neighborTile.occupant = WildlifeToken.BEAR // Ein Bär liegt schon hier
        botPlayer.board[Triple(1, 0, -1)] = neighborTile

        game.choices.clear()

        // Den Markt präparieren: 3 Bären, 1 Lachs
        val dummyTile = Tile(99, habitats, listOf(WildlifeToken.BEAR))
        // Wir manipulieren den Markt: 3 Bären und 1 Lachs!
        repeat(3){game.choices.add(Pair(dummyTile, WildlifeToken.BEAR))}
        game.choices.add(Pair(dummyTile, WildlifeToken.SALMON))

        // --- 2. ACT ---
        // der Bot berechnet nun den Score für den Bären. Da er ihn platzieren kann,
        // bekommt er Punkte dafür (> 15). Er wird nicht wischen, sondern selectColumn() aufrufen.
        bot.makeTurn()

        // --- 3. ASSERT ---
        // 1. Beweis: Es wurde NICHT gewischt (die 3 Bären liegen noch da)
        val bearCount = game.choices.count { it.second == WildlifeToken.BEAR }
        assertEquals(3, bearCount,
            "Der Bot hat gewischt, obwohl die Bären Punkte bringen!")

        // 3. Beweis: Der Status des Spiels ist weitergegangen
        // (normalerweise MADE_CHOICE, nachdem man eine Spalte gewählt hat)
        assertEquals(GameState.MADE_CHOICE, game.gameState)
    }

    /**
     * Wenn ich ein Tile ausgewählt habe, platziert der Bot es dann korrekt auf dem Board
     * der Bot platziert korrekt das WildlifeToken
     */
    @Test
    fun testBotPlacesTileCorrectly() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        val botPlayer = game.playerQueue.peek()!!
        game.gameState = GameState.START_OF_TURN // Richtig für Schritt 1

        val habitats1 = MutableList(6) { Habitates.MOUNTAINS }

        // 1. Das besetzte Plättchen (bringt die Nachbar-Punkte!)
        val startTile = Tile(1, habitats1, listOf(WildlifeToken.BEAR))
        botPlayer.board[Triple(0, 0, 0)] = startTile
        startTile.occupant = WildlifeToken.BEAR

        // 2. NEU: Das LEERE Plättchen (hier sieht der Bot: "Ah, hier kann ich den neuen Bären ablegen!")
        val emptyTile = Tile(2, habitats1, listOf(WildlifeToken.BEAR))
        botPlayer.board[Triple(1, -1, 0)] = emptyTile // Direkt daneben legen


        // Markt vorbereiten
        game.choices.clear()
        val dummyTile1 = Tile(99, habitats1, listOf(WildlifeToken.SALMON))
        val targetTile = Tile(999, habitats1, listOf(WildlifeToken.BEAR)) // DAS soll er wählen

        // Wir geben ihm 4 Auswahlmöglichkeiten
        game.choices.add(Pair(dummyTile1, WildlifeToken.SALMON))
        game.choices.add(Pair(targetTile, WildlifeToken.BEAR)) // Index 1
        game.choices.add(Pair(dummyTile1, WildlifeToken.FOX))
        game.choices.add(Pair(dummyTile1, WildlifeToken.SALMON))

        // --- 2. ACT & ASSERT (Schritt für Schritt) ---

        // SCHRITT 1: Bot muss jetzt wählen
        bot.makeTurn()

        // Prüfen, ob der Bot überhaupt etwas ausgewählt hat
        assertNotEquals(Pair(-1, -1), game.selectedChoice,
            "Der Bot hat gar kein Paar ausgewählt! Er hat keine Punkte gefunden.")

        // Prüfen: GameState muss jetzt gewechselt sein
        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "Fehler: GameState ist nicht auf MADE_CHOICE gewechselt!")

        // Prüfen: Hat er das richtige Tile gewählt?
        assertEquals(1, game.selectedChoice.first,
            "Bot hat das falsche Tile gewählt!")

        // SCHRITT 2: Bot platziert das Tile
        bot.makeTurn()
        assertEquals(GameState.PLAYED_TILE, game.gameState, "Tile wurde nicht platziert")

        // SCHRITT 3: Bot platziert das WildlifeToken
        bot.makeTurn()
        assertEquals(GameState.END_OF_TURN, game.gameState,
            "wildLifeToken wurde nicht platziert")
        assertTrue { emptyTile.occupant == WildlifeToken.BEAR }
    }

    /**
     * Testet, dass der Bot nicht hängen bleibt, wenn kein Wischen möglich ist
     * und alle Auswahlen im Markt auf seinem Board nicht platziert werden können (0 Punkte).
     * Er muss trotzdem eine Wahl treffen!
     */
    @Test
    fun testBotChoosesSomethingEvenIfAllScoresAreZero() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        val botPlayer = game.playerQueue.peek()!!
        game.gameState = GameState.START_OF_TURN

        val habitats = MutableList(6) { Habitates.MOUNTAINS }

        // 3 Start-Plättchen erstellen, wie es im echten Spiel ist.
        // TRICK: Sie sind LEER, aber sie erlauben ALLE NUR den Bussard (HAWK).
        val startTile1 = Tile(1, habitats, listOf(WildlifeToken.HAWK))
        val startTile2 = Tile(2, habitats, listOf(WildlifeToken.HAWK))
        val startTile3 = Tile(3, habitats, listOf(WildlifeToken.HAWK))

        // Auf dem Board platzieren (zusammenhängend)
        botPlayer.board[Triple(0, 0, 0)] = startTile1
        botPlayer.board[Triple(1, -1, 0)] = startTile2
        botPlayer.board[Triple(0, 1, -1)] = startTile3


        // Markt präparieren: 4 VERSCHIEDENE Tiere, und KEIN Bussard ist dabei!
        // Da keine 3 gleichen Tiere ausliegen, darf er nicht wischen.
        // Da kein Bussard da ist, kann er kein Tier auf seinem Board ablegen (Score = 0).
        game.choices.clear()
        val dummyTile = Tile(99, habitats, listOf(WildlifeToken.BEAR))
        game.choices.add(Pair(dummyTile, WildlifeToken.BEAR))
        game.choices.add(Pair(dummyTile, WildlifeToken.SALMON))
        game.choices.add(Pair(dummyTile, WildlifeToken.FOX))
        game.choices.add(Pair(dummyTile, WildlifeToken.ELK))

        // --- 2. ACT ---
        bot.makeTurn() // Er muss jetzt den Markt evaluieren

        // --- 3. ASSERT ---
        // Beweis: Er hat sich NICHT aufgehängt, sondern irgendwas (z.B. das Erste) gewählt.
        assertNotEquals(Pair(-1, -1), game.selectedChoice,
            "Der Bot hat aufgegeben, weil alles 0 Punkte bringt! Er muss aber etwas wählen.")

        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "GameState hat sich nicht geändert. Bot steckt fest.")
    }

    /**
     * Testet, dass der Bot einen Natur-Zapfen (Nature Token) einsetzt (Free Selection),
     * wenn die normalen Marktpaare schlecht bewertet werden (< 30), aber eine
     * Kombination über Kreuz deutlich mehr Punkte bringt.
     */
    @Test
    fun testBotUsesNatureTokenForFreeSelection() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        val botPlayer = game.playerQueue.peek()!!
        game.gameState = GameState.START_OF_TURN

        // GIB DEM BOT EINEN ZAPFEN!
        botPlayer.natureTokens = 1

        // Board vorbereiten: Wir legen ein Wald-Plättchen in die Mitte,
        // das nur BÄREN akzeptiert.
        val habitatsForest = MutableList(6) { Habitates.FORESTS }
        val startTile = Tile(1, habitatsForest, listOf(WildlifeToken.BEAR))
        botPlayer.board.clear()
        botPlayer.board[Triple(0, 0, 0)] = startTile

        // Markt vorbereiten
        game.choices.clear()
        val habitatsRIVERS = MutableList(6) { Habitates.RIVERS }

        // Spalte 0: Super Plättchen (Wald passt an Wald), aber schlechtes Tier (Fuchs)
        game.choices.add(Pair(Tile(2, habitatsForest, listOf(WildlifeToken.FOX)), WildlifeToken.FOX))

        // Spalte 1: Super Plättchen, schlechtes Tier (Lachs)
        game.choices.add(Pair(Tile(3, habitatsForest, listOf(WildlifeToken.SALMON)), WildlifeToken.SALMON))

        // Spalte 2: Super Plättchen, schlechtes Tier (Bussard)
        game.choices.add(Pair(Tile(4, habitatsForest, listOf(WildlifeToken.HAWK)), WildlifeToken.HAWK))

        // Spalte 3: Schlechtes Plättchen (Wüste passt nicht an Wald), aber SUPER Tier (Bär passt!)
        game.choices.add(Pair(Tile(5, habitatsRIVERS, listOf(WildlifeToken.BEAR)), WildlifeToken.BEAR))

        // --- 2. ACT ---
        bot.makeTurn()

        // --- 3. ASSERT ---
        // wenn der Bot Free Selection genutzt hat, müssten die Indizes für Plättchen und Tier
        // in der "selectedChoice" jetzt unterschiedlich sein.
        val selectedTileIndex = game.selectedChoice.first
        val selectedAnimalIndex = game.selectedChoice.second

        assertNotEquals(selectedTileIndex, selectedAnimalIndex,
            "Der Bot hat keinen Zapfen genutzt! Er hat ein reguläres Paar gewählt.")

        assertEquals(3, selectedAnimalIndex,
            "Der Bot hätte den Bären (Index 3) als Tier wählen sollen, weil er am besten passt.")

        assertTrue(selectedTileIndex in 0..2,
            "Der Bot hätte eines der Wald-Plättchen (Index 0, 1 oder 2) wählen sollen.")
    }

    /**
     * Testet, dass der Bot ein Tier (Wildlife Token) korrekt verwirft,
     * wenn es auf dem gesamten Board keinen gültigen Platz dafür gibt.
     */
    @Test
    fun testBotDiscardsWildlifeTokenWhenNoValidPosition() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        val botPlayer = game.playerQueue.peek()!!

        // Wir simulieren, dass das Landschaftsplättchen bereits gelegt wurde.
        // Der Bot ist also direkt in der Phase der Tier-Platzierung
        game.gameState = GameState.PLAYED_TILE

        // Board vorbereiten: Ein Plättchen in die Mitte legen.
        // WICHTIG: Dieses Plättchen erlaubt NUR Füchse (FOX)!
        botPlayer.board.clear()
        val startTile = Tile(1, mutableListOf(Habitates.FORESTS), listOf(WildlifeToken.FOX))
        botPlayer.board[Triple(0, 0, 0)] = startTile

        // Markt vorbereiten: Wir tun so, als hätte der Bot Spalte 0 gewählt.
        // Dort liegt als Tier ein BÄR.
        game.choices.clear()
        game.choices.add(
            Pair(
                Tile(2, mutableListOf(Habitates.RIVERS), listOf(WildlifeToken.BEAR)),
                WildlifeToken.BEAR
            )
        )
        // Setze die Auswahl auf Index 0 für Plättchen und Index 0 für Tier
        game.selectedChoice = Pair(0, 0)

        // --- 2. ACT ---
        bot.makeTurn()

        // --- 3. ASSERT ---
        // wir prüfen, ob das Plättchen auf dem Board immer noch leer ist (occupant = null)
        val tileOnBoard = botPlayer.board[Triple(0, 0, 0)]!!

        assertNull(tileOnBoard.occupant,
            "Fehler: Der Bot hat den Bären platziert, obwohl das Plättchen nur Füchse erlaubt!")

        // Wir prüfen, dass der Bot den Zug korrekt beendet hat.
        // Entweder steht es auf END_OF_TURN, oder (falls changeTurn aufgerufen wurde)
        // bereits auf START_OF_TURN für den nächsten Spieler.
        assertEquals(
            game.gameState,
            GameState.END_OF_TURN,
            "Fehler: Der GameState hätte den Zug beenden müssen, steht aber auf ${game.gameState}"
        )

    }

    /**
     * Testet, dass der Bot ein Landschaftsplättchen korrekt rotiert,
     * um perfekt passende Kanten (Habitate) zu erzeugen und maximale Punkte zu holen.
     */
    @Test
    fun testBotRotatesTileForMaximumScore() {
        // --- 1. ARRANGE ---
        val game = rootService.currentGame!!
        val botPlayer = game.playerQueue.peek()!!

        // Wir simulieren, dass der Bot sein Markt-Paar bereits gewählt hat
        // und das Plättchen nun auf dem Board platzieren muss
        game.gameState = GameState.MADE_CHOICE
        game.selectedChoice = Pair(0, 0) // Spalte 0 wurde gewählt

        // Board vorbereiten: Start-Plättchen in die Mitte.
        // Index 1 (Rechte Kante) ist WASSER, der Rest ist WALD.
        botPlayer.board.clear()
        val startHabs = mutableListOf(
            Habitates.FORESTS, Habitates.RIVERS, Habitates.FORESTS,
            Habitates.FORESTS, Habitates.FORESTS, Habitates.FORESTS
        )
        val startTile = Tile(1, startHabs, listOf(WildlifeToken.BEAR))
        botPlayer.board[Triple(0, 0, 0)] = startTile

        // Markt vorbereiten: Das neue Plättchen hat WASSER auf Index 0, Rest ist WÜSTE.
        val marketHabs = mutableListOf(
            Habitates.RIVERS, Habitates.MOUNTAINS, Habitates.MOUNTAINS,
            Habitates.MOUNTAINS, Habitates.MOUNTAINS, Habitates.MOUNTAINS
        )
        val newTile = Tile(2, marketHabs, listOf(WildlifeToken.BEAR))
        newTile.rotation = 0 // Start-Rotation ist 0

        game.choices.clear()
        game.choices.add(Pair(newTile, WildlifeToken.BEAR))

        // --- 2. ACT ---
        bot.makeTurn()

        // --- 3. ASSERT ---
        // das Plättchen sollte rechts vom Startplättchen liegen, da dort das Wasser ist.
        // (Rechts entspricht den Koordinaten 1, 0, -1 in deinem Koordinatensystem)
        val targetPosition = Triple(1, 0, -1)
        val placedTile = botPlayer.board[targetPosition]

        assertNotNull(placedTile,
            "Fehler: Das Plättchen wurde nicht an der besten Position (1, 0, -1) platziert.")

        // Die Rotation MUSS 4 sein!
        // Erklärung:
        // Startplättchen (0,0,0) hat Wasser auf Kante 1 (Rechts).
        // Das neue Plättchen liegt auf (1,0,-1). Seine Kante 4 (Links) zeigt zurück zum Startplättchen.
        // Das neue Plättchen hat sein Wasser aber auf Kante 0.
        // Um Wasser (0) auf Links (4) zu drehen, braucht es 4 Drehungen!
        assertEquals(4, placedTile.rotation,
            "Fehler: Das Plättchen wurde nicht korrekt gedreht, um Wasser an Wasser (15 Punkte) zu legen!")

    }
}