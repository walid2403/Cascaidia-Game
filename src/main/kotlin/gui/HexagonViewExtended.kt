package gui

import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.visual.Visual

class HexagonViewExtended(size: Double, visual: Visual) : HexagonView(size = size, visual = visual) {
    public var inShop = false
    public var isPlaced = false
    public var choiceHex = false
}