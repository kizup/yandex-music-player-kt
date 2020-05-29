package ru.kizapp.ymplayer.view

import ru.kizapp.ymplayer.app.Styles
import tornadofx.*

class MainView : View("Hello TornadoFX") {
    override val root = hbox {
        label(title) {
            addClass(Styles.heading)
        }
    }
}