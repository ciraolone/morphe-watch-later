/*
 * La patch vera e propria: aggiunge una voce "Guarda piu' tardi" alla barra di navigazione di YouTube.
 * Funziona in due iniezioni. Nella prima intercetta la costruzione di ogni voce della barra e, quando
 * riconosce quella Home, chiede all'estensione di sfornarne una copia modificata (icona, etichetta e
 * destinazione verso la playlist WL): la copia viene ricostruita come oggetto YouTube e messa da parte.
 * Nella seconda intercetta il montaggio della lista delle voci e ci infila quella messa da parte.
 * A differenza del pulsante Cerca ufficiale non serve agganciare nulla al tap: la destinazione viaggia
 * come dato dentro la voce stessa (browseId VLWL), quindi e' YouTube a navigare da sola.
 * Il blocco smali e la gestione dei registri sono ricopiati da NavigationBarPatch di morphe-patches:
 * l'ordine delle istruzioni e il backup del registro non sono arbitrari, non riordinarli.
 */

package app.ciraolone.patches.watchlater

import app.ciraolone.patches.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS =
    "Lapp/ciraolone/extension/watchlater/WatchLaterButtonPatch;"

@Suppress("unused")
val watchLaterButtonPatch = bytecodePatch(
    name = "Watch later button",
    description = "Aggiunge un pulsante 'Guarda piu tardi' alla barra di navigazione in basso, "
            + "che apre direttamente la playlist Guarda piu tardi.",
    default = true
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(protoLibraryFixPatch)

    extendWith("extensions/extension.mpe")

    execute {
        PivotBarRendererFingerprint.let {
            it.method.apply {
                val pivotBarItemRendererType =
                    it.instructionMatches[2].instruction.getReference<TypeReference>()!!.type
                val pivotBarRendererConstructorIndex = it.instructionMatches[3].index
                val pivotBarRendererConstructorReference =
                    getInstruction<ReferenceInstruction>(pivotBarRendererConstructorIndex).reference as MethodReference
                val pivotBarRendererConstructorInstruction =
                    getInstruction<RegisterRangeInstruction>(pivotBarRendererConstructorIndex)
                val pivotBarRendererConstructorStartRegister =
                    pivotBarRendererConstructorInstruction.startRegister
                val pivotBarRendererConstructorEndRegister =
                    pivotBarRendererConstructorStartRegister + pivotBarRendererConstructorInstruction.registerCount - 1
                val messageLiteIndex = pivotBarRendererConstructorReference.parameterTypes
                    .indexOfFirst { parameterType -> parameterType == "Lcom/google/protobuf/MessageLite;" }
                val messageLiteRegister =
                    pivotBarRendererConstructorStartRegister + messageLiteIndex + 1
                val insertIndex = it.instructionMatches.last().index
                val backupRegister = getFreeRegisterProvider(insertIndex, 1).getFreeRegister()
                val parseByteArrayMethod = parseByteArrayMethodRef.get()!!

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        # Backup original MessageLite register using /16 to avoid 4-bit register limits
                        move-object/16 v$backupRegister, v$messageLiteRegister

                        invoke-static { v$messageLiteRegister }, $EXTENSION_CLASS->parsePivotBarItemRenderer(Lcom/google/protobuf/MessageLite;)[B
                        move-result-object v$pivotBarRendererConstructorStartRegister
                        if-eqz v$pivotBarRendererConstructorStartRegister, :ignore_watch_later

                        sget-object v$messageLiteRegister, $pivotBarItemRendererType->a:$pivotBarItemRendererType
                        invoke-static { v$messageLiteRegister, v$pivotBarRendererConstructorStartRegister }, $parseByteArrayMethod
                        move-result-object v$messageLiteRegister
                        check-cast v$messageLiteRegister, $pivotBarItemRendererType

                        new-instance v$pivotBarRendererConstructorStartRegister, ${pivotBarRendererConstructorReference.definingClass}
                        invoke-direct/range { v$pivotBarRendererConstructorStartRegister .. v$pivotBarRendererConstructorEndRegister }, $pivotBarRendererConstructorReference

                        invoke-static { v$pivotBarRendererConstructorStartRegister }, $EXTENSION_CLASS->setPivotBarRenderer(Ljava/lang/Object;)V
                        :ignore_watch_later

                        # Restore MessageLite register
                        move-object/16 v$messageLiteRegister, v$backupRegister
                        nop
                        """
                )
            }
        }

        PivotBarRendererListFingerprint.let {
            it.method.apply {
                val insertMatch = it.instructionMatches[2]
                val insertIndex = insertMatch.index
                val insertRegister =
                    getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val protoListBuilderMethod = Fingerprint(
                    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
                    returnType = insertMatch.instruction.getReference<FieldReference>()!!.type,
                    parameters = listOf("Ljava/util/Collection;")
                ).method

                addInstructions(
                    insertIndex,
                    """
                        # If a renderer was copied to the extension, it is added to the list.
                        invoke-static { v$insertRegister }, $EXTENSION_CLASS->getPivotBarRendererList(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$insertRegister

                        # Convert to proto list.
                        invoke-static { v$insertRegister }, $protoListBuilderMethod
                        move-result-object v$insertRegister
                    """
                )
            }
        }
    }
}
