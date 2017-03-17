package validators

fun <T: Enum<T>> isInEnum(v:String, values: Array<T>) : String? =
        if (v in values.map {it.name}) null else "Must be one of " + values

fun notEmptyString(s: String?): String? = if (s == null || s.isEmpty()) "Value is empty or null" else null

fun fileExists(it: java.io.File?) : String? {
    if (it == null) {
        return "File name is null"
    }
    return if (it.exists()) null else "File not found: " + it.absoluteFile
}

fun dirExists(it: java.io.File?) : String? {
    if (it == null) {
        return "Filename is null"
    }
    if (it.exists() ) {
        if (!it.isDirectory())   {
            return "File is not a directory: " + it.absoluteFile
        }
        if (!it.canWrite()) {
            return "Directory doesn't have write  rights: " + it.absoluteFile

        }
    } else {
        return  "Directory not found: " + it.absoluteFile

    }
    return null
}
