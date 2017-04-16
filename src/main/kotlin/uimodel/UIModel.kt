/**
 * Created by richard.colvin on 17/03/2017.
 */
package uimodel


interface UIComponent {
    val name: String

}

interface UIEditor<T> : UIComponent  {
    val uiState:UIState<T>
    val onUpdate:(T)->UIState<T>

}

interface UIAction : UIComponent {
    val enabled : Boolean
    val onFired : (source: UIAction) -> Unit
}


sealed class ValidState(val msg: String = "") {
    object OK : ValidState()
    class Warning(msg: String) : ValidState(msg)
    class Error(msg: String) : ValidState(msg)
}


data class UIState<T> (
        val value: T,
        val isReadOnly: Boolean = false,
        val validState: ValidState = ValidState.OK,
        val hasFocus: Boolean = false

        )