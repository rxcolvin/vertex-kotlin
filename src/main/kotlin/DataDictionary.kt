/**
 * Created by richard.colvin on 30/11/2016.
 */
package datadictionary

import meta.enumFieldMeta
import meta.fileFieldMeta
import meta.stringFieldMeta
import meta.stringListFieldMeta
import validators.dirExists
import validators.fileExists

enum class ElementType {
  PACKAGE,
  DIAGRAM,
  COMPONENT
}

enum class HeaderLevel(val level: Int) {
  H1(1),
  H2(2),
  H3(3),
  H4(4),
  H5(5),
  H6(6)
}

val url = stringFieldMeta(
    name = "url",
    description = "The url of the confluence instance. Typically, https://confluence.performgroup.com"
)

val user = stringFieldMeta(
    name = "user",
    description = "The confluence user id for the given instance"
)

val password = stringFieldMeta(
    name = "password",
    description = "The confluence password for the given user"
)

val diagramDir = fileFieldMeta(
    name = "diagramDir",
    description = "A temporary directory where images generated from EA will be stored so they can be uploaded into Confluence",
    validator = ::dirExists
)

val unwantedTags = stringListFieldMeta(
    name = "unwantedTags",
    description = "A csv list of tagged values, which are typically included by EA directly, which we wish to exclude from our model "
)


val eapFile = fileFieldMeta(
    name = "eapFile",
    description = "The path to the EAP file to ue for the document generation",
    validator = ::fileExists
)

val taggedValueFile = fileFieldMeta(
    name = "taggedValueFile",
    description = "The path to a copy of the Tagged Value Type export xml file",
    validator = ::fileExists
)

val parentPage = stringFieldMeta(
    name = "parentPage",
    description = "The parent page in confluence where new pages will be added."
)


val protoPackage = stringFieldMeta(
    name = "protoPackage",
    description = "This is the path to the package that contains the Prototype model in an EAP file. Typcially this is needed to generated the correct properties for Customizations"
)

val patternPackage = stringFieldMeta(
    name = "patternPackage",
    description = "This is the path to the package that contains the Pattern model in an EAP file"
)

val spaceId = stringFieldMeta(
    name = "spaceId",
    description = "The id of the confluence space where content will be generated"
)

val sourcePath = stringFieldMeta(
    name = "sourcePath",
    description = "The path to the element (package, diagram or component) for which content will be generated"
)

val elementType = enumFieldMeta(
    name = "elementType",
    description = "The type of the elememt that the source path points to: one of ",
    kclass = ElementType::class
)


val pageTitle = stringFieldMeta(
    name = "pageTitle",
    description = "The title for the created page. This MUST be unique in the selected space"
)

val templateName = stringFieldMeta(
    name = "templateName",
    description = "The name of the template to use. Please see separate list"
)

val headerLevel = enumFieldMeta(
    name = "headerLevel",
    description = "The starting header level to use when generating header content: one of ",
    kclass = HeaderLevel::class
)

val dataDictionary = listOf(
    ::url,
    ::elementType,
    ::diagramDir,
    ::eapFile,
    ::headerLevel,
    ::pageTitle,
    ::parentPage,
    ::user,
    ::password,
    ::protoPackage,
    ::spaceId,
    ::templateName,
    ::unwantedTags
).map { Pair(it.name, it.get()) }
