package su.geocaching.android.ui.map.providers;

/**
 * @author Yuri Denison
 * @since 01/09/14
 */
public class MapBoxStandardProvider extends OsmUrlTileProvider {
    private final String mapBoxMapId = "volkman.jckjiine";

    @Override
    protected String getUrlTemplate() {
        return "https://api.tiles.mapbox.com/v3/" + mapBoxMapId + "/%d/%d/%d.png";
    }
}
