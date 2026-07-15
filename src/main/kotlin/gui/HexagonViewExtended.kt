package gui

import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.visual.Visual

/**
 * Die Klasse [HexagonViewExtended] ist eine Erweiterung des HexagonViews um abspeichern zu können, ob es ein gelegtes
 * Tile ist oder ein graues Hexagon, wo man ein Tile platzieren darf
 * @param size ein Objekt des Typs [Double], die Größe des HexagonViews
 * @param visual ein Objekt des Typs [Visual], das Visual des HexagonViews
 * @property choiceHex ein Objekt des Typs [Boolean], ob das Tile ein gaues Hexagon ist
 */
class HexagonViewExtended(size: Double, visual: Visual) : HexagonView(size = size, visual = visual) {
    var choiceHex = false
}