/**
 * Created by richard.colvin on 17/03/2017.
 */
package uimodel

import kotlin.reflect.KClass


interface UI{
    val name: String

}

interface UIFactory {
   fun <T: Any> createFieldEditor(type: KClass<T>)
  
}

interface FieldEditorUI<T> : UI  {
    var uiState:UIState<T>
    var onUpdate:(T)-> UIState<T>

}

interface ActionUI : UI {
    val enabled : Boolean
    val onFired : (source: ActionUI) -> Unit
}

interface ActionGroupUI : UI {
  val actions: MutableList<ActionUI>
}


sealed class ValidState(val msg: String = "") {
    object OK : ValidState()
    class Warning(msg: String) : ValidState(msg)
    class Error(msg: String) : ValidState(msg)
}


data class UIState<T> (
        val value: T?,
        val isReadOnly: Boolean = false,
        val validState: ValidState = ValidState.OK,
        val hasFocus: Boolean = false

        )

interface EntityEditorUI {

}



interface AppUI {

  val actions: MutableList<ActionUI>



}


