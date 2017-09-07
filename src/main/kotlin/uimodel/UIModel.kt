/**
 * Created by richard.colvin on 17/03/2017.
 */
package uimodel

class Image {

}

class Date {

}

class Time {

}

class Timestamp {

}

interface UI {
  val name: String
}


interface FieldEditorUI<T> : UI {
  var uiState: UIState<T>
  var updateListener: (String) -> UIState<T>
}

interface TextFieldEditorUI : FieldEditorUI<String> {
  var format: String
}

interface LongFieldEditorUI : FieldEditorUI<Long> {
  var range: Pair<Long, Long>
  var style: String //Slider, List
}

interface DateFieldEditorUI : FieldEditorUI<Date> {
  var range: Pair<Date, Date>
  var style: String //Slider, List
}


interface ListEditorUI : UI {
  var uiState: UIState<Int>
  var onUpdate: (String) -> UIState<Int>
  var style: String //Dropdown, radio
  var values: List<String>
}

interface LabelUI : UI {
  val label: String
}

// Placeholder
interface ImageIUI : UI {
  var image: Image
}


interface ActionUI : UI {
  val label: String
  val enabled: Boolean
  val onFired: (source: ActionUI) -> Unit
}


interface ActionGroupUI : UI {
  val actions: MutableList<ActionUI>
}

sealed class ValidState(val msg: String = "") {
  object OK : ValidState()
  class Warning(msg: String) : ValidState(msg)
  class Error(msg: String) : ValidState(msg)
}

data class UIState<T>(
    val value: T? = null,
    val isReadOnly: Boolean = false,
    val validState: ValidState = ValidState.OK,
    val hasFocus: Boolean = false
)

/**
 *
 */
interface ContainerUI {
  fun textFieldEditorUI(name: String): TextFieldEditorUI
  fun longFieldEditorUI(name: String): LongFieldEditorUI
  fun fieldListUI(name: String): ListEditorUI
  fun containerUI(name: String): ContainerUI
  fun actionUI(name: String): ActionUI
  fun labelUI(name: String): LabelUI
  fun actionGroupUI(name: String): ActionGroupUI
}

interface WindowUI {
  val mainContainer: ContainerUI
}

/**
 *
 */
interface AppUI {

}


