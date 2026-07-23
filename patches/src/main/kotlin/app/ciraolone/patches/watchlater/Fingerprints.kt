/*
 * Le "impronte" che localizzano nel bytecode offuscato di YouTube i punti in cui questa patch
 * inserisce il pulsante. PivotBarRendererFingerprint trova il metodo che costruisce una singola
 * voce della barra di navigazione (identificato dal numero 117501096, che e' il campo protobuf
 * pivotBarItemRenderer); PivotBarRendererListFingerprint trova il punto in cui la lista completa
 * delle voci viene assemblata, dove aggiungiamo la nostra. Le altre tre servono al fix della
 * libreria protobuf (vedi ProtoLibraryFixPatch). Tutte ricopiate da morphe-patches ufficiale:
 * se YouTube cambia quei metodi le impronte smettono di combaciare e la patch va riallineata li'.
 */

package app.ciraolone.patches.watchlater

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PivotBarRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    returnType = "Lj$/util/Optional;",
    filters = listOf(
        literal(117501096L),
        opcode(Opcode.IF_NE),
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            definingClass = "this",
            name = "<init>",
            returnType = "V"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

internal object PivotBarRendererListFingerprint : Fingerprint(
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "L"
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L"),
            returnType = "L"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "L"
        ),
        literal(45633821L),
    )
)

internal object EmptyRegistryFingerprint : Fingerprint(
    definingClass = "Lcom/google/protobuf/ExtensionRegistryLite;",
    name = "getGeneratedRegistry",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Lcom/google/protobuf/ExtensionRegistryLite;"
)

internal object MessageLiteWriteToFingerprint : Fingerprint(
    definingClass = "Lcom/google/protobuf/MessageLite;",
    name = "writeTo",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.ABSTRACT),
    parameters = listOf("L"),
    returnType = "V"
)

internal object ProtobufClassParseByteArrayFingerprint : Fingerprint(
    name = "parseFrom",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "[B")
)
