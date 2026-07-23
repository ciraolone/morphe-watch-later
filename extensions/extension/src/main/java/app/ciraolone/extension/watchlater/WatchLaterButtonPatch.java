/*
 * Codice che gira dentro YouTube per conto della patch. Espone tre punti d'ingresso chiamati dal
 * bytecode iniettato: parsePivotBarItemRenderer riceve una voce della barra, e se e' quella Home
 * ne restituisce una copia trasformata in "Guarda piu' tardi" (icona orologio, etichetta nostra,
 * destinazione verso la playlist WL); setPivotBarRenderer mette da parte la voce gia' ricostruita
 * come oggetto YouTube; getPivotBarRendererList la aggiunge in coda alla lista delle voci quando
 * la barra viene montata. La voce Home e' scelta come modello perche' e' l'unica sempre presente
 * e con la struttura giusta da clonare.
 */

package app.ciraolone.extension.watchlater;

import android.util.Log;

import com.google.protobuf.MessageLite;

import java.util.ArrayList;
import java.util.List;

import app.ciraolone.extension.watchlater.WatchLaterOuterClass.Accessibility;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.AccessibilityData;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.BrowseEndpoint;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.Icon;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.NavigationEndpoint;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.PivotBarItemRenderer;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.Runs;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.Title;
import app.ciraolone.extension.watchlater.WatchLaterOuterClass.YTIconType;

@SuppressWarnings("unused")
public final class WatchLaterButtonPatch {

    private static final String TAG = "WatchLaterButton";

    /**
     * Identificatore della playlist Guarda piu' tardi nel formato browseId di YouTube
     * ("VL" + id playlist). Usato sia come destinazione sia come identita' della voce,
     * cosi' YouTube non la confonde con la voce Home da cui e' stata clonata.
     */
    private static final String WATCH_LATER_BROWSE_ID = "VLWL";

    private static final String BUTTON_LABEL = "Guarda più tardi";

    private static Object pivotBarWatchLaterRenderer = null;

    /**
     * Injection point.
     */
    public static byte[] parsePivotBarItemRenderer(MessageLite messageLite) {
        try {
            PivotBarItemRenderer.Builder builder =
                    PivotBarItemRenderer.parseFrom(messageLite.toByteArray()).toBuilder();

            // Il valore numerico e non il nome: l'enum dichiarato nel proto e' parziale, quindi
            // le icone non elencate tornerebbero come UNRECOGNIZED.
            int iconValue = builder.getIcon().getYtIconTypeValue();
            if (iconValue != YTIconType.PIVOT_HOME_VALUE
                    && iconValue != YTIconType.TAB_HOME_CAIRO_VALUE) {
                return null;
            }

            builder.setPivotIdentifier(WATCH_LATER_BROWSE_ID);
            builder.setTargetId(WATCH_LATER_BROWSE_ID);

            builder.clearIcon();
            builder.setIcon(Icon.newBuilder().setYtIconType(YTIconType.WATCH_LATER_CAIRO).build());

            builder.clearTitle();
            builder.setTitle(Title.newBuilder()
                    .setRuns(Runs.newBuilder().setText(BUTTON_LABEL))
                    .build());

            builder.clearAccessibility();
            builder.setAccessibility(Accessibility.newBuilder()
                    .setAccessibilityData(AccessibilityData.newBuilder().setLabel(BUTTON_LABEL))
                    .build());

            builder.clearNavigationEndpoint();
            builder.setNavigationEndpoint(NavigationEndpoint.newBuilder()
                    .setBrowseEndpoint(BrowseEndpoint.newBuilder().setBrowseId(WATCH_LATER_BROWSE_ID))
                    .build());

            return builder.build().toByteArray();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to parse PivotBarItemRenderer", ex);
            return null;
        }
    }

    /**
     * Injection point.
     * Called after {@link #parsePivotBarItemRenderer(MessageLite)}.
     */
    public static void setPivotBarRenderer(Object object) {
        pivotBarWatchLaterRenderer = object;
    }

    /**
     * Injection point.
     * Called after {@link #setPivotBarRenderer(Object)}.
     */
    public static List<Object> getPivotBarRendererList(List<Object> list) {
        if (list == null || list.isEmpty() || pivotBarWatchLaterRenderer == null) {
            return list;
        }

        List<Object> newList = new ArrayList<>(list);
        newList.add(pivotBarWatchLaterRenderer);
        return newList;
    }
}
